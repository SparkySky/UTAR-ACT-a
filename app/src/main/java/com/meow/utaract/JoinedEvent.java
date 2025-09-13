package com.meow.utaract;

import com.meow.utaract.utils.Event;

public class JoinedEvent {
    private Event event;
    private String registrationStatus;
    private String ticketCode;

    public JoinedEvent(Event event, String registrationStatus, String ticketCode) {
        this.event = event;
        this.registrationStatus = registrationStatus;
        this.ticketCode = ticketCode;
    }

    public Event getEvent() {
        return event;
    }

    public String getRegistrationStatus() {
        return registrationStatus;
    }

    public String getTicketCode() {
        return ticketCode;
    }
}