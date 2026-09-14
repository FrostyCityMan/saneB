package com.saneb.qa;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saneb.domain.announcementattachment.extraction.AttachmentRuntimeIdentity;
import com.saneb.domain.announcementattachment.service.impl.AttachmentApplicationCodeFingerprint;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.engine.JupiterTestEngine;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.launcher.*;
import org.junit.platform.launcher.core.LauncherConfig;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

/** 별도 JVM/namespace의 고정 합성 DB 계약 검증. 정책 전체 QA나 운영 게시 증거를 생성하지 않는다. */
public final class AttachmentContractQaMain {
    static final String SCOPE = "SYNTHETIC_WORKER_DB_CONTRACTS_V2";
    static final List<String> SUITES = List.of(
            "com.saneb.db.AnnouncementAttachmentJobIntegrationTest",
            "com.saneb.db.AnnouncementAttachmentMigrationTest",
            "com.saneb.db.AnnouncementAttachmentBackfillIntegrationTest",
            "com.saneb.db.AnnouncementAttachmentWorkerIntegrationTest");
    private AttachmentContractQaMain() { }

    public static void main(String[] args) {
        PrintStream report = System.out;
        // JUnit/SQL/임시 DB의 원문 예외와 logger가 보고서 채널에 섞이지 않게 한다.
        System.setOut(new PrintStream(OutputStream.nullOutputStream()));
        System.setErr(new PrintStream(OutputStream.nullOutputStream()));
        int exit = 1;
        try {
            boolean inventory = args.length == 1 && "--inventory".equals(args[0]);
            if (!inventory && args.length != 0) throw new QaFailure("ARGUMENTS_NOT_ALLOWED");
            if (!inventory) {
                validateEnvironment(System.getenv(), System.getProperties());
                System.setProperty("saneb.attachment-qa.extractor-root", "/qa/extractor");
            }
            String artifactHash = selectArtifactHash();
            var application = new AttachmentApplicationCodeFingerprint(new ObjectMapper());
            String executionCodeHash = application.selectVerifiedHash();
            // 전체 QA 산출물 hash만으로 현재 서버 업무 코드/추출기와 같다고 추정하지 않는다.
            // inventory는 Windows에서도 실행하므로 Linux runtime 증거를 생성하지 않는다.
            var extractor = inventory ? null : new AttachmentRuntimeIdentity("/qa/extractor");
            String extractorRuntimeHash = extractor == null ? null : extractor.selectIdentity().configHash();
            String runId = UUID.randomUUID().toString();
            String startedAt = Instant.now().toString();
            var request = LauncherDiscoveryRequestBuilder.request()
                    .selectors(SUITES.stream().map(DiscoverySelectors::selectClass).toList())
                    .configurationParameter("junit.jupiter.execution.parallel.enabled", "false")
                    .configurationParameter("junit.jupiter.extensions.autodetection.enabled", "false")
                    .configurationParameter("junit.jupiter.execution.timeout.default", "60 s")
                    .configurationParameter("junit.jupiter.execution.timeout.beforeall.method.default", "120 s")
                    .build();
            var config = LauncherConfig.builder().enableTestEngineAutoRegistration(false)
                    .enableTestExecutionListenerAutoRegistration(false)
                    .enableLauncherSessionListenerAutoRegistration(false)
                    .enableLauncherDiscoveryListenerAutoRegistration(false)
                    .enablePostDiscoveryFilterAutoRegistration(false)
                    .addTestEngines(new JupiterTestEngine()).build();
            try (var session = LauncherFactory.openSession(config)) {
                var plan = session.getLauncher().discover(request);
                var ledger = new Ledger(plan, SUITES);
                if (!inventory) session.getLauncher().execute(plan, ledger);
                if (!artifactHash.equals(selectArtifactHash())) throw new QaFailure("ARTIFACT_CHANGED");
                if (!executionCodeHash.equals(application.selectVerifiedHash())) throw new QaFailure("APPLICATION_CODE_CHANGED");
                if (extractor != null && !extractorRuntimeHash.equals(extractor.selectIdentity().configHash())) throw new QaFailure("EXTRACTOR_RUNTIME_CHANGED");
                var result = ledger.selectResult(inventory);
                var output = new TreeMap<String,Object>();
                output.put("scope", SCOPE); output.put("runId", runId);
                output.put("reportSchemaVersion", 2);
                output.put("startedAt", startedAt); output.put("completedAt", Instant.now().toString());
                output.put("artifactHash", artifactHash); output.put("result", result);
                output.put("executionCodeHash", executionCodeHash); output.put("extractorRuntimeHash", extractorRuntimeHash);
                report.println(new ObjectMapper().writeValueAsString(output));
                exit = inventory || "PASSED".equals(result.status()) ? 0 : 1;
            }
        } catch (Throwable exception) {
            String reason = exception instanceof QaFailure failure ? failure.code : "QA_EXECUTION_FAILED";
            report.println("{\"scope\":\"" + SCOPE + "\",\"status\":\"FAILED\",\"reasonCode\":\"" + reason + "\"}");
        }
        report.flush();
        System.exit(exit);
    }

    static void validateEnvironment(Map<String,String> env, Properties properties) throws Exception {
        if (!"Linux".equals(properties.getProperty("os.name"))) throw new QaFailure("LINUX_REQUIRED");
        if (!"/work/tmp".equals(properties.getProperty("java.io.tmpdir"))
                || !"/work".equals(properties.getProperty("user.dir"))) throw new QaFailure("ISOLATED_WORK_DIRECTORY_REQUIRED");
        // bubblewrap은 --clearenv 뒤에도 --chdir에 맞춘 PWD를 생성한다. 호스트 경로는 허용하지 않는다.
        Set<String> allowed = Set.of("PATH", "LANG", "HOME", "PWD", "TMPDIR", "SANEB_ATTACHMENT_JOB_TEST", "SANEB_ATTACHMENT_MIGRATION_TEST", "SANEB_ATTACHMENT_WORKER_QA");
        if (!allowed.containsAll(env.keySet()) || !"true".equals(env.get("SANEB_ATTACHMENT_JOB_TEST"))
                || !"true".equals(env.get("SANEB_ATTACHMENT_MIGRATION_TEST"))
                || !"true".equals(env.get("SANEB_ATTACHMENT_WORKER_QA"))
                || !"/work".equals(env.get("HOME")) || !"/work".equals(env.get("PWD"))
                || !"/work/tmp".equals(env.get("TMPDIR")))
            throw new QaFailure("CLEAN_ENVIRONMENT_REQUIRED");
        if (properties.stringPropertyNames().stream().anyMatch(name -> name.startsWith("spring.datasource.")
                || name.startsWith("spring.config.") || name.startsWith("jdbc.") || name.startsWith("junit.jupiter.conditions.")
                || name.startsWith("saneb.attachment-qa.")))
            throw new QaFailure("EXTERNAL_CONFIGURATION_NOT_ALLOWED");
        String uid = Files.readAllLines(Path.of("/proc/self/status")).stream().filter(line -> line.startsWith("Uid:"))
                .findFirst().orElseThrow(() -> new QaFailure("PROCESS_IDENTITY_UNAVAILABLE"));
        if (Arrays.stream(uid.substring(4).strip().split("\\s+")).anyMatch("0"::equals)) throw new QaFailure("NON_ROOT_REQUIRED");
    }

    private static String selectArtifactHash() throws Exception {
        Path jar = Path.of(AttachmentContractQaMain.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath();
        if (!Files.isRegularFile(jar) || !jar.toString().endsWith(".jar")) throw new QaFailure("PACKAGED_ARTIFACT_REQUIRED");
        Path lib = jar.getParent();
        if (!"lib".equals(lib.getFileName().toString())) throw new QaFailure("PACKAGED_ARTIFACT_REQUIRED");
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        List<Path> files;
        try (var paths = Files.list(lib)) { files = new ArrayList<>(paths.sorted().toList()); }
        if (files.isEmpty() || files.size() > 300) throw new QaFailure("ARTIFACT_LAYOUT_INVALID");
        for (Path file : files) if (!file.toString().endsWith(".jar")) throw new QaFailure("ARTIFACT_LAYOUT_INVALID");
        files.add(lib.getParent().resolve("bin/run-attachment-contract-qa.sh"));
        files.add(lib.getParent().resolve("config/logback-qa.xml"));
        files.add(lib.getParent().resolve("config/logging.properties"));
        files.add(lib.getParent().resolve("config/hosts"));
        Path extractor = lib.getParent().resolve("extractor");
        if (Files.isSymbolicLink(extractor) || !Files.isDirectory(extractor)) throw new QaFailure("EXTRACTOR_ARTIFACT_MISSING");
        try (var paths = Files.walk(extractor)) {
            var entries = paths.sorted().toList();
            if (entries.size() > 100 || entries.stream().anyMatch(Files::isSymbolicLink)) throw new QaFailure("ARTIFACT_LAYOUT_INVALID");
            files.addAll(entries.stream().filter(Files::isRegularFile).toList());
        }
        for (Path file : files) {
            if (Files.isSymbolicLink(file) || !Files.isRegularFile(file)) throw new QaFailure("ARTIFACT_LAYOUT_INVALID");
            digest.update(lib.getParent().relativize(file).toString().replace('\\', '/').getBytes(StandardCharsets.UTF_8));
            digest.update((byte)0);
            try (var input = Files.newInputStream(file)) {
                byte[] buffer = new byte[65536]; int count;
                while ((count = input.read(buffer)) != -1) digest.update(buffer, 0, count);
            }
            digest.update((byte)0);
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    record CaseResult(String caseIdHash, String status) { }
    record SuiteResult(String suite, int discovered, int passed, int failed, int skipped, int notRun) { }
    record Result(String status, int discovered, int passed, int failed, int skipped, int notRun,
                  int failedContainers, List<SuiteResult> suites, List<CaseResult> cases) { }

    static final class Ledger implements TestExecutionListener {
        private final Map<String,String> suiteByTest = new TreeMap<>();
        private final Map<String,String> statusByTest = new HashMap<>();
        private final List<String> suites;
        private int failedContainers;
        Ledger(TestPlan plan, List<String> required) {
            suites = List.copyOf(required);
            Set<String> found = new HashSet<>();
            for (var root : plan.getRoots()) for (var item : plan.getDescendants(root)) {
                item.getSource().filter(ClassSource.class::isInstance).map(ClassSource.class::cast)
                        .map(ClassSource::getClassName).filter(required::contains).ifPresent(found::add);
                if (item.isTest()) {
                    var parent = item;
                    String suite = null;
                    while (parent != null) {
                        if (parent.getSource().orElse(null) instanceof ClassSource source && required.contains(source.getClassName())) {
                            suite = source.getClassName(); break;
                        }
                        parent = plan.getParent(parent).orElse(null);
                    }
                    if (suite == null || suiteByTest.put(item.getUniqueId(), suite) != null) throw new QaFailure("UNEXPECTED_TEST_PLAN");
                }
            }
            if (!found.equals(new HashSet<>(required)) || suiteByTest.isEmpty() || suiteByTest.size() > 10000
                    || required.stream().anyMatch(suite -> !suiteByTest.containsValue(suite))) throw new QaFailure("REQUIRED_SUITE_MISSING");
        }
        @Override public synchronized void executionSkipped(TestIdentifier test, String reason) {
            if (test.isTest()) record(test, "SKIPPED");
            else failedContainers++;
        }
        @Override public synchronized void executionFinished(TestIdentifier test, TestExecutionResult result) {
            if (test.isTest()) record(test, switch (result.getStatus()) {
                case SUCCESSFUL -> "PASSED"; case FAILED -> "FAILED"; case ABORTED -> "SKIPPED";
            });
            else if (result.getStatus() != TestExecutionResult.Status.SUCCESSFUL) failedContainers++;
        }
        @Override public synchronized void dynamicTestRegistered(TestIdentifier test) { failedContainers++; }
        private void record(TestIdentifier test, String status) {
            if (!suiteByTest.containsKey(test.getUniqueId()) || statusByTest.putIfAbsent(test.getUniqueId(), status) != null) failedContainers++;
        }
        synchronized Result selectResult(boolean inventory) {
            List<SuiteResult> result = new ArrayList<>();
            List<CaseResult> cases = new ArrayList<>();
            int passed = 0, failed = 0, skipped = 0, notRun = 0;
            for (String suite : suites) {
                int p = 0, f = 0, s = 0, n = 0;
                for (var item : suiteByTest.entrySet()) if (suite.equals(item.getValue())) {
                    String status = statusByTest.getOrDefault(item.getKey(), "NOT_RUN");
                    switch (status) { case "PASSED" -> p++; case "FAILED" -> f++; case "SKIPPED" -> s++; default -> n++; }
                    cases.add(new CaseResult(hashId(item.getKey()), status));
                }
                result.add(new SuiteResult(suite, p + f + s + n, p, f, s, n));
                passed += p; failed += f; skipped += s; notRun += n;
            }
            String status = inventory ? "INVENTORY_ONLY" : failed + skipped + notRun + failedContainers == 0 ? "PASSED" : "FAILED";
            return new Result(status, suiteByTest.size(), passed, failed, skipped, notRun, failedContainers, List.copyOf(result), List.copyOf(cases));
        }
        private String hashId(String id) {
            try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(id.getBytes(StandardCharsets.UTF_8))); }
            catch (Exception exception) { throw new QaFailure("CASE_HASH_FAILED"); }
        }
    }
    static final class QaFailure extends RuntimeException {
        final String code;
        QaFailure(String code) { super(code); this.code = code; }
    }
}
