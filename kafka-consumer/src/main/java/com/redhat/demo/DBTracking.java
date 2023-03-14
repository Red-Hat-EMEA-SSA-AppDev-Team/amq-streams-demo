package com.redhat.demo;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import javax.persistence.PersistenceException;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.hibernate.reactive.mutiny.Mutiny;

import com.redhat.demo.model.Event;
import com.redhat.demo.model.KafkaState;

import io.quarkus.arc.lookup.LookupIfProperty;
import io.smallrye.mutiny.Uni;

@LookupIfProperty(name = "tracking.db", stringValue = "true")
@ApplicationScoped
public class DBTracking implements TrackingService {

    @Inject
    Mutiny.Session session;

    @Override
    @ActivateRequestContext
    public Uni<Void> track(ConsumerRecord<Long, String> record) {
        System.out.println("DBTracking.track() - key: " + record.key());
        return session
                .withTransaction(t -> {
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
                .onFailure(PersistenceException.class).recoverWithNull()
                .onTermination().call(() -> session.close());
    }
}
