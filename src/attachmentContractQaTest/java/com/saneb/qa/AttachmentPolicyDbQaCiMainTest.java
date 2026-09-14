package com.saneb.qa;

import java.io.StringReader;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.*;
import org.xml.sax.InputSource;
import static org.assertj.core.api.Assertions.*;

class AttachmentPolicyDbQaCiMainTest {
    @Test void directJUnitRunsAllCasesAndProducesExactSuiteReport() throws Exception {
        var result=AttachmentPolicyDbQaCiMain.selectRun(Success.class.getName());
        assertThat(result.status()).isEqualTo("PASSED");assertThat(result.passed()).isEqualTo(2);
        var document=parse(AttachmentPolicyDbQaCiMain.selectXml(Success.class.getName(),result));
        var root=document.getDocumentElement();assertThat(root.getAttribute("name")).isEqualTo(Success.class.getName());
        assertThat(root.getAttribute("tests")).isEqualTo("2");assertThat(root.getAttribute("failures")).isEqualTo("0");
        assertThat(root.getAttribute("errors")).isEqualTo("0");assertThat(root.getAttribute("skipped")).isEqualTo("0");
        assertThat(document.getElementsByTagName("testcase").getLength()).isEqualTo(2);
    }
    @Test void failuresAndAbortsCannotBecomePassedOrLeakExceptionText() throws Exception {
        var result=AttachmentPolicyDbQaCiMain.selectRun(Failures.class.getName());
        assertThat(result.status()).isEqualTo("FAILED");assertThat(result.failed()).isEqualTo(1);assertThat(result.skipped()).isEqualTo(1);
        var xml=AttachmentPolicyDbQaCiMain.selectXml(Failures.class.getName(),result);assertThat(xml).doesNotContain("PRIVATE_CANARY");
        var document=parse(xml);assertThat(document.getDocumentElement().getAttribute("failures")).isEqualTo("1");
        assertThat(document.getDocumentElement().getAttribute("skipped")).isEqualTo("1");
    }
    @Test void disabledAndSetupFailuresRetainNotRunAndContainerErrors() throws Exception {
        for(var suite:java.util.List.of(DisabledSuite.class,SetupFailure.class)) {
            var result=AttachmentPolicyDbQaCiMain.selectRun(suite.getName());
            assertThat(result.status()).isEqualTo("FAILED");assertThat(result.passed()).isZero();assertThat(result.failedContainers()).isPositive();
            assertThat(parse(AttachmentPolicyDbQaCiMain.selectXml(suite.getName(),result)).getDocumentElement().getAttribute("errors")).isNotEqualTo("0");
        }
    }
    @Test void reportingRejectsDifferentSuiteAndWhitelistsOnlyFixedFailureCodes() throws Exception {
        var result=AttachmentPolicyDbQaCiMain.selectRun(Success.class.getName());
        assertThatIllegalArgumentException().isThrownBy(()->AttachmentPolicyDbQaCiMain.selectXml("other",result));
        assertThat(AttachmentPolicyDbQaCiMain.selectFailureCode(new RuntimeException("PRIVATE_CANARY"))).isEqualTo("TEST_FAILED");
        assertThat(AttachmentPolicyDbQaCiMain.selectFailureCode(new RuntimeException("outer",new RuntimeException("QA_CHILD_PROCESS_LIMIT")))).isEqualTo("QA_CHILD_PROCESS_LIMIT");
    }
    private static org.w3c.dom.Document parse(String xml) throws Exception {
        var factory=DocumentBuilderFactory.newInstance();factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
        return factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
    }
    static class Success {@Test void one() { } @Test void two() { }}
    static class Failures {@Test void fail(){throw new IllegalStateException("PRIVATE_CANARY");} @Test void abort(){Assumptions.assumeTrue(false,"PRIVATE_CANARY");}}
    @Disabled static class DisabledSuite {@Test void unrun() { }}
    static class SetupFailure {@BeforeAll static void start(){throw new IllegalStateException("PRIVATE_CANARY");} @Test void unrun() { }}
}
