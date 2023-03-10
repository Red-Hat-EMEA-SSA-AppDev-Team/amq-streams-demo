package com.redhat.demo;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Incoming;

import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class EventConsumer {

    private boolean failure;

    @ConfigProperty(name = "consumer.kind", defaultValue = "in-mem")
    private String kind;

    @Inject
    InMemTracking inMemTracking;

    @Inject
    DBTracking dbTracking;

    public boolean isFailure() {
        return failure;
    }

    public void setFailure(boolean failure) {
        this.failure = failure;
    }

    @Incoming("event")
    public Uni<Void> consume(ConsumerRecord<Long, String> record) {

        Uni<Void> result = null;

        if ("in-mem".equalsIgnoreCase(kind)) {
            result = Uni.createFrom().item(record).invoke(r -> {
                inMemTracking.track(r.key());
            }).replaceWithVoid();
        } else {
            result = dbTracking.persist(record);
        }

        if (isFailure()) {
            setFailure(false);
            throw new RuntimeException();
        }
        return result;
    }


}