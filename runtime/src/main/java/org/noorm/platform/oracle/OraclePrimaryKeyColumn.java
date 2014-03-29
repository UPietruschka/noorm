package org.noorm.platform.oracle;

import org.noorm.jdbc.JDBCColumn;

/**
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 04.09.11
 *         Time: 14:55
 */
public class OraclePrimaryKeyColumn {

	@JDBCColumn(name="TABLE_NAME", updatable=false)
	private String tableName;
	@JDBCColumn(name="COLUMN_NAME", updatable=false)
	private String columnName;

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
}
