package com.movie.movieticket.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "movies")
public class Movie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;
    
    @Column(length = 1000)
    private String description;
    
    private String genre;
    
    @Column(name = "duration_minutes")
    private Integer durationMinutes;
    
    private String director;
    
    private String cast;
    
    @Column(name = "image_url")
    private String imageUrl;
    
    // NEW: Add image data field for storing Base64 images
    @Lob
    @Column(name = "image_data", columnDefinition = "LONGTEXT")
    private String imageData;
    
    @Column(name = "trailer_url")
    private String trailerUrl;
    
    private boolean released;
    
    private String rating;
    
    private String language;
    
    @Column(name = "screening_duration_days")
    private Integer screeningDurationDays = 28;
    
    @OneToMany(mappedBy = "movie", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Screening> screenings = new ArrayList<>();

    // Default constructor
    public Movie() {
    }

    // Constructor with fields
    public Movie(String title, String description, String genre, Integer durationMinutes, 
                String director, String cast, String imageUrl, String trailerUrl, 
                boolean released, String rating, String language, Integer screeningDurationDays) {
        this.title = title;
        this.description = description;
        this.genre = genre;
        this.durationMinutes = durationMinutes;
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
        return durationMinutes;
    }

    public void setDuration(Integer duration) {
        this.durationMinutes = duration;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
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

    // NEW: Helper method to get the effective image URL (prioritizes database image)
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

    public List<Screening> getScreenings() {
        return screenings;
    }

    public void setScreenings(List<Screening> screenings) {
        this.screenings = screenings;
    }

    public void addScreening(Screening screening) {
        screenings.add(screening);
        screening.setMovie(this);
    }

    public void removeScreening(Screening screening) {
        screenings.remove(screening);
        screening.setMovie(null);
    }
    
    @Override
    public String toString() {
        return "Movie{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", genre='" + genre + '\'' +
                ", durationMinutes=" + durationMinutes +
                ", director='" + director + '\'' +
                ", rating='" + rating + '\'' +
                ", language='" + language + '\'' +
                ", released=" + released +
                ", screeningDurationDays=" + screeningDurationDays +
                ", hasImageData=" + (imageData != null && !imageData.isEmpty()) +
                '}';
    }
}