package com.redhat.demo;

import java.time.Duration;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

import io.smallrye.mutiny.Multi;
import io.smallrye.reactive.messaging.kafka.KafkaRecord;


@ApplicationScoped
public class KafkaProducer {

    private static Long lastKey=0L;
    
    @ConfigProperty(name = "producer.tick-frequency", defaultValue="1000") 
    private Long tickFrequency;

    @Outgoing("event-out")
    public Multi<KafkaRecord<Long,String>> generate() {
        return Multi.createFrom().ticks().every(Duration.ofMillis(tickFrequency))
            .map(x -> {
                lastKey++;
                return KafkaRecord.of(lastKey, "demo message "+lastKey);
            });
    }
}
