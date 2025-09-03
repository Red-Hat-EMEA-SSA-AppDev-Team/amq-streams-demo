package com.redhat.ssa.example;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.jboss.logging.Logger;

import com.redhat.quarkus.kafka.Movie;

/**
 * REST resource for enqueuing movies to Kafka.
 * This resource provides a POST endpoint that allows clients to send
 * Movie objects to a Kafka topic.
 */
@Path("/movies")
public class MovieResource {
    private static final Logger LOGGER = Logger.getLogger(MovieResource.class);

    /**
     * Emitter for sending movies to Kafka.
     */
    @Channel("movies")
    Emitter<Movie> emitter;

    /**
     * Enqueues a movie to Kafka.
     * @param movie The Movie object to enqueue.
     * @return A Response object indicating the status of the operation.
     */
    @POST
    public Response enqueueMovie(Movie movie) {
        LOGGER.infof("Sending movie %s to Kafka", movie.getTitle());
        emitter.send(movie);
        return Response.accepted().build();
    }
}
