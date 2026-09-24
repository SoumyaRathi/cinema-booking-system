package com.movie.movieticket.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/debug")
public class DebugController {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @GetMapping("/uploads")
    public String debugUploads(Model model) {
        File uploadDirectory = new File(uploadDir);
        List<String> files = new ArrayList<>();
        
        if (uploadDirectory.exists() && uploadDirectory.isDirectory()) {
            scanDirectory(uploadDirectory, files, "");
        }
        
        model.addAttribute("uploadPath", uploadDirectory.getAbsolutePath());
        model.addAttribute("files", files);
        
        return "debug-uploads";
    }
    
    private void scanDirectory(File directory, List<String> files, String path) {
        File[] fileList = directory.listFiles();
        if (fileList != null) {
            for (File file : fileList) {
                if (file.isFile()) {
                    files.add(path + "/" + file.getName());
                } else if (file.isDirectory()) {
                    scanDirectory(file, files, path + "/" + file.getName());
                }
            }
        }
    }
}