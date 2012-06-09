package org.noorm.generator.enumgenerator;

import org.noorm.generator.GeneratorException;
import org.noorm.generator.GeneratorUtil;
import org.noorm.generator.ValidatorClassDescriptor;
import org.noorm.generator.m2plugin.IParameters;
import org.noorm.metadata.MetadataService;
import org.noorm.jdbc.Utils;
import org.noorm.metadata.beans.TableMetadataBean;
import org.noorm.jdbc.JDBCStatementProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Generator for database related Enum source files based on Velocity templates.
 *
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 19.04.11
 *         Time: 14:48
 */
public class EnumGenerator {

	private static final Logger log = LoggerFactory.getLogger(EnumGenerator.class);
	private static final String ENUM_VM_TEMPLATE_FILE = "/enum.vm";
	private static final String ENUM_VALIDATOR_VM_TEMPLATE_FILE = "/enum_validator.vm";
	private static final String ENUM_VALIDATOR_CLASS_NAME = "GenericEnumValidator";

	private IParameters parameters;

	public EnumGenerator(final IParameters pParameters) {
		parameters = pParameters;
	}

	public void execute() throws GeneratorException {

		if (parameters.getEnumPackageName() == null || parameters.getEnumPackageName().isEmpty()) {
			throw new IllegalArgumentException("Parameter [enumPackageName] is null.");
		}
		if (parameters.getDestinationDirectory() == null || !parameters.getDestinationDirectory().exists()) {
			throw new IllegalArgumentException("Parameter [destinationDirectory] is null or mis-configured.");
		}

		ValidatorClassDescriptor validatorClassDescriptor = new ValidatorClassDescriptor();
		validatorClassDescriptor.setPackageName(parameters.getEnumPackageName());

		log.info("Retrieving table metadata from Oracle database.");
		final MetadataService metadataService = MetadataService.getInstance();
		final Map<String, List<TableMetadataBean>> tableColumnMap = metadataService.findTableMetadata();

		log.info("Generating NoORM Enum classes.");
		final File enumPackageDir =
				GeneratorUtil.createPackageDir(parameters.getDestinationDirectory(), parameters.getEnumPackageName());

		for (final String tableName0 : tableColumnMap.keySet()) {
			if (parameters.getEnumTableFilterRegex() != null &&
					!tableName0.matches(parameters.getEnumTableFilterRegex())) {
				log.info("Exclude table ".concat(tableName0)
						.concat(", table name does not match regex '")
						.concat(parameters.getEnumTableFilterRegex())
						.concat("'"));
				continue;
			}
			final String javaEnumName =
					Utils.convertTableName2JavaName(tableName0, parameters.getIgnoreTableNamePrefixes());
			final List<TableMetadataBean> tableMetadataBeanList1 = tableColumnMap.get(tableName0);
			final EnumClassDescriptor enumClassDescriptor = new EnumClassDescriptor();
			enumClassDescriptor.setName(javaEnumName);
			validatorClassDescriptor.getClassNames().add(javaEnumName);
			enumClassDescriptor.setTableName(tableName0);
			enumClassDescriptor.setPackageName(parameters.getEnumPackageName());
			String displayColumnName;
			if (parameters.getEnumTable2DisplayColumnMapping() != null) {
				displayColumnName = Utils.getPropertyString(tableName0, parameters.getEnumTable2DisplayColumnMapping());
			} else {
				throw new GeneratorException
						("Parameter [enumTable2DisplayColumnMapping] must be set to enable enum generation.");
			}
			if (displayColumnName.isEmpty()) {
				throw new GeneratorException
						("Could not resolve the enum display column name using [enumTable2DisplayColumnMapping].");
			}
			enumClassDescriptor.setDisplayColumnName(displayColumnName);
			for (TableMetadataBean tableMetadataBean : tableMetadataBeanList1) {
				final EnumAttributeDescriptor enumAttributeDescriptor = new EnumAttributeDescriptor();
				final String javaColumnName = Utils.convertDBName2JavaName(tableMetadataBean.getColumnName(), false);
				enumAttributeDescriptor.setName(javaColumnName);
				final String javaType = Utils.convertOracleType2JavaType(tableMetadataBean.getDataType(),
						tableMetadataBean.getDataPrecision(), tableMetadataBean.getDataScale());
				enumAttributeDescriptor.setType(javaType);
				enumAttributeDescriptor.setColumnName(tableMetadataBean.getColumnName());
				enumClassDescriptor.addAttribute(enumAttributeDescriptor);
			}
			final JDBCStatementProcessor jdbcStatementProcessor = JDBCStatementProcessor.getInstance();
			final String query = "SELECT * FROM ".concat(tableName0);
			final List<Map<String, Object>> recordList = jdbcStatementProcessor.executeGenericSelect(query);
			if (recordList.isEmpty()) {
				log.info("Cannot generate enum: specified enum database table does not contain any data.");
				continue;
			}
			for (Map<String, Object> record : recordList) {
				final EnumRecordDescriptor enumRecordDescriptor = new EnumRecordDescriptor();
				final String displayColumnValue = (String) record.get(displayColumnName);
				if (displayColumnValue == null || displayColumnValue.isEmpty()) {
					String errMsg =
							"Could not resolve the enum display column value using [enumTable2DisplayColumnMapping]"
							.concat(" for table [".concat(tableName0)
							.concat("] and display column name [").concat(displayColumnName).concat("]."));
					throw new GeneratorException(errMsg);
				}
				enumRecordDescriptor.setDisplayColumnValue(displayColumnValue);
				final List<EnumAttributeDescriptor> enumAttributeDescriptorList = enumClassDescriptor.getAttributes();
				for (EnumAttributeDescriptor enumAttributeDescriptor : enumAttributeDescriptorList) {
					final String columnName = enumAttributeDescriptor.getColumnName();
					final Object value = record.get(columnName);
					enumRecordDescriptor.setAttributeValue(enumAttributeDescriptor, value);
				}
				enumClassDescriptor.addRecord(enumRecordDescriptor);
			}
			GeneratorUtil.generateFile(enumPackageDir, ENUM_VM_TEMPLATE_FILE,
					enumClassDescriptor.getName(), enumClassDescriptor);
		}
		GeneratorUtil.generateFile(enumPackageDir, ENUM_VALIDATOR_VM_TEMPLATE_FILE,
				ENUM_VALIDATOR_CLASS_NAME, validatorClassDescriptor);
	}
}
