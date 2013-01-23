package org.noorm.jdbc;

/**
 * Operator definitions for the generic SQL query construction.
 * Supported are the operators equal-to (default, when used within QueryColumn), not-equal-to, greater-than,
 * greater-than-or-equal-to, less-than and less-than-or-equal-to. The exact syntax as listed is to be used when
 * configuring SQL queries using query declarations in the Maven generator plugin.
 *
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 20.01.13
 *         Time: 09:29
 */
public class Operator {

    private OperatorName operatorName;
    private String operatorSyntax;

    public Operator() {
        operatorName = OperatorName.EQUAL_TO;
    }

    public Operator(final String pOperatorName) {
        setOperatorName(pOperatorName);
    }

    public OperatorName getOperatorName() {
        return operatorName;
    }

    public void setOperatorName(final String pOperatorName) {

        String normalizedName = pOperatorName.toUpperCase();
        normalizedName = normalizedName.replace('-', '_');
        operatorName = OperatorName.valueOf(normalizedName);
        if (operatorName.equals(OperatorName.EQUAL_TO)) { operatorSyntax = " = "; }
        if (operatorName.equals(OperatorName.NOT_EQUAL_TO)) { operatorSyntax = " != "; }
        if (operatorName.equals(OperatorName.GREATER_THAN)) { operatorSyntax = " > "; }
        if (operatorName.equals(OperatorName.GREATER_THAN_OR_EQUAL_TO)) { operatorSyntax = " >= "; }
        if (operatorName.equals(OperatorName.LESS_THAN)) { operatorSyntax = " < "; }
        if (operatorName.equals(OperatorName.LESS_THAN_OR_EQUAL_TO)) { operatorSyntax = " =< "; }
        if (operatorName.equals(OperatorName.LIKE)) { operatorSyntax = " LIKE "; }
    }

    public String getOperatorSyntax() {
        return operatorSyntax;
    }

    enum OperatorName {

        EQUAL_TO("equal-to"),
        NOT_EQUAL_TO("not-equal-to"),
        GREATER_THAN("greater-than"),
        GREATER_THAN_OR_EQUAL_TO("greater-than-or-equal-to"),
        LESS_THAN("less-than"),
        LESS_THAN_OR_EQUAL_TO("less-than-or-equal-to"),
        LIKE("like");

        private OperatorName(final String pOperatorName) {
            operatorName = pOperatorName;
        }

        private String operatorName;

        public String getOperatorName() {
            return operatorName;
        }
    }
}
