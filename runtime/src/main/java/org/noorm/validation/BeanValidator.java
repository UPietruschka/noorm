package org.noorm.validation;

import org.noorm.jdbc.IBean;
import org.noorm.jdbc.JDBCColumn;
import org.noorm.metadata.BeanMetaDataUtil;
import org.noorm.metadata.MetadataService;
import org.noorm.metadata.beans.PrimaryKeyColumnBean;
import org.noorm.metadata.beans.SequenceBean;
import org.noorm.metadata.beans.TableMetadataBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 28.11.11
 *         Time: 15:42
 */
public class BeanValidator {

	private static final Logger log = LoggerFactory.getLogger(BeanValidator.class);

	protected Map<String, List<TableMetadataBean>> tableColumnMap;
	protected List<PrimaryKeyColumnBean> allPKColumnNameList;
	protected List<SequenceBean> sequenceDBNameList;

	public void loadMetadata() {

		final MetadataService metadataService = MetadataService.getInstance();

		log.debug("Retrieving table metadata from database.");
		tableColumnMap = metadataService.findTableMetadata();

		log.debug("Retrieving primary key metadata from database.");
		allPKColumnNameList = metadataService.findPkColumns();

		log.debug("Retrieving sequence metadata from database.");
		sequenceDBNameList = metadataService.findSequenceNames();
	}

	public void validateBean(final IBean pBean) {

		final String tableName = pBean.getTableName();
		final String javaBeanName = pBean.getClass().getName();
		log.info("Validating Java Bean ".concat(javaBeanName).concat(" against database table ".concat(tableName)));
		final List<TableMetadataBean> tableMetadataBeanList = tableColumnMap.get(tableName);
		if (tableMetadataBeanList == null || tableMetadataBeanList.isEmpty()) {
			validationError("Cannot find table ".concat(tableName).concat(" in connected DB schema."));
		}
		final Map<String, JDBCColumn> beanMetadata = BeanMetaDataUtil.getColumnMetaData(pBean.getClass());
		for (final TableMetadataBean tableMetadataBean : tableMetadataBeanList) {
			final String columnName = tableMetadataBean.getColumnName();
			final StringBuilder msgBuilder = new StringBuilder();
			msgBuilder.append("Table column ");
			msgBuilder.append(tableName);
			msgBuilder.append(".");
			msgBuilder.append(columnName);
			msgBuilder.append(" does not match corresponding Java Bean attribute for ");
			msgBuilder.append(javaBeanName);
			msgBuilder.append(". ");
			final String exceptionPrefix = msgBuilder.toString();
			boolean beanColumnValidated = false;
			String javaAttributeName = null;
			for (final Map.Entry<String, JDBCColumn> beanMetaDataEntry : beanMetadata.entrySet()) {
				final JDBCColumn jdbcColumn = beanMetaDataEntry.getValue();
				if (jdbcColumn.name().equals(columnName)) {
					// Validating annotated data-type against database metadata.
// TODO: re-enable validation
//					if (!jdbcColumn.dataType().equals(tableMetadataBean.getDataType())) {
//						validationError(exceptionPrefix.concat("Data-type mismatch: [")
//								.concat(tableMetadataBean.getDataType()).concat(" / ")
//								.concat(jdbcColumn.dataType().concat("].")));
//					}
					// Validating annotated nullable-attribute against database metadata.
					boolean isNullable = true;
					if (tableMetadataBean.getNullable().equals(BeanMetaDataUtil.NOT_NULLABLE)) {
						isNullable = false;
					}
					if (jdbcColumn.nullable() != isNullable) {
						validationError(exceptionPrefix.concat("Nullable indicator mismatch"));
					}
					// Validating annotated updatable-attribute against database metadata.
					boolean isUpdatable = true;
					if (tableMetadataBean.getUpdatable().equals(BeanMetaDataUtil.NOT_UPDATABLE)) {
						isUpdatable = false;
					}
					if (jdbcColumn.updatable() != isUpdatable) {
						validationError(exceptionPrefix.concat("Updatable indicator mismatch"));
					}
					// Validating annotated max-length against database metadata.
					if (jdbcColumn.maxLength() > 0 && (jdbcColumn.maxLength() != tableMetadataBean.getCharLength())) {
						validationError(exceptionPrefix.concat("Data length mismatch"));
					}
					log.debug("Table column ".concat(tableName).concat(".")
							.concat(columnName).concat(" successfully validated against bean specification."));
					javaAttributeName = beanMetaDataEntry.getKey();
					beanColumnValidated = true;
				}
			}
			if (beanColumnValidated) {
				beanMetadata.remove(javaAttributeName);
			} else {
				validationError("Table column ".concat(tableName).concat(".").concat(columnName)
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
			validationError(message.toString());
		}

		final List<String> beanPKColumnList = Arrays.asList(pBean.getPrimaryKeyColumnNames());
		final ArrayList<String> databasePKColumnList = new ArrayList<String>();
		for (final PrimaryKeyColumnBean primaryKeyColumnBean : allPKColumnNameList) {
			if (tableName.equals(primaryKeyColumnBean.getTableName())) {
				databasePKColumnList.add(primaryKeyColumnBean.getColumnName());
				if (!beanPKColumnList.contains(primaryKeyColumnBean.getColumnName())) {
					validationError("Primary key column ".concat(primaryKeyColumnBean.getColumnName())
							.concat(" of database table ".concat(tableName)
									.concat(" is not configured in Java Bean ").concat(javaBeanName)));
				}
			}
		}

		// We do not check, whether the primary keys configured with the Java Bean do exist in the database, too.
		// This is omitted, since generator configuration parameter "viewName2PrimaryKeyMapping" allows for a generic
		// primary key definition for views (which do not have an implicit primary key definition in the database).

		//if (!databasePKColumnList.containsAll(beanPKColumnList)) {
		//	String pkColumnNames = "";
		//	String delimiter = "";
		//	for (final String pkColumnName : beanPKColumnList) {
		//		pkColumnNames = pkColumnNames.concat(delimiter).concat(pkColumnName);
		//		delimiter = ",";
		//	}
		//	validationError("Primary key column(s) ".concat(pkColumnNames)
		//			.concat(" of Java Bean ".concat(javaBeanName)
		//					.concat(" is/are not defined for database table ").concat(tableName)));
		//}

		if (!pBean.getSequenceName().isEmpty()) {
			boolean sequenceFound = false;
			for (final SequenceBean sequenceName : sequenceDBNameList) {
				if (sequenceName.getName().equals(pBean.getSequenceName())) {
					sequenceFound = true;
				}
			}
			if (!sequenceFound) {
				validationError("Sequence ".concat(pBean.getSequenceName()).concat(" not found in database."));
			}
		}

		log.info("Validation of Java Bean ".concat(javaBeanName).concat(" successful."));
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
