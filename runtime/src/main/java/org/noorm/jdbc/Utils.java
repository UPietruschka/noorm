package org.noorm.jdbc;

import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility methods to support JDBCStatementProcessor and the class generators.
 *
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 06.05.11
 *         Time: 14:23
 */
public class Utils {

	public static final String JAVA_SOURCE_FILE_APPENDIX = ".java";

	private static final String DB_NAME_TOKEN_SPLIT = "_";
	private static final String BEAN_NAME_APPENDIX = "Bean";
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

    public static String convertDBName2JavaName(final String pDBName,
                                                final boolean pCapitalizeFirst,
                                                final List<String> pIgnoreColumnNamePrefixes) {
        String nameBaseColumnName = pDBName;
        if (pIgnoreColumnNamePrefixes != null) {
            for (final String ignoredPrefix : pIgnoreColumnNamePrefixes) {
                if (pDBName.startsWith(ignoredPrefix)) {
                    nameBaseColumnName = pDBName.substring(ignoredPrefix.length());
                }
            }
        }
        return convertDBName2JavaName(nameBaseColumnName, pCapitalizeFirst);
    }

    /**
     * Converts a database object name into a java name. Database names (table names, column names, etc.) are
     * typically stored in the Oracle data dictionary in upper case. Most database modellers follow the convention
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
     * Converts an Oracle table name to a short table name.
     * @see #convertTableName2JavaName(String, java.util.List)
     * @param pTableName the database table name
     * @param pIgnoreTableNamePrefixes optional list of table name prefixes to be ignored at conversion
     * @return the short table name
     */
	public static String convertTableName2ShortName(final String pTableName,
												    final List<String> pIgnoreTableNamePrefixes) {

		return convertTableName2JavaName(pTableName, pIgnoreTableNamePrefixes);
	}

    /**
     * Converts an Oracle table name to a Java Bean name.
     * @see #convertTableName2JavaName(String, java.util.List)
     * @param pTableName the database table name
     * @param pIgnoreTableNamePrefixes optional list of table name prefixes to be ignored at conversion
     * @return the Java Bean name
     */
    public static String convertTableName2BeanName(final String pTableName,
												   final List<String> pIgnoreTableNamePrefixes) {

		return convertTableName2JavaName(pTableName, pIgnoreTableNamePrefixes).concat(BEAN_NAME_APPENDIX);
	}

    /**
     * Converts an Oracle table name to a Java name. The optional list of ignored table name prefixes is used
     * to remove the table name prefix from the generated name. This is useful, when many or all tables in a
     * database schema share a common name prefix, which should be visible in the generated code (e.g. "TBL_").
     *
     * @see #convertDBName2JavaName(String, boolean)
     * @param pTableName the database table name
     * @param pIgnoreTableNamePrefixes optional list of table name prefixes to be ignored at conversion
     * @return the Java name
     */
	public static String convertTableName2JavaName(final String pTableName,
												   final List<String> pIgnoreTableNamePrefixes) {

		String nameBaseTableName = pTableName;
		if (pIgnoreTableNamePrefixes != null) {
			for (final String ignoredPrefix : pIgnoreTableNamePrefixes) {
				if (pTableName.startsWith(ignoredPrefix)) {
					nameBaseTableName = pTableName.substring(ignoredPrefix.length());
				}
			}
		}
		return convertDBName2JavaName(nameBaseTableName, true);
	}

    /**
     * Maps the given Oracle type to the corresponding Java type, which represents the Oracle type in
     * the generated Java Bean class (e.g. "VARCHAR2" -> "String").
     *
     * @param pOracleType the Oracle type name
     * @param pDataPrecision the data precision of the Oracle type, if available
     * @param pDataScale the data scale of the Oracle type, if available
     * @return the Java type name
     */
	public static String convertOracleType2JavaType(final String pOracleType,
													final Long pDataPrecision,
													final Long pDataScale) {

		String javaType = "String";
		if (pOracleType.equals("RAW")) {
			javaType = "byte[]";
		}
        if (pOracleType.equals("BLOB")) {
            javaType = "byte[]";
        }
        if (pOracleType.equals("NUMBER")) {
			if (pDataPrecision != null && pDataPrecision > 0L && pDataScale != 0 && pDataScale > 0L) {
				javaType = "Double";
			} else {
				javaType = "Long";
			}
		}
		if (pOracleType.equals("BINARY_FLOAT")) {
			javaType = "Float";
		}
		if (pOracleType.equals("BINARY_DOUBLE")) {
			javaType = "Double";
		}
		if (pOracleType.equals("FLOAT")) {
			javaType = "Double";
		}
		if (pOracleType.equals("DATE")) {
			javaType = "java.util.Date";
		}
		if (pOracleType.length() >= 9) {
			if (pOracleType.substring(0, 9).equals("TIMESTAMP")) {
				javaType = "java.util.Date";
			}
		}
		return javaType;
	}

    /**
     * Replaces strings or parts of strings with a replacement expression.
     * This method iterates through the given properties, which should contain pairs of search pattern and
     * replacement patterns for string replacement in the input string. For the first search pattern, which
     * matches the input string, the replacement pattern is applied. The simplest form of string replacement
     * is a direct replacement of the search pattern with the replacement pattern without any usage of
     * regular expressions (e.g. "CUSTOMER" -> "CUSTOMER_SEQ"). Regular expressions allow for a compact
     * representation of more generic replacements (e.g. search/replace expressions "(.*)" / "$1_SEQ" also
     * results in the replacement string "CUSTOMER_SEQ" for input string "CUSTOMER").
     *
     * @param pInput the input string subject to regular expression replacement
     * @param pProperties a collection of key/value pairs for search/replace expressions
     * @return the modified input string
     */
	public static String getPropertyString(final String pInput, final Properties pProperties) {

		String propertyString = "";
        if (pProperties != null) {
            for (String searchPattern : pProperties.stringPropertyNames()) {
                final Pattern finder = Pattern.compile(searchPattern);
                final Matcher matcher = finder.matcher(pInput);
                if (matcher.matches()) {
                    final String replacePattern = pProperties.getProperty(searchPattern);
                    propertyString = matcher.replaceFirst(replacePattern);
                    break;
                }
            }
        }
		return propertyString;
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
}
