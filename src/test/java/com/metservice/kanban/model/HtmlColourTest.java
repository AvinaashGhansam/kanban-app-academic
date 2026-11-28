package com.metservice.kanban.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.awt.Color;
import java.util.stream.Stream;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Unit tests for HtmlColour.
 *
 * HtmlColour is an immutable wrapper around six hexadecimal digits whose only observable behaviour is
 * toString, so every test builds a colour and checks its text. The input space of each constructor is
 * partitioned as follows:
 *
 *   HtmlColour(String), valid: with and without a leading '#', upper, lower and mixed case, and the
 *   boundary digits 0, 9, A, F, a and f;
 *   HtmlColour(String), invalid: wrong length (including 3-digit shorthand and 8-digit values with alpha),
 *   a second '#', words, a "0x" prefix, whitespace, and the characters just outside the hex ranges ('/', ':', '@',
 *   '`', 'G', 'g');
 *   HtmlColour(Color): the primary colours, black and white, values with leading zeros, and colours with
 *   an alpha channel at or above and below 0x10;
 *   null for either constructor.
 *
 * Round trips feed toString output back into the String constructor, and the two constructors are compared
 * on the same colour.
 *
 * The disabled tests document bugs HC-01 (the Color constructor breaks when the alpha channel is below 0x10) and
 * HC-02 (the two constructors disagree on letter case).
 */
class HtmlColourTest {

    static Stream<Arguments> colorsAndHexText() {
        return Stream.of(
                Arguments.of(Color.RED, "#FF0000"),
                Arguments.of(Color.GREEN, "#00FF00"),
                Arguments.of(Color.BLUE, "#0000FF"),
                Arguments.of(Color.BLACK, "#000000"),
                Arguments.of(Color.WHITE, "#FFFFFF"),
                Arguments.of(new Color(1, 2, 3), "#010203"),
                Arguments.of(new Color(0xAB, 0xCD, 0xEF), "#ABCDEF"));
    }

    @Test
    void toString_ValidStringWithoutHash_PrependsHash() {
        assertThat(new HtmlColour("FF00AA").toString()).isEqualTo("#FF00AA");
    }

    @Test
    void toString_ValidStringWithHash_KeepsSingleHash() {
        assertThat(new HtmlColour("#FF00AA").toString()).isEqualTo("#FF00AA");
    }

    @ParameterizedTest(name = "string \"{0}\"")
    @ValueSource(strings = {"000000", "999999", "AAAAAA", "FFFFFF", "aaaaaa", "ffffff", "aBcDeF", "012345"})
    void toString_ValidString_ReturnsHashAndSameDigits(String hex) {
        assertThat(new HtmlColour(hex).toString()).isEqualToIgnoringCase("#" + hex);
    }

    @ParameterizedTest(name = "string \"{0}\"")
    @EmptySource
    @ValueSource(
            strings = {
                "#",
                "##FF0000",
                "FFF",
                "#FFF",
                "FF000",
                "FF00000",
                "#FF00000",
                "FF0000FF",
                "red",
                "0xFF0000",
                " FF0000",
                "FF0000 ",
                "FF 000",
                "FF00GG",
                "00000G",
                "00000g",
                "00000/",
                "00000:",
                "00000@",
                "00000`"
            })
    void constructor_InvalidString_ThrowsIllegalArgumentException(String value) {
        assertThatThrownBy(() -> new HtmlColour(value)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_InvalidString_MessageNamesTheValue() {
        assertThatThrownBy(() -> new HtmlColour("#GGGGGG"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("GGGGGG");
    }

    @Test
    void constructor_NullString_ThrowsNullPointerException() {
        assertThatThrownBy(() -> new HtmlColour((String) null)).isInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("colorsAndHexText")
    void toString_Color_ReturnsHashAndUppercaseHex(Color color, String expected) {
        assertThat(new HtmlColour(color).toString()).isEqualTo(expected);
    }

    @ParameterizedTest(name = "alpha {0}")
    @ValueSource(ints = {16, 128, 254, 255})
    void toString_ColorWithAlphaOfAtLeast16_IgnoresAlpha(int alpha) {
        assertThat(new HtmlColour(new Color(255, 0, 0, alpha)).toString()).isEqualTo("#FF0000");
    }

    @Disabled("documents bug HC-01")
    @ParameterizedTest(name = "alpha {0}")
    @ValueSource(ints = {0, 1, 15})
    void toString_ColorWithAlphaBelow16_IgnoresAlpha(int alpha) {
        assertThat(new HtmlColour(new Color(255, 0, 0, alpha)).toString()).isEqualTo("#FF0000");
    }

    @Test
    void constructor_NullColor_ThrowsNullPointerException() {
        assertThatThrownBy(() -> new HtmlColour((Color) null)).isInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest(name = "string \"{0}\"")
    @ValueSource(strings = {"#FF0000", "00FF00", "#0000ff", "012345", "aBcDeF"})
    void toString_StringOutputParsedBack_ReturnsSameText(String value) {
        String text = new HtmlColour(value).toString();

        assertThat(new HtmlColour(text).toString()).isEqualTo(text);
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("colorsAndHexText")
    void toString_ColorOutputParsedBack_ReturnsSameText(Color color) {
        String text = new HtmlColour(color).toString();

        assertThat(new HtmlColour(text).toString()).isEqualTo(text);
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("colorsAndHexText")
    void toString_UppercaseStringAndEquivalentColor_ReturnSameText(Color color, String expected) {
        assertThat(new HtmlColour(expected).toString()).isEqualTo(new HtmlColour(color).toString());
    }

    @Disabled("documents bug HC-02")
    @Test
    void toString_LowercaseStringAndEquivalentColor_ReturnSameText() {
        assertThat(new HtmlColour("#ff0000").toString()).isEqualTo(new HtmlColour(Color.RED).toString());
    }
}
