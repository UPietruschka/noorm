package org.noorm.generator.enumgenerator;

import org.noorm.generator.GeneratorException;
import org.noorm.generator.GeneratorUtil;
import org.noorm.generator.ValidatorClassDescriptor;
import org.noorm.generator.IParameters;
import org.noorm.generator.schema.GeneratorConfiguration;
import org.noorm.generator.schema.Regex;
import org.noorm.jdbc.DataSourceProvider;
import org.noorm.jdbc.JDBCQueryProcessor;
import org.noorm.jdbc.platform.IMetadata;
import org.noorm.jdbc.platform.TableMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.Map;

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
    private GeneratorConfiguration configuration;

    public EnumGenerator(final IParameters pParameters, final GeneratorConfiguration pConfiguration) {
        parameters = pParameters;
        configuration = pConfiguration;
    }

	public void execute() throws GeneratorException {

        if (!GeneratorUtil.hasEnumPackageName(configuration)) {
			throw new IllegalArgumentException("Parameter [enumPackageName] is null.");
		}
		if (parameters.getDestinationDirectory() == null || !parameters.getDestinationDirectory().exists()) {
			throw new IllegalArgumentException("Parameter [destinationDirectory] is null or mis-configured.");
		}

		ValidatorClassDescriptor validatorClassDescriptor = new ValidatorClassDescriptor();
		validatorClassDescriptor.setPackageName(configuration.getEnumJavaPackage().getName());
        if (GeneratorUtil.hasDataSourceName(configuration)) {
            validatorClassDescriptor.setDataSourceName(configuration.getDataSource().getName());
        }

        log.info("Retrieving table metadata from database.");
        final IMetadata metadata = DataSourceProvider.getPlatform().getMetadata();
        String enumTableFilterRegex = null;
        if (configuration.getEnumTableFilter() != null) {
            enumTableFilterRegex = configuration.getEnumTableFilter().getRegex();
        } else {
            log.info("Parameter [enumTableFilter] not set. NoORM Enum generator will quit.");
            return;
        }
        final Map<String, List<TableMetadata>> tableColumnMap = metadata.findTableMetadata(enumTableFilterRegex);

		log.info("Generating NoORM Enum classes.");
		final File enumPackageDir =	GeneratorUtil.createPackageDir
                (parameters.getDestinationDirectory(), configuration.getEnumJavaPackage().getName());

		for (final String tableName0 : tableColumnMap.keySet()) {
			final String javaEnumName =
					GeneratorUtil.convertTableName2JavaName(tableName0, configuration.getTableNameMappings());
			final List<TableMetadata> tableMetadataList1 = tableColumnMap.get(tableName0);
			final EnumClassDescriptor enumClassDescriptor = new EnumClassDescriptor();
			enumClassDescriptor.setName(javaEnumName);
			validatorClassDescriptor.getClassNames().add(javaEnumName);
			enumClassDescriptor.setTableName(tableName0);
			enumClassDescriptor.setPackageName(configuration.getEnumJavaPackage().getName());
			String displayColumnName;
			if (configuration.getEnumTable2DisplayColumnMappings() != null) {
				displayColumnName =
                        GeneratorUtil.getMappedString(tableName0, configuration.getEnumTable2DisplayColumnMappings());
			} else {
				throw new GeneratorException
						("Parameter [enumTable2DisplayColumnMapping] must be set to enable enum generation.");
			}
			if (displayColumnName.isEmpty()) {
				throw new GeneratorException("Could not resolve the enum display column name using" +
                        " [enumTable2DisplayColumnMapping]; ".concat(tableName0));
			}
			enumClassDescriptor.setDisplayColumnName(displayColumnName);
			for (TableMetadata tableMetadata : tableMetadataList1) {
				final EnumAttributeDescriptor enumAttributeDescriptor = new EnumAttributeDescriptor();
                final String columnName = tableMetadata.getColumnName();
                final String javaName = GeneratorUtil.convertColumnName2JavaName
                        (columnName, false, configuration.getColumnNameMappings());
                enumAttributeDescriptor.setName(javaName);
                final String methodNamePostfix = GeneratorUtil.convertColumnName2JavaName
                        (columnName, true, configuration.getColumnNameMappings());
                enumAttributeDescriptor.setMethodNamePostfix(methodNamePostfix);
				final String javaType = GeneratorUtil.convertDatabaseType2JavaType(tableMetadata.getJDBCType(),
                        tableMetadata.getDecimalDigits(), tableMetadata.getTableName(),
                        columnName, configuration.getTypeMappings());
				enumAttributeDescriptor.setType(javaType);
				enumAttributeDescriptor.setColumnName(columnName);
				enumClassDescriptor.addAttribute(enumAttributeDescriptor);
			}
			final JDBCQueryProcessor jdbcQueryProcessor = JDBCQueryProcessor.getInstance();
			final String query = "SELECT * FROM ".concat(tableName0);
			final List<Map<String, Object>> recordList = jdbcQueryProcessor.executeGenericSelect(query);
			if (recordList.isEmpty()) {
                String errMsg =
                        "Cannot generate enum: specified enum database table does not contain any data."
                        .concat(" [".concat(tableName0).concat("]"));
                throw new GeneratorException(errMsg);
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
