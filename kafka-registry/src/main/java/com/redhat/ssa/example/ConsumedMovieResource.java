package com.redhat.ssa.example;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.jboss.resteasy.reactive.RestStreamElementType;

import com.redhat.quarkus.kafka.Movie;

import io.smallrye.mutiny.Multi;

/**
 * REST resource for consuming movies from Kafka and streaming them to clients.
 * This resource provides a Server-Sent Events (SSE) endpoint that streams
 * movies consumed from a Kafka topic in real-time.
 */
@ApplicationScoped
@Path("/consumed-movies")
public class ConsumedMovieResource {
    
    /**
     * Multi stream of movies consumed from Kafka.
     */
    @Channel("movies-from-kafka")
    Multi<Movie> movies;

    /**
     * Streams movies consumed from Kafka to clients.
     * @return A Multi stream of Movie objects.
     */
    @GET
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.TEXT_PLAIN)
    public Multi<String> stream() {
        // Map each Movie object to its string representation for streaming to clients
        return movies.map(movie -> movie.toString());
    }
}
