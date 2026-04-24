package com.quizgen.controller;

import com.quizgen.model.StudyMaterial;
import com.quizgen.repository.StudyMaterialRepository;
import com.quizgen.service.TextExtractionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/materials")
@CrossOrigin(origins = "*")
public class StudyMaterialController {

    private static final Logger logger = LoggerFactory.getLogger(StudyMaterialController.class);

    @Autowired
    private StudyMaterialRepository studyMaterialRepository;

    @Autowired
    private TextExtractionService textExtractionService;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    
    @PostMapping("/upload")
    public ResponseEntity<?> uploadMaterial(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("File is empty"));
            }

            String fileName = file.getOriginalFilename();
            if (fileName == null || !textExtractionService.isSupportedFileType(fileName)) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Unsupported file type. Supported: txt, pdf, docx"));
            }

            logger.info("Uploading file: {}", fileName);

            // Save file physically to uploads directory
            try {
                Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }
                Path filePath = uploadPath.resolve(fileName);
                try (InputStream inputStream = file.getInputStream()) {
                    Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
                }
                logger.info("File saved to disk at: {}", filePath.toAbsolutePath());
            } catch (IOException e) {
                logger.error("Could not save file to disk", e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(createErrorResponse("Failed to physically store the file: " + e.getMessage()));
            }

            String extractedText = textExtractionService.extractText(file);

            StudyMaterial material = new StudyMaterial();
            material.setFileName(fileName);
            material.setFileType(getFileExtension(fileName));
            material.setExtractedText(extractedText);
            material.setUploadedBy("teacher");

            material = studyMaterialRepository.save(material);

            logger.info("Study material saved with ID: {}", material.getId());

            return ResponseEntity.ok(material);

        } catch (Exception e) {
            logger.error("Error uploading file: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to upload file: " + e.getMessage()));
        }
    }

    
    @GetMapping
    public ResponseEntity<List<StudyMaterial>> getAllMaterials() {
        List<StudyMaterial> materials = studyMaterialRepository.findAll();
        return ResponseEntity.ok(materials);
    }

    
    @GetMapping("/{id}")
    public ResponseEntity<?> getMaterialById(@PathVariable Long id) {
        return studyMaterialRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMaterial(@PathVariable Long id) {
        if (!studyMaterialRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        studyMaterialRepository.deleteById(id);
        return ResponseEntity.ok(createSuccessResponse("Material deleted successfully"));
    }

    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return "";
        }
        return fileName.substring(lastDotIndex + 1);
    }

    private Map<String, String> createErrorResponse(String message) {
        Map<String, String> response = new HashMap<>();
        response.put("error", message);
        return response;
    }

    private Map<String, String> createSuccessResponse(String message) {
        Map<String, String> response = new HashMap<>();
        response.put("message", message);
        return response;
    }
}
