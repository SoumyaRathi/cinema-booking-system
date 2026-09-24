package com.movie.movieticket.controller;

import com.movie.movieticket.dto.MovieDto;
import com.movie.movieticket.model.Movie;
import com.movie.movieticket.service.MovieService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.logging.Logger;

@Controller
@RequestMapping("/admin/movies")
public class AdminMovieController {

    private static final Logger logger = Logger.getLogger(AdminMovieController.class.getName());
    private static final int MAX_WIDTH = 400;
    private static final int MAX_HEIGHT = 600;
    private static final float COMPRESSION_QUALITY = 0.8f;
    private static final long MAX_IMAGE_SIZE_BYTES = 500 * 1024; // 500KB limit for Base64

    @Autowired
    private MovieService movieService;

    @GetMapping
    public String listMovies(Model model) {
        List<Movie> movies = movieService.getAllMovies();
        
        for (Movie movie : movies) {
            logger.info("Admin Movie List - Movie: " + movie.getTitle() + 
                       ", Has Image Data: " + (movie.getImageData() != null && !movie.getImageData().isEmpty()) +
                       ", Image URL: " + movie.getImageUrl());
        }
        
        model.addAttribute("movies", movies);
        model.addAttribute("active", "movies");
        
        return "movies-admin";
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("movie", new MovieDto());
        return "add-movieadmin";
    }

    @PostMapping("/add")
    public String addMovie(@ModelAttribute MovieDto movie, 
                          @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                          RedirectAttributes redirectAttributes) {
        
        try {
            logger.info("Adding movie: " + movie.getTitle());
            
            // Handle poster file upload with compression
            if (imageFile != null && !imageFile.isEmpty()) {
                logger.info("Processing poster upload for movie: " + movie.getTitle());
                logger.info("Original file name: " + imageFile.getOriginalFilename());
                logger.info("Original file size: " + imageFile.getSize() + " bytes");
                
                try {
                    // Compress and convert image to Base64
                    String compressedBase64Image = compressAndEncodeImage(imageFile);
                    movie.setImageData(compressedBase64Image);
                    
                    logger.info("Poster compressed and converted to Base64 successfully. Final length: " + compressedBase64Image.length());
                } catch (Exception e) {
                    logger.severe("Error processing image: " + e.getMessage());
                    redirectAttributes.addFlashAttribute("error", "Error processing image: " + e.getMessage());
                    return "redirect:/admin/movies/add";
                }
            } else {
                logger.warning("No poster file uploaded or file is empty for movie: " + movie.getTitle());
                movie.setImageData(null);
            }
            
            // Set default values if not provided
            if (movie.getScreeningDurationDays() == null) {
                movie.setScreeningDurationDays(28);
            }
            
            if (movie.getRating() == null || movie.getRating().isEmpty()) {
                movie.setRating("Not Rated");
            }
            
            if (movie.getLanguage() == null || movie.getLanguage().isEmpty()) {
                movie.setLanguage("English");
            }
            
            logger.info("Movie details before saving:");
            logger.info("Title: " + movie.getTitle());
            logger.info("Genre: " + movie.getGenre());
            logger.info("Duration: " + movie.getDuration());
            logger.info("Has Image Data: " + (movie.getImageData() != null && !movie.getImageData().isEmpty()));
            logger.info("Rating: " + movie.getRating());
            logger.info("Language: " + movie.getLanguage());
            logger.info("Released: " + movie.isReleased());
            
            Movie savedMovie = movieService.saveMovie(movie);
            
            logger.info("Movie saved successfully with ID: " + savedMovie.getId());
            logger.info("Saved movie has image data: " + (savedMovie.getImageData() != null && !savedMovie.getImageData().isEmpty()));
            
            redirectAttributes.addFlashAttribute("success", "Movie added successfully!");
            return "redirect:/admin/movies";
        } catch (Exception e) {
            logger.severe("Error adding movie: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error adding movie: " + e.getMessage());
            return "redirect:/admin/movies/add";
        }
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Movie movie = movieService.getMovieById(id);
        if (movie == null) {
            return "redirect:/admin/movies";
        }
        
        // Convert Movie to MovieDto
        MovieDto movieDto = new MovieDto();
        movieDto.setId(movie.getId());
        movieDto.setTitle(movie.getTitle());
        movieDto.setDescription(movie.getDescription());
        movieDto.setGenre(movie.getGenre());
        movieDto.setDuration(movie.getDuration());
        movieDto.setDirector(movie.getDirector());
        movieDto.setCast(movie.getCast());
        movieDto.setImageUrl(movie.getImageUrl());
        movieDto.setImageData(movie.getImageData());
        movieDto.setTrailerUrl(movie.getTrailerUrl());
        movieDto.setReleased(movie.isReleased());
        movieDto.setRating(movie.getRating());
        movieDto.setLanguage(movie.getLanguage());
        movieDto.setScreeningDurationDays(movie.getScreeningDurationDays());
        
        model.addAttribute("movie", movieDto);
        model.addAttribute("movieId", id);
        return "edit-movieadmin";
    }

    @PostMapping("/edit/{id}")
    public String updateMovie(@PathVariable Long id, 
                             @ModelAttribute MovieDto movie,
                             @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                             RedirectAttributes redirectAttributes) {
        
        try {
            logger.info("Updating movie ID: " + id + ", Title: " + movie.getTitle());
            
            // Handle poster file upload with compression
            if (imageFile != null && !imageFile.isEmpty()) {
                logger.info("Processing new poster upload for movie: " + movie.getTitle());
                logger.info("Original file size: " + imageFile.getSize() + " bytes");
                
                try {
                    // Compress and convert new image to Base64
                    String compressedBase64Image = compressAndEncodeImage(imageFile);
                    movie.setImageData(compressedBase64Image);
                    
                    logger.info("New poster compressed and converted to Base64 successfully. Final length: " + compressedBase64Image.length());
                } catch (Exception e) {
                    logger.severe("Error processing new image: " + e.getMessage());
                    redirectAttributes.addFlashAttribute("error", "Error processing image: " + e.getMessage());
                    return "redirect:/admin/movies/edit/" + id;
                }
            } else {
                logger.info("No new poster file uploaded, keeping existing image");
                // Keep the existing image data if no new file is uploaded
                Movie existingMovie = movieService.getMovieById(id);
                if (existingMovie != null && existingMovie.getImageData() != null) {
                    movie.setImageData(existingMovie.getImageData());
                    logger.info("Keeping existing image data");
                }
            }
            
            // Set default values if not provided
            if (movie.getScreeningDurationDays() == null) {
                movie.setScreeningDurationDays(28);
            }
            
            if (movie.getRating() == null || movie.getRating().isEmpty()) {
                movie.setRating("Not Rated");
            }
            
            if (movie.getLanguage() == null || movie.getLanguage().isEmpty()) {
                movie.setLanguage("English");
            }
            
            movie.setId(id); // Ensure ID is set
            
            logger.info("Movie details before updating:");
            logger.info("ID: " + movie.getId());
            logger.info("Title: " + movie.getTitle());
            logger.info("Has Image Data: " + (movie.getImageData() != null && !movie.getImageData().isEmpty()));
            
            Movie updatedMovie = movieService.updateMovie(id, movie);
            
            logger.info("Movie updated successfully. Has image data: " + (updatedMovie.getImageData() != null && !updatedMovie.getImageData().isEmpty()));
            
            redirectAttributes.addFlashAttribute("success", "Movie updated successfully!");
            return "redirect:/admin/movies";
        } catch (Exception e) {
            logger.severe("Error updating movie: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error updating movie: " + e.getMessage());
            return "redirect:/admin/movies/edit/" + id;
        }
    }

    @GetMapping("/delete/{id}")
    public String deleteMovie(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            movieService.deleteMovie(id);
            redirectAttributes.addFlashAttribute("success", "Movie deleted successfully!");
        } catch (Exception e) {
            logger.severe("Error deleting movie: " + e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Error deleting movie: " + e.getMessage());
        }
        return "redirect:/admin/movies";
    }

    /**
     * Compress and encode image to Base64 string
     * This method resizes the image and compresses it to reduce file size
     */
    private String compressAndEncodeImage(MultipartFile imageFile) throws IOException {
        logger.info("Starting image compression process...");
        
        // Read the original image
        BufferedImage originalImage = ImageIO.read(imageFile.getInputStream());
        if (originalImage == null) {
            throw new IOException("Invalid image file or unsupported format");
        }
        
        logger.info("Original image dimensions: " + originalImage.getWidth() + "x" + originalImage.getHeight());
        
        // Calculate new dimensions while maintaining aspect ratio
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();
        
        double aspectRatio = (double) originalWidth / originalHeight;
        int newWidth = originalWidth;
        int newHeight = originalHeight;
        
        // Resize if image is too large
        if (originalWidth > MAX_WIDTH || originalHeight > MAX_HEIGHT) {
            if (aspectRatio > 1) {
                // Landscape orientation
                newWidth = MAX_WIDTH;
                newHeight = (int) (MAX_WIDTH / aspectRatio);
            } else {
                // Portrait orientation
                newHeight = MAX_HEIGHT;
                newWidth = (int) (MAX_HEIGHT * aspectRatio);
            }
        }
        
        logger.info("New image dimensions: " + newWidth + "x" + newHeight);
        
        // Create resized image
        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resizedImage.createGraphics();
        
        // Set rendering hints for better quality
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Draw the resized image
        g2d.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
        g2d.dispose();
        
        // Convert to JPEG and compress
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        // Try different compression qualities if the image is still too large
        float quality = COMPRESSION_QUALITY;
        byte[] imageBytes;
        
        do {
            baos.reset();
            
            // Write compressed JPEG
            var writers = ImageIO.getImageWritersByFormatName("jpg");
            if (!writers.hasNext()) {
                throw new IOException("No JPEG writers available");
            }
            
            var writer = writers.next();
            var ios = ImageIO.createImageOutputStream(baos);
            writer.setOutput(ios);
            
            var param = writer.getDefaultWriteParam();
            param.setCompressionMode(javax.imageio.ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(quality);
            
            writer.write(null, new javax.imageio.IIOImage(resizedImage, null, null), param);
            writer.dispose();
            ios.close();
            
            imageBytes = baos.toByteArray();
            logger.info("Compressed image size at quality " + quality + ": " + imageBytes.length + " bytes");
            
            // If still too large, reduce quality
            if (imageBytes.length > MAX_IMAGE_SIZE_BYTES && quality > 0.3f) {
                quality -= 0.1f;
                logger.info("Image still too large, reducing quality to: " + quality);
            } else {
                break;
            }
        } while (quality > 0.3f);
        
        baos.close();
        
        // Convert to Base64
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);
        
        logger.info("Final compressed image size: " + imageBytes.length + " bytes");
        logger.info("Base64 string length: " + base64Image.length());
        
        // Final check
        if (imageBytes.length > MAX_IMAGE_SIZE_BYTES) {
            logger.warning("Image is still large after compression: " + imageBytes.length + " bytes");
        }
        
        return base64Image;
    }
}