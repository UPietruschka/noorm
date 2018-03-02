package org.noorm.generator;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.noorm.generator.schema.*;
import org.noorm.jdbc.Utils;
import org.noorm.jdbc.platform.JDBCType;

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

    public static String convertColumnName2JavaName(final String pColumnName,
                                                    final boolean pCapitalizeFirst,
                                                    final NameMappingList pNameMappingList) {

        String nameBaseColumnName = pColumnName;
        if (pNameMappingList != null) {
            if (pNameMappingList.isPreApplyCamelCaseConversion()) {
                nameBaseColumnName = Utils.convertDBName2JavaName(pColumnName, pCapitalizeFirst);
            }
            final String mappedString = getMappedString(nameBaseColumnName, pNameMappingList.getMapping());
            if (mappedString != null && !mappedString.isEmpty()) {
                char firstChar;
                if (pCapitalizeFirst) {
                    firstChar = mappedString.toUpperCase().charAt(0);
                } else {
                    firstChar = mappedString.toLowerCase().charAt(0);
                }
                nameBaseColumnName = firstChar + mappedString.substring(1, mappedString.length());
            } else {
                nameBaseColumnName = Utils.convertDBName2JavaName(pColumnName, pCapitalizeFirst);
            }
        } else {
            nameBaseColumnName = Utils.convertDBName2JavaName(pColumnName, pCapitalizeFirst);
        }
        return nameBaseColumnName;
    }

    /**
     * Converts a table name to a Java name. The optional list of ignored table name prefixes is used
     * to remove the table name prefix from the generated name. This is useful, when many or all tables in a
     * database schema share a common name prefix, which should be visible in the generated code (e.g. "TBL_").
     *
     * @param pTableName the database table name
     * @param pNameMappingList optional list of table name mappings to define Java names
     * @return the Java name
     */
    public static String convertTableName2JavaName(final String pTableName,
                                                   final NameMappingList pNameMappingList) {

        String nameBaseTableName = pTableName;
        if (pNameMappingList != null) {
            if (pNameMappingList.isPreApplyCamelCaseConversion()) {
                nameBaseTableName = Utils.convertDBName2JavaName(pTableName, true);
            }
            final String mappedString = getMappedString(nameBaseTableName, pNameMappingList.getMapping());
            if (mappedString != null && !mappedString.isEmpty()) {
                nameBaseTableName = mappedString;
            } else {
                nameBaseTableName = Utils.convertDBName2JavaName(pTableName, true);
            }
        } else {
            nameBaseTableName = Utils.convertDBName2JavaName(pTableName, true);
        }
        return nameBaseTableName;
    }

    /**
     * Maps the given database type to the corresponding Java type, which represents the database type in
     * the generated Java service class (e.g. "VARCHAR2" / "String").
     *
     * @param pJDBCType the database type
     * @param pParamName the parameter name (of a stored procedure)
     * @param pTypeMappings the custom type mapping
     * @return the Java type name
     */
    public static String convertDatabaseType2JavaType(final JDBCType pJDBCType,
                                                      final String pParamName,
                                                      final List<TypeMapping> pTypeMappings) {

        if (pTypeMappings != null) {
            for (final TypeMapping typeMapping : pTypeMappings) {
                final JDBCType mapType = JDBCType.valueOf(typeMapping.getDatabaseType().value());
                if (pJDBCType.equals(mapType)) {
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

        return convertDatabaseType2JavaType(pJDBCType, 0);
    }

    /**
     * Checks, whether the given column for the given table is configured to never get updated.
     *
     * @param pTableName the table name
     * @param pColumnName the column name
     * @param pNoUpdateColumnMappings the configured mapping for no update columns
     * @return the Java type name
     */
    public static boolean checkForNoUpdateColumns(final String pTableName,
                                                  final String pColumnName,
                                                  final List<NoUpdateColumnMapping> pNoUpdateColumnMappings) {

        if (pNoUpdateColumnMappings != null) {
            for (final NoUpdateColumnMapping noUpdateColumnMapping : pNoUpdateColumnMappings) {
                final String columnFilterRegex = noUpdateColumnMapping.getColumnFilterRegex();
                final String tableFilterRegex = noUpdateColumnMapping.getTableFilterRegex();
                if (checkTableColumnFilterRegexPair(pTableName, pColumnName, tableFilterRegex, columnFilterRegex)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Maps the given database type to the corresponding Java type, which represents the database type in
     * the generated Java Bean class (e.g. "VARCHAR2" / "String").
     *
     * @param pJDBCType the database type
     * @param pDecimalDigits the data scale of the database type, if available
     * @param pTableName the table name
     * @param pColumnName the column name
     * @param pTypeMappings the custom type mapping
     * @return the Java type name
     */
    public static String convertDatabaseType2JavaType(final JDBCType pJDBCType,
                                                      final int pDecimalDigits,
                                                      final String pTableName,
                                                      final String pColumnName,
                                                      final List<TypeMapping> pTypeMappings) {

        if (pTypeMappings != null) {
            for (final TypeMapping typeMapping : pTypeMappings) {
                final JDBCType mapType = JDBCType.valueOf(typeMapping.getDatabaseType().value());
                if (pJDBCType.equals(mapType) &&
                        typeMapping.getParameterFilterRegex() == null) {
                    final String columnFilterRegex = typeMapping.getColumnFilterRegex();
                    final String tableFilterRegex = typeMapping.getTableFilterRegex();
                    if (checkTableColumnFilterRegexPair(pTableName, pColumnName, tableFilterRegex, columnFilterRegex)) {
                        return typeMapping.getJavaType().value();
                    }
                }
            }
        }

        return convertDatabaseType2JavaType(pJDBCType, pDecimalDigits);
    }

    private static boolean checkTableColumnFilterRegexPair(final String pTableName,
                                                           final String pColumnName,
                                                           final String pTableFilterRegex,
                                                           final String pColumnFilterRegex) {

        if (pColumnFilterRegex == null && pTableFilterRegex == null) {
            // Though it is possible to specify attributes 'minOccurs' and 'maxOccurs' to a choice
            // element in XML schema to indicate that "at least" one element should be defined, the
            // generated code for this construction is rather awkward, so we do the check for this here
            // (a 'choice' is no longer used, but simple elements with 'minOccurs="0"')
            throw new GeneratorException("At least one of the elements 'columnFilterRegex'"
                    .concat(" or 'tableFilterRegex' must be specified for each mapping"));
        }
        boolean tableNameMatches = false;
        boolean columnNameMatches = false;
        if (pColumnFilterRegex != null) {
            final Pattern finder1 = Pattern.compile(pColumnFilterRegex.toUpperCase());
            final Matcher matcher1 = finder1.matcher(pColumnName.toUpperCase());
            if (matcher1.matches()) {
                columnNameMatches = true;
            }
        } else {
            // No specified column-name restriction includes all columns.
            columnNameMatches = true;
        }
        if (pTableFilterRegex != null) {
            final Pattern finder2 = Pattern.compile(pTableFilterRegex.toUpperCase());
            final Matcher matcher2 = finder2.matcher(pTableName.toUpperCase());
            if (matcher2.matches()) {
                tableNameMatches = true;
            }
        } else {
            // No specified table-name restriction includes all tables.
            tableNameMatches = true;
        }
        if (tableNameMatches && columnNameMatches) {
            return true;
        }
        return false;
    }

    private static String convertDatabaseType2JavaType(final JDBCType pJDBCType,
                                                       final int pDecimalDigits) {

        // TODO: Handle non-Ora types more thoroughly
        String javaType = "String";
        if (pJDBCType.equals(JDBCType.BINARY)) {
            javaType = "byte[]";
        }
        if (pJDBCType.equals(JDBCType.VARBINARY)) {
            javaType = "byte[]";
        }
        if (pJDBCType.equals(JDBCType.BLOB)) {
            javaType = "java.sql.Blob";
        }
        if (pJDBCType.equals(JDBCType.CLOB)) {
            javaType = "java.sql.Clob";
        }
        if (pJDBCType.equals(JDBCType.NCLOB)) {
            javaType = "java.sql.NClob";
        }
        if (pJDBCType.equals(JDBCType.SQLXML)) {
            javaType = "java.sql.SQLXML";
        }
        if (pJDBCType.equals(JDBCType.NUMERIC)) {
            if (pDecimalDigits > 0) {
                javaType = "Double";
            } else {
                javaType = "Long";
            }
        }
        if (pJDBCType.equals(JDBCType.FLOAT)) {
            javaType = "Float";
        }
        if (pJDBCType.equals(JDBCType.DOUBLE)) {
            javaType = "Double";
        }
        if (pJDBCType.equals(JDBCType.INTEGER)) {
            javaType = "Integer";
        }
        if (pJDBCType.equals(JDBCType.DATE)) {
            javaType = "java.util.Date";
        }
        if (pJDBCType.equals(JDBCType.TIMESTAMP)) {
            javaType = "java.util.Date";
        }
        return javaType;
    }

    /**
     * Replaces strings or parts of strings with a replacement expression.
     * This method iterates through the given mappings, which should contain pairs of search patterns and
     * replacement patterns for string replacement in the input string. For the first search pattern, which
     * matches the input string, the replacement pattern is applied. The simplest form of string replacement
     * is a direct replacement of the search pattern with the replacement pattern without any usage of
     * regular expressions (e.g. "CUSTOMER" / "CUSTOMER_SEQ"). Regular expressions allow for a compact
     * representation of more generic replacements (e.g. search/replace expressions "(.*)" / "$1_SEQ" also
     * results in the replacement string "CUSTOMER_SEQ" for input string "CUSTOMER").
     *
     * @param pInput the input string subject to regular expression replacement
     * @param pMappings a collection of key/value pairs for search/replace expressions
     * @return the modified input string
     */
    public static String getMappedString(final String pInput, final List<Mapping> pMappings) {

        String mappedString = "";
        if (pMappings != null) {
            for (final Mapping mapping : pMappings) {
                final String searchPattern = mapping.getKey();
                final Pattern finder = Pattern.compile(searchPattern);
                final Matcher matcher = finder.matcher(pInput);
                if (matcher.matches()) {
                    final String replacePattern = mapping.getValue();
                    mappedString = matcher.replaceFirst(replacePattern);
                    break;
                }
            }
        }
        return mappedString;
    }

    public static boolean hasBeanPackageName(final GeneratorConfiguration pConfiguration) {

        if (pConfiguration.getBeanJavaPackage() == null) {
            return false;
        }
        return !(pConfiguration.getBeanJavaPackage().getName() == null
                || pConfiguration.getBeanJavaPackage().getName().isEmpty());
    }

    public static boolean hasEnumPackageName(final GeneratorConfiguration pConfiguration) {

        if (pConfiguration.getEnumJavaPackage() == null) {
            return false;
        }
        return !(pConfiguration.getEnumJavaPackage().getName() == null
                || pConfiguration.getEnumJavaPackage().getName().isEmpty());
    }

    public static boolean hasServicePackageName(final GeneratorConfiguration pConfiguration) {

        if (pConfiguration.getServiceJavaPackage() == null) {
            return false;
        }
        return !(pConfiguration.getServiceJavaPackage().getName() == null
                || pConfiguration.getServiceJavaPackage().getName().isEmpty());
    }

    public static boolean hasServiceInterfacePackageName(final GeneratorConfiguration pConfiguration) {

        if (pConfiguration.getServiceInterfaceJavaPackage() == null) {
            return false;
        }
        return !(pConfiguration.getServiceInterfaceJavaPackage().getName() == null
                || pConfiguration.getServiceInterfaceJavaPackage().getName().isEmpty());
    }

    public static boolean hasDataSourceName(final GeneratorConfiguration pConfiguration) {

        if (pConfiguration.getDataSource() == null) {
            return false;
        }
        if (pConfiguration.getDataSource().getName() == null || pConfiguration.getDataSource().getName().isEmpty()) {
            return false;
        }
        return true;
    }
}
