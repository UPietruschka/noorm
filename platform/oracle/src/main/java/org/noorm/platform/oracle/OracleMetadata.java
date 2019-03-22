package org.noorm.platform.oracle;

import org.noorm.jdbc.*;
import org.noorm.jdbc.platform.Parameter;
import org.noorm.jdbc.platform.PrimaryKeyColumn;
import org.noorm.jdbc.platform.Sequence;
import org.noorm.jdbc.platform.TableMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.DatatypeConverter;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.sql.JDBCType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    public static class ObjectName {

        @JDBCColumn(name="OBJECT_NAME", updatable=false)
        private String name;
        public String getName() {
            return name;
        }
        public void setName(final String pName) {
            name = pName;
        }
    }

    /**
     * Returns the list of packages of stored procedures subject to Java code generation.
     *
     * @param pSearchRegex a regular expression to filter the list of packages
     * @return the list of packages
     */
	@Override
    public List<String> findPackageNames(final String pSearchRegex) {

        final Map<QueryColumn, Object> parameters = new HashMap<QueryColumn, Object>();
        parameters.put(new QueryColumn("object_type", new Operator(Operator.Name.EQUAL_TO), null), "PACKAGE");
        parameters.put(new QueryColumn("object_name", new Operator(Operator.Name.CUSTOM), "REGEXP_LIKE(OBJECT_NAME, ?)"), pSearchRegex);
        final JDBCQueryProcessor<ObjectName> queryProcessor = JDBCQueryProcessor.getInstance();
        final List<ObjectName> results = queryProcessor.getBeanListFromSQL("USER_OBJECTS", parameters, ObjectName.class, false);
        final List<String> nameList = new ArrayList<String>();
        for (final ObjectName name : results) {
            nameList.add(name.getName());
        }
        return nameList;
	}

    public static class ProcedureName {

        @JDBCColumn(name="PROCEDURE_NAME", updatable=false)
        private String name;
        public String getName() {
            return name;
        }
        public void setName(final String pName) {
            name = pName;
        }
    }

    /**
     * Returns the list of procedures contained in the given package.
     *
     * @param pPackageName the package name
     * @return the list of procedures
     */
	@Override
    public List<String> findProcedureNames(final String pPackageName) {

        final Map<QueryColumn, Object> parameters = new HashMap<QueryColumn, Object>();
        parameters.put(new QueryColumn("object_name", new Operator(Operator.Name.EQUAL_TO), null), pPackageName);
        parameters.put(new QueryColumn("procedure_name", new Operator(Operator.Name.IS_NOT_NULL), null), null);
        final JDBCQueryProcessor<ProcedureName> queryProcessor = JDBCQueryProcessor.getInstance();
        final List<ProcedureName> results = queryProcessor.getBeanListFromSQL("USER_PROCEDURES", parameters, ProcedureName.class, false);
        final List<String> nameList = new ArrayList<String>();
        for (final ProcedureName name : results) {
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

    private static final String PROCEDURE_PARAMETER_QUERY =
            "SELECT argument_name name, " +
            "       data_type, " +
            "       type_name, " +
            "       in_out direction " +
            "FROM   user_arguments " +
            "WHERE  object_name = p_procedure_name " +
            "AND    package_name = p_package_name " +
            "AND    data_level = 0 " +
            "AND    argument_name IS NOT NULL " +
            "ORDER  BY sequence";

    /**
     * Unfortunately, PL/SQL record definitions are only subject to the PL/SQL compiler interpretation
     * and not available in the Oracle data dictionary. For reverse engineering PL/SQL procedure calls
     * based on PL/SQL records (which include implicit record definitions using %ROWTYPE) the list of
     * fields in the record can be retrieved using data dictionary view USER_ARGUMENTS, but without a
     * reference to the declaring row-type, if any.
     * For this reason, evaluating the referenced row-type is done by comparing the given list with all
     * explicitly declared row-types, i.e. tables and views. Currently, this limits the supported record
     * definitions to row-types declared by tables and views.
     *
     * @param pPackageName the package name
     * @param pProcedureName the procedure name
     * @return the list of parameters for the given procedure
     */
	@Override
    public List<Parameter> findProcedureParameters(final String pPackageName, final String pProcedureName) {

	    String query = PROCEDURE_PARAMETER_QUERY;
        query = query.replace("p_procedure_name", "'" + pProcedureName + "'");
        query = query.replace("p_package_name", "'" + pPackageName + "'");
        final List<Parameter> parameterList = new ArrayList<>();
        final List<Map<String, Object>> paramResults = queryProcessor.executeGenericSelect(query);
        for (final Map<String, Object> paramResult : paramResults) {
            final Parameter parameter = new Parameter();
            parameter.setName((String) paramResult.get("NAME"));
            parameter.setDirection((String) paramResult.get("DIRECTION"));
            parameter.setTypeName((String) paramResult.get("TYPE_NAME"));
            parameter.setJDBCType(convertOracleType2JDBCType((String) paramResult.get("DATA_TYPE"), 0));
            parameterList.add(parameter);
        }
        return parameterList;
	}

    /**
     * Returns the hash value for the source code of a given stored procedure package.
     *
     * @param pPackageName the package name
     * @return the has value
     */
	@Override
    public String getPackageHashValue(final String pPackageName) {

        final String sourceText = getUserSource(pPackageName);
        try {
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            final byte[] packageHash = digest.digest(sourceText.getBytes("UTF-8"));
            return DatatypeConverter.printBase64Binary(packageHash);
        } catch (Exception e) {
            throw new DataAccessException(DataAccessException.Type.INITIALIZATION_FAILURE, e);
        }
	}

    /**
     * Returns the type for a given parameter of a stored procedure.
     * PL/SQL records declared on basis of a rowtype are hard to detect, since no data dictionary view provides
     * the required information, thus the only way to get the required information is parsing the source code.
     * For explicitly declared record types things are easier, since the internal name of the record type is
     * available through data dictionary view USER_ARGUMENTS.
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

	    log.debug("Collecting procedure return parameter type for [package/procedure/parameter] "
                + pPackageName + "/" + pProcedureName + "/" + pParameterName);
        String query = PARAMETER_ROWTYPE_QUERY;
        query = query.replace("p_package_name", "'" + pPackageName + "'");
        query = query.replace("p_procedure_name", "'" + pProcedureName + "'");
        final List<Map<String, Object>> parameters = queryProcessor.executeGenericSelect(query);
        if (parameters.size() != 0) {
            final Map<String, Object> parameterRecord = parameters.get(0);
            final String typeName = (String) parameterRecord.get("TYPE_SUBNAME");
            if (typeName != null) {
                return typeName;
            }
        }
        final String sourceText = getUserSource(pPackageName).toUpperCase();

        // First, we extract the parameter type of the OUT parameter from the source based on the known parameter name
        final Pattern procPattern = Pattern.compile("PROCEDURE\\s*" + pProcedureName + "\\((.|\\n)*" + pParameterName + "\\s*OUT\\s*(.*)\\)");
        final Matcher procMatcher = procPattern.matcher(sourceText);
        String typeName;
        if (procMatcher.find()) {
            typeName = procMatcher.group(2);
        } else {
            throw new IllegalArgumentException("Could not retrieve procedure return parameter type name from source.");
        }

        // Using the previously extracted type, we can extract the row type, which is the format basis for the OUT parameter
        final Pattern typePattern = Pattern.compile("TYPE\\s*" + typeName + "\\s*IS\\s*REF\\s*CURSOR\\s*RETURN\\s*(.*)\\s*%ROWTYPE");
        final Matcher typeMatcher = typePattern.matcher(sourceText);
        String parameterRowType;
        if (typeMatcher.find()) {
            parameterRowType = typeMatcher.group(1);
        } else {
            throw new IllegalArgumentException("Could not retrieve procedure return parameter type from source.");
        }
        return parameterRowType;
	}

	private String getUserSource(final String pPackageName) {

        String query = USER_SOURCE_QUERY;
        query = query.replace("p_package_name", "'" + pPackageName + "'");
        final List<Map<String, Object>> sourceLines = queryProcessor.executeGenericSelect(query);
        if (sourceLines.size() == 0) {
            return null;
        }
        final StringBuilder sourceText = new StringBuilder();
        for (final Map<String, Object> sourceLine : sourceLines) {
            sourceText.append((String) sourceLine.get("TEXT"));
        }
        return sourceText.toString();
    }

    private static final String PARAMETER_ROWTYPE_QUERY =
	        "SELECT type_subname " +
            "FROM   user_arguments " +
            "WHERE  package_name = p_package_name " +
            "AND    object_name  = p_procedure_name " +
            "AND    data_type    = 'PL/SQL RECORD'";

	private static final String USER_SOURCE_QUERY =
            "SELECT text " +
            "FROM   user_source " +
            "WHERE  type = 'PACKAGE' " +
            "AND    name = UPPER(p_package_name) " +
            "ORDER  BY line";
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

	private static final String RECORD_METADATA_QUERY =
            "SELECT ref_cursor.type_subname table_name," +
            "       record_elements.argument_name column_name," +
            "       record_elements.data_type," +
            "       record_elements.data_precision," +
            "       record_elements.data_scale," +
            "       record_elements.char_length," +
            "       record_elements.position column_id " +
            "FROM   user_arguments ref_cursor," +
            "       user_arguments record_elements " +
            "WHERE  ref_cursor.type_subname IS NOT NULL " +
            "AND    ref_cursor.type_subname   != 'ID_RECORD' " +
            "AND    ref_cursor.type_name      != 'NOORM_METADATA' " +
            "AND    ref_cursor.data_type       = 'PL/SQL RECORD' " +
            "AND    ref_cursor.object_name     = record_elements.object_name " +
            "AND    ref_cursor.package_name    = record_elements.package_name " +
            "AND    record_elements.data_level = 2 " +
            "AND    record_elements.data_type != 'PL/SQL RECORD' " +
            "GROUP  BY ref_cursor.type_subname," +
            "          record_elements.argument_name," +
            "          record_elements.data_type," +
            "          record_elements.data_length," +
            "          record_elements.data_precision," +
            "          record_elements.data_scale," +
            "          record_elements.char_length," +
            "          record_elements.position " +
            "ORDER  BY ref_cursor.type_subname," +
            "          record_elements.position";

    /**
     * Returned REF CURSOR type variables can either be mapped to a table or view (ROWTYPE) or to a PL/SQL
     * record type as specified locally in a PL/SQL package. For the latter, the corresponding Java Bean must
     * be assembled using the information available in data dictionary view USER_ARGUMENTS.
     */
	private List<OracleTableMetadata> findRecordMetadata0() {

        final String query = RECORD_METADATA_QUERY;
        final List<Map<String, Object>> userArguments = queryProcessor.executeGenericSelect(query);
        final List<OracleTableMetadata> recordMetadataList = new ArrayList<>();
        for (final Map<String, Object> userArgument : userArguments) {
            final OracleTableMetadata recordMetadata = new OracleTableMetadata();
            recordMetadata.setTableName((String) userArgument.get("TABLE_NAME"));
            recordMetadata.setColumnName((String) userArgument.get("COLUMN_NAME"));
            recordMetadata.setDataType((String) userArgument.get("DATA_TYPE"));
            recordMetadata.setDataPrecision(convertBigDecimal2Long(userArgument.get("DATA_PRECISION")));
            recordMetadata.setDataScale(convertBigDecimal2Long(userArgument.get("DATA_SCALE")));
            recordMetadata.setCharLength(convertBigDecimal2Long(userArgument.get("CHAR_LENGTH")));
            recordMetadata.setNullable("Y");
            recordMetadata.setColumnId(convertBigDecimal2Long(userArgument.get("COLUMN_ID")));
            recordMetadata.setUpdatable("N");
            recordMetadata.setInsertable("N");
            recordMetadataList.add(recordMetadata);
        }
        return recordMetadataList;
	}

	private Long convertBigDecimal2Long(final Object pValue) {

	    return pValue == null ? null : ((BigDecimal) pValue).longValue();
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
