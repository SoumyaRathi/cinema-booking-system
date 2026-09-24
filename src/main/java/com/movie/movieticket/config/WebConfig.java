package com.movie.movieticket.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.resource.PathResourceResolver;

import jakarta.servlet.ServletContext;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    @Autowired
    private ServletContext servletContext;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Get the actual context path from servlet context
        String contextPath = servletContext.getContextPath();
        System.out.println("=== WAR DEPLOYMENT CONFIGURATION ===");
        System.out.println("Application context path: " + contextPath);
        System.out.println("Upload directory configured: " + uploadDir);

        // Create upload directory structure
        File uploadDirectory;
        
        // Handle different deployment scenarios
        if (uploadDir.contains("${catalina.home}")) {
            // For Tomcat deployment, use catalina.home if available
            String catalinaHome = System.getProperty("catalina.home");
            if (catalinaHome != null) {
                uploadDirectory = new File(catalinaHome, "webapps/uploads");
            } else {
                // Fallback to user.home or current directory
                String userHome = System.getProperty("user.home");
                uploadDirectory = new File(userHome, "uploads");
                System.out.println("Catalina home not found, using fallback: " + uploadDirectory.getAbsolutePath());
            }
        } else {
            uploadDirectory = new File(uploadDir);
        }

        // Create directories if they don't exist
        if (!uploadDirectory.exists()) {
            boolean created = uploadDirectory.mkdirs();
            System.out.println("Created upload directory: " + uploadDirectory.getAbsolutePath() + " - Success: " + created);
        }

        File postersDirectory = new File(uploadDirectory, "posters");
        if (!postersDirectory.exists()) {
            boolean created = postersDirectory.mkdirs();
            System.out.println("Created posters directory: " + postersDirectory.getAbsolutePath() + " - Success: " + created);
        }

        // Get absolute path and normalize
        String absolutePath = uploadDirectory.getAbsolutePath();
        absolutePath = Paths.get(absolutePath).normalize().toString();
        
        System.out.println("Final upload directory path: " + absolutePath);

        // Configure file URL for resource handler
        String fileUrl = "file:" + absolutePath + File.separator;
        if (!fileUrl.endsWith("/") && !fileUrl.endsWith("\\")) {
            fileUrl += "/";
        }
        
        System.out.println("File URL for resource handler: " + fileUrl);

        // Configure resource handler for uploads
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(fileUrl)
                .setCachePeriod(3600)
                .resourceChain(true)
                .addResolver(new PathResourceResolver());

        // Also add a handler that includes the context path for WAR deployment
        if (!contextPath.isEmpty()) {
            registry.addResourceHandler(contextPath + "/uploads/**")
                    .addResourceLocations(fileUrl)
                    .setCachePeriod(3600)
                    .resourceChain(true)
                    .addResolver(new PathResourceResolver());
        }

        // Add other static resource handlers
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(3600)
                .resourceChain(true);

        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/")
                .setCachePeriod(3600)
                .resourceChain(true);

        registry.addResourceHandler("/css/**")
                .addResourceLocations("classpath:/static/css/")
                .setCachePeriod(3600)
                .resourceChain(true);

        registry.addResourceHandler("/js/**")
                .addResourceLocations("classpath:/static/js/")
                .setCachePeriod(3600)
                .resourceChain(true);

        System.out.println("=== RESOURCE HANDLERS CONFIGURED ===");
        testUploadDirectory(uploadDirectory);
    }
    
    private void testUploadDirectory(File uploadDirectory) {
        try {
            System.out.println("=== UPLOAD DIRECTORY TEST ===");
            System.out.println("Directory exists: " + uploadDirectory.exists());
            System.out.println("Directory is readable: " + uploadDirectory.canRead());
            System.out.println("Directory is writable: " + uploadDirectory.canWrite());
            System.out.println("Directory absolute path: " + uploadDirectory.getAbsolutePath());
            
            File postersDir = new File(uploadDirectory, "posters");
            System.out.println("Posters directory exists: " + postersDir.exists());
            System.out.println("Posters directory is readable: " + postersDir.canRead());
            System.out.println("Posters directory absolute path: " + postersDir.getAbsolutePath());
            
            if (postersDir.exists()) {
                File[] files = postersDir.listFiles();
                System.out.println("Number of files in posters directory: " + (files != null ? files.length : 0));
                if (files != null && files.length > 0) {
                    System.out.println("Sample files:");
                    for (int i = 0; i < Math.min(5, files.length); i++) {
                        System.out.println("  - " + files[i].getName() + " (size: " + files[i].length() + " bytes)");
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error testing upload directory: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("forward:/index");
        registry.addViewController("/home").setViewName("forward:/index");
        registry.addViewController("/debug/context").setViewName("forward:/session-info");
    }
}