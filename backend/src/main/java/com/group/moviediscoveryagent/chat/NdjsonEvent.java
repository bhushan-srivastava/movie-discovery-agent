package com.group.moviediscoveryagent.chat;

public class NdjsonEvent {
    private String eventType;
    private Object data;

    public NdjsonEvent() {}

    public NdjsonEvent(String eventType, Object data) {
        this.eventType = eventType;
        this.data = data;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}

