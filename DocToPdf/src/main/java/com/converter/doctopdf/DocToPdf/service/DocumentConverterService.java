
package com.converter.doctopdf.DocToPdf.service;

import org.jodconverter.core.DocumentConverter;
import org.jodconverter.core.document.DefaultDocumentFormatRegistry;
import org.jodconverter.core.document.DocumentFormat;
import org.jodconverter.core.office.OfficeException;
import org.jodconverter.local.office.LocalOfficeManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Service
public class DocumentConverterService {

    @Autowired
    private DocumentConverter documentConverter;

    @Autowired
    private LocalOfficeManager officeManager;

    // Supported file extensions
    private static final List<String> SUPPORTED_EXTENSIONS = Arrays.asList(
            "doc", "docx", "xls", "xlsx", "ppt", "pptx", "odt", "ods", "odp", "rtf");

    public byte[] convertToPdf(MultipartFile file) throws IOException {
        validateFile(file);

        // Ensure LibreOffice is running
        try {
            if (!officeManager.isRunning()) {
                System.out.println("Starting LibreOffice...");
                officeManager.start();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to start LibreOffice: " + e.getMessage(), e);
        }

        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);

        System.out.println("Processing file: " + originalFilename + " with extension: " + extension);

        DocumentFormat sourceFormat = DefaultDocumentFormatRegistry.getFormatByExtension(extension);
        if (sourceFormat == null) {
            throw new IllegalArgumentException("Unsupported file format: " + extension +
                    ". Supported formats: " + SUPPORTED_EXTENSIONS);
        }

        DocumentFormat targetFormat = DefaultDocumentFormatRegistry.PDF;

        ByteArrayInputStream inputStream = new ByteArrayInputStream(file.getBytes());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try {
            System.out.println("Starting conversion process...");
            documentConverter.convert(inputStream)
                    .as(sourceFormat)
                    .to(outputStream)
                    .as(targetFormat)
                    .execute();
            System.out.println("Conversion completed successfully");
        } catch (OfficeException e) {
            System.err.println("Office conversion error: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Document conversion failed: " + e.getMessage(), e);
        } catch (Exception e) {
            System.err.println("General conversion error: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Unexpected error during conversion: " + e.getMessage(), e);
        }

        byte[] result = outputStream.toByteArray();
        if (result.length == 0) {
            throw new RuntimeException("Conversion resulted in empty PDF");
        }

        return result;
    }

    private void validateFile(MultipartFile file) {
        if (file == null) {
            throw new IllegalArgumentException("File cannot be null");
        }

        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        if (file.getSize() > 50 * 1024 * 1024) { // 50MB limit
            throw new IllegalArgumentException("File size exceeds 50MB limit");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || filename.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid filename");
        }

        String extension = getFileExtension(filename);
        if (!SUPPORTED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new IllegalArgumentException("Unsupported file format: " + extension +
                    ". Supported formats: " + SUPPORTED_EXTENSIONS);
        }

        // Additional MIME type validation
        String contentType = file.getContentType();
        System.out.println("File content type: " + contentType);

        // Log file details for debugging
        System.out.println("File validation passed:");
        System.out.println("- Name: " + filename);
        System.out.println("- Size: " + file.getSize() + " bytes");
        System.out.println("- Extension: " + extension);
        System.out.println("- Content Type: " + contentType);
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot + 1).toLowerCase() : "";
    }
}