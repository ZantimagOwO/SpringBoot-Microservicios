/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.paymentchain.transactions.exceptions.customExceptions;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.http.HttpStatus;

/**
 *
 * @author sotobotero
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
public class BussinesRuleException extends Exception{
    private String description;   
    private HttpStatus httpStatus;

    public BussinesRuleException(String description, HttpStatus httpStatus) {
        this.description = description;
        this.httpStatus = httpStatus;
    }

    public BussinesRuleException(String description, HttpStatus httpStatus, String message, Throwable cause) {
        super(message, cause);
        this.description = description;
        this.httpStatus = httpStatus;
    }
}
