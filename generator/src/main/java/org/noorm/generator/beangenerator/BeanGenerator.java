package org.noorm.generator.beangenerator;

import org.noorm.generator.GeneratorException;
import org.noorm.generator.GeneratorUtil;
import org.noorm.generator.ValidatorClassDescriptor;
import org.noorm.generator.m2plugin.IParameters;
import org.noorm.generator.schema.GeneratorConfiguration;
import org.noorm.metadata.BeanMetaDataUtil;
import org.noorm.metadata.MetadataService;
import org.noorm.metadata.beans.PrimaryKeyColumnBean;
import org.noorm.metadata.beans.SequenceBean;
import org.noorm.metadata.beans.TableMetadataBean;
import org.noorm.jdbc.Utils;
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

		if (configuration.getBeanPackageName() == null || configuration.getBeanPackageName().isEmpty()) {
			throw new IllegalArgumentException("Parameter [beanPackageName] is null.");
		}
		if (parameters.getDestinationDirectory() == null || !parameters.getDestinationDirectory().exists()) {
			throw new IllegalArgumentException("Parameter [destinationDirectory] is null or mis-configured.");
		}

		final ValidatorClassDescriptor validatorClassDescriptor = new ValidatorClassDescriptor();
		validatorClassDescriptor.setPackageName(configuration.getBeanPackageName());

		final BeanDMLClassDescriptor beanDMLClassDescriptor = new BeanDMLClassDescriptor();
		beanDMLClassDescriptor.setBeanPackageName(configuration.getBeanPackageName());
		beanDMLClassDescriptor.setInterfacePackageName(configuration.getServiceInterfacePackageName());
		beanDMLClassDescriptor.setPackageName(configuration.getServicePackageName());

        if (configuration.getDataSourceName() != null && !configuration.getDataSourceName().isEmpty()) {
            validatorClassDescriptor.setDataSourceName(configuration.getDataSourceName());
            beanDMLClassDescriptor.setDataSourceName(configuration.getDataSourceName());
        }

        final MetadataService metadataService = MetadataService.getInstance();

		log.info("Retrieving table metadata from Oracle database.");
		final Map<String, List<TableMetadataBean>> tableColumnMap = metadataService.findTableMetadata();
		log.info("Retrieving record metadata from Oracle database.");
		final Map<String, List<TableMetadataBean>> recordColumnMap = metadataService.findRecordMetadata();
		tableColumnMap.putAll(recordColumnMap);

		log.info("Retrieving primary key metadata from Oracle database.");
		final List<PrimaryKeyColumnBean> pkColumnNameList = metadataService.findPkColumns();

		log.info("Retrieving sequence metadata from Oracle database.");
		final List<SequenceBean> sequenceList = metadataService.findSequenceNames();

		log.info("Generating NoORM Bean classes.");
		final File beanPackageDir = GeneratorUtil.createPackageDir
				(parameters.getDestinationDirectory(), configuration.getBeanPackageName());
		final File servicePackageDir = GeneratorUtil.createPackageDir
				(parameters.getDestinationDirectory(), configuration.getServicePackageName());
		File serviceInterfacePackageDir = null;
		if (configuration.getServiceInterfacePackageName() != null &&
				!configuration.getServiceInterfacePackageName().isEmpty()) {
			serviceInterfacePackageDir = GeneratorUtil.createPackageDir
					(parameters.getDestinationDirectory(), configuration.getServiceInterfacePackageName());
		}

		for (final String tableName0 : tableColumnMap.keySet()) {
			if (configuration.getBeanTableFilterRegex() != null &&
					!tableName0.matches(configuration.getBeanTableFilterRegex())) {
				log.info("Exclude table ".concat(tableName0)
						.concat(", table name does not match regex '")
						.concat(configuration.getBeanTableFilterRegex())
						.concat("'"));
				continue;
			}
			if (tableName0.matches(IGNORE_BEAN_FILTER_REGEX)) {
				// Ignore the NoORM tables
				continue;
			}
			final String javaBeanName =
                    GeneratorUtil.convertTableName2BeanName(tableName0, configuration.getIgnoreTableNamePrefixes());
			final String shortName =
					Utils.convertTableName2ShortName(tableName0, configuration.getIgnoreTableNamePrefixes());
			final List<TableMetadataBean> tableMetadataBeanList1 = tableColumnMap.get(tableName0);
			final BeanClassDescriptor beanClassDescriptor = new BeanClassDescriptor();
			beanClassDescriptor.setName(javaBeanName);
			beanClassDescriptor.setShortName(shortName);
			if (configuration.getExtendedBeans() != null) {
				final String extJavaBeanName =
                        GeneratorUtil.getPropertyString(javaBeanName, configuration.getExtendedBeans());
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
                primaryKeyJavaNames[i] = GeneratorUtil.convertDBName2JavaName(primaryKeyColumnNames[i],
                        true, configuration.getIgnoreColumnNamePrefixes());
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
			final String versionColumnName = getVersionColumnName(tableName0, tableMetadataBeanList1);
			beanClassDescriptor.setVersionColumnName(versionColumnName);
			beanClassDescriptor.setPackageName(configuration.getBeanPackageName());
			// Use a unique seed for serialVersionUID generation to guarantee the generation of a reproducible
			// serialVersionUID with every new source code generation cycle.
			final Random random = new Random(javaBeanName.hashCode());
			final long serialVersionUID = random.nextLong();
			beanClassDescriptor.setSerialVersionUID(serialVersionUID);
            boolean unsupportedOptLockFullRowCompareTypes = false;
            String versionColumnType = "";
			for (final TableMetadataBean tableMetadataBean : tableMetadataBeanList1) {
				final BeanAttributeDescriptor beanAttributeDescriptor = new BeanAttributeDescriptor();
                final String columnName = tableMetadataBean.getColumnName();
				final String javaName = Utils.convertDBName2JavaName(columnName, false);
				beanAttributeDescriptor.setName(javaName);
                final String methodNamePostfix = GeneratorUtil.convertDBName2JavaName
                        (columnName, true, configuration.getIgnoreColumnNamePrefixes());
                beanAttributeDescriptor.setMethodNamePostfix(methodNamePostfix);
                final String dataType = tableMetadataBean.getDataType();
				final String javaType = GeneratorUtil.convertOracleType2JavaType(dataType,
						tableMetadataBean.getDataPrecision(), tableMetadataBean.getDataScale(),
                        tableMetadataBean.getTableName(), columnName, configuration.getCustomTypeMappings());
				if (tableMetadataBean.getUpdatable().equals(BeanMetaDataUtil.NOT_UPDATABLE) ||
						tableMetadataBean.getInsertable().equals(BeanMetaDataUtil.NOT_UPDATABLE)) {
					beanAttributeDescriptor.setUpdatable(false);
				}
				beanAttributeDescriptor.setType(javaType);
				beanAttributeDescriptor.setDataType(dataType);
                if (dataType.equals("CLOB") || dataType.equals("BLOB") || dataType.equals("XMLTYPE")) {
                    unsupportedOptLockFullRowCompareTypes = true;
                }
                if (!columnName.equals(columnName.toUpperCase())) {
                    beanAttributeDescriptor.setCaseSensitiveName(true);
                }
				beanAttributeDescriptor.setColumnName(columnName);
                if (versionColumnName.equals(columnName)) {
                    versionColumnType = dataType;
                }
				beanAttributeDescriptor.setMaxLength(tableMetadataBean.getCharLength().intValue());
				if (tableMetadataBean.getNullable().equals(BeanMetaDataUtil.NOT_NULLABLE)) {
					beanAttributeDescriptor.setNullable(false);
				}
				beanClassDescriptor.addAttribute(beanAttributeDescriptor);
			}
            if (!versionColumnType.isEmpty()) {
                if (versionColumnType.length() > 9) { versionColumnType = versionColumnType.substring(0, 9); }
                beanClassDescriptor.setVersionColumnType(versionColumnType);
            }
            if (configuration.getOptLockFullRowCompareTableRegex() != null &&
                    tableName0.matches(configuration.getOptLockFullRowCompareTableRegex())) {
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
		if (configuration.getServiceInterfacePackageName() != null &&
				!configuration.getServiceInterfacePackageName().isEmpty()) {
			beanDMLClassDescriptor.setInterface(true);
			GeneratorUtil.generateFile(serviceInterfacePackageDir, BEAN_DML_VM_TEMPLATE_FILE,
					beanDMLClassDescriptor.getJavaInterfaceName(), beanDMLClassDescriptor);
		}
	}

	private SequenceBean getSequence(final String pTableName,
                                     final List<SequenceBean> pSequenceList) {

		final String sequenceName =
                GeneratorUtil.getPropertyString(pTableName, configuration.getTable2SequenceMapping());
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
			if (configuration.getViewName2PrimaryKeyMapping() != null) {
				final String viewPKName =
                        GeneratorUtil.getPropertyString(pTableName, configuration.getViewName2PrimaryKeyMapping());
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
										final List<TableMetadataBean> pTableMetadataBeanList) {

		String versionColumnName = "";
		if (configuration.getOptLockVersionColumnMapping() != null) {
			versionColumnName =
                    GeneratorUtil.getPropertyString(pTableName, configuration.getOptLockVersionColumnMapping());
		}
		if (versionColumnName.isEmpty()) {
			log.info("No matching version-column-name has been found for table-name ".concat(pTableName));
			return versionColumnName;
		}
		// Check the matched version column name against the column names of this table
		for (final TableMetadataBean tableMetadataBean : pTableMetadataBeanList) {
			if (versionColumnName.equals(tableMetadataBean.getColumnName())) {
				return versionColumnName;
			}
		}
		log.info("Matching version-column-name ".concat(versionColumnName).concat(" has been found for ")
				.concat("table-name ").concat(pTableName)
				.concat(", but no table-column with this name could be found."));
		return versionColumnName;
	}
}
