package com.acme.agenda.domain.exceptions;

/**
 * Uma regra de negócio foi violada (HTTP 422).
 */
public class BusinessRuleException extends DomainException {

    public BusinessRuleException(String message) {
        super(message);
    }

}
