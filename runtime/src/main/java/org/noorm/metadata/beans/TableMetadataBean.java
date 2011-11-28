package org.noorm.metadata.beans;

import org.noorm.jdbc.JDBCColumn;

/**
 * Bean containing the table metadata for a given schema
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 16.04.11
 *         Time: 18:10
 */
public class TableMetadataBean {

	@JDBCColumn(name="TABLE_NAME", updatable=false)
	private String tableName;
	@JDBCColumn(name="COLUMN_NAME", updatable=false)
	private String columnName;
	@JDBCColumn(name="DATA_TYPE", updatable=false)
	private String dataType;
	@JDBCColumn(name="DATA_PRECISION", updatable=false)
	private Long dataPrecision;
	@JDBCColumn(name="DATA_SCALE", updatable=false)
	private Long dataScale;
	@JDBCColumn(name="CHAR_LENGTH", updatable=false)
	private Long charLength;
	@JDBCColumn(name="NULLABLE", updatable=false)
	private String nullable;
	@JDBCColumn(name="COLUMN_ID", updatable=false)
	private Long columnId;
	@JDBCColumn(name="UPDATABLE", updatable=false)
	private String updatable;
	@JDBCColumn(name="INSERTABLE", updatable=false)
	private String insertable;

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

	public String getDataType() {
		return dataType;
	}

	public void setDataType(final String pDataType) {
		dataType = pDataType;
	}

	public Long getDataPrecision() {
		return dataPrecision;
	}

	public void setDataPrecision(final Long pDataPrecision) {
		dataPrecision = pDataPrecision;
	}

	public Long getDataScale() {
		return dataScale;
	}

	public void setDataScale(final Long pDataScale) {
		dataScale = pDataScale;
	}

	public Long getCharLength() {
		return charLength;
	}

	public void setCharLength(final Long pCharLength) {
		charLength = pCharLength;
	}

	public String getNullable() {
		return nullable;
	}

	public void setNullable(final String pNullable) {
		nullable = pNullable;
	}

	public Long getColumnId() {
		return columnId;
	}

	public void setColumnId(final Long pColumnId) {
		columnId = pColumnId;
	}

	public String getUpdatable() {
		return updatable;
	}

	public void setUpdatable(final String pUpdatable) {
		updatable = pUpdatable;
	}

	public String getInsertable() {
		return insertable;
	}

	public void setInsertable(final String pInsertable) {
		insertable = pInsertable;
	}
}
