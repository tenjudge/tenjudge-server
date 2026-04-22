package io.github.yush1x.tenjudge.server.common;

import lombok.Getter;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
public enum Checker {

    FCMP("fcmp"),
    LCMP("lcmp"),
    WCMP("wcmp"),
    SPECIAL("special");


    private final String value;

    Checker(String value) {
        this.value = value;
    }

    // 使用 Set 缓存所有 checker 值
    private static final Set<String> Checker_SET = Arrays.stream(Checker.values())
            .map(Checker::getValue)
            .collect(Collectors.toSet());

    // 判断字符串是否属于有效的checker
    public static boolean contains(String checker) {
        return checker != null && Checker_SET.contains(checker);
    }
}
