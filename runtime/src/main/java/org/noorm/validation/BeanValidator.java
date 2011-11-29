package org.noorm.validation;

import org.noorm.jdbc.IBean;
import org.noorm.jdbc.JDBCColumn;
import org.noorm.metadata.BeanMetaDataUtil;
import org.noorm.metadata.MetadataService;
import org.noorm.metadata.beans.NameBean;
import org.noorm.metadata.beans.PrimaryKeyColumnBean;
import org.noorm.metadata.beans.TableMetadataBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 28.11.11
 *         Time: 15:42
 */
public class BeanValidator {

	private static final Logger log = LoggerFactory.getLogger(BeanValidator.class);

	Map<String, List<TableMetadataBean>> tableColumnMap;
	List<PrimaryKeyColumnBean> pkColumnNameList;
	List<NameBean> sequenceDBNameList;

	public void loadMetadata() {

		final MetadataService metadataService = MetadataService.getInstance();

		log.info("Retrieving table metadata from Oracle database.");
		tableColumnMap = metadataService.findTableMetadata();

		log.info("Retrieving primary key metadata from Oracle database.");
		pkColumnNameList = metadataService.findPkColumns();

		log.info("Retrieving sequence metadata from Oracle database.");
		sequenceDBNameList = metadataService.findSequenceNames();
	}

	public void validateBean(final IBean pBean) {

		final String tableName = pBean.getTableName();
		final String javaBeanName = pBean.getClass().getName();
		final List<TableMetadataBean> tableMetadataBeanList = tableColumnMap.get(tableName);
		if (tableMetadataBeanList == null || tableMetadataBeanList.isEmpty()) {
			throw new ValidationException("Cannot find table ".concat(tableName).concat(" in connected DB schema."));
		}
		final Map<String, JDBCColumn> beanMetadata = BeanMetaDataUtil.getColumnMetaData(pBean.getClass());
		for (final TableMetadataBean tableMetadataBean : tableMetadataBeanList) {
			final String columnName = tableMetadataBean.getColumnName();
			boolean beanColumnValidated = false;
			String javaAttributeName = null;
			for (final Map.Entry<String, JDBCColumn> beanMetaDataEntry : beanMetadata.entrySet()) {
				final JDBCColumn jdbcColumn = beanMetaDataEntry.getValue();
				if (jdbcColumn.name().equals(columnName)) {
					if (!jdbcColumn.dataType().equals(tableMetadataBean.getDataType())) {
						throw new ValidationException("Table column ".concat(tableName).concat(".").concat(columnName)
								.concat(" datatype ").concat(tableMetadataBean.getDataType())
								.concat(" does not match datatype ").concat(jdbcColumn.dataType())
								.concat(" in Java Bean ").concat(javaBeanName));
					}
					// BeanValidator does not yet validate JDBCColumn attributes nullable, updatable
					// and maxLength against the data dictionary of the database.
					log.debug("Table column ".concat(tableName).concat(".")
							.concat(columnName).concat(" successfully validated against bean specification."));
					javaAttributeName = beanMetaDataEntry.getKey();
					beanColumnValidated = true;
				}
			}
			if (beanColumnValidated) {
				beanMetadata.remove(javaAttributeName);
			} else {
				throw new ValidationException("Table column ".concat(tableName).concat(".").concat(columnName)
						.concat(" not found in Java Bean ").concat(javaBeanName));
			}
		}
		if (!beanMetadata.isEmpty()) {
			final StringBuilder message = new StringBuilder();
			String delimiter = "";
			for (final String javaAttributeName : beanMetadata.keySet()) {
				message.append(delimiter);
				message.append("Java Bean attribute ");
				message.append(javaBeanName);
				message.append(".");
				message.append(javaAttributeName);
				message.append(" not found in database table ");
				message.append(tableName);
				delimiter = "\n";
			}
			throw new ValidationException(message.toString());
		}
	}
}
