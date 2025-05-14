package models;

import java.io.Serializable;

public class Movie implements Serializable{
    private int movieId;
    private String title;
    private int duration; // in minutes
    private String description;
    private String genre;
    private String director;
    private int releaseYear;
    private String language;

    public Movie(int movieId, String title, int duration, String description, String genre, String director, int releaseYear, String language) {
        this.movieId = movieId;
        this.title = title;
        this.duration = duration;
        this.description = description;
        this.genre = genre;
        this.director = director;
        this.releaseYear = releaseYear;
        this.language = language;
    }

    // Getters
    public int getMovieId() {
        return movieId;
    }

    public String getTitle() {
        return title;
    }

    public int getDuration() {
        return duration;
    }

    public String getDescription() {
        return description;
    }

    public String getGenre() {
        return genre;
    }

    public String getDirector() {
        return director;
    }

    public int getReleaseYear() {
        return releaseYear;
    }

    public String getLanguage() {
        return language;
    }

    // Setters
    public void setMovieId(int movieId) {
        this.movieId = movieId;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public void setDirector(String director) {
        this.director = director;
    }

    public void setReleaseYear(int releaseYear) {
        this.releaseYear = releaseYear;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    @Override
    public String toString() {
        return "Movie{" +
                "movieId=" + movieId +
                ", title='" + title + '\'' +
                ", duration=" + duration +
                ", description='" + description + '\'' +
                ", genre='" + genre + '\'' +
                ", director='" + director + '\'' +
                ", releaseYear=" + releaseYear +
                ", language='" + language + '\'' +
                '}';
    }
}
