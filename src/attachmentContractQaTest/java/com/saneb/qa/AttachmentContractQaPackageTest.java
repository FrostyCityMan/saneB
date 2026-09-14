package com.saneb.qa;

import static org.assertj.core.api.Assertions.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.JarFile;
import org.junit.jupiter.api.Test;

/** 설치 산출물의 정확한 코드·migration 포함 여부만 검증한다. DB 실행을 대신하지 않는다. */
class AttachmentContractQaPackageTest {
    private final Path root = Path.of("build/install/attachment-contract-qa");
    private Path selectJar() throws Exception {
        try (var files = Files.list(root.resolve("lib"))) {
            var jars = files.filter(path -> path.getFileName().toString().startsWith("saneb-attachment-contract-qa-")).toList();
            assertThat(jars).hasSize(1); return jars.getFirst();
        }
    }
    @Test void packagedInventoryVerifiesCurrentApplicationBytesButCannotClaimRuntimeOrExecution() throws Exception {
        String executable = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win") ? "java.exe" : "java";
        var process = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", executable).toString(), "-cp",
                root.resolve("lib").toAbsolutePath() + "/*", "com.saneb.qa.AttachmentContractQaMain", "--inventory")
                .redirectError(ProcessBuilder.Redirect.DISCARD).start();
        try (var readers = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            var output = readers.submit(() -> process.getInputStream().readNBytes(1048577));
            try {
                assertThat(process.waitFor(30, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
                assertThat(process.exitValue()).isZero();
                byte[] bytes = output.get(2, java.util.concurrent.TimeUnit.SECONDS);
                assertThat(bytes.length).isLessThanOrEqualTo(1048576);
                var value = new com.fasterxml.jackson.databind.ObjectMapper().readTree(bytes);
                assertThat(value.path("reportSchemaVersion").asInt()).isEqualTo(2);
                assertThat(value.path("scope").asText()).isEqualTo(AttachmentContractQaMain.SCOPE);
                assertThat(value.path("executionCodeHash").asText()).isEqualTo(new com.saneb.domain.announcementattachment.service.impl.AttachmentApplicationCodeFingerprint(
                        new com.fasterxml.jackson.databind.ObjectMapper()).selectVerifiedHash());
                assertThat(value.path("extractorRuntimeHash").isNull()).isTrue();
                assertThat(value.path("result").path("status").asText()).isEqualTo("INVENTORY_ONLY");
                assertThat(value.path("result").path("passed").asInt()).isZero();
                assertThat(value.path("result").path("discovered").asInt()).isPositive();
                assertThat(value.path("result").path("notRun").asInt()).isEqualTo(value.path("result").path("discovered").asInt());
            } finally {
                process.descendants().forEach(handle -> { if (handle.isAlive()) handle.destroyForcibly(); });
                if (process.isAlive()) process.destroyForcibly();
                process.waitFor(2, java.util.concurrent.TimeUnit.SECONDS);
                process.getInputStream().close(); output.cancel(true);
            }
        }
    }
    @Test void runnerAndEveryCurrentApplicationClassAndMigrationAreIncludedUnchanged() throws Exception {
        try (var jar = new JarFile(selectJar().toFile())) {
            assertThat(jar.getEntry("com/saneb/qa/AttachmentContractQaMain.class")).isNotNull();
            Path classes = Path.of("build/classes/java/main");
            try (var files = Files.walk(classes)) {
                for (Path file : files.filter(Files::isRegularFile).toList()) {
                    String name = classes.relativize(file).toString().replace('\\', '/');
                    assertThat(jar.getEntry(name)).as(name).isNotNull();
                    try (var content = jar.getInputStream(jar.getEntry(name))) { assertThat(content.readAllBytes()).as(name).isEqualTo(Files.readAllBytes(file)); }
                }
            }
            Path resources = Path.of("src/main/resources");
            try (var files = Files.walk(resources.resolve("db/migration"))) {
                for (Path file : files.filter(Files::isRegularFile).toList()) {
                    String name = resources.relativize(file).toString().replace('\\', '/');
                    assertThat(jar.getEntry(name)).as(name).isNotNull();
                    try (var content = jar.getInputStream(jar.getEntry(name))) { assertThat(content.readAllBytes()).as(name).isEqualTo(Files.readAllBytes(file)); }
                }
            }
        }
    }
    @Test void fixedSuiteInventoryCannotOmitDynamicallyRegisteredTestCases() throws Exception {
        for (String suite : AttachmentContractQaMain.SUITES) {
            for (var method : Class.forName(suite).getDeclaredMethods()) {
                for (var annotation : method.getDeclaredAnnotations()) {
                    assertThat(annotation.annotationType().getName()).as(suite + "." + method.getName())
                            .isNotIn("org.junit.jupiter.params.ParameterizedTest", "org.junit.jupiter.api.TestFactory",
                                    "org.junit.jupiter.api.TestTemplate", "org.junit.jupiter.api.RepeatedTest");
                }
            }
        }
    }
    @Test void onlyFixedDatabaseSuitesArePackagedAndStandaloneLauncherIsPresent() throws Exception {
        try (var jar = new JarFile(selectJar().toFile())) {
            var entries = jar.stream().map(java.util.zip.ZipEntry::getName).filter(name -> name.endsWith(".class")).toList();
            var tests = entries.stream().filter(name -> name.contains("Test")).toList();
            assertThat(tests).isNotEmpty();
            assertThat(tests).allSatisfy(name -> assertThat(AttachmentContractQaMain.SUITES)
                    .anyMatch(suite -> name.equals(suite.replace('.', '/') + ".class") || name.startsWith(suite.replace('.', '/') + "$")));
            for (String suite : AttachmentContractQaMain.SUITES) assertThat(entries).contains(suite.replace('.', '/') + ".class");
            for (String name : tests) try (var input = jar.getInputStream(jar.getEntry(name))) {
                assertThat(input.readAllBytes()).as(name).isEqualTo(Files.readAllBytes(Path.of("build/classes/java/test").resolve(name)));
            }
            assertThat(entries).noneMatch(name -> name.startsWith("org/junit/") || name.startsWith("io/zonky/") || name.startsWith("org/apache/pdfbox/"));
        }
        assertThat(root.resolve("bin/run-attachment-contract-qa.sh")).isRegularFile();
        assertThat(Files.readAllBytes(root.resolve("bin/run-attachment-contract-qa.sh"))).doesNotContain((byte)'\r');
        assertThat(root.resolve("config/logback-qa.xml")).isRegularFile();
        assertThat(Files.readString(root.resolve("config/hosts")).strip()).isEqualTo("127.0.0.1 localhost");
        assertThat(root.resolve("extractor/lib/attachment-extractor-1.0.0.jar")).isRegularFile();
        try (var files = Files.list(root.resolve("lib"))) {
            assertThat(files.map(path -> path.getFileName().toString()).toList()).anyMatch(name -> name.startsWith("junit-platform-launcher-"))
                    .anyMatch(name -> name.startsWith("embedded-postgres-binaries-linux-amd64-"));
        }
    }
    @Test void packagedExtractorAndSyntheticInputsExactlyMatchCurrentBuild() throws Exception {
        Path original = Path.of("attachment-extractor/build/install/attachment-extractor");
        Path packaged = root.resolve("extractor");
        List<Path> relative;
        try (var files = Files.walk(original)) {
            relative = files.filter(Files::isRegularFile).map(original::relativize).sorted().toList();
        }
        assertThat(relative).isNotEmpty();
        try (var files = Files.walk(packaged)) {
            assertThat(files.filter(Files::isRegularFile).map(packaged::relativize).sorted().toList()).isEqualTo(relative);
        }
        for (Path file : relative) assertThat(Files.readAllBytes(packaged.resolve(file))).as(file.toString())
                .isEqualTo(Files.readAllBytes(original.resolve(file)));
        Path fixtures = Path.of("attachment-extractor/build/generated/runtime-qa");
        try (var jar = new JarFile(selectJar().toFile()); var files = Files.walk(fixtures)) {
            for (Path file : files.filter(Files::isRegularFile).toList()) {
                String name = fixtures.relativize(file).toString().replace('\\', '/');
                assertThat(jar.getEntry(name)).as(name).isNotNull();
                try (var input = jar.getInputStream(jar.getEntry(name))) {
                    assertThat(input.readAllBytes()).as(name).isEqualTo(Files.readAllBytes(file));
                }
            }
        }
    }
    @Test void fullCodeCatalogMatchesEveryCurrentClassAndResourceInWebAndQaArtifacts() throws Exception {
        String name = "attachment-qa-code/catalog.json";
        byte[] current = Files.readAllBytes(Path.of("build/generated/attachment-qa-code").resolve(name));
        var catalog = new com.fasterxml.jackson.databind.ObjectMapper().readTree(current);
        assertThat(catalog.path("schemaVersion").asInt()).isEqualTo(1);
        var expected = new TreeSet<String>();
        for (Path directory : List.of(Path.of("build/classes/java/main"),Path.of("build/resources/main"))) {
            try (var files = Files.walk(directory)) {
                files.filter(Files::isRegularFile).forEach(file -> expected.add(directory.relativize(file).toString().replace('\\','/')));
            }
        }
        var actual = new TreeSet<String>();
        for (var entry : catalog.path("entries")) assertThat(actual.add(entry.path("path").asText())).isTrue();
        assertThat(actual).containsExactlyElementsOf(expected);
        Path web;
        try (var paths = Files.list(Path.of("build/libs"))) {
            web = paths.filter(path -> path.getFileName().toString().equals("saneB-0.0.1-SNAPSHOT.jar")).findFirst().orElseThrow();
        }
        try (var qa = new JarFile(selectJar().toFile()); var app = new JarFile(web.toFile())) {
            for (var jar : List.of(qa,app)) {
                String prefix = jar == app ? "BOOT-INF/classes/" : "";
                try (var input = jar.getInputStream(jar.getEntry(prefix+name))) { assertThat(input.readAllBytes()).isEqualTo(current); }
                for (var entry : catalog.path("entries")) {
                    String path = prefix+entry.path("path").asText();
                    assertThat(jar.getEntry(path)).as(path).isNotNull();
                    try (var input = jar.getInputStream(jar.getEntry(path))) {
                        byte[] bytes = input.readAllBytes();
                        assertThat((long)bytes.length).as(path).isEqualTo(entry.path("size").asLong());
                        assertThat(HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes)))
                                .as(path).isEqualTo(entry.path("sha256").asText());
                    }
                }
            }
        }
    }
}
