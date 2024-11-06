package com.tool.draw.network;

public class PendingMessage {
    private final String type;
    private final int value;

    public PendingMessage(String type, int value) {
        this.type = type;
        this.value = value;
    }

    public String getName() {
        return type;
    }

    public int getCheckNumber() {
        return value;
    }
}

