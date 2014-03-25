package org.noorm.generator.beangenerator;

import org.noorm.generator.GeneratorException;
import org.noorm.generator.GeneratorUtil;
import org.noorm.generator.ValidatorClassDescriptor;
import org.noorm.generator.m2plugin.IParameters;
import org.noorm.generator.schema.GeneratorConfiguration;
import org.noorm.generator.schema.Regex;
import org.noorm.jdbc.DataSourceProvider;
import org.noorm.platform.IMetadata;
import org.noorm.metadata.beans.PrimaryKeyColumnBean;
import org.noorm.metadata.beans.SequenceBean;
import org.noorm.platform.JDBCType;
import org.noorm.platform.TableMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
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
	private static final String IGNORE_BEAN_FILTER_REGEX = "(DYNSQL_QUERY_TEMPLATE)";

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

		final BeanDMLClassDescriptor beanDMLClassDescriptor = new BeanDMLClassDescriptor();
		beanDMLClassDescriptor.setBeanPackageName(configuration.getBeanJavaPackage().getName());
        if (GeneratorUtil.hasServiceInterfacePackageName(configuration)) {
            beanDMLClassDescriptor.setInterfacePackageName(configuration.getServiceInterfaceJavaPackage().getName());
        }
		beanDMLClassDescriptor.setPackageName(configuration.getServiceJavaPackage().getName());

        if (GeneratorUtil.hasDataSourceName(configuration)) {
            validatorClassDescriptor.setDataSourceName(configuration.getDataSource().getName());
            beanDMLClassDescriptor.setDataSourceName(configuration.getDataSource().getName());
        }

        final IMetadata metadata = DataSourceProvider.getPlatform().getMetadata();

		log.info("Retrieving table metadata from database.");
		final Map<String, List<TableMetadata>> tableColumnMap = metadata.findTableMetadata();
		log.info("Retrieving record metadata from database.");
		final Map<String, List<TableMetadata>> recordColumnMap = metadata.findRecordMetadata();
		tableColumnMap.putAll(recordColumnMap);

		log.info("Retrieving primary key metadata from database.");
		final List<PrimaryKeyColumnBean> pkColumnNameList = metadata.findPkColumns();

		log.info("Retrieving sequence metadata from database.");
		final List<SequenceBean> sequenceList = metadata.findSequenceNames();

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
            final Regex beanTableFilter = configuration.getBeanTableFilter();
			if (beanTableFilter != null && beanTableFilter.getRegex() != null
					&& !tableName0.matches(beanTableFilter.getRegex())) {
				log.info("Exclude table ".concat(tableName0)
						.concat(", table name does not match regex '")
						.concat(beanTableFilter.getRegex())
						.concat("'"));
				continue;
			}
			if (tableName0.matches(IGNORE_BEAN_FILTER_REGEX)) {
				// Ignore the NoORM tables
				continue;
			}
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
			final String[] primaryKeyColumnNames = getPrimaryKeyColumnNames(tableName0, pkColumnNameList);
			beanClassDescriptor.setPrimaryKeyColumnNames(primaryKeyColumnNames);
            final String[] primaryKeyJavaNames = new String[primaryKeyColumnNames.length];
            for (int i = 0; i < primaryKeyColumnNames.length; i++) {
                primaryKeyJavaNames[i] = GeneratorUtil.convertColumnName2JavaName(primaryKeyColumnNames[i],
                        false, configuration.getColumnNameMappings());
            }
            beanClassDescriptor.setPrimaryKeyJavaNames(primaryKeyJavaNames);
            beanClassDescriptor.setGeneratePKBasedEqualsAndHashCode(configuration.isGeneratePKBasedEqualsAndHashCode());
			final SequenceBean sequence = getSequence(tableName0, sequenceList);
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
                beanClassDescriptor.setSequenceIncrement(0L);
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

                final JDBCType jdbcType = tableMetadata.getJDBCType();
				final String javaType = GeneratorUtil.convertDatabaseType2JavaType(jdbcType,
                        tableMetadata.getDecimalDigits(), tableMetadata.getTableName(),
                        columnName, configuration.getTypeMappings());
				if (!tableMetadata.getUpdatable()) {
					beanAttributeDescriptor.setUpdatable(false);
				}
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
                    versionColumnType = typeName;
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
			beanDMLClassDescriptor.addBean(beanClassDescriptor);
		}
		GeneratorUtil.generateFile(beanPackageDir, BEAN_VALIDATOR_VM_TEMPLATE_FILE,
				BEAN_VALIDATOR_CLASS_NAME, validatorClassDescriptor);
		GeneratorUtil.generateFile(servicePackageDir, BEAN_DML_VM_TEMPLATE_FILE,
                beanDMLClassDescriptor.getJavaName(), beanDMLClassDescriptor);
        if (GeneratorUtil.hasServiceInterfacePackageName(configuration)) {
			beanDMLClassDescriptor.setInterface(true);
			GeneratorUtil.generateFile(serviceInterfacePackageDir, BEAN_DML_VM_TEMPLATE_FILE,
					beanDMLClassDescriptor.getJavaInterfaceName(), beanDMLClassDescriptor);
		}
	}

	private SequenceBean getSequence(final String pTableName,
                                     final List<SequenceBean> pSequenceList) {

		final String sequenceName =
                GeneratorUtil.getMappedString(pTableName, configuration.getTable2SequenceMappings());
		if (sequenceName.isEmpty()) {
			log.info("No matching sequence-name has been found for table-name ".concat(pTableName));
			return null;
		}
		// Check the matched sequence name against the sequence names retrieved from the database
		for (final SequenceBean sequence : pSequenceList) {
			if (sequenceName.equals(sequence.getName())) {
				return sequence;
			}
		}
		throw new GeneratorException("Matching sequence-name ".concat(sequenceName)
                .concat(" has been found for table-name ")
				.concat(pTableName).concat(", but no database sequence with this name could be found."));
	}

	private String[] getPrimaryKeyColumnNames(final String pTableName,
										      final List<PrimaryKeyColumnBean> pPrimaryKeyColumnList) {

		final List<String> pkColumnNames = new ArrayList<String>();
		for (final PrimaryKeyColumnBean primaryKeyBean : pPrimaryKeyColumnList) {
			if (pTableName.equals(primaryKeyBean.getTableName())) {
				log.info("Primary key column ".concat(primaryKeyBean.getColumnName())
						.concat(" found for table ").concat(pTableName));
				pkColumnNames.add(primaryKeyBean.getColumnName());
			}
		}
		if (pkColumnNames.isEmpty()) {
			if (configuration.getViewName2PrimaryKeyMappings() != null) {
				final String viewPKName =
                        GeneratorUtil.getMappedString(pTableName, configuration.getViewName2PrimaryKeyMappings());
				if (!viewPKName.isEmpty()) {
					pkColumnNames.add(viewPKName);
				}
			}
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
