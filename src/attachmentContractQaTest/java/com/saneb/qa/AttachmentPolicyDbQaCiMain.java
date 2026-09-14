package com.saneb.qa;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.engine.JupiterTestEngine;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.core.LauncherConfig;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

/** 임시 CI 계정 전용. Gradle의 같은 UID 스레드 점유 없이 실제 부모 JUnit 2사례를 실행한다. */
public final class AttachmentPolicyDbQaCiMain {
    static final String SUITE="com.saneb.domain.announcementattachment.service.impl.AttachmentWorkerDbQaLinuxIntegrationTest";
    private AttachmentPolicyDbQaCiMain() { }
    public static void main(String[] args) {
        int exit=1;
        try {
            if(args.length!=0 || !"Linux".equals(System.getProperty("os.name"))
                    || !"true".equals(System.getenv("SANEB_ATTACHMENT_POLICY_DB_QA"))
                    || !"saneb-policy-qa".equals(System.getProperty("user.name")))throw new IllegalArgumentException();
            var cwd=Path.of("").toRealPath();var parent=cwd.getParent();
            if(!"source".equals(cwd.getFileName().toString()) || parent==null || !parent.getFileName().toString().startsWith("saneb-policy-parent-qa.")
                    || !Path.of("/tmp").equals(parent.getParent()))throw new IllegalArgumentException();
            var uid=Files.readAllLines(Path.of("/proc/self/status")).stream().filter(line->line.startsWith("Uid:")).findFirst().orElseThrow();
            if(java.util.Arrays.stream(uid.substring(4).strip().split("\\s+")).anyMatch("0"::equals))throw new IllegalArgumentException();
            var result=selectRun(SUITE);
            if(result.discovered()!=2)throw new IllegalStateException();
            var target=cwd.resolve("build/test-results/attachmentPolicyDbQaIntegrationTest");Files.createDirectories(target);
            Files.writeString(target.resolve("TEST-"+SUITE+".xml"),selectXml(SUITE,result),StandardOpenOption.CREATE_NEW);
            System.out.println("POLICY_DB_QA_DIRECT_RESULT="+result.status());
            exit="PASSED".equals(result.status())?0:1;
        } catch(Exception exception) { System.err.println("POLICY_DB_QA_DIRECT_EXECUTION_FAILED"); }
        System.exit(exit);
    }
    static AttachmentContractQaMain.Result selectRun(String suite) {
        var request=LauncherDiscoveryRequestBuilder.request().selectors(DiscoverySelectors.selectClass(suite))
                .configurationParameter("junit.jupiter.execution.parallel.enabled","false")
                .configurationParameter("junit.jupiter.extensions.autodetection.enabled","false").build();
        var config=LauncherConfig.builder().enableTestEngineAutoRegistration(false).enableTestExecutionListenerAutoRegistration(false)
                .enableLauncherSessionListenerAutoRegistration(false).enableLauncherDiscoveryListenerAutoRegistration(false)
                .enablePostDiscoveryFilterAutoRegistration(false).addTestEngines(new JupiterTestEngine()).build();
        try(var session=LauncherFactory.openSession(config)) {
            var plan=session.getLauncher().discover(request);
            var ledger=new AttachmentContractQaMain.Ledger(plan,List.of(suite));
            session.getLauncher().execute(plan,ledger,new TestExecutionListener() {
                @Override public void executionFinished(TestIdentifier test,TestExecutionResult result) {
                    if(result.getStatus()==TestExecutionResult.Status.FAILED)
                        System.out.println("POLICY_DB_QA_FAILURE_CODE="+selectFailureCode(result.getThrowable().orElse(null)));
                }
            });
            return ledger.selectResult(false);
        }
    }
    static String selectFailureCode(Throwable error) {
        for(int depth=0;error!=null && depth<10;depth++,error=error.getCause())
            if(error.getMessage()!=null && Set.of("QA_CHILD_PROCESS_LIMIT","QA_PROCESS_START_FAILED","QA_REPORT_PARSE_FAILED","QA_OUTPUT_TIMEOUT",
                    "QA_PROCESS_IO_FAILED","QA_PROCESS_PERMISSION_FAILED","QA_PROCESS_FAILED","QA_EXECUTION_FAILED","QA_CHILD_FAILED","EXECUTION_STOPPED").contains(error.getMessage()))
                return error.getMessage();
        return "TEST_FAILED";
    }
    static String selectXml(String suite,AttachmentContractQaMain.Result result) throws Exception {
        if(suite==null || !suite.matches("[A-Za-z0-9_.$]+") || result.suites().size()!=1 || !suite.equals(result.suites().getFirst().suite()))throw new IllegalArgumentException();
        var output=new java.io.StringWriter();var xml=javax.xml.stream.XMLOutputFactory.newFactory().createXMLStreamWriter(output);
        xml.writeStartDocument("UTF-8","1.0");xml.writeStartElement("testsuite");xml.writeAttribute("name",suite);
        xml.writeAttribute("tests",String.valueOf(result.discovered()));xml.writeAttribute("failures",String.valueOf(result.failed()));
        xml.writeAttribute("errors",String.valueOf(result.failedContainers()+result.notRun()));xml.writeAttribute("skipped",String.valueOf(result.skipped()));
        for(var item:result.cases()) {
            xml.writeStartElement("testcase");xml.writeAttribute("name",item.caseIdHash());xml.writeAttribute("classname",suite);
            if(!"PASSED".equals(item.status())) {
                xml.writeEmptyElement("SKIPPED".equals(item.status())?"skipped":"NOT_RUN".equals(item.status())?"error":"failure");
                xml.writeAttribute("message",item.status());
            }
            xml.writeEndElement();
        }
        xml.writeEndElement();xml.writeEndDocument();xml.close();return output.toString();
    }
}
