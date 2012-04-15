package org.noorm.jdbc;

import oracle.jdbc.OracleCallableStatement;
import oracle.jdbc.OracleConnection;
import oracle.jdbc.OraclePreparedStatement;
import oracle.sql.ARRAY;
import oracle.sql.ArrayDescriptor;
import org.noorm.metadata.BeanMetaDataUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Processor for JDBC database access.
 * This class only supports access to stored procedures, insert and update statements.
 * Based on the expectation that all data-driven business logic is implemented in stored
 * procedures, there is no need for direct SQL support. Due to the limitations and
 * complexity of the type handling required for passing Beans to stored procedures,
 * inserting and updating is supported as well.
 *
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 */
public class JDBCStatementProcessor<T> {

	private static final Logger log = LoggerFactory.getLogger(JDBCStatementProcessor.class);

	private static JDBCStatementProcessor statementProcessor = new JDBCStatementProcessor();

	private final StatementBuilder statementBuilder = new StatementBuilder();

	public static final String NOORM_ID_LIST_ORACLE_TYPE_NAME = "NUM_ARRAY";
	private static final Long VERSION_COLUMN_DEFAULT = 1L;
	/*
	 * Using named parameters for callable statements does not work consistently over the various supported
	 * combinations of JDBC driver versions and database version. In addition, a bug in Oracle 11.2.0.1.0
	 * (9147506 Named parameter in callable statement not working from JDBC), prevents the usage of named
	 * parameters for callable statements when using Oracle JDBC 11.2.0.2.0). Although the release notes for
	  * Oracle database patch 11.2.0.2.0 state that bug 9147506 is fixed with this patch, there are still
	  * problems with Oracle XE 11.2.0.2.0 beta, which should have the same code base.
	  * The following parameters disabled the usage of named parameters for callable statements (instead,
	  * indexed parameters are used. This does not apply to prepared statements).
	 */
	private static final boolean USE_NAMED_PARAMETERS = false;

	private JDBCStatementProcessor() {
	}

	public static <T> JDBCStatementProcessor<T> getInstance() {

		return statementProcessor;
	}

	/**
	 * Call a PL/SQL procedure without OUT parameter.
	 *
	 * @param pPLSQLCallable the name of PL/SQL procedure or the notation PACKAGENAME.PROCEDURE.
	 * @param pInParameters  the map containing all IN parameters.
	 */
	public void callPLSQL(final String pPLSQLCallable,
						  final Map<String, Object> pInParameters) {

		callPLSQL(pPLSQLCallable, null, pInParameters, null);
	}

	/**
	 * Call a PL/SQL procedure with a scalar OUT parameter or without OUT parameter.
	 *
	 * @param pPLSQLCallable the name of PL/SQL procedure or the notation PACKAGENAME.PROCEDURE.
	 * @param pOutParamName  the name of the scalar OUT parameter.
	 * @param pInParameters  the map containing all IN parameters.
	 * @param pOutClass	  the type of the scalar OUT parameter.
	 * @return The scalar data-type returned by the PL/SQL procedure, if any.
	 */
	public T callPLSQL(final String pPLSQLCallable,
					   final String pOutParamName,
					   final Map<String, Object> pInParameters,
					   final Class<T> pOutClass) {

		try {
			if (pPLSQLCallable == null || pPLSQLCallable.isEmpty()) {
				throw new IllegalArgumentException("Parameter [pPLSQLCallable] must not be null.");
			}
			if (pOutParamName == null) {
				if (!(pOutClass == null)) {
					throw new IllegalArgumentException("Parameters [pOutParamName, pOutClass] are invalid.");
				}
			}
		} catch (IllegalArgumentException e) {
			throw new DataAccessException(DataAccessException.Type.PARAMETERS_MUST_NOT_BE_NULL, e);
		}

		if (log.isDebugEnabled()) {
			debugPLSQLCall(pPLSQLCallable, pInParameters, null);
		}

		boolean success = true;
		OracleConnection con = null;
		OracleCallableStatement cstmt = null;
		try {
			con = DataSourceProvider.getConnection();
			final String plSQLCall = statementBuilder.buildPLSQLCall
					(pPLSQLCallable, pOutParamName, pInParameters, USE_NAMED_PARAMETERS);
			if (log.isDebugEnabled()) {
				log.debug("PL/SQL Call: ".concat(plSQLCall));
			}
			cstmt = (OracleCallableStatement) con.prepareCall(plSQLCall);

			int parameterIndex = 1;
			if (pOutParamName != null) {
				int type = oracle.jdbc.OracleTypes.VARCHAR;
				if (pOutClass.getSuperclass().equals(Number.class)) {
					type = oracle.jdbc.OracleTypes.NUMBER;
				}
				if (pOutClass.isAssignableFrom(java.util.Date.class)) {
					type = oracle.jdbc.OracleTypes.TIMESTAMP;
				}

				if (USE_NAMED_PARAMETERS) {
					cstmt.registerOutParameter(pOutParamName, type);
				} else {
					cstmt.registerOutParameter(parameterIndex++, type);
				}
			}

			if (pInParameters != null) {
				bindParameters(con, pInParameters, cstmt, parameterIndex);
			}

			cstmt.execute();

			T outValue = null;
			if (pOutParamName != null) {
				outValue = getOutParameter(pOutParamName, pOutClass, cstmt);
			}

			if (log.isDebugEnabled()) {
				debugPLSQLTermination(pPLSQLCallable, -1);
			}

			return outValue;
		} catch (Exception e) {
			log.error(DataAccessException.Type.COULD_NOT_ACCESS_DATA.getDescription(), e);
			success = false;
			throw new DataAccessException(DataAccessException.Type.COULD_NOT_ACCESS_DATA, e);
		} finally {
			try {
				if (cstmt != null) {
					cstmt.close();
				}
				if (con != null && !con.isClosed()) {
					DataSourceProvider.returnConnection(con, success);
				}
			} catch (SQLException ignored) {
			} // Nothing to do
		}

	}

	private T getOutParameter(final String pOutParamName,
							  final Class<T> pOutClass,
							  final OracleCallableStatement cstmt) throws SQLException {

		T outValue = null;
		if (USE_NAMED_PARAMETERS) {
			if (pOutClass.equals(Long.class)) {
				outValue = (T) Long.valueOf(cstmt.getLong(pOutParamName));
			}
			if (pOutClass.equals(Integer.class)) {
				outValue = (T) Integer.valueOf(cstmt.getInt(pOutParamName));
			}
			if (pOutClass.equals(Double.class)) {
				outValue = (T) Double.valueOf(cstmt.getDouble(pOutParamName));
			}
			if (pOutClass.equals(Short.class)) {
				outValue = (T) Short.valueOf(cstmt.getShort(pOutParamName));
			}
			if (pOutClass.equals(Float.class)) {
				outValue = (T) Float.valueOf(cstmt.getFloat(pOutParamName));
			}
			if (pOutClass.equals(BigDecimal.class)) {
				outValue = (T) cstmt.getBigDecimal(pOutParamName);
			}
			if (pOutClass.equals(String.class)) {
				outValue = (T) cstmt.getString(pOutParamName);
			}
			if (pOutClass.equals(java.sql.Date.class)) {
				outValue = (T) cstmt.getDate(pOutParamName);
			}
			if (pOutClass.equals(Timestamp.class)) {
				outValue = (T) cstmt.getTimestamp(pOutParamName);
			}
		} else {
			if (pOutClass.equals(Long.class)) {
				outValue = (T) Long.valueOf(cstmt.getLong(1));
			}
			if (pOutClass.equals(Integer.class)) {
				outValue = (T) Integer.valueOf(cstmt.getInt(1));
			}
			if (pOutClass.equals(Double.class)) {
				outValue = (T) Double.valueOf(cstmt.getDouble(1));
			}
			if (pOutClass.equals(Short.class)) {
				outValue = (T) Short.valueOf(cstmt.getShort(1));
			}
			if (pOutClass.equals(Float.class)) {
				outValue = (T) Float.valueOf(cstmt.getFloat(1));
			}
			if (pOutClass.equals(BigDecimal.class)) {
				outValue = (T) cstmt.getBigDecimal(1);
			}
			if (pOutClass.equals(String.class)) {
				outValue = (T) cstmt.getString(1);
			}
			if (pOutClass.equals(java.sql.Date.class)) {
				outValue = (T) cstmt.getDate(1);
			}
			if (pOutClass.equals(Timestamp.class)) {
				outValue = (T) cstmt.getTimestamp(1);
			}
		}

		if (outValue == null) {
			throw new DataAccessException(DataAccessException.Type.UNSUPPORTED_DATATYPE, pOutClass.getName());
		}

		return outValue;
	}

	/**
	 * Calls a PL/SQL procedure with a ref cursor as OUT parameter. This method is expected to return
	 * a single record or no record as a result. In case of multiple records matching the given
	 * parameter map, a DataAccessException is thrown.
	 *
	 * @param pPLSQLCallable the name of PL/SQL procedure or the notation PACKAGENAME.PROCEDURE.
	 * @param pRefCursorName the parameter name of the procedure out parameter ref cursor.
	 * @param pInParameters  the map containing all IN parameters.
	 * @param pBeanClass	 the type of the Bean matching the fields of the ResultSet.
	 * @return The Beans containing the retrieved data.
	 */
	public T getBeanFromPLSQL(final String pPLSQLCallable,
							  final String pRefCursorName,
							  final Map<String, Object> pInParameters,
							  final Class<T> pBeanClass) {

		final List<T> beanList = getBeanListFromPLSQL
				(pPLSQLCallable, pRefCursorName, pInParameters, pBeanClass);
		if (beanList.isEmpty()) {
			return null;
		}
		if (beanList.size() > 1) {
			throw new DataAccessException(DataAccessException.Type.MULTIPLE_RECORDS_FOUND);
		}
		return beanList.get(0);
	}

	/**
	 * Calls a PL/SQL procedure with a ref cursor as OUT parameter.
	 *
	 * @param pPLSQLCallable the name of PL/SQL procedure or the notation PACKAGENAME.PROCEDURE.
	 * @param pRefCursorName the parameter name of the procedure out parameter ref cursor.
	 * @param pBeanClass	 the type of the Bean matching the fields of the ResultSet.
	 * @return The list of Beans containing the retrieved data.
	 */
	public List<T> getBeanListFromPLSQL(final String pPLSQLCallable,
										final String pRefCursorName,
										final Class<T> pBeanClass) {

		return getBeanListFromPLSQL(pPLSQLCallable, pRefCursorName, null, pBeanClass);
	}

	public List<T> getBeanListFromPLSQL(final String pPLSQLCallable,
										final String pRefCursorName,
										final Map<String, Object> pInParameters,
										final Class<T> pBeanClass) {

		try {
			if (pPLSQLCallable == null || pPLSQLCallable.isEmpty()) {
				throw new IllegalArgumentException("Parameter [pPLSQLCallable] must not be null.");
			}
			if (pRefCursorName == null || pRefCursorName.isEmpty()) {
				throw new IllegalArgumentException("Parameter [pRefCursorName] must not be null.");
			}
			if (pBeanClass == null) {
				throw new IllegalArgumentException("Parameter [pBeanClass] must not be null.");
			}
			if (pInParameters == null) {
				throw new IllegalArgumentException("Parameter [pInParameters] must not be null.");
			}
		} catch (IllegalArgumentException e) {
			throw new DataAccessException(DataAccessException.Type.PARAMETERS_MUST_NOT_BE_NULL, e);
		}

		if (log.isDebugEnabled()) {
			debugPLSQLCall(pPLSQLCallable, pInParameters, pBeanClass);
		}

		boolean success = true;
		List<T> beanList;
		OracleConnection con = null;
		OracleCallableStatement cstmt = null;
		try {
			con = DataSourceProvider.getConnection();
			final String plSQLCall = statementBuilder.buildPLSQLCall
					(pPLSQLCallable, pRefCursorName, pInParameters, USE_NAMED_PARAMETERS);
			if (log.isDebugEnabled()) {
				log.debug("PL/SQL Call: ".concat(plSQLCall));
			}
			cstmt = (OracleCallableStatement) con.prepareCall(plSQLCall);

			int parameterIndex = 1;
			if (USE_NAMED_PARAMETERS) {
				cstmt.registerOutParameter(pRefCursorName, oracle.jdbc.OracleTypes.CURSOR);
			} else {
				cstmt.registerOutParameter(parameterIndex++, oracle.jdbc.OracleTypes.CURSOR);
			}

			bindParameters(con, pInParameters, cstmt, parameterIndex);

			cstmt.execute();

			ResultSet rs;
			if (USE_NAMED_PARAMETERS) {
				rs = (ResultSet) cstmt.getObject(pRefCursorName);
			} else {
				rs = (ResultSet) cstmt.getObject(1);
			}
			final BeanMapper<T> mapper = BeanMapper.getInstance();
			beanList = mapper.toBeanList(rs, pBeanClass);
			if (beanList.isEmpty()) {
				beanList = new ArrayList<T>();
			}
			rs.close();

			if (log.isDebugEnabled()) {
				debugPLSQLTermination(pPLSQLCallable, beanList.size());
			}

			return beanList;
		} catch (Exception e) {
			log.error(DataAccessException.Type.COULD_NOT_ACCESS_DATA.getDescription(), e);
			success = false;
			throw new DataAccessException(DataAccessException.Type.COULD_NOT_ACCESS_DATA, e);
		} finally {
			try {
				if (cstmt != null) {
					cstmt.close();
				}
				if (con != null && !con.isClosed()) {
					DataSourceProvider.returnConnection(con, success);
				}
			} catch (SQLException ignored) {
			} // Nothing to do
		}
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
			if (pBatchType.equals(BatchType.INSERT)) {
				batch = statementBuilder.buildInsert
						(tableName, primaryKeyColumnNames, sequenceName, fieldMap);
			}
			if (pBatchType.equals(BatchType.UPDATE)) {
				if (primaryKeyColumnNames.length == 0) {
					throw new DataAccessException(DataAccessException.Type.GENERIC_UPDATE_NOT_SUPPORTED_WITHOUT_PK);
				}
				batch = statementBuilder.buildUpdate
						(tableName, primaryKeyColumnNames, versionColumnName, fieldMap);
			}
			if (pBatchType.equals(BatchType.DELETE)) {
				if (primaryKeyColumnNames.length == 0) {
					throw new DataAccessException(DataAccessException.Type.GENERIC_DELETE_NOT_SUPPORTED_WITHOUT_PK);
				}
				batch = statementBuilder.buildDelete
						(tableName, primaryKeyColumnNames, versionColumnName);
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
								// in an OPTIMISTIC_LOCK_CONFLICT with the next update.
								if (value == null) {
									value = VERSION_COLUMN_DEFAULT;
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
							pstmt.setObjectAtName(fieldName, value);
						}
					}
					if (pBatchType.equals(BatchType.DELETE)) {
						if (isPKColumn) {
							pstmt.setObjectAtName(fieldName, value);
						}
					}
				}

				if (pBatchType.equals(BatchType.UPDATE) || pBatchType.equals(BatchType.DELETE)) {
					if (versionColumnName != null && !versionColumnName.isEmpty()) {
						pstmt.setObjectAtName(versionColumnName.concat(StatementBuilder.OLD_VERSION_APPENDIX),
								fieldMap.get(versionColumnName));
					}
				}

				int batchCount0 = pstmt.executeUpdate();
				batchCount += batchCount0;
			}
			batchCount += pstmt.sendBatch();
			if (batchCount != pBeanList.size()) {
				if (pBatchType.equals(BatchType.INSERT)) {
					issueUpdateCountException(batchCount, pBeanList.size());
				} else {
					throw new DataAccessException(DataAccessException.Type.OPTIMISTIC_LOCK_CONFLICT);
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
			debugPLSQLCall(pPLSQLCallable, null, beanClass);
		}

		DataSourceProvider.begin();
		batch(pBeanList, BatchType.INSERT, pTemporaryTableName);
		callPLSQL(pPLSQLCallable, null, null, null);
		DataSourceProvider.commit();
	}

	/**
	 * Convenience method to a provide some minimal SQL functionality for the application. Usage of this method
	 * is discouraged, but maybe helpful under some circumstances.
	 *
	 * @param pSelectStatement The SELECT statement for generic execution.
	 * @return A list containing a map for each record with a column-name to object mapping.
	 */
	public List<Map<String, Object>> executeGenericSelect(final String pSelectStatement) {


		try {
			if (pSelectStatement == null || pSelectStatement.isEmpty()) {
				throw new IllegalArgumentException("Parameter [pSelectStatement] must not be null or empty.");
			}
		} catch (IllegalArgumentException e) {
			throw new DataAccessException(DataAccessException.Type.PARAMETERS_MUST_NOT_BE_NULL, e);
		}

		boolean success = true;
		final List<Map<String, Object>> recordList = new ArrayList<Map<String, Object>>();
		OracleConnection con = null;
		OraclePreparedStatement pstmt = null;

		try {
			con = DataSourceProvider.getConnection();
			pstmt = (OraclePreparedStatement) con.prepareStatement(pSelectStatement);
			final ResultSet resultSet = pstmt.executeQuery();
			final ResultSetMetaData metaData = resultSet.getMetaData();
			final int columnCount = metaData.getColumnCount();
			while (resultSet.next()) {
				final Map<String, Object> record = new HashMap<String, Object>();
				for (int i = 1; i <= columnCount; i++) {
					final String columnName = metaData.getColumnName(i);
					final Object value = resultSet.getObject(columnName);
					record.put(columnName, value);
				}
				recordList.add(record);
			}
			return recordList;
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

	private void issueUpdateCountException(final int pUpdateCount, final int pPassedRows) {

		StringBuilder message = new StringBuilder();
		message.append("Number of rows processed by database does not match number of passed rows. [");
		message.append(pUpdateCount);
		message.append(", ");
		message.append(pPassedRows);
		message.append("]");
		throw new DataAccessException(DataAccessException.Type.COULD_NOT_ACCESS_DATA, message.toString());
	}

	private void bindParameters(final OracleConnection pCon,
								final Map<String, Object> pInParameters,
								final OracleCallableStatement pCstmt,
								final int pParameterIndex) throws SQLException {

		int parameterIndex = pParameterIndex;
		Map<String, Object> orderedParameters = new TreeMap<String, Object>(pInParameters);
		for (final String paramName : orderedParameters.keySet()) {
			Object value = orderedParameters.get(paramName);
			if (value == null) {
				continue;
			}
			if (value instanceof Long[]) {
				final ArrayDescriptor descriptor =
						ArrayDescriptor.createDescriptor(NOORM_ID_LIST_ORACLE_TYPE_NAME, pCon);
				final ARRAY arrayToPass = new ARRAY(descriptor, pCon, value);
				if (USE_NAMED_PARAMETERS) {
					// The following works for the Oracle JDBC 11.2.0.1.0 driver, but is actually not correct,
					// since named parameter binding should use the setXXXAtName methods (which does NOT work).
					pCstmt.setARRAY(paramName, arrayToPass);
				} else {
					pCstmt.setARRAY(parameterIndex++, arrayToPass);
				}
				continue;
			}
			if (value instanceof String) {
				if ((value).equals("")) {
					continue;
				} else {
					value = ((String) value).trim();
				}
			}
			if (value instanceof byte[]) {
				if (((byte[]) value).length == 0) {
					continue;
				}
			}
			if (value instanceof java.util.Date) {
				value = new Timestamp(((java.util.Date) value).getTime());
			}
			if (USE_NAMED_PARAMETERS) {
				// The following works for the Oracle JDBC 11.2.0.1.0 driver, but is actually not correct,
				// since named parameter binding should use the setXXXAtName methods (which does NOT work).
				pCstmt.setObject(paramName, value);
			} else {
				pCstmt.setObject(parameterIndex++, value);
			}
		}
	}

	private void debugPLSQLCall(final String pPLSQLCallable,
								final Map<String, Object> pInParameters,
								final Class<T> pBeanClass) {

		StringBuilder formattedParameters = new StringBuilder();
		formattedParameters.append("Calling PL/SQL procedure ").append(pPLSQLCallable);
		if (pInParameters != null) {
			String prefix = "\nInput parameters: ";
			for (final String paramName : pInParameters.keySet()) {
				String parameterToString;
				Object parameter = pInParameters.get(paramName);
				if (parameter instanceof byte[]) {
					HexBinaryAdapter hexBinaryAdapter = new HexBinaryAdapter();
					parameterToString = hexBinaryAdapter.marshal((byte[]) parameter);
				} else {
					if (parameter instanceof Long[]) {
						StringBuilder formattedIDList = new StringBuilder();
						String delimiter = "";
						for (final Long id : (Long[]) parameter) {
							formattedIDList.append(delimiter).append(id);
							delimiter = ", ";
						}
						parameterToString = formattedIDList.toString();
					} else {
						Object parameterValue = pInParameters.get(paramName);
						if (parameterValue != null) {
							parameterToString = parameterValue.toString();
						} else {
							parameterToString = "NULL";
						}
					}
				}
				formattedParameters.append(prefix).append(paramName).append(" : ").append(parameterToString);
				prefix = "\n                  ";
			}
		}
		if (pBeanClass != null) {
			formattedParameters.append("\nBean Class:        ").append(pBeanClass.getName());
		}
		log.debug(formattedParameters.toString());
	}

	private void debugPLSQLTermination(final String pPLSQLCallable,
									   final int pRowsProcessed) {

		StringBuilder logMessage = new StringBuilder();
		logMessage.append("PL/SQL procedure ").append(pPLSQLCallable).append(" successfully terminated. ");
		if (pRowsProcessed >= 0) {
			logMessage.append(Integer.toString(pRowsProcessed)).append(" rows processed.");
		}
		log.debug(logMessage.toString());
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
