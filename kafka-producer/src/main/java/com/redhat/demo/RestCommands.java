package com.redhat.demo;

import javax.inject.Inject;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;

@Path("/")
public class RestCommands {
    @Inject
    KafkaProducer kafkaProducer;

    @PUT
    public void setFailure() {
        kafkaProducer.setFailure(true);
    }
}
