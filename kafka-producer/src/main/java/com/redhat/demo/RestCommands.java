package com.redhat.demo;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;

@Path("/")
public class RestCommands {
    @Inject
    Instance<RecordGenerator> instanceGen;

    @PUT
    public void setFailure() {
        instanceGen.get().setFailure(true);
    }
}
