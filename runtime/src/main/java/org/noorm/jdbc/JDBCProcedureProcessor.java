package org.noorm.jdbc;

import org.noorm.jdbc.platform.IPlatform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
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
public class JDBCProcedureProcessor<T> {

	private static final Logger log = LoggerFactory.getLogger(JDBCProcedureProcessor.class);

	private static JDBCProcedureProcessor statementProcessor = new JDBCProcedureProcessor();

	private final StatementBuilder statementBuilder = new StatementBuilder();

	private JDBCProcedureProcessor() {
	}

	public static <T> JDBCProcedureProcessor<T> getInstance() {

		return statementProcessor;
	}

	/**
	 * Call a PL/SQL procedure without OUT parameter.
	 *
	 * @param pCallable the name of PL/SQL procedure or the notation PACKAGENAME.PROCEDURE.
	 * @param pInParameters  the map containing all IN parameters.
	 */
	public void callProcedure(final String pCallable,
                              final Map<String, Object> pInParameters) {

		callProcedure(pCallable, null, pInParameters, null);
	}

	/**
	 * Call a PL/SQL procedure with a scalar OUT parameter or without OUT parameter.
	 *
	 * @param pCallable the name of PL/SQL procedure or the notation PACKAGENAME.PROCEDURE.
	 * @param pOutParamName  the name of the scalar OUT parameter.
	 * @param pInParameters  the map containing all IN parameters.
	 * @param pOutClass	  the type of the scalar OUT parameter.
	 * @return The scalar data-type returned by the PL/SQL procedure, if any.
	 */
	public T callProcedure(final String pCallable,
                           final String pOutParamName,
                           final Map<String, Object> pInParameters,
                           final Class<T> pOutClass) {

		try {
			if (pCallable == null || pCallable.isEmpty()) {
				throw new IllegalArgumentException("Parameter [pCallable] must not be null.");
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
			debugProcedureCall(pCallable, pInParameters, null);
		}

		boolean success = true;
		Connection con = null;
		CallableStatement cstmt = null;
		try {
			con = DataSourceProvider.getConnection();
			final String procedureCall = statementBuilder.buildProcedureCall
                    (pCallable, pOutParamName, pInParameters);
			if (log.isDebugEnabled()) {
				log.debug("Preparing and executing PL/SQL Call: ".concat(procedureCall)
                        .concat("; using connection : ".concat(con.toString())));
			}
			cstmt = con.prepareCall(procedureCall);

			int parameterIndex = 1;
			if (pOutParamName != null) {
				int type = Types.VARCHAR;
				if (pOutClass.getSuperclass().equals(Number.class)) {
					type = Types.NUMERIC;
				}
				if (pOutClass.isAssignableFrom(java.util.Date.class)) {
					type = Types.TIMESTAMP;
				}
                cstmt.registerOutParameter(parameterIndex++, type);
			}

			if (pInParameters != null) {
				bindParameters(pInParameters, cstmt, parameterIndex);
			}

			cstmt.execute();

			T outValue = null;
			if (pOutParamName != null) {
				outValue = getOutParameter(pOutClass, cstmt);
			}

			if (log.isDebugEnabled()) {
				debugProcedureTermination(pCallable, -1);
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
					DataSourceProvider.returnConnection(success);
				}
			} catch (SQLException ignored) {
			} // Nothing to do
		}

	}

	private T getOutParameter(final Class<T> pOutClass,
							  final CallableStatement cstmt) throws SQLException {

		T outValue = null;
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
	 * @param pCallable the name of PL/SQL procedure or the notation PACKAGENAME.PROCEDURE.
	 * @param pRefCursorName the parameter name of the procedure out parameter ref cursor.
	 * @param pInParameters  the map containing all IN parameters.
	 * @param pBeanClass	 the type of the Bean matching the fields of the ResultSet.
	 * @return The Beans containing the retrieved data.
	 */
	public T getBeanFromProcedure(final String pCallable,
                                  final String pRefCursorName,
                                  final Map<String, Object> pInParameters,
                                  final Class<T> pBeanClass) {

		final List<T> beanList = getBeanListFromProcedure
                (pCallable, pRefCursorName, pInParameters, pBeanClass);
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
	 * @param pCallable the name of PL/SQL procedure or the notation PACKAGENAME.PROCEDURE.
	 * @param pRefCursorName the parameter name of the procedure out parameter ref cursor.
	 * @param pBeanClass	 the type of the Bean matching the fields of the ResultSet.
	 * @return The list of Beans containing the retrieved data.
	 */
	public List<T> getBeanListFromProcedure(final String pCallable,
                                            final String pRefCursorName,
                                            final Class<T> pBeanClass) {

		return getBeanListFromProcedure(pCallable, pRefCursorName, null, pBeanClass);
	}

	public List<T> getBeanListFromProcedure(final String pCallable,
                                            final String pRefCursorName,
                                            final Map<String, Object> pInParameters,
                                            final Class<T> pBeanClass) {

		try {
			if (pCallable == null || pCallable.isEmpty()) {
				throw new IllegalArgumentException("Parameter [pCallable] must not be null.");
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
			debugProcedureCall(pCallable, pInParameters, pBeanClass);
		}

		boolean success = true;
		List<T> beanList;
		Connection con = null;
		CallableStatement cstmt = null;
		try {
			con = DataSourceProvider.getConnection();
			final IPlatform platform = DataSourceProvider.getPlatform();
			final String procedureCall = statementBuilder.buildProcedureCall
                    (pCallable, pRefCursorName, pInParameters);
			if (log.isDebugEnabled()) {
                log.debug("Preparing and executing PL/SQL Call: ".concat(procedureCall)
                        .concat("; using connection : ".concat(con.toString())));
			}
			cstmt = con.prepareCall(procedureCall);

			int parameterIndex = 1;
            cstmt.registerOutParameter(parameterIndex++, platform.getRefCursorJDBCType());

			bindParameters(pInParameters, cstmt, parameterIndex);

			cstmt.execute();

			ResultSet rs;
            rs = (ResultSet) cstmt.getObject(1);
			final BeanMapper<T> mapper = BeanMapper.getInstance();
			beanList = mapper.toBeanList(rs, pBeanClass, false);
			if (beanList.isEmpty()) {
				beanList = new ArrayList<T>();
			}
			rs.close();

			if (log.isDebugEnabled()) {
				debugProcedureTermination(pCallable, beanList.size());
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
					DataSourceProvider.returnConnection(success);
				}
			} catch (SQLException ignored) {
			} // Nothing to do
		}
	}

	private void bindParameters(final Map<String, Object> pInParameters,
								final CallableStatement pCstmt,
								final int pParameterIndex) throws SQLException {

		int parameterIndex = pParameterIndex;
		Map<String, Object> orderedParameters = new TreeMap<>(pInParameters);
		for (final String paramName : orderedParameters.keySet()) {
			Object value = orderedParameters.get(paramName);
			if (value == null) {
				continue;
			}
			if (value instanceof String) {
				if ((value).equals("")) {
					continue;
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
            pCstmt.setObject(parameterIndex++, value);
		}
	}

	public void debugProcedureCall(final String pCallable,
                                   final Map<String, Object> pInParameters,
                                   final Class<T> pBeanClass) {

		final StringBuilder formattedParameters = new StringBuilder();
		formattedParameters.append("Calling PL/SQL procedure ").append(pCallable);
		if (pInParameters != null) {
			String prefix = "\nInput parameters: ";
			for (final String paramName : pInParameters.keySet()) {
                final Object parameter = pInParameters.get(paramName);
                String parameterToString = Utils.getParameter2String(parameter);
				formattedParameters.append(prefix).append(paramName).append(" : ").append(parameterToString);
				prefix = "\n                  ";
			}
		}
		if (pBeanClass != null) {
			formattedParameters.append("\nBean Class:        ").append(pBeanClass.getName());
		}
		log.debug(formattedParameters.toString());
	}

    private void debugProcedureTermination(final String pCallable,
                                           final int pRowsProcessed) {

		StringBuilder logMessage = new StringBuilder();
		logMessage.append("PL/SQL procedure ").append(pCallable).append(" successfully terminated. ");
		if (pRowsProcessed >= 0) {
			logMessage.append(Integer.toString(pRowsProcessed)).append(" rows processed.");
		}
		log.debug(logMessage.toString());
	}
}
