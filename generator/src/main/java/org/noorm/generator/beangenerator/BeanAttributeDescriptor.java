package org.noorm.generator.beangenerator;

/**
 * Velocity attribute descriptor for the Bean generator
 *
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 03.05.11
 *         Time: 17:12
 */
public class BeanAttributeDescriptor {

	private String name;
    private String methodNamePostfix;
	private String columnName;
	private String type;
	private String dataType;
	private boolean insertable = true;
	private boolean updatable = true;
	private boolean nullable = true;
    private boolean caseSensitiveName = false;
	private int maxLength = 0;

    public void setName(final String pName) {
		name = pName;
	}

	public String getName() {
		return name;
	}

    public String getMethodNamePostfix() {
        return methodNamePostfix;
    }

    public void setMethodNamePostfix(final String pMethodNamePostfix) {
        methodNamePostfix = pMethodNamePostfix;
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

	public boolean isInsertable() {
		return insertable;
	}

	public void setInsertable(final boolean pInsertable) {
		insertable = pInsertable;
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

    public boolean isCaseSensitiveName() {
        return caseSensitiveName;
    }

    public void setCaseSensitiveName(final boolean pCaseSensitiveName) {
        caseSensitiveName = pCaseSensitiveName;
    }
}
