package kr.inventory.global.exception;

import org.springframework.http.HttpStatus;

public interface ErrorModel {
    HttpStatus getStatus();
    String getCode();
    String getMessage();
}