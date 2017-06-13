package com.rcr2;

public class ScriptParseException extends RuntimeException {
    public ScriptParseException(String message) {
        super(message);
    }

    public ScriptParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
