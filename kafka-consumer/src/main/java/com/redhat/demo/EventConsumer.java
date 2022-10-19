package com.redhat.demo;

import java.util.TreeMap;

import javax.enterprise.context.ApplicationScoped;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.eclipse.microprofile.reactive.messaging.Incoming;

@ApplicationScoped
public class EventConsumer {

    private TreeMap<Long, Boolean> check = new TreeMap<>();
    private Long last = 0L;
    
    @Incoming("event")
    public void consume(ConsumerRecord<Long, String> record) {
        Long key = record.key(); // Can be `null` if the incoming record has no key
        String value = record.value(); // Can be `null` if the incoming record has no value
        String topic = record.topic();
        int partition = record.partition();

        System.out.println(key);
        // for (long i = last; i < key; i++) {
        //     check.put(i, false);
        // }
        // check.put(key, true);
        // System.out.println("key:" + key);
        // SortedMap<Long, Boolean> headMap = check.headMap(last);
        // for (Long entry : headMap.keySet()) {
        //     if (check.get(entry)== true) {
        //         check.remove(entry);
        //     }
        // }
        // last=key;
        // System.out.println("Missing messages: "+check.values().stream().filter(v -> v == false).count());
    }
}
