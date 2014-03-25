package org.noorm.platform.oracle;

import org.noorm.jdbc.JDBCProcedureProcessor;
import org.noorm.metadata.beans.NameBean;
import org.noorm.metadata.beans.ParameterBean;
import org.noorm.metadata.beans.PrimaryKeyColumnBean;
import org.noorm.metadata.beans.SequenceBean;
import org.noorm.platform.IMetadata;
import org.noorm.platform.TableMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 19.04.11
 *         Time: 14:48
 */
public class MetadataService implements IMetadata {

	private static final Logger log = LoggerFactory.getLogger(MetadataService.class);

    public static final String UPDATABLE = "YES";
    public static final String NULLABLE = "Y";
	private static MetadataService metadataService = new MetadataService();

	protected MetadataService() {
	}

	public static MetadataService getInstance() {

		return metadataService;
	}

	@Override
    public String getVersion() {

		final JDBCProcedureProcessor<String> statementProcessor = JDBCProcedureProcessor.getInstance();
		final Map<String, Object> filterParameters = new HashMap<String, Object>();
		return statementProcessor.callPLSQL
				("noorm_metadata.get_version", "p_version", filterParameters, String.class);

	}

	@Override
    public Map<String, List<TableMetadata>> findTableMetadata() {

		final List<TableMetadataBean> tableMetadataBeanList = findTableMetadata0();
		final Map<String, List<TableMetadata>> tableColumnMap = new HashMap<String, List<TableMetadata>>();
		String tableName = "";
		List<TableMetadata> tableMetadataList0 = null;
		for (TableMetadataBean tableMetadataBean : tableMetadataBeanList) {
			// Filter out duplicates
			if (!tableName.equals(tableMetadataBean.getTableName())) {
				tableName = tableMetadataBean.getTableName();
				log.debug("Collecting table metadata for table ".concat(tableName));
				tableMetadataList0 = new ArrayList<TableMetadata>();
				tableColumnMap.put(tableName, tableMetadataList0);
			}
            final TableMetadata tableMetadata = new TableMetadata();
            tableMetadata.setTableName(tableMetadataBean.getTableName());
            tableMetadata.setColumnName(tableMetadataBean.getColumnName());
            tableMetadata.setTypeName(tableMetadataBean.getDataType());
            tableMetadata.setColumnSize(tableMetadataBean.getCharLength().intValue());
            if (tableMetadataBean.getDataScale() != null) {
                tableMetadata.setDecimalDigits(tableMetadataBean.getDataScale().intValue());
            }
            tableMetadata.setNullable(tableMetadataBean.getNullable().equals(NULLABLE));
            tableMetadata.setUpdatable(tableMetadataBean.getUpdatable().equals(UPDATABLE));
			tableMetadataList0.add(tableMetadata);
		}
		return tableColumnMap;
	}

	private List<TableMetadataBean> findTableMetadata0() {

		final JDBCProcedureProcessor<TableMetadataBean> statementProcessor = JDBCProcedureProcessor.getInstance();
		final Map<String, Object> filterParameters = new HashMap<String, Object>();
		return statementProcessor.getBeanListFromPLSQL
				("noorm_metadata.find_table_metadata", "p_table_metadata", filterParameters, TableMetadataBean.class);
	}

	@Override
    public List<NameBean> findPackageNames(final String pSearchRegex) {

		final JDBCProcedureProcessor<NameBean> statementProcessor = JDBCProcedureProcessor.getInstance();
		final Map<String, Object> filterParameters = new HashMap<String, Object>();
		filterParameters.put("p_search_regex", pSearchRegex);
		return statementProcessor.getBeanListFromPLSQL
				("noorm_metadata.find_package_names", "p_package_names", filterParameters, NameBean.class);
	}

	@Override
    public List<NameBean> findProcedureNames(final String pPackageName) {

		final JDBCProcedureProcessor<NameBean> statementProcessor = JDBCProcedureProcessor.getInstance();
		final Map<String, Object> filterParameters = new HashMap<String, Object>();
		filterParameters.put("p_package_name", pPackageName);
		return statementProcessor.getBeanListFromPLSQL
				("noorm_metadata.find_procedure_names", "p_procedure_names", filterParameters, NameBean.class);
	}

	@Override
    public List<SequenceBean> findSequenceNames() {

		final JDBCProcedureProcessor<SequenceBean> statementProcessor = JDBCProcedureProcessor.getInstance();
		final Map<String, Object> filterParameters = new HashMap<String, Object>();
		return statementProcessor.getBeanListFromPLSQL
				("noorm_metadata.find_sequence_names", "p_sequence_names", filterParameters, SequenceBean.class);
	}

	@Override
    public List<PrimaryKeyColumnBean> findPkColumns() {

		final JDBCProcedureProcessor<PrimaryKeyColumnBean> statementProcessor = JDBCProcedureProcessor.getInstance();
		final Map<String, Object> filterParameters = new HashMap<String, Object>();
		return statementProcessor.getBeanListFromPLSQL
				("noorm_metadata.find_pk_columns", "p_pk_columns", filterParameters, PrimaryKeyColumnBean.class);
	}

	@Override
    public List<ParameterBean> findProcedureParameters(final String pPackageName, final String pProcedureName) {

		final JDBCProcedureProcessor<ParameterBean> statementProcessor = JDBCProcedureProcessor.getInstance();
		final Map<String, Object> filterParameters = new HashMap<String, Object>();
		filterParameters.put("p_package_name", pPackageName);
		filterParameters.put("p_procedure_name", pProcedureName);
		return statementProcessor.getBeanListFromPLSQL
				("noorm_metadata.find_procedure_parameters", "p_parameters", filterParameters, ParameterBean.class);
	}

	@Override
    public Integer getPackageHashValue(final String pPackageName) {

		final JDBCProcedureProcessor<Integer> statementProcessor = JDBCProcedureProcessor.getInstance();
		final Map<String, Object> filterParameters = new HashMap<String, Object>();
		filterParameters.put("p_package_name", pPackageName);
		return statementProcessor.callPLSQL
				("noorm_metadata.get_package_hash_value", "p_code_hash_value", filterParameters, Integer.class);
	}

	@Override
    public String getParameterRowtype(final String pPackageName,
                                      final String pProcedureName,
                                      final String pParameterName) {

		final JDBCProcedureProcessor<String> statementProcessor = JDBCProcedureProcessor.getInstance();
		final Map<String, Object> filterParameters = new HashMap<String, Object>();
		filterParameters.put("p_package_name", pPackageName);
		filterParameters.put("p_procedure_name", pProcedureName);
		filterParameters.put("p_parameter_name", pParameterName);
		return statementProcessor.callPLSQL
				("noorm_metadata.get_parameter_rowtype", "p_rowtype_name", filterParameters, String.class);
	}

	@Override
    public Map<String, List<TableMetadata>> findRecordMetadata() {

		final List<TableMetadataBean> recordMetadataBeanList = findRecordMetadata0();
		final Map<String, List<TableMetadata>> recordColumnMap = new HashMap<String, List<TableMetadata>>();
		String recordName = "";
		List<TableMetadata> recordMetadataList0 = null;
		for (TableMetadataBean recordMetadataBean : recordMetadataBeanList) {
			// Filter out duplicates
			if (!recordName.equals(recordMetadataBean.getTableName())) {
				recordName = recordMetadataBean.getTableName();
				log.debug("Collecting record metadata for record ".concat(recordName));
				recordMetadataList0 = new ArrayList<TableMetadata>();
				recordColumnMap.put(recordName, recordMetadataList0);
			}
            final TableMetadata recordMetadata = new TableMetadata();
            recordMetadata.setTableName(recordMetadataBean.getTableName());
            recordMetadata.setColumnName(recordMetadataBean.getColumnName());
            recordMetadata.setTypeName(recordMetadataBean.getDataType());
            recordMetadata.setColumnSize(recordMetadataBean.getCharLength().intValue());
            if (recordMetadataBean.getDataScale() != null) {
                recordMetadata.setDecimalDigits(recordMetadataBean.getDataScale().intValue());
            }
            recordMetadata.setNullable(recordMetadataBean.getNullable().equals(NULLABLE));
            recordMetadata.setUpdatable(recordMetadataBean.getUpdatable().equals(UPDATABLE));
			recordMetadataList0.add(recordMetadata);
		}
		return recordColumnMap;
	}

	private List<TableMetadataBean> findRecordMetadata0() {

		final JDBCProcedureProcessor<TableMetadataBean> statementProcessor = JDBCProcedureProcessor.getInstance();
		final Map<String, Object> filterParameters = new HashMap<String, Object>();
		return statementProcessor.getBeanListFromPLSQL
				("noorm_metadata.find_record_metadata", "p_record_metadata", filterParameters, TableMetadataBean.class);
	}
}
