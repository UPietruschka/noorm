package org.noorm.generator.enumgenerator;

/**
 * Velocity attribute descriptor for the Bean generator
 *
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 03.05.11
 *         Time: 17:12
 */
public class EnumAttributeDescriptor {

	private String name;
    private String methodNamePostfix;
	private String columnName;
	private String type;

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
}
