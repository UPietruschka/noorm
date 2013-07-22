package org.noorm.generator;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.noorm.generator.schema.CustomTypeMapping;
import org.noorm.generator.schema.Property;
import org.noorm.jdbc.Utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 30.11.11
 *         Time: 12:18
 */
public class GeneratorUtil {

    private static final String BEAN_NAME_APPENDIX = "Bean";

	public static void generateFile(final File pDir,
									final String pVelocityTemplateFile,
									final String pJavaName,
									final Object pClassDescriptor) throws GeneratorException {

		final File javaSourceFile = new File(pDir, pJavaName + Utils.JAVA_SOURCE_FILE_APPENDIX);
		try {
			final VelocityContext context = new VelocityContext();
			context.put("class", pClassDescriptor);
			context.put("nl", "\n");
			context.put("subindent", "\t\t\t\t\t");
			// The following macro is used as a workaround for an un-intentional Velocity behaviour.
			// Usually, Velocity just takes the newlines of the template as they occur in the template.
			// However, when a line ends with a velocity command like "#if(...)" or "#end", Velocity
			// omits the newline. When a newline is desired here, we need to append something to the
			// lines end to force a newline. Since this addendum should not be visible in the generated
			// code, we define a macro here, which is visible in the template, but not in the generated
			// code (just an empty macro).
			context.put("force_newline", "");
			final Template template = Velocity.getTemplate(pVelocityTemplateFile);
			final BufferedWriter writer = new BufferedWriter(new FileWriter(javaSourceFile));
			template.merge(context, writer);
			writer.flush();
			writer.close();
		} catch (IOException e) {
			throw new GeneratorException("Writing Java source file failed.", e);
		}
	}

	public static File createPackageDir(final File pDestinationDirectory,
										final String pPackageName) throws GeneratorException {

		final File packageDir =	new File(pDestinationDirectory, pPackageName.replace(".", File.separator));
		if (!packageDir.exists()) {
			if (!packageDir.mkdirs()) {
				throw new GeneratorException("Could not create directory ".concat(packageDir.toString()));
			}
		}
		return packageDir;
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
        return Utils.convertDBName2JavaName(nameBaseColumnName, pCapitalizeFirst);
    }

    /**
     * Converts an Oracle table name to a Java Bean name.
     * @see Utils (convertTableName2JavaName(String, java.util.List))
     * @param pTableName the database table name
     * @param pIgnoreTableNamePrefixes optional list of table name prefixes to be ignored at conversion
     * @return the Java Bean name
     */
    public static String convertTableName2BeanName(final String pTableName,
                                                   final List<String> pIgnoreTableNamePrefixes) {

        return Utils.convertTableName2JavaName(pTableName, pIgnoreTableNamePrefixes).concat(BEAN_NAME_APPENDIX);
    }

    /**
     * Maps the given Oracle type to the corresponding Java type, which represents the Oracle type in
     * the generated Java service class (e.g. "VARCHAR2" -> "String").
     *
     * @param pOracleType the Oracle type name
     * @param pParamName the parameter name (of a stored procedure)
     * @param pCustomTypeMappings the custom type mapping
     * @return the Java type name
     */
    public static String convertOracleType2JavaType(final String pOracleType,
                                                    final String pParamName,
                                                    final List<CustomTypeMapping> pCustomTypeMappings) {

        if (pCustomTypeMappings != null) {
            for (final CustomTypeMapping typeMapping : pCustomTypeMappings) {
                if (pOracleType.startsWith(typeMapping.getDatabaseType())) {
                    if (typeMapping.getParameterFilterRegex() != null) {
                        if (typeMapping.getColumnFilterRegex() != null || typeMapping.getTableFilterRegex() != null) {
                            throw new GeneratorException("Element 'parameterFilterRegex' must not be defined"
                                    .concat(" together with elements 'tableFilterRegex' or 'columnFilterRegex'"));
                        }
                        final Pattern finder0 = Pattern.compile(typeMapping.getParameterFilterRegex().toUpperCase());
                        final Matcher matcher0 = finder0.matcher(pParamName.toUpperCase());
                        if (matcher0.matches()) {
                            return typeMapping.getJavaType().value();
                        }
                    }
                }
            }
        }

        return convertOracleType2JavaType(pOracleType, 0L, 0L);
    }

    /**
     * Maps the given Oracle type to the corresponding Java type, which represents the Oracle type in
     * the generated Java Bean class (e.g. "VARCHAR2" -> "String").
     *
     * @param pOracleType the Oracle type name
     * @param pDataPrecision the data precision of the Oracle type, if available
     * @param pDataScale the data scale of the Oracle type, if available
     * @param pTableName the table name
     * @param pColumnName the column name
     * @param pCustomTypeMappings the custom type mapping
     * @return the Java type name
     */

    public static String convertOracleType2JavaType(final String pOracleType,
                                                    final Long pDataPrecision,
                                                    final Long pDataScale,
                                                    final String pTableName,
                                                    final String pColumnName,
                                                    final List<CustomTypeMapping> pCustomTypeMappings) {

        if (pCustomTypeMappings != null) {
            for (final CustomTypeMapping typeMapping : pCustomTypeMappings) {
                if (pOracleType.startsWith(typeMapping.getDatabaseType()) &&
                        typeMapping.getParameterFilterRegex() == null) {
                    if (typeMapping.getColumnFilterRegex() == null && typeMapping.getTableFilterRegex() == null) {
                        // Though it is possible to specify attributes 'minOccurs' and 'maxOccurs' to a choice
                        // element in XML schema to indicate that "al least" one element should be defined, the
                        // generated code for this construction is rather awkward, so we do the check for this here
                        // (a 'choice' is no longer used, but simple elements with 'minOccurs="0"')
                        throw new GeneratorException("At least one of the elements 'columnFilterRegex'"
                                .concat(" or 'tableFilterRegex' must be specified for each custom type mapping"));
                    }
                    boolean tableNameMatches = false;
                    boolean columnNameMatches = false;
                    if (typeMapping.getColumnFilterRegex() != null) {
                        final Pattern finder1 = Pattern.compile(typeMapping.getColumnFilterRegex().toUpperCase());
                        final Matcher matcher1 = finder1.matcher(pColumnName.toUpperCase());
                        if (matcher1.matches()) {
                            columnNameMatches = true;
                        }
                    } else {
                        // No specified column-name restriction includes all columns.
                        columnNameMatches = true;
                    }
                    if (typeMapping.getTableFilterRegex() != null) {
                        final Pattern finder2 = Pattern.compile(typeMapping.getTableFilterRegex().toUpperCase());
                        final Matcher matcher2 = finder2.matcher(pTableName.toUpperCase());
                        if (matcher2.matches()) {
                            tableNameMatches = true;
                        }
                    } else {
                        // No specified table-name restriction includes all tables.
                        tableNameMatches = true;
                    }
                    if (tableNameMatches && columnNameMatches) {
                        return typeMapping.getJavaType().value();
                    }
                }
            }
        }

        return convertOracleType2JavaType(pOracleType, pDataPrecision, pDataScale);
    }

    private static String convertOracleType2JavaType(final String pOracleType,
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
            if (pDataPrecision != null && pDataPrecision > 0L && pDataScale != null && pDataScale > 0L) {
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
        if (pOracleType.startsWith("TIMESTAMP")) {
            javaType = "java.util.Date";
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
    public static String getPropertyString(final String pInput, final List<Property> pProperties) {

        String propertyString = "";
        if (pProperties != null) {
            for (final Property property : pProperties) {
                final String searchPattern = property.getName();
                final Pattern finder = Pattern.compile(searchPattern);
                final Matcher matcher = finder.matcher(pInput);
                if (matcher.matches()) {
                    final String replacePattern = property.getValue();
                    propertyString = matcher.replaceFirst(replacePattern);
                    break;
                }
            }
        }
        return propertyString;
    }
}
