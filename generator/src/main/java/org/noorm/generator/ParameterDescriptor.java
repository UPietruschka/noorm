package org.noorm.generator;

import org.noorm.generator.schema.OperatorName;

/**
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 17.05.11
 *         Time: 08:16
 */
public class ParameterDescriptor {

	private String javaName;
	private String dbParamName;
	private String javaType;
    // The operator and the unaryOperator-indicator are only used for declared queries
    private OperatorName operator;
    private boolean unaryOperator = false;
    private boolean isList = false;
    private String customExpression;

	public void setJavaName(final String pJavaName) {
		javaName = pJavaName;
	}

    public String getJavaName() {
		return javaName;
	}

	public String getDbParamName() {
		return dbParamName;
	}

	public void setDbParamName(final String pDbParamName) {
		dbParamName = pDbParamName;
	}

	public String getJavaType() {
		return javaType;
	}

	public void setJavaType(final String pJavaType) {
		javaType = pJavaType;
	}

	public String getFirstUpperName() {
		return javaName.toUpperCase().substring(0, 1).concat(javaName.substring(1));
	}

    public OperatorName getOperator() {
        return operator;
    }

    public void setOperator(final OperatorName pOperator) {
        operator = pOperator;
    }

    public boolean isUnaryOperator() {
        return unaryOperator;
    }

    public void setUnaryOperator(final boolean pUnary) {
        unaryOperator = pUnary;
    }

	public boolean isList() {
		return isList;
	}

	public void setIsList(final boolean pIsList) {
		isList = pIsList;
	}

	public String getCustomExpression() {
		return customExpression;
	}

	public void setCustomExpression(final String pCustomExpression) {
		customExpression = pCustomExpression;
	}

	public boolean hasCustomExpression() {
		return customExpression != null;
	}
}
