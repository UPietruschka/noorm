package org.noorm.validation;

import org.noorm.jdbc.IEnum;
import org.noorm.jdbc.JDBCStatementProcessor;
import org.noorm.jdbc.Utils;
import org.noorm.metadata.MetadataService;
import org.noorm.metadata.beans.TableMetadataBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	protected Map<String, List<TableMetadataBean>> tableColumnMap;

	public void loadMetadata() {

		final MetadataService metadataService = MetadataService.getInstance();

		log.info("Retrieving table metadata from Oracle database.");
		tableColumnMap = metadataService.findTableMetadata();
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
		final String typeColumnName = enumArray[0].getTypeColumnName();
		final List<TableMetadataBean> tableMetadataBeanList = tableColumnMap.get(tableName);
		if (tableMetadataBeanList == null || tableMetadataBeanList.isEmpty()) {
			throw new ValidationException(exceptionPrefix.concat(" Cannot find table database table."));
		}
		// We omit direct meta-data validation here (like we do in BeanValidator), since we do access
		// the contained data, which would fail with non-validating meta-data anyway.
		final JDBCStatementProcessor jdbcStatementProcessor = JDBCStatementProcessor.getInstance();
		final String query = "SELECT * FROM ".concat(tableName);
		final List<Map<String, Object>> recordList = jdbcStatementProcessor.executeGenericSelect(query);
		if (recordList.isEmpty()) {
			throw new ValidationException(exceptionPrefix.concat(" Table does not contain any data."));
		}
		final Map<String, Map<String, Object>> typeColumnValue2Record = new HashMap<String, Map<String, Object>>();
		for (Map<String, Object> record : recordList) {
			final Object typeColumnValue = record.get(typeColumnName);
			if (typeColumnValue == null) {
				throw new ValidationException(exceptionPrefix
						.concat(" Table does not contain the enum type column ".concat(typeColumnName).concat(".")));
			}
			if (!(typeColumnValue instanceof String)) {
				throw new ValidationException(exceptionPrefix
						.concat(" Type columns other than type String are not supported yet."));
			}
			final String typeColumnStringValue = Utils.getNormalizedTypeColumnValue((String) typeColumnValue);
			typeColumnValue2Record.put(typeColumnStringValue, record);
		}

		log.debug("Validating Enum ".concat(pEnumClass.getName()));
		for (T enum0 : enumArray) {
			final Map<String, Object> record = typeColumnValue2Record.get(enum0.toString());
			if (record == null) {
				throw new ValidationException(exceptionPrefix
						.concat(" No matching table row found for type column value "
						.concat(enum0.toString())).concat("."));
			}
			log.debug(enum0.toString().concat(" validated."));
		}
	}
}
