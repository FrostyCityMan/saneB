package com.saneb.qa;

import static org.assertj.core.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.*;
import org.junit.platform.launcher.core.*;

/** 실행기 판정 자체의 합성 테스트다. PostgreSQL 또는 Linux 성공으로 집계하지 않는다. */
class AttachmentContractQaMainTest {
    private record Fixture(Launcher launcher, TestPlan plan, AttachmentContractQaMain.Ledger ledger) { }
    private Fixture fixture(Class<?> type) {
        var launcher = LauncherFactory.create();
        var plan = launcher.discover(LauncherDiscoveryRequestBuilder.request()
                .selectors(DiscoverySelectors.selectClass(type)).build());
        return new Fixture(launcher, plan, new AttachmentContractQaMain.Ledger(plan, List.of(type.getName())));
    }
    @Test void realJUnitExecutionRequiresEveryDiscoveredCaseToFinish() {
        var fixture = fixture(SuccessCases.class);
        assertThat(fixture.ledger.selectResult(false).status()).isEqualTo("FAILED");
        assertThat(fixture.ledger.selectResult(false).notRun()).isEqualTo(2);
        fixture.launcher.execute(fixture.plan, fixture.ledger);
        var result = fixture.ledger.selectResult(false);
        assertThat(result.status()).isEqualTo("PASSED");
        assertThat(result.discovered()).isEqualTo(2);
        assertThat(result.passed()).isEqualTo(2);
        assertThat(result.cases()).allSatisfy(item -> assertThat(item.caseIdHash()).matches("[a-f0-9]{64}"));
    }
    @Test void inventoryCannotClaimExecutionSuccess() {
        var result = fixture(SuccessCases.class).ledger.selectResult(true);
        assertThat(result.status()).isEqualTo("INVENTORY_ONLY");
        assertThat(result.passed()).isZero();
        assertThat(result.notRun()).isEqualTo(2);
    }
    @Test void missingOrUnrelatedSuiteIsRejected() {
        var fixture = fixture(SuccessCases.class);
        assertThatThrownBy(() -> new AttachmentContractQaMain.Ledger(fixture.plan, List.of("missing")))
                .isInstanceOf(AttachmentContractQaMain.QaFailure.class);
        assertThatThrownBy(() -> new AttachmentContractQaMain.Ledger(fixture.plan, List.of(SuccessCases.class.getName(), "missing")))
                .isInstanceOf(AttachmentContractQaMain.QaFailure.class);
    }
    @Test void failedTestAndAbortedTestCannotPassOrLeakExceptionText() {
        var fixture = fixture(FailureCases.class);
        fixture.launcher.execute(fixture.plan, fixture.ledger);
        var result = fixture.ledger.selectResult(false);
        assertThat(result.status()).isEqualTo("FAILED");
        assertThat(result.failed()).isEqualTo(1);
        assertThat(result.skipped()).isEqualTo(1);
        assertThat(result.toString()).doesNotContain("PRIVATE_FIXTURE_MARKER");
    }
    @Test void disabledContainerCannotHideItsUnexecutedChildren() {
        var fixture = fixture(DisabledCases.class);
        fixture.launcher.execute(fixture.plan, fixture.ledger);
        assertThat(fixture.ledger.selectResult(false).status()).isEqualTo("FAILED");
        assertThat(fixture.ledger.selectResult(false).failedContainers()).isPositive();
        assertThat(fixture.ledger.selectResult(false).passed()).isZero();
    }
    @Test void beforeAllFailureIsNotMistakenForZeroSuccessfulTests() {
        var fixture = fixture(SetupFailureCases.class);
        fixture.launcher.execute(fixture.plan, fixture.ledger);
        var result = fixture.ledger.selectResult(false);
        assertThat(result.status()).isEqualTo("FAILED");
        assertThat(result.failedContainers()).isPositive();
        assertThat(result.notRun()).isEqualTo(1);
    }
    @Test void duplicateOrUnexpectedExecutionEventsCannotPass() {
        var fixture = fixture(SuccessCases.class);
        fixture.launcher.execute(fixture.plan, fixture.ledger);
        var test = fixture.plan.getRoots().stream().flatMap(root -> fixture.plan.getDescendants(root).stream())
                .filter(TestIdentifier::isTest).findFirst().orElseThrow();
        fixture.ledger.executionFinished(test, TestExecutionResult.successful());
        assertThat(fixture.ledger.selectResult(false).status()).isEqualTo("FAILED");
    }
    @Test void runtimeCannotAcceptWindowsOrUnownedWorkingDirectory() {
        var properties = new Properties();properties.setProperty("os.name", "Windows 11");
        assertThatThrownBy(() -> AttachmentContractQaMain.validateEnvironment(Map.of(), properties)).hasMessage("LINUX_REQUIRED");
        properties.setProperty("os.name", "Linux");
        assertThatThrownBy(() -> AttachmentContractQaMain.validateEnvironment(Map.of(), properties)).hasMessage("ISOLATED_WORK_DIRECTORY_REQUIRED");
    }
    @Test void operatingCredentialsAndConditionBypassAreRejectedBeforeProcessInspection() {
        var properties = new Properties();properties.setProperty("os.name", "Linux");
        properties.setProperty("java.io.tmpdir", "/work/tmp");properties.setProperty("user.dir", "/work");
        var env = new HashMap<>(Map.of("HOME", "/work", "PWD", "/work", "TMPDIR", "/work/tmp", "SANEB_ATTACHMENT_JOB_TEST", "true", "SANEB_ATTACHMENT_MIGRATION_TEST", "true", "SANEB_ATTACHMENT_WORKER_QA", "true"));
        env.put("DB_URL", "PRIVATE_FIXTURE_MARKER");
        assertThatThrownBy(() -> AttachmentContractQaMain.validateEnvironment(env, properties)).hasMessage("CLEAN_ENVIRONMENT_REQUIRED");
        env.remove("DB_URL");properties.setProperty("junit.jupiter.conditions.deactivate", "*");
        assertThatThrownBy(() -> AttachmentContractQaMain.validateEnvironment(env, properties)).hasMessage("EXTERNAL_CONFIGURATION_NOT_ALLOWED");
    }
    @Test void bubblewrapWorkingDirectoryMustMatchTheIsolatedDirectoryExactly() {
        var properties = new Properties();properties.setProperty("os.name", "Linux");
        properties.setProperty("java.io.tmpdir", "/work/tmp");properties.setProperty("user.dir", "/work");
        // 다음 검증에서 중단하여 Windows 단위 시험이 Linux /proc 성공을 주장하지 않게 한다.
        properties.setProperty("junit.jupiter.conditions.deactivate", "*");
        var env = new HashMap<>(Map.of("HOME", "/work", "PWD", "/work", "TMPDIR", "/work/tmp", "SANEB_ATTACHMENT_JOB_TEST", "true", "SANEB_ATTACHMENT_MIGRATION_TEST", "true", "SANEB_ATTACHMENT_WORKER_QA", "true"));
        assertThatThrownBy(() -> AttachmentContractQaMain.validateEnvironment(env, properties)).hasMessage("EXTERNAL_CONFIGURATION_NOT_ALLOWED");
        for (String invalid : List.of("/home/runner/work", "/work/tmp", "/work/../work", "")) {
            env.put("PWD", invalid);
            assertThatThrownBy(() -> AttachmentContractQaMain.validateEnvironment(env, properties)).hasMessage("CLEAN_ENVIRONMENT_REQUIRED");
        }
        env.remove("PWD");
        assertThatThrownBy(() -> AttachmentContractQaMain.validateEnvironment(env, properties)).hasMessage("CLEAN_ENVIRONMENT_REQUIRED");
    }

    static class SuccessCases { @Test void one() { } @Test void two() { } }
    static class FailureCases {
        @Test void failed() { throw new AssertionError("PRIVATE_FIXTURE_MARKER"); }
        @Test void aborted() { Assumptions.assumeTrue(false, "PRIVATE_FIXTURE_MARKER"); }
    }
    @Disabled static class DisabledCases { @Test void one() { } }
    static class SetupFailureCases {
        @BeforeAll static void setup() { throw new IllegalStateException("PRIVATE_FIXTURE_MARKER"); }
        @Test void one() { }
    }
}
