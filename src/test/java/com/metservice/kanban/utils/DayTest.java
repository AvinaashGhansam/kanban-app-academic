package com.metservice.kanban.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.joda.time.LocalDate;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Unit tests for Day.
 *
 * Day wraps a single {@link LocalDate}, so the tests need no fixture beyond a few dates. They are selected as
 * follows:
 *
 *   {@code equals} is covered branch by branch, one test per exit of its control-flow graph: same instance, null,
 *   another class (a plain {@code LocalDate} and a subclass of Day), both dates null, exactly one date null (each
 *   way round), equal dates and different dates;
 *   {@code compareTo} is covered for before, after and same date, using pairs of dates that straddle boundaries
 *   (the leap day to 1 March, the end of a year, and a day/month swap that text order would get wrong), and by
 *   sorting a list;
 *   {@code toString} is covered for zero padding, day-before-month order and the leap day;
 *   {@code hashCode} is covered for consistency with {@code equals}, directly and through a {@code HashSet}.
 *
 *  Day accepts a null date. {@code equals} and {@code hashCode} cope with it, but {@code compareTo} and
 * {@code toString} throw. The disabled tests document that inconsistency as bugs DAY-01 and DAY-02.
 */
class DayTest {

    private static final LocalDate LEAP_DAY = new LocalDate(2012, 2, 29);
    private static final LocalDate MARCH_4_2013 = new LocalDate(2013, 3, 4);

    static Stream<Arguments> earlierAndLaterDates() {
        return Stream.of(
                Arguments.of(MARCH_4_2013, new LocalDate(2013, 3, 5)),
                Arguments.of(LEAP_DAY, new LocalDate(2012, 3, 1)),
                Arguments.of(new LocalDate(2012, 12, 31), new LocalDate(2013, 1, 1)),
                Arguments.of(MARCH_4_2013, new LocalDate(2013, 4, 3)),
                Arguments.of(new LocalDate(1999, 12, 31), new LocalDate(2000, 1, 1)));
    }

    private static Day dayOf(int year, int month, int dayOfMonth) {
        return new Day(new LocalDate(year, month, dayOfMonth));
    }

    @Test
    void getDate_DayCreatedWithDate_ReturnsSameDate() {
        assertThat(new Day(MARCH_4_2013).getDate()).isSameAs(MARCH_4_2013);
    }

    @Test
    void getDate_DayCreatedWithNull_ReturnsNull() {
        assertThat(new Day(null).getDate()).isNull();
    }

    @ParameterizedTest(name = "{0} before {1}")
    @MethodSource("earlierAndLaterDates")
    void compareTo_EarlierDay_ReturnsNegative(LocalDate earlier, LocalDate later) {
        assertThat(new Day(earlier).compareTo(new Day(later))).isNegative();
    }

    @ParameterizedTest(name = "{1} after {0}")
    @MethodSource("earlierAndLaterDates")
    void compareTo_LaterDay_ReturnsPositive(LocalDate earlier, LocalDate later) {
        assertThat(new Day(later).compareTo(new Day(earlier))).isPositive();
    }

    @Test
    void compareTo_DifferentInstanceSameDate_ReturnsZero() {
        assertThat(new Day(MARCH_4_2013).compareTo(dayOf(2013, 3, 4))).isZero();
    }

    @Test
    void compareTo_SameInstance_ReturnsZero() {
        Day day = new Day(LEAP_DAY);

        assertThat(day.compareTo(day)).isZero();
    }

    @Test
    void compareTo_UnorderedDays_SortsChronologicallyRatherThanByText() {
        List<Day> days = List.of(dayOf(2013, 3, 4), dayOf(2012, 2, 29), dayOf(2013, 1, 1), dayOf(2012, 12, 31));

        assertThat(days.stream().sorted().toList())
                .containsExactly(dayOf(2012, 2, 29), dayOf(2012, 12, 31), dayOf(2013, 1, 1), dayOf(2013, 3, 4));
    }

    @Test
    void compareTo_NullDay_ThrowsNullPointerException() {
        assertThatThrownBy(() -> new Day(MARCH_4_2013).compareTo(null)).isInstanceOf(NullPointerException.class);
    }

    @Disabled("documents bug DAY-01")
    @Test
    void compareTo_BothDatesNull_ReturnsZero() {
        assertThat(new Day(null).compareTo(new Day(null))).isZero();
    }

    @Test
    void equals_SameInstance_ReturnsTrue() {
        Day day = new Day(MARCH_4_2013);

        assertThat(day.equals(day)).isTrue();
    }

    @Test
    void equals_DifferentInstanceSameDate_ReturnsTrueBothWays() {
        Day first = new Day(LEAP_DAY);
        Day second = dayOf(2012, 2, 29);

        assertThat(first.equals(second)).isTrue();
        assertThat(second.equals(first)).isTrue();
    }

    @ParameterizedTest(name = "{0} and {1}")
    @MethodSource("earlierAndLaterDates")
    void equals_DifferentDates_ReturnsFalseBothWays(LocalDate earlier, LocalDate later) {
        Day earlierDay = new Day(earlier);
        Day laterDay = new Day(later);

        assertThat(earlierDay.equals(laterDay)).isFalse();
        assertThat(laterDay.equals(earlierDay)).isFalse();
    }

    @Test
    void equals_Null_ReturnsFalse() {
        assertThat(new Day(MARCH_4_2013).equals(null)).isFalse();
    }

    @Test
    void equals_LocalDateWithSameDate_ReturnsFalse() {
        assertThat(new Day(MARCH_4_2013).equals(new LocalDate(2013, 3, 4))).isFalse();
    }

    @Test
    void equals_SubclassWithSameDate_ReturnsFalseBothWays() {
        Day day = new Day(LEAP_DAY);
        Day subclass = new Day(LEAP_DAY) {};

        assertThat(day.equals(subclass)).isFalse();
        assertThat(subclass.equals(day)).isFalse();
    }

    @Test
    void equals_BothDatesNull_ReturnsTrue() {
        assertThat(new Day(null).equals(new Day(null))).isTrue();
    }

    @Test
    void equals_NullDateComparedWithDate_ReturnsFalse() {
        assertThat(new Day(null).equals(new Day(MARCH_4_2013))).isFalse();
    }

    @Test
    void equals_DateComparedWithNullDate_ReturnsFalse() {
        assertThat(new Day(MARCH_4_2013).equals(new Day(null))).isFalse();
    }

    @Test
    void hashCode_EqualDays_ReturnsSameHashCode() {
        assertThat(new Day(LEAP_DAY).hashCode()).isEqualTo(dayOf(2012, 2, 29).hashCode());
    }

    @Test
    void hashCode_BothDatesNull_ReturnsSameHashCode() {
        assertThat(new Day(null).hashCode()).isEqualTo(new Day(null).hashCode());
    }

    @Test
    void hashCode_DayInHashSet_IsFoundByEqualDay() {
        Set<Day> days = new HashSet<>(List.of(new Day(LEAP_DAY), new Day(MARCH_4_2013)));

        assertThat(days.contains(dayOf(2012, 2, 29))).isTrue();
        assertThat(days.contains(dayOf(2012, 3, 1))).isFalse();
    }

    @Test
    void toString_SingleDigitMonthAndDay_PadsWithZerosAndPutsDayFirst() {
        assertThat(new Day(MARCH_4_2013).toString()).isEqualTo("04/03/2013");
    }

    @Test
    void toString_LeapDay_ReturnsFebruary29() {
        assertThat(new Day(LEAP_DAY).toString()).isEqualTo("29/02/2012");
    }

    @Test
    void toString_LastDayOfYear_ReturnsDecember31() {
        assertThat(dayOf(2012, 12, 31).toString()).isEqualTo("31/12/2012");
    }

    @Disabled("documents bug DAY-02")
    @Test
    void toString_NullDate_DoesNotThrow() {
        assertThatCode(() -> new Day(null).toString()).doesNotThrowAnyException();
    }
}
