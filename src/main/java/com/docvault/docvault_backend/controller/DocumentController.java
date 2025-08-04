package com.docvault.docvault_backend.controller;

import com.docvault.docvault_backend.model.Document;
import com.docvault.docvault_backend.service.DocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.io.*;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/documents")
@CrossOrigin(origins = {
        "http://localhost:*",
        "http://127.0.0.1:*"
}, allowCredentials = "true")
public class DocumentController {

    private final DocumentService documentService;

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Autowired
    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping
    public ResponseEntity<?> uploadDocument(@RequestParam("file") MultipartFile file,
                                            @RequestParam("title") String title,
                                            @RequestParam("tags") String tags,
                                            @AuthenticationPrincipal Jwt jwt) throws IOException {
        if (file.isEmpty()) return ResponseEntity.badRequest().body("Empty file.");

        try {
            String originalFilename = file.getOriginalFilename();
            String sanitizedFilename = originalFilename.replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
            String fileName = System.currentTimeMillis() + "_" + sanitizedFilename;
            String ownerId = jwt.getClaimAsString("preferred_username"); // or "sub" or "email"

            String docType = getExtension(fileName);
            File uploadPath = new File(uploadDir);
            if (!uploadPath.exists()) uploadPath.mkdirs();

            File dest = new File(uploadPath, fileName);
            try (InputStream in = file.getInputStream();
                 OutputStream out = new FileOutputStream(dest)) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }

            Document doc = new Document();
            doc.setTitle(title);
            doc.setName(originalFilename);
            doc.setFileName(fileName);
            doc.setDocType(docType);
            doc.setOwnerId(ownerId);
            doc.setUploadedBy(ownerId);
            doc.setFilePath(dest.getAbsolutePath());
            doc.setTags(tags);
            doc.setAdminOnly(false);
            doc.setUploadedAt(LocalDateTime.now());

            Document saved = documentService.save(doc);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Upload failed: " + e.getMessage());
        }
    }


    @GetMapping("/my")
    public ResponseEntity<List<Document>> getMyDocuments(@AuthenticationPrincipal Jwt jwt) {
        String ownerId = jwt.getClaimAsString("preferred_username");
        System.out.println(ownerId);
        return ResponseEntity.ok(documentService.getUserDocuments(ownerId));
    }

    @GetMapping
    public ResponseEntity<List<Document>> getAllDocuments() {
        return ResponseEntity.ok(documentService.getAllDocuments());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getDocumentMetadata(@PathVariable Long id) {
        Optional<Document> optionalDoc = documentService.getDocumentById(id);
        return optionalDoc.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long id) {
        Optional<Document> optionalDoc = documentService.getDocumentById(id);
        if (optionalDoc.isPresent()) {
            Document doc = optionalDoc.get();
            File file = new File(doc.getFilePath());
            if (!file.exists()) return ResponseEntity.notFound().build();

            Resource resource = new FileSystemResource(file);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + doc.getFileName() + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        } else return ResponseEntity.notFound().build();
    }

    @GetMapping("/{id}/preview")
    public ResponseEntity<Resource> previewDocument(@PathVariable Long id) {
        Optional<Document> optionalDoc = documentService.getDocumentById(id);
        if (optionalDoc.isPresent()) {
            Document doc = optionalDoc.get();
            File file = new File(doc.getFilePath());
            if (!file.exists()) return ResponseEntity.notFound().build();

            Resource resource = new FileSystemResource(file);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(resource);
        } else return ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateDocumentMetadata(@PathVariable Long id,
                                                    @RequestParam("title") String title,
                                                    @RequestParam("tags") String tags) {
        Optional<Document> optionalDoc = documentService.getDocumentById(id);
        if (optionalDoc.isPresent()) {
            Document doc = optionalDoc.get();
            doc.setTitle(title);
            doc.setTags(tags);
            documentService.save(doc);
            return ResponseEntity.ok(doc);
        } else return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDocument(@PathVariable Long id) {
        Optional<Document> optionalDoc = documentService.getDocumentById(id);
        if (optionalDoc.isPresent()) {
            documentService.deleteDocument(id);
            return ResponseEntity.ok("Deleted document with id: " + id);
        } else return ResponseEntity.notFound().build();
    }

    private String getExtension(String filename) {
        int i = filename.lastIndexOf('.');
        return (i > 0) ? filename.substring(i + 1) : "unknown";
    }
}
