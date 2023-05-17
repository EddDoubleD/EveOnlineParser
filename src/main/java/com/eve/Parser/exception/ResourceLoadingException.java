package com.eve.Parser.exception;

public class ResourceLoadingException extends Exception {

    public ResourceLoadingException(String message) {
        super(message);
    }

    public ResourceLoadingException(String message, Throwable e) {
        super(message, e);
    }
}
