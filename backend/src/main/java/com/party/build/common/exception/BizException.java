package com.party.build.common.exception;

import com.party.build.common.api.ErrorCode;
import lombok.Getter;

@Getter
public class BizException extends RuntimeException {
    private final int code;

    public BizException(ErrorCode errorCode) {
        super(errorCode.message());
        this.code = errorCode.code();
    }

    public BizException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.code();
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }
}