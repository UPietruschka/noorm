package org.noorm.generator.beangenerator;

import org.noorm.generator.GeneratorException;
import org.noorm.generator.GeneratorUtil;
import org.noorm.generator.ValidatorClassDescriptor;
import org.noorm.generator.m2plugin.IParameters;
import org.noorm.metadata.BeanMetaDataUtil;
import org.noorm.metadata.MetadataService;
import org.noorm.metadata.beans.NameBean;
import org.noorm.metadata.beans.PrimaryKeyColumnBean;
import org.noorm.metadata.beans.TableMetadataBean;
import org.noorm.jdbc.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
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
	private static final String BEAN_DML_CLASS_NAME = "BeanDML";
	private static final String IGNORE_BEAN_FILTER_REGEX = "(DYNSQL_QUERY_TEMPLATE)";

	private IParameters parameters;

	public BeanGenerator(final IParameters pParameters) {
		parameters = pParameters;
	}

	public void execute() throws GeneratorException {

		if (parameters.getBeanPackageName() == null || parameters.getBeanPackageName().isEmpty()) {
			throw new IllegalArgumentException("Parameter [beanPackageName] is null.");
		}
		if (parameters.getDestinationDirectory() == null || !parameters.getDestinationDirectory().exists()) {
			throw new IllegalArgumentException("Parameter [destinationDirectory] is null or mis-configured.");
		}

		final ValidatorClassDescriptor validatorClassDescriptor = new ValidatorClassDescriptor();
		validatorClassDescriptor.setPackageName(parameters.getBeanPackageName());

		final BeanDMLClassDescriptor beanDMLClassDescriptor = new BeanDMLClassDescriptor();
		beanDMLClassDescriptor.setBeanPackageName(parameters.getBeanPackageName());
		beanDMLClassDescriptor.setInterfacePackageName(parameters.getServiceInterfacePackageName());
		beanDMLClassDescriptor.setPackageName(parameters.getServicePackageName());

        if (parameters.getDataSourceName() != null && !parameters.getDataSourceName().isEmpty()) {
            validatorClassDescriptor.setDataSourceName(parameters.getDataSourceName());
            beanDMLClassDescriptor.setDataSourceName(parameters.getDataSourceName());
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
		final List<NameBean> sequenceDBNameList = metadataService.findSequenceNames();

		log.info("Generating NoORM Bean classes.");
		final File beanPackageDir = GeneratorUtil.createPackageDir
				(parameters.getDestinationDirectory(), parameters.getBeanPackageName());
		final File servicePackageDir = GeneratorUtil.createPackageDir
				(parameters.getDestinationDirectory(), parameters.getServicePackageName());
		File serviceInterfacePackageDir = null;
		if (parameters.getServiceInterfacePackageName() != null &&
				!parameters.getServiceInterfacePackageName().isEmpty()) {
			serviceInterfacePackageDir = GeneratorUtil.createPackageDir
					(parameters.getDestinationDirectory(), parameters.getServiceInterfacePackageName());
		}

		for (final String tableName0 : tableColumnMap.keySet()) {
			if (parameters.getBeanTableFilterRegex() != null &&
					!tableName0.matches(parameters.getBeanTableFilterRegex())) {
				log.info("Exclude table ".concat(tableName0)
						.concat(", table name does not match regex '")
						.concat(parameters.getBeanTableFilterRegex())
						.concat("'"));
				continue;
			}
			if (tableName0.matches(IGNORE_BEAN_FILTER_REGEX)) {
				// Ignore the NoORM tables
				continue;
			}
			final String javaBeanName =
					Utils.convertTableName2BeanName(tableName0, parameters.getIgnoreTableNamePrefixes());
			final String shortName =
					Utils.convertTableName2ShortName(tableName0, parameters.getIgnoreTableNamePrefixes());
			final List<TableMetadataBean> tableMetadataBeanList1 = tableColumnMap.get(tableName0);
			final BeanClassDescriptor beanClassDescriptor = new BeanClassDescriptor();
			beanClassDescriptor.setName(javaBeanName);
			beanClassDescriptor.setShortName(shortName);
			if (parameters.getExtendedBeans() != null) {
				final String extJavaBeanName = Utils.getPropertyString(javaBeanName, parameters.getExtendedBeans());
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
			final String[] primaryKeyColumnNames = getPrimaryKeyColumnNames(tableName0, pkColumnNameList);
			beanClassDescriptor.setPrimaryKeyColumnNames(primaryKeyColumnNames);
			final String sequenceName = getSequenceName(tableName0, sequenceDBNameList);
			beanClassDescriptor.setSequenceName(sequenceName);
			final String versionColumnName = getVersionColumnName(tableName0, tableMetadataBeanList1);
			beanClassDescriptor.setVersionColumnName(versionColumnName);
			beanClassDescriptor.setPackageName(parameters.getBeanPackageName());
			// Use a unique seed for serialVersionUID generation to guarantee the generation of a reproducible
			// serialVersionUID with every new source code generation cycle.
			final Random random = new Random(javaBeanName.hashCode());
			final long serialVersionUID = random.nextLong();
			beanClassDescriptor.setSerialVersionUID(serialVersionUID);
			for (final TableMetadataBean tableMetadataBean : tableMetadataBeanList1) {
				final BeanAttributeDescriptor beanAttributeDescriptor = new BeanAttributeDescriptor();
				final String javaColumnName = Utils.convertDBName2JavaName(tableMetadataBean.getColumnName(), false);
				beanAttributeDescriptor.setName(javaColumnName);
				final String javaType = Utils.convertOracleType2JavaType(tableMetadataBean.getDataType(),
						tableMetadataBean.getDataPrecision(), tableMetadataBean.getDataScale());
				if (tableMetadataBean.getUpdatable().equals(BeanMetaDataUtil.NOT_UPDATABLE) ||
						tableMetadataBean.getInsertable().equals(BeanMetaDataUtil.NOT_UPDATABLE)) {
					beanAttributeDescriptor.setUpdatable(false);
				}
				beanAttributeDescriptor.setType(javaType);
				beanAttributeDescriptor.setDataType(tableMetadataBean.getDataType());
				beanAttributeDescriptor.setColumnName(tableMetadataBean.getColumnName());
				beanAttributeDescriptor.setMaxLength(tableMetadataBean.getCharLength().intValue());
				if (tableMetadataBean.getNullable().equals(BeanMetaDataUtil.NOT_NULLABLE)) {
					beanAttributeDescriptor.setNullable(false);
				}
				beanClassDescriptor.addAttribute(beanAttributeDescriptor);
			}
			GeneratorUtil.generateFile(beanPackageDir, BEAN_VM_TEMPLATE_FILE,
					beanClassDescriptor.getName(), beanClassDescriptor);
			beanDMLClassDescriptor.addBean(beanClassDescriptor);
		}
		GeneratorUtil.generateFile(beanPackageDir, BEAN_VALIDATOR_VM_TEMPLATE_FILE,
				BEAN_VALIDATOR_CLASS_NAME, validatorClassDescriptor);
		GeneratorUtil.generateFile(servicePackageDir, BEAN_DML_VM_TEMPLATE_FILE,
				BEAN_DML_CLASS_NAME, beanDMLClassDescriptor);
		if (parameters.getServiceInterfacePackageName() != null &&
				!parameters.getServiceInterfacePackageName().isEmpty()) {
			beanDMLClassDescriptor.setInterface(true);
			GeneratorUtil.generateFile(serviceInterfacePackageDir, BEAN_DML_VM_TEMPLATE_FILE,
					beanDMLClassDescriptor.getJavaInterfaceName(), beanDMLClassDescriptor);
		}
	}

	private String getSequenceName(final String pTableName,
								   final List<NameBean> pSequenceDBNameList) {

		final String sequenceName = Utils.getPropertyString(pTableName, parameters.getOracleTable2SequenceMapping());
		if (sequenceName.isEmpty()) {
			log.info("No matching sequence-name has been found for table-name ".concat(pTableName));
			return sequenceName;
		}
		// Check the matched sequence name against the sequence names retrieved from the database
		for (final NameBean sequenceDBName : pSequenceDBNameList) {
			if (sequenceName.equals(sequenceDBName.getName())) {
				return sequenceName;
			}
		}
		log.info("Matching sequence-name ".concat(sequenceName).concat(" has been found for table-name ")
				.concat(pTableName).concat(", but no database sequence with this name could be found."));
		return sequenceName;
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
			if (parameters.getViewName2PrimaryKeyMapping() != null) {
				final String viewPKName =
						Utils.getPropertyString(pTableName, parameters.getViewName2PrimaryKeyMapping());
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
		if (parameters.getOptimisticLockColumnMapping() != null) {
			versionColumnName = Utils.getPropertyString(pTableName, parameters.getOptimisticLockColumnMapping());
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
