package io.github.yush1x.tenjudge.server.common;

import lombok.Getter;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
public enum Tag {
    TWO_SAT("2-sat"),
    BINARY_SEARCH("binary search"),
    BITMASKS("bitmasks"),
    BRUTE_FORCE("brute force"),
    CHINESE_REMAINDER_THEOREM("chinese remainder theorem"),
    COMBINATORICS("combinatorics"),
    COMMUNICATION("communication"),
    CONSTRUCTIVE_ALGORITHMS("constructive algorithms"),
    DATA_STRUCTURES("data structures"),
    DFS_AND_SIMILAR("dfs and similar"),
    DIVIDE_AND_CONQUER("divide and conquer"),
    DP("dp"),
    DSU("dsu"),
    EXPRESSION_PARSING("expression parsing"),
    FFT("fft"),
    FLOWS("flows"),
    GAMES("games"),
    GEOMETRY("geometry"),
    GRAPH_MATCHINGS("graph matchings"),
    GRAPHS("graphs"),
    GREEDY("greedy"),
    HASHING("hashing"),
    IMPLEMENTATION("implementation"),
    INTERACTIVE("interactive"),
    MATH("math"),
    MATRICES("matrices"),
    MEET_IN_THE_MIDDLE("meet-in-the-middle"),
    NUMBER_THEORY("number theory"),
    PROBABILITIES("probabilities"),
    SCHEDULES("schedules"),
    SHORTEST_PATHS("shortest paths"),
    SORTINGS("sortings"),
    STRING_SUFFIX_STRUCTURES("string suffix structures"),
    STRINGS("strings"),
    TERNARY_SEARCH("ternary search"),
    TREES("trees"),
    TWO_POINTERS("two pointers");

    private final String value;

    Tag(String value) {
        this.value = value;
    }

    // 使用 Set 缓存所有标签值
    private static final Set<String> TAG_SET = Arrays.stream(Tag.values())
            .map(Tag::getValue)
            .collect(Collectors.toSet());

    // 判断字符串是否属于有效的Tag标签
    public static boolean contains(String tag) {
        return tag != null && TAG_SET.contains(tag);
    }
}
