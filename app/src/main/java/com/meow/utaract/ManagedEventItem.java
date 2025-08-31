package com.meow.utaract;

import com.meow.utaract.utils.Event;

public class ManagedEventItem {
    private final Event event;
    private int pendingCount;
    private int acceptedCount;
    private int rejectedCount;

    public ManagedEventItem(Event event) {
        this.event = event;
    }

    public Event getEvent() { return event; }
    public int getPendingCount() { return pendingCount; }
    public int getAcceptedCount() { return acceptedCount; }
    public int getRejectedCount() { return rejectedCount; }

    public void setPendingCount(int pendingCount) { this.pendingCount = pendingCount; }
    public void setAcceptedCount(int acceptedCount) { this.acceptedCount = acceptedCount; }
    public void setRejectedCount(int rejectedCount) { this.rejectedCount = rejectedCount; }
}