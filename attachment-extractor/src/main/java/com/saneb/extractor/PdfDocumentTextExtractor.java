package com.saneb.extractor;

import java.io.IOException;
import java.nio.file.Path;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;

final class PdfDocumentTextExtractor {
    ExtractionResult selectExtraction(Path file) throws IOException {
        TextEvidence evidence = new TextEvidence();
        try (var document = Loader.loadPDF(file.toFile())) {
            if (document.isEncrypted() || !document.getCurrentAccessPermission().canExtractContent())
                return ExtractionResult.failure("ENCRYPTED");
            if (document.getNumberOfPages() > 200) throw new IOException("LIMIT_EXCEEDED");
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            for (int page = 1; page <= document.getNumberOfPages(); page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String text = stripper.getText(document);
                // PDF 그리기 순서만으로 표·다단의 AND 범위를 증명할 수 없으므로 범위 신뢰를 부여하지 않는다.
                evidence.insertBlock(text, "page:" + page, false);
                var resources = document.getPage(page - 1).getResources();
                if (text.isBlank() || (resources != null && resources.getXObjectNames().iterator().hasNext()))
                    evidence.updatePartial();
            }
            // 텍스트 추출 성공과 자동 분류에 필요한 문단 구조 신뢰는 별도 값이다.
            return evidence.selectResult("PDF", document.getNumberOfPages());
        } catch (InvalidPasswordException exception) {
            return ExtractionResult.failure("ENCRYPTED");
        }
    }
}
