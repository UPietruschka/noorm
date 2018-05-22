package org.noorm.generator.beangenerator;

import org.noorm.generator.GeneratorException;
import org.noorm.generator.GeneratorUtil;
import org.noorm.generator.ValidatorClassDescriptor;
import org.noorm.generator.IParameters;
import org.noorm.generator.schema.GeneratorConfiguration;
import org.noorm.generator.schema.Regex;
import org.noorm.jdbc.DataSourceProvider;
import org.noorm.jdbc.platform.IMetadata;
import org.noorm.jdbc.platform.PrimaryKeyColumn;
import org.noorm.jdbc.platform.Sequence;
import org.noorm.jdbc.platform.TableMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.JDBCType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Generator for database related Bean source files based on Velocity templates.
 *
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 19.04.11
 *         Time: 14:48
 */
public class BeanGenerator {

	private static final Logger log = LoggerFactory.getLogger(BeanGenerator.class);
	private static final String BEAN_VM_TEMPLATE_FILE = "/bean.vm";
	private static final String BEAN_VALIDATOR_VM_TEMPLATE_FILE = "/bean_validator.vm";
	private static final String BEAN_VALIDATOR_CLASS_NAME = "GenericBeanValidator";
	private static final String BEAN_DML_VM_TEMPLATE_FILE = "/bean_dml.vm";

	private IParameters parameters;
    private GeneratorConfiguration configuration;

	public BeanGenerator(final IParameters pParameters, final GeneratorConfiguration pConfiguration) {
		parameters = pParameters;
        configuration = pConfiguration;
	}

	public void execute() throws GeneratorException {

        if (!GeneratorUtil.hasBeanPackageName(configuration)) {
			throw new IllegalArgumentException("Parameter [beanPackageName] is null.");
		}
        if (!GeneratorUtil.hasServicePackageName(configuration)) {
            throw new IllegalArgumentException("Parameter [servicePackageName] is null.");
        }
		if (parameters.getDestinationDirectory() == null || !parameters.getDestinationDirectory().exists()) {
			throw new IllegalArgumentException("Parameter [destinationDirectory] is null or mis-configured.");
		}

		final ValidatorClassDescriptor validatorClassDescriptor = new ValidatorClassDescriptor();
		validatorClassDescriptor.setPackageName(configuration.getBeanJavaPackage().getName());

        final IMetadata metadata = DataSourceProvider.getPlatform().getMetadata();

		log.info("Retrieving table metadata from database.");
        String beanTableFilterRegex = null;
        if (configuration.getBeanTableFilter() != null) {
            beanTableFilterRegex = configuration.getBeanTableFilter().getRegex();
            validatorClassDescriptor.setTableNamePattern(beanTableFilterRegex);
        } else {
            validatorClassDescriptor.setTableNamePattern(".*");
        }
        final String schemaFilterRegex = configuration.getSchemaFilter().getRegex();
		validatorClassDescriptor.setSchemaPattern(schemaFilterRegex);
		final Map<String, List<TableMetadata>> tableColumnMap =
				metadata.findTableMetadata(schemaFilterRegex, beanTableFilterRegex);
		log.info("Retrieving record metadata from database.");
		final Map<String, List<TableMetadata>> recordColumnMap = metadata.findRecordMetadata();
		tableColumnMap.putAll(recordColumnMap);

		log.info("Retrieving sequence metadata from database.");
		final List<Sequence> sequenceList = metadata.findSequences();

		log.info("Generating NoORM Bean classes.");
		final File beanPackageDir = GeneratorUtil.createPackageDir
				(parameters.getDestinationDirectory(), configuration.getBeanJavaPackage().getName());
		final File servicePackageDir = GeneratorUtil.createPackageDir
				(parameters.getDestinationDirectory(), configuration.getServiceJavaPackage().getName());
		File serviceInterfacePackageDir = null;
        if (GeneratorUtil.hasServiceInterfacePackageName(configuration)) {
			serviceInterfacePackageDir = GeneratorUtil.createPackageDir
					(parameters.getDestinationDirectory(), configuration.getServiceInterfaceJavaPackage().getName());
		}

		for (final String tableName0 : tableColumnMap.keySet()) {
			final String javaBeanName =
                    GeneratorUtil.convertTableName2JavaName(tableName0, configuration.getTableNameMappings());
			final List<TableMetadata> tableMetadataList1 = tableColumnMap.get(tableName0);
			final BeanClassDescriptor beanClassDescriptor = new BeanClassDescriptor();
			beanClassDescriptor.setName(javaBeanName);
			if (configuration.getExtendedBeanMappings() != null) {
				final String extJavaBeanName =
                        GeneratorUtil.getMappedString(javaBeanName, configuration.getExtendedBeanMappings());
				if (!extJavaBeanName.isEmpty()) {
					beanClassDescriptor.setExtendedName(extJavaBeanName);
				}
			}
			// Do not add PL/SQL record beans to the validator (record beans are declared in the PL/SQL code
			// and get automatically validated by the service validator)
			if (!recordColumnMap.containsKey(tableName0)) {
				validatorClassDescriptor.getClassNames().add(javaBeanName);
			}
			beanClassDescriptor.setTableName(tableName0);
            if (!tableName0.equals(tableName0.toUpperCase())) {
                beanClassDescriptor.setTableNameCaseSensitive(true);
            }

            log.info("Retrieving primary key metadata from database.");
            final List<PrimaryKeyColumn> pkColumnNameList = metadata.findPkColumns(tableName0);

            final String[] primaryKeyColumnNames = getPrimaryKeyColumnNames(tableName0, pkColumnNameList);
			beanClassDescriptor.setPrimaryKeyColumnNames(primaryKeyColumnNames);
            final String[] primaryKeyJavaNames = new String[primaryKeyColumnNames.length];
            for (int i = 0; i < primaryKeyColumnNames.length; i++) {
                primaryKeyJavaNames[i] = GeneratorUtil.convertColumnName2JavaName(primaryKeyColumnNames[i],
                        false, configuration.getColumnNameMappings());
            }
            beanClassDescriptor.setPrimaryKeyJavaNames(primaryKeyJavaNames);
            beanClassDescriptor.setGeneratePKBasedEqualsAndHashCode(configuration.isGeneratePKBasedEqualsAndHashCode());

			final String interfaceName =
					GeneratorUtil.getMappedString(tableName0, configuration.getTable2InterfaceMappings());
			if (interfaceName != null && !interfaceName.isEmpty()) {
				beanClassDescriptor.setCustomInterfaceName(interfaceName);
			}

			final String superClassName =
					GeneratorUtil.getMappedString(tableName0, configuration.getTable2SuperClassMappings());
			if (superClassName != null && !superClassName.isEmpty()) {
				beanClassDescriptor.setSuperClassName(superClassName);
			}

			final Sequence sequence = getSequence(tableName0, sequenceList);
            if (sequence != null) {
                final String sequenceName = sequence.getName();
                if (primaryKeyColumnNames.length != 1) {
                    throw new GeneratorException(("Using sequences to generate IDs is only supported for tables "
                            .concat(" with one and only one primary key column [")
                            .concat(tableName0).concat("]")));
                }
                beanClassDescriptor.setSequenceName(sequenceName);
                beanClassDescriptor.setSequenceIncrement(sequence.getIncrementBy());
            } else {
                beanClassDescriptor.setSequenceName("");
                beanClassDescriptor.setSequenceIncrement(0);
            }
            final Regex inlineSequenceTableFilter = configuration.getInlineSequenceTableFilter();
            if (inlineSequenceTableFilter != null && inlineSequenceTableFilter.getRegex() != null
                    && tableName0.matches(inlineSequenceTableFilter.getRegex())) {
                beanClassDescriptor.setUseInlineSequenceValueGeneration(true);
            }
			final String versionColumnName = getVersionColumnName(tableName0, tableMetadataList1);
			beanClassDescriptor.setVersionColumnName(versionColumnName);
			beanClassDescriptor.setPackageName(configuration.getBeanJavaPackage().getName());
			// Use a unique seed for serialVersionUID generation to guarantee the generation of a reproducible
			// serialVersionUID with every new source code generation cycle.
			final Random random = new Random(javaBeanName.hashCode());
			final long serialVersionUID = random.nextLong();
			beanClassDescriptor.setSerialVersionUID(serialVersionUID);
            boolean unsupportedOptLockFullRowCompareTypes = false;
            String versionColumnType = "";
			for (final TableMetadata tableMetadata : tableMetadataList1) {
				final BeanAttributeDescriptor beanAttributeDescriptor = new BeanAttributeDescriptor();
                final String columnName = tableMetadata.getColumnName();
                final String javaName = GeneratorUtil.convertColumnName2JavaName
                        (columnName, false, configuration.getColumnNameMappings());
				beanAttributeDescriptor.setName(javaName);
                final String methodNamePostfix = GeneratorUtil.convertColumnName2JavaName
                        (columnName, true, configuration.getColumnNameMappings());
                beanAttributeDescriptor.setMethodNamePostfix(methodNamePostfix);

                final boolean noUpdateConfigured = GeneratorUtil.checkForNoUpdateColumns
						(tableMetadata.getTableName(), columnName, configuration.getNoUpdateColumnMappings());
				if (noUpdateConfigured) {
					beanAttributeDescriptor.setUpdatable(false);
				}

                final JDBCType jdbcType = tableMetadata.getJDBCType();
				final String javaType = GeneratorUtil.convertDatabaseType2JavaType(jdbcType,
                        tableMetadata.getDecimalDigits(), tableMetadata.getTableName(),
                        columnName, configuration.getTypeMappings());

				beanAttributeDescriptor.setType(javaType);

                String jdbcTypeDeclaration = "Types.".concat(jdbcType.getName());
                beanAttributeDescriptor.setDataType(jdbcTypeDeclaration);

                if (jdbcType.equals(JDBCType.CLOB) || jdbcType.equals(JDBCType.BLOB)
                    || jdbcType.equals(JDBCType.NCLOB) || jdbcType.equals(JDBCType.SQLXML)) {
                    unsupportedOptLockFullRowCompareTypes = true;
                }
                if (!columnName.equals(columnName.toUpperCase())) {
                    beanAttributeDescriptor.setCaseSensitiveName(true);
                }
				beanAttributeDescriptor.setColumnName(columnName);
                if (versionColumnName.equals(columnName)) {
                    versionColumnType = jdbcType.getName();
                    beanClassDescriptor.setVersionColumnJavaName(javaName);
                }
				beanAttributeDescriptor.setMaxLength(tableMetadata.getColumnSize());
				if (!tableMetadata.getNullable()) {
					beanAttributeDescriptor.setNullable(false);
				}
				beanClassDescriptor.addAttribute(beanAttributeDescriptor);
			}
            if (!versionColumnType.isEmpty()) {
                if (versionColumnType.length() > 9) { versionColumnType = versionColumnType.substring(0, 9); }
                beanClassDescriptor.setVersionColumnType(versionColumnType);
            }
            final Regex optLockFullRowCompareTableFilter = configuration.getOptLockFullRowCompareTableFilter();
            if (optLockFullRowCompareTableFilter != null && optLockFullRowCompareTableFilter.getRegex() != null
                    && tableName0.matches(optLockFullRowCompareTableFilter.getRegex())) {
                if (unsupportedOptLockFullRowCompareTypes) {
                    throw new GeneratorException("Optimistic locking using pre-change image comparison is not "
                            .concat("supported for tables with complex data-types (CLOB, BLOB, XMLTYPE) [")
                            .concat(tableName0).concat("]"));
                }
                beanClassDescriptor.setEnableOptLockFullRowCompare(true);
            }
			GeneratorUtil.generateFile(beanPackageDir, BEAN_VM_TEMPLATE_FILE,
					beanClassDescriptor.getName(), beanClassDescriptor);

			final BeanDMLClassDescriptor beanDMLClassDescriptor = new BeanDMLClassDescriptor();
			beanDMLClassDescriptor.setBeanPackageName(configuration.getBeanJavaPackage().getName());
			beanDMLClassDescriptor.setJavaName(beanClassDescriptor.getName() + "DML");
            beanDMLClassDescriptor.addBean(beanClassDescriptor);
			if (GeneratorUtil.hasServiceInterfacePackageName(configuration)) {
				beanDMLClassDescriptor.setInterfacePackageName
                        (configuration.getServiceInterfaceJavaPackage().getName());
			}
			beanDMLClassDescriptor.setPackageName(configuration.getServiceJavaPackage().getName());

			if (GeneratorUtil.hasDataSourceName(configuration)) {
				validatorClassDescriptor.setDataSourceName(configuration.getDataSource().getName());
				beanDMLClassDescriptor.setDataSourceName(configuration.getDataSource().getName());
			}

			GeneratorUtil.generateFile(servicePackageDir, BEAN_DML_VM_TEMPLATE_FILE,
					beanDMLClassDescriptor.getJavaName(), beanDMLClassDescriptor);
			if (GeneratorUtil.hasServiceInterfacePackageName(configuration)) {
				beanDMLClassDescriptor.setInterface(true);
				GeneratorUtil.generateFile(serviceInterfacePackageDir, BEAN_DML_VM_TEMPLATE_FILE,
						"I" + beanDMLClassDescriptor.getJavaName(), beanDMLClassDescriptor);
			}
		}
		GeneratorUtil.generateFile(beanPackageDir, BEAN_VALIDATOR_VM_TEMPLATE_FILE,
				BEAN_VALIDATOR_CLASS_NAME, validatorClassDescriptor);
	}

	private void generateBeanDML() {

    }

	private Sequence getSequence(final String pTableName,
                                 final List<Sequence> pSequenceList) {

		final String sequenceName =
                GeneratorUtil.getMappedString(pTableName, configuration.getTable2SequenceMappings());
		if (sequenceName.isEmpty()) {
			log.info("No matching sequence-name has been found for table-name ".concat(pTableName));
			return null;
		}
		// Check the matched sequence name against the sequence names retrieved from the database
		for (final Sequence sequence : pSequenceList) {
			if (sequenceName.equals(sequence.getName())) {
				return sequence;
			}
		}
		throw new GeneratorException("Matching sequence-name ".concat(sequenceName)
                .concat(" has been found for table-name ")
				.concat(pTableName).concat(", but no database sequence with this name could be found."));
	}

	private String[] getPrimaryKeyColumnNames(final String pTableName,
										      final List<PrimaryKeyColumn> pPrimaryKeyColumnList) {

		final List<String> pkColumnNames = new ArrayList<>();
		for (final PrimaryKeyColumn primaryKeyBean : pPrimaryKeyColumnList) {
            log.info("Primary key column ".concat(primaryKeyBean.getColumnName())
                    .concat(" found for table ").concat(pTableName));
            pkColumnNames.add(primaryKeyBean.getColumnName());
		}
		if (pkColumnNames.isEmpty()) {
			if (pkColumnNames.isEmpty()) {
				log.info("No primary key found."
						.concat("Automatic support for DML will not be available for table ")
						.concat(pTableName));
			}
		}
		return pkColumnNames.toArray(new String[pkColumnNames.size()]);
	}

	private String getVersionColumnName(final String pTableName,
										final List<TableMetadata> pTableMetadataList) {

		String versionColumnName = "";
		if (configuration.getOptLockVersionColumnMappings() != null) {
			versionColumnName =
                    GeneratorUtil.getMappedString(pTableName, configuration.getOptLockVersionColumnMappings());
		}
		if (versionColumnName.isEmpty()) {
			log.info("No matching version-column-name has been found for table-name ".concat(pTableName));
			return versionColumnName;
		}
		// Check the matched version column name against the column names of this table
		for (final TableMetadata tableMetadata : pTableMetadataList) {
			if (versionColumnName.equals(tableMetadata.getColumnName())) {
				return versionColumnName;
			}
		}
		log.info("Matching version-column-name ".concat(versionColumnName).concat(" has been found for ")
				.concat("table-name ").concat(pTableName)
				.concat(", but no table-column with this name could be found."));
		return versionColumnName;
	}
}
