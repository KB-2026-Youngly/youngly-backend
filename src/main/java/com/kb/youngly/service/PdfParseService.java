package com.kb.youngly.service;

import com.kb.youngly.exception.PdfParseException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.regex.Pattern;

@Service
public class PdfParseService {

    private static final Pattern HORIZONTAL_SPACES = Pattern.compile("[\\t\\x0B\\f\\r ]+");
    private static final Pattern MULTIPLE_NEW_LINES = Pattern.compile("\\n{3,}");

    public String extractText(byte[] pdfBytes) {
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new PdfParseException("PDF 바이트가 비어 있습니다.", new IllegalArgumentException("empty pdf bytes"));
        }

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return normalize(stripper.getText(document));
        } catch (IOException e) {
            throw new PdfParseException("PDF 텍스트 추출에 실패했습니다.", e);
        }
    }

    public String normalize(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }

        String normalized = text.replace("\r\n", "\n").replace('\r', '\n');
        normalized = HORIZONTAL_SPACES.matcher(normalized).replaceAll(" ");
        normalized = normalized.lines()
                .map(String::trim)
                .filter(StringUtils::hasText)
                .reduce((left, right) -> left + "\n" + right)
                .orElse("");
        return MULTIPLE_NEW_LINES.matcher(normalized).replaceAll("\n\n").trim();
    }
}
