package org.noorm.generator;

import org.noorm.generator.schema.QueryColumn;
import java.util.List;

/**
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 21.01.13
 *         Time: 13:50
 */
public interface IDeclaration {

    /**
     * Returns the table name for the SQL/DML code generation
     * @return the table name
     */
    String getTableName();

    /**
     * Returns the name of the Java method subject to code generation
     * @return the method name
     */
    String getGeneratedMethodName();

    /**
     * Returns the query columns specified for code generation
     * @return the query columns
     */
    List<QueryColumn> getQueryColumn();
}
