package org.noorm.jdbc;

/**
 * Query column definition for the generic SQL query construction.
 *
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 20.01.13
 *         Time: 09:37
 */
public class QueryColumn implements Comparable {

    private String columnName;
    private Operator operator;
    private String customExpression;

    public QueryColumn () {
        operator = new Operator();
    }

    public QueryColumn(final String pColumnName, final Operator pOperator, final String pCustomExpression) {
        columnName = pColumnName;
        operator = pOperator;
        customExpression = pCustomExpression;
    }

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

    public String getCustomExpression() {
        return customExpression;
    }

    public void setCustomExpression(final String pCustomExpression) {
        customExpression = pCustomExpression;
    }

    @Override
    public int compareTo(final Object pQueryColumn) {
        final String thisComparatorValue = columnName + operator.getOperatorName().name();
        final QueryColumn other = (QueryColumn) pQueryColumn;
        final String otherComparatorValue = other.getColumnName() + other.getOperator().getOperatorName().name();
        return thisComparatorValue.compareTo(otherComparatorValue);
    }
}
