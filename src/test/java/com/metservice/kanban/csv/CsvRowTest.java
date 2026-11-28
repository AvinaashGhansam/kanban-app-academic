package com.metservice.kanban.csv;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.joda.time.LocalDate;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Unit tests for CsvRow.
 *
 * CsvRow looks a cell up by column name, so each getter has the same three input blocks, and the tests take one or
 * more values from each:
 *
 *   the column is missing from the header: every getter returns its default (an empty string, 0, false or null)
 *   instead of failing;
 *   the column exists and the cell is empty: getString and getDate return null, getBoolean
 *   returns false;
 *   the column exists and the cell has text: valid text is converted, and text that is not a number, a date or
 *   "true" is rejected or read as false, depending on the getter.
 *
 * setString is the only method that asks for a column index without first checking that the column exists,
 * so it is the one that fails for an unknown column. It is also tested by reading the value back through each getter.
 *
 * The disabled tests document bugs CR-01 (an empty int cell throws instead of returning the default), CR-02
 * setString(null) makes later reads of the cell throw) and CR-03 (a row with fewer cells than the header
 * throws when a missing cell is read).
 */
class CsvRowTest {

    private static CsvColumnNames columnNames(String... names) {
        return new CsvColumnNames(names);
    }

    private static CsvRow rowWith(String columnName, String cell) {
        return new CsvRow(columnNames(columnName), new String[] {cell});
    }

    @Test
    void getString_ExistingColumn_ReturnsCell() {
        assertThat(rowWith("name", "Ada").getString("name")).isEqualTo("Ada");
    }

    @Test
    void getString_SeveralColumns_ReturnsCellOfRequestedColumn() {
        CsvRow row = new CsvRow(columnNames("id", "name", "date"), new String[] {"7", "Ada", "2012-02-29"});

        assertThat(row.getString("id")).isEqualTo("7");
        assertThat(row.getString("name")).isEqualTo("Ada");
        assertThat(row.getString("date")).isEqualTo("2012-02-29");
    }

    @Test
    void getString_EmptyCell_ReturnsNull() {
        assertThat(rowWith("name", "").getString("name")).isNull();
    }

    @Test
    void getString_MissingColumn_ReturnsEmptyString() {
        assertThat(rowWith("name", "Ada").getString("missing")).isEmpty();
    }

    @Disabled("documents bug CR-03")
    @Test
    void getString_RowShorterThanHeader_ReturnsNull() {
        CsvRow row = new CsvRow(columnNames("id", "name"), new String[] {"7"});

        assertThat(row.getString("name")).isNull();
    }

    @ParameterizedTest(name = "\"{0}\" is {1}")
    @CsvSource({"0,0", "42,42", "-7,-7", "+5,5", "2147483647,2147483647", "-2147483648,-2147483648"})
    void getInt_NumericCell_ReturnsNumber(String cell, int expected) {
        assertThat(rowWith("size", cell).getInt("size")).isEqualTo(expected);
    }

    @ParameterizedTest(name = "cell \"{0}\"")
    @ValueSource(
            strings = {"abc", "1.5", "12abc", "1,000", " 42", "42 ", "0x10", "2147483648", "-2147483649"})
    void getInt_NonNumericCell_ThrowsNumberFormatException(String cell) {
        CsvRow row = rowWith("size", cell);

        assertThatThrownBy(() -> row.getInt("size")).isInstanceOf(NumberFormatException.class);
    }

    @Test
    void getInt_MissingColumn_ReturnsIntegerDefault() {
        assertThat(rowWith("size", "42").getInt("missing")).isEqualTo(CsvRow.INTEGER_DEFAULT);
    }

    @Disabled("documents bug CR-01")
    @Test
    void getInt_EmptyCell_ReturnsIntegerDefault() {
        assertThat(rowWith("size", "").getInt("size")).isEqualTo(CsvRow.INTEGER_DEFAULT);
    }

    @Test
    void getDate_ValidIsoCell_ReturnsDate() {
        assertThat(rowWith("start", "2013-03-04").getDate("start")).isEqualTo(new LocalDate(2013, 3, 4));
    }

    @Test
    void getDate_LeapDayCell_ReturnsFebruary29() {
        assertThat(rowWith("start", "2012-02-29").getDate("start")).isEqualTo(new LocalDate(2012, 2, 29));
    }

    @ParameterizedTest(name = "cell \"{0}\"")
    @ValueSource(strings = {"not a date", "29/02/2012", "2013-02-29", "2012-02-30", "2012-13-01"})
    void getDate_InvalidCell_ThrowsIllegalArgumentException(String cell) {
        CsvRow row = rowWith("start", cell);

        assertThatThrownBy(() -> row.getDate("start")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getDate_EmptyCell_ReturnsNull() {
        assertThat(rowWith("start", "").getDate("start")).isNull();
    }

    @Test
    void getDate_MissingColumn_ReturnsNull() {
        assertThat(rowWith("start", "2012-02-29").getDate("missing")).isNull();
    }

    @ParameterizedTest(name = "cell \"{0}\"")
    @ValueSource(strings = {"true", "TRUE", "True", "tRuE"})
    void getBoolean_TrueInAnyCase_ReturnsTrue(String cell) {
        assertThat(rowWith("done", cell).getBoolean("done")).isTrue();
    }

    @ParameterizedTest(name = "cell \"{0}\"")
    @ValueSource(strings = {"false", "yes", "1", "y", "on", "garbage"})
    void getBoolean_OtherText_ReturnsFalse(String cell) {
        assertThat(rowWith("done", cell).getBoolean("done")).isFalse();
    }

    @Test
    void getBoolean_EmptyCell_ReturnsFalse() {
        assertThat(rowWith("done", "").getBoolean("done")).isFalse();
    }

    @Test
    void getBoolean_MissingColumn_ReturnsFalse() {
        assertThat(rowWith("done", "true").getBoolean("missing")).isFalse();
    }

    @Test
    void setString_ExistingColumn_ThenGetString_ReturnsNewValue() {
        CsvRow row = rowWith("name", "Ada");

        row.setString("name", "Grace");

        assertThat(row.getString("name")).isEqualTo("Grace");
    }

    @Test
    void setString_ExistingColumn_LeavesOtherCellsUnchanged() {
        CsvRow row = new CsvRow(columnNames("id", "name", "date"), new String[] {"7", "Ada", "2012-02-29"});

        row.setString("name", "Grace");

        assertThat(row.getString("id")).isEqualTo("7");
        assertThat(row.getString("date")).isEqualTo("2012-02-29");
    }

    @Test
    void setString_EmptyString_ThenGetString_ReturnsNull() {
        CsvRow row = rowWith("name", "Ada");

        row.setString("name", "");

        assertThat(row.getString("name")).isNull();
    }

    @Test
    void setString_NumericText_ThenGetInt_ReturnsNumber() {
        CsvRow row = rowWith("size", "1");

        row.setString("size", "13");

        assertThat(row.getInt("size")).isEqualTo(13);
    }

    @Test
    void setString_IsoDate_ThenGetDate_ReturnsDate() {
        CsvRow row = rowWith("start", "");

        row.setString("start", "2012-02-29");

        assertThat(row.getDate("start")).isEqualTo(new LocalDate(2012, 2, 29));
    }

    @Test
    void setString_MissingColumn_ThrowsRuntimeException() {
        CsvRow row = rowWith("name", "Ada");

        assertThatThrownBy(() -> row.setString("missing", "Grace")).isInstanceOf(RuntimeException.class);
    }

    @Disabled("documents bug CR-02")
    @Test
    void setString_Null_ThenGetString_ReturnsNull() {
        CsvRow row = rowWith("name", "Ada");

        row.setString("name", null);

        assertThat(row.getString("name")).isNull();
    }
}