package com.sushil.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

public final class AppExceptions {

    private AppExceptions() {}

    @ResponseStatus(HttpStatus.NOT_FOUND)
    public static class ResourceNotFoundException extends RuntimeException {
        @java.io.Serial private static final long serialVersionUID = 1L;

        public ResourceNotFoundException(String resource, Object identifier) {
            super("%s not found: %s".formatted(resource, identifier));
        }
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    public static class DuplicateResourceException extends RuntimeException {
        @java.io.Serial private static final long serialVersionUID = 1L;

        public DuplicateResourceException(String message) {
            super(message);
        }
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public static class InvalidTokenException extends RuntimeException {
        @java.io.Serial private static final long serialVersionUID = 1L;

        public InvalidTokenException(String message) {
            super(message);
        }
    }

    @ResponseStatus(HttpStatus.FORBIDDEN)
    public static class UnauthorizedException extends RuntimeException {
        @java.io.Serial private static final long serialVersionUID = 1L;

        public UnauthorizedException(String message) {
            super(message);
        }
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public static class BadRequestException extends RuntimeException {
        @java.io.Serial private static final long serialVersionUID = 1L;

        public BadRequestException(String message) {
            super(message);
        }
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public static class OtpException extends RuntimeException {
        @java.io.Serial private static final long serialVersionUID = 1L;

        public OtpException(String message) {
            super(message);
        }
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public static class MiningException extends RuntimeException {
        @java.io.Serial private static final long serialVersionUID = 1L;

        public MiningException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
