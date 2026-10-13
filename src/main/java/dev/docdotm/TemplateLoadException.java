package dev.docdotm;

/** Thrown when a template file is missing or is not a WordprocessingML package. */
public class TemplateLoadException extends RuntimeException {

    TemplateLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
