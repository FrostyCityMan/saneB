package com.saneb.extractor;

import java.io.IOException;
import java.nio.file.Path;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;

final class PdfDocumentTextExtractor {
    ExtractionResult selectExtraction(Path file) throws IOException {
        TextEvidence evidence = new TextEvidence();
        try (var document = Loader.loadPDF(file.toFile())) {
            if (document.isEncrypted() || !document.getCurrentAccessPermission().canExtractContent())
                return ExtractionResult.failure("ENCRYPTED");
            if (document.getNumberOfPages() > 200) throw new IOException("LIMIT_EXCEEDED");
            var scopes=PdfStructureScopes.select(document);
            int reliablePages=0,externalObjects=0,inlineImages=0,blankPages=0;
            for (int page = 1; page <= document.getNumberOfPages(); page++) {
                var result=new PdfStructuredTextStripper(PdfStructureScopes.selectPage(scopes,document.getPage(page-1).getCOSObject())).selectPage(document,page);
                if(result.reliable())reliablePages++;
                externalObjects+=result.externalObjectInvocations();inlineImages+=result.inlineImageInvocations();
                if(result.text().isBlank())blankPages++;
                if(result.reliable())for(var part:result.parts())evidence.insertBlock(part.text(),part.scope().locator(),true);
                else evidence.insertBlock(result.text(),"page:"+page,false);
                if(result.partial() || result.text().isBlank())evidence.updatePartial();
            }
            // 텍스트 추출 성공과 자동 분류에 필요한 문단 구조 신뢰는 별도 값이다.
            var extracted=evidence.selectResult("PDF", document.getNumberOfPages());
            int replacements=(int)extracted.text().codePoints().filter(point->point==0xfffd).count();
            return new ExtractionResult(extracted.format(),extracted.extractorVersion(),extracted.qualityCode(),extracted.text(),
                    extracted.blocks(),extracted.pageCount(),extracted.errorCode(),null,null,null,
                    new ExtractionResult.PdfStructure(document.getNumberOfPages(),reliablePages,externalObjects,inlineImages,blankPages,replacements));
        } catch (InvalidPasswordException exception) {
            return ExtractionResult.failure("ENCRYPTED");
        }
    }
}
