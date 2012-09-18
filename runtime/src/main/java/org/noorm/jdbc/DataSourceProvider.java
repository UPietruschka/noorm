package org.noorm.jdbc;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Properties;

import oracle.jdbc.OracleCallableStatement;
import oracle.jdbc.OracleConnection;
import oracle.jdbc.pool.OracleDataSource;
//import oracle.ucp.jdbc.PoolDataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

/**
 * The DataSourceProvider organizes  data source access and transaction control for NoORM.
 * Access to the JDBC connection objects is discouraged outside of NoORM. The only reason
 * to use DataSourceProvider directly in an application is for fine grained transaction
 * control. DataSourceProvider provides the required methods begin(), commit() and rollback()
 * to control a transaction in an application (direct access to the underlying JDBC
 * connection is possible for special purposes like integration with Oracle AQ).
 * When these methods for explicit transaction control are not used, DataSourceProvider will
 * manage transactions automatically by issuing an auto-commit for every database access.
 * Issuing a commit even for read-only database access imposes a little performance overhead.
 * For most usage scenarios, this overhead is not significant. However, when an application
 * performs a large number of read only operation it it worth comparing performance with
 * explicit transaction handling.
 * Explicit transaction handling (using begin(), commit(), rollback()) is managed by
 * maintaining the JDBC connection in a ThreadLocal variable. This removes the burden of
 * connection control from the application programmer, since DataSourceProvider will keep
 * track of the current transaction beyond class and method boundaries in the application.
 * However, using explicit transaction control requires the application programmer to control
 * transaction termination properly, i.e., the application must guarantee that a transaction
 * started with begin() will always be terminated with commit() or rollback().
 *
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 */
public class DataSourceProvider {

	private static final Logger log = LoggerFactory.getLogger(DataSourceProvider.class);

	private static DataSourceProvider dataSourceFactory;
	private DataSource dataSource;

	private static final String NOORM_PROPERTIES_FILENAME = "/META-INF/noorm.properties";
	private static final String NOORM_XML_FILENAME = "/META-INF/noorm.xml";
	private static final String DATABASE_JNDINAME = "database.jndiname";
	private static final String DATABASE_PASSWORD = "database.password";
	private static final String DATABASE_URL = "database.url";
	private static final String DATABASE_USERNAME = "database.username";
	private static final String DATABASE_BATCH_UPDATE_SIZE = "database.batch_update_size";
	private static final String DEBUG_MODE = "debug.mode";
	private static final String DEBUG_JDWP_HOST = "debug.host";
	private static final String DEBUG_JDWP_PORT = "debug.port";

	private static final ThreadLocal<OracleConnection> conThreadLocal = new ThreadLocal<OracleConnection>();
	private static final ThreadLocal<Long> tsStackThreadLocal = new ThreadLocal<Long>();

	private int batchUpdateSize = 100;

	private boolean debugMode = false;
	private String debugJDWPPort = "4000";
	private String debugJDWPHost = "localhost";

	private final Properties noormProperties = new Properties();

	private DataSourceProvider() {
	}

	/**
	 * Initializes and instantiates the DataSourceProvider with the given, pre-configured DataSource.
	 * This step is optionally but strictly recommend for production usage. Alternatively, the datasource
	 * could be configured using the noorm.properties file, but the latter is only suitable for a simple
	 * setup with username, password and URL. To apply advanced settings to the DataSource, this
	 * pre-configuration method should be used.
	 *
	 * @param pDataSource the pre-configured Oracle datasource.
	 */
	public static void setDataSource(final DataSource pDataSource) {

		if (dataSourceFactory == null) {
			log.debug("Instantiating DataSourceProvider.");
			dataSourceFactory = new DataSourceProvider();
			dataSourceFactory.loadNoormProperties();
			dataSourceFactory.dataSource = pDataSource;
			dataSourceFactory.initNoormProperties();
			validateDataSource();
		}
	}

	/**
	 * Validate the data source. Either for the data source submitted using setDataSource or for the
	 * data source constructed using the NoORM properties, a connection is established to validate
	 * the data source.
	 */
	private static void validateDataSource() {

		final StringBuilder validationInfo = new StringBuilder();
		validationInfo.append("Validating data source. ");
		try {
			//		To provide information for a failed data source validation in case of an UCP
			//		(Oracle Universal Connection Pool) PoolDataSource, we need to include UCP into
			//		the list of dependencies for NoORM as well as for the projects using NoORM.
			//		The typically undesired dependency for a specific connection pool implementation
			//		outweighs the advantage of a proper reporting for a mis-configured data source.
			//		Thus, UCP is no longer considered within NoORM.
			//if (dataSourceFactory.dataSource instanceof PoolDataSource) {
			//	validationInfo.append("Connection parameters: ");
			//	validationInfo.append(";URL: ");
			//	validationInfo.append(((PoolDataSource) dataSourceFactory.dataSource).getURL());
			//	validationInfo.append(";Username: ");
			//	validationInfo.append(((PoolDataSource) dataSourceFactory.dataSource).getUser());
			//} else {
			if (dataSourceFactory.dataSource instanceof OracleDataSource) {
				validationInfo.append("Connection parameters: ");
				validationInfo.append(";URL: ");
				validationInfo.append(((OracleDataSource) dataSourceFactory.dataSource).getURL());
				validationInfo.append(";Username: ");
				validationInfo.append(((OracleDataSource) dataSourceFactory.dataSource).getUser());
			} else {
				validationInfo.append("Unable to retrieve connection parameters from data source. [");
				validationInfo.append(dataSourceFactory.dataSource.getClass().getName());
				validationInfo.append("]");
			}
			//}
			log.info(validationInfo.toString());
			dataSourceFactory.dataSource.getConnection();
		} catch (Exception e) {
			throw new DataAccessException(DataAccessException.Type.COULD_NOT_ESTABLISH_CONNECTION, e);
		}
	}

	private static DataSource getDataSource() throws SQLException {

		if (dataSourceFactory == null) {
			log.debug("Instantiating DataSourceProvider.");
			dataSourceFactory = new DataSourceProvider();
			dataSourceFactory.loadNoormProperties();
			dataSourceFactory.initDataSource();
			dataSourceFactory.initNoormProperties();
		}
		return dataSourceFactory.dataSource;
	}

	private void loadNoormProperties() {

		log.info("Trying to load configuration file ".concat(NOORM_XML_FILENAME));
		InputStream is = dataSourceFactory.getClass().getResourceAsStream(NOORM_XML_FILENAME);
		try {
			if (is != null) {
				noormProperties.loadFromXML(is);
				log.info("Configuration file ".concat(NOORM_XML_FILENAME).concat(" loaded."));
			} else {
				log.info("Configuration file ".concat(NOORM_XML_FILENAME).concat(" not found."));
				log.info("Trying to load configuration file ".concat(NOORM_PROPERTIES_FILENAME));
				is = dataSourceFactory.getClass().getResourceAsStream(NOORM_PROPERTIES_FILENAME);
				if (is != null) {
					noormProperties.load(is);
					log.info("Configuration file ".concat(NOORM_PROPERTIES_FILENAME).concat(" loaded."));
				} else {
					log.info("Configuration file ".concat(NOORM_PROPERTIES_FILENAME).concat(" not found."));
					throw new FileNotFoundException();
				}
			}
		} catch (FileNotFoundException ex) {
			// File noorm.properties is optional, failing to load the properties is considered to be
			// an exception only when the reason for the failure is not a missing file.
		} catch (IOException ex) {
			throw new DataAccessException(DataAccessException.Type.INITIALIZATION_FAILURE,
					"Loading of noorm.properties failed.");
		}
	}

	private void initNoormProperties() {

		final String batchUpdateSizeProp = noormProperties.getProperty(DATABASE_BATCH_UPDATE_SIZE);
		if (batchUpdateSizeProp != null && !batchUpdateSizeProp.isEmpty()) {
			try {
				batchUpdateSize = Integer.parseInt(batchUpdateSizeProp);
				log.info("Setting ".concat(DATABASE_BATCH_UPDATE_SIZE).concat(" = ").concat(batchUpdateSizeProp));
			} catch (NumberFormatException ex) {
				throw new DataAccessException(DataAccessException.Type.INITIALIZATION_FAILURE, ex);
			}
		} else {
			final String batchUpdateSizeS = (new Integer(batchUpdateSize).toString());
			log.info("Setting (default) ".concat(DATABASE_BATCH_UPDATE_SIZE).concat(" = ").concat(batchUpdateSizeS));
		}

		final String debugModeProp = noormProperties.getProperty(DEBUG_MODE);
		if (debugModeProp != null && debugModeProp.toLowerCase().equals("true")) {
			debugMode = true;
			log.info("Setting ".concat(DEBUG_MODE).concat(" = true"));
		} else {
			log.info("Setting (default) ".concat(DEBUG_MODE).concat(" = false"));
		}

		final String debugHostProp = noormProperties.getProperty(DEBUG_JDWP_HOST);
		if (debugHostProp != null && !debugHostProp.isEmpty()) {
			debugJDWPHost = debugHostProp;
			log.info("Setting ".concat(DEBUG_JDWP_HOST).concat(" = ").concat(debugHostProp));
		} else {
			log.info("Setting (default) ".concat(DEBUG_JDWP_HOST).concat(" = ").concat(debugJDWPHost));
		}

		final String debugPortProp = noormProperties.getProperty(DEBUG_JDWP_PORT);
		if (debugPortProp != null && !debugPortProp.isEmpty()) {
			debugJDWPPort = debugPortProp;
			log.info("Setting ".concat(DEBUG_JDWP_PORT).concat(" = ").concat(debugPortProp));
		} else {
			log.info("Setting (default) ".concat(DEBUG_JDWP_PORT).concat(" = ").concat(debugJDWPPort));
		}
	}

	/*
     * The data source parameters in the NoORM properties can either be specified using JNDI or directly
	 * by providing the required parameters for setting up an OracleDataSource. When both types of
	 * configuration settings are available, the JNDI configuration has precedence.
	 */
	private void initDataSource() throws SQLException {

		final String jndiName = noormProperties.getProperty(DATABASE_JNDINAME);

		if (jndiName != null) {

			try {
				log.info("Trying to establish data source using JDNI name ".concat(jndiName));
				final Context initCtx = new InitialContext();
				dataSource = (DataSource) initCtx.lookup(jndiName);
				log.info("JNDI data source lookup successful.");
			} catch (NamingException e) {
				throw new DataAccessException("JNDI data source lookup failed.", e);
			}

		} else {

			final String url = noormProperties.getProperty(DATABASE_URL);
			if (url == null) {
				throw new DataAccessException(DataAccessException.Type.INITIALIZATION_FAILURE,
						"Data source parameter 'database.url' not properly configured.");
			} else {
				log.info("Setting ".concat(DATABASE_URL).concat(" = ").concat(url));
			}
			final String username = noormProperties.getProperty(DATABASE_USERNAME);
			if (username == null) {
				throw new DataAccessException(DataAccessException.Type.INITIALIZATION_FAILURE,
						"Data source parameter 'database.username' not properly configured.");
			} else {
				log.info("Setting ".concat(DATABASE_USERNAME).concat(" = ").concat(username));
			}
			final String password = noormProperties.getProperty(DATABASE_PASSWORD);
			if (password == null) {
				throw new DataAccessException(DataAccessException.Type.INITIALIZATION_FAILURE,
						"Data source parameter 'database.password' not properly configured.");
			} else {
				log.info("Setting ".concat(DATABASE_PASSWORD).concat(" = ").concat(password));
			}

			OracleDataSource oracleDataSource = new OracleDataSource();
			oracleDataSource.setURL(url);
			oracleDataSource.setUser(username);
			oracleDataSource.setPassword(password);
			dataSource = oracleDataSource;
		}

		validateDataSource();
	}

	static void returnConnection(final OracleConnection pCon, final boolean pSuccess) {

		OracleConnection con = conThreadLocal.get();
		try {
			if (con == null || con.isClosed()) {
				if (log.isDebugEnabled()) {
					log.debug("Returning connection to connection pool.");
				}
				// When closing the connection (resp. returning it to the connection pool, we
				// always issue a commit, since in any case, there is no meaningful transaction
				// scope beyond this point in time.
				// Committing even in case of a read only transaction may incur some additional
				// cost, since from a technical perspective, the commit is not necessary. On
				// the other hand, Oracle reduces the cost in this case to a minimum, so that
				// the advantage of this way of transaction automation outweighs a possible
				// minimal performance reduction.
				if (pSuccess) {
					pCon.commit();
				} else {
					pCon.rollback();
				}
				pCon.close();
			}
		} catch (SQLException e) {
			throw new DataAccessException(DataAccessException.Type.CONNECTION_ACCESS_FAILURE, e);
		}
	}

	/**
	 * Returns the Oracle database connection currently managed by the DataSourceProvider.
	 * Note that the life-cycle of the connection is managed by the DataSourceProvider, i.e. acquiring a
	 * connection, closing a connection, resp. returning a connection to the connection pool and managing
	 * transaction boundaries SHOULD NOT BE DONE in the application code. Providing the database connection
	 * directly by this method has the sole purpose to support additional Oracle Java client software, which
	 * requires access to the connection (e.g. Oracle Streams Advanced Queueing, which requires the Oracle
	 * connection to create an AQSession).
	 *
	 * @return the Oracle database connection currently managed by the DataSourceProvider.
	 * @throws SQLException
	 */
	public static OracleConnection getConnection() throws SQLException {

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
	private static OracleConnection getConnection(final boolean pRetain) throws SQLException {

		OracleConnection con = conThreadLocal.get();
		if (con == null || con.isClosed()) {
			if (log.isDebugEnabled()) {
				if (pRetain) {
					log.debug("Acquiring retainable connection from connection pool.");
				} else {
					log.debug("Acquiring new connection from connection pool.");
				}
			}

			// JDBC resources provided by an application server may wrap the native connection with some
			// proprietary connection implementation. This prevents the usage of vendor specific features,
			// in particular Oracle specific features used here. Thus, the provided connection is checked
			// for a wrapped native connection and this connection is then unwrapped and used instead.
			java.sql.Connection dataSourceConn = getDataSource().getConnection();
			if (dataSourceConn.isWrapperFor(oracle.jdbc.OracleConnection.class)) {
				con = dataSourceConn.unwrap(oracle.jdbc.OracleConnection.class);
			} else {
				con = (OracleConnection) dataSourceConn;
			}

			con.setAutoCommit(false);
			if (dataSourceFactory.debugMode) {
				enableDebugMode(con);
			}
			if (pRetain) {
				conThreadLocal.set(con);
			}
		}
		if (pRetain) {
			Long tsStackThreadLocal0 = tsStackThreadLocal.get();
			if (tsStackThreadLocal0 == null) {
				tsStackThreadLocal.set(1L);
			} else {
				tsStackThreadLocal.set(tsStackThreadLocal0 + 1L);
			}
			if (log.isDebugEnabled()) {
				log.debug("Setting transaction call stack level to ".concat(tsStackThreadLocal.get().toString()));
			}
		}
		return con;
	}

	/**
	 * To control transactions in the calling application, the connection in use must not
	 * turned back to the connection pool, but should be preserved in a ThreadLocal variable.
	 * This method is used to indicate that the calling application intends to broaden the
	 * transaction boundaries beyond the scope of a single PL/SQL or DML call. Oracle does not know
	 * about the classical "begin" call, since an Oracle session has always an active
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
	 * The connection object itself is not exploited to accessing classes outside of the package, but
	 * for controlling transactions from outside, using methods commit() and rollback() provides the
	 * required functionality.
	 * Note that using this method requires that a user managed transaction has been started using
	 * method begin(), other a DataAccessException is thrown.
	 * Transactions, which are not user managed are always committed automatically, when the connection
	 * is returned to the connection pool (restricting automated transaction management to transactions
	 * with a scope limited to a single call to PL/SQL, an insert, update or an delete operation.
	 */
	public static void commit() {

		OracleConnection con = null;
		try {
			con = conThreadLocal.get();
			if (con == null || con.isClosed()) {
				throw new DataAccessException(DataAccessException.Type.STALE_TRANSACTION);
			}
			// When the transaction stack counter indicates that the transaction is beyond the scope of
			// the caller, reduce the transactions stack counter and remain the connection open.
			Long tsStackThreadLocal0 = tsStackThreadLocal.get();
			if (tsStackThreadLocal0 == null) {
				throw new DataAccessException(DataAccessException.Type.STALE_TRANSACTION);
			} else {
				tsStackThreadLocal.set(tsStackThreadLocal0 - 1L);
			}
			if (log.isDebugEnabled()) {
				log.debug("Setting transaction call stack level to ".concat(tsStackThreadLocal.get().toString()));
			}
			if (tsStackThreadLocal.get() == 0L) {
				if (log.isDebugEnabled()) {
					log.debug("Committing transaction");
				}
				con.commit();
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
					if (tsStackThreadLocal.get() == 0L) {
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

		OracleConnection con = null;
		try {
			con = conThreadLocal.get();
			if (con == null || con.isClosed()) {
				throw new DataAccessException(DataAccessException.Type.STALE_TRANSACTION);
			}
			// When the transaction stack counter indicates that the transaction is beyond the scope of
			// the caller, reduce the transactions stack counter and remain the connection open.
			Long tsStackThreadLocal0 = tsStackThreadLocal.get();
			if (tsStackThreadLocal0 == null) {
				throw new DataAccessException(DataAccessException.Type.STALE_TRANSACTION);
			} else {
				tsStackThreadLocal.set(tsStackThreadLocal0 - 1L);
			}
			if (log.isDebugEnabled()) {
				log.debug("Setting transaction call stack level to ".concat(tsStackThreadLocal.get().toString()));
			}
			if (tsStackThreadLocal.get() == 0L) {
				if (log.isDebugEnabled()) {
					log.debug("Rolling back transaction");
				}
				con.rollback();
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
					if (tsStackThreadLocal.get() == 0L) {
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
	 * Returns the currently size of JDBC update and insert batches. The default is 100.
	 *
	 * @return the batch size.
	 */
	public static int getBatchUpdateSize() {

		return dataSourceFactory.batchUpdateSize;
	}

	/**
	 * Sets the size of JDBC update and insert batches. The default is 100.
	 *
	 * @param pBatchUpdateSize
	 */
	public static void setBatchUpdateSize(final int pBatchUpdateSize) {

		dataSourceFactory.batchUpdateSize = pBatchUpdateSize;
	}

	/**
	 * Shows whether debug mode has been enabled.
	 *
	 * @return true, when debug mode has been enabled, false otherwise.
	 */
	public boolean isDebugOn() {

		return debugMode;
	}

	/**
	 * Debug mode is enabled by establishing a JPDA/JDWP connection to a listening source code debugger,
	 * which listens on the configured host and port for database session debugging.
	 * DO NOT enable debugging without an active listening source code debugger.
	 *
	 * @param pDebugMode debug mode toggle.
	 */
	public static void setDebugMode(final boolean pDebugMode) {

		dataSourceFactory.debugMode = pDebugMode;
	}

	// There is no easy configurable way to control a clean disconnect for the established debug
	// connection, thus clean-up of the associated socket is left the Connection, resp. Pool.
	static private void enableDebugMode(final OracleConnection pCon) throws SQLException {

		StringBuilder logMessage = new StringBuilder();
		logMessage.append("Enabling PL/SQL debugging. Connecting to host : ");
		logMessage.append(dataSourceFactory.debugJDWPHost);
		logMessage.append(", port : ");
		logMessage.append(dataSourceFactory.debugJDWPPort);
		log.debug(logMessage.toString());
		final String plSQLCall = "{ call dbms_debug_jdwp.connect_tcp(host => :host, port => :port)";
		final OracleCallableStatement cstmt = (OracleCallableStatement) pCon.prepareCall(plSQLCall);
		cstmt.setString("host", dataSourceFactory.debugJDWPHost);
		cstmt.setString("port", dataSourceFactory.debugJDWPPort);
		cstmt.execute();
	}
}
