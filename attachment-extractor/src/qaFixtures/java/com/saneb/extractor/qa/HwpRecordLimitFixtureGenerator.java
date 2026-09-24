package com.saneb.extractor.qa;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;

/** 시험 전용 HWP 노드 경계 입력. 설치 runtime fixture와 배포 artifact에는 포함하지 않는다. */
public final class HwpRecordLimitFixtureGenerator {
    private HwpRecordLimitFixtureGenerator() { }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("시험 출력 경로가 필요합니다.");
        Path root = Files.createDirectories(Path.of(args[0]));
        for (int count : new int[] {20_000, 20_001}) {
            byte[] header = new byte[256];
            System.arraycopy("HWP Document File".getBytes(StandardCharsets.US_ASCII), 0, header, 0, 17);
            header[35] = 5;
            // 비압축으로 생성해 압축률 차단이 아닌 실제 문단 노드 제한에 도달하게 한다.
            var body = ByteBuffer.allocate(count * 28).order(ByteOrder.LITTLE_ENDIAN);
            for (int i = 0; i < count; i++) {
                body.putInt(66 | (24 << 20));
                body.putInt(0x80000001);
                body.put(new byte[20]);
            }
            try (var ole = new POIFSFileSystem()) {
                ole.getRoot().createDocument("FileHeader", new ByteArrayInputStream(header));
                ole.getRoot().createDirectory("BodyText").createDocument("Section0", new ByteArrayInputStream(body.array()));
                try (var output = Files.newOutputStream(root.resolve("paragraphs-" + count + ".hwp"))) {
                    ole.writeFilesystem(output);
                }
            }
        }
    }
}
