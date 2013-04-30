package org.noorm.jdbc;

import oracle.jdbc.OracleConnection;
import oracle.jdbc.OraclePreparedStatement;
import org.noorm.metadata.BeanMetaDataUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private static JDBCDMLProcessor dmlProcessor = new JDBCDMLProcessor();
    private JDBCStatementProcessor statementProcessor = JDBCStatementProcessor.getInstance();

    private final StatementBuilder statementBuilder = new StatementBuilder();

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

        List<IBean> beanList = new ArrayList<IBean>();
        beanList.add(pBean);
        return batch(beanList, BatchType.INSERT, null);
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

        batch(pBeanList, BatchType.INSERT, null);
    }

    /**
     * Convenience wrapper for the list update for modifying a single Bean.
     *
     * @param pBean beans object for modification
     * @return the passed bean with updated optimistic lock version.
     */
    public T update(final IBean pBean) {

        List<IBean> beanList = new ArrayList<IBean>();
        beanList.add(pBean);
        return batch(beanList, BatchType.UPDATE, null);
    }

    /**
     * Updates the provided Bean in the database. This method assumes that the Bean
     * does not conflict with constraints, which may have been defined on the
     * target table.
     *
     * @param pBeanList list of Bean objects for modification
     */
    public void update(final List<? extends IBean> pBeanList) {

        batch(pBeanList, BatchType.UPDATE, null);
    }

    /**
     * Convenience wrapper for the list delete for deleting a single Bean.
     *
     * @param pBean beans object for deletion
     */
    public void delete(final IBean pBean) {

        List<IBean> beanList = new ArrayList<IBean>();
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

        batch(pBeanList, BatchType.DELETE, null);
    }

    private T batch(final List<? extends IBean> pBeanList, final BatchType pBatchType, final String pTableName) {

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
        OracleConnection con = null;
        OraclePreparedStatement pstmt = null;

        try {
            con = DataSourceProvider.getConnection();
            final IBean firstBean = pBeanList.get(0);
            String tableName = firstBean.getTableName();
            final String[] primaryKeyColumnNames = firstBean.getPrimaryKeyColumnNames();
            // There is currently no full support for returning generated keys in batch operation
            // Thus we support this for single-row inserts only.
            if (pBeanList.size() == 1 && pBatchType.equals(BatchType.INSERT) && primaryKeyColumnNames.length > 0) {
                returnModifiedBean = true;
            }
            final String versionColumnName = firstBean.getVersionColumnName();
            final String sequenceName = firstBean.getSequenceName();
            if (pTableName != null && !pTableName.isEmpty()) {
                // Special treatment of intermediary inserts into temporary tables for merge.
                // The passed table-name replaces the table-name of the Bean and sequences are disabled.
                tableName = pTableName;
            }
            final BeanMapper<IBean> mapper = BeanMapper.getInstance();
            Map<String, Object> fieldMap = mapper.toMap(firstBean);
            if (fieldMap.isEmpty()) {
                throw new DataAccessException(DataAccessException.Type.COULD_NOT_UPDATE_NON_UPDATABLE_BEAN);
            }
            String batch = null;
            final boolean useOptLockFullRowCompare = firstBean.getModifiedFieldsInitialValue() == null ? false : true;
            if (pBatchType.equals(BatchType.INSERT)) {
                batch = statementBuilder.buildInsert
                        (tableName, primaryKeyColumnNames, sequenceName, fieldMap);
            }
            if (pBatchType.equals(BatchType.UPDATE)) {
                if (primaryKeyColumnNames.length == 0) {
                    throw new DataAccessException(DataAccessException.Type.GENERIC_UPDATE_NOT_SUPPORTED_WITHOUT_PK);
                }
                batch = statementBuilder.buildUpdate
                        (tableName, primaryKeyColumnNames, versionColumnName, fieldMap, useOptLockFullRowCompare);
            }
            if (pBatchType.equals(BatchType.DELETE)) {
                if (primaryKeyColumnNames.length == 0) {
                    throw new DataAccessException(DataAccessException.Type.GENERIC_DELETE_NOT_SUPPORTED_WITHOUT_PK);
                }
                batch = statementBuilder.buildDelete
                        (tableName, primaryKeyColumnNames, versionColumnName, fieldMap, useOptLockFullRowCompare);
            }
            if (log.isDebugEnabled()) {
                debugDML(tableName, sequenceName, batch);
            }
            if (returnModifiedBean) {
                if (primaryKeyColumnNames.length != 1) {
                    throw new DataAccessException(DataAccessException.Type.OPERATION_NOT_SUPPORTED_WITH_COMPOSITE_PK);
                }
                pstmt = (OraclePreparedStatement) con.prepareStatement(batch, new String[]{primaryKeyColumnNames[0]});
            } else {
                pstmt = (OraclePreparedStatement) con.prepareStatement(batch);
            }
            // We do "Oracle style" batching here, which is easier to implement and superior in performance
            // as well. Through presetting the batch size (setExecuteBatch), we do not have to care for the
            // regular database updates using "executeBatch". Note that "executeUpdate" does NOT issue a
            // direct database update, but adds the given statement to the batch list (Neither "addBatch"
            // nor "executeBatch" are needed for "Oracle style" batching).
            if (!returnModifiedBean) {
                pstmt.setExecuteBatch(DataSourceProvider.getBatchUpdateSize());
            }
            int batchCount = 0;
            for (final IBean bean : pBeanList) {

                fieldMap = mapper.toMap(bean);
                for (final String fieldName : fieldMap.keySet()) {

                    boolean isPKColumn = false;
                    for (final String pkColumnName : primaryKeyColumnNames) {
                        if (fieldName.toUpperCase().equals(pkColumnName)) {
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
                                // When the version column has not been initialized by the caller,
                                // we set it here, otherwise NULL in the version column will result
                                // in an VERSION_COLUMN_NULL exception with the next update.
                                if (value == null) {
                                    value = VERSION_COLUMN_LONG_DEFAULT;
                                }
                                BeanMetaDataUtil.setVersionColumnValue(bean, (Long) value);
                            }
                            pstmt.setObjectAtName(fieldName, value);
                        }
                    }

                    if (pBatchType.equals(BatchType.UPDATE)) {
                        if (fieldName.equals(versionColumnName)) {
                            if (value instanceof Long) {
                                final Long incVersion = ((Long) value) + 1L;
                                BeanMetaDataUtil.setVersionColumnValue(bean, incVersion);
                                pstmt.setObjectAtName(fieldName, incVersion);
                            } else {
                                if (value == null) {
                                    throw new DataAccessException(DataAccessException.Type.VERSION_COLUMN_NULL);
                                }
                                throw new DataAccessException(DataAccessException.Type.UNSUPPORTED_VERSION_COLUMN_TYPE);
                            }
                        } else {
                            if (isPKColumn && value instanceof String) {
                                // SQL CHAR comparison semantics by default uses padding, which causes some
                                // confusion, since it does not even matter, whether the data has initially been
                                // provided with or without padding. Using the following proprietary Oracle method
                                // disabled this behaviour and turns off padding.
                                pstmt.setFixedCHARAtName(fieldName, (String) value);
                            } else {
                                pstmt.setObjectAtName(fieldName, value);
                            }
                        }
                    }
                    if (pBatchType.equals(BatchType.DELETE)) {
                        if (isPKColumn) {
                            if (value instanceof String) {
                                // SQL CHAR comparison semantics by default uses padding, which causes some
                                // confusion, since it does not even matter, whether the data has initially been
                                // provided with or without padding. Using the following proprietary Oracle method
                                // disabled this behaviour and turns off padding.
                                pstmt.setFixedCHARAtName(fieldName, (String) value);
                            } else {
                                pstmt.setObjectAtName(fieldName, value);
                            }
                        }
                    }
                }

                if (pBatchType.equals(BatchType.UPDATE) || pBatchType.equals(BatchType.DELETE)) {
                    if (versionColumnName != null && !versionColumnName.isEmpty()) {
                        pstmt.setObjectAtName(versionColumnName.concat(StatementBuilder.OLD_VERSION_APPENDIX),
                                fieldMap.get(versionColumnName));
                    }
                    if (useOptLockFullRowCompare) {
                        final HashMap<String, Object> modifiedFieldsInitialValue = bean.getModifiedFieldsInitialValue();
                        for (final String fieldName : fieldMap.keySet()) {
                            boolean isPKColumn = false;
                            for (final String pkColumnName : primaryKeyColumnNames) {
                                if (fieldName.toUpperCase().equals(pkColumnName)) {
                                    isPKColumn = true;
                                }
                            }
                            Object value = modifiedFieldsInitialValue.get(fieldName);
                            if (value == null) {
                                value = fieldMap.get(fieldName);
                            }
                            if (value instanceof java.util.Date) {
                                value = new Timestamp(((java.util.Date) value).getTime());
                            }
                            if (!isPKColumn || sequenceName == null || sequenceName.isEmpty()) {
                                pstmt.setObjectAtName(fieldName.concat(StatementBuilder.OLD_VERSION_APPENDIX), value);
                            }
                        }
                        if (pBatchType.equals(BatchType.UPDATE)) {
                            bean.getModifiedFieldsInitialValue().clear();
                        }
                    }
                }

                int batchCount0 = pstmt.executeUpdate();
                batchCount += batchCount0;
            }
            if (log.isDebugEnabled()) {
                log.debug(("Bean data has been attached to JDBC prepared statement. " +
                        "Executing DML statement for table/entity ".concat(tableName)
                                .concat(" [").concat(batch).concat("] using connection : ".concat(con.toString()))));
            }
            batchCount += pstmt.sendBatch();
            if (batchCount != pBeanList.size()) {
                if (pBatchType.equals(BatchType.INSERT)) {
                    issueUpdateCountException(batchCount, pBeanList.size());
                } else {
                    // When the number of affected records does not match the number of provided records,
                    // we can either have an optimistic lock conflict, or the record(s) have not been provided
                    // with a valid primary key. This can happen, when the beans initially prepared with a null
                    // primary key are reused for an update or delete
                    // We only investigate the first bean here, though it is actually possible that we do not
                    // have an optimistic lock conflict, but null values in the PK for another bean, though this
                    // scenario is considered unlikely.
                    boolean firstBeanHasNullPK = false;
                    for (final String pkColumn : primaryKeyColumnNames) {
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
                            BeanMetaDataUtil.getBeanPropertyType(firstBean, primaryKeyColumnNames[0]);
                    if (primaryKeyType.equals(Long.class)) {
                        Long generatedKey = generatedKeyResultSet.getLong(1);
                        log.debug("Generated key value " + generatedKey + " retrieved for table " + tableName);
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
                    DataSourceProvider.returnConnection(con, success);
                }
            } catch (SQLException ignored) {
            } // Nothing to do
        }
    }

    /**
     * Convenience wrapper for the list merge for merging a single Bean.
     *
     * @param pPLSQLCallable	  the name of PL/SQL merge procedure with the notation PACKAGENAME.PROCEDURE.
     * @param pTemporaryTableName the name of the temporary table used to prepare the merge operation.
     * @param pBean			   the Bean to be persisted.
     */
    public void merge(final String pPLSQLCallable,
                      final String pTemporaryTableName,
                      final IBean pBean) {

        List<IBean> beanList = new ArrayList<IBean>();
        beanList.add(pBean);
        merge(pPLSQLCallable, pTemporaryTableName, beanList);
    }

    /**
     * This method is somewhat similar to method "merge" of a JPA EntityManager. It supports inserts
     * as well as updates. Beyond this method, Class JDBCStatementProcessor only supports access to
     * stored procedures, but for inserts and updates (in particular: bulk inserts/updates) using
     * a prepared statement is still the preferable option, not only considering the complexity
     * of complex types required for inserts and updates using PL/SQL, but also, because we have
     * a quite enhanced support for batch inserts/updates in the JDBC driver as well.
     *
     * @param pPLSQLCallable	  the name of PL/SQL merge procedure with the notation PACKAGENAME.PROCEDURE.
     * @param pTemporaryTableName the name of the temporary table used to prepare the merge operation.
     * @param pBeanList		   a list containing the Beans to be persisted.
     */
    public void merge(final String pPLSQLCallable,
                      final String pTemporaryTableName,
                      final List<? extends IBean> pBeanList) {

        try {
            if (pPLSQLCallable == null || pPLSQLCallable.isEmpty()) {
                throw new IllegalArgumentException("Parameter [pPLSQLCallable] must not be null.");
            }
            if (pTemporaryTableName == null || pTemporaryTableName.isEmpty()) {
                throw new IllegalArgumentException("Parameter [pTemporaryTableName] must not be null.");
            }
            if (pBeanList == null) {
                throw new IllegalArgumentException("Parameter [pBeanList] must not be null.");
            }
            if (pBeanList.isEmpty()) {
                return;
            }
            if (pBeanList.get(0) == null) {
                throw new IllegalArgumentException("Parameter [pBeanList] must not contain null members.");
            }
        } catch (IllegalArgumentException e) {
            throw new DataAccessException(DataAccessException.Type.PARAMETERS_MUST_NOT_BE_NULL, e);
        }

        if (log.isDebugEnabled()) {
            Class beanClass = pBeanList.get(0).getClass();
            statementProcessor.debugPLSQLCall(pPLSQLCallable, null, beanClass);
        }

        DataSourceProvider.begin();
        batch(pBeanList, BatchType.INSERT, pTemporaryTableName);
        statementProcessor.callPLSQL(pPLSQLCallable, null, null, null);
        DataSourceProvider.commit();
    }

    private void issueUpdateCountException(final int pUpdateCount, final int pPassedRows) {

        StringBuilder message = new StringBuilder();
        message.append("Number of rows processed by database does not match number of passed rows. [");
        message.append(pUpdateCount);
        message.append(", ");
        message.append(pPassedRows);
        message.append("]");
        throw new DataAccessException(DataAccessException.Type.COULD_NOT_ACCESS_DATA, message.toString());
    }

    private void debugDML(final String pTableName, final String pSequenceName, final String pStatement) {

        StringBuilder logMessage = new StringBuilder();
        if (pStatement.toUpperCase().startsWith("INSERT")) {
            logMessage.append("Inserting into ").append(pTableName);
            logMessage.append(" with sequence ").append(pSequenceName);
            logMessage.append(".\n");
            logMessage.append("Using insert statement: ").append(pStatement);
        } else {
            if (pStatement.toUpperCase().startsWith("UPDATE")) {
                logMessage.append("Updating ").append(pTableName);
                logMessage.append(".\n");
                logMessage.append("Using update statement: ").append(pStatement);
            } else { // DELETE
                logMessage.append("Deleting from ").append(pTableName);
                logMessage.append(".\n");
                logMessage.append("Using delete statement: ").append(pStatement);
            }
        }
        log.debug(logMessage.toString());
    }

    private enum BatchType {

        INSERT,
        UPDATE,
        DELETE
    }
}
