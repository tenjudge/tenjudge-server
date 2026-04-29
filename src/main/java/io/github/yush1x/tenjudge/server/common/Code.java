package io.github.yush1x.tenjudge.server.common;

import lombok.Getter;

@Getter
public enum Code {
    SUCCESS(0, "success"),

    UNAUTHORIZED(10001, "unauthorized"),
    FORBIDDEN(10002, "forbidden"),
    USERNAME_INVALID(10003, "username is invalid"),
    PASSWORD_INVALID(10004, "password is invalid"),
    EMAIL_INVALID(10005, "email is invalid"),
    ROLE_INVALID(10006, "role is invalid"),
    REGISTER_FAILED(10007, "register failed"),
    LOGIN_FAILED(10008, "login failed"),
    USERNAME_ALREADY_EXISTS(10009, "username already exists"),
    EMAIL_ALREADY_EXISTS(10010, "email already exists"),

    UNZIP_FAILED(20001, "unzip failed"),
    CONFIG_FILE_INVALID(20002, "config file invalid"),
    FILE_MISSING(20003, "file missing"),
    READ_FILE_FAILED(20004, "read file failed"),
    SAVE_FILE_FAILED(20005, "save file failed"),
    TOO_MANY_REQUESTS(20006, "too many requests"),
    PROBLEM_NOT_FOUND(20007, "problem not found"),
    PROBLEM_REQUEST_INVALID(20008, "problem request invalid"),

    CONTEST_REQUEST_INVALID(30001, "contest request invalid"),
    CONTEST_PROBLEM_INVALID(30002, "contest problem invalid"),
    CONTEST_NOT_FOUND(30003, "contest not found"),
    CONTEST_ENDED(30004, "contest ended"),
    CONTEST_NOT_STARTED(30005, "contest not started"),

    SUBMIT_REQUEST_INVALID(40001, "submit request invalid"),



    SERVER_ERROR(1, "server error");

    public final int code;
    public final String message;

    Code(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
