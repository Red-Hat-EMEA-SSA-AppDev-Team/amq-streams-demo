package com.redhat.demo.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;

@Entity
@IdClass(KafkaStateId.class)
public class KafkaState extends PanacheEntityBase {
    @Id
    public String topic;

    @Id
    public int partition;

    @Id
    public long offsetN;
}