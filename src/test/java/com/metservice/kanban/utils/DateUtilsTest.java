package com.metservice.kanban.utils;

import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Unit tests for DateUtils.
 *
 * DateUtils is a stateless helper whose methods convert between {@code String} and {@link LocalDate}, so there is
 * no fixture to build. Instead the input space of each method is partitioned and one or more tests are written per
 * block:
 *
 *  valid dates: an ordinary date, a date whose day and month are both single-digit and different (4 March, so
 *  dd/MM and MM/dd cannot be confused), and the leap day 2012-02-29 with the days around it;
 *  malformed strings: empty, words, missing or wrong separators, missing parts, trailing text;
 *  well-formed strings that name no real day: month 13, day 0, and 29 February in a non-leap year;
 *  a string written in the other method's format (ISO into the New Zealand parser and the reverse);
 *  null and empty strings, which {@code parseDate} turns into its default value.
 *
 * Round trips (date → string → date and string → date → string, within each format and across both) check that every
 * parser is the inverse of its formatter on the valid blocks.
 *
 * The disabled tests document bug DU-01: {@code parseIsoDate} and {@code parseConventionalNewZealandDate} parse to
 * a {@code DateTime} in the JVM default time zone before dropping the time, so a date whose midnight does not exist in
 * that zone (a DST change at 00:00) cannot be parsed.
 */
class DateUtilsTest {

    private static final LocalDate LEAP_DAY = new LocalDate(2012, 2, 29);
    private static final String LEAP_DAY_ISO = "2012-02-29";
    private static final String LEAP_DAY_NZ = "29/02/2012";

    private static final LocalDate MARCH_4_2013 = new LocalDate(2013, 3, 4);
    private static final String MARCH_4_2013_ISO = "2013-03-04";
    private static final String MARCH_4_2013_NZ = "04/03/2013";

    private static final LocalDate DEFAULT_DATE = new LocalDate(1970, 1, 1);

    private static final DateTimeZone ZONE_WITH_MIDNIGHT_GAP = DateTimeZone.forID("America/Sao_Paulo");
    private static final LocalDate MIDNIGHT_GAP_DATE = new LocalDate(2018, 11, 4);
    private static final String MIDNIGHT_GAP_ISO = "2018-11-04";
    private static final String MIDNIGHT_GAP_NZ = "04/11/2018";

    static Stream<LocalDate> roundTripDates() {
        return Stream.of(
                LEAP_DAY,
                new LocalDate(2012, 3, 1),
                new LocalDate(2012, 12, 31),
                new LocalDate(2013, 1, 1),
                MARCH_4_2013,
                new LocalDate(2000, 2, 29),
                new LocalDate(1999, 12, 31));
    }

    private static <T> T withDefaultZone(DateTimeZone zone, Supplier<T> action) {
        DateTimeZone original = DateTimeZone.getDefault();
        DateTimeZone.setDefault(zone);
        try {
            return action.get();
        } finally {
            DateTimeZone.setDefault(original);
        }
    }

    @Test
    void parseIsoDate_ValidDate_ReturnsThatDate() {
        assertThat(DateUtils.parseIsoDate(MARCH_4_2013_ISO)).isEqualTo(MARCH_4_2013);
    }

    @Test
    void parseIsoDate_LeapDay_ReturnsFebruary29() {
        assertThat(DateUtils.parseIsoDate(LEAP_DAY_ISO)).isEqualTo(LEAP_DAY);
    }

    @Test
    void parseIsoDate_DayAfterLeapDay_ReturnsMarch1() {
        assertThat(DateUtils.parseIsoDate("2012-03-01")).isEqualTo(new LocalDate(2012, 3, 1));
    }

    @ParameterizedTest(name = "ISO string \"{0}\"")
    @EmptySource
    @ValueSource(
            strings = {
                "not a date",
                "2012-02",
                "2012/02/29",
                "29-02-2012",
                "2012-02-29x",
                "2012-02-29T10:15:30",
                "2013-02-29",
                "2012-02-30",
                "2012-13-01",
                "2012-00-10",
                "2012-01-00",
                "29/02/2012"
            })
    void parseIsoDate_InvalidString_ThrowsIllegalArgumentException(String dateString) {
        assertThatThrownBy(() -> DateUtils.parseIsoDate(dateString)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parseIsoDate_NullString_ThrowsRuntimeException() {
        assertThatThrownBy(() -> DateUtils.parseIsoDate(null)).isInstanceOf(RuntimeException.class);
    }

    @Disabled("documents bug DU-01")
    @Test
    void parseIsoDate_MidnightMissingInDefaultZone_ReturnsDate() {
        LocalDate parsed = withDefaultZone(ZONE_WITH_MIDNIGHT_GAP, () -> DateUtils.parseIsoDate(MIDNIGHT_GAP_ISO));

        assertThat(parsed).isEqualTo(MIDNIGHT_GAP_DATE);
    }

    @Test
    void formatIsoDate_SingleDigitMonthAndDay_PadsWithZeros() {
        assertThat(DateUtils.formatIsoDate(MARCH_4_2013)).isEqualTo(MARCH_4_2013_ISO);
    }

    @Test
    void formatIsoDate_LeapDay_ReturnsFebruary29() {
        assertThat(DateUtils.formatIsoDate(LEAP_DAY)).isEqualTo(LEAP_DAY_ISO);
    }

    @Test
    void formatIsoDate_NullDate_ThrowsNullPointerException() {
        assertThatThrownBy(() -> DateUtils.formatIsoDate(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void parseConventionalNewZealandDate_ValidDate_ReadsDayBeforeMonth() {
        assertThat(DateUtils.parseConventionalNewZealandDate(MARCH_4_2013_NZ)).isEqualTo(MARCH_4_2013);
    }

    @Test
    void parseConventionalNewZealandDate_LeapDay_ReturnsFebruary29() {
        assertThat(DateUtils.parseConventionalNewZealandDate(LEAP_DAY_NZ)).isEqualTo(LEAP_DAY);
    }

    @ParameterizedTest(name = "New Zealand string \"{0}\"")
    @EmptySource
    @ValueSource(
            strings = {
                "not a date",
                "29/02",
                "29-02-2012",
                "29/02/2012x",
                "29/02/2013",
                "30/02/2012",
                "31/04/2012",
                "12/31/2013",
                "2012-02-29"
            })
    void parseConventionalNewZealandDate_InvalidString_ThrowsIllegalArgumentException(String dateString) {
        assertThatThrownBy(() -> DateUtils.parseConventionalNewZealandDate(dateString))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parseConventionalNewZealandDate_NullString_ThrowsRuntimeException() {
        assertThatThrownBy(() -> DateUtils.parseConventionalNewZealandDate(null))
                .isInstanceOf(RuntimeException.class);
    }

    @Disabled("documents bug DU-01")
    @Test
    void parseConventionalNewZealandDate_MidnightMissingInDefaultZone_ReturnsDate() {
        LocalDate parsed = withDefaultZone(
                ZONE_WITH_MIDNIGHT_GAP, () -> DateUtils.parseConventionalNewZealandDate(MIDNIGHT_GAP_NZ));

        assertThat(parsed).isEqualTo(MIDNIGHT_GAP_DATE);
    }

    @Test
    void formatConventionalNewZealandDate_SingleDigitMonthAndDay_PadsWithZerosAndPutsDayFirst() {
        assertThat(DateUtils.formatConventionalNewZealandDate(MARCH_4_2013)).isEqualTo(MARCH_4_2013_NZ);
    }

    @Test
    void formatConventionalNewZealandDate_LeapDay_ReturnsFebruary29() {
        assertThat(DateUtils.formatConventionalNewZealandDate(LEAP_DAY)).isEqualTo(LEAP_DAY_NZ);
    }

    @Test
    void formatConventionalNewZealandDate_NullDate_ThrowsNullPointerException() {
        assertThatThrownBy(() -> DateUtils.formatConventionalNewZealandDate(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void parseDate_ValidString_ReturnsParsedDate() {
        assertThat(DateUtils.parseDate(MARCH_4_2013_ISO, DEFAULT_DATE)).isEqualTo(MARCH_4_2013);
    }

    @Test
    void parseDate_LeapDayString_ReturnsLeapDay() {
        assertThat(DateUtils.parseDate(LEAP_DAY_ISO, DEFAULT_DATE)).isEqualTo(LEAP_DAY);
    }

    @Test
    void parseDate_ValidStringAndNullDefault_ReturnsParsedDate() {
        assertThat(DateUtils.parseDate(MARCH_4_2013_ISO, null)).isEqualTo(MARCH_4_2013);
    }

    @ParameterizedTest(name = "date string \"{0}\"")
    @NullAndEmptySource
    void parseDate_NullOrEmptyString_ReturnsDefaultValue(String dateString) {
        assertThat(DateUtils.parseDate(dateString, DEFAULT_DATE)).isSameAs(DEFAULT_DATE);
    }

    @ParameterizedTest(name = "date string \"{0}\"")
    @NullAndEmptySource
    void parseDate_NullOrEmptyStringAndNullDefault_ReturnsNull(String dateString) {
        assertThat(DateUtils.parseDate(dateString, null)).isNull();
    }

    @ParameterizedTest(name = "date string \"{0}\"")
    @ValueSource(strings = {"not a date", "   ", "2012-02-29x", "2013-02-29", "2012-02-30", "2012-13-01", "29/02/2012"})
    void parseDate_InvalidString_ReturnsDefaultValue(String dateString) {
        assertThat(DateUtils.parseDate(dateString, DEFAULT_DATE)).isSameAs(DEFAULT_DATE);
    }

    @Test
    void parseDate_InvalidStringAndNullDefault_ReturnsNull() {
        assertThat(DateUtils.parseDate("not a date", null)).isNull();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("roundTripDates")
    void formatIsoDate_ThenParseIsoDate_ReturnsOriginalDate(LocalDate date) {
        assertThat(DateUtils.parseIsoDate(DateUtils.formatIsoDate(date))).isEqualTo(date);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("roundTripDates")
    void formatConventionalNewZealandDate_ThenParseConventionalNewZealandDate_ReturnsOriginalDate(LocalDate date) {
        assertThat(DateUtils.parseConventionalNewZealandDate(DateUtils.formatConventionalNewZealandDate(date)))
                .isEqualTo(date);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("roundTripDates")
    void formatIsoDate_ThenParseDate_ReturnsOriginalDate(LocalDate date) {
        assertThat(DateUtils.parseDate(DateUtils.formatIsoDate(date), DEFAULT_DATE))
                .isEqualTo(date);
    }

    @ParameterizedTest(name = "ISO string \"{0}\"")
    @ValueSource(strings = {"2012-02-29", "2012-03-01", "2012-12-31", "2013-03-04", "2000-02-29", "1999-12-31"})
    void parseIsoDate_ThenFormatIsoDate_ReturnsOriginalString(String isoString) {
        assertThat(DateUtils.formatIsoDate(DateUtils.parseIsoDate(isoString))).isEqualTo(isoString);
    }

    @ParameterizedTest(name = "New Zealand string \"{0}\"")
    @ValueSource(strings = {"29/02/2012", "01/03/2012", "31/12/2012", "04/03/2013", "29/02/2000", "31/12/1999"})
    void parseConventionalNewZealandDate_ThenFormatConventionalNewZealandDate_ReturnsOriginalString(
            String newZealandString) {
        assertThat(DateUtils.formatConventionalNewZealandDate(
                        DateUtils.parseConventionalNewZealandDate(newZealandString)))
                .isEqualTo(newZealandString);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("roundTripDates")
    void parseIsoDate_AndParseConventionalNewZealandDate_SameDate_ReturnEqualDates(LocalDate date) {
        LocalDate fromIso = DateUtils.parseIsoDate(DateUtils.formatIsoDate(date));
        LocalDate fromConventional =
                DateUtils.parseConventionalNewZealandDate(DateUtils.formatConventionalNewZealandDate(date));

        assertThat(fromIso).isEqualTo(fromConventional);
    }

    @Test
    void parseIsoDate_LeapDayFormattedConventionallyAndReparsed_ReturnsLeapDay() {
        String conventional = DateUtils.formatConventionalNewZealandDate(DateUtils.parseIsoDate(LEAP_DAY_ISO));

        assertThat(conventional).isEqualTo(LEAP_DAY_NZ);
        assertThat(DateUtils.parseConventionalNewZealandDate(conventional)).isEqualTo(LEAP_DAY);
    }
}
