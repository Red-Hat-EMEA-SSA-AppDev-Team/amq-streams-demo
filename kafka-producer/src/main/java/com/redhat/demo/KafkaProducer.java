package com.redhat.demo;

import java.time.Duration;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

import io.smallrye.mutiny.Multi;
import io.smallrye.reactive.messaging.kafka.KafkaRecord;

@ApplicationScoped
public class KafkaProducer {

    @ConfigProperty(name = "producer.tick-frequency", defaultValue = "1000")
    private Long tickFrequency;

    @Inject
    Instance<RecordGenerator> generator;

    @Outgoing("event-out")
    public Multi<KafkaRecord<Long, String>> generate() {
        return Multi.createFrom().ticks().every(Duration.ofMillis(tickFrequency))
                .onOverflow().buffer(1000)
                .onOverflow()
                    .invoke(tick -> System.out.println("buffer overflow tick: " + tick))
                    .drop()
                .map(generator.get()::createRecord);
    }
}
