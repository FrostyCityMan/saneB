package com.saneb.extractor;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Arrays;

/** Spring·DB·HTTP를 시작하지 않는 일회성 CLI. 호출자는 OS 격리를 반드시 적용한다. */
public final class AttachmentExtractorMain {
    private AttachmentExtractorMain() { }
    public static void main(String[] args) throws Exception {
        PrintStream ipc = System.out;
        // 파서의 진단에 파일명·문서 내용이 포함되어도 IPC/운영 로그에 유출하지 않는다.
        System.setOut(new PrintStream(OutputStream.nullOutputStream()));
        System.setErr(new PrintStream(OutputStream.nullOutputStream()));
        ExtractionResult result;
        try {
            if (args.length != 1) throw new IllegalArgumentException();
            result = selectExtraction(Path.of(args[0]));
        } catch (Exception exception) {
            String code = "LIMIT_EXCEEDED".equals(exception.getMessage()) ? "LIMIT_EXCEEDED"
                    : "ENCRYPTED".equals(exception.getMessage()) ? "ENCRYPTED" : "CORRUPT";
            result = ExtractionResult.failure(code);
        }
        ipc.print(new ObjectMapper().writeValueAsString(result));
        ipc.flush();
    }

    public static ExtractionResult selectExtraction(Path file) throws Exception {
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS) || Files.size(file) > 20L * 1024 * 1024)
            throw new java.io.IOException("LIMIT_EXCEEDED");
        byte[] prefix;
        try (var input = Files.newInputStream(file)) { prefix = input.readNBytes(8); }
        if (prefix.length >= 5 && new String(prefix, 0, 5, java.nio.charset.StandardCharsets.US_ASCII).equals("%PDF-"))
            return new PdfDocumentTextExtractor().selectExtraction(file);
        if (Arrays.equals(prefix, new byte[] {(byte)0xd0,(byte)0xcf,0x11,(byte)0xe0,(byte)0xa1,(byte)0xb1,0x1a,(byte)0xe1}))
            return new HwpDocumentTextExtractor().selectExtraction(file);
        if (prefix.length >= 4 && prefix[0] == 'P' && prefix[1] == 'K' && prefix[2] == 3 && prefix[3] == 4)
            return new HwpxDocumentTextExtractor().selectExtraction(file);
        return ExtractionResult.failure("UNSUPPORTED");
    }
}
