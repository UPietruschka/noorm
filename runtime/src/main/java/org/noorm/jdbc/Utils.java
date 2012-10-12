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
     * typically stored in the Oracle data dictionary in upper case. Most database modellers follow the convention
     * to separate parts of the name by underscores, e.g. "FIRST_NAME", "ORDER_NUMBER", etc. In Java, a widely
     * used convention is to separate parts of the name by capitalizing the first name of the new part, e.g.
     * "firstName", "orderNumber".
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

	public static String convertTableName2ShortName(final String pTableName,
												    final List<String> pIgnoreTableNamePrefixes) {

		return convertTableName2JavaName(pTableName, pIgnoreTableNamePrefixes);
	}

	public static String convertTableName2BeanName(final String pTableName,
												   final List<String> pIgnoreTableNamePrefixes) {

		return convertTableName2JavaName(pTableName, pIgnoreTableNamePrefixes).concat(BEAN_NAME_APPENDIX);
	}

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
		return Utils.convertDBName2JavaName(nameBaseTableName, true);
	}

	public static String convertOracleType2JavaType(final String pOracleType,
													final Long pDataPrecision,
													final Long pDataScale) {

		String javaType = "String";
		if (pOracleType.equals("RAW")) {
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

	public static String getPropertyString(final String pRegex, final Properties pProperties) {

		String propertyString = "";
        if (pProperties != null) {
            for (String searchPattern : pProperties.stringPropertyNames()) {
                final Pattern finder = Pattern.compile(searchPattern);
                final Matcher matcher = finder.matcher(pRegex);
                if (matcher.matches()) {
                    final String replacePattern = pProperties.getProperty(searchPattern);
                    propertyString = matcher.replaceAll(replacePattern);
                    break;
                }
            }
        }
		return propertyString;
	}

	public static String getNormalizedDisplayColumnValue(final String pDisplayColumnValue) {

		return pDisplayColumnValue.trim().toUpperCase().replaceAll
				(ENUM_UNSUPPORTED_REGEX, ENUM_UNSUPPORTED_REGEX_SUBSTITUTE);
	}
}
