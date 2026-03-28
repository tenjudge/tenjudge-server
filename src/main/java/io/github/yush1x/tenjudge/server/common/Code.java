package io.github.yush1x.tenjudge.server.common;

import lombok.Getter;

@Getter
public enum Code {
    SUCCESS(0, "success"),
    ERROR(1, "error");

    public final int code;
    public final String message;

    Code(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
