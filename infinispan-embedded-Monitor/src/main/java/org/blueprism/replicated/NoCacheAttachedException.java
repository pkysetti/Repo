package org.blueprism.replicated;

public class NoCacheAttachedException extends Exception {
    public NoCacheAttachedException() {
        super();
    }

    public NoCacheAttachedException(String message) {
        super(message);
    }
}