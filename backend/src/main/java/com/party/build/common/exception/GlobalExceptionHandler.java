package com.party.build.common.exception;

import com.party.build.common.api.ErrorCode;
import com.party.build.common.api.Result;
import com.party.build.common.api.TraceId;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ResponseEntity<Result<Void>> handleBiz(BizException e) {
        HttpStatus status = switch (e.getCode()) {
            case 10001, 10006 -> HttpStatus.UNAUTHORIZED;
            case 10003, 10004 -> HttpStatus.FORBIDDEN;
            case 10005 -> HttpStatus.LOCKED;
            case 20001, 20002, 20003 -> HttpStatus.BAD_REQUEST;
            case 30001, 30002 -> HttpStatus.UNPROCESSABLE_ENTITY;
            case 30003, 30004, 30005 -> HttpStatus.CONFLICT;
            case 40001 -> HttpStatus.GATEWAY_TIMEOUT;
            case 40002, 40003 -> HttpStatus.BAD_GATEWAY;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
        return ResponseEntity.status(status).body(Result.fail(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<Result<Void>> handleValidation(Exception e) {
        String msg = "参数错误";
        if (e instanceof MethodArgumentNotValidException ex && ex.getBindingResult().getFieldError() != null) {
            msg = ex.getBindingResult().getFieldError().getField() + " " + ex.getBindingResult().getFieldError().getDefaultMessage();
        }
        return ResponseEntity.badRequest().body(Result.fail(ErrorCode.VALIDATION_ERROR.code(), msg));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Void>> handleConstraint(ConstraintViolationException e) {
        return ResponseEntity.badRequest().body(Result.fail(ErrorCode.VALIDATION_ERROR.code(), e.getMessage()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Result<Void>> handleUploadSize(MaxUploadSizeExceededException e) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(Result.fail(ErrorCode.FILE_TOO_LARGE.code(), ErrorCode.FILE_TOO_LARGE.message()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Result<Void>> handleAccessDenied(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Result.fail(ErrorCode.FORBIDDEN.code(), ErrorCode.FORBIDDEN.message()));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Result<Void>> handleAuth(AuthenticationException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Result.fail(ErrorCode.UNAUTHORIZED.code(), ErrorCode.UNAUTHORIZED.message()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleUnknown(Exception e) {
        log.error("unhandled error traceId={}", TraceId.current(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.fail(ErrorCode.UNKNOWN.code(), ErrorCode.UNKNOWN.message()));
    }
}