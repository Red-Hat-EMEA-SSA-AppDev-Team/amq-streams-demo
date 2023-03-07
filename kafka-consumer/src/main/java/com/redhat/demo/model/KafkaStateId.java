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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((topic == null) ? 0 : topic.hashCode());
        result = prime * result + partition;
        result = prime * result + (int) (offsetN ^ (offsetN >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        KafkaStateId other = (KafkaStateId) obj;
        if (topic == null) {
            if (other.topic != null)
                return false;
        } else if (!topic.equals(other.topic))
            return false;
        if (partition != other.partition)
            return false;
        if (offsetN != other.offsetN)
            return false;
        return true;
    }
}