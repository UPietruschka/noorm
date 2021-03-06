package org.noorm.jdbc;

import org.noorm.jdbc.platform.IPlatform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private final Utils utils = new Utils();
    private final LoggingHelper loggingHelper = new LoggingHelper();

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

        return getBeanListFromSQL(pTableName, pInParameters, pBeanClass, pAcquireLock, null);
    }

    /**
     * Executes a generic SQL statement for the given table (or view) name with the given query parameters.
     * This functionality is designated to support the query declaration available in the Maven generator plugin.
     * Any complex SQL like joins is expected to be encapsulated within a database view definition and is not
     * supported here.
     *
     * @param pTableName the table or view name used for the SQL query
     * @param pQueryParameters the parameters for the where-clause of the SQL query
     * @param pBeanClass the return type
     * @param pAcquireLock flag to indicate, whether a write lock should be acquired for the retrieved records
     * @param pFilterExtension additional parameters for paging and sorting
     * @return a list containing the results of type pBeanClass
     */
    public List<T> getBeanListFromSQL(final String pTableName,
                                      final Map<QueryColumn, Object> pQueryParameters,
                                      final Class<T> pBeanClass,
                                      final boolean pAcquireLock,
                                      final FilterExtension pFilterExtension) {
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
            loggingHelper.debugSQLCall(pTableName, pQueryParameters, pBeanClass, pFilterExtension);
        }

        boolean success = true;
        List<T> beanList;
        Connection con = null;
        PreparedStatement pstmt = null;
        try {
            con = DataSourceProvider.getConnection();
            final IPlatform platform = DataSourceProvider.getPlatform();
            if (pFilterExtension != null) {
                final Map<String, String> javaNames2ColumnNames = BeanMetaDataUtil.getJavaNames2ColumnNames(pBeanClass);
                for (final FilterExtension.SortCriteria sortCriteria : pFilterExtension.getSortCriteria()) {
                    final String attributeName = sortCriteria.getAttributeName();
                    final String columnName = javaNames2ColumnNames.get(attributeName);
                    if (columnName == null || attributeName == null) {
                        throw new DataAccessException(DataAccessException.Type.ILLEGAL_SORT_CRITERIA);
                    }
                    sortCriteria.setColumnName(columnName);
                }
            }
            final String sqlStmt = platform.buildSQLStatement
                    (pTableName, pQueryParameters, USE_NAMED_PARAMETERS, pAcquireLock, pFilterExtension);
            if (log.isDebugEnabled()) {
                log.debug("Preparing and executing SQL statement: ".concat(sqlStmt)
                        .concat("; using connection : ".concat(con.toString())));
            }
            pstmt = con.prepareStatement(sqlStmt);

            int parameterIndex = 1;
            utils.setQueryParameter(pQueryParameters, pstmt, parameterIndex);
            ResultSet rs = pstmt.executeQuery();

            final BeanMapper<T> mapper = BeanMapper.getInstance();
            boolean fetchPagingTotal = false;
            if (pFilterExtension != null) {
                fetchPagingTotal = pFilterExtension.isPagingTotalSupported();
            }
            beanList = mapper.toBeanList(rs, pBeanClass, fetchPagingTotal);
            if (beanList.isEmpty()) {
                beanList = new ArrayList<>();
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
		final List<Map<String, Object>> recordList = new ArrayList<>();
		Connection con = null;
		PreparedStatement pstmt = null;

		try {
			con = DataSourceProvider.getConnection();
			pstmt = con.prepareStatement(pSelectStatement);
			final ResultSet resultSet = pstmt.executeQuery();
			final ResultSetMetaData metaData = resultSet.getMetaData();
			final int columnCount = metaData.getColumnCount();
			while (resultSet.next()) {
				final Map<String, Object> record = new HashMap<>();
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
