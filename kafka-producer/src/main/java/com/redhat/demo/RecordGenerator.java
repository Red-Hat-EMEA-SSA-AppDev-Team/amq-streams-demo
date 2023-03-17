package com.redhat.demo;

import io.smallrye.reactive.messaging.kafka.KafkaRecord;

public interface RecordGenerator {
    public void setFailure(boolean b);
    
    public KafkaRecord<Long,String> createRecord(Long tick);
}
