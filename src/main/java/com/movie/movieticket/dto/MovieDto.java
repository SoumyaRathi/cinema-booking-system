package com.movie.movieticket.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public class MovieDto {

    private Long id;
    
    @NotEmpty(message = "Title cannot be empty")
    private String title;
    
    @NotEmpty(message = "Description cannot be empty")
    private String description;
    
    @NotEmpty(message = "Genre cannot be empty")
    private String genre;
    
    @NotNull(message = "Duration cannot be null")
    @Min(value = 1, message = "Duration must be at least 1 minute")
    private Integer duration;
    
    private String director;
    
    private String cast;
    
    private String imageUrl;
    
    // NEW: Add imageData field
    private String imageData;
    
    private String trailerUrl;
    
    private boolean released;
    
    private String rating;
    
    private String language;
    
    private Integer screeningDurationDays = 28;

    // Default constructor
    public MovieDto() {
    }

    // Constructor with fields
    public MovieDto(Long id, String title, String description, String genre, Integer duration, 
                   String director, String cast, String imageUrl, String trailerUrl, 
                   boolean released, String rating, String language, Integer screeningDurationDays) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.genre = genre;
        this.duration = duration;
        this.director = director;
        this.cast = cast;
        this.imageUrl = imageUrl;
        this.trailerUrl = trailerUrl;
        this.released = released;
        this.rating = rating;
        this.language = language;
        this.screeningDurationDays = screeningDurationDays != null ? screeningDurationDays : 28;
    }

    // Getters and Setters
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
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

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    // NEW: Image data getter and setter
    public String getImageData() {
        return imageData;
    }

    public void setImageData(String imageData) {
        this.imageData = imageData;
    }
    
    // NEW: Add getEffectiveImageUrl method to match the one in Movie entity
    public String getEffectiveImageUrl() {
        if (imageData != null && !imageData.isEmpty()) {
            return "data:image/jpeg;base64," + imageData;
        } else if (imageUrl != null && !imageUrl.isEmpty()) {
            return imageUrl;
        }
        return null;
    }

    public String getTrailerUrl() {
        return trailerUrl;
    }

    public void setTrailerUrl(String trailerUrl) {
        this.trailerUrl = trailerUrl;
    }

    public boolean isReleased() {
        return released;
    }

    public void setReleased(boolean released) {
        this.released = released;
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

    public Integer getScreeningDurationDays() {
        return screeningDurationDays;
    }

    public void setScreeningDurationDays(Integer screeningDurationDays) {
        this.screeningDurationDays = screeningDurationDays;
    }
    
    @Override
    public String toString() {
        return "MovieDto{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", genre='" + genre + '\'' +
                ", duration=" + duration +
                ", director='" + director + '\'' +
                ", rating='" + rating + '\'' +
                ", language='" + language + '\'' +
                ", released=" + released +
                ", hasImageData=" + (imageData != null && !imageData.isEmpty()) +
                '}';
    }
}