package org.noorm.jdbc;

import oracle.jdbc.OracleCallableStatement;
import oracle.jdbc.OracleConnection;
import oracle.jdbc.OraclePreparedStatement;
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
 * This class only supports access to stored procedures and generic queries as specified with the Maven plugin.
 * Based on the expectation that all data-driven business logic is implemented in stored
 * procedures or views, there is no need for overcomplex SQL support.
 *
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 */
public class JDBCStatementProcessor<T> {

	private static final Logger log = LoggerFactory.getLogger(JDBCStatementProcessor.class);

	private static JDBCStatementProcessor statementProcessor = new JDBCStatementProcessor();

	private final StatementBuilder statementBuilder = new StatementBuilder();

	public static final String NOORM_ID_LIST_ORACLE_TYPE_NAME = "NUM_ARRAY";

	/*
	 * Using named parameters for callable statements does not work consistently over the various supported
	 * combinations of JDBC driver versions and database versions. In addition, a bug in Oracle 11.2.0.1.0
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
				log.debug("Preparing and executing PL/SQL Call: ".concat(plSQLCall)
                        .concat("; using connection : ".concat(con.toString())));
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

    /**
     * Executes a generic SQL statement for the given table (or view) name with the given query parameters.
     * This functionality is designated to support the query declaration available in the Maven generator plugin.
     * Any complex SQL like joins is expected to be encapsulated within a database view definition and is not
     * supported here.
     *
     * @param pTableName the table or view name used for the SQL query
     * @param pInParameters the parameters for the where-clause of the SQL query
     * @param pBeanClass the return type
     * @return a list containing the results of type pBeanClass
     */
    public List<T> getBeanListFromSQL(final String pTableName,
                                      final Map<QueryColumn, Object> pInParameters,
                                      final Class<T> pBeanClass,
                                      final boolean pAcquireLock) {
        try {
            if (pTableName == null || pTableName.isEmpty()) {
                throw new IllegalArgumentException("Parameter [pTableName] must not be null.");
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
            debugSQLCall(pTableName, pInParameters, pBeanClass);
        }

        boolean success = true;
        List<T> beanList;
        OracleConnection con = null;
        OraclePreparedStatement pstmt = null;
        try {
            con = DataSourceProvider.getConnection();
            final String sqlStmt = statementBuilder.buildSQLStatement
                    (pTableName, pInParameters, USE_NAMED_PARAMETERS, pAcquireLock);
            if (log.isDebugEnabled()) {
                log.debug("Preparing and executing SQL statement: ".concat(sqlStmt)
                        .concat("; using connection : ".concat(con.toString())));
            }
            pstmt = (OraclePreparedStatement) con.prepareStatement(sqlStmt);

            int parameterIndex = 1;
            final Map<QueryColumn, Object> orderedParameters = new TreeMap<QueryColumn, Object>(pInParameters);
            for (final QueryColumn queryColumn : orderedParameters.keySet()) {
                if (!queryColumn.getOperator().isUnary()) {
                    final Object value = orderedParameters.get(queryColumn);
                    if (value instanceof String) {
                        // SQL CHAR comparison semantics by default uses padding, which causes some
                        // confusion, since it does not even matter, whether the data has initially been
                        // provided with or without padding. Using the following proprietary Oracle method
                        // disabled this behaviour and turns off padding.
                        pstmt.setFixedCHAR(parameterIndex++, (String) value);
                    } else {
                        pstmt.setObject(parameterIndex++, value);
                    }
                }
            }
            ResultSet rs = pstmt.executeQuery();

            final BeanMapper<T> mapper = BeanMapper.getInstance();
            beanList = mapper.toBeanList(rs, pBeanClass);
            if (beanList.isEmpty()) {
                beanList = new ArrayList<T>();
            }
            rs.close();

            if (log.isDebugEnabled()) {
                debugSQLTermination(pTableName, beanList.size());
            }

            return beanList;
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
                log.debug("Preparing and executing PL/SQL Call: ".concat(plSQLCall)
                        .concat("; using connection : ".concat(con.toString())));
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

	public void debugPLSQLCall(final String pPLSQLCallable,
								final Map<String, Object> pInParameters,
								final Class<T> pBeanClass) {

		final StringBuilder formattedParameters = new StringBuilder();
		formattedParameters.append("Calling PL/SQL procedure ").append(pPLSQLCallable);
		if (pInParameters != null) {
			String prefix = "\nInput parameters: ";
			for (final String paramName : pInParameters.keySet()) {
                final Object parameter = pInParameters.get(paramName);
                String parameterToString = getParameter2String(parameter);
				formattedParameters.append(prefix).append(paramName).append(" : ").append(parameterToString);
				prefix = "\n                  ";
			}
		}
		if (pBeanClass != null) {
			formattedParameters.append("\nBean Class:        ").append(pBeanClass.getName());
		}
		log.debug(formattedParameters.toString());
	}

    private void debugSQLCall(final String pTableName,
                              final Map<QueryColumn, Object> pInParameters,
                              final Class<T> pBeanClass) {

        final StringBuilder formattedParameters = new StringBuilder();
        formattedParameters.append("Executing SQL statement on table ").append(pTableName);
        if (pInParameters != null) {
            String prefix = "\nInput parameters: ";
            for (final QueryColumn queryColumn : pInParameters.keySet()) {
                final String paramName = queryColumn.getColumnName();
                final Object parameter = pInParameters.get(queryColumn);
                String parameterToString = getParameter2String(parameter);
                formattedParameters.append(prefix).append(paramName)
                        .append(queryColumn.getOperator().getOperatorSyntax()).append(parameterToString);
                prefix = "\n                  ";
            }
        }
        if (pBeanClass != null) {
            formattedParameters.append("\nBean Class:        ").append(pBeanClass.getName());
        }
        log.debug(formattedParameters.toString());
    }

    private String getParameter2String(final Object pParameter) {

        String parameterToString;
        if (pParameter instanceof byte[]) {
            if (((byte[]) pParameter).length < 4096) {
                final HexBinaryAdapter hexBinaryAdapter = new HexBinaryAdapter();
                parameterToString = hexBinaryAdapter.marshal((byte[]) pParameter);
            } else {
                parameterToString = "Binary content too large for debug output.";
            }
        } else {
            if (pParameter instanceof Long[]) {
                final StringBuilder formattedIDList = new StringBuilder();
                String delimiter = "";
                for (final Long id : (Long[]) pParameter) {
                    formattedIDList.append(delimiter).append(id);
                    delimiter = ", ";
                }
                parameterToString = formattedIDList.toString();
            } else {
                if (pParameter != null) {
                    parameterToString = pParameter.toString();
                } else {
                    parameterToString = "NULL";
                }
            }
        }
        return parameterToString;
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

    private void debugSQLTermination(final String pTableName,
                                     final int pRowsProcessed) {

        StringBuilder logMessage = new StringBuilder();
        logMessage.append("SQL satement on table ").append(pTableName).append(" successfully terminated. ");
        if (pRowsProcessed >= 0) {
            logMessage.append(Integer.toString(pRowsProcessed)).append(" rows processed.");
        }
        log.debug(logMessage.toString());
    }
}
