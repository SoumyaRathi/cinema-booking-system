package com.movie.movieticket.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

@Controller
@RequestMapping("/direct-image")
public class DirectImageController {

    private static final Logger logger = Logger.getLogger(DirectImageController.class.getName());

    @Value("${file.upload-dir}")
    private String uploadDir;

    @GetMapping("/poster/{filename}")
    @ResponseBody
    public ResponseEntity<Resource> getPosterImage(@PathVariable String filename) {
        try {
            // Create the path to the file
            Path filePath = Paths.get(uploadDir, "posters", filename);
            File file = filePath.toFile();
            
            logger.info("Attempting to serve image directly: " + filePath.toAbsolutePath());
            
            if (!file.exists()) {
                logger.warning("Image file not found: " + filePath.toAbsolutePath());
                return ResponseEntity.notFound().build();
            }
            
            // Determine content type
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            
            Resource resource = new FileSystemResource(file);
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);
        } catch (IOException e) {
            logger.severe("Error serving image: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}