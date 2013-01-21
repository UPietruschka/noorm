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
 *
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 18.01.13
 *         Time: 19:48
 */
public class QueryDeclaration {

    private String tableName;
    private List<QueryColumn> queryColumns = new ArrayList<QueryColumn>();

    public String getTableName() {
        return tableName;
    }

    public void setTableName(final String pTableName) {
        tableName = pTableName;
    }

    public List<QueryColumn> getQueryColumns() {
        return queryColumns;
    }

    public void setQueryColumns(List<QueryColumn> queryColumns) {
        this.queryColumns = queryColumns;
    }
}
