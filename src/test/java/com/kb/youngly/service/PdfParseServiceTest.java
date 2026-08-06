package com.kb.youngly.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfParseServiceTest {

    private final PdfParseService pdfParseService = new PdfParseService();

    @Test
    @DisplayName("PDFBox로 PDF 바이트에서 텍스트를 추출하고 공백을 정리한다")
    void extractText_extractsAndNormalizesText() throws Exception {
        byte[] pdfBytes = createSamplePdf("FSS afternoon market report");

        String text = pdfParseService.extractText(pdfBytes);

        assertTrue(text.contains("FSS afternoon market report"));
    }

    private byte[] createSamplePdf(String text) throws Exception {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                contentStream.newLineAtOffset(50, 700);
                contentStream.showText(text);
                contentStream.endText();
            }

            document.save(out);
            return out.toByteArray();
        }
    }
}
