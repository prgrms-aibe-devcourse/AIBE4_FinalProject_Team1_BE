package kr.dontworry.global.exception;

import org.springframework.http.HttpStatus;

public interface ErrorModel {
    HttpStatus getStatus();
    String getCode();
    String getMessage();
}