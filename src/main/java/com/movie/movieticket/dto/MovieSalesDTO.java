package com.movie.movieticket.dto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MovieSalesDTO {
    private Long id;
    private String title;
    private String imageUrl;
    private String imageData; // Add this field for base64 image data
    private double revenue;
    private int ticketsSold;
    private String genre; // Original genre field (comma-separated string)
    private List<String> genres; // New field for individual genres
    
    // New fields for enhanced report
    private String director;
    private String cast;
    private Integer releaseYear;
    private String rating;
    private String language;
    private Integer duration;
    
    public MovieSalesDTO() {
        this.revenue = 0.0;
        this.ticketsSold = 0;
        this.genres = new ArrayList<>();
    }
    
    // Constructor with all fields
    public MovieSalesDTO(Long id, String title, String imageUrl, String genre, double revenue, int ticketsSold,
                        String director, String cast, Integer releaseYear, String rating, String language, Integer duration) {
        this();
        this.id = id;
        this.title = title;
        this.imageUrl = imageUrl;
        this.genre = genre;
        this.revenue = revenue;
        this.ticketsSold = ticketsSold;
        this.director = director;
        this.cast = cast;
        this.releaseYear = releaseYear;
        this.rating = rating;
        this.language = language;
        this.duration = duration;
        
        // Parse genres when setting
        if (genre != null && !genre.isEmpty()) {
            this.genres = Arrays.asList(genre.split(",\\s*"));
        }
    }
    
    // Getters and setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getImageUrl() {
        return imageUrl;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    
    // Add imageData getter and setter
    public String getImageData() {
        return imageData;
    }
    
    public void setImageData(String imageData) {
        this.imageData = imageData;
    }
    
    // Updated effectiveImageUrl method to handle base64 data
    public String getEffectiveImageUrl() {
        if (imageData != null && !imageData.trim().isEmpty()) {
            return "data:image/jpeg;base64," + imageData;
        } else if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            return imageUrl;
        }
        return "/images/default-movie-poster.jpg";
    }
    
    public double getRevenue() {
        return revenue;
    }
    
    public void setRevenue(double revenue) {
        this.revenue = revenue;
    }
    
    public int getTicketsSold() {
        return ticketsSold;
    }
    
    public void setTicketsSold(int ticketsSold) {
        this.ticketsSold = ticketsSold;
    }
    
    public String getGenre() {
        return genre;
    }
    
    public void setGenre(String genre) {
        this.genre = genre;
        
        // When setting the genre string, also parse it into the genres list
        if (genre != null && !genre.isEmpty()) {
            this.genres = Arrays.asList(genre.split(",\\s*"));
        }
    }
    
    public List<String> getGenres() {
        return genres;
    }
    
    public void setGenres(List<String> genres) {
        this.genres = genres;
    }
    
    public String getDirector() {
        return director;
    }
    
    public void setDirector(String director) {
        this.director = director;
    }
    
    public String getCast() {
        return cast;
    }
    
    public void setCast(String cast) {
        this.cast = cast;
    }
    
    public Integer getReleaseYear() {
        return releaseYear;
    }
    
    public void setReleaseYear(Integer releaseYear) {
        this.releaseYear = releaseYear;
    }
    
    public String getRating() {
        return rating;
    }
    
    public void setRating(String rating) {
        this.rating = rating;
    }
    
    public String getLanguage() {
        return language;
    }
    
    public void setLanguage(String language) {
        this.language = language;
    }
    
    public Integer getDuration() {
        return duration;
    }
    
    public void setDuration(Integer duration) {
        this.duration = duration;
    }
    
    // Helper method to check if this movie has a specific genre
    public boolean hasGenre(String genreToCheck) {
        if (genreToCheck == null || genreToCheck.isEmpty() || genreToCheck.equalsIgnoreCase("All")) {
            return true;
        }
        
        // Check if the genre is in the parsed list
        if (genres != null && !genres.isEmpty()) {
            return genres.stream()
                    .anyMatch(g -> g.trim().equalsIgnoreCase(genreToCheck.trim()));
        }
        
        // Fallback to checking the original genre string
        if (genre != null && !genre.isEmpty()) {
            return Arrays.stream(genre.split(",\\s*"))
                    .anyMatch(g -> g.trim().equalsIgnoreCase(genreToCheck.trim()));
        }
        
        return false;
    }
    
    // Helper method to get cast as a list
    public List<String> getCastList() {
        if (cast == null || cast.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.asList(cast.split(",\\s*"));
    }
    
    @Override
    public String toString() {
        return "MovieSalesDTO{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", genre='" + genre + '\'' +
                ", director='" + director + '\'' +
                ", revenue=" + revenue +
                ", ticketsSold=" + ticketsSold +
                ", releaseYear=" + releaseYear +
                ", hasImageData=" + (imageData != null && !imageData.isEmpty()) +
                '}';
    }
}