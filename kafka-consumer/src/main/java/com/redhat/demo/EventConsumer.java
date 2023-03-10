package com.redhat.demo;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.eclipse.microprofile.reactive.messaging.Incoming;

import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class EventConsumer {

    private boolean failure;

    @Inject
    Instance<TrackingService> tracking;

    public boolean isFailure() {
        return failure;
    }

    public void setFailure(boolean failure) {
        this.failure = failure;
    }

    @Incoming("event")
    public Uni<Void> consume(ConsumerRecord<Long, String> record) {

        Uni<Void> result = tracking.get().track(record);

        if (isFailure()) {
            setFailure(false);
            throw new RuntimeException();
        }

        return result;
    }

}