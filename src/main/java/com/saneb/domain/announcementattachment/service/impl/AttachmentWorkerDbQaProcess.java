package com.saneb.domain.announcementattachment.service.impl;

import com.fasterxml.jackson.databind.*;
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BooleanSupplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** 정책 부모가 namespace/pipe/임시 DB를 소유한다. shell·운영 DB·외부 네트워크를 실행 입력으로 받지 않는다. */
@Component
public final class AttachmentWorkerDbQaProcess {
    static final int MAX_OUTPUT = 1048576;
    static final int MAX_ERROR_OUTPUT = 65536;
    private static final Semaphore PROCESS_SLOT=new Semaphore(1);
    private final Path distribution;
    private final ObjectMapper mapper;
    public record Result(JsonNode report, Instant startedAt, Instant completedAt, boolean originalRemoved) { }
    public AttachmentWorkerDbQaProcess(
            @Value("${saneb.announcement-attachment.policy-validation.contract-qa-root:/opt/saneb/attachment-contract-qa}") String root,
            ObjectMapper mapper) {
        this.distribution=Path.of(root).toAbsolutePath().normalize();
        this.mapper=mapper.copy().enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                .enable(com.fasterxml.jackson.core.JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
    }
    Path selectDistribution() { return distribution; }
    public Result selectResult(boolean inventory, Instant deadline, BooleanSupplier allowed) {
        if(!PROCESS_SLOT.tryAcquire()) throw new Failure("QA_PROCESS_BUSY");
        Path work=null;
        try {
            if (!"Linux".equals(System.getProperty("os.name"))) throw new Failure("ISOLATION_UNAVAILABLE");
            String uid=Files.readAllLines(Path.of("/proc/self/status")).stream().filter(v->v.startsWith("Uid:")).findFirst().orElseThrow();
            if(Arrays.stream(uid.substring(4).strip().split("\\s+")).anyMatch("0"::equals)) throw new Failure("NON_ROOT_REQUIRED");
            for(String tool:List.of("/usr/bin/bwrap","/usr/bin/prlimit"))
                if(!Files.isExecutable(Path.of(tool))) throw new Failure("ISOLATION_UNAVAILABLE");
            Path root=distribution.toRealPath(), javaHome=Path.of(System.getProperty("java.home")).toRealPath();
            for(String directory:List.of("lib","bin","config","extractor"))
                if(Files.isSymbolicLink(root.resolve(directory)) || !Files.isDirectory(root.resolve(directory))) throw new Failure("QA_ARTIFACT_INVALID");
            for(String file:List.of("bin/run-attachment-contract-qa.sh","config/logback-qa.xml","config/logging.properties","config/hosts"))
                if(!Files.isRegularFile(root.resolve(file),LinkOption.NOFOLLOW_LINKS)) throw new Failure("QA_ARTIFACT_INVALID");
            if(!Files.isExecutable(javaHome.resolve("bin/java"))) throw new Failure("ISOLATION_UNAVAILABLE");
            if(!allowed.getAsBoolean() || deadline==null || !deadline.isAfter(Instant.now())) throw new Failure("EXECUTION_STOPPED");
            work=Files.createTempDirectory(Path.of("/tmp"),"saneb-policy-db-qa-",PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
            Files.createDirectory(work.resolve("tmp"));
            var builder=new ProcessBuilder(selectCommand(root,javaHome,work,inventory));
            builder.environment().clear();builder.environment().put("PATH","/usr/bin:/bin");builder.environment().put("LANG","C.UTF-8");
            builder.directory(work.toFile());
            var result=selectProcessResult(builder::start,deadline,inventory?30:600,allowed,mapper);
            deleteWork(work); work=null;
            return new Result(result.report(),result.startedAt(),Instant.now(),true);
        } catch(Failure failure) { throw failure; }
        catch(Exception exception) { throw new Failure("QA_PROCESS_OR_CLEANUP_FAILED"); }
        finally {try {if(work!=null) deleteWork(work);} finally {PROCESS_SLOT.release();}}
    }
    static List<String> selectCommand(Path root,Path javaHome,Path work,boolean inventory) throws IOException {
        var args=new ArrayList<>(List.of("/usr/bin/prlimit","--as=2147483648","--cpu=480","--nofile=256","--nproc=128","--fsize=134217728","--",
                "/usr/bin/bwrap","--unshare-all","--die-with-parent","--new-session","--cap-drop","ALL"));
        for(String mount:List.of("/usr","/bin","/lib")) args.addAll(List.of("--ro-bind",mount,mount));
        if(Files.isDirectory(Path.of("/lib64"))) args.addAll(List.of("--ro-bind","/lib64","/lib64"));
        args.addAll(List.of("--ro-bind",javaHome.toString(),"/jre"));
        for(String file:List.of("/etc/ld.so.cache","/etc/passwd","/etc/group","/etc/nsswitch.conf"))
            if(Files.isRegularFile(Path.of(file))) args.addAll(List.of("--ro-bind",file,file));
        Path security=javaHome.resolve("conf/security/java.security");
        if(Files.exists(security) && !security.toRealPath().startsWith(javaHome))
            args.addAll(List.of("--ro-bind",security.toRealPath().toString(),security.toRealPath().toString()));
        args.addAll(List.of("--ro-bind",root.resolve("config/hosts").toString(),"/etc/hosts","--dir","/qa"));
        // 배포 폴더 전체나 app.env는 넣지 않는다. QA 코드/설정/추출기 네 하위 경로만 읽기 전용이다.
        for(String directory:List.of("lib","bin","config","extractor"))
            args.addAll(List.of("--ro-bind",root.resolve(directory).toString(),"/qa/"+directory));
        args.addAll(List.of("--proc","/proc","--dev","/dev","--tmpfs","/dev/shm","--tmpfs","/tmp",
                "--bind",work.toString(),"/work","--chdir","/work","--clearenv"));
        var env=new TreeMap<>(Map.of("PATH","/usr/bin:/bin","LANG","C.UTF-8","HOME","/work","TMPDIR","/work/tmp",
                "SANEB_ATTACHMENT_JOB_TEST","true","SANEB_ATTACHMENT_MIGRATION_TEST","true","SANEB_ATTACHMENT_WORKER_QA","true"));
        env.forEach((key,value)->args.addAll(List.of("--setenv",key,value)));
        args.addAll(List.of("/jre/bin/java","-Xms32m","-Xmx384m","-XX:ActiveProcessorCount=1","-XX:+UseSerialGC","-XX:MaxMetaspaceSize=192m",
                "-XX:CompressedClassSpaceSize=64m","-XX:ReservedCodeCacheSize=64m","-Xss512k","-Djava.io.tmpdir=/work/tmp",
                "-Djava.net.preferIPv4Stack=true","-Dlogback.configurationFile=/qa/config/logback-qa.xml",
                "-Djava.util.logging.config.file=/qa/config/logging.properties","-cp","/qa/lib/*","com.saneb.qa.AttachmentContractQaMain"));
        if(inventory) args.add("--inventory");
        return List.copyOf(args);
    }
    @FunctionalInterface interface Launcher { Process start() throws IOException; }
    static Result selectProcessResult(Launcher launcher,Instant deadline,int maximumSeconds,BooleanSupplier allowed,ObjectMapper mapper) {
        Instant start=Instant.now();
        long remaining=deadline==null?0:Math.min(Duration.between(start,deadline).toMillis(),maximumSeconds*1000L);
        if(remaining<=0 || !allowed.getAsBoolean()) throw new Failure("EXECUTION_STOPPED");
        long expires=System.nanoTime()+TimeUnit.MILLISECONDS.toNanos(remaining);
        Process process=null;ExecutorService readers=Executors.newVirtualThreadPerTaskExecutor();Future<byte[]> output=null,error=null;
        try {
            try { process=launcher.start(); }
            catch(IOException exception) { throw new Failure("QA_PROCESS_START_FAILED"); }
            process.getOutputStream().close();
            Process owned=process;
            output=readers.submit(()->selectBoundedOutput(owned.getInputStream(),MAX_OUTPUT));
            // stderr도 동시에 소비하되 원문을 예외·로그·보고서에 남기지 않는다.
            error=readers.submit(()->selectBoundedOutput(owned.getErrorStream(),MAX_ERROR_OUTPUT));
            while(true) {
                if(Thread.currentThread().isInterrupted() || !allowed.getAsBoolean()) throw new Failure("EXECUTION_STOPPED");
                if(System.nanoTime()>=expires) throw new Failure("QA_DEADLINE_EXCEEDED");
                if(output.isDone()) output.get(); // 출력 초과/pipe 실패를 프로세스 종료까지 미루지 않는다.
                if(error.isDone()) error.get();
                if(process.waitFor(200,TimeUnit.MILLISECONDS)) break;
            }
            if(!allowed.getAsBoolean() || System.nanoTime()>=expires) throw new Failure("EXECUTION_STOPPED");
            byte[] bytes=output.get(Math.max(1,Math.min(2000,TimeUnit.NANOSECONDS.toMillis(expires-System.nanoTime()))),TimeUnit.MILLISECONDS);
            byte[] errors=error.get(Math.max(1,Math.min(2000,TimeUnit.NANOSECONDS.toMillis(expires-System.nanoTime()))),TimeUnit.MILLISECONDS);
            if(process.exitValue()!=0) throw new Failure(selectChildFailureCode(bytes,errors));
            JsonNode report;
            try { report=mapper.readTree(bytes); }
            catch(com.fasterxml.jackson.core.JsonProcessingException exception) {
                // exit 0이어도 JVM 경고 등이 섞인 stdout은 JSON 증거가 아니다. 경고를 잘라내 성공시키지 않는다.
                String diagnostic=selectChildFailureCode(bytes,errors);
                throw new Failure("QA_CHILD_FAILED".equals(diagnostic)?"QA_REPORT_PARSE_FAILED":diagnostic);
            }
            if(report==null || !report.isObject()) throw new Failure("QA_REPORT_INVALID");
            return new Result(report,start,Instant.now(),false);
        } catch(Failure failure) { throw failure; }
        catch(InterruptedException exception) { Thread.currentThread().interrupt();throw new Failure("EXECUTION_STOPPED"); }
        catch(ExecutionException exception) { throw exception.getCause() instanceof Failure failure?failure:new Failure("QA_OUTPUT_FAILED"); }
        catch(TimeoutException exception) { throw new Failure("QA_OUTPUT_TIMEOUT"); }
        catch(IOException exception) { throw new Failure("QA_PROCESS_IO_FAILED"); }
        catch(SecurityException exception) { throw new Failure("QA_PROCESS_PERMISSION_FAILED"); }
        catch(Exception exception) { throw new Failure("QA_PROCESS_FAILED"); }
        finally {
            boolean interrupted=Thread.interrupted();
            try {
                if(process!=null) {
                    // bwrap private PID namespace의 종료가 PG 자손의 수명 경계다. 소유한 프로세스만 종료한다.
                    var children=process.descendants().toList();
                    if(process.isAlive()) process.destroyForcibly();
                    children.forEach(child->{if(child.isAlive()) child.destroyForcibly();});
                    if(!process.waitFor(5,TimeUnit.SECONDS)) throw new Failure("QA_PROCESS_CLEANUP_FAILED");
                    process.getInputStream().close();process.getErrorStream().close();
                }
            } catch(InterruptedException exception) { interrupted=true;throw new Failure("QA_PROCESS_CLEANUP_FAILED"); }
            catch(IOException exception) { throw new Failure("QA_PROCESS_CLEANUP_FAILED"); }
            finally {if(output!=null) output.cancel(true);if(error!=null) error.cancel(true);readers.shutdownNow();if(interrupted) Thread.currentThread().interrupt();}
        }
    }
    private static byte[] selectBoundedOutput(InputStream stream,int maximum) throws IOException {
        try(var input=stream;var bytes=new ByteArrayOutputStream()) {
            byte[] buffer=new byte[8192];int count;
            while((count=input.read(buffer))!=-1) {
                if(bytes.size()+count>maximum) throw new Failure("QA_OUTPUT_LIMIT");
                bytes.write(buffer,0,count);
            }
            return bytes.toByteArray();
        }
    }
    private static String selectChildFailureCode(byte[] output,byte[] error) {
        // 외부 출력에서 추출한 문자열·경로·값은 절대 반환하지 않는다. 어떤 코드도 성공 근거가 아니다.
        String value=(new String(output,StandardCharsets.UTF_8)+"\n"+new String(error,StandardCharsets.UTF_8)).toLowerCase(Locale.ROOT);
        if(value.contains("resource temporarily unavailable") || value.contains("unable to create native thread")
                || value.contains("pthread_create failed")) return "QA_CHILD_PROCESS_LIMIT";
        if(value.contains("could not reserve enough space") || value.contains("native memory allocation")
                || value.contains("cannot allocate memory")) return "QA_CHILD_MEMORY_LIMIT";
        if(value.contains("could not find or load main class") || value.contains("noclassdeffounderror")) return "QA_CHILD_CLASS_LOADING_FAILED";
        if(value.contains("bwrap:")) {
            if(value.contains("operation not permitted") || value.contains("permission denied")) return "QA_ISOLATION_PERMISSION_FAILED";
            if(value.contains("no such file or directory") || value.contains("can't find source")
                    || value.contains("mount")) return "QA_ISOLATION_MOUNT_FAILED";
            return "QA_ISOLATION_START_FAILED";
        }
        return "QA_CHILD_FAILED";
    }
    private static void deleteWork(Path work) {
        try {
            Path target=work.toAbsolutePath().normalize();
            if(!target.getParent().equals(Path.of("/tmp")) || !target.getFileName().toString().startsWith("saneb-policy-db-qa-")
                    || Files.isSymbolicLink(target) || !target.toRealPath().equals(target)) throw new Failure("QA_CLEANUP_PATH_INVALID");
            Files.walkFileTree(target,new SimpleFileVisitor<>() {
                @Override public FileVisitResult visitFile(Path file,BasicFileAttributes attrs) throws IOException {Files.delete(file);return FileVisitResult.CONTINUE;}
                @Override public FileVisitResult postVisitDirectory(Path directory,IOException error) throws IOException {if(error!=null) throw error;Files.delete(directory);return FileVisitResult.CONTINUE;}
            });
            if(Files.exists(target,LinkOption.NOFOLLOW_LINKS)) throw new Failure("QA_PROCESS_CLEANUP_FAILED");
        } catch(Failure failure) {throw failure;}
        catch(Exception exception) {throw new Failure("QA_PROCESS_CLEANUP_FAILED");}
    }
    public static final class Failure extends RuntimeException {
        private final String code;
        public Failure(String code) {super(code);this.code=code;}
        public String selectCode() {return code;}
    }
}
