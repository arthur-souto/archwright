package com.acme.legado.shared.adapter.in.http;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

/**
 * Corpo de erro do {@link GlobalExceptionHandler}, com os campos do {@code ProblemDetail} (RFC 9457) que o
 * Spring Boot 2.7 não tem: {@code type}, {@code title}, {@code status}, {@code detail} e {@code instance}, mais
 * {@code errors} nas falhas de validação.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {

    private final String type = "about:blank";
    private final String title;
    private final int status;
    private final String detail;
    private final String instance;
    private final List<Map<String, String>> errors;

    public ApiError(HttpStatus status, String detail, String instance, List<Map<String, String>> errors) {
        this.title = status.getReasonPhrase();
        this.status = status.value();
        this.detail = detail;
        this.instance = instance;
        this.errors = errors;
    }

    public String getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public int getStatus() {
        return status;
    }

    public String getDetail() {
        return detail;
    }

    public String getInstance() {
        return instance;
    }

    public List<Map<String, String>> getErrors() {
        return errors;
    }

}
