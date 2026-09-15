package com.saneb.extractor;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;

final class PdfDocumentTextExtractor {
    ExtractionResult selectExtraction(Path file) throws IOException {
        TextEvidence evidence = new TextEvidence();
        try (var document = Loader.loadPDF(file.toFile())) {
            if (document.isEncrypted() || !document.getCurrentAccessPermission().canExtractContent())
                return ExtractionResult.failure("ENCRYPTED");
            if (document.getNumberOfPages() > 200) throw new IOException("LIMIT_EXCEEDED");
            PDFTextStripper stripper = new PDFTextStripper() {
                @Override protected void processOperator(Operator operator,List<COSBase> operands) throws IOException {
                    // 자원 사전의 존재가 아니라 실제 실행한 그리기 명령을 확인한다.
                    // Form의 시각 내용·인라인 이미지도 텍스트 완전성이 증명되지 않았으므로 보존한다.
                    // 누락된 XObject가 PDFBox에서 경고로만 끝나도 정상 추출로 승격하지 않는다.
                    if ("Do".equals(operator.getName()) || "BI".equals(operator.getName())) evidence.updatePartial();
                    super.processOperator(operator,operands);
                }
            };
            stripper.setSortByPosition(true);
            for (int page = 1; page <= document.getNumberOfPages(); page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String text = stripper.getText(document);
                // PDF 그리기 순서만으로 표·다단의 AND 범위를 증명할 수 없으므로 범위 신뢰를 부여하지 않는다.
                evidence.insertBlock(text, "page:" + page, false);
                if (text.isBlank()) evidence.updatePartial();
            }
            // 텍스트 추출 성공과 자동 분류에 필요한 문단 구조 신뢰는 별도 값이다.
            return evidence.selectResult("PDF", document.getNumberOfPages());
        } catch (InvalidPasswordException exception) {
            return ExtractionResult.failure("ENCRYPTED");
        }
    }
}
