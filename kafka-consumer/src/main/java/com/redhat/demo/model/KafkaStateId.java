package com.redhat.demo.model;

import java.io.Serializable;

public class KafkaStateId implements Serializable {

    protected String topic;
    protected int partition;
    protected long offsetN;

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public int getPartition() {
        return partition;
    }

    public void setPartition(int partition) {
        this.partition = partition;
    }
    
    public long getOffsetN() {
        return offsetN;
    }

    public void setOffsetN(long offsetN) {
        this.offsetN = offsetN;
    }
}