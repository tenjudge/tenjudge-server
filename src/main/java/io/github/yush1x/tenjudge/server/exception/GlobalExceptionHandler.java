package io.github.yush1x.tenjudge.server.exception;

import io.github.yush1x.tenjudge.server.common.Code;
import io.github.yush1x.tenjudge.server.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler
    public Result<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.error(Code.SERVER_ERROR);
    }

    @ExceptionHandler
    public Result<Void> handleBizException(BizException e) {
        log.info("业务异常", e);
        return Result.error(e.getCode());
    }
}