package com.movie.movieticket.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    /**
     * Store a file in the default upload directory
     */
    public String storeFile(MultipartFile file) {
        try {
            if (file.isEmpty()) {
                throw new RuntimeException("Failed to store empty file");
            }

            // Create the upload directory if it doesn't exist
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                System.out.println("Created upload directory: " + uploadPath.toAbsolutePath());
            }

            // Generate a unique file name
            String originalFileName = file.getOriginalFilename();
            String fileExtension = "";
            if (originalFileName != null && originalFileName.contains(".")) {
                fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
            }
            String fileName = UUID.randomUUID().toString() + fileExtension;

            // Copy the file to the upload directory
            Path targetLocation = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            System.out.println("File saved to: " + targetLocation.toAbsolutePath());
            
            // Return the URL path that can be used to access the file
            return "/uploads/" + fileName;
        } catch (IOException ex) {
            ex.printStackTrace();
            throw new RuntimeException("Could not store file " + file.getOriginalFilename(), ex);
        }
    }

    /**
     * Store a file in a subdirectory of the upload directory
     */
    public String storeFile(MultipartFile file, String subDirectory) {
        try {
            if (file.isEmpty()) {
                throw new RuntimeException("Failed to store empty file");
            }

            // Create the upload directory with subdirectory if it doesn't exist
            Path uploadPath = Paths.get(uploadDir, subDirectory);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                System.out.println("Created upload subdirectory: " + uploadPath.toAbsolutePath());
            }

            // Generate a unique file name
            String originalFileName = file.getOriginalFilename();
            String fileExtension = "";
            if (originalFileName != null && originalFileName.contains(".")) {
                fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
            }
            String fileName = UUID.randomUUID().toString() + fileExtension;

            // Copy the file to the upload directory
            Path targetLocation = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            System.out.println("File saved to: " + targetLocation.toAbsolutePath());
            
            // Return the URL path that can be used to access the file
            return "/uploads/" + subDirectory + "/" + fileName;
        } catch (IOException ex) {
            ex.printStackTrace();
            throw new RuntimeException("Could not store file " + file.getOriginalFilename() + " in subdirectory " + subDirectory, ex);
        }
    }

    /**
     * Delete a file from the upload directory
     */
    public boolean deleteFile(String filePath) {
        try {
            // Extract the filename from the URL path
            String fileName = filePath.substring(filePath.lastIndexOf("/") + 1);
            
            // Create the path to the file
            Path file = Paths.get(uploadDir, fileName);
            
            System.out.println("Attempting to delete file: " + file.toAbsolutePath());
            
            // Check if the file exists and delete it
            if (Files.exists(file)) {
                Files.delete(file);
                System.out.println("File deleted successfully");
                return true;
            }
            System.out.println("File does not exist: " + file.toAbsolutePath());
            return false;
        } catch (IOException ex) {
            ex.printStackTrace();
            throw new RuntimeException("Could not delete file " + filePath, ex);
        }
    }
    
    /**
     * Delete a file from a subdirectory of the upload directory
     */
    public boolean deleteFileFromSubDirectory(String filePath) {
        try {
            // Extract the subdirectory and filename from the URL path
            String relativePath = filePath.substring("/uploads/".length());
            
            // Create the path to the file
            Path file = Paths.get(uploadDir, relativePath);
            
            System.out.println("Attempting to delete file: " + file.toAbsolutePath());
            
            // Check if the file exists and delete it
            if (Files.exists(file)) {
                Files.delete(file);
                System.out.println("File deleted successfully");
                return true;
            }
            System.out.println("File does not exist: " + file.toAbsolutePath());
            return false;
        } catch (IOException ex) {
            ex.printStackTrace();
            throw new RuntimeException("Could not delete file " + filePath, ex);
        }
    }
}