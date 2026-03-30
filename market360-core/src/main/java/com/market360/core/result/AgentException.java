package com.market360.core.result;

public class AgentException extends RuntimeException {

    public AgentException(String message, Throwable cause) {
        super(message, cause);
    }

    public AgentException(String message) {
        super(message);
    }
}
