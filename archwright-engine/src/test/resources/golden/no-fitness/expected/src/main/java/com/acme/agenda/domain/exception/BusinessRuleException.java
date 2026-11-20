package com.acme.agenda.domain.exception;

/**
 * Uma regra de negócio foi violada (HTTP 422).
 */
public class BusinessRuleException extends DomainException {

    public BusinessRuleException(String message) {
        super(message);
    }

}
