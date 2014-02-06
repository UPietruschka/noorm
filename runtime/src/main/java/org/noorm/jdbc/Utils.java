package org.noorm.jdbc;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;

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
     * Converts a Java name into a database name. Inversion of convertDBName2JavaName.
     * @see #convertDBName2JavaName(String, boolean)
     * @param pJavaName the Java name
     * @return the database name
     */
	public static String convertJavaName2DBName(final String pJavaName) {

		char[] javaNameChars = pJavaName.toCharArray();
		char[] lowerJavaNameChars = pJavaName.toLowerCase().toCharArray();
		StringBuilder dbNameBuilder = new StringBuilder();
		for (int i = 0; i < javaNameChars.length; i++) {
			if ((javaNameChars[i] != lowerJavaNameChars[i]) && i > 0) {
				dbNameBuilder.append('_');
			}
			dbNameBuilder.append(lowerJavaNameChars[i]);
		}
		return dbNameBuilder.toString();
	}

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
                if (pParameter != null) {
                    parameterToString = pParameter.toString();
                } else {
                    parameterToString = "NULL";
                }
            }
        }
        return parameterToString;
    }
}
