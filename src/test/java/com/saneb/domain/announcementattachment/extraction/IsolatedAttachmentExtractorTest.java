package com.saneb.domain.announcementattachment.extraction;

import static org.assertj.core.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

class IsolatedAttachmentExtractorTest {
    @Test void processCommandRequiresNetworkPidFilesystemAndMemoryIsolation() {
        var command=IsolatedAttachmentExtractor.selectCommand(Path.of("/trusted/jre"),Path.of("/trusted/lib"),Path.of("/owned/input.bin"));
        assertThat(command).contains("/usr/bin/prlimit","--as=536870912","--cpu=30","--fsize=8388608",
                "/usr/bin/bwrap","--unshare-all","--clearenv","--die-with-parent","--cap-drop","ALL",
                "-Xmx128m","com.saneb.extractor.AttachmentExtractorMain");
        assertThat(command).doesNotContain("-Xmx256m");
        assertThat(command).doesNotContain("--share-net","--bind","/home","/var","/etc","sh","-c");
    }
    @Test @EnabledOnOs(OS.WINDOWS)
    void unsupportedWindowsIsolationDoesNotSilentlyRunParserOnHost() throws Exception {
        var result=new IsolatedAttachmentExtractor(new ObjectMapper(),"missing-extractor")
                .selectExtraction(Path.of("missing-input.bin"));
        assertThat(result.path("errorCode").asText()).isEqualTo("ISOLATION_UNAVAILABLE");
    }
}
