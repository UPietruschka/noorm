package org.noorm.generator.enumgenerator;

import org.noorm.generator.GeneratorException;
import org.noorm.generator.GeneratorUtil;
import org.noorm.generator.ValidatorClassDescriptor;
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
	private static EnumGenerator enumGenerator;

	/**
	 * Destination directory for generated source files.
	 */
	private File destinationDirectory;

	/**
	 * Package name for generated Enum source files.
	 */
	private String enumPackageName;

	/**
	 * List of table name prefixes to be ignored for java class name construction.
	 * Some data modelers use a common table name prefix to identify tables of a
	 * given schema or group. When those prefixes are not desired in the constructed
	 * java class name, they should be listed here.
	 */
	private List<String> ignoreTableNamePrefixes;

	/**
	 * Regular expression to filter tables and views for Enum generation.
	 */
	private String enumTableFilterRegex;

	/**
	 * To generate Enums from database tables, NoORM must now, which table column should be used
	 * for the enums constant type generation. Typically, a table with constant content has a column
	 * with a code or denominator in uppercase letters, which uniquely identifies the row.
	 */
	private Properties enumTable2TypeColumnMapping;

	protected EnumGenerator() {
	}

	public void setDestinationDirectory(final File pDestinationDirectory) {
		destinationDirectory = pDestinationDirectory;
	}

	public void setEnumPackageName(final String pEnumPackageName) {
		enumPackageName = pEnumPackageName;
	}

	public void setIgnoreTableNamePrefixes(final List<String> pIgnoreTableNamePrefixes) {
		ignoreTableNamePrefixes = pIgnoreTableNamePrefixes;
	}

	public void setEnumTableFilterRegex(final String pEnumTableFilterRegex) {
		enumTableFilterRegex = pEnumTableFilterRegex;
	}

	public void setEnumTable2TypeColumnMapping(final Properties pEnumTable2TypeColumnMapping) {
		enumTable2TypeColumnMapping = pEnumTable2TypeColumnMapping;
	}

	public static EnumGenerator getInstance() {

		synchronized (EnumGenerator.class) {
			if (enumGenerator == null) {
				enumGenerator = new EnumGenerator();
			}
		}
		return enumGenerator;
	}

	public void execute() throws GeneratorException {

		if (enumPackageName == null || enumPackageName.isEmpty()) {
			throw new IllegalArgumentException("Parameter [enumPackageName] is null.");
		}
		if (destinationDirectory == null || !destinationDirectory.exists()) {
			throw new IllegalArgumentException("Parameter [destinationDirectory] is null or mis-configured.");
		}

		ValidatorClassDescriptor validatorClassDescriptor = new ValidatorClassDescriptor();
		validatorClassDescriptor.setPackageName(enumPackageName);

		log.info("Retrieving table metadata from Oracle database.");
		final MetadataService metadataService = MetadataService.getInstance();
		final Map<String, List<TableMetadataBean>> tableColumnMap = metadataService.findTableMetadata();

		log.info("Generating NoORM Enum classes.");
		final File enumPackageDir = new File(destinationDirectory, enumPackageName.replace(".", File.separator));
		if (!enumPackageDir.exists()) {
			enumPackageDir.mkdirs();
		}

		for (final String tableName0 : tableColumnMap.keySet()) {
			if (enumTableFilterRegex != null && !tableName0.matches(enumTableFilterRegex)) {
				log.info("Exclude table ".concat(tableName0)
						.concat(", table name does not match regex '")
						.concat(enumTableFilterRegex)
						.concat("'"));
				continue;
			}
			final String javaEnumName = Utils.convertTableName2JavaName(tableName0, ignoreTableNamePrefixes);
			final List<TableMetadataBean> tableMetadataBeanList1 = tableColumnMap.get(tableName0);
			final EnumClassDescriptor enumClassDescriptor = new EnumClassDescriptor();
			enumClassDescriptor.setName(javaEnumName);
			validatorClassDescriptor.getClassNames().add(javaEnumName);
			enumClassDescriptor.setTableName(tableName0);
			enumClassDescriptor.setPackageName(enumPackageName);
			String typeColumnName = "";
			if (enumTable2TypeColumnMapping != null) {
				typeColumnName = Utils.getPropertyString(tableName0, enumTable2TypeColumnMapping);
			} else {
				throw new GeneratorException
						("Parameter [enumTable2TypeColumnMapping] must be set to enable enum generation.");
			}
			if (typeColumnName.isEmpty()) {
				throw new GeneratorException
						("Could not resolve the enum type column name using [enumTable2TypeColumnMapping].");
			}
			enumClassDescriptor.setTypeColumnName(typeColumnName);
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
				final String typeColumnValue = (String) record.get(typeColumnName);
				if (typeColumnValue == null || typeColumnValue.isEmpty()) {
					String errMsg = "Could not resolve the enum type column value using [enumTable2TypeColumnMapping]"
							.concat(" for table [".concat(tableName0)
							.concat("] and type column name [").concat(typeColumnName).concat("]."));
					throw new GeneratorException(errMsg);
				}
				enumRecordDescriptor.setTypeColumnValue(typeColumnValue);
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
