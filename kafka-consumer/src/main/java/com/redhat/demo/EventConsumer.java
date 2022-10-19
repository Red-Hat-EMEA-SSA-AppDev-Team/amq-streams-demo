package com.redhat.demo;

import java.util.SortedMap;
import java.util.TreeMap;

import javax.enterprise.context.ApplicationScoped;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.eclipse.microprofile.reactive.messaging.Incoming;

@ApplicationScoped
public class EventConsumer {

    private static TreeMap<Long, Boolean> check = new TreeMap<>();
    private static Long last = 1L;
    
    @Incoming("event")
    public void consume(ConsumerRecord<Long, String> record) {
        Long key = record.key(); // Can be `null` if the incoming record has no key

        for (long i = last; i < key; i++) {
            check.put(i, false);
        }
        if (key >= last) last=key+1L;

        check.remove(key);

        System.out.println("key:" + key);
        System.out.println("Missing messages: "+check.size());
    }
}
