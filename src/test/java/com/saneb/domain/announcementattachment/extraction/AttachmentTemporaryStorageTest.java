package com.saneb.domain.announcementattachment.extraction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AttachmentTemporaryStorageTest {
    @TempDir Path temporary;
    private static final Instant NOW = Instant.parse("2026-09-10T00:00:00Z");
    private AttachmentTemporaryStorage selectStorage(long bytes) {
        return new AttachmentTemporaryStorage(temporary, bytes, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test void att038SuccessAndExceptionScopesRemoveOnlyTheirOwnOriginals() throws Exception {
        var storage = selectStorage(42L * 1024 * 1024);
        Path file;
        try (var workspace = storage.insertWorkspace(UUID.randomUUID(), UUID.randomUUID())) {
            file = workspace.selectBinaryPath();
            Files.writeString(file, "test document");
            Files.writeString(workspace.selectDetailPath(), "<html>test</html>");
            assertThat(file.getParent().getParent()).isEqualTo(temporary.resolve("announcement-attachment-tmp"));
        }
        assertThat(file).doesNotExist();
        assertThat(file.getParent()).doesNotExist();
        assertThat(temporary.resolve("announcement-attachment-tmp/.owner")).exists();
        assertThatThrownBy(() -> {
            try (var workspace = storage.insertWorkspace(UUID.randomUUID(), UUID.randomUUID())) {
                Files.writeString(workspace.selectBinaryPath(), "partial");
                throw new IOException("EXPECTED_TEST_FAILURE");
            }
        }).hasMessage("EXPECTED_TEST_FAILURE");
        try (var paths = Files.list(temporary.resolve("announcement-attachment-tmp"))) {
            assertThat(paths.filter(path -> path.getFileName().toString().startsWith("job-")).count()).isZero();
        }
    }

    @Test void att024CapacityIsReservedBeforeNetworkAndReleasedAfterCleanup() throws Exception {
        var storage = selectStorage(21L * 1024 * 1024);
        try (var workspace = storage.insertWorkspace(UUID.randomUUID(), UUID.randomUUID())) {
            assertThatThrownBy(() -> storage.insertWorkspace(UUID.randomUUID(), UUID.randomUUID()))
                    .isInstanceOf(IOException.class).hasMessage("TEMPORARY_BYTE_LIMIT");
        }
        try (var next = storage.insertWorkspace(UUID.randomUUID(), UUID.randomUUID())) {
            assertThat(next.selectBinaryPath().getParent()).exists();
        }
    }

    @Test void att038ExistingUnownedDirectoryIsNotTakenOverOrDeleted() throws Exception {
        Path existing = Files.createDirectories(temporary.resolve("announcement-attachment-tmp"));
        Path userFile = Files.writeString(existing.resolve("user-data.txt"), "preserve");
        var storage = selectStorage(21L * 1024 * 1024);
        assertThatThrownBy(() -> storage.insertWorkspace(UUID.randomUUID(), UUID.randomUUID()))
                .hasMessage("TEMPORARY_PATH_NOT_OWNED");
        assertThat(Files.readString(userFile)).isEqualTo("preserve");
        assertThat(existing.resolve(".owner")).doesNotExist();
    }

    @Test void att038ExpiredOrphanIsCleanedButLiveWorkspaceIsPreserved() throws Exception {
        var storage = selectStorage(63L * 1024 * 1024);
        try (var live = storage.insertWorkspace(UUID.randomUUID(), UUID.randomUUID())) {
            Files.setLastModifiedTime(live.selectBinaryPath().getParent().resolve(".owner"), FileTime.from(NOW.minus(Duration.ofHours(25))));
            Path orphan = selectOrphan(temporary.resolve("announcement-attachment-tmp"), 25);
            Path fresh = selectOrphan(temporary.resolve("announcement-attachment-tmp"), 23);
            assertThat(storage.deleteExpiredWorkspaces()).isEqualTo(1);
            assertThat(orphan).doesNotExist();
            assertThat(fresh).exists();
            assertThat(live.selectBinaryPath().getParent()).exists();
        }
    }

    @Test void att038UnexpectedFilePreventsDeletionOfUserData() throws Exception {
        var storage = selectStorage(21L * 1024 * 1024);
        var workspace = storage.insertWorkspace(UUID.randomUUID(), UUID.randomUUID());
        Path userFile = Files.writeString(workspace.selectBinaryPath().getParent().resolve("unexpected.txt"), "preserve");
        assertThatThrownBy(workspace::close).hasMessage("TEMPORARY_PATH_NOT_OWNED");
        assertThat(Files.readString(userFile)).isEqualTo("preserve");
        assertThat(storage.deleteExpiredWorkspaces()).isZero();
    }

    @Test void att024ReservationsAreSharedAcrossStorageInstances() throws Exception {
        var firstStorage = selectStorage(21L * 1024 * 1024);
        try (var first = firstStorage.insertWorkspace(UUID.randomUUID(), UUID.randomUUID())) {
            var secondStorage = selectStorage(21L * 1024 * 1024);
            assertThatThrownBy(() -> secondStorage.insertWorkspace(UUID.randomUUID(), UUID.randomUUID()))
                    .hasMessage("TEMPORARY_BYTE_LIMIT");
        }
    }

    @Test void att038CleanupWithoutWorkerDoesNotCreateStorageDirectories() throws Exception {
        assertThat(selectStorage(21L * 1024 * 1024).deleteExpiredWorkspaces()).isZero();
        assertThat(temporary.resolve("announcement-attachment-tmp")).doesNotExist();
    }

    @Test void att024HeldQuotaLockRejectsReservationAndCleanupReleasesWorkspaceHandle() throws Exception {
        var storage = selectStorage(42L * 1024 * 1024);
        var workspace = storage.insertWorkspace(UUID.randomUUID(), UUID.randomUUID());
        Path directory = workspace.selectBinaryPath().getParent();
        Files.setLastModifiedTime(directory.resolve(".owner"), FileTime.from(NOW.minus(Duration.ofHours(25))));
        try (var channel = java.nio.channels.FileChannel.open(directory.getParent().resolve(".quota.lock"), java.nio.file.StandardOpenOption.WRITE);
             var lock = channel.lock()) {
            assertThatThrownBy(() -> storage.insertWorkspace(UUID.randomUUID(), UUID.randomUUID())).hasMessage("TEMPORARY_RESOURCE_BUSY");
            assertThatThrownBy(workspace::close).hasMessage("TEMPORARY_RESOURCE_BUSY");
        }
        assertThat(storage.deleteExpiredWorkspaces()).isEqualTo(1);
        assertThat(directory).doesNotExist();
    }

    private Path selectOrphan(Path root, int ageHours) throws Exception {
        Path orphan = Files.createDirectory(root.resolve("job-" + UUID.randomUUID() + "-" + UUID.randomUUID() + "-12345"));
        Files.writeString(orphan.resolve(".owner"), "saneb-attachment-temporary-v1");
        Files.writeString(orphan.resolve(".lock"), "");
        Files.writeString(orphan.resolve("attachment.bin"), "interrupted process original");
        Files.setLastModifiedTime(orphan.resolve(".owner"), FileTime.from(NOW.minus(Duration.ofHours(ageHours))));
        return orphan;
    }
}
