package com.redhat.demo;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.hibernate.reactive.mutiny.Mutiny;

import com.redhat.demo.model.Event;
import com.redhat.demo.model.KafkaState;

import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class DBTracking {

    @Inject
    Mutiny.Session session;

    @ActivateRequestContext
    public Uni<Void> persist(ConsumerRecord<Long, String> record) {
        return session
                .withTransaction(t -> {
                    KafkaState state = new KafkaState();
                    state.topic = record.topic();
                    state.partition = record.partition();
                    state.offsetN = record.offset();

                    return state.persist();
                })
                .onFailure().recoverWithNull()
                .chain(t -> {
                    if (t != null) {
                        Event event = new Event();
                        event.key = record.key();
                        event.message = record.value();

                        return event.persistAndFlush().replaceWithVoid();
                    } else {
                        return Uni.createFrom().nullItem();
                    }
                })
                .onTermination().call(() -> session.close());
    }
}
