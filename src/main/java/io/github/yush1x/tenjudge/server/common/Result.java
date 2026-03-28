package io.github.yush1x.tenjudge.server.common;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Result<T> {
    private int code;
    private String message;
    private T data;

    public static <T> Result<T> success(T data) {
        return new Result<>(Code.SUCCESS.code, Code.SUCCESS.message, data);
    }

    public static <T> Result<T> success() {
        return new Result<>(Code.SUCCESS.code, Code.SUCCESS.message, null);
    }

    public static <T> Result<T> error(Code code) {
        return new Result<>(code.code, code.message, null);
    }

    public static <T> Result<T> error(Code code, String message) {
        return new Result<>(code.code, message, null);
    }
}
