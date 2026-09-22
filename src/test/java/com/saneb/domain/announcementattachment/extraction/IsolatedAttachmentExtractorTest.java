package com.saneb.domain.announcementattachment.extraction;

import static org.assertj.core.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.nio.file.Files;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

class IsolatedAttachmentExtractorTest {
    @Test void syntheticResourceFaultProbeCompilesWithoutApplicationClasses(@TempDir Path root) throws Exception {
        Path distribution=AttachmentRuntimeGateIntegrationTest.selectFaultDistribution(root);
        try(var archive=new java.util.jar.JarFile(distribution.resolve("lib/canary.jar").toFile())) {
            assertThat(archive.stream().map(java.util.jar.JarEntry::getName).toList())
                    .containsExactly("com/saneb/extractor/AttachmentExtractorMain.class");
        }
    }
    @Test void syntheticSandboxProbeCompilesWithoutProductionOrThirdPartyClasses(@TempDir Path root) throws Exception {
        Path distribution=AttachmentRuntimeGateIntegrationTest.selectCanaryDistribution(root);
        try(var archive=new java.util.jar.JarFile(distribution.resolve("lib/canary.jar").toFile())) {
            assertThat(archive.stream().map(java.util.jar.JarEntry::getName).toList())
                    .containsExactly("com/saneb/extractor/AttachmentExtractorMain.class");
        }
    }
    @Test void processCommandRequiresNetworkPidFilesystemAndMemoryIsolation() throws Exception {
        var command=IsolatedAttachmentExtractor.selectCommand(Path.of("/trusted/jre"),Path.of("/trusted/lib"),Path.of("/owned/input.bin"));
        assertThat(command).contains("/usr/bin/prlimit","--as=536870912","--cpu=30","--fsize=8388608",
                "/usr/bin/bwrap","--unshare-all","--clearenv","--die-with-parent","--cap-drop","ALL",
                "-Xmx128m","com.saneb.extractor.AttachmentExtractorMain");
        assertThat(command).doesNotContain("-Xmx256m");
        assertThat(command).containsSubsequence("--setenv","MALLOC_ARENA_MAX","1");
        assertThat(command).doesNotContain("--share-net","--bind","/home","/var","/etc","sh","-c");
    }
    @Test @EnabledOnOs(OS.LINUX)
    void onlyResolvedJavaSecurityFileIsMountedOutsideJavaHome(@TempDir Path root) throws Exception {
        Path javaHome=Files.createDirectories(root.resolve("jre"));
        Path security=Files.createDirectories(root.resolve("jdk-config")).resolve("java.security");
        Files.writeString(security,"security.provider.1=SUN\n");
        Path link=Files.createDirectories(javaHome.resolve("conf/security")).resolve("java.security");
        Files.createSymbolicLink(link,security);
        var command=IsolatedAttachmentExtractor.selectCommand(javaHome,root.resolve("lib"),root.resolve("input.bin"));
        assertThat(command).containsSubsequence("--ro-bind",security.toRealPath().toString(),security.toRealPath().toString());
        assertThat(command).doesNotContain(root.resolve("jdk-config").toString(),"/etc","--bind","--share-net");
    }
    @Test @EnabledOnOs(OS.WINDOWS)
    void unsupportedWindowsIsolationDoesNotSilentlyRunParserOnHost() throws Exception {
        var result=new IsolatedAttachmentExtractor(new ObjectMapper(),"missing-extractor")
                .selectExtraction(Path.of("missing-input.bin"));
        assertThat(result.path("errorCode").asText()).isEqualTo("ISOLATION_UNAVAILABLE");
    }
}
