package org.noorm.jdbc;

import oracle.jdbc.OracleCallableStatement;
import oracle.jdbc.OracleConnection;
import oracle.jdbc.OraclePreparedStatement;
import oracle.jdbc.OracleTypes;
import oracle.sql.ARRAY;
import oracle.sql.ArrayDescriptor;
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

	private static JDBCStatementProcessor statementProcessor;

	private StatementBuilder statementBuilder = new StatementBuilder();

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

		synchronized (JDBCStatementProcessor.class) {
			if (statementProcessor == null) {
				log.debug("Instantiating JDBCStatementProcessor.");
				statementProcessor = new JDBCStatementProcessor<T>();
			}
		}
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
			debugPLSQLCall(pPLSQLCallable, pInParameters, null, null);
		}

		boolean success = true;
		OracleConnection con = null;
		OracleCallableStatement cstmt = null;
		try {
			con = DataSourceProvider.getConnection();
			final String plSQLCall = statementBuilder.buildPLSQLCall
					(pPLSQLCallable, pOutParamName, pInParameters, null, USE_NAMED_PARAMETERS);
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
				bindParameters(pInParameters, cstmt, parameterIndex);
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
		} catch (SQLException e) {
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
				(pPLSQLCallable, pRefCursorName, pInParameters, null, null, pBeanClass);
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
	 * @param pInParameters  the map containing all IN parameters.
	 * @param pBeanClass	 the type of the Bean matching the fields of the ResultSet.
	 * @return The list of Beans containing the retrieved data.
	 */
	public List<T> getBeanListFromPLSQL(final String pPLSQLCallable,
										final String pRefCursorName,
										final Map<String, Object> pInParameters,
										final Class<T> pBeanClass) {

		return getBeanListFromPLSQL(pPLSQLCallable, pRefCursorName, pInParameters, null, null, pBeanClass);
	}

	/**
	 * Calls a PL/SQL procedure with a ref cursor as OUT parameter.
	 *
	 * @param pPLSQLCallable the name of PL/SQL procedure or the notation PACKAGENAME.PROCEDURE.
	 * @param pRefCursorName the parameter name of the procedure out parameter ref cursor.
	 * @param pIDListName	the a list of IDs uniquely identifying the records.
	 * @param pIDArray	   the array containing the Ids of type Long.
	 * @param pBeanClass	 the type of the Bean matching the fields of the ResultSet.
	 * @return The list of Beans containing the retrieved data.
	 */
	public List<T> getBeanListFromPLSQL(final String pPLSQLCallable,
										final String pRefCursorName,
										final String pIDListName,
										final Long[] pIDArray,
										final Class<T> pBeanClass) {

		return getBeanListFromPLSQL(pPLSQLCallable, pRefCursorName, null, pIDListName, pIDArray, pBeanClass);
	}

	private List<T> getBeanListFromPLSQL(final String pPLSQLCallable,
										 final String pRefCursorName,
										 final Map<String, Object> pInParameters,
										 final String pIDListName,
										 final Long[] pIDArray,
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
			if (pInParameters == null && pIDArray == null) {
				throw new IllegalArgumentException("Parameter [pInParameters,pIDList] must not be null.");
			}
		} catch (IllegalArgumentException e) {
			throw new DataAccessException(DataAccessException.Type.PARAMETERS_MUST_NOT_BE_NULL, e);
		}

		if (log.isDebugEnabled()) {
			debugPLSQLCall(pPLSQLCallable, pInParameters, pIDArray, pBeanClass);
		}

		boolean success = true;
		List<T> beanList;
		OracleConnection con = null;
		OracleCallableStatement cstmt = null;
		try {
			con = DataSourceProvider.getConnection();
			final String plSQLCall = statementBuilder.buildPLSQLCall
					(pPLSQLCallable, pRefCursorName, pInParameters, pIDListName, USE_NAMED_PARAMETERS);
			cstmt = (OracleCallableStatement) con.prepareCall(plSQLCall);

			int parameterIndex = 1;
			if (USE_NAMED_PARAMETERS) {
				cstmt.registerOutParameter(pRefCursorName, oracle.jdbc.OracleTypes.CURSOR);
			} else {
				cstmt.registerOutParameter(parameterIndex++, oracle.jdbc.OracleTypes.CURSOR);
			}

			// Check, whether the parameter map contains an idList or a named parameter map.
			if (pInParameters != null) {
				bindParameters(pInParameters, cstmt, parameterIndex);
			} else { // pIDList provided
				if (pIDListName == null || pIDListName.isEmpty()) {
					try {
						throw new IllegalArgumentException("Parameter [pIDListName] must not be null.");
					} catch (IllegalArgumentException e) {
						throw new DataAccessException(DataAccessException.Type.PARAMETERS_MUST_NOT_BE_NULL, e);
					}
				}
				final ArrayDescriptor descriptor =
						ArrayDescriptor.createDescriptor(NOORM_ID_LIST_ORACLE_TYPE_NAME, con);
				final ARRAY arrayToPass = new ARRAY(descriptor, con, pIDArray);
				if (USE_NAMED_PARAMETERS) {
					// The following works for the Oracle JDBC 11.2.0.1.0 driver, but is actually not correct,
					// since named parameter binding should use the setXXXAtName methods (which does NOT work).
					cstmt.setARRAY(pIDListName, arrayToPass);
				} else {
					cstmt.setARRAY(parameterIndex++, arrayToPass);
				}
			}

			cstmt.execute();

			ResultSet rs = null;
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
		} catch (SQLException e) {
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
	 * @return the generated primary key for this record.
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
	 */
	public void update(final IBean pBean) {

		List<IBean> beanList = new ArrayList<IBean>();
		beanList.add(pBean);
		update(beanList);
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

		boolean returnGeneratedKey = false;
		boolean success = true;
		OracleConnection con = null;
		OraclePreparedStatement pstmt = null;

		try {
			con = DataSourceProvider.getConnection();
			final IBean firstBean = pBeanList.get(0);
			String tableName = firstBean.getTableName();
			final String primaryKeyColumnName = firstBean.getPrimaryKeyColumnName();
			// There is currently no full support for returning generated keys in batch operation
			// Thus we support this for single-row inserts only.
			if (pBeanList.size() == 1 && pBatchType.equals(BatchType.INSERT) &&
				primaryKeyColumnName != null && !primaryKeyColumnName.isEmpty()) {
				returnGeneratedKey = true;
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
						(tableName, primaryKeyColumnName, sequenceName, fieldMap);
			}
			if (pBatchType.equals(BatchType.UPDATE)) {
				if (primaryKeyColumnName == null || primaryKeyColumnName.isEmpty()) {
					throw new DataAccessException(DataAccessException.Type.GENERIC_UPDATE_NOT_SUPPORTED_WITHOUT_PK);
				}
				batch = statementBuilder.buildUpdate
						(tableName, primaryKeyColumnName, versionColumnName, fieldMap);
			}
			if (pBatchType.equals(BatchType.DELETE)) {
				if (primaryKeyColumnName == null || primaryKeyColumnName.isEmpty()) {
					throw new DataAccessException(DataAccessException.Type.GENERIC_DELETE_NOT_SUPPORTED_WITHOUT_PK);
				}
				batch = statementBuilder.buildDelete
						(tableName, primaryKeyColumnName, versionColumnName);
			}
			if (log.isDebugEnabled()) {
				debugDML(tableName, sequenceName, batch);
			}
			if (returnGeneratedKey) {
				pstmt = (OraclePreparedStatement)
						con.prepareStatement(batch, new String[]{primaryKeyColumnName, versionColumnName});
			} else {
				pstmt = (OraclePreparedStatement) con.prepareStatement(batch);
			}
			// We do "Oracle style" batching here, which is easier to implement and superior in performance
			// as well. Through presetting the batch size (setExecuteBatch), we do not have to care for the
			// regular database updates using "executeBatch". Note that "executeUpdate" does NOT issue a
			// direct database update, but adds the given statement to the batch list (Neither "addBatch"
			// nor "executeBatch" are needed for "Oracle style" batching).
			if (!returnGeneratedKey)  {
				pstmt.setExecuteBatch(DataSourceProvider.getBatchUpdateSize());
			}
			int batchCount = 0;
			for (final IBean bean : pBeanList) {

				fieldMap = mapper.toMap(bean);
				for (final String fieldName : fieldMap.keySet()) {

					Object value = fieldMap.get(fieldName);
					if (value instanceof java.util.Date) {
						value = new Timestamp(((java.util.Date) value).getTime());
					}
					if (pBatchType.equals(BatchType.INSERT)) {
						if (!fieldName.toUpperCase().equals(primaryKeyColumnName) ||
							sequenceName == null || sequenceName.isEmpty()) {
							if (fieldName.equals(versionColumnName)) {
								// When the version column has not been initialized by the caller,
								// we set it here, otherwise NULL in the version column will result
								// in an OPTIMISTIC_LOCK_CONFLICT with the next update.
								if (value == null) {
									value = VERSION_COLUMN_DEFAULT;
								}
							}
							pstmt.setObjectAtName(fieldName, value);
						}
					}

					if (pBatchType.equals(BatchType.UPDATE)) {
						if (fieldName.equals(versionColumnName)) {
							if (value instanceof Long) {
								final Long incVersion = ((Long) value) + 1L;
								pstmt.setObjectAtName(fieldName, incVersion);
							} else {
								if (value == null) {
									throw new DataAccessException(DataAccessException.Type.VERSION_COLUMN_NULL);
								}
								throw new DataAccessException(DataAccessException.Type.UNSUPPORTED_VERSION_COLUMN_TYPE);
							}
						} else {
							if (pBatchType.equals(BatchType.UPDATE)) {
								pstmt.setObjectAtName(fieldName, value);
							}
						}
					}
					if (pBatchType.equals(BatchType.DELETE)) {
						if (fieldName.toUpperCase().equals(primaryKeyColumnName)) {
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
			if (returnGeneratedKey) {
				ResultSet generatedKeyResultSet = pstmt.getGeneratedKeys();
				while(generatedKeyResultSet.next()) {
					Long generatedKey = generatedKeyResultSet.getLong(1);
					log.debug("Generated key value " + generatedKey + " retrieved for table " + tableName);
					BeanMetaDataUtil.setPrimaryKeyValue(firstBean, generatedKey);
					// We must handle the value for the version column, too. When the caller does not set
					// the version column, we set it it to VERSION_COLUMN_DEFAULT. To reflect this in the
					// returned record, we must set it here, too. NOTE THAT ENABLING THE RETURN OF THE
					// GENERATED KEYS FOR ALL RECORDS, SOME MORE CHANGES ARE REQUIRED!
					Long versionColumnValue = generatedKeyResultSet.getLong(2);
					log.debug("Version column value " + versionColumnValue + " retrieved for table " + tableName);
					BeanMetaDataUtil.setVersionColumnValue(firstBean, versionColumnValue);
					return (T) firstBean;
				}
			}
			return null;
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
			debugPLSQLCall(pPLSQLCallable, null, null, beanClass);
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
		} catch (SQLException e) {
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

	private void bindParameters(final Map<String, Object> pInParameters,
								final OracleCallableStatement pCstmt,
								final int pParameterIndex) throws SQLException {

		int parameterIndex = pParameterIndex;
		Map<String, Object> orderedParameters = new TreeMap<String, Object>(pInParameters);
		for (final String paramName : orderedParameters.keySet()) {
			Object filterValue = orderedParameters.get(paramName);
			if (filterValue == null) {
				continue;
			}
			if (filterValue instanceof String) {
				if ((filterValue).equals("")) {
					continue;
				} else {
					filterValue = ((String) filterValue).trim();
				}
			}
			if (filterValue instanceof byte[]) {
				if (((byte[]) filterValue).length == 0) {
					continue;
				}
			}
			if (filterValue instanceof java.util.Date) {
				filterValue = new Timestamp(((java.util.Date) filterValue).getTime());
			}
			if (USE_NAMED_PARAMETERS) {
				// The following works for the Oracle JDBC 11.2.0.1.0 driver, but is actually not correct,
				// since named parameter binding should use the setXXXAtName methods (which does NOT work).
				pCstmt.setObject(paramName, filterValue);
			} else {
				pCstmt.setObject(parameterIndex++, filterValue);
			}
		}
	}

	private void debugPLSQLCall(final String pPLSQLCallable,
								final Map<String, Object> pInParameters,
								final Long[] pIDArray,
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
					Object parameterValue = pInParameters.get(paramName);
					if (parameterValue != null) {
						parameterToString = parameterValue.toString();
					} else {
						parameterToString = "NULL";
					}
				}
				formattedParameters.append(prefix).append(paramName).append(" : ").append(parameterToString);
				prefix = "\n                  ";
			}
		}
		if (pIDArray != null) {
			String prefix = "\nInput ID List:    ";
			for (final Long id : pIDArray) {
				formattedParameters.append(prefix).append(id);
				prefix = ", ";
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
