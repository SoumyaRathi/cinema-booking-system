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
@RequestMapping("/poster-images")
public class PosterImageController {

    private static final Logger logger = Logger.getLogger(PosterImageController.class.getName());

    @Value("${file.upload-dir}")
    private String uploadDir;

    @GetMapping("/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {
        try {
            // Try to find the file in the posters directory
            Path filePath = Paths.get(uploadDir, "posters", filename);
            File file = filePath.toFile();
            
            logger.info("Looking for image at: " + filePath.toAbsolutePath());
            
            if (!file.exists()) {
                // If not found in posters directory, try the root uploads directory
                filePath = Paths.get(uploadDir, filename);
                file = filePath.toFile();
                logger.info("Not found in posters, trying: " + filePath.toAbsolutePath());
                
                if (!file.exists()) {
                    logger.warning("Image file not found: " + filename);
                    return ResponseEntity.notFound().build();
                }
            }
            
            // Determine content type
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "image/jpeg"; // Default to JPEG if can't determine
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