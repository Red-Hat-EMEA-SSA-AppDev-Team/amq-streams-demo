package com.redhat.demo;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import io.smallrye.mutiny.Uni;

public interface TrackingService {
    public Uni<Void> track(ConsumerRecord<Long, String> record);
}
