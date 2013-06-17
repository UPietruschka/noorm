package org.noorm.generator;

import org.noorm.generator.schema.Operator;

/**
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 17.05.11
 *         Time: 08:16
 */
public class ParameterDescriptor {

	private String javaName;
	private String oracleName;
	private String javaType;
    // The operator and the unaryOperator-indicator are only used for declared queries
    private Operator operator;
    private boolean unaryOperator = false;

	public void setJavaName(final String pJavaName) {
		javaName = pJavaName;
	}

    public String getJavaName() {
		return javaName;
	}

	public String getOracleName() {
		return oracleName;
	}

	public void setOracleName(final String pOracleName) {
		oracleName = pOracleName;
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

    public Operator getOperator() {
        return operator;
    }

    public void setOperator(final Operator pOperator) {
        operator = pOperator;
    }

    public boolean isUnaryOperator() {
        return unaryOperator;
    }

    public void setUnaryOperator(final boolean pUnary) {
        unaryOperator = pUnary;
    }
}
