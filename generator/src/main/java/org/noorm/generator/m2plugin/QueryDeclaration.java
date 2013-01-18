package org.noorm.generator.m2plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Complex type definition for the NoORM Maven generator plugin.
 * The NoORM query declaration is intentionally much simpler than most other approaches to specify alternatives
 * to original SQL. While specifications like the JPA 2.0 criteria API aim to cover most of the capabilities of
 * SQL, this approach consequently follows the paradigm to move database-centric functionality to the database.
 * In particular, this means that the complexity of an SQL statement should be implemented inside the database
 * using views. Following this approach, it is almost always possible to reduce the query declaration for
 * automatic Java code generation to a single entity (table, view), the columns subject to the where-conditions
 * and the operators used for the columns in the where-conditions.
 *
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 18.01.13
 *         Time: 19:48
 */
public class QueryDeclaration {

    private String tableName;
    private List<QueryColumns> queryColumns = new ArrayList<QueryColumns>();

    public String getTableName() {
        return tableName;
    }

    public void setTableName(final String pTableName) {
        tableName = pTableName;
    }

    public List<QueryColumns> getQueryColumns() {
        return queryColumns;
    }

    public void setQueryColumns(List<QueryColumns> queryColumns) {
        this.queryColumns = queryColumns;
    }

    public static class QueryColumns {

        private String columnName;
        private Operator operator;

        public String getColumnName() {
            return columnName;
        }

        public void setColumnName(final String pColumnName) {
            columnName = pColumnName;
        }

        public Operator getOperator() {
            return operator;
        }

        public void setOperator(final Operator pOperator) {
            operator = pOperator;
        }
    }

    public static class Operator {

        private OperatorName operatorName;
        private String operatorSyntax;

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
        }

        public String getOperatorSyntax() {
            return operatorSyntax;
        }
    }

    enum OperatorName {

        EQUAL_TO("equal-to"),
        NOT_EQUAL_TO("not-equal-to"),
        GREATER_THAN("greater-than"),
        GREATER_THAN_OR_EQUAL_TO("greater-than-or-equal-to"),
        LESS_THAN("less-than"),
        LESS_THAN_OR_EQUAL_TO("less-than-or-equal-to");

        private OperatorName(final String pOperatorName) {
            operatorName = pOperatorName;
        }

        private String operatorName;

        public String getOperatorName() {
            return operatorName;
        }
    }
}
