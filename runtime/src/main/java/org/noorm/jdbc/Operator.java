package org.noorm.jdbc;

/**
 * Operator definitions for the generic SQL query construction.
 * Supported are the operators EQUAL_TO (default, when used within QueryColumn), NOT_EQUAL_TO, GREATER_THAN,
 * GREATER_THAN_OR_EQUAL_TO, LESS_THAN, LESS_THAN_OR_EQUAL_TO, LIKE, IS_NULL and IS_NOT_NULL. The exact syntax as
 * listed is to be used when configuring SQL queries using query declarations in the Maven generator plugin.
 *
 * The setter for the operator name with the String parameter is used by Maven 2.2.1 (and earlier versions) only.
 * Maven 3.X is able to resolve enums and is thus able to the use the setter with the enum instead.
 *
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 20.01.13
 *         Time: 09:29
 */
public class Operator {

    private Name operatorName;
    private String operatorSyntax;
    private boolean unary = false;

    public Operator() {
        operatorName = Name.EQUAL_TO;
    }

    public Operator(final String pOperatorName) {
        setOperatorName(pOperatorName);
    }

    public Operator(final Name pOperatorName) {
        setOperatorName(pOperatorName);
    }

    public Name getOperatorName() {
        return operatorName;
    }

    public void setOperatorName(final String pOperatorName) {

        operatorName = Name.valueOf(pOperatorName);
        setOperatorName(operatorName);
    }

    public void setOperatorName(final Name pOperatorName) {

        operatorName = pOperatorName;
        if (operatorName.equals(Name.EQUAL_TO)) { operatorSyntax = " = "; }
        if (operatorName.equals(Name.NOT_EQUAL_TO)) { operatorSyntax = " != "; }
        if (operatorName.equals(Name.GREATER_THAN)) { operatorSyntax = " > "; }
        if (operatorName.equals(Name.GREATER_THAN_OR_EQUAL_TO)) { operatorSyntax = " >= "; }
        if (operatorName.equals(Name.LESS_THAN)) { operatorSyntax = " < "; }
        if (operatorName.equals(Name.LESS_THAN_OR_EQUAL_TO)) { operatorSyntax = " <= "; }
        if (operatorName.equals(Name.LIKE)) { operatorSyntax = " LIKE "; }
        if (operatorName.equals(Name.IS_NULL)) {
            operatorSyntax = " IS NULL ";
            unary = true;
        }
        if (operatorName.equals(Name.IS_NOT_NULL)) {
            operatorSyntax = " IS NOT NULL ";
            unary = true;
        }
        if (operatorName.equals(Name.IN)) { operatorSyntax = " IN "; }
    }

    public String getOperatorSyntax() {
        return operatorSyntax;
    }

    public boolean isUnary() {
        return unary;
    }

    public void setUnary(final boolean pUnary) {
        unary = pUnary;
    }

    public enum Name {

        EQUAL_TO("EQUAL_TO"),
        NOT_EQUAL_TO("NOT_EQUAL_TO"),
        GREATER_THAN("GREATER_THAN"),
        GREATER_THAN_OR_EQUAL_TO("GREATER_THAN_OR_EQUAL_TO"),
        LESS_THAN("LESS_THAN"),
        LESS_THAN_OR_EQUAL_TO("LESS_THAN_OR_EQUAL_TO"),
        LIKE("LIKE"),
        IS_NULL("IS_NULL"),
        IS_NOT_NULL("IS_NOT_NULL"),
        IN("IN"),
        CUSTOM("CUSTOM");

        Name(final String pOperatorName) {
            operatorName = pOperatorName;
        }

        private String operatorName;

        public String getOperatorName() {
            return operatorName;
        }
    }
}
