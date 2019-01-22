package org.noorm.jdbc;

import org.noorm.jdbc.platform.IPlatform;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Utility methods to support JDBCProcedureProcessor and the class generators.
 *
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 06.05.11
 *         Time: 14:23
 */
public class Utils {

	public static final String JAVA_SOURCE_FILE_APPENDIX = ".java";

	private static final String DB_NAME_TOKEN_SPLIT = "_";
	private static final String ENUM_UNSUPPORTED_REGEX = "[ /\\-\\,\\.;]";
	private static final String ENUM_UNSUPPORTED_REGEX_SUBSTITUTE = "_";

    /**
     * Converts a database object name into a java name. Database names (table names, column names, etc.) are
     * typically stored in the data dictionary in upper case. Most database modellers follow the convention
     * to separate parts of the name by underscores, e.g. "FIRST_NAME", "ORDER_NUMBER", etc. In Java, a widely
     * used convention is to separate parts of the name by capitalizing the first name of the new part, e.g.
     * "firstName", "orderNumber".
     *
     * @param pDBName a database object name
     * @param pCapitalizeFirst indicator, whether the first letter in the converted name is to be capitalized
     * @return the converted name
     */
	public static String convertDBName2JavaName(final String pDBName, final boolean pCapitalizeFirst) {

		final String[] dbNameTokens = pDBName.split(DB_NAME_TOKEN_SPLIT);
		final StringBuilder javaName = new StringBuilder();
		for (int i = 0; i < dbNameTokens.length; i++) {
            // Two consecutive underscores produce a single empty token, which is ignored
            if (dbNameTokens[i].length() > 0) {
                if (!pCapitalizeFirst && i == 0) {
                    javaName.append(dbNameTokens[i].toLowerCase().charAt(0));
                } else {
                    javaName.append(dbNameTokens[i].toUpperCase().charAt(0));
                }
                javaName.append(dbNameTokens[i].substring(1).toLowerCase());
            }
		}
		return javaName.toString();
	}

    /**
     * Converts the given input into a valid format for the Java enum display name.
     * Java enum display names are not allowed to contain the characters " ", "/", "-", ",", ".", ";".
     * These characters are replaced by an underscore. In addition, when the name starts with a digit,
     * an underscore is placed at the beginning of the normalized name.
     *
     * @param pDisplayNameValue the name to be normalized
     * @return the normalized name
     */
	public static String getNormalizedDisplayColumnValue(final String pDisplayNameValue) {

        String prefix = "";
        if (pDisplayNameValue.matches("[0-9].*")) {
            prefix = ENUM_UNSUPPORTED_REGEX_SUBSTITUTE;
        }
		return prefix.concat(pDisplayNameValue.trim().toUpperCase().replaceAll
                (ENUM_UNSUPPORTED_REGEX, ENUM_UNSUPPORTED_REGEX_SUBSTITUTE));
	}

    /**
     * Formats a bind parameter for debug output.
     *
     * @param pParameter the parameter
     * @return the parameter String representation
     */
    public static String getParameter2String(final Object pParameter) {

        String parameterToString;
        if (pParameter instanceof byte[]) {
            if (((byte[]) pParameter).length < 4096) {
                final HexBinaryAdapter hexBinaryAdapter = new HexBinaryAdapter();
                parameterToString = hexBinaryAdapter.marshal((byte[]) pParameter);
            } else {
                parameterToString = "Binary content too large for debug output.";
            }
        } else {
            if (pParameter instanceof Long[]) {
                final StringBuilder formattedIDList = new StringBuilder();
                String delimiter = "";
                for (final Long id : (Long[]) pParameter) {
                    formattedIDList.append(delimiter).append(id);
                    delimiter = ", ";
                }
                parameterToString = formattedIDList.toString();
            } else {
                if (pParameter instanceof List) {
                    final StringBuilder formattedList = new StringBuilder();
                    String delimiter = "";
                    for (final Object o : (List) pParameter) {
                        formattedList.append(delimiter).append(o);
                        delimiter = ", ";
                    }
                    parameterToString = formattedList.toString();
                } else {
                    if (pParameter != null) {
                        parameterToString = pParameter.toString();
                    } else {
                        parameterToString = "NULL";
                    }
                }
            }
        }
        return parameterToString;
    }

    /**
     * Sets the provided query parameters for the provided prepared statement for further processing.
     *
     * @param pQueryParameters the map of query parameters
     * @param pStmt the JDBC prepared statement
     * @param pParameterIndex the current parameter index within the JDBC prepared statement
     * @throws SQLException SQL processing exception thrown by database driver
     */
    public void setQueryParameter(final Map<QueryColumn, Object> pQueryParameters,
                                  final PreparedStatement pStmt,
                                  int pParameterIndex) throws SQLException {

        final IPlatform platform = DataSourceProvider.getPlatform();
        final Map<QueryColumn, Object> orderedQueryParameters = new TreeMap<>(pQueryParameters);
        for (final QueryColumn queryColumn : orderedQueryParameters.keySet()) {
            if (!queryColumn.getOperator().isUnary()) {
                Object value = orderedQueryParameters.get(queryColumn);
                if (value instanceof java.util.Date) {
                    value = new Timestamp(((java.util.Date) value).getTime());
                }
                if (value instanceof List) {
                    final List<Object> inClauseValues = ((List<Object>) orderedQueryParameters.get(queryColumn));
                    for (final Object inClauseValue : inClauseValues) {
                        platform.setObject(pStmt, inClauseValue, pParameterIndex++, -1);
                    }
                } else {
                    if (value != null) {
                        platform.setObject(pStmt, value, pParameterIndex++, -1);
                    }
                }
            }
        }
    }
}
