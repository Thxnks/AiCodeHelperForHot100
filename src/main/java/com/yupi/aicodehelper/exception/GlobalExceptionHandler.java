package com.yupi.aicodehelper.exception;

import com.yupi.aicodehelper.common.BaseResponse;
import com.yupi.aicodehelper.common.ErrorCode;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public BaseResponse<Void> businessExceptionHandler(BusinessException e) {
        log.warn("Business exception: {}", e.getMessage());
        return new BaseResponse<>(e.getCode(), e.getMessage(), null);
    }

    @ExceptionHandler({
            IllegalArgumentException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class
    })
    public BaseResponse<Void> badRequestExceptionHandler(Exception e) {
        log.warn("Bad request: {}", e.getMessage());
        return BaseResponse.error(ErrorCode.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public BaseResponse<Void> methodArgumentNotValidExceptionHandler(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        if (message.isBlank()) {
            message = "Request body validation failed";
        }
        log.warn("Request body validation failed: {}", message);
        return BaseResponse.error(ErrorCode.VALIDATION_ERROR, message);
    }

    @ExceptionHandler(BindException.class)
    public BaseResponse<Void> bindExceptionHandler(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        if (message.isBlank()) {
            message = "Request parameter binding failed";
        }
        log.warn("Request parameter binding failed: {}", message);
        return BaseResponse.error(ErrorCode.VALIDATION_ERROR, message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public BaseResponse<Void> constraintViolationExceptionHandler(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining("; "));
        if (message.isBlank()) {
            message = "Request parameter validation failed";
        }
        log.warn("Constraint violation: {}", message);
        return BaseResponse.error(ErrorCode.VALIDATION_ERROR, message);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public BaseResponse<Void> dataIntegrityViolationExceptionHandler(DataIntegrityViolationException e) {
        log.warn("Data integrity violation: {}", e.getMessage());
        return BaseResponse.error(ErrorCode.RESOURCE_CONFLICT, "Database constraint violation");
    }

    @ExceptionHandler(Exception.class)
    public BaseResponse<Void> runtimeExceptionHandler(Exception e) {
        log.error("System error", e);
        return BaseResponse.error(ErrorCode.SYSTEM_ERROR);
    }
}
