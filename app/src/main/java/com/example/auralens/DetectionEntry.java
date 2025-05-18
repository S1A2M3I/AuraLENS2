package com.example.auralens;

import java.util.Date;

public class DetectionEntry {
    private final String label;
    private final String timestamp;

    public DetectionEntry(String label, String timestamp) {
        this.label = label;
        this.timestamp = timestamp;
    }

    // Required getter methods
    public String getLabel() {
        return label;
    }

    public String getTimestamp() {
        return timestamp;
    }
}