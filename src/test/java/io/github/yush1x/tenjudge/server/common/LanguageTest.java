package io.github.yush1x.tenjudge.server.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LanguageTest {

    @Test
    void getSuffixByName_shouldReturnExpectedSuffix() {
        assertEquals("cpp", Language.getSuffixByName("cpp"));
        assertEquals("py", Language.getSuffixByName("python"));
    }

    @Test
    void getSuffixByName_shouldReturnNullForUnknownOrNullName() {
        assertNull(Language.getSuffixByName("java"));
        assertNull(Language.getSuffixByName(null));
    }

    @Test
    void contains_shouldReturnTrueOnlyForSupportedLanguageName() {
        assertTrue(Language.contains("cpp"));
        assertTrue(Language.contains("python"));
        assertFalse(Language.contains("CPP"));
        assertFalse(Language.contains("java"));
    }
}
