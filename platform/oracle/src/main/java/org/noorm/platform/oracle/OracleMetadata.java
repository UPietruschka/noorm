package org.noorm.platform.oracle;

import org.noorm.jdbc.JDBCProcedureProcessor;
import org.noorm.jdbc.platform.*;
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
public class OracleMetadata implements IMetadata {

	private static final Logger log = LoggerFactory.getLogger(OracleMetadata.class);

    public static final String UPDATABLE = "YES";
    public static final String NULLABLE = "Y";
	private static OracleMetadata oracleMetadata = new OracleMetadata();

	protected OracleMetadata() {
	}

	public static OracleMetadata getInstance() {

		return oracleMetadata;
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

		final List<OracleTableMetadata> oracleTableMetadataList = findTableMetadata0();
		final Map<String, List<TableMetadata>> tableColumnMap = new HashMap<String, List<TableMetadata>>();
		String tableName = "";
		List<TableMetadata> tableMetadataList0 = null;
		for (OracleTableMetadata oracleTableMetadata : oracleTableMetadataList) {
			// Filter out duplicates
			if (!tableName.equals(oracleTableMetadata.getTableName())) {
				tableName = oracleTableMetadata.getTableName();
				log.debug("Collecting table metadata for table ".concat(tableName));
				tableMetadataList0 = new ArrayList<TableMetadata>();
				tableColumnMap.put(tableName, tableMetadataList0);
			}
            int decimalDigits = 0;
            if (oracleTableMetadata.getDataScale() != null) {
                decimalDigits = oracleTableMetadata.getDataScale().intValue();
            }
            final TableMetadata tableMetadata = new TableMetadata();
            tableMetadata.setTableName(oracleTableMetadata.getTableName());
            tableMetadata.setColumnName(oracleTableMetadata.getColumnName());
            tableMetadata.setJDBCType(convertOracleType2JDBCType(oracleTableMetadata.getDataType(), decimalDigits));
            tableMetadata.setColumnSize(oracleTableMetadata.getCharLength().intValue());
            tableMetadata.setDecimalDigits(decimalDigits);
            tableMetadata.setNullable(oracleTableMetadata.getNullable().equals(NULLABLE));
            tableMetadata.setUpdatable(oracleTableMetadata.getUpdatable().equals(UPDATABLE));
			tableMetadataList0.add(tableMetadata);
		}
		return tableColumnMap;
	}

	private List<OracleTableMetadata> findTableMetadata0() {

		final JDBCProcedureProcessor<OracleTableMetadata> statementProcessor = JDBCProcedureProcessor.getInstance();
		final Map<String, Object> filterParameters = new HashMap<String, Object>();
		return statementProcessor.getBeanListFromPLSQL
				("noorm_metadata.find_table_metadata", "p_table_metadata", filterParameters, OracleTableMetadata.class);
	}

	@Override
    public List<String> findPackageNames(final String pSearchRegex) {

		final JDBCProcedureProcessor<OracleName> statementProcessor = JDBCProcedureProcessor.getInstance();
		final Map<String, Object> filterParameters = new HashMap<String, Object>();
		filterParameters.put("p_search_regex", pSearchRegex);
		final List<OracleName> names = statementProcessor.getBeanListFromPLSQL
				("noorm_metadata.find_package_names", "p_package_names", filterParameters, OracleName.class);
        final List<String> nameList = new ArrayList<String>();
        for (final OracleName name : names) {
            nameList.add(name.getName());
        }
        return nameList;
	}

	@Override
    public List<String> findProcedureNames(final String pPackageName) {

		final JDBCProcedureProcessor<OracleName> statementProcessor = JDBCProcedureProcessor.getInstance();
		final Map<String, Object> filterParameters = new HashMap<String, Object>();
		filterParameters.put("p_package_name", pPackageName);
        final List<OracleName> names = statementProcessor.getBeanListFromPLSQL
				("noorm_metadata.find_procedure_names", "p_procedure_names", filterParameters, OracleName.class);
        final List<String> nameList = new ArrayList<String>();
        for (final OracleName name : names) {
            nameList.add(name.getName());
        }
        return nameList;
	}

	@Override
    public List<Sequence> findSequences() {

		final JDBCProcedureProcessor<OracleSequence> statementProcessor = JDBCProcedureProcessor.getInstance();
		final Map<String, Object> filterParameters = new HashMap<String, Object>();
		final List<OracleSequence> oracleSequenceList = statementProcessor.getBeanListFromPLSQL
				("noorm_metadata.find_sequence_names", "p_sequence_names", filterParameters, OracleSequence.class);
        final List<Sequence> sequenceList = new ArrayList<Sequence>();
        for (final OracleSequence oracleSequence : oracleSequenceList) {
            final Sequence sequence = new Sequence();
            sequence.setName(oracleSequence.getName());
            sequence.setIncrementBy(oracleSequence.getIncrementBy());
            sequenceList.add(sequence);
        }
        return sequenceList;
	}

	@Override
    public List<PrimaryKeyColumn> findPkColumns() {

		final JDBCProcedureProcessor<OraclePrimaryKeyColumn> statementProcessor = JDBCProcedureProcessor.getInstance();
		final Map<String, Object> filterParameters = new HashMap<String, Object>();
		final List<OraclePrimaryKeyColumn> pkColumnBeans = statementProcessor.getBeanListFromPLSQL
                ("noorm_metadata.find_pk_columns", "p_pk_columns", filterParameters, OraclePrimaryKeyColumn.class);
        final List<PrimaryKeyColumn> pkColumns = new ArrayList<PrimaryKeyColumn>();
        for (final OraclePrimaryKeyColumn pkColumnBean : pkColumnBeans) {
            final PrimaryKeyColumn pkColumn = new PrimaryKeyColumn();
            pkColumn.setTableName(pkColumnBean.getTableName());
            pkColumn.setColumnName(pkColumnBean.getColumnName());
            pkColumns.add(pkColumn);
        }
        return pkColumns;
	}

	@Override
    public List<Parameter> findProcedureParameters(final String pPackageName, final String pProcedureName) {

		final JDBCProcedureProcessor<OracleParameter> statementProcessor = JDBCProcedureProcessor.getInstance();
		final Map<String, Object> filterParameters = new HashMap<String, Object>();
		filterParameters.put("p_package_name", pPackageName);
		filterParameters.put("p_procedure_name", pProcedureName);
		final List<OracleParameter> oracleParameterList = statementProcessor.getBeanListFromPLSQL
                ("noorm_metadata.find_procedure_parameters", "p_parameters", filterParameters, OracleParameter.class);
        final List<Parameter> parameters = new ArrayList<Parameter>();
        for (final OracleParameter oracleParameter : oracleParameterList) {
            final Parameter parameter = new Parameter();
            parameter.setName(oracleParameter.getName());
            parameter.setDirection(oracleParameter.getDirection());
            parameter.setTypeName(oracleParameter.getTypeName());
            parameter.setJDBCType(convertOracleType2JDBCType(oracleParameter.getDataType(), 0));
            parameters.add(parameter);
        }
        return parameters;
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

		final List<OracleTableMetadata> recordMetadataBeanList = findRecordMetadata0();
		final Map<String, List<TableMetadata>> recordColumnMap = new HashMap<String, List<TableMetadata>>();
		String recordName = "";
		List<TableMetadata> recordMetadataList0 = null;
		for (OracleTableMetadata recordMetadataBean : recordMetadataBeanList) {
			// Filter out duplicates
			if (!recordName.equals(recordMetadataBean.getTableName())) {
				recordName = recordMetadataBean.getTableName();
				log.debug("Collecting record metadata for record ".concat(recordName));
				recordMetadataList0 = new ArrayList<TableMetadata>();
				recordColumnMap.put(recordName, recordMetadataList0);
			}
            int decimalDigits = 0;
            if (recordMetadataBean.getDataScale() != null) {
                decimalDigits = recordMetadataBean.getDataScale().intValue();
            }
            final TableMetadata recordMetadata = new TableMetadata();
            recordMetadata.setTableName(recordMetadataBean.getTableName());
            recordMetadata.setColumnName(recordMetadataBean.getColumnName());
            recordMetadata.setJDBCType(convertOracleType2JDBCType(recordMetadataBean.getDataType(), decimalDigits));
            recordMetadata.setColumnSize(recordMetadataBean.getCharLength().intValue());
            recordMetadata.setDecimalDigits(decimalDigits);
            recordMetadata.setNullable(recordMetadataBean.getNullable().equals(NULLABLE));
            recordMetadata.setUpdatable(recordMetadataBean.getUpdatable().equals(UPDATABLE));
			recordMetadataList0.add(recordMetadata);
		}
		return recordColumnMap;
	}

	private List<OracleTableMetadata> findRecordMetadata0() {

		final JDBCProcedureProcessor<OracleTableMetadata> statementProcessor = JDBCProcedureProcessor.getInstance();
		final Map<String, Object> filterParameters = new HashMap<String, Object>();
		return statementProcessor.getBeanListFromPLSQL
				("noorm_metadata.find_record_metadata", "p_record_metadata", filterParameters, OracleTableMetadata.class);
	}

    private JDBCType convertOracleType2JDBCType(final String pOracleTypeName, final int pDecimalDigits) {

        JDBCType jdbcType = null;
        if (pOracleTypeName.endsWith("RAW")) {
            jdbcType = JDBCType.BINARY;
        }
        if (pOracleTypeName.contains("XMLTYPE")) {
            jdbcType = JDBCType.SQLXML;
        }
        if (pOracleTypeName.equals("NUMBER")) {
            if (pDecimalDigits > 0) {
                jdbcType = JDBCType.DOUBLE;
            } else {
                jdbcType = JDBCType.NUMERIC;
            }
        }
        if (pOracleTypeName.equals("BINARY_FLOAT")) {
            jdbcType = JDBCType.FLOAT;
        }
        if (pOracleTypeName.equals("BINARY_DOUBLE")) {
            jdbcType = JDBCType.DOUBLE;
        }
        if (pOracleTypeName.equals("FLOAT")) {
            jdbcType = JDBCType.DOUBLE;
        }
        if (pOracleTypeName.startsWith("TIMESTAMP")) {
            jdbcType = JDBCType.TIMESTAMP;
        }
        if (pOracleTypeName.equals("REF CURSOR")) {
            jdbcType = JDBCType.REF_CURSOR;
        }
        if (jdbcType == null) {
            try {
                jdbcType = JDBCType.valueOf(pOracleTypeName);
            } catch (IllegalArgumentException e) {
                jdbcType = JDBCType.VARCHAR;
            }
        }
        return jdbcType;
    }
}
