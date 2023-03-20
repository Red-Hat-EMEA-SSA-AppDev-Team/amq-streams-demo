package com.redhat.demo;

public abstract class AbstractRecordGenerator implements RecordGenerator {

    private boolean failure;

    public boolean getFailure() {
        return failure;
    }

    @Override
    public void setFailure(boolean failure) {
        this.failure = failure;
    }

    protected void failureSimulation() {
        if (getFailure()) {
            setFailure(false);
            throw new RuntimeException();
        }
    }
}