package com.redhat.demo;

import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.faulttolerance.Retry;

import io.quarkus.arc.lookup.LookupUnlessProperty;
import io.smallrye.reactive.messaging.kafka.KafkaRecord;

@ApplicationScoped
@LookupUnlessProperty(name = "sequence.db", stringValue = "true")
public class InMemRecordGenerator extends AbstractRecordGenerator {

    private static Long lastKey = 0L;

    @ConfigProperty(name = "producer.partitions", defaultValue = "")
    private String partitions;

    @ConfigProperty(name = "producer.parted", defaultValue = "false")
    private Boolean parted;

    List<Integer> partitionList = null;
    
    public List<Integer> getPartitionList() {
        if (partitionList == null) {
            if (parted && partitions != null && partitions.length() > 0 )
                partitionList = Arrays.stream(partitions.split(",")).map(Integer::parseInt).collect(Collectors.toList());
        }
        return partitionList;
    }

    @Retry(maxRetries = 2, delay = 1, delayUnit = ChronoUnit.SECONDS)
    @Override
    public KafkaRecord<Long, String> createRecord(Long tick) {
        // workaround to avoid other events to surpass the first
        if (lastKey == 1)
            try {
                Thread.sleep(3000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        lastKey++;
        if (parted == false) {
            System.out.println("Generating message key: " + lastKey);
            failureSimulation();
            return KafkaRecord.of(lastKey, "demo message " + lastKey);
        } else {
            int iteration = lastKey.intValue() % getPartitionList().size();
            int partition = getPartitionList().get(iteration);
            System.out.println("Generating message counter: " + lastKey + " on partition: " + partition);
            failureSimulation();
            return KafkaRecord.of(null, Long.valueOf(partition), "demo message " + lastKey, null,
                    partition);
        }
    }
}
