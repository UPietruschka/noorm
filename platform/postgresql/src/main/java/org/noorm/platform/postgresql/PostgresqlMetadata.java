package org.noorm.platform.postgresql;

import org.noorm.jdbc.DataAccessException;
import org.noorm.jdbc.JDBCMetadata;
import org.noorm.jdbc.JDBCQueryProcessor;
import org.noorm.jdbc.platform.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 21.02.17
 *         Time: 13:44
 */
public class PostgresqlMetadata extends JDBCMetadata {

    private static final Logger log = LoggerFactory.getLogger(PostgresqlMetadata.class);

    private JDBCQueryProcessor queryProcessor = JDBCQueryProcessor.getInstance();


    /**
     * Returns the version of the proprietary metadata retrieval implementation.
     *
     * @return the version
     */
    @Override
    public String getVersion() {
        return "1.2.3";
    }

    /**
     * Returns the list of packages of stored procedures subject to Java code generation.
     *
     * @param pSearchRegex a regular expression to filter the list of packages
     * @return the list of packages
     */
    @Override
    public List<String> findPackageNames(String pSearchRegex) {

        final List<String> packageNames = new ArrayList<>();
        return packageNames;
    }

    /**
     * Returns the list of procedures contained in the given package.
     *
     * @param pPackageName the package name
     * @return the list of procedures
     */
    @Override
    public List<String> findProcedureNames(String pPackageName) {

        final List<String> procedureNames = new ArrayList<>();
        return procedureNames;
    }

    /**
     * Returns all accessible sequences.
     *
     * @return the list of sequence names
     */
    @Override
    public List<Sequence> findSequences() {

        final String sequenceNameQuery = "SELECT C.RELNAME sequence_name FROM PG_CLASS C WHERE C.RELKIND = 'S'";
        final String sequenceIncrementQuery = "SELECT INCREMENT_BY FROM ";
        final List<Map<String, Object>> nameResults = queryProcessor.executeGenericSelect(sequenceNameQuery);
        final List<Sequence> sequences = new ArrayList<>();
        for (final Map<String, Object> nameResult : nameResults) {
            final Sequence sequence = new Sequence();
            sequence.setName((String) nameResult.get("sequence_name"));
            final String incQuery = sequenceIncrementQuery.concat(sequence.getName());
            final List<Map<String, Object>> incResults = queryProcessor.executeGenericSelect(incQuery);
            if (incResults.size() != 1) {
                throw new DataAccessException(DataAccessException.Type.COULD_NOT_ACCESS_JDBC_METADATA);
            }
            final Map<String, Object> incResult= incResults.get(0);
            sequence.setIncrementBy(((Long) incResult.get("increment_by")).intValue());
            sequences.add(sequence);
            log.debug("Found sequence metadata for sequence name/increment by : "
                    + sequence.getName() + "/" + sequence.getIncrementBy());
        }
        return sequences;
    }

    /**
     * Returns the parameters for a given stored procedure.
     *
     * @param pPackageName   the package name
     * @param pProcedureName the procedure name
     * @return the list of parameters for the given procedure
     */
    @Override
    public List<Parameter> findProcedureParameters(String pPackageName, String pProcedureName) {

        final List<Parameter> parameters = new ArrayList<>();
        return parameters;
    }

    /**
     * Returns the hash value for the source code of a given stored procedure package.
     *
     * @param pPackageName the package name
     * @return the has value
     */
    @Override
    public Integer getPackageHashValue(String pPackageName) {
        return 0;
    }

    /**
     * Returns the type for a given parameter of a stored procedure.
     *
     * @param pPackageName   the package name
     * @param pProcedureName the procedure name
     * @param pParameterName the parameter name
     * @return the type
     */
    @Override
    public String getParameterRowtype(String pPackageName, String pProcedureName, String pParameterName) {

        return "Unknown";
    }

    /**
     * Returns metadata for a database type definition.
     *
     * @return the list of record metadata
     */
    @Override
    public Map<String, List<TableMetadata>> findRecordMetadata() {

        final Map<String, List<TableMetadata>> recordMetadata = new HashMap<>();
        return recordMetadata;
    }
}
