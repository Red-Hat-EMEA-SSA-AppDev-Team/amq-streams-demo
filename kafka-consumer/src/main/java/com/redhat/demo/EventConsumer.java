package com.redhat.demo;

import java.util.TreeMap;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.PersistenceException;
import javax.transaction.Transactional;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.eclipse.microprofile.reactive.messaging.Incoming;

import com.redhat.demo.model.Event;
import com.redhat.demo.model.KafkaState;

@ApplicationScoped
public class EventConsumer {

    private static TreeMap<Long, Boolean> check = new TreeMap<>();
    private static Long last = 1L;
    private static Long duplicated = 0L;
    private boolean failure;

    public boolean isFailure() {
        return failure;
    }

    public void setFailure(boolean failure) {
        this.failure = failure;
    }

    @Incoming("event")
    @Transactional
    public void consume(ConsumerRecord<Long, String> record) {
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

        if (key < last && !check.containsKey(key))
            duplicated++;

        if (key >= last)
            last = key + 1L;

        check.remove(key);

        System.out.println(String.format("Current Key: %d, Missing messages: %d, Duplicated msg: %d", key, check.size(),
                duplicated));

        persist(record);
        if (isFailure()) {
            setFailure(false);
            throw new RuntimeException();
        }
    }

    public void persist(ConsumerRecord<Long, String> record) {
        try {

            KafkaState state = new KafkaState();
            state.topic = record.topic();
            state.partition = record.partition();
            state.offsetN = record.offset();
            state.persist();
            
            Event event = new Event();
            event.key = record.key();
            event.message = record.value();

            event.persistAndFlush();
        } catch (PersistenceException pe) {
            System.out.println(">>> "+pe);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}