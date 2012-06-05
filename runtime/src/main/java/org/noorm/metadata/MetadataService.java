package org.noorm.metadata;

import org.noorm.metadata.beans.NameBean;
import org.noorm.metadata.beans.ParameterBean;
import org.noorm.metadata.beans.PrimaryKeyColumnBean;
import org.noorm.metadata.beans.TableMetadataBean;
import org.noorm.jdbc.JDBCStatementProcessor;
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
public class MetadataService {

	private static final Logger log = LoggerFactory.getLogger(MetadataService.class);

	private static MetadataService metadataService = new MetadataService();

	protected MetadataService() {
	}

	public static MetadataService getInstance() {

		return metadataService;
	}

	public String getVersion() {

		final JDBCStatementProcessor<String> statementProcessor = JDBCStatementProcessor.getInstance();
		final Map<String, Object> filterParameters = new HashMap<String, Object>();
		return statementProcessor.callPLSQL
				("noorm_metadata.get_version", "p_version", filterParameters, String.class);

	}

	public Map<String, List<TableMetadataBean>> findTableMetadata() {

		final List<TableMetadataBean> tableMetadataBeanList = findTableMetadata0();
		final Map<String, List<TableMetadataBean>> tableColumnMap = new HashMap<String, List<TableMetadataBean>>();
		String tableName = "";
		List<TableMetadataBean> tableMetadataBeanList0 = null;
		for (TableMetadataBean tableMetadataBean : tableMetadataBeanList) {
			// Filter out duplicates
			if (!tableName.equals(tableMetadataBean.getTableName())) {
				tableName = tableMetadataBean.getTableName();
				log.debug("Collecting table metadata for table ".concat(tableName));
				tableMetadataBeanList0 = new ArrayList<TableMetadataBean>();
				tableColumnMap.put(tableName, tableMetadataBeanList0);
			}
			tableMetadataBeanList0.add(tableMetadataBean);
		}
		return tableColumnMap;
	}

	private List<TableMetadataBean> findTableMetadata0() {

		final JDBCStatementProcessor<TableMetadataBean> statementProcessor = JDBCStatementProcessor.getInstance();
		final Map<String, Object> filterParameters = new HashMap<String, Object>();
		return statementProcessor.getBeanListFromPLSQL
				("noorm_metadata.find_table_metadata", "p_table_metadata", filterParameters, TableMetadataBean.class);
	}

	public List<NameBean> findPackageNames(final String pSearchRegex) {

		final JDBCStatementProcessor<NameBean> statementProcessor = JDBCStatementProcessor.getInstance();
		final Map<String, Object> filterParameters = new HashMap<String, Object>();
		filterParameters.put("p_search_regex", pSearchRegex);
		return statementProcessor.getBeanListFromPLSQL
				("noorm_metadata.find_package_names", "p_package_names", filterParameters, NameBean.class);
	}

	public List<NameBean> findProcedureNames(final String pPackageName) {

		final JDBCStatementProcessor<NameBean> statementProcessor = JDBCStatementProcessor.getInstance();
		final Map<String, Object> filterParameters = new HashMap<String, Object>();
		filterParameters.put("p_package_name", pPackageName);
		return statementProcessor.getBeanListFromPLSQL
				("noorm_metadata.find_procedure_names", "p_procedure_names", filterParameters, NameBean.class);
	}

	public List<NameBean> findSequenceNames() {

		final JDBCStatementProcessor<NameBean> statementProcessor = JDBCStatementProcessor.getInstance();
		final Map<String, Object> filterParameters = new HashMap<String, Object>();
		return statementProcessor.getBeanListFromPLSQL
				("noorm_metadata.find_sequence_names", "p_sequence_names", filterParameters, NameBean.class);
	}

	public List<PrimaryKeyColumnBean> findPkColumns() {

		final JDBCStatementProcessor<PrimaryKeyColumnBean> statementProcessor = JDBCStatementProcessor.getInstance();
		final Map<String, Object> filterParameters = new HashMap<String, Object>();
		return statementProcessor.getBeanListFromPLSQL
				("noorm_metadata.find_pk_columns", "p_pk_columns", filterParameters, PrimaryKeyColumnBean.class);
	}

	public List<ParameterBean> findProcedureParameters(final String pPackageName, final String pProcedureName) {

		final JDBCStatementProcessor<ParameterBean> statementProcessor = JDBCStatementProcessor.getInstance();
		final Map<String, Object> filterParameters = new HashMap<String, Object>();
		filterParameters.put("p_package_name", pPackageName);
		filterParameters.put("p_procedure_name", pProcedureName);
		return statementProcessor.getBeanListFromPLSQL
				("noorm_metadata.find_procedure_parameters", "p_parameters", filterParameters, ParameterBean.class);
	}

	public Integer getPackageHashValue(final String pPackageName) {

		final JDBCStatementProcessor<Integer> statementProcessor = JDBCStatementProcessor.getInstance();
		final Map<String, Object> filterParameters = new HashMap<String, Object>();
		filterParameters.put("p_package_name", pPackageName);
		return statementProcessor.callPLSQL
				("noorm_metadata.get_package_hash_value", "p_code_hash_value", filterParameters, Integer.class);
	}

	public String getParameterRowtype(final String pPackageName,
									  final String pProcedureName,
									  final String pParameterName) {

		final JDBCStatementProcessor<String> statementProcessor = JDBCStatementProcessor.getInstance();
		final Map<String, Object> filterParameters = new HashMap<String, Object>();
		filterParameters.put("p_package_name", pPackageName);
		filterParameters.put("p_procedure_name", pProcedureName);
		filterParameters.put("p_parameter_name", pParameterName);
		return statementProcessor.callPLSQL
				("noorm_metadata.get_parameter_rowtype", "p_rowtype_name", filterParameters, String.class);
	}

	public Map<String, List<TableMetadataBean>> findRecordMetadata() {

		final List<TableMetadataBean> recordMetadataBeanList = findRecordMetadata0();
		final Map<String, List<TableMetadataBean>> recordColumnMap = new HashMap<String, List<TableMetadataBean>>();
		String recordName = "";
		List<TableMetadataBean> recordMetadataBeanList0 = null;
		for (TableMetadataBean recordMetadataBean : recordMetadataBeanList) {
			// Filter out duplicates
			if (!recordName.equals(recordMetadataBean.getTableName())) {
				recordName = recordMetadataBean.getTableName();
				log.debug("Collecting record metadata for record ".concat(recordName));
				recordMetadataBeanList0 = new ArrayList<TableMetadataBean>();
				recordColumnMap.put(recordName, recordMetadataBeanList0);
			}
			recordMetadataBeanList0.add(recordMetadataBean);
		}
		return recordColumnMap;
	}

	private List<TableMetadataBean> findRecordMetadata0() {

		final JDBCStatementProcessor<TableMetadataBean> statementProcessor = JDBCStatementProcessor.getInstance();
		final Map<String, Object> filterParameters = new HashMap<String, Object>();
		return statementProcessor.getBeanListFromPLSQL
				("noorm_metadata.find_record_metadata", "p_record_metadata", filterParameters, TableMetadataBean.class);
	}
}
