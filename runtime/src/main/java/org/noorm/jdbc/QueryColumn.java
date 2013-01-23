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

    public QueryColumn () {
        operator = new Operator();
    }

    public QueryColumn(final String pColumnName, final Operator pOperator) {
        columnName = pColumnName;
        operator = pOperator;
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

    @Override
    public int compareTo(final Object pQueryColumn) {
        return columnName.compareTo(((QueryColumn) pQueryColumn).getColumnName());
    }
}
