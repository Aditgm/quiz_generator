package com.quizgen.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.core.io.ByteArrayResource;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class TextExtractionService {

    private static final Logger logger = LoggerFactory.getLogger(TextExtractionService.class);

    @Value("${ocr.space.api.key:K84061803788957}")
    private String ocrSpaceApiKey; // This is a free public key, but users should override it

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    
    public String extractText(MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename();
        if (fileName == null) {
            throw new IllegalArgumentException("File name cannot be null");
        }

        String fileExtension = getFileExtension(fileName).toLowerCase();

        return switch (fileExtension) {
            case "txt" -> extractFromTextFile(file);
            case "pdf" -> extractFromPDF(file);
            case "docx" -> extractFromWord(file);
            case "jpg", "jpeg", "png" -> extractFromImage(file);
            default -> throw new IllegalArgumentException("Unsupported file type: " + fileExtension);
        };
    }

    
    private String extractFromTextFile(MultipartFile file) throws IOException {
        logger.info("Extracting text from .txt file");
        return new String(file.getBytes(), StandardCharsets.UTF_8);
    }

    
    private String extractFromPDF(MultipartFile file) throws IOException {
        logger.info("Extracting text from PDF file");
        try (PDDocument document = org.apache.pdfbox.Loader.loadPDF(file.getBytes())) {

            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            if (text == null || text.trim().isEmpty()) {
                logger.warn("Empty text returned from native PDF extraction. This is likely a scanned image PDF. Using OCR fallback.");
                return extractUsingOCRSpace(file);
            }

            logger.info("Successfully extracted {} characters from PDF natively", text.length());
            return text;
        } catch (Exception e) {
            logger.warn("PDF extraction failed natively, attempting OCR fallback: {}", e.getMessage());
            return extractUsingOCRSpace(file);
        }
    }

    
    private String extractFromWord(MultipartFile file) throws IOException {
        logger.info("Extracting text from Word document");
        try (InputStream inputStream = file.getInputStream();
                XWPFDocument document = new XWPFDocument(inputStream)) {

            StringBuilder text = new StringBuilder();
            List<XWPFParagraph> paragraphs = document.getParagraphs();

            for (XWPFParagraph paragraph : paragraphs) {
                text.append(paragraph.getText()).append("\n");
            }

            String extractedText = text.toString();
            logger.info("Successfully extracted {} characters from Word document", extractedText.length());
            return extractedText;
        }
    }

    
    private String extractFromImage(MultipartFile file) throws IOException {
        logger.info("Extracting text from Image natively using OCR Space API");
        return extractUsingOCRSpace(file);
    }

    
    private String extractUsingOCRSpace(MultipartFile file) throws IOException {
        try {
            logger.info("Sending {} to OCR.Space API for extraction", file.getOriginalFilename());
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.set("apikey", ocrSpaceApiKey);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            
            ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };
            body.add("file", resource);
            body.add("scale", true);
            body.add("isTable", true);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity("https://api.ocr.space/parse/image", requestEntity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode node = mapper.readTree(response.getBody());
                JsonNode parsedResults = node.get("ParsedResults");
                if (parsedResults != null && parsedResults.isArray() && parsedResults.size() > 0) {
                    StringBuilder fullText = new StringBuilder();
                    for (JsonNode res : parsedResults) {
                        JsonNode parsedTextNode = res.get("ParsedText");
                        if (parsedTextNode != null) {
                            fullText.append(parsedTextNode.asText()).append("\n");
                        }
                    }
                    if (fullText.length() > 0) {
                         logger.info("OCR successful, returned {} chars", fullText.length());
                         return fullText.toString();
                    }
                }
            }
            logger.warn("OCR.Space returned empty or unrecognized format.");
        } catch (Exception e) {
            logger.error("OCR API Exception: {}", e.getMessage());
        }
        return "Warning: Could not extract text from document due to missing OCR dependencies or unreachable API.";
    }

    
    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return "";
        }
        return fileName.substring(lastDotIndex + 1);
    }

    
    public boolean isSupportedFileType(String fileName) {
        String extension = getFileExtension(fileName).toLowerCase();
        return List.of("txt", "pdf", "docx", "jpg", "jpeg", "png").contains(extension);
    }
}
