package com.quickserv.quickserv.exception;

/**
 * Exception thrown when booking state transition is invalid
 */
public class InvalidBookingStateException extends BookingException {

    private String currentState;
    private String requestedState;

    public InvalidBookingStateException(String message) {
        super(message, "INVALID_BOOKING_STATE");
    }

    public InvalidBookingStateException(String currentState, String requestedState) {
        super("Cannot transition from " + currentState + " to " + requestedState, "INVALID_BOOKING_STATE");
        this.currentState = currentState;
        this.requestedState = requestedState;
    }

    public String getCurrentState() { return currentState; }
    public String getRequestedState() { return requestedState; }
}

