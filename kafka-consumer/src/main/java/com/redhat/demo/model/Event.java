package com.redhat.demo.model;

import javax.persistence.Entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
public class Event extends PanacheEntity {

    public Long key;
    public String message;
}