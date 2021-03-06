package org.noorm.jdbc;

import org.noorm.jdbc.platform.IPlatform;
import org.noorm.jdbc.platform.PlatformFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * DataSourceProvider manages data sources and controls transactions in the NoORM runtime engine.
 * Data sources are typically configured in the NoORM configuration file ("noorm.xml" or "noorm.properties").
 * Alternatively, data sources can be added using static method addDataSource. Due to the complex nature of
 * database resource handling in a running system, data sources, which have been added to the system cannot
 * be removed.
 * DataSourceProvider is a Java singleton, which takes over full control over the database connections. Direct
 * usage of database connections is possible for dedicated purposes (e.g. integration with AQ), but
 * discouraged for typical database access operations. The public API for DataSourceProvider is simple to use
 * and primarily provides the required functionality for explicit transaction handling using the methods begin(),
 * commit() and rollback(). Without explicit transaction handling, DataSourceProvider manages transactions
 * automatically by issuing an auto-commit after every database call. Issuing a commit even for read-only
 * database access imposes a little performance overhead. However, for most usage scenarios, this overhead is
 * not significant.
 * Explicit transaction handling (using begin(), commit(), rollback()) is managed by maintaining the JDBC
 * connection in a ThreadLocal variable. This removes the burden of connection control from the application
 * programmer, since DataSourceProvider will keep track of the current transaction beyond class and method
 * boundaries in the application.
 * Using explicit transaction control requires the application programmer to control transaction termination
 * properly, i.e., the application must guarantee that a transaction started with begin() will always be
 * terminated with commit() or rollback().
 *
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 */
public class DataSourceProvider {

	private static final Logger log = LoggerFactory.getLogger(DataSourceProvider.class);

    private static final Map<String, ActiveDataSource> activeDataSourceMap = new HashMap<String, ActiveDataSource>();
    private static final ThreadLocal<ActiveConnectionData> activeConThreadDta = new ThreadLocal<ActiveConnectionData>();
    private static final DataSourceProvider dataSourceProvider = new DataSourceProvider();
    private static IPlatform platform;

	private DataSourceProvider() {

        log.info("Instantiating NoORM DataSourceProvider.");
        final ConfigurationInitializer configurationInitializer = new ConfigurationInitializer();
        final Map<String, DataSourceConfiguration> dataSourceConfigurations = configurationInitializer.init();
        for (final String dataSourceName : dataSourceConfigurations.keySet()) {
            final DataSourceConfiguration dataSourceConfiguration = dataSourceConfigurations.get(dataSourceName);
            final ActiveDataSource activeDataSource = new ActiveDataSource();
            activeDataSource.setName(dataSourceName);
            activeDataSource.setConfiguration(dataSourceConfiguration);
            activeDataSourceMap.put(dataSourceName, activeDataSource);
        }
    }

    private static DataSourceConfiguration getActiveConfiguration() {

        return getActiveConnectionData().getActiveDataSource().getConfiguration();
    }

    /**
     * The active connection data contains the currently active data source and the database connection, if the
     * connection is retained for an explicitly managed transaction. The active connection data is stored in a
     * ThreadLocal variable. When a new thread acquires a database connection, a new ActiveConnectionData object
     * is instantiated. The active data source is automatically assigned, when only one data source has been
     * configured or when the associated ActiveDataSource object is provided.
     *
     * @return the currently active connection data
     */
    private static ActiveConnectionData getActiveConnectionData() {

        ActiveConnectionData activeConnectionData = activeConThreadDta.get();
        if (activeConnectionData == null) {
            activeConnectionData = new ActiveConnectionData();
            // In case of a single configured data source, we activate this data source as default data source.
            if (activeDataSourceMap.size() == 1) {
                log.info("Activating default data source.");
                ActiveDataSource activeDataSource = activeDataSourceMap.values().iterator().next();
                activeConnectionData.setActiveDataSource(activeDataSource);
            } else {
                // In case of multiple configured data sources (or none), the caller must activate the
                // data source to decide, which one to use.
                throw new DataAccessException(DataAccessException.Type.NO_ACTIVE_DATA_SOURCE);
            }
            activeConThreadDta.set(activeConnectionData);
        }
        return activeConnectionData;
    }

	/**
	 * Removes the active connection data hold in a ThreadLocal variable of the DataSourceProvider.
	 * THIS METHOD SHOULD ONLY BE CALLED for thread clean up, i.e., from ServletRequestListener.requestDestroyed
	 * implementations!
	 */
	public static void clearActiveConnectionData() {

    	activeConThreadDta.remove();
	}

    /**
     * Activates the data source with the given name.
     * The name of the data source is either the name provided for the data source in the NoORM configuration
     * file (noorm.xml or noorm.properties) or the name provided, when the data source been added using method
     * addDataSource.
     *
     * @param pDataSourceName the name of the data source to be activated.
     */
    public static void setActiveDataSource(final String pDataSourceName) {

        final ActiveDataSource activeDataSource = activeDataSourceMap.get(pDataSourceName);
        if (activeDataSource == null) {
            throw new DataAccessException(DataAccessException.Type.UNKNOWN_DATA_SOURCE);
        }
        ActiveConnectionData activeConnectionData = activeConThreadDta.get();
        if (activeConnectionData != null) {
            // Switching the data source in the middle of user-managed transaction may lead to
            // unpredictable results and is not supported.
            final String oldDataSourceName = activeConnectionData.getActiveDataSource().getName();
            if (!oldDataSourceName.equals(pDataSourceName)) {
                final Long tsStackThreadLocal = activeConnectionData.getTsStack();
                if (tsStackThreadLocal != null && tsStackThreadLocal > 0L) {
                    throw new DataAccessException(DataAccessException.Type.INVALID_DATA_SOURCE_SWITCH);
                }
            }
        } else {
            activeConnectionData = new ActiveConnectionData();
            activeConThreadDta.set(activeConnectionData);
        }
        activeConnectionData.setActiveDataSource(activeDataSource);
    }

    /**
     * Adds and optionally activates a new data source to be controlled by the DataSourceProvider.
     *
     * @param pDataSource the pre-configured data source.
     * @param pDataSourceName the name of the data source. Used by method setActiveDataSource.
     * @param pPlatformName the database platform name
     * @param pActivate determines, whether the data source should be activated right after it has been added.
     */
	public static void addDataSource(final DataSource pDataSource,
                                     final String pDataSourceName,
                                     final String pPlatformName,
                                     final boolean pActivate) {

        ActiveDataSource activeDataSource = activeDataSourceMap.get(pDataSourceName);
        if (activeDataSource != null) {
            throw new DataAccessException(DataAccessException.Type.DATA_SOURCE_ALREADY_ADDED);
        }
        validateDataSource(pDataSource, pPlatformName);
        activeDataSource = new ActiveDataSource();
        final DataSourceConfiguration configuration = new DataSourceConfiguration();
        activeDataSource.setName(pDataSourceName);
        activeDataSource.setDataSource(pDataSource);
        activeDataSource.setConfiguration(configuration);
        activeDataSourceMap.put(pDataSourceName, activeDataSource);
        if (pActivate) {
            setActiveDataSource(pDataSourceName);
        }
    }

	/**
	 * Validate the data source. Either for the data source submitted using setDataSource or for the
	 * data source constructed using the NoORM properties, a connection is established to validate
	 * the data source.
	 */
	private static void validateDataSource(final DataSource dataSource, final String pPlatform) {

		try {
            if (platform == null) {
                platform = PlatformFactory.createPlatform(dataSource.getClass(), pPlatform);
            }
            log.info(platform.validateDataSource(dataSource));
            java.sql.Connection con = dataSource.getConnection();
            con.close();
		} catch (Exception e) {
			throw new DataAccessException(DataAccessException.Type.COULD_NOT_ESTABLISH_CONNECTION, e);
		}
	}

    public static IPlatform getPlatform() {
        return platform;
    }

	private static DataSource getDataSource() throws SQLException {

        final ActiveConnectionData activeConnectionData = getActiveConnectionData();
        final ActiveDataSource activeDataSource = activeConnectionData.getActiveDataSource();
        synchronized (activeDataSource) {
            if (activeDataSource.getDataSource() == null) {
                final DataSource dataSource = dataSourceProvider.initDataSource(activeDataSource.getConfiguration());
                activeDataSource.setDataSource(dataSource);
            }
        }
        return activeDataSource.getDataSource();
	}

	/*
     * The data source parameters in the NoORM properties can either be specified using JNDI or directly
	 * by providing the required parameters for setting up an DataSource. When both types of
	 * configuration settings are available, the JNDI configuration has precedence.
	 */
	private DataSource initDataSource(final DataSourceConfiguration pDataSourceConfiguration) throws SQLException {

        log.debug("Initializing data source.");
        DataSource dataSource;
        pDataSourceConfiguration.validate();
		final String jndiName = pDataSourceConfiguration.getDatabaseJNDIName();

		if (jndiName != null) {

			try {
				log.info("Trying to establish data source using JNDI name ".concat(jndiName));
				final Context initCtx = new InitialContext();
                dataSource = (DataSource) initCtx.lookup(jndiName);
				log.info("JNDI data source lookup successful.");
			} catch (NamingException e) {
				throw new DataAccessException("JNDI data source lookup failed.", e);
			}

		} else {

            log.info("Trying to establish data source using NoORM configuration.");
            platform = PlatformFactory.createPlatform(pDataSourceConfiguration.getDatabasePlatform());
            final String url = pDataSourceConfiguration.getDatabaseURL();
            final String username = pDataSourceConfiguration.getDatabaseUsername();
            final String password = pDataSourceConfiguration.getDatabasePassword();
            dataSource = platform.getDataSource(url, username, password);
		}

		validateDataSource(dataSource, pDataSourceConfiguration.getDatabasePlatform());
        return dataSource;
	}

	static void returnConnection(final boolean pSuccess) {

		Connection con = getActiveConnectionData().getConnection();
		try {
            if (con == null || con.isClosed()) {
                throw new DataAccessException(DataAccessException.Type.STALE_TRANSACTION);
            }
			if (getActiveConnectionData().getTsStack() == 0L) {
				if (log.isDebugEnabled()) {
					log.debug("Returning connection to connection pool.");
				}
				// When closing the connection (resp. returning it to the connection pool, we
				// always issue a commit, since in any case, there is no meaningful transaction
				// scope beyond this point in time.
				// Committing even in case of a read only transaction may incur some additional
				// cost, since from a technical perspective, the commit is not necessary. On
				// the other hand, the DB reduces the cost in this case to a minimum, so that
				// the advantage of this way of transaction automation outweighs a possible
				// minimal performance reduction.
				if (pSuccess) {
                    con.commit();
				} else {
                    con.rollback();
				}
                con.close();
			}
		} catch (SQLException e) {
			throw new DataAccessException(DataAccessException.Type.CONNECTION_ACCESS_FAILURE, e);
		}
	}

	/**
	 * Returns the database connection currently managed by the DataSourceProvider.
	 * Note that the life-cycle of the connection is managed by the DataSourceProvider, i.e. acquiring a
	 * connection, closing a connection, resp. returning a connection to the connection pool and managing
	 * transaction boundaries SHOULD NOT BE DONE in the application code. Providing the database connection
	 * directly by this method has the sole purpose to support additional Java client software, which
	 * requires access to the connection (e.g. Streams Advanced Queueing, which requires the
	 * connection to create an AQSession).
	 *
	 * @return the database connection currently managed by the DataSourceProvider.
	 * @throws SQLException JDBC driver exception
	 */
	public static Connection getConnection() throws SQLException {

		return getConnection(false);
	}

	/**
	 * Retaining the connection is only required when starting a new transaction
	 * explicitly (indicating that the transaction is beyond the scope of a single
	 * PL/SQL call or insert or update operation). Having already a retained connection
	 * for this thread indicates that this transaction is beyond the scope of the
	 * caller.
	 *
	 * @param pRetain indicates, if the connection should be retained.
	 * @return the acquired connection.
	 * @throws SQLException
	 */
	private static Connection getConnection(final boolean pRetain) throws SQLException {

		Connection con = getActiveConnectionData().getConnection();
		if (getActiveConnectionData().getTsStack() == 0L) {
			if (log.isDebugEnabled()) {
				if (pRetain) {
					log.debug("Acquiring retainable connection from connection pool.");
				} else {
					log.debug("Acquiring new connection from connection pool.");
				}
			}

			java.sql.Connection dataSourceConn = getDataSource().getConnection();
		    con = dataSourceConn;
            if (log.isDebugEnabled()) {
                log.debug("Acquired connection : ".concat(con.toString()));
            }

			con.setAutoCommit(false);
            if (getActiveConfiguration().isDebugMode()) {
				enableDebugMode(con);
			}
            getActiveConnectionData().setConnection(con);
		}
		if (pRetain) {
			Long tsStackThreadLocal0 = getActiveConnectionData().getTsStack();
			if (tsStackThreadLocal0 == null || tsStackThreadLocal0 == 0L) {
                getActiveConnectionData().setTsStack(1L);
			} else {
                getActiveConnectionData().setTsStack(tsStackThreadLocal0 + 1L);
			}
			if (log.isDebugEnabled()) {
				log.debug("Setting transaction call stack level to "
                        .concat(getActiveConnectionData().getTsStack().toString()));
			}
		}
		return con;
	}

    /**
     * Verifies, whether an explicit (user managed) transaction is currently in progress, i.e., the application
     * has explicitly issued a "begin", but has not yet terminated the transaction with "commit" or "rollback".
     *
     * @return true, when a transaction is in progress
     */
    public static boolean activeUserManagedTransaction() {

        return getActiveConnectionData().getTsStack() > 0L;
    }

	/**
	 * To control transactions in the calling application, the connection in use must not
	 * turned back to the connection pool, but should be preserved in a ThreadLocal variable.
	 * This method is used to indicate that the calling application intends to broaden the
	 * transaction boundaries beyond the scope of a single PL/SQL or DML call. JDBC does not know
	 * about the classical "begin" call, since a database session has always an active
	 * transaction (after issuing a "commit" or "rollback", a new transaction is started
	 * implicitly).
	 */
	public static void begin() {

		try {
			getConnection(true);
		} catch (Exception e) {
			throw new DataAccessException(DataAccessException.Type.CONNECTION_ACCESS_FAILURE, e);
		}
	}

	/**
	 * Commits a user managed transaction.
	 * Connections are maintained in the connection pool.
	 * Note that using this method requires that a user managed transaction has been started using
	 * method begin(), other a DataAccessException is thrown.
	 * Transactions, which are not user managed are always committed automatically, when the connection
	 * is returned to the connection pool (restricting automated transaction management to transactions
	 * with a scope limited to a single call to PL/SQL, an insert, update or an delete operation.
	 */
	public static void commit() {

		Connection con = null;
		try {
			con = getActiveConnectionData().getConnection();
			if (con == null || con.isClosed()) {
				throw new DataAccessException(DataAccessException.Type.STALE_TRANSACTION);
			}
			// When the transaction stack counter indicates that the transaction is beyond the scope of
			// the caller, reduce the transactions stack counter and remain the connection open.
			Long tsStackThreadLocal0 = getActiveConnectionData().getTsStack();
			if (tsStackThreadLocal0 == null) {
				throw new DataAccessException(DataAccessException.Type.STALE_TRANSACTION);
			} else {
                getActiveConnectionData().setTsStack(tsStackThreadLocal0 - 1L);
			}
			if (log.isDebugEnabled()) {
				log.debug("Setting transaction call stack level to "
                        .concat(getActiveConnectionData().getTsStack().toString()));
			}
			if (getActiveConnectionData().getTsStack() == 0L) {
				if (log.isDebugEnabled()) {
					log.debug("Committing transaction");
				}
				con.commit();
                getActiveConnectionData().reset();
			} else {
				if (log.isDebugEnabled()) {
					log.debug("Delegating transaction termination to caller.");
				}
			}
		} catch (SQLException e) {
			throw new DataAccessException(DataAccessException.Type.CONNECTION_ACCESS_FAILURE, e);
		} finally {
			try {
				if (con != null && !con.isClosed()) {
					// Although the connection may get reused in the current thread, we do not know
					// here, if the thread will be closed. In the latter case we cannot influence,
					// when the connection is returned to the pool, so we do it here.
					if (getActiveConnectionData().getTsStack() == 0L) {
						if (log.isDebugEnabled()) {
							log.debug("Returning retained connection to connection pool.");
						}
						con.close();
					}
				}
			} catch (SQLException ignored) {
			} // Nothing to do
		}
	}

	/**
	 * Issues a rollback on a user managed transaction.
	 * See DataSourceProvider.commit();
	 */
	public static void rollback() {

		Connection con = null;
		try {
			con = getActiveConnectionData().getConnection();
			if (con == null || con.isClosed()) {
				throw new DataAccessException(DataAccessException.Type.STALE_TRANSACTION);
			}
			// When the transaction stack counter indicates that the transaction is beyond the scope of
			// the caller, reduce the transactions stack counter and remain the connection open.
			Long tsStackThreadLocal0 = getActiveConnectionData().getTsStack();
			if (tsStackThreadLocal0 == null) {
				throw new DataAccessException(DataAccessException.Type.STALE_TRANSACTION);
			} else {
                getActiveConnectionData().setTsStack(tsStackThreadLocal0 - 1L);
			}
			if (log.isDebugEnabled()) {
				log.debug("Setting transaction call stack level to "
                        .concat(getActiveConnectionData().getTsStack().toString()));
			}
			if (getActiveConnectionData().getTsStack() == 0L) {
				if (log.isDebugEnabled()) {
					log.debug("Rolling back transaction");
				}
				con.rollback();
                getActiveConnectionData().reset();
			} else {
				if (log.isDebugEnabled()) {
					log.debug("Delegating transaction termination to caller.");
				}
                throw new DataAccessException(DataAccessException.Type.NESTED_TRANSACTION_ROLLBACK);
            }
		} catch (SQLException e) {
			throw new DataAccessException(DataAccessException.Type.CONNECTION_ACCESS_FAILURE, e);
		} finally {
			try {
				if (con != null && !con.isClosed()) {
					// Although the connection may get reused in the current thread, we do not know
					// here, if the thread will be closed. In the latter case we cannot influence,
					// when the connection is returned to the pool, so we do it here.
					if (getActiveConnectionData().getTsStack() == 0L) {
						if (log.isDebugEnabled()) {
							log.debug("Returning retained connection to connection pool.");
						}
						con.close();
					}
				}
			} catch (SQLException ignored) {
			} // Nothing to do
		}
	}

    static Number getNextSequenceValue(final String pSequenceName,
                                       final Long pSequenceIncrement,
                                       final Class pType) {

        return getActiveConnectionData().getNextSequenceValue(pSequenceName, pSequenceIncrement, pType);
    }

	/**
	 * Returns the currently size of JDBC update and insert batches. The default is 100.
	 *
	 * @return the batch size.
	 */
	public static int getBatchUpdateSize() {

        return getActiveConfiguration().getDatabaseBatchUpdateSize();
	}

	/**
	 * Sets the size of JDBC update and insert batches. The default is 100.
	 *
	 * @param pBatchUpdateSize the number of records processed in one DML batch operation
	 */
	public static void setBatchUpdateSize(final int pBatchUpdateSize) {

        getActiveConfiguration().setDatabaseBatchUpdateSize(pBatchUpdateSize);
	}

	/**
	 * Shows whether debug mode has been enabled.
	 *
	 * @return true, when debug mode has been enabled, false otherwise.
	 */
	public boolean isDebugOn() {

        return getActiveConfiguration().isDebugMode();
	}

	/**
	 * Debug mode is enabled by establishing a JPDA/JDWP connection to a listening source code debugger,
	 * which listens on the configured host and port for database session debugging.
	 * DO NOT enable debugging without an active listening source code debugger.
	 *
	 * @param pDebugMode debug mode toggle.
	 */
	public static void setDebugMode(final boolean pDebugMode) {

        getActiveConfiguration().setDebugMode(pDebugMode);
	}

	// There is no easy configurable way to control a clean disconnect for the established debug
	// connection, thus clean-up of the associated socket is left the Connection, resp. Pool.
	static private void enableDebugMode(final Connection pCon) throws SQLException {

		StringBuilder logMessage = new StringBuilder();
		logMessage.append("Enabling PL/SQL debugging. Connecting to host : ");
		logMessage.append(getActiveConfiguration().getDebugJDWPHost());
		logMessage.append(", port : ");
		logMessage.append(getActiveConfiguration().getDebugJDWPPort());
		log.debug(logMessage.toString());
		final String procedureCall = "{ call dbms_debug_jdwp.connect_tcp(host => :host, port => :port)";
		final CallableStatement cstmt = pCon.prepareCall(procedureCall);
		cstmt.setString("host", getActiveConfiguration().getDebugJDWPHost());
		cstmt.setString("port", getActiveConfiguration().getDebugJDWPPort());
		cstmt.execute();
	}

    static class ActiveDataSource {

        private String name;
        private DataSourceConfiguration configuration;
        private DataSource dataSource;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public DataSourceConfiguration getConfiguration() {
            return configuration;
        }

        public void setConfiguration(final DataSourceConfiguration pConfiguration) {
            configuration = pConfiguration;
        }

        public DataSource getDataSource() {
            return dataSource;
        }

        public void setDataSource(final DataSource pDataSource) {
            dataSource = pDataSource;
        }
    }

    static class ActiveConnectionData {

        private ActiveDataSource activeDataSource;
        private Connection connection;
        private Long tsStack = 0L;
        private Map<String, Number> lastSequenceDBValues = new HashMap<String, Number>();
        private Map<String, Number> sequenceCache = new HashMap<String, Number>();

        public ActiveDataSource getActiveDataSource() {
            return activeDataSource;
        }

        public void setActiveDataSource(ActiveDataSource pActiveDataSource) {
            activeDataSource = pActiveDataSource;
        }

        public Connection getConnection() {
            return connection;
        }

        public void setConnection(final Connection pConnection) {
            connection = pConnection;
        }

        public Long getTsStack() {
            return tsStack;
        }

        public void setTsStack(final Long pTSStack) {
            tsStack = pTSStack;
        }

        public void reset() {
            connection = null;
            tsStack = 0L;
        }

        public synchronized Number getNextSequenceValue(final String pSequenceName,
                                                        final Long pSequenceIncrement,
                                                        final Class pType) {

            Number lastSequenceDBValue = lastSequenceDBValues.get(pSequenceName);
            final Number currentSequenceValue = sequenceCache.get(pSequenceName);
            final boolean useCachedValues = lastSequenceDBValue != null && currentSequenceValue.longValue()
                    < lastSequenceDBValue.longValue() + pSequenceIncrement.longValue() - 1;
            if (useCachedValues) {
                Number nextSequenceValue = null;
                if (pType.equals(Long.class)) {
                    nextSequenceValue = currentSequenceValue.longValue() + 1;
                }
                if (pType.equals(Integer.class)) {
                    nextSequenceValue = currentSequenceValue.intValue() + 1;
                }
                if (pType.equals(Short.class)) {
                    nextSequenceValue = currentSequenceValue.shortValue() + 1;
                }
                sequenceCache.put(pSequenceName, nextSequenceValue);
                return nextSequenceValue;
            } else {
                lastSequenceDBValue = getLastSequenceDBValue(pSequenceName, pType);
                lastSequenceDBValues.put(pSequenceName, lastSequenceDBValue);
                sequenceCache.put(pSequenceName, lastSequenceDBValue);
                return lastSequenceDBValue;
            }
        }

        private Number getLastSequenceDBValue(final String pSequenceName, final Class pType) {

            String sequenceName = pSequenceName;
            if (!pSequenceName.equals(pSequenceName.toUpperCase())) {
                sequenceName = "\"".concat(pSequenceName).concat("\"");
            }
            final String sequenceQuery = platform.getSequenceQuery(sequenceName);
            PreparedStatement pstmt = null;
            boolean success = true;
            boolean conAlreadyEstablished = true;

            try {
                if (connection == null) {
                    // The connection used here will usually be just the connection held by this subclass, but when
                    // it has not been initialized yet, we will run into a NPE, thus we go the standard way be using
                    // DataSourceProvider.getConnection()
                    DataSourceProvider.getConnection();
                    conAlreadyEstablished = false;
                }
                pstmt = connection.prepareStatement(sequenceQuery);
                final ResultSet resultSet = pstmt.executeQuery();
                resultSet.next();
                Number sequenceValue = null;
                if (pType.equals(Long.class)) {
                    sequenceValue = resultSet.getLong(1);
                }
                if (pType.equals(Integer.class)) {
                    sequenceValue = resultSet.getInt(1);
                }
                if (pType.equals(Short.class)) {
                    sequenceValue = resultSet.getShort(1);
                }
                return sequenceValue;
            } catch (Exception e) {
                log.error(DataAccessException.Type.COULD_NOT_ACCESS_DATA.getDescription(), e);
                success = false;
                throw new DataAccessException(DataAccessException.Type.COULD_NOT_ACCESS_DATA, e);
            } finally {
                try {
                    if (pstmt != null) {
                        pstmt.close();
                    }
                    if (connection != null && !connection.isClosed() && !conAlreadyEstablished) {
                        DataSourceProvider.returnConnection(success);
                    }
                } catch (SQLException ignored) {
                } // Nothing to do
            }
        }
    }
}
