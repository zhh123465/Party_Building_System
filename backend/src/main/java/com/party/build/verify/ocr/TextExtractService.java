package com.party.build.verify.ocr;

import com.party.build.common.api.ErrorCode;
import com.party.build.common.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class TextExtractService {
    private final String tesseractCmd;
    private final String language;
    private final long timeoutSeconds;

    public TextExtractService(@Value("${app.ocr.tesseract:tesseract}") String tesseractCmd,
                              @Value("${app.ocr.language:chi_sim+eng}") String language,
                              @Value("${app.ocr.timeout-seconds:60}") long timeoutSeconds) {
        this.tesseractCmd = tesseractCmd;
        this.language = language;
        this.timeoutSeconds = timeoutSeconds;
    }

    public ExtractResult extract(InputStream inputStream, String originalName) {
        String ext = extension(originalName);
        try {
            Path temp = Files.createTempFile("party-poc-", "." + ext);
            Files.copy(inputStream, temp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            try {
                return switch (ext) {
                    case "pdf" -> extractPdf(temp.toFile());
                    case "docx" -> extractDocx(temp.toFile());
                    case "jpg", "jpeg", "png" -> new ExtractResult(ocr(temp.toFile()), 1, true, "tesseract");
                    default -> throw new BizException(ErrorCode.FILE_TYPE_NOT_SUPPORTED,
                            "仅支持 PDF、JPG、PNG、DOCX");
                };
            } finally {
                Files.deleteIfExists(temp);
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.warn("extract failed", e);
            throw new BizException(ErrorCode.OCR_FAILED, "文本抽取失败: " + e.getMessage());
        }
    }

    private ExtractResult extractPdf(File file) throws Exception {
        try (PDDocument doc = PDDocument.load(file)) {
            int pages = doc.getNumberOfPages();
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(doc);
            if (text != null && text.replaceAll("\\s", "").length() >= Math.max(50, pages * 50)) {
                return new ExtractResult(text, pages, false, "pdfbox");
            }
            // 扫描件：最多 OCR 前 5 页，避免 POC 超时
            PDFRenderer renderer = new PDFRenderer(doc);
            StringBuilder sb = new StringBuilder();
            int limit = Math.min(pages, 5);
            for (int i = 0; i < limit; i++) {
                BufferedImage image = renderer.renderImageWithDPI(i, 200);
                Path img = Files.createTempFile("party-pdf-page-", ".png");
                ImageIO.write(image, "png", img.toFile());
                try {
                    sb.append(ocr(img.toFile())).append("\n");
                } finally {
                    Files.deleteIfExists(img);
                }
            }
            return new ExtractResult(sb.toString(), pages, true, "pdfbox+tesseract");
        }
    }

    private ExtractResult extractDocx(File file) throws Exception {
        try (InputStream in = Files.newInputStream(file.toPath());
             XWPFDocument doc = new XWPFDocument(in)) {
            StringBuilder sb = new StringBuilder();
            doc.getParagraphs().forEach(p -> sb.append(p.getText()).append("\n"));
            for (XWPFTable table : doc.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    List<String> cells = new ArrayList<>();
                    for (XWPFTableCell cell : row.getTableCells()) {
                        cells.add(cell.getText());
                    }
                    sb.append(String.join("\t", cells)).append("\n");
                }
            }
            return new ExtractResult(sb.toString(), 1, false, "poi");
        }
    }

    private String ocr(File image) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    tesseractCmd, image.getAbsolutePath(), "stdout",
                    "-l", language, "--psm", "6"
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();
            byte[] output = process.getInputStream().readAllBytes();
            if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new BizException(ErrorCode.OCR_FAILED, "OCR 超时");
            }
            String text = new String(output, StandardCharsets.UTF_8);
            if (process.exitValue() != 0) {
                throw new BizException(ErrorCode.OCR_FAILED, "OCR 进程失败: " + truncate(text, 200));
            }
            return text;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(ErrorCode.OCR_FAILED, "OCR 失败: " + e.getMessage());
        }
    }

    private String extension(String name) {
        if (name == null || !name.contains(".")) {
            return "";
        }
        return name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private String truncate(String text, int max) {
        return text == null || text.length() <= max ? text : text.substring(0, max);
    }
}