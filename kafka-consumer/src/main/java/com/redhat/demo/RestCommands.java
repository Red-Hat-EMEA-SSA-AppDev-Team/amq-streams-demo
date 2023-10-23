package com.redhat.demo;

import jakarta.inject.Inject;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;

@Path("/")
public class RestCommands {
    @Inject
    EventConsumer eventConsumer;

    @PUT
    public void setFailure() {
        eventConsumer.setFailure(true);
    }
}
