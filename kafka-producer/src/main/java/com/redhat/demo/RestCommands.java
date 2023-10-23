package com.redhat.demo;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;

@Path("/")
public class RestCommands {
    @Inject
    Instance<RecordGenerator> instanceGen;

    @PUT
    public void setFailure() {
        instanceGen.get().setFailure(true);
    }
}
