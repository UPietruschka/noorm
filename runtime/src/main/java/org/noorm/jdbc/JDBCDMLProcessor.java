package org.noorm.jdbc;

import org.noorm.jdbc.platform.IPlatform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.JDBCType;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

/**
 * Processor for database DML.
 * Due to the limitations and complexity of the type handling required for passing Beans to stored procedures,
 * inserting and updating is supported on basis of generic DML execution.
 *
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 20.01.13
 *         Time: 12:20
 */
public class JDBCDMLProcessor<T> {

    private static final Logger log = LoggerFactory.getLogger(JDBCDMLProcessor.class);

    private static final Long VERSION_COLUMN_LONG_DEFAULT = 1L;
    private static final boolean USE_NAMED_PARAMETERS = false;

    private static JDBCDMLProcessor dmlProcessor = new JDBCDMLProcessor();
    private final StatementBuilder statementBuilder = new StatementBuilder();
    private final LoggingHelper loggingHelper = new LoggingHelper();

    private JDBCDMLProcessor() {
    }

    public static <T> JDBCDMLProcessor<T> getInstance() {

        return dmlProcessor;
    }

    /**
     * Convenience wrapper for the list insert for inserting a single Bean.
     *
     * @param pBean beans object for insertion
     * @return the passed bean with new primary key and initialized optimistic lock version.
     */
    public T insert(final IBean pBean) {

        final List<IBean> beanList = new ArrayList<IBean>();
        beanList.add(pBean);
        return batch(beanList, BatchType.INSERT);
    }

    /**
     * Inserts the provided Beans into the database. All Beans in the list must have the same
     * type. This method does not check for potentially existing records, but assumes that all
     * Beans in the list do not conflict with constraints, which may have been defined on the
     * target table.
     *
     * @param pBeanList list of Bean objects for insertion
     */
    public void insert(final List<? extends IBean> pBeanList) {

        batch(pBeanList, BatchType.INSERT);
    }

    /**
     * Convenience wrapper for the list update for modifying a single Bean.
     *
     * @param pBean beans object for modification
     * @return the passed bean with updated optimistic lock version.
     */
    public T update(final IBean pBean) {

        final List<IBean> beanList = new ArrayList<IBean>();
        beanList.add(pBean);
        return batch(beanList, BatchType.UPDATE);
    }

    /**
     * Updates the provided Bean in the database. This method assumes that the Bean
     * does not conflict with constraints, which may have been defined on the
     * target table.
     *
     * @param pBeanList list of Bean objects for modification
     */
    public void update(final List<? extends IBean> pBeanList) {

        if (pBeanList != null && pBeanList.size() > 1) {
            if (pBeanList.get(0).getModifiedFieldsInitialValue() != null) {
                // When optimistic locking based on pre-change image compare is used, the particular update
                // statements may differ due to different NULL values (since checking against NULL requires
                // a different SQL syntax).
                // For that reason, batch mode is not supported here.
                for (IBean bean : pBeanList) {
                    update(bean);
                }
                return;
            }
        }
        batch(pBeanList, BatchType.UPDATE);
    }

    /**
     * Convenience wrapper for the list delete for deleting a single Bean.
     *
     * @param pBean beans object for deletion
     */
    public void delete(final IBean pBean) {

        final List<IBean> beanList = new ArrayList<IBean>();
        beanList.add(pBean);
        delete(beanList);
    }

    /**
     * Deletes the provided Bean from the database. This method assumes that the Bean
     * does not conflict with constraints, which may have been defined on the
     * target table.
     *
     * @param pBeanList list of Bean objects for deletion
     */
    public void delete(final List<? extends IBean> pBeanList) {

        if (pBeanList != null && pBeanList.size() > 1) {
            if (pBeanList.get(0).getModifiedFieldsInitialValue() != null) {
                // When optimistic locking based on pre-change image compare is used, the particular delete
                // statements may differ due to different NULL values (since checking against NULL requires
                // a different SQL syntax).
                // For that reason, batch mode is not supported here.
                for (IBean bean : pBeanList) {
                    delete(bean);
                }
                return;
            }
        }
        batch(pBeanList, BatchType.DELETE);
    }

    private T batch(final List<? extends IBean> pBeanList, final BatchType pBatchType) {

        try {
            if (pBeanList == null) {
                throw new IllegalArgumentException("Parameter [pBeanList] must not be null.");
            }
            if (pBeanList.isEmpty()) {
                return null;
            }
            if (pBeanList.get(0) == null) {
                throw new IllegalArgumentException("Parameter [pBeanList] must not contain null members.");
            }
        } catch (IllegalArgumentException e) {
            throw new DataAccessException(DataAccessException.Type.PARAMETERS_MUST_NOT_BE_NULL, e);
        }

        boolean returnModifiedBean = false;
        boolean success = true;
        Connection con = null;
        PreparedStatement pstmt = null;

        try {
            con = DataSourceProvider.getConnection();
            final IBean firstBean = pBeanList.get(0);
            final String[] primaryKeyColumnNames = firstBean.getPrimaryKeyColumnNames();
            final String[] primaryKeyJavaNames = firstBean.getPrimaryKeyJavaNames();
            final Map<String, JDBCColumn> beanJDBCMetaData = BeanMetaDataUtil.getColumnMetaData(firstBean.getClass());
            // There is currently no full support for returning generated keys in batch operation
            // Thus we support this for single-row inserts only, which use a sequence for ID generation
            final String sequenceName = firstBean.getSequenceName();
            final Long sequenceIncrement = firstBean.getSequenceIncrement();
            final boolean useInlineSequenceValueGeneration = firstBean.useInlineSequenceValueGeneration();
            if (pBeanList.size() == 1 && pBatchType.equals(BatchType.INSERT) &&
                    sequenceName != null && !sequenceName.isEmpty() && useInlineSequenceValueGeneration) {
                returnModifiedBean = true;
            }
            final String versionColumnName = firstBean.getVersionColumnName();
            String batch = null;
            final boolean useOptLockFullRowCompare = firstBean.getModifiedFieldsInitialValue() != null;
            final Map<String, Integer> fieldName2ParameterIndex = new HashMap<String, Integer>();
            if (pBatchType.equals(BatchType.INSERT)) {
                batch = statementBuilder.buildInsert(firstBean, fieldName2ParameterIndex, USE_NAMED_PARAMETERS);
            }
            if (pBatchType.equals(BatchType.UPDATE)) {
                if (primaryKeyColumnNames.length == 0) {
                    throw new DataAccessException(DataAccessException.Type.GENERIC_UPDATE_NOT_SUPPORTED_WITHOUT_PK);
                }
                batch = statementBuilder.buildUpdate
                        (firstBean, useOptLockFullRowCompare, fieldName2ParameterIndex, USE_NAMED_PARAMETERS);
            }
            if (pBatchType.equals(BatchType.DELETE)) {
                if (primaryKeyColumnNames.length == 0) {
                    throw new DataAccessException(DataAccessException.Type.GENERIC_DELETE_NOT_SUPPORTED_WITHOUT_PK);
                }
                batch = statementBuilder.buildDelete
                        (firstBean, useOptLockFullRowCompare, fieldName2ParameterIndex, USE_NAMED_PARAMETERS);
            }
            if (log.isDebugEnabled()) {
                final String tableName = firstBean.getTableName();
                loggingHelper.debugDML(tableName, sequenceName, batch);
            }
            if (returnModifiedBean) {
                if (primaryKeyColumnNames.length != 1) {
                    throw new DataAccessException(DataAccessException.Type.OPERATION_NOT_SUPPORTED_WITH_COMPOSITE_PK);
                }
                pstmt = con.prepareStatement(batch, new String[]{primaryKeyColumnNames[0]});
            } else {
                pstmt = con.prepareStatement(batch);
            }

            int batchCount = 0;
            int updateCount = 0;
            final IPlatform platform = DataSourceProvider.getPlatform();
            for (final IBean bean : pBeanList) {

                final BeanMapper<IBean> mapper = BeanMapper.getInstance();
                final Map<String, Object> fieldMap = mapper.toMap(bean);
                if (fieldMap.isEmpty()) {
                    throw new DataAccessException(DataAccessException.Type.COULD_NOT_UPDATE_NON_UPDATABLE_BEAN);
                }
                for (final String fieldName : fieldMap.keySet()) {

                    final JDBCColumn jdbcColumn = beanJDBCMetaData.get(fieldName);

                    final Integer parameterIndex = fieldName2ParameterIndex.get(fieldName);
                    if (parameterIndex == null) {
                        continue;
                    }
                    boolean isPKColumn = false;
                    for (final String pkColumnName : primaryKeyColumnNames) {
                        if (fieldName.equals(pkColumnName)) {
                            isPKColumn = true;
                        }
                    }
                    Object value = fieldMap.get(fieldName);
                    if (value instanceof java.util.Date) {
                        value = new Timestamp(((java.util.Date) value).getTime());
                    }
                    if (pBatchType.equals(BatchType.INSERT)) {
                        if (!isPKColumn || sequenceName == null || sequenceName.isEmpty()) {
                            if (fieldName.equals(versionColumnName)) {
                                if (value == null) {
                                    // When the version column has not been initialized by the caller,
                                    // we set it here, otherwise NULL in the version column will result
                                    // in an VERSION_COLUMN_NULL exception with the next update.
                                    value = buildVersionColumnValue(bean, pBatchType, value);
                                }
                            }
                            platform.setObject(pstmt, value, parameterIndex, jdbcColumn.dataType());
                        } else {
                            if (!useInlineSequenceValueGeneration) {
                                final Class primaryKeyType =
                                        BeanMetaDataUtil.getBeanPropertyType(firstBean, primaryKeyJavaNames[0]);
                                final Number sequenceValue = DataSourceProvider
                                        .getNextSequenceValue(sequenceName, sequenceIncrement, primaryKeyType);
                                BeanMetaDataUtil.setPrimaryKeyValue(firstBean, sequenceValue);
                                platform.setObject(pstmt, sequenceValue, parameterIndex, jdbcColumn.dataType());
                            }
                        }
                    }

                    if (pBatchType.equals(BatchType.UPDATE)) {
                        if (fieldName.equals(versionColumnName)) {
                            if (value == null) {
                                throw new DataAccessException(DataAccessException.Type.VERSION_COLUMN_NULL);
                            }
                            Object newVersion = buildVersionColumnValue(bean, pBatchType, value);
                            platform.setObject(pstmt, newVersion, parameterIndex, jdbcColumn.dataType());
                        } else {
                            platform.setObject(pstmt, value, parameterIndex, jdbcColumn.dataType());
                        }
                    }
                    if (pBatchType.equals(BatchType.DELETE)) {
                        if (isPKColumn) {
                            platform.setObject(pstmt, value, parameterIndex, jdbcColumn.dataType());
                        }
                    }
                }

                if (pBatchType.equals(BatchType.INSERT) && useOptLockFullRowCompare) {
                    bean.getModifiedFieldsInitialValue().clear();
                }
                if (pBatchType.equals(BatchType.UPDATE) || pBatchType.equals(BatchType.DELETE)) {
                    if (versionColumnName != null && !versionColumnName.isEmpty()) {
                        final JDBCColumn jdbcColumn = beanJDBCMetaData.get(versionColumnName);
                        final int parameterIndex = fieldName2ParameterIndex.get
                                (versionColumnName.concat(StatementBuilder.OLD_VERSION_APPENDIX));
                        platform.setObject(pstmt, fieldMap.get(versionColumnName), parameterIndex, jdbcColumn.dataType());
                    }
                    if (useOptLockFullRowCompare) {
                        final Map<String, Object> modifiedFieldsInitialValue = bean.getModifiedFieldsInitialValue();
                        for (final String fieldName : fieldMap.keySet()) {
                            final JDBCColumn jdbcColumn = beanJDBCMetaData.get(fieldName);
                            boolean isPKColumn = false;
                            for (final String pkColumnName : primaryKeyColumnNames) {
                                if (fieldName.equals(pkColumnName)) {
                                    isPKColumn = true;
                                }
                            }
                            Object value;
                            if (modifiedFieldsInitialValue.containsKey(fieldName)) {
                                value = modifiedFieldsInitialValue.get(fieldName);
                            } else {
                                value = fieldMap.get(fieldName);
                            }
                            if (value instanceof java.util.Date) {
                                value = new Timestamp(((java.util.Date) value).getTime());
                            }
                            if (!isPKColumn && value != null) {
                                final String namedParameter = fieldName.concat(StatementBuilder.OLD_VERSION_APPENDIX);
                                final int parameterIndex = fieldName2ParameterIndex.get(namedParameter);
                                platform.setObject(pstmt, value, parameterIndex, jdbcColumn.dataType());
                            }
                        }
                        if (pBatchType.equals(BatchType.UPDATE)) {
                            bean.getModifiedFieldsInitialValue().clear();
                        }
                    }
                }

                pstmt.addBatch();
                if (++batchCount % DataSourceProvider.getBatchUpdateSize() == 0) {
                    updateCount += platform.executeBatchWithReliableCount(pstmt);
                }
            }
            if (log.isDebugEnabled()) {
                final String tableName = firstBean.getTableName();
                log.debug(("Bean data has been attached to JDBC prepared statement. " +
                        "Executing DML statement for table/entity ".concat(tableName)
                                .concat(" [").concat(batch).concat("] using connection : ".concat(con.toString()))));
            }
            if (batchCount % DataSourceProvider.getBatchUpdateSize() > 0) {
                updateCount += platform.executeBatchWithReliableCount(pstmt);
            }
            if (updateCount != pBeanList.size()) {
                if (pBatchType.equals(BatchType.INSERT)) {
                    issueUpdateCountException(updateCount, pBeanList.size());
                } else {
                    // When the number of affected records does not match the number of provided records,
                    // we can either have an optimistic lock conflict, or the record(s) have not been provided
                    // with a valid primary key. This can happen, when the beans initially prepared with a null
                    // primary key are reused for an update or delete
                    // We only investigate the first bean here, though it is actually possible that we do not
                    // have an optimistic lock conflict, but null values in the PK for another bean, though this
                    // scenario is considered unlikely.
                    boolean firstBeanHasNullPK = false;
                    for (final String pkColumn : primaryKeyJavaNames) {
                        if (BeanMetaDataUtil.getBeanPropertyByName(firstBean, pkColumn) == null) {
                            firstBeanHasNullPK = true;
                        }
                    }
                    if (firstBeanHasNullPK) {
                        if (pBatchType.equals(BatchType.UPDATE)) {
                            throw new DataAccessException(DataAccessException.Type.GENERIC_UPDATE_FAILED_WITH_NULL_PK);
                        } else { // DELETE
                            throw new DataAccessException(DataAccessException.Type.GENERIC_DELETE_FAILED_WITH_NULL_PK);
                        }
                    } else {
                        throw new DataAccessException(DataAccessException.Type.OPTIMISTIC_LOCK_CONFLICT);
                    }
                }
            }
            if (returnModifiedBean) {
                ResultSet generatedKeyResultSet = pstmt.getGeneratedKeys();
                while (generatedKeyResultSet.next()) {
                    // Generated keys are supported for a numeric primary key only. For other data-types we
                    // assume that the primary has already been set by the caller.
                    final Class primaryKeyType =
                            BeanMetaDataUtil.getBeanPropertyType(firstBean, primaryKeyJavaNames[0]);
                    if (primaryKeyType.equals(Long.class)) {
                        final Long generatedKey = generatedKeyResultSet.getLong(1);
                        BeanMetaDataUtil.setPrimaryKeyValue(firstBean, generatedKey);
                        continue;
                    }
                    if (primaryKeyType.equals(Integer.class)) {
                        final Integer generatedKey = generatedKeyResultSet.getInt(1);
                        BeanMetaDataUtil.setPrimaryKeyValue(firstBean, generatedKey);
                        continue;
                    }
                    if (primaryKeyType.equals(Short.class)) {
                        final Short generatedKey = generatedKeyResultSet.getShort(1);
                        BeanMetaDataUtil.setPrimaryKeyValue(firstBean, generatedKey);
                    }
                }
            }
            return (T) firstBean;
        } catch (DataAccessException e) {
            // Avoid that a DataAccessException is wrapped into another (generic COULD_NOT_ACCESS_DATA) one.
            log.error("DML execution failed.", e);
            success = false;
            throw e;
        } catch (Exception e) {
            log.error(DataAccessException.Type.COULD_NOT_ACCESS_DATA.getDescription(), e);
            success = false;
            throw new DataAccessException(DataAccessException.Type.COULD_NOT_ACCESS_DATA, e);
        } finally {
            try {
                if (pstmt != null) {
                    pstmt.close();
                }
                if (con != null && !con.isClosed()) {
                    DataSourceProvider.returnConnection(success);
                }
            } catch (SQLException ignored) {
            } // Nothing to do
        }
    }

    /**
     * Executes a generic UPDATE statement for the given table (or view) name with the given query parameters.
     * This functionality is designated to support the query declaration available in the Maven generator plugin.
     * Any complex SQL like joins is expected to be encapsulated within a database view definition and is not
     * supported here.
     *
     * @param pTableName the table or view name used for the SQL query
     * @param pUpdateParameters the column names and values to be updated
     * @param pQueryParameters the parameters for the where-clause of the SQL query
     * @param pBeanClass the return type
     * @return the number of updated records
     */
    public int update(final String pTableName,
                      final Map<String, Object> pUpdateParameters,
                      final Map<QueryColumn, Object> pQueryParameters,
                      final Class<T> pBeanClass) {

        try {
            if (pTableName == null || pTableName.isEmpty()) {
                throw new IllegalArgumentException("Parameter [pTableName] must not be null.");
            }
            if (pBeanClass == null) {
                throw new IllegalArgumentException("Parameter [pBeanClass] must not be null.");
            }
            if (pUpdateParameters == null || pUpdateParameters.size() == 0) {
                throw new IllegalArgumentException("Parameter [pUpdateParameters] must not be empty or null.");
            }
            if (pQueryParameters == null) {
                throw new IllegalArgumentException("Parameter [pQueryParameters] must not be null.");
            }
        } catch (IllegalArgumentException e) {
            throw new DataAccessException(DataAccessException.Type.PARAMETERS_MUST_NOT_BE_NULL, e);
        }

        if (log.isDebugEnabled()) {
            loggingHelper.debugUpdate(pTableName, pUpdateParameters, pQueryParameters, pBeanClass);
        }

        boolean success = true;
        Connection con = null;
        PreparedStatement pstmt = null;
        try {
            con = DataSourceProvider.getConnection();
            final IPlatform platform = DataSourceProvider.getPlatform();
            final String sqlStmt = statementBuilder.buildUpdate
                    (pTableName, pUpdateParameters, pQueryParameters, USE_NAMED_PARAMETERS);
            if (log.isDebugEnabled()) {
                log.debug("Preparing and executing UPDATE statement: ".concat(sqlStmt)
                        .concat("; using connection : ".concat(con.toString())));
            }
            pstmt = con.prepareStatement(sqlStmt);

            int parameterIndex = 1;

            final Map<String, Object> orderedUpdateParameters = new TreeMap<>(pUpdateParameters);
            for (final String updateColumn : orderedUpdateParameters.keySet()) {
                Object value = orderedUpdateParameters.get(updateColumn);
                if (value != null) {
                    if (value instanceof java.util.Date) {
                        value = new Timestamp(((java.util.Date) value).getTime());
                    } else {
                        platform.setObject(pstmt, value, parameterIndex++, -1);
                    }
                }
            }

            final Map<QueryColumn, Object> orderedQueryParameters = new TreeMap<>(pQueryParameters);
            for (final QueryColumn queryColumn : orderedQueryParameters.keySet()) {
                if (!queryColumn.getOperator().isUnary()) {
                    Object value = orderedQueryParameters.get(queryColumn);
                    if (value instanceof java.util.Date) {
                        value = new Timestamp(((java.util.Date) value).getTime());
                    }
                    if (value instanceof List) {
                        final List<Object> inClauseValues = ((List<Object>) orderedQueryParameters.get(queryColumn));
                        for (final Object inClauseValue : inClauseValues) {
                            platform.setObject(pstmt, inClauseValue, parameterIndex++, -1);
                        }
                    } else {
                        if (value != null) {
                            platform.setObject(pstmt, value, parameterIndex++, -1);
                        }
                    }
                }
            }

            int updateCount = pstmt.executeUpdate();
            if (log.isDebugEnabled()) {
                log.debug("Bulk update operation updated " + updateCount + " records");
            }
            return updateCount;
        } catch (Exception e) {
            log.error(DataAccessException.Type.COULD_NOT_ACCESS_DATA.getDescription(), e);
            success = false;
            throw new DataAccessException(DataAccessException.Type.COULD_NOT_ACCESS_DATA, e);
        } finally {
            try {
                if (pstmt != null) {
                    pstmt.close();
                }
                if (con != null && !con.isClosed()) {
                    DataSourceProvider.returnConnection(success);
                }
            } catch (SQLException ignored) {
            } // Nothing to do
        }
    }

    /**
     * Executes a generic DELETE statement for the given table (or view) name with the given query parameters.
     * This functionality is designated to support the query declaration available in the Maven generator plugin.
     * Any complex SQL like joins is expected to be encapsulated within a database view definition and is not
     * supported here.
     *
     * @param pTableName the table or view name used for the SQL query
     * @param pQueryParameters the parameters for the where-clause of the SQL query
     * @param pBeanClass the return type
     * @return the number of deleted records
     */
    public int delete(final String pTableName,
                      final Map<QueryColumn, Object> pQueryParameters,
                      final Class<T> pBeanClass) {

        try {
            if (pTableName == null || pTableName.isEmpty()) {
                throw new IllegalArgumentException("Parameter [pTableName] must not be null.");
            }
            if (pBeanClass == null) {
                throw new IllegalArgumentException("Parameter [pBeanClass] must not be null.");
            }
            if (pQueryParameters == null) {
                throw new IllegalArgumentException("Parameter [pQueryParameters] must not be null.");
            }
        } catch (IllegalArgumentException e) {
            throw new DataAccessException(DataAccessException.Type.PARAMETERS_MUST_NOT_BE_NULL, e);
        }

        if (log.isDebugEnabled()) {
            loggingHelper.debugDelete(pTableName, pQueryParameters, pBeanClass);
        }

        boolean success = true;
        Connection con = null;
        PreparedStatement pstmt = null;
        try {
            con = DataSourceProvider.getConnection();
            final IPlatform platform = DataSourceProvider.getPlatform();
            final String sqlStmt = statementBuilder.buildDelete(pTableName, pQueryParameters, USE_NAMED_PARAMETERS);
            if (log.isDebugEnabled()) {
                log.debug("Preparing and executing DELETE statement: ".concat(sqlStmt)
                        .concat("; using connection : ".concat(con.toString())));
            }
            pstmt = con.prepareStatement(sqlStmt);

            int parameterIndex = 1;
            final Map<QueryColumn, Object> orderedQueryParameters = new TreeMap<>(pQueryParameters);
            for (final QueryColumn queryColumn : orderedQueryParameters.keySet()) {
                if (!queryColumn.getOperator().isUnary()) {
                    Object value = orderedQueryParameters.get(queryColumn);
                    if (value instanceof java.util.Date) {
                        value = new Timestamp(((java.util.Date) value).getTime());
                    }
                    if (value instanceof List) {
                        final List<Object> inClauseValues = ((List<Object>) orderedQueryParameters.get(queryColumn));
                        for (final Object inClauseValue : inClauseValues) {
                            platform.setObject(pstmt, inClauseValue, parameterIndex++, -1);
                        }
                    } else {
                        if (value != null) {
                            platform.setObject(pstmt, value, parameterIndex++, -1);
                        }
                    }
                }
            }
            int deleteCount = pstmt.executeUpdate();
            if (log.isDebugEnabled()) {
                log.debug("Bulk deletion operation deleted " + deleteCount + " records");
            }
            return deleteCount;
        } catch (Exception e) {
            log.error(DataAccessException.Type.COULD_NOT_ACCESS_DATA.getDescription(), e);
            success = false;
            throw new DataAccessException(DataAccessException.Type.COULD_NOT_ACCESS_DATA, e);
        } finally {
            try {
                if (pstmt != null) {
                    pstmt.close();
                }
                if (con != null && !con.isClosed()) {
                    DataSourceProvider.returnConnection(success);
                }
            } catch (SQLException ignored) {
            } // Nothing to do
        }
    }

    private Object buildVersionColumnValue(final IBean pBean, final BatchType pBatchType, final Object pOldValue) {

        Object value;
        final JDBCType versionColumnType = pBean.getVersionColumnType();
        if (versionColumnType.equals(JDBCType.NUMERIC)) {
            if (pBatchType.equals(BatchType.INSERT)) {
                value = VERSION_COLUMN_LONG_DEFAULT;
            } else { // BatchType.UPDATE
                value = ((Long) pOldValue) + 1L;
            }
        } else {
            final Calendar calendar = Calendar.getInstance();
            if (versionColumnType.equals(JDBCType.TIMESTAMP)) {
                value = new Timestamp(calendar.getTimeInMillis());
            } else {
                if (versionColumnType.equals(JDBCType.DATE)) {
                    // The DATE type precision is limited to seconds only, so we have
                    // to truncate the fractions here to get consistent settings
                    calendar.set(Calendar.MILLISECOND, 0);
                    value = new Timestamp(calendar.getTimeInMillis());
                } else {
                    throw new DataAccessException
                            (DataAccessException.Type.UNSUPPORTED_VERSION_COLUMN_TYPE);
                }
            }
        }
        BeanMetaDataUtil.setVersionColumnValue(pBean, value);
        return value;
    }

    private void issueUpdateCountException(final int pUpdateCount, final int pPassedRows) {

        final StringBuilder message = new StringBuilder();
        message.append("Number of rows processed by database does not match number of passed rows. [");
        message.append(pUpdateCount);
        message.append(", ");
        message.append(pPassedRows);
        message.append("]");
        throw new DataAccessException(DataAccessException.Type.COULD_NOT_ACCESS_DATA, message.toString());
    }

    private enum BatchType {

        INSERT,
        UPDATE,
        DELETE
    }
}
