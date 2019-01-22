package org.noorm.jdbc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Helper class for extensive SQL logging in DEBUG logging level.
 *
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 20.01.19
 *         Time: 16:31
 */
public class LoggingHelper {

    private static final Logger log = LoggerFactory.getLogger(LoggingHelper.class);

    public void debugSQLCall(final String pTableName,
                             final Map<QueryColumn, Object> pQueryParameters,
                             final Class pBeanClass,
                             final FilterExtension pFilterExtension) {

        final StringBuilder formattedParameters = new StringBuilder();
        formattedParameters.append("Executing SQL statement on table ").append(pTableName);
        if (pFilterExtension != null) {
            formattedParameters.append("\nFilter extension: Offset: ");
            formattedParameters.append(pFilterExtension.getOffset());
            formattedParameters.append(", Limit: ");
            formattedParameters.append(pFilterExtension.getLimit());
            for (final FilterExtension.SortCriteria sortCriteria : pFilterExtension.getSortCriteria()) {
                formattedParameters.append("\n  ").append(sortCriteria.getColumnName());
                formattedParameters.append(" / ").append(sortCriteria.getDirection());
            }
        }
        log.debug(debugQueryColumns(formattedParameters, pQueryParameters, pBeanClass).toString());
    }

    public void debugUpdate(final String pTableName,
                            final Map<String, Object> pUpdateParameters,
                            final Map<QueryColumn, Object> pQueryParameters,
                            final Class pBeanClass) {

        final StringBuilder formattedParameters = new StringBuilder();
        formattedParameters.append("Executing UPDATE statement on table ").append(pTableName);
        String prefix = "\nUpdate parameters: ";
        for (final String updateColumn : pUpdateParameters.keySet()) {
            final Object parameter = pUpdateParameters.get(updateColumn);
            String parameterToString = Utils.getParameter2String(parameter);
            formattedParameters.append(prefix).append(updateColumn).append(" = ").append(parameterToString);
            prefix = "\n                  ";
        }
        log.debug(debugQueryColumns(formattedParameters, pQueryParameters, pBeanClass).toString());
    }

    public void debugDelete(final String pTableName,
                            final Map<QueryColumn, Object> pQueryParameters,
                            final Class pBeanClass) {

        final StringBuilder formattedParameters = new StringBuilder();
        formattedParameters.append("Executing DELETE statement on table ").append(pTableName);
        log.debug(debugQueryColumns(formattedParameters, pQueryParameters, pBeanClass).toString());
    }

    private StringBuilder debugQueryColumns(final StringBuilder formattedParameters,
                                            final Map<QueryColumn, Object> pQueryParameters,
                                            final Class pBeanClass) {
        if (pQueryParameters != null) {
            String prefix = "\nQuery parameters: ";
            for (final QueryColumn queryColumn : pQueryParameters.keySet()) {
                final String paramName = queryColumn.getColumnName();
                final Object parameter = pQueryParameters.get(queryColumn);
                String parameterToString = Utils.getParameter2String(parameter);
                formattedParameters.append(prefix).append(paramName)
                        .append(queryColumn.getOperator().getOperatorSyntax()).append(parameterToString);
                prefix = "\n                  ";
            }
        }
        if (pBeanClass != null) {
            formattedParameters.append("\nBean Class:        ").append(pBeanClass.getName());
        }
        return formattedParameters;
    }

    public void debugDML(final String pTableName, final String pSequenceName, final String pStatement) {

        final StringBuilder logMessage = new StringBuilder();
        if (pStatement.toUpperCase().startsWith("INSERT")) {
            logMessage.append("Inserting into ").append(pTableName);
            logMessage.append(" with sequence ").append(pSequenceName);
            logMessage.append(".\n");
            logMessage.append("Using insert statement: ").append(pStatement);
        } else {
            if (pStatement.toUpperCase().startsWith("UPDATE")) {
                logMessage.append("Updating ").append(pTableName);
                logMessage.append(".\n");
                logMessage.append("Using update statement: ").append(pStatement);
            } else { // DELETE
                logMessage.append("Deleting from ").append(pTableName);
                logMessage.append(".\n");
                logMessage.append("Using delete statement: ").append(pStatement);
            }
        }
        log.debug(logMessage.toString());
    }

    public void debugProcedureCall(final String pCallable,
                                   final Map<String, Object> pInParameters,
                                   final Class pBeanClass) {

        final StringBuilder formattedParameters = new StringBuilder();
        formattedParameters.append("Calling stored procedure ").append(pCallable);
        if (pInParameters != null) {
            String prefix = "\nInput parameters: ";
            for (final String paramName : pInParameters.keySet()) {
                final Object parameter = pInParameters.get(paramName);
                String parameterToString = Utils.getParameter2String(parameter);
                formattedParameters.append(prefix).append(paramName).append(" : ").append(parameterToString);
                prefix = "\n                  ";
            }
        }
        if (pBeanClass != null) {
            formattedParameters.append("\nBean Class:        ").append(pBeanClass.getName());
        }
        log.debug(formattedParameters.toString());
    }

    public void debugProcedureTermination(final String pCallable,
                                          final int pRowsProcessed) {

        StringBuilder logMessage = new StringBuilder();
        logMessage.append("Stored procedure ").append(pCallable).append(" successfully terminated. ");
        if (pRowsProcessed >= 0) {
            logMessage.append(Integer.toString(pRowsProcessed)).append(" rows processed.");
        }
        log.debug(logMessage.toString());
    }
}
