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
        var artifact=steps.stream().filter(item -> "actions/upload-artifact@v4".equals(item.get("uses"))).findFirst().orElseThrow();
        var options=(Map<?,?>)artifact.get("with");
        assertThat(options.get("if-no-files-found")).isEqualTo("error");
        assertThat(options.get("retention-days")).isEqualTo(7);
        assertThat(((String)options.get("path")).lines().toList()).allSatisfy(path -> assertThat(path).endsWith("/TEST-*.xml"));
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
        var parent=all.stream().filter(item -> String.valueOf(item.get("run")).contains("bash ./gradlew attachmentPolicyDbQaIntegrationTest"))
                .findFirst().orElseThrow();
        assertThat(parent.get("if")).isEqualTo("${{ !cancelled() && steps.contracts.outcome == 'success' }}");
        assertThat(parent.containsKey("continue-on-error")).isFalse();
    }
}
