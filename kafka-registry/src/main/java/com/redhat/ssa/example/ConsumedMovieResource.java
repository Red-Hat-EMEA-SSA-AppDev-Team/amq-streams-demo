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

@ApplicationScoped
@Path("/consumed-movies")
public class ConsumedMovieResource {
    
    @Channel("movies-from-kafka")
    Multi<Movie> movies;

    @GET
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.TEXT_PLAIN)
    public Multi<String> stream() {
        return movies.map(movie -> movie.toString());
    }
}
