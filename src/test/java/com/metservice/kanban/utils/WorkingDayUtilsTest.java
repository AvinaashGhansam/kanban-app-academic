package com.metservice.kanban.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

import org.joda.time.LocalDate;

/**
 * 
 * Test names follow Roy Osherove's convention:
 * UnitOfWork_StateUnderTest_ExpectedBehavior.
 */
public class WorkingDayUtilsTest {

    @Test
    void getWorkingDaysBetween_AcceptanceAfterInception_ReturnCorrectCount() {

        LocalDate inceptionDate = new LocalDate(2026, 11, 23); // Monday
        LocalDate acceptanceDate = new LocalDate(2026, 11, 24); // Tuesday

        int result = WorkingDayUtils.getWorkingDaysBetween(inceptionDate, acceptanceDate);

        assertThat(result).isEqualTo(1);
    }

    @Test
    void getWorkingDaysBetween_AcceptanceSameInception_ReturnZero() {

        LocalDate inceptionDate = new LocalDate(2026, 11, 23); // Monday
        LocalDate acceptanceDate = inceptionDate;

        int result = WorkingDayUtils.getWorkingDaysBetween(inceptionDate, acceptanceDate);

        assertThat(result).isEqualTo(0);
    }

    @Test
    void getWorkingDaysBetween_AcceptanceBeforeInception_ReturnZero() {

        LocalDate acceptanceDate = new LocalDate(2026, 11, 23); // Monday
        LocalDate inceptionDate = new LocalDate(2026, 11, 24); // Tuesday

        int result = WorkingDayUtils.getWorkingDaysBetween(inceptionDate, acceptanceDate);

        assertThat(result).isEqualTo(0);
    }

    @Test
    void getWorkingDaysBetween_AcceptanceAfterInceptionWithNonWorkingDays_ReturnCorrectCount() {

        LocalDate acceptanceDate = new LocalDate(2026, 11, 23); // Monday
        LocalDate inceptionDate = new LocalDate(2026, 11, 30); // Monday After a Weekend

        int result = WorkingDayUtils.getWorkingDaysBetween(acceptanceDate, inceptionDate);

        assertThat(result).isEqualTo(5);
    }

    @Test
    void isWorkingDay_WorkingDay_ReturnTrue() {

        LocalDate testDate = new LocalDate(2026, 11, 23); // Monday

        boolean result = WorkingDayUtils.isWorkingDay(testDate);

        assertThat(result).isTrue();
    }

    @Test
    void isWorkingDay_Weekend_ReturnFalse() {

        LocalDate testDate = new LocalDate(2026, 11, 21); // Saturday

        boolean result = WorkingDayUtils.isWorkingDay(testDate);

        assertThat(result).isFalse();
    }

    @Test
    void isWorkingDay_Holiday_ReturnFalse() {

        LocalDate testDate = new LocalDate(2010, 12, 27); // Holiday

        boolean result = WorkingDayUtils.isWorkingDay(testDate);

        assertThat(result).isFalse();
    }

    @Test
    void nextWorkingDay_MondayNoHoliday_ReturnMonday() {

        LocalDate testDate = new LocalDate(2026, 11, 23); // Monday
        LocalDate expected = testDate; // Is working day so is the next

        LocalDate result = WorkingDayUtils.nextWorkingDay(testDate);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void nextWorkingDay_SundayNoHolidays_ReturnMonday() {

        LocalDate testDate = new LocalDate(2026, 11, 22);
        LocalDate expected = new LocalDate(2026, 11, 23);

        LocalDate result = WorkingDayUtils.nextWorkingDay(testDate);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void nextWorkingDay_Holiday_ReturnWorkingDayAfterHoliday() {

        LocalDate testDate = new LocalDate(2010, 12, 27);
        LocalDate expected = new LocalDate(2010, 12, 29);

        LocalDate result = WorkingDayUtils.nextWorkingDay(testDate);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void nextWorkingDay_Null_ReturnNull() {
        LocalDate result = WorkingDayUtils.nextWorkingDay(null);

        assertThat(result).isEqualTo(null);
    }
}
