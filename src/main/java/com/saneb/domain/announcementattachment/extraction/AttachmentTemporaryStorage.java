package com.saneb.domain.announcementattachment.extraction;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** 첨부 원본 전용 하위 경로만 관리한다. 파일 이름·원문 URL을 디렉터리 이름으로 사용하지 않는다. */
@Component
public final class AttachmentTemporaryStorage {
    private static final String OWNER = "saneb-attachment-temporary-v1";
    private static final long RESERVATION_BYTES = 21L * 1024 * 1024;
    private static final long HARD_CAP_BYTES = 1024L * 1024 * 1024;
    private static final List<String> OWNED_FILES = List.of("detail.html", "attachment.bin", ".owner", ".lock");
    private final Path configuredRoot;
    private final long capacity;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public AttachmentTemporaryStorage(@Value("${saneb.storage.root:build/saneb-storage}") String storageRoot) {
        this(Path.of(storageRoot), HARD_CAP_BYTES, Clock.systemUTC());
    }
    AttachmentTemporaryStorage(Path storageRoot, long capacity, Clock clock) {
        if (capacity < RESERVATION_BYTES || capacity > HARD_CAP_BYTES) throw new IllegalArgumentException("첨부 임시 용량은 21 MiB~1 GiB여야 합니다.");
        this.configuredRoot = storageRoot.toAbsolutePath().normalize().resolve("announcement-attachment-tmp");
        this.capacity = capacity;
        this.clock = clock;
    }

    public synchronized Workspace insertWorkspace(UUID jobId, UUID leaseToken) throws IOException {
        if (jobId == null || leaseToken == null) throw new IOException("TEMPORARY_OWNER_REQUIRED");
        Path root = selectRoot();
        try (var quotaChannel = FileChannel.open(root.resolve(".quota.lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
             var quotaLock = selectLock(quotaChannel)) {
            long used = selectReservedBytes(root);
            if (used > capacity - RESERVATION_BYTES) throw new IOException("TEMPORARY_BYTE_LIMIT");
            Path directory = Files.createTempDirectory(root, "job-" + jobId + "-" + leaseToken + "-");
            FileChannel channel = null;
            FileLock lock = null;
            try {
                savePrivatePermissions(directory);
                Files.writeString(directory.resolve(".owner"), OWNER, StandardOpenOption.CREATE_NEW);
                channel = FileChannel.open(directory.resolve(".lock"), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
                lock = selectLock(channel);
                return new Workspace(root, directory, channel, lock);
            } catch (IOException | RuntimeException exception) {
                if (lock != null) lock.close();
                if (channel != null) channel.close();
                // 실패한 생성 중에도 이 메서드가 만든 빈/소유 디렉터리만 정리한다.
                for (String name : OWNED_FILES) Files.deleteIfExists(directory.resolve(name));
                Files.deleteIfExists(directory);
                throw exception;
            }
        }
    }

    public synchronized int deleteExpiredWorkspaces() throws IOException {
        if (!Files.exists(configuredRoot, LinkOption.NOFOLLOW_LINKS)) return 0;
        Path root = selectRoot();
        int deleted = 0;
        try (var quotaChannel = FileChannel.open(root.resolve(".quota.lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
             var quotaLock = selectLock(quotaChannel); var directories = Files.newDirectoryStream(root, "job-*")) {
            for (Path directory : directories) {
                if (!selectOwnedDirectory(root, directory)) continue;
                if (Files.getLastModifiedTime(directory.resolve(".owner"), LinkOption.NOFOLLOW_LINKS).toInstant()
                        .plus(Duration.ofHours(24)).isAfter(clock.instant())) continue;
                if (!Files.isRegularFile(directory.resolve(".lock"), LinkOption.NOFOLLOW_LINKS)) continue;
                boolean available = false;
                try (var channel = FileChannel.open(directory.resolve(".lock"), StandardOpenOption.WRITE)) {
                    try (var lock = channel.tryLock()) { available = lock != null; }
                    catch (OverlappingFileLockException exception) { /* 살아 있는 동일 JVM worker는 보존한다. */ }
                }
                if (available) { deleteOwnedDirectory(root, directory); deleted++; }
            }
        }
        return deleted;
    }

    public synchronized boolean selectWorkspaceAvailable() throws IOException {
        if (!Files.exists(configuredRoot, LinkOption.NOFOLLOW_LINKS)) return true;
        Path root=selectRoot();
        try (var channel=FileChannel.open(root.resolve(".quota.lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE);
             var lock=selectLock(channel)) {
            return selectReservedBytes(root)<=capacity-RESERVATION_BYTES;
        }
    }

    private Path selectRoot() throws IOException {
        if (Files.isSymbolicLink(configuredRoot)) throw new IOException("TEMPORARY_PATH_UNSAFE");
        try { Files.createDirectories(configuredRoot, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------"))); }
        catch (UnsupportedOperationException exception) { Files.createDirectories(configuredRoot); }
        Path root = configuredRoot.toRealPath();
        if (!"announcement-attachment-tmp".equals(root.getFileName().toString())) throw new IOException("TEMPORARY_PATH_UNSAFE");
        Path marker = root.resolve(".owner");
        if (!Files.exists(marker, LinkOption.NOFOLLOW_LINKS)) {
            // 기존 업무 파일이 있는 폴더를 첨부 임시 폴더로 인수하지 않는다.
            try (var children = Files.list(root)) {
                if (children.findAny().isPresent()) throw new IOException("TEMPORARY_PATH_NOT_OWNED");
            }
            Files.writeString(marker, OWNER, StandardOpenOption.CREATE_NEW);
        }
        if (!Files.isRegularFile(marker, LinkOption.NOFOLLOW_LINKS) || Files.size(marker) > 100
                || !OWNER.equals(Files.readString(marker))) throw new IOException("TEMPORARY_PATH_NOT_OWNED");
        savePrivatePermissions(root);
        if (Files.isSymbolicLink(root.resolve(".quota.lock"))) throw new IOException("TEMPORARY_PATH_UNSAFE");
        return root;
    }

    private long selectReservedBytes(Path root) throws IOException {
        long total = 0;
        try (var entries = Files.newDirectoryStream(root)) {
            for (Path entry : entries) {
                if (List.of(".owner", ".quota.lock").contains(entry.getFileName().toString())) continue;
                if (!selectOwnedDirectory(root, entry)) throw new IOException("TEMPORARY_PATH_NOT_OWNED");
                long bytes = 0;
                try (var files = Files.newDirectoryStream(entry)) {
                    for (Path file : files) {
                        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) throw new IOException("TEMPORARY_PATH_UNSAFE");
                        bytes = Math.addExact(bytes, Files.size(file));
                    }
                }
                total = Math.addExact(total, Math.max(RESERVATION_BYTES, bytes));
                if (total > capacity) return total;
            }
        }
        return total;
    }

    private boolean selectOwnedDirectory(Path root, Path directory) throws IOException {
        if (!directory.toAbsolutePath().normalize().getParent().equals(root)
                || !directory.getFileName().toString().matches("job-[0-9a-f-]{73}-[0-9]+")
                || !Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) return false;
        Path owner = directory.resolve(".owner");
        if (!Files.isRegularFile(owner, LinkOption.NOFOLLOW_LINKS) || Files.size(owner) > 100 || !OWNER.equals(Files.readString(owner))) return false;
        try (var files = Files.newDirectoryStream(directory)) {
            for (Path file : files) if (!OWNED_FILES.contains(file.getFileName().toString())
                    || !Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) return false;
        }
        return true;
    }

    private void deleteOwnedDirectory(Path root, Path directory) throws IOException {
        if (!selectOwnedDirectory(root, directory)) throw new IOException("TEMPORARY_PATH_NOT_OWNED");
        // 비재귀 삭제: 예상치 못한 사용자 파일/하위 디렉터리는 삭제하지 않는다.
        for (String name : OWNED_FILES) Files.deleteIfExists(directory.resolve(name));
        Files.delete(directory);
    }

    private FileLock selectLock(FileChannel channel) throws IOException {
        try {
            FileLock lock = channel.tryLock();
            if (lock == null) throw new IOException("TEMPORARY_RESOURCE_BUSY");
            return lock;
        } catch (OverlappingFileLockException exception) { throw new IOException("TEMPORARY_RESOURCE_BUSY"); }
    }
    private void savePrivatePermissions(Path path) throws IOException {
        if (Files.getFileStore(path).supportsFileAttributeView("posix"))
            Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rwx------"));
    }

    public final class Workspace implements AutoCloseable {
        private final Path root;
        private final Path directory;
        private final FileChannel channel;
        private final FileLock lock;
        private final AtomicBoolean closed = new AtomicBoolean();
        private Workspace(Path root, Path directory, FileChannel channel, FileLock lock) {
            this.root = root; this.directory = directory; this.channel = channel; this.lock = lock;
        }
        public Path selectDetailPath() { return directory.resolve("detail.html"); }
        public Path selectBinaryPath() { return directory.resolve("attachment.bin"); }
        @Override public void close() throws IOException {
            synchronized (AttachmentTemporaryStorage.this) {
                if (!closed.compareAndSet(false, true)) return;
                try (var quotaChannel = FileChannel.open(root.resolve(".quota.lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                     var quotaLock = selectLock(quotaChannel)) {
                    try { lock.close(); } finally { channel.close(); }
                    deleteOwnedDirectory(root, directory);
                } finally {
                    // quota 경합/정리 실패도 파일 핸들을 누수시키지 않는다. 남은 소유 파일은 24시간 정리 대상이다.
                    try { if (lock.isValid()) lock.close(); } finally { channel.close(); }
                }
            }
        }
    }
}
