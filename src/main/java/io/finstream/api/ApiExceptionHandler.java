package io.finstream.api;

import io.finstream.query.QueryException;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebInputException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(QueryException.class)
    ResponseEntity<ApiError> queryError(QueryException error) {
        HttpStatus status = error.notFound() ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status)
                .body(new ApiError(error.code(), error.getMessage(), Instant.now()));
    }

    @ExceptionHandler(ServerWebInputException.class)
    ResponseEntity<ApiError> invalidInput(ServerWebInputException error) {
        return ResponseEntity.badRequest().body(new ApiError(
                inputCode(error), "Invalid request parameter", Instant.now()));
    }

    private String inputCode(ServerWebInputException error) {
        if (error.getMethodParameter() != null) {
            String parameter = error.getMethodParameter().getParameterName();
            if ("eventId".equals(parameter)) return "INVALID_EVENT_ID";
            if ("since".equals(parameter)) return "INVALID_SINCE";
            if ("minScore".equals(parameter)) return "INVALID_MIN_SCORE";
            if ("limit".equals(parameter)) return "INVALID_LIMIT";
        }
        String message = error.getReason() == null ? "" : error.getReason();
        if (message.contains("eventId") || message.contains("UUID")) return "INVALID_EVENT_ID";
        if (message.contains("since") || message.contains("Instant")) return "INVALID_SINCE";
        if (message.contains("minScore") || message.contains("Double")) return "INVALID_MIN_SCORE";
        if (message.contains("limit") || message.contains("Integer")) return "INVALID_LIMIT";
        return "INVALID_PARAMETER";
    }
}
