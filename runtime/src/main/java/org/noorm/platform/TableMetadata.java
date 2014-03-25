package org.noorm.platform;

/**
 * Table metadata container
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 25.03.14
 *         Time: 18:10
 */
public class TableMetadata {

	private String tableName;
	private String columnName;
	private int dataType;
    private String typeName;
	private int decimalDigits;
	private int columnSize;
	private boolean nullable;
	private boolean updatable;

	public String getTableName() {
		return tableName;
	}

	public void setTableName(final String pTableName) {
		tableName = pTableName;
	}

	public String getColumnName() {
		return columnName;
	}

	public void setColumnName(final String pColumnName) {
		columnName = pColumnName;
	}

	public int getDataType() {
		return dataType;
	}

	public void setDataType(final int pDataType) {
		dataType = pDataType;
	}

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(final String pTypeName) {
        typeName = pTypeName;
    }

    public int getDecimalDigits() {
		return decimalDigits;
	}

	public void setDecimalDigits(final int pDecimalDigits) {
        decimalDigits = pDecimalDigits;
	}

	public int getColumnSize() {
		return columnSize;
	}

	public void setColumnSize(final int pColumnSize) {
        columnSize = pColumnSize;
	}

	public boolean getNullable() {
		return nullable;
	}

	public void setNullable(final boolean pNullable) {
		nullable = pNullable;
	}

	public boolean getUpdatable() {
		return updatable;
	}

	public void setUpdatable(final boolean pUpdatable) {
		updatable = pUpdatable;
	}
}
