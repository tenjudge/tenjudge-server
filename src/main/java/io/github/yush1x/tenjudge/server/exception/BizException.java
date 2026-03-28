package io.github.yush1x.tenjudge.server.exception;

import io.github.yush1x.tenjudge.server.common.Code;
import lombok.Getter;

@Getter
public class BizException extends RuntimeException {

    private final Code code;

    public BizException(Code code) {
        super(code.getMessage());
        this.code = code;
    }

    public BizException(Code code, String message) {
        super(message);
        this.code = code;
    }
}
