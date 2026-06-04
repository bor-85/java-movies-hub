package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.HashMap;
import java.util.Map;

public class MoviesStore {
    private static final int DEFAULT_COUNTER = 1;
    private final Map<Integer, Movie> movies = new HashMap<>();
    private int idCounter = DEFAULT_COUNTER;

    public Movie addMovie(String title, int year) {
        Movie movie = new Movie(idCounter, title, year);
        movies.put(idCounter, movie);
        idCounter++;
        return movie;
    }

    public Map<Integer, Movie> getMovies() {
        return movies;
    }

    public Movie getMovieById(int id) {
        return movies.get(id);
    }

    public boolean deleteMovieById(int id) {
        return movies.remove(id) != null;
    }

    public void clearStorage() {
        movies.clear();
        idCounter = DEFAULT_COUNTER;
    }
}