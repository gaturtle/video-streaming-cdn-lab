package com.streaminglab.origin;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class NoSuchVideoException extends RuntimeException {

    public NoSuchVideoException(String id) {
        super("No such video: " + id);
    }
}
