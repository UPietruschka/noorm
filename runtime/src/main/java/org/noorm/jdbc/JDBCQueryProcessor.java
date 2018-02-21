package org.noorm.jdbc;

import org.noorm.jdbc.platform.IPlatform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
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
public class JDBCQueryProcessor<T> {

	private static final Logger log = LoggerFactory.getLogger(JDBCQueryProcessor.class);
    private static final boolean USE_NAMED_PARAMETERS = false;

	private static JDBCQueryProcessor queryProcessor = new JDBCQueryProcessor();

	private final StatementBuilder statementBuilder = new StatementBuilder();

	private JDBCQueryProcessor() { }

	public static <T> JDBCQueryProcessor<T> getInstance() {

		return queryProcessor;
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
     * @param pAcquireLock flag to indicate, whether a write lock should be acquired for the retrieved records
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
        Connection con = null;
        PreparedStatement pstmt = null;
        try {
            con = DataSourceProvider.getConnection();
            final String sqlStmt = statementBuilder.buildSQLStatement
                    (pTableName, pInParameters, USE_NAMED_PARAMETERS, pAcquireLock);
            if (log.isDebugEnabled()) {
                log.debug("Preparing and executing SQL statement: ".concat(sqlStmt)
                        .concat("; using connection : ".concat(con.toString())));
            }
            pstmt = con.prepareStatement(sqlStmt);

            int parameterIndex = 1;
            final Map<QueryColumn, Object> orderedParameters = new TreeMap<QueryColumn, Object>(pInParameters);
            final IPlatform platform = DataSourceProvider.getPlatform();
            for (final QueryColumn queryColumn : orderedParameters.keySet()) {
                if (!queryColumn.getOperator().isUnary()) {
                    Object value = orderedParameters.get(queryColumn);
                    if (value instanceof java.util.Date) {
                        value = new Timestamp(((java.util.Date) value).getTime());
                    }
                    if (value != null) {
                        platform.setObject(pstmt, value, parameterIndex++, -1);
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
                    DataSourceProvider.returnConnection(success);
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
		Connection con = null;
		PreparedStatement pstmt = null;

		try {
			con = DataSourceProvider.getConnection();
			pstmt = con.prepareStatement(pSelectStatement);
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
					DataSourceProvider.returnConnection(success);
				}
			} catch (SQLException ignored) {
			} // Nothing to do
		}
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
                String parameterToString = Utils.getParameter2String(parameter);
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

    private void debugSQLTermination(final String pTableName,
                                     final int pRowsProcessed) {

        StringBuilder logMessage = new StringBuilder();
        logMessage.append("SQL statement on table ").append(pTableName).append(" successfully terminated. ");
        if (pRowsProcessed >= 0) {
            logMessage.append(Integer.toString(pRowsProcessed)).append(" rows processed.");
        }
        log.debug(logMessage.toString());
    }
}
