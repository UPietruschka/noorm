package org.noorm.jdbc;

/**
 * Enum for supported version column types used for optimistic locking
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 */
public enum VersionColumnType {

    NUMBER("NUMBER"),
    DATE("DATE"),
    TIMESTAMP("TIMESTAMP");

    private String columnType;

    private VersionColumnType(final String pColumnType) {
        columnType = pColumnType;
    }

    public String getColumnType() {
        return columnType;
    }
}
