package com.saneb.db;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.domain.announcementattachment.service.impl.AttachmentApplicationCodeFingerprint;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;

/** 동일 설치 코드에서 기존 고정 시험을 JUnit lifecycle/timeout 그대로 실행하는 일회성 도구. */
public final class AnnouncementAttachmentOfficialWorkerProbe {
    static final List<String> CASES=List.of("YANGPYEONG-312241","YANGPYEONG-311846","YANGPYEONG-311507");
    private static final Set<String> ENV=Set.of("PATH","LANG","HOME","TMPDIR","PWD","SANEB_ATTACHMENT_OFFICIAL_WORKER_QA");
    private AnnouncementAttachmentOfficialWorkerProbe() {}

    static boolean selectComplete(long found,long succeeded,long failed,long skipped,long aborted,long containersFailed) {
        return found==3&&succeeded==3&&failed==0&&skipped==0&&aborted==0&&containersFailed==0;
    }
    static List<Object> selectFailureTrace(Throwable failure) {
        var trace=new java.util.ArrayList<Object>();
        var seen=java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<Throwable,Boolean>());
        for(Throwable cause=failure;cause!=null&&trace.size()<8&&seen.add(cause);cause=cause.getCause()) {
            var frames=java.util.Arrays.stream(cause.getStackTrace()).limit(6)
                    .map(frame->frame.getClassName()+"#"+frame.getMethodName()+":"+frame.getLineNumber()).toList();
            trace.add(java.util.Map.of("type",cause.getClass().getName(),"frames",frames));
        }
        return trace;
    }
    public static void main(String[] args) {
        PrintStream output=System.out;
        System.setOut(new PrintStream(OutputStream.nullOutputStream()));
        System.setErr(new PrintStream(OutputStream.nullOutputStream()));
        var result=new LinkedHashMap<String,Object>();
        result.put("kind","OFFICIAL_WORKER_PROBE");result.put("productionDatabaseUsed",false);
        result.put("isPolicyQaPassed",false);result.put("isAuthenticatedBrowserE2e",false);
        boolean passed=false;String stage="BOUNDARY";
        try {
            if(args.length!=1||!args[0].matches("[0-9a-f]{64}")
                    ||!"Linux".equals(System.getProperty("os.name"))||"root".equals(System.getProperty("user.name"))
                    ||!"/work".equals(Path.of("").toAbsolutePath().toString())
                    ||!"/work/tmp".equals(System.getProperty("java.io.tmpdir"))
                    ||!"true".equals(System.getenv("SANEB_ATTACHMENT_OFFICIAL_WORKER_QA"))
                    ||!ENV.containsAll(System.getenv().keySet())) throw new IllegalStateException();
            var json=new ObjectMapper();
            stage="CODE_IDENTITY";
            String codeHash=new AttachmentApplicationCodeFingerprint(json).selectVerifiedHash();
            if(!args[0].equals(codeHash))throw new IllegalStateException();
            result.put("executionCodeHash",codeHash);
            stage="JCE_TLS_RUNTIME";
            // DB/외부 요청 전에 설치 JDK의 암호화 정책·기본 TLS 초기화를 확인한다.
            javax.crypto.Cipher.getInstance("AES/GCM/NoPadding");
            javax.net.ssl.SSLContext.getDefault();
            System.setProperty("saneb.attachment-qa.extractor-root","/qa/extractor");
            System.setProperty("saneb.attachment-official-worker.report","/work/reports");
            stage="JUNIT_EXECUTION";
            var listener=new SummaryGeneratingListener();
            var request=LauncherDiscoveryRequestBuilder.request()
                    .selectors(DiscoverySelectors.selectClass(AnnouncementAttachmentOfficialWorkerIntegrationTest.class))
                    .configurationParameter("junit.jupiter.execution.parallel.enabled","false")
                    .configurationParameter("junit.jupiter.tempdir.cleanup.mode.default","ALWAYS").build();
            LauncherFactory.create().execute(request,listener);
            var summary=listener.getSummary();
            result.put("found",summary.getTestsFoundCount());result.put("passed",summary.getTestsSucceededCount());
            result.put("failed",summary.getTestsFailedCount());result.put("skipped",summary.getTestsSkippedCount());
            result.put("aborted",summary.getTestsAbortedCount());result.put("failedContainers",summary.getContainersFailedCount());
            // 예외 메시지/본문/SQL을 제외하고 자료형과 제한된 클래스·메서드 위치만 진단한다.
            result.put("failureTypes",summary.getFailures().stream().map(f->f.getException().getClass().getSimpleName()).distinct().limit(8).toList());
            result.put("failureSites",summary.getFailures().stream().limit(3).map(f->selectFailureTrace(f.getException())).toList());
            stage="REPORTS";
            var reports=new java.util.ArrayList<Object>();boolean reportsComplete=true;
            for(String code:CASES) {
                Path path=Path.of("/work/reports",code+".json");
                if(!Files.isRegularFile(path)||Files.size(path)>65536){reportsComplete=false;continue;}
                var report=json.readTree(Files.readAllBytes(path));
                if(!code.equals(report.path("caseCode").asText())
                        ||!"OFFICIAL_WORKER_EPHEMERAL_DB_API_V1".equals(report.path("scope").asText()))throw new IllegalStateException();
                reports.add(report);
                String expected=code.equals(CASES.getLast())?"TITLE_EXCLUDED_NOT_FETCHED":"WORKER_DB_API_OBSERVED_NOT_APPROVED";
                reportsComplete&=expected.equals(report.path("status").asText())
                        &&report.path("originalFilesRemoved").asBoolean(false)&&report.path("remainingResourceLeases").asInt(-1)==0;
            }
            result.put("cases",reports);
            stage="FINAL_IDENTITY";
            if(!codeHash.equals(new AttachmentApplicationCodeFingerprint(json).selectVerifiedHash()))throw new IllegalStateException();
            passed=reportsComplete&&selectComplete(summary.getTestsFoundCount(),summary.getTestsSucceededCount(),
                    summary.getTestsFailedCount(),summary.getTestsSkippedCount(),summary.getTestsAbortedCount(),summary.getContainersFailedCount());
        } catch(Exception|LinkageError|AssertionError failure) {
            result.put("failedStage",stage);result.put("failureCode","OFFICIAL_WORKER_PROBE_INCOMPLETE");
            result.put("failureSites",selectFailureTrace(failure));
        }
        result.put("status",passed?"PASSED":"INCOMPLETE");
        try {output.println(new ObjectMapper().writeValueAsString(result));}
        catch(Exception ignored){output.println("{\"kind\":\"OFFICIAL_WORKER_PROBE\",\"status\":\"INCOMPLETE\"}");}
        System.exit(passed?0:1);
    }
}
