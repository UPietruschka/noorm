package org.noorm.metadata.beans;

import org.noorm.jdbc.JDBCColumn;

/**
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 15.05.11
 *         Time: 17:47
 */
public class ParameterBean {

	@JDBCColumn(name="NAME", updatable=false)
	private String name;
	@JDBCColumn(name="DATA_TYPE", updatable=false)
	private String dataType;
	@JDBCColumn(name="TYPE_NAME", updatable=false)
	private String typeName;
	@JDBCColumn(name="DIRECTION", updatable=false)
	private String direction;

	public String getName() {
		return name;
	}

	public void setName(final String pName) {
		name = pName;
	}

	public String getDataType() {
		return dataType;
	}

	public void setDataType(final String pDataType) {
		dataType = pDataType;
	}

	public String getTypeName() {
		return typeName;
	}

	public void setTypeName(final String pTypeName) {
		typeName = pTypeName;
	}

	public String getDirection() {
		return direction;
	}

	public void setDirection(final String pDirection) {
		direction = pDirection;
	}
}
