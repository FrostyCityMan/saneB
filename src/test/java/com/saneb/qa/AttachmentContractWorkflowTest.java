package com.saneb.qa;

import static org.assertj.core.api.Assertions.assertThat;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

/** workflow 구조 검증이며 원격 Actions 또는 Linux PostgreSQL 실행 결과가 아니다. */
class AttachmentContractWorkflowTest {
    @Test void yangpyeongObservationIsOptInBoundedAndUploadsOnlyNamedMetadata() throws Exception {
        var flow=workflow();
        var inputs=(Map<?,?>)((Map<?,?>)((Map<?,?>)flow.get("on")).get("workflow_dispatch")).get("inputs");
        assertThat(((Map<?,?>)inputs.get("observe-yangpyeong-official-files")).get("default")).isEqualTo(false);
        var all=steps(job(flow)).stream().map(item->(Map<?,?>)item).toList();
        var observation=all.stream().filter(item->"yangpyeong-official-observation".equals(item.get("id"))).findFirst().orElseThrow();
        assertThat(observation.get("if")).isEqualTo("${{ !cancelled() && steps.contracts.outcome == 'success' && ((github.event_name == 'workflow_dispatch' && inputs.observe-yangpyeong-official-files == true) || (github.event_name == 'push' && contains(github.event.head_commit.message, '[yangpyeong-observation]'))) }}");
        assertThat(observation.get("run")).isEqualTo("bash ./gradlew attachmentBbsOfficialFileObservation -PsanebBbsObservationGroup=YANGPYEONG --no-daemon --console=plain --max-workers=1");
        var artifact=all.stream().filter(item->"양평 세 단계 관측 metadata 보관 — 원문 없음".equals(item.get("name"))).findFirst().orElseThrow();
        assertThat(((String)((Map<?,?>)artifact.get("with")).get("path")).lines().toList()).containsExactly(
                "build/reports/attachment-bbs-official-observation/YANGPYEONG-312241.json",
                "build/reports/attachment-bbs-official-observation/YANGPYEONG-311846.json",
                "build/reports/attachment-bbs-official-observation/YANGPYEONG-311507.json",
                "build/test-results/attachmentBbsOfficialFileObservation/TEST-*.xml");
        assertThat(Files.readString(Path.of("build.gradle"))).contains("providers.gradleProperty('sanebBbsObservationGroup').getOrElse('TAEBAEK')");
    }
    private Map<?,?> workflow() throws IOException {
        return new Yaml(new SafeConstructor(new LoaderOptions())).load(
                Files.readString(Path.of(".github/workflows/attachment-contract-qa.yml")));
    }
    private Map<?,?> job(Map<?,?> workflow) {
        return (Map<?,?>)((Map<?,?>)workflow.get("jobs")).get("contracts");
    }
    private List<?> steps(Map<?,?> job) { return (List<?>)job.get("steps"); }
    @Test void scopedQaBranchAndManualWorkflowCannotDeployOrUseOperatingCredentials() throws Exception {
        var flow=workflow();
        assertThat(((Map<?,?>)flow.get("on")).size()).isEqualTo(2);
        assertThat(((Map<?,?>)flow.get("on")).containsKey("workflow_dispatch")).isTrue();
        assertThat(((Map<?,?>)flow.get("on")).get("push"))
                .isEqualTo(Map.of("branches", List.of("codex/attachment-three-stage-linux-qa")));
        assertThat(flow.get("permissions")).isEqualTo(Map.of("contents","read"));
        assertThat(((Map<?,?>)flow.get("jobs")).size()).isEqualTo(1);
        assertThat(job(flow).get("timeout-minutes")).isEqualTo(30);
        String raw=Files.readString(Path.of(".github/workflows/attachment-contract-qa.yml"));
        assertThat(raw).doesNotContain("secrets.","id-token", "aws-actions/", "aws ", "DB_URL", "environment:", "workflow_call:");
    }
    @Test void runsActualIsolatedDatabaseTasksWithoutEnablingExternalProviders() throws Exception {
        var job=job(workflow());
        assertThat(job.get("runs-on")).isEqualTo("ubuntu-22.04");
        var environment=(Map<?,?>)job.get("env");
        for(String name:List.of("SANEB_ATTACHMENT_REAL_FILE_QA","SANEB_ATTACHMENT_RULE_EXPORT","SANEB_FLYWAY_INTEGRATION",
                "SANEB_ANNOUNCEMENT_ATTACHMENT_WORKER_ENABLED")) assertThat(environment.get(name)).isEqualTo("false");
        var build=steps(job).stream().map(item -> (Map<?,?>)item)
                .map(item -> String.valueOf(item.get("run"))).filter(run -> run.contains("bash ./gradlew")).findFirst().orElseThrow();
        assertThat(build).contains(":test ", ":attachment-extractor:test", "attachmentJobIntegrationTest", "attachmentMigrationTest",
                "attachmentRuntimeIntegrationTest", "attachmentWorkerIntegrationTest", "bootJar", ":attachment-extractor:installDist", "--rerun-tasks", "--no-daemon", "--max-workers=1", "--continue", "set -euo pipefail")
                .doesNotContain("|| true", "-x ", "attachmentRealFileQa");
    }
    @Test void actualIsolationToolsAreRequiredWithoutRelaxingRunnerSecurity() throws Exception {
        String raw=Files.readString(Path.of(".github/workflows/attachment-contract-qa.yml"));
        assertThat(raw).contains("sudo apt-get install --no-install-recommends -y bubblewrap", "test -x /usr/bin/prlimit",
                "build/test-results/attachmentRuntimeIntegrationTest/TEST-*.xml");
        assertThat(raw).doesNotContain("sysctl", "--privileged", "apparmor", "--share-net", "continue-on-error");
    }
    @Test void missingOrSkippedRequiredReportsCannotBeIgnoredAndOnlyTestXmlIsUploaded() throws Exception {
        var steps=steps(job(workflow())).stream().map(item -> (Map<?,?>)item).toList();
        var report=steps.stream().filter(item -> "node scripts/qa/attachment-contract-report.mjs".equals(item.get("run"))).findFirst().orElseThrow();
        assertThat(report.get("if")).isEqualTo("always()");
        assertThat(report.containsKey("continue-on-error")).isFalse();
        var artifact=steps.stream().filter(item -> "검증 결과 보관".equals(item.get("name"))).findFirst().orElseThrow();
        var options=(Map<?,?>)artifact.get("with");
        assertThat(options.get("if-no-files-found")).isEqualTo("error");
        assertThat(options.get("retention-days")).isEqualTo(7);
        assertThat(((String)options.get("path")).lines().toList()).allSatisfy(path -> assertThat(path).endsWith("/TEST-*.xml"));
    }
    @Test void officialObservationIsExplicitlyOptedInAndNeverInNormalTestsOrRawArtifactUploads() throws Exception {
        var flow=workflow();
        var inputs=(Map<?,?>)((Map<?,?>)((Map<?,?>)flow.get("on")).get("workflow_dispatch")).get("inputs");
        assertThat(inputs.get("observe-official-files")).isEqualTo(Map.of("description","고정 기업마당 3공고 전체 파일 관측 (기대값 승인·배포 없음)","type","boolean","default",false));
        var all=steps(job(flow)).stream().map(item -> (Map<?,?>)item).toList();
        var observation=all.stream().filter(item -> "official-observation".equals(item.get("id"))).findFirst().orElseThrow();
        assertThat(observation.get("if")).isEqualTo("${{ !cancelled() && steps.contracts.outcome == 'success' && ((github.event_name == 'workflow_dispatch' && inputs.observe-official-files == true) || (github.event_name == 'push' && contains(github.event.head_commit.message, '[official-file-observation]'))) }}");
        assertThat(observation.get("run")).isEqualTo("bash ./gradlew attachmentOfficialFileObservation --no-daemon --console=plain --max-workers=1");
        assertThat(((Map<?,?>)job(flow).get("env")).get("SANEB_ATTACHMENT_OFFICIAL_OBSERVATION")).isEqualTo("false");
        var artifact=all.stream().filter(item -> "공식 파일 관측 metadata 보관 — 원문 없음".equals(item.get("name"))).findFirst().orElseThrow();
        assertThat(((String)((Map<?,?>)artifact.get("with")).get("path")).lines().toList()).containsExactly(
                "build/reports/attachment-official-observation/BIZINFO-SEMAS-2026.json",
                "build/reports/attachment-official-observation/BIZINFO-ANYANG-2026.json",
                "build/reports/attachment-official-observation/BIZINFO-SDM-2026.json",
                "build/test-results/attachmentOfficialFileObservation/TEST-*.xml");
        assertThat(Files.readString(Path.of("build.gradle"))).contains("environment 'SANEB_ATTACHMENT_OFFICIAL_OBSERVATION', 'false'");
    }
    @Test void includesPolicyUiAndIndependentContractRuntimeInsteadOfOnlyPackaging() throws Exception {
        String raw=Files.readString(Path.of(".github/workflows/attachment-contract-qa.yml"));
        assertThat(raw).contains("scripts/qa/attachment-policy-ui.test.mjs", "scripts/qa/attachment-queue-ui.test.mjs", "attachmentContractQaTest", "installAttachmentContractQa",
                "run: bash build/install/attachment-contract-qa/bin/run-attachment-contract-qa.sh");
        String runner=Files.readString(Path.of("scripts/qa/run-attachment-contract-qa.sh"));
        assertThat(runner).contains("--unshare-all", "--die-with-parent", "--new-session", "--cap-drop ALL", "env -i",
                "--kill-after=5 600", "--clearenv", "--setenv SANEB_ATTACHMENT_JOB_TEST true", "--setenv SANEB_ATTACHMENT_MIGRATION_TEST true");
        assertThat(runner).doesNotContain("--share-net", "--privileged", "--ro-bind / /", "source /", "sudo ");
    }
    @Test void parentDiagnosisRunsAfterArtifactSuccessEvenWhenStandaloneFailedButNeverAfterCancellation() throws Exception {
        var all=steps(job(workflow())).stream().map(item -> (Map<?,?>)item).toList();
        var build=all.stream().filter(item -> "contracts".equals(item.get("id"))).findFirst().orElseThrow();
        assertThat(String.valueOf(build.get("run"))).contains("installAttachmentContractQa", "--continue");
        var parent=all.stream().filter(item -> "bash scripts/qa/run-policy-db-qa-isolated-user.sh".equals(item.get("run")))
                .findFirst().orElseThrow();
        assertThat(parent.get("if")).isEqualTo("${{ !cancelled() && steps.contracts.outcome == 'success' }}");
        assertThat(parent.containsKey("continue-on-error")).isFalse();
        String runner=Files.readString(Path.of("scripts/qa/run-policy-db-qa-isolated-user.sh"));
        assertThat(runner).contains("-Dorg.gradle.jvmargs=-Xmx512m -XX:ActiveProcessorCount=1 -XX:+UseSerialGC");
        String buildScript=Files.readString(Path.of("build.gradle"));
        String parentTask=buildScript.substring(buildScript.indexOf("tasks.register('attachmentPolicyDbQaIntegrationTest'"),buildScript.indexOf("\ndependencies {"));
        assertThat(parentTask).contains("maxParallelForks = 1", "maxHeapSize = '256m'", "jvmArgs '-XX:ActiveProcessorCount=1', '-XX:+UseSerialGC'");
        assertThat(Files.readString(Path.of("src/main/java/com/saneb/domain/announcementattachment/service/impl/AttachmentWorkerDbQaProcess.java")))
                .contains("--nproc=128", "--as=2147483648");
    }
    @Test void dedicatedUidUsesOnlyCommittedSourcesCleanEnvironmentAndOwnedCleanup() throws Exception {
        String runner=Files.readString(Path.of("scripts/qa/run-policy-db-qa-isolated-user.sh"));
        assertThat(runner).contains("${GITHUB_ACTIONS:-}", "${GITHUB_SHA:-}", "ACCOUNT_ALREADY_EXISTS",
                "useradd --system --user-group --no-create-home --shell /usr/sbin/nologin",
                "git archive --format=tar HEAD", "env -i PATH=", "--kill-after=5 900",
                "pkill -TERM -u \"$qa_uid\"", "pkill -KILL -u \"$qa_uid\"",
                "\"$qa_uid\" != \"$qa_runner_uid\"", "\"$resolved\" == \"$qa_work\"",
                "saneb-policy-parent-qa.*", "userdel \"$qa_account\"", "POLICY_DB_QA_CLEANUP=SUCCEEDED",
                "REQUIRED_REPORT_MISSING", "! sudo -n test -L \"$qa_report\"");
        assertThat(runner).contains("JAVA_OPTS='-XX:ActiveProcessorCount=1 -XX:+UseSerialGC'",
                "prepareAttachmentPolicyDbQaCi", "com.saneb.qa.AttachmentPolicyDbQaCiMain", "export SANEB_ATTACHMENT_POLICY_DB_QA=true",
                "exec \"$JAVA_HOME/bin/java\" -Xmx256m -XX:ActiveProcessorCount=1 -XX:+UseSerialGC");
        assertThat(runner.indexOf("prepareAttachmentPolicyDbQaCi")).isLessThan(runner.indexOf("exec \"$JAVA_HOME/bin/java\""));
        assertThat(runner).doesNotContain("exec /bin/bash ./gradlew");
        assertThat(runner).doesNotContain("--preserve-env", "sudo -E", "chmod -R", "chown -R",
                "--nproc=", "DB_URL=", "GITHUB_TOKEN=", "cp -r", "userdel -r", "|| true");
    }
}
