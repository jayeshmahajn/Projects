
package com.converter.doctopdf.DocToPdf.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.converter.doctopdf.DocToPdf.service.DocumentConverterService;

import java.util.HashMap;
import java.util.Map;

@Controller
public class DocumentController {

    @Autowired
    private DocumentConverterService converterService;

    @GetMapping("/")
    public String home(Model model) {
        return "index";
    }

    @PostMapping("/convert")
    public ResponseEntity<?> convertToPdf(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "filename", required = false) String customFilename) {

        try {
            // Enhanced validation
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("No file provided or file is empty"));
            }

            // Log file details for debugging
            System.out.println("Received file: " + file.getOriginalFilename());
            System.out.println("File size: " + file.getSize());
            System.out.println("Content type: " + file.getContentType());

            byte[] pdfBytes = converterService.convertToPdf(file);

            String filename = customFilename != null && !customFilename.trim().isEmpty()
                    ? customFilename.trim() + ".pdf"
                    : getBaseFilename(file.getOriginalFilename()) + ".pdf";

            ByteArrayResource resource = new ByteArrayResource(pdfBytes);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(pdfBytes.length)
                    .body(resource);

        } catch (IllegalArgumentException e) {
            System.err.println("Validation error: " + e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            System.err.println("Conversion error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest()
                    .body(createErrorResponse("Failed to convert document: " + e.getMessage()));
        }
    }

    @PostMapping("/api/convert")
    @ResponseBody
    public ResponseEntity<?> convertToPdfApi(
            @RequestParam("file") MultipartFile file) {
        return convertToPdf(file, null);
    }

    @GetMapping("/api/health")
    @ResponseBody
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Service is running");
    }

    private String getBaseFilename(String originalFilename) {
        if (originalFilename == null)
            return "converted";
        int lastDot = originalFilename.lastIndexOf('.');
        return lastDot > 0 ? originalFilename.substring(0, lastDot) : originalFilename;
    }

    private Map<String, String> createErrorResponse(String message) {
        Map<String, String> error = new HashMap<>();
        error.put("error", message);
        return error;
    }
}