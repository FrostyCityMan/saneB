package com.saneb.domain.announcementattachment.extraction;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** 실제 설치된 격리 실행 코드/JDK/파서 JAR을 고정한다. QA 성공 여부를 hash만으로 추정하지 않는다. */
@Component
public final class AttachmentRuntimeIdentity {
    public static final String EXTRACTOR_VERSION = "1.0.11";
    private static final long MAX_LIBRARY_BYTES = 256L * 1024 * 1024;
    private final Path distribution;
    public AttachmentRuntimeIdentity(@Value("${saneb.announcement-attachment.extractor-root:/opt/saneb/attachment-extractor}") String root) {
        distribution = Path.of(root).toAbsolutePath().normalize();
    }
    public record Identity(String extractorVersion, String configHash, int libraryCount, long libraryBytes) { }

    public Identity selectIdentity() throws IOException {
        if (!System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("linux")
                || !Files.isExecutable(Path.of("/usr/bin/bwrap")) || !Files.isExecutable(Path.of("/usr/bin/prlimit")))
            throw new IOException("ISOLATION_UNAVAILABLE");
        Path root = distribution.toRealPath();
        Path library = root.resolve("lib").toRealPath();
        if (!library.getParent().equals(root)) throw new IOException("EXTRACTOR_LIBRARY_UNSAFE");
        List<Path> jars;
        try (var files = Files.list(library)) {
            jars = files.filter(file -> file.getFileName().toString().endsWith(".jar"))
                    .sorted(Comparator.comparing(file -> file.getFileName().toString())).toList();
        }
        if (jars.isEmpty() || jars.size() > 64) throw new IOException("EXTRACTOR_LIBRARY_INVALID");
        var digest = selectDigest();
        savePart(digest, "schema", "1");
        savePart(digest, "extractorVersion", EXTRACTOR_VERSION);
        savePart(digest, "javaRuntime", System.getProperty("java.runtime.version"));
        long total = 0;
        for (Path jar : jars) {
            if (!Files.isRegularFile(jar, LinkOption.NOFOLLOW_LINKS) || !jar.toRealPath().getParent().equals(library))
                throw new IOException("EXTRACTOR_LIBRARY_UNSAFE");
            total += Files.size(jar);
            if (total > MAX_LIBRARY_BYTES) throw new IOException("EXTRACTOR_LIBRARY_LIMIT");
            savePart(digest, jar.getFileName().toString(), selectFileHash(jar, 64L * 1024 * 1024));
        }
        Path javaHome = Path.of(System.getProperty("java.home")).toRealPath();
        savePart(digest, "java", selectFileHash(javaHome.resolve("bin/java"), 64L * 1024 * 1024));
        savePart(digest, "java.security", selectFileHash(javaHome.resolve("conf/security/java.security").toRealPath(), 4L * 1024 * 1024));
        savePart(digest, "bwrap", selectFileHash(Path.of("/usr/bin/bwrap"), 16L * 1024 * 1024));
        savePart(digest, "prlimit", selectFileHash(Path.of("/usr/bin/prlimit"), 16L * 1024 * 1024));
        try (InputStream code = IsolatedAttachmentExtractor.class.getResourceAsStream("IsolatedAttachmentExtractor.class")) {
            if (code == null) throw new IOException("EXTRACTOR_RUNNER_UNAVAILABLE");
            byte[] bytes = code.readNBytes(1024 * 1024 + 1);
            if (bytes.length > 1024 * 1024) throw new IOException("EXTRACTOR_RUNNER_LIMIT");
            savePart(digest, "runner", HexFormat.of().formatHex(selectDigest().digest(bytes)));
        }
        return new Identity(EXTRACTOR_VERSION, HexFormat.of().formatHex(digest.digest()), jars.size(), total);
    }

    private static String selectFileHash(Path file, long maximum) throws IOException {
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS) || Files.size(file) > maximum)
            throw new IOException("EXTRACTOR_ARTIFACT_INVALID");
        var digest = selectDigest();
        try (var stream = Files.newInputStream(file)) {
            byte[] buffer = new byte[32768];
            long bytes = 0;
            for (int count; (count = stream.read(buffer)) != -1;) {
                bytes += count;
                if (bytes > maximum) throw new IOException("EXTRACTOR_ARTIFACT_LIMIT");
                digest.update(buffer, 0, count);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }
    private static MessageDigest selectDigest() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException exception) { throw new IllegalStateException("SHA-256을 사용할 수 없습니다."); }
    }
    private static void savePart(MessageDigest digest, String key, String value) {
        digest.update((key + "\0" + value + "\n").getBytes(StandardCharsets.UTF_8));
    }
}
