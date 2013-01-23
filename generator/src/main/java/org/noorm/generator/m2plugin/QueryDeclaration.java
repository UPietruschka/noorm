package org.noorm.generator.m2plugin;

import org.noorm.jdbc.QueryColumn;

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
 * <br/>
 * To declare a query, at least the name of the used table or view must be specified and a (possibly empty) list
 * of columns, which compose the where-condition. Each column requires an operator with operator "equal" as
 * default.
 * Optional parameter "baseTable" is used, when the mapped bean is not constructed from a view directly, but from
 * a base table, i.e. one table, which is part of the view definition. Note that the used view must return the
 * same row structure as the base table.
 * Another optional parameter is the "methodName", which can be used to give the generated database access method
 * a custom name. Without this setting, the method name is constructed as "get<TAB>By<COL1><COL2>.." with <TAB>
 * denoting the first three characters of the table name or the base table (if specified) and <COLX> denoting the
 * first three characters of the column name(s).
 *
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 18.01.13
 *         Time: 19:48
 */
public class QueryDeclaration {

    private String tableName;
    private String baseTable;
    private String methodName;
    private boolean singleRowQuery = false;
    private List<QueryColumn> queryColumns = new ArrayList<QueryColumn>();

    public String getTableName() {
        return tableName;
    }

    public void setTableName(final String pTableName) {
        tableName = pTableName;
    }

    public String getBaseTable() {
        return baseTable;
    }

    public void setBaseTable(final String pBaseTable) {
        baseTable = pBaseTable;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(final String pMethodName) {
        methodName = pMethodName;
    }

    public boolean isSingleRowQuery() {
        return singleRowQuery;
    }

    public void setSingleRowQuery(final boolean pSingleRowQuery) {
        singleRowQuery = pSingleRowQuery;
    }

    public List<QueryColumn> getQueryColumns() {
        return queryColumns;
    }

    public void setQueryColumns(final List<QueryColumn> queryColumns) {
        this.queryColumns = queryColumns;
    }
}
