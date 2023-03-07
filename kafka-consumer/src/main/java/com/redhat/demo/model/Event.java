package com.redhat.demo.model;

import javax.persistence.Entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;

@Entity
public class Event extends PanacheEntity {

    public Long key;
    public String message;
}