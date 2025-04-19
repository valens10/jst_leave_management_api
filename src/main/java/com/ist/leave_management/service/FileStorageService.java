package com.ist.leave_management.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${file.upload.dir}")
    private String uploadDir;

    @Value("${file.allowed-types}")
    private String allowedTypes;

    public String storeFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }

        // Validate file type
        String fileExtension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        if (!isValidFileType(fileExtension)) {
            throw new IllegalArgumentException("Invalid file type. Allowed types: " + allowedTypes);
        }

        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(uploadPath);

        // Generate unique filename
        String fileName = UUID.randomUUID().toString() + "." + fileExtension;

        // Copy file to target location
        Path targetLocation = uploadPath.resolve(fileName);
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        return fileName;
    }

    private boolean isValidFileType(String fileExtension) {
        if (fileExtension == null) {
            return false;
        }
        String[] allowedExtensions = allowedTypes.split(",");
        for (String ext : allowedExtensions) {
            if (ext.trim().equalsIgnoreCase(fileExtension)) {
                return true;
            }
        }
        return false;
    }

    public Path loadFile(String fileName) {
        return Paths.get(uploadDir).resolve(fileName).normalize();
    }

    public void deleteFile(String fileName) throws IOException {
        if (fileName == null) {
            return;
        }
        Path filePath = loadFile(fileName);
        Files.deleteIfExists(filePath);
    }
}