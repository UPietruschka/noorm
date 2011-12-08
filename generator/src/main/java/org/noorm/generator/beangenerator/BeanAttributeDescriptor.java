package org.noorm.generator.beangenerator;

/**
 * Velocity attribute descriptor for the Bean generator
 *
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 03.05.11
 *         Time: 17:12
 */
class BeanAttributeDescriptor {

	private String name;
	private String columnName;
	private String type;
	private String dataType;
	private boolean updatable = true;
	private boolean nullable = true;
	private int maxLength = 0;

	public void setName(final String pName) {

		name = pName;
	}

	public String getName() {
		return name;
	}

	public String getColumnName() {
		return columnName;
	}

	public void setColumnName(final String pColumnName) {
		columnName = pColumnName;
	}

	public void setType(final String pType) {
		type = pType;
	}

	public String getType() {
		return type;
	}

	public String getDataType() {
		return dataType;
	}

	public void setDataType(final String pDataType) {
		dataType = pDataType;
	}

	public String getFirstUpperName() {
		return name.toUpperCase().substring(0, 1).concat(name.substring(1));
	}

	public boolean isUpdatable() {
		return updatable;
	}

	public void setUpdatable(final boolean pUpdatable) {
		updatable = pUpdatable;
	}

	public int getMaxLength() {
		return maxLength;
	}

	public void setMaxLength(final int pMaxLength) {
		maxLength = pMaxLength;
	}

	public boolean isNullable() {
		return nullable;
	}

	public void setNullable(final boolean pNullable) {
		nullable = pNullable;
	}
}
