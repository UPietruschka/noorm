package org.noorm.platform.oracle;

import org.noorm.jdbc.JDBCMetadata;
import org.noorm.jdbc.JDBCProcedureProcessor;
import org.noorm.jdbc.JDBCQueryProcessor;
import org.noorm.jdbc.platform.Parameter;
import org.noorm.jdbc.platform.PrimaryKeyColumn;
import org.noorm.jdbc.platform.Sequence;
import org.noorm.jdbc.platform.TableMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.JDBCType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 19.04.11
 *         Time: 14:48
 */
public class OracleMetadata extends JDBCMetadata {

	private static final Logger log = LoggerFactory.getLogger(OracleMetadata.class);

    public static final String NULLABLE = "Y";
	private static OracleMetadata oracleMetadata = new OracleMetadata();
    private JDBCQueryProcessor queryProcessor = JDBCQueryProcessor.getInstance();

	protected OracleMetadata() {
	}

	public static OracleMetadata getInstance() {

		return oracleMetadata;
	}

    /**
     * Returns the version of the proprietary metadata retrieval implementation.
     *
     * @return the version
     */
	@Override
    public String getVersion() {

		final JDBCProcedureProcessor<String> statementProcessor = JDBCProcedureProcessor.getInstance();
		final Map<String, Object> filterParameters = new HashMap<>();
		return statementProcessor.callProcedure
                ("noorm_metadata.get_version", "p_version", filterParameters, String.class);

	}

    private static final String UPDATABLE_COLUMN_QUERY =
            "SELECT column_name, updatable FROM all_updatable_columns " +
                    "WHERE  owner = p_owner AND table_name = p_table_name";

    /**
     * Resolves the JDBC datatype on basis of the platform specific type information found in JDBC metadata
     *
     * @param pDataType      the numeric data type code
     * @param pDataTypeName  the data type name
     * @param pDecimalDigits the number of decimal digits for non-integer like numeric types
     * @return the mapped JDBC standard type
     */
    @Override
    public JDBCType findJDBCType(int pDataType, String pDataTypeName, int pDecimalDigits) {

        return convertOracleType2JDBCType(pDataTypeName, pDecimalDigits);
    }

    /**
     * Returns the list of table/column metadata accessible for the authenticated database user.
     *
     * @param pSchemaPattern a regular expression narrowing the set of schemas subject to metadata retrieval
     * @param pTableNamePattern a regular expression narrowing the set of tables subject to metadata retrieval
     * @return the requested
     */
	//@Override
    public Map<String, List<TableMetadata>> findTableMetadataz(final String pSchemaPattern,
                                                               final String pTableNamePattern) {

		final List<OracleTableMetadata> oracleTableMetadataList = findTableMetadata0();
		final Map<String, List<TableMetadata>> tableColumnMap = new HashMap<>();
		String tableName = "";
		List<TableMetadata> tableMetadataList0 = null;
		for (OracleTableMetadata oracleTableMetadata : oracleTableMetadataList) {
            // Filter out duplicates
			if (!tableName.equals(oracleTableMetadata.getTableName())) {
				tableName = oracleTableMetadata.getTableName();
                if (pTableNamePattern != null && !tableName.matches(pTableNamePattern)) {
                    tableName = "";
                    continue;
                }
				log.debug("Collecting table metadata for table ".concat(tableName));
				tableMetadataList0 = new ArrayList<>();
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
			tableMetadataList0.add(tableMetadata);
		}
		return tableColumnMap;
	}

	private List<OracleTableMetadata> findTableMetadata0() {

		final JDBCProcedureProcessor<OracleTableMetadata> statementProcessor = JDBCProcedureProcessor.getInstance();
		final Map<String, Object> filterParameters = new HashMap<>();
		return statementProcessor.getBeanListFromProcedure
                ("noorm_metadata.find_table_metadata", "p_table_metadata", filterParameters, OracleTableMetadata.class);
	}

    /**
     * Returns the list of packages of stored procedures subject to Java code generation.
     *
     * @param pSearchRegex a regular expression to filter the list of packages
     * @return the list of packages
     */
	@Override
    public List<String> findPackageNames(final String pSearchRegex) {

		final JDBCProcedureProcessor<OracleName> statementProcessor = JDBCProcedureProcessor.getInstance();
		final Map<String, Object> filterParameters = new HashMap<>();
		filterParameters.put("p_search_regex", pSearchRegex);
		final List<OracleName> names = statementProcessor.getBeanListFromProcedure
                ("noorm_metadata.find_package_names", "p_package_names", filterParameters, OracleName.class);
        final List<String> nameList = new ArrayList<String>();
        for (final OracleName name : names) {
            nameList.add(name.getName());
        }
        return nameList;
	}

    /**
     * Returns the list of procedures contained in the given package.
     *
     * @param pPackageName the package name
     * @return the list of procedures
     */
	@Override
    public List<String> findProcedureNames(final String pPackageName) {

		final JDBCProcedureProcessor<OracleName> statementProcessor = JDBCProcedureProcessor.getInstance();
		final Map<String, Object> filterParameters = new HashMap<>();
		filterParameters.put("p_package_name", pPackageName);
        final List<OracleName> names = statementProcessor.getBeanListFromProcedure
                ("noorm_metadata.find_procedure_names", "p_procedure_names", filterParameters, OracleName.class);
        final List<String> nameList = new ArrayList<String>();
        for (final OracleName name : names) {
            nameList.add(name.getName());
        }
        return nameList;
	}

	private static final String SEQUENCE_QUERY = "SELECT SEQUENCE_NAME, INCREMENT_BY FROM USER_SEQUENCES";

    /**
     * Returns all accessible sequences.
     *
     * @return the list of sequence names
     */
	@Override
    public List<Sequence> findSequences() {

        final List<Map<String, Object>> seqResults = queryProcessor.executeGenericSelect(SEQUENCE_QUERY);
        final List<Sequence> sequences = new ArrayList<>();
        for (final Map<String, Object> seqResult : seqResults) {
            final Sequence sequence = new Sequence();
            sequence.setName((String) seqResult.get("SEQUENCE_NAME"));
            sequence.setIncrementBy(((BigDecimal) seqResult.get("INCREMENT_BY")).intValue());
            sequences.add(sequence);
            log.debug("Found sequence metadata for sequence name/increment by : "
                    + sequence.getName() + "/" + sequence.getIncrementBy());
        }
        return sequences;
	}

	private static final String SYNONYM_PK_COLUMN_QUERY =
            "SELECT us.synonym_name table_name, " +
            "       cc.column_name " +
            "FROM   all_constraints ac, " +
            "       all_cons_columns cc, " +
            "       user_synonyms us " +
            "WHERE  ac.table_name      = cc.table_name " +
            "AND    ac.constraint_name = cc.constraint_name " +
            "AND    ac.owner           = cc.owner " +
            "AND    ac.table_name      = us.table_name " +
            "AND    ac.owner           = us.table_owner " +
            "AND    us.synonym_name    = p_table_name " +
            "AND    ac.constraint_type = 'P'";

    /**
     * Returns all primary key columns.
     *
     * @param pTableName the table name
     * @return the list of primary key columns for the given table
     */
	@Override
    public List<PrimaryKeyColumn> findPkColumns(final String pTableName) {

	    // We get the PK columns within the schema using the JDBC metadata
        final List<PrimaryKeyColumn> primaryKeyColumnList = super.findPkColumns(pTableName);
        // PK columns for tables referenced by synonyms must be retrieved directly from the DD
        final String synonymPKColumnQuery =
                SYNONYM_PK_COLUMN_QUERY.replace("p_table_name", "'" + pTableName + "'");
        final List<Map<String, Object>> pkSynonymResults = queryProcessor.executeGenericSelect(synonymPKColumnQuery);
        for (final Map<String, Object> pkSynonymResult : pkSynonymResults) {
            final PrimaryKeyColumn pkColumn = new PrimaryKeyColumn();
            pkColumn.setTableName((String) pkSynonymResult.get("TABLE_NAME"));
            pkColumn.setColumnName((String) pkSynonymResult.get("COLUMN_NAME"));
            primaryKeyColumnList.add(pkColumn);
        }
        return primaryKeyColumnList;
	}

    /**
     * Returns the parameters for a given stored procedure.
     *
     * @param pPackageName the package name
     * @param pProcedureName the procedure name
     * @return the list of parameters for the given procedure
     */
	@Override
    public List<Parameter> findProcedureParameters(final String pPackageName, final String pProcedureName) {

		final JDBCProcedureProcessor<OracleParameter> statementProcessor = JDBCProcedureProcessor.getInstance();
		final Map<String, Object> filterParameters = new HashMap<>();
		filterParameters.put("p_package_name", pPackageName);
		filterParameters.put("p_procedure_name", pProcedureName);
		final List<OracleParameter> oracleParameterList = statementProcessor.getBeanListFromProcedure
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

    /**
     * Returns the hash value for the source code of a given stored procedure package.
     *
     * @param pPackageName the package name
     * @return the has value
     */
	@Override
    public Integer getPackageHashValue(final String pPackageName) {

		final JDBCProcedureProcessor<Integer> statementProcessor = JDBCProcedureProcessor.getInstance();
		final Map<String, Object> filterParameters = new HashMap<>();
		filterParameters.put("p_package_name", pPackageName);
		return statementProcessor.callProcedure
                ("noorm_metadata.get_package_hash_value", "p_code_hash_value", filterParameters, Integer.class);
	}

    /**
     * Returns the type for a given parameter of a stored procedure.
     *
     * @param pPackageName the package name
     * @param pProcedureName the procedure name
     * @param pParameterName the parameter name
     * @return the type
     */
	@Override
    public String getParameterRowtype(final String pPackageName,
                                      final String pProcedureName,
                                      final String pParameterName) {

		final JDBCProcedureProcessor<String> statementProcessor = JDBCProcedureProcessor.getInstance();
		final Map<String, Object> filterParameters = new HashMap<>();
		filterParameters.put("p_package_name", pPackageName);
		filterParameters.put("p_procedure_name", pProcedureName);
		filterParameters.put("p_parameter_name", pParameterName);
		return statementProcessor.callProcedure
                ("noorm_metadata.get_parameter_rowtype", "p_rowtype_name", filterParameters, String.class);
	}

    /**
     * Returns metadata for a database type definition.
     *
     * @return the list of record metadata
     */
	@Override
    public Map<String, List<TableMetadata>> findRecordMetadata() {

		final List<OracleTableMetadata> recordMetadataBeanList = findRecordMetadata0();
		final Map<String, List<TableMetadata>> recordColumnMap = new HashMap<>();
		String recordName = "";
		List<TableMetadata> recordMetadataList0 = null;
		for (OracleTableMetadata recordMetadataBean : recordMetadataBeanList) {
			// Filter out duplicates
			if (!recordName.equals(recordMetadataBean.getTableName())) {
				recordName = recordMetadataBean.getTableName();
				log.debug("Collecting record metadata for record ".concat(recordName));
				recordMetadataList0 = new ArrayList<>();
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
			recordMetadataList0.add(recordMetadata);
		}
		return recordColumnMap;
	}

	private List<OracleTableMetadata> findRecordMetadata0() {

		final JDBCProcedureProcessor<OracleTableMetadata> statementProcessor = JDBCProcedureProcessor.getInstance();
		final Map<String, Object> filterParameters = new HashMap<>();
		return statementProcessor.getBeanListFromProcedure
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
