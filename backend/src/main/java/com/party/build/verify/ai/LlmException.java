package com.party.build.verify.ai;

import com.party.build.common.api.ErrorCode;
import com.party.build.common.exception.BizException;

public class LlmException extends BizException {
    public LlmException(ErrorCode code, String message) {
        super(code, message);
    }
}