package org.noorm.validation;

import org.noorm.jdbc.DataSourceProvider;
import org.noorm.jdbc.IEnum;
import org.noorm.jdbc.JDBCColumn;
import org.noorm.jdbc.JDBCQueryProcessor;
import org.noorm.jdbc.Utils;
import org.noorm.jdbc.BeanMetaDataUtil;
import org.noorm.jdbc.platform.IMetadata;
import org.noorm.jdbc.platform.TableMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 30.11.11
 *         Time: 13:22
 */
public class EnumValidator {

	private static final Logger log = LoggerFactory.getLogger(EnumValidator.class);

	protected Map<String, List<TableMetadata>> tableColumnMap;

	public void loadMetadata(final String pSchemaPattern, final String pTableNamePattern) {

        final IMetadata metadata = DataSourceProvider.getPlatform().getMetadata();

		log.debug("Retrieving table metadata from database.");
		tableColumnMap = metadata.findTableMetadata(pSchemaPattern, pTableNamePattern);
	}

	public <T extends Enum<T> & IEnum> void validateEnum(final Class<T> pEnumClass) {

		T[] enumArray = pEnumClass.getEnumConstants();
		final String tableName = enumArray[0].getTableName();
		final String javaEnumName = pEnumClass.getName();
		final StringBuilder msgBuilder = new StringBuilder();
		msgBuilder.append("Validating enum ");
		msgBuilder.append(javaEnumName);
		msgBuilder.append(" against database table ");
		msgBuilder.append(tableName);
		msgBuilder.append(" failed.");
		final String exceptionPrefix = msgBuilder.toString();
		final String displayColumnName = enumArray[0].getDisplayColumnName();
		final List<TableMetadata> tableMetadataList = tableColumnMap.get(tableName);
		if (tableMetadataList == null || tableMetadataList.isEmpty()) {
			validationError(exceptionPrefix.concat(" Cannot find database table."));
		}
		// We omit direct meta-data validation here (like we do in BeanValidator), since we do access
		// the contained data, which would fail with non-validating meta-data anyway.
		final JDBCQueryProcessor jdbcQueryProcessor = JDBCQueryProcessor.getInstance();
		final String query = "SELECT * FROM ".concat(tableName);
		final List<Map<String, Object>> recordList = jdbcQueryProcessor.executeGenericSelect(query);
		if (recordList.isEmpty()) {
			validationError(exceptionPrefix.concat(" Database table does not contain any data."));
		}
		final Map<String, Map<String, Object>> displayColumnValue2Record = new HashMap<String, Map<String, Object>>();
		for (Map<String, Object> record : recordList) {
			final Object displayColumnValue = record.get(displayColumnName);
			if (displayColumnValue == null) {
				validationError(exceptionPrefix
						.concat(" Database table does not contain the enum display column "
						.concat(displayColumnName).concat(".")));
			}
			if (!(displayColumnValue instanceof String)) {
				validationError(exceptionPrefix
						.concat(" Display columns other than type String are not yet supported."));
			}
			final String displayColumnStringValue = Utils.getNormalizedDisplayColumnValue((String) displayColumnValue);
			displayColumnValue2Record.put(displayColumnStringValue, record);
		}

		log.info("Validating Enum ".concat(pEnumClass.getName()));
		for (T enum0 : enumArray) {
			final Map<String, Object> record = displayColumnValue2Record.get(enum0.toString());
			if (record == null) {
				validationError(exceptionPrefix
						.concat(" No matching table row found for display column value "
						.concat(enum0.toString())).concat("."));
			}
			// Iterate over the remaining fields and validate enum against database content
            final Field[] fields = BeanMetaDataUtil.getDeclaredFieldsInclParent(pEnumClass);
            final Map<String, String> columnName2JavaName = new HashMap<String, String>();
            for (final Field field : fields) {
                final JDBCColumn colAnn = BeanMetaDataUtil.getJDBCColumnAnnotation(field);
                if (colAnn != null) {
                    final String javaName = field.getName();
                    columnName2JavaName.put(colAnn.name(), javaName);
                }
            }
			for (Map.Entry<String, Object> columnMap : record.entrySet()) {
				final String columnName = columnMap.getKey();
				if (!columnName.equals((displayColumnName))) {
					final Object columnValue = columnMap.getValue();
                    final String javaPropertyName = columnName2JavaName.get(columnName);
                    if (javaPropertyName == null) {
                        validationError(exceptionPrefix
                                .concat(" No Java attribute found for column ").concat(columnName).concat("."));
                    }
					final Object enumValue = BeanMetaDataUtil.getBeanPropertyByName(enum0, javaPropertyName);
					log.debug("Validating Enum value against database value for column [".concat(columnName)
							.concat("]; ").concat(enumValue.toString()).concat(" / ")
							.concat(columnValue.toString()).concat("."));
					// Using "toString" provides a suitable normalization for the data subject to comparison,
					// at least for the data-types supported for enums so far (Long, String)
					if (!enumValue.toString().equals(columnValue.toString())) {
						validationError(exceptionPrefix
								.concat(" Content mismatch for column ").concat(columnName).concat("."));
					}
				}
			}
		}
		log.info("Enum ".concat(pEnumClass.getName()).concat(" validated."));
	}

	/*
	 Validation errors result in a validation exception.
	 For a web application, the validators may get called from inside of a ServletContextListener implementation,
	 which does not write to the NoORM logger or application logger. Thus, validation errors are explicitly logged.
	 */
	private void validationError(final String pErrorMessage) {

		log.error(pErrorMessage);
		throw new ValidationException(pErrorMessage);
	}
}
