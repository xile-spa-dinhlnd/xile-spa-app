package com.xilespa.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TextNormalizerTest {

    @Test
    void plainNameIsTrimmedAndCollapsed() {
        assertThat(TextNormalizer.normalizeName("  Lan   A  ")).isEqualTo("Lan A");
    }

    @Test
    void nbspAndThinSpacesBecomeSpace() {
        // \u00A0: NBSP, \u2009: Thin space
        assertThat(TextNormalizer.normalizeName("\u00A0Lan\u2009A\u00A0")).isEqualTo("Lan A");
    }

    @Test
    void zeroWidthAndBomAreRemoved() {
        // \u200B: Zero-width space, \uFEFF: Zero-width no-break space (BOM)
        assertThat(TextNormalizer.normalizeName("Lan\u200BA\uFEFF")).isEqualTo("LanA");
    }

    @Test
    void bidiControlIsRemoved() {
        // \u202E: Right-to-Left Override
        assertThat(TextNormalizer.normalizeName("Lan\u202EA")).isEqualTo("LanA");
    }

    @Test
    void decomposedVietnameseBecomesComposed() {
        // "G\u006F\u0323\u0302i" là dạng tổ hợp NFD của "Gội"
        assertThat(TextNormalizer.normalizeName("G\u006F\u0323\u0302i")).isEqualTo("Gội");
    }

    @Test
    void vietnameseDiacriticsAreKept() {
        assertThat(TextNormalizer.normalizeName("Massage Cổ Vai Gáy"))
                .isEqualTo("Massage Cổ Vai Gáy");
    }

    @Test
    void onlyInvisibleCharactersBecomeEmpty() {
        // \u200B: Zero-width space, \u200C: Zero-width non-joiner, \uFEFF: BOM
        assertThat(TextNormalizer.normalizeName("\u200B\u200C\uFEFF")).isEqualTo("");
    }

    @Test
    void tabsAndNewlinesBecomeSpace() {
        assertThat(TextNormalizer.normalizeName("Lan\t\nA")).isEqualTo("Lan A");
    }

    @Test
    void controlCharactersAreRemoved() {
        // \u0000: NUL, \u0007: BEL
        assertThat(TextNormalizer.normalizeName("Lan\u0000\u0007A")).isEqualTo("LanA");
    }

    @Test
    void nextLineBecomesSpace() {
        // \u0085: Next Line (NEL)
        assertThat(TextNormalizer.normalizeName("Lan\u0085A")).isEqualTo("Lan A");
    }

    @Test
    void hangulFillerAndBrailleBlankAreRemoved() {
        // \u115F, \u1160, \u3164, \uFFA0: Hangul filler, \u2800: Braille pattern blank
        assertThat(TextNormalizer.normalizeName("\u115F\u1160\u3164\uFFA0\u2800")).isEqualTo("");
    }
}
