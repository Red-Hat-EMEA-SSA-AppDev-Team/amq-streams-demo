package com.redhat.demo;

import javax.inject.Inject;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;

@Path("/")
public class RestCommands {
    @Inject
    EventConsumer eventConsumer;

    @PUT
    public void setFailure() {
        eventConsumer.setFailure(true);
    }
}
