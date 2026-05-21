package com.youssefhenna.model;

import io.fabric8.generator.annotation.Required;

public class PollConfig {
    public PollConfig(){}

    public PollConfig(int every, Unit unit) {
        this.every = every;
        this.unit = unit;
    }

    @Required
    private int every;

    @Required
    private Unit unit;

    public int getEvery() {
        return every;
    }

    public void setEvery(int every) {
        this.every = every;
    }

    public Unit getUnit() {
        return unit;
    }

    public void setUnit(Unit unit) {
        this.unit = unit;
    }

    public enum Unit {
        DAYS,
        HOURS,
        MINUTES
    }

}
