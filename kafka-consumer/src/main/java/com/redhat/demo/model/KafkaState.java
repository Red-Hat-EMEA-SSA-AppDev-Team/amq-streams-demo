package com.redhat.demo.model;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;

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