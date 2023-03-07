package com.redhat.demo;

import java.util.TreeMap;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.hibernate.reactive.mutiny.Mutiny;

import com.redhat.demo.model.Event;
import com.redhat.demo.model.KafkaState;

import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class EventConsumer {

    private static TreeMap<Long, Boolean> check = new TreeMap<>();
    private static Long last = 1L;
    private static Long duplicated = 0L;
    private boolean failure;

    @Inject
    Mutiny.Session session;

    public boolean isFailure() {
        return failure;
    }

    public void setFailure(boolean failure) {
        this.failure = failure;
    }

    @Incoming("event")
    public Uni<Void> consume(ConsumerRecord<Long, String> record) {
        Long key = record.key(); // Can be `null` if the incoming record has no key

        // reset message
        if (key == 1) {
            last = 1L;
            duplicated = 0L;
            check.clear();
        }

        for (long i = last; i < key; i++) {
            check.put(i, false);
        }

        duplicated++;

        if (key >= last)
            last = key + 1L;
        if (key < last && !check.containsKey(key))

            check.remove(key);

        System.out.println(String.format("Current Key: %d, Missing messages: %d, Duplicated msg: %d", key, check.size(),
                duplicated));

        var result = persist(record);
        if (isFailure()) {
            setFailure(false);
            throw new RuntimeException();
        }
        return result;
    }

    @ActivateRequestContext
    public Uni<Void> persist(ConsumerRecord<Long, String> record) {
        return session
        .withTransaction(t -> {
            KafkaState state = new KafkaState();
            state.topic = record.topic();
            state.partition = record.partition();
            state.offsetN = record.offset();
            
            return state.persist().replaceWithVoid();
        })
        .chain(none -> {
            Event event = new Event();
            event.key = record.key();
            event.message = record.value();

            return event.persist().replaceWithVoid();
        })
        .onTermination()
                .call(() -> session.close());
    }
}