package com.redhat.demo;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import com.redhat.demo.model.Event;
import com.redhat.demo.model.KafkaState;

import io.quarkus.arc.lookup.LookupIfProperty;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.persistence.PersistenceException;

@LookupIfProperty(name = "tracking.db", stringValue = "true")
@ApplicationScoped
public class DBTracking implements TrackingService {

    @Override
    @ActivateRequestContext
    @WithTransaction
    public Uni<Void> track(ConsumerRecord<Long, String> record) {
        System.out.println("DBTracking.track() - key: " + record.key());
        
        return Panache.withTransaction( () -> {
                    KafkaState state = new KafkaState();
                    state.topic = record.topic();
                    state.partition = record.partition();
                    state.offsetN = record.offset();

                    return state.persist();
                })
                .chain(t -> {
                    Event event = new Event();
                    event.key = record.key();
                    event.message = record.value();

                    return event.persistAndFlush().replaceWithVoid();
                })
                .onFailure(PersistenceException.class).transform(m -> {
                    System.err.println(">>> " + m);
                    if (m.getMessage().contains("duplicate"))
                        return m;
                    else
                        return m.getCause();
                })
                .onFailure(PersistenceException.class).recoverWithNull();
    }
}
