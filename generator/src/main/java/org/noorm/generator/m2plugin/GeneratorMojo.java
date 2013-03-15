package org.noorm.generator.m2plugin;

import oracle.jdbc.pool.OracleDataSource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.velocity.app.Velocity;
import org.noorm.generator.beangenerator.BeanGenerator;
import org.noorm.generator.enumgenerator.EnumGenerator;
import org.noorm.generator.querygenerator.QueryGenerator;
import org.noorm.generator.servicegenerator.ServiceGenerator;
import org.noorm.jdbc.DataSourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.SQLException;
import java.util.*;

/**
 * Maven plugin base class for the NoORM class generator.
 *
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 */
@Mojo(name = "generate-noorm", defaultPhase = LifecyclePhase.PROCESS_SOURCES)
public class GeneratorMojo extends AbstractMojo implements IParameters {

	private static final Logger log = LoggerFactory.getLogger(GeneratorMojo.class);

	/**
	 * Destination directory for generated source files.
	 */
    @Parameter(defaultValue = "${project.build.directory}/generated-sources/noorm")
	protected File destinationDirectory;

	/**
	 * Package name for generated Bean source files.
	 */
    @Parameter(required = true)
	protected String beanPackageName;

	/**
	 * Package name for generated Enum source files.
	 */
    @Parameter
	protected String enumPackageName;

	/**
	 * Package name for generated Service / DAO source files.
	 */
    @Parameter(required = true)
	protected String servicePackageName;

	/**
	 * Maven project name.
	 */
    @Parameter(defaultValue = "${project}")
	protected MavenProject project;

    /**
     * When multiple data sources are used, the data source name as configured in the NoORM configuration file is
     * used to identify different data sources. To specify the data source used for the following statements, one
     * can either set the data source explicitly using DataSourceProvider.setActiveDataSource, or one can specify
     * the data source name here. Note that for explicit transaction handling, one still have to specify the data
     * source name.
     */
    @Parameter
    protected String dataSourceName;

    /**
	 * JDBC connection URL for the Oracle schema containing the tables, views and stored procedures
	 * subject to Java code generation.
	 */
    @Parameter(required = true)
	protected String url;

	/**
	 * Username for the Oracle schema.
	 */
    @Parameter(required = true)
	protected String username;

	/**
	 * Password for the Oracle schema.
	 */
    @Parameter(required = true)
	protected String password;

	/**
	 * List of table name prefixes to be ignored for java class name construction.
	 * Some data modelers use a common table name prefix to identify tables of a
	 * given schema or group. When those prefixes are not desired in the constructed
	 * java class name, they should be listed here.
	 * This setting applies to the bean generator and the enum generator.
	 */
    @Parameter
	protected List<String> ignoreTableNamePrefixes;

	/**
	 * Regular expression to filter tables and views for Bean generation.
	 */
    @Parameter
	protected String beanTableFilterRegex;

	/**
	 * Regular expression to filter tables and views for Enum generation.
	 */
    @Parameter
	protected String enumTableFilterRegex;

	/**
	 * To generate Enums from database tables, NoORM must now, which table column should be used
	 * for the enums constant type generation. Typically, a table with constant content has a column
	 * with a code or denominator in uppercase letters, which uniquely identifies the row.
	 */
    @Parameter
	protected Properties enumTable2DisplayColumnMapping;

	/**
	 * Primary key generation for new records being inserted into the database is based on
	 * a numeric ID column and an Oracle sequence. Oracle sequences are not tied to a table
	 * by definition, so associating a table with a sequence is done using this property
	 * list. Note that the association TABLE_NAME/SEQUENCE_NAME can either be done on a per
	 * table basis, or using one or more regular expressions to specify a mapping rule like
	 * "TBL_(.*)" -> "SEQ_$1" (This rule would map TBL_PRODUCT to SEQ_PRODUCT, for example).
	 */
    @Parameter
	protected Properties oracleTable2SequenceMapping;

	/**
	 * Concurrency control is based on optimistic locking. To identify the version column,
	 * a mapping from the table-name to the version column should be specified. Dependent on
	 * how specific the column-names are with respect to the table-names, one or more
	 * mapping are required. In case of a unique name of the version column for all tables,
	 * one simple rule like ".*" -> "VERSION" is sufficient.
	 */
    @Parameter
	protected Properties optimisticLockColumnMapping;

	/**
	 * The Oracle data dictionary does not provide unambiguous information for the primary key
	 * of a view (for tables, this information is available). When the intended use of a view
	 * includes DML operations (which requires the view to contain one and only one key-preserved
	 * table) or data access with the PageableBeanList, NoORM needs a key to uniquely distinguish
	 * the records of this view. Use this parameter to specify the column name of the key used
	 * for a given view. Typically, this key is the primary key of the single key-preserved table
	 * contained in the view definition.
	 */
    @Parameter
	protected Properties viewName2PrimaryKeyMapping;

	/**
	 * Regular expression to filter packages for service generation.
	 */
    @Parameter
	protected String packageFilterRegex;

	/**
	 * Accessing data mapped to Beans always uses an Oracle reference cursor to
	 * retrieve the data. However, using the PL/SQL procedures signature does
	 * not provide any hint, if the reference cursor being returned is limited
	 * to a single row (which in turn changes the signature of the generated Java
	 * code, instead of a List a single Bean is returned).
	 *
	 * Use this parameter to specify a regular expression matching all procedure
	 * names subject to single row retrieval.
	 */
    @Parameter
	protected String singleRowFinderRegex;

	/**
	 * Large query results can be mapped into a PageableBeanList to provide efficient
	 * access to the data by loading the full record only for the requested page.
	 */
    @Parameter
	protected String pageableProcedureNameRegex;

	/**
	 * Services and DAOs generated by NoORM are singletons. By default, the services
	 * and DAOs are instantiated right after class loading and provided by the class method
	 * "getInstance".
	 * Alternatively, class instantiation can be delegated to a dependency injection framework
	 * like Spring. By specifying parameter serviceInterfacePackageName, the service generator
	 * is directed to omit the in-class singleton implementation and generate appropriate
	 * interfaces for every service, resp. DAO in the given package.
	 */
    @Parameter
	protected String serviceInterfacePackageName;

	/**
	 * Beans generated from database entities are often subject to data enrichment in
	 * the service utilizing the bean data. One option to add additional data to the
	 * bean is the generic (generated) bean property "auxiliaryData". However, some
	 * data consumers may require data provided in a single bean without nested data
	 * (i.e., the additional data is available using standard bean properties).
	 * As an alternative approach to the auxiliary data property, the user may create
	 * a subclass for the generated bean with additional bean properties. To utilize
	 * this inherited bean classes, the generated services using the originally
	 * generated class should use the subclass. This parameter allows for a mapping
	 * of originally generated bean classes to data enriched subclasses. Note that
	 * the subclass must be fully classified.
	 */
    @Parameter
	protected Properties extendedBeans;

    /**
     * The NoORM query declaration is intentionally much simpler than most other approaches to specify alternatives
     * to original SQL. While specifications like the JPA 2.0 criteria API aim to cover most of the capabilities of
     * SQL, this approach consequently follows the paradigm to move database-centric functionality to the database.
     * In particular, this means that the complexity of an SQL statement should be implemented inside the database
     * using views. Following this approach, it is almost always possible to reduce the query declaration for
     * automatic Java code generation to a single entity (table, view), the columns subject to the where-conditions
     * and the operators used for the columns in the where-conditions.
     */
    @Parameter
    protected List<QueryDeclaration> queryDeclarations;

    /**
     * The implementation of methods "equals" and "hashCode" for the generated beans raises the same questions
     * intensively discussed for JPA entities. In particular, three options are available: do not implement these
     * methods at all, implement them based on the technical ID, i.e. the primary key, or implement them based on
     * some business-id. The latter is not applicable for generated code, since we do not have the required
     * insight into the semantics of the bean/table to decide on a business-id.
     * Thus, options one and two remain and this option can be used to choose one. Note that this option is set
     * to true by default (i.e. methods equals and hashCode are automatically implemented based on the primary
     * key).
     */
    @Parameter(defaultValue = "true")
    protected boolean generatePKBasedEqualsAndHashCode;

	private OracleDataSource initializePoolDataSource() throws SQLException {

		final OracleDataSource oracleDataSource = new OracleDataSource();
		oracleDataSource.setURL(url);
		oracleDataSource.setUser(username);
		oracleDataSource.setPassword(password);
        // We enable the Oracle connection cache integrated with the Oracle JDBC driver.
        // Even for single-threaded stand-alone applications using a connection pool/cache makes sense.
        // Like any other ORM tool, NoORM does not manage data sources, but simply uses the JDBC API.
        // When transactions are not handled explicitly by the calling application, the implicit
        // auto-commit mode will cause connections to be closed with every single database call. Though
        // DataSourceProvider could retain connections for some time, its primary function is not the
        // maintenance of a connection cache or pool, so this job is delegated to the used data source,
        // which should provide some caching functionality for any usage scenario.
        // Unfortunately, Oracle stopped development of the build-in connection cache, so, starting with
        // Oracle 11.2, the build-in cache is deprecated. We still use it here, since explicit data source
        // initialization as performed here is not to be used in production systems anyway.
        oracleDataSource.setConnectionCachingEnabled(true);
        Properties cacheProps = new Properties();
        cacheProps.setProperty("MinLimit", "1");
        cacheProps.setProperty("MaxLimit", "4");
        cacheProps.setProperty("InitialLimit", "1");
        oracleDataSource.setConnectionCacheProperties(cacheProps);
        return oracleDataSource;
	}

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {

		try {
            // Generate an arbitrary data source name. Since the generator can be called more than once within
            // a single Maven build, we should at least generate a unique name, since DataSourceProvider does
            // not allow to replace an already added data source (with the same name).
            final String dataSourceName = Long.toString(System.currentTimeMillis());
			DataSourceProvider.addDataSource(initializePoolDataSource(), dataSourceName, true);
		} catch (SQLException e) {
			throw new MojoExecutionException("Initializing Oracle PoolDataSource failed.", e);
		}

		log.info("Creating destination directory for generated sources.");
		if (!destinationDirectory.exists()) {
			if (!destinationDirectory.mkdirs()) {
				throw new MojoFailureException("Could not create directory ".concat(destinationDirectory.toString()));
			}
		}
		project.addCompileSourceRoot(destinationDirectory.getAbsolutePath());

		// Initialize Velocity and configure Velocity to load resources from the classpath
		Velocity.setProperty("resource.loader", "class");
		Velocity.setProperty("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
		Velocity.init();

		// To avoid acquiring a new database connection for every single access to the MetadataService, the generators
		// run in one single transaction. Exception handling with clean rollback handling has been omitted, since all
		// database operations are read-only.
		DataSourceProvider.begin();

		// Generate Beans
		final BeanGenerator beanGenerator = new BeanGenerator(this);
		beanGenerator.execute();

		// Generate Enums
		if (enumPackageName != null && !enumPackageName.isEmpty()) {
			final EnumGenerator enumGenerator = new EnumGenerator(this);
			enumGenerator.execute();
		}

		// Generate Services
		if (servicePackageName !=null && !servicePackageName.isEmpty()) {
			final ServiceGenerator serviceGenerator = new ServiceGenerator(this);
			serviceGenerator.execute();
		}

        // Generate Declared Queries
        if (queryDeclarations !=null && !queryDeclarations.isEmpty()) {
            final QueryGenerator queryGenerator = new QueryGenerator(this);
            queryGenerator.execute();
        }

		DataSourceProvider.commit();
	}

	@Override
	public File getDestinationDirectory() {
		return destinationDirectory;
	}

	@Override
	public String getBeanPackageName() {
		return beanPackageName;
	}

	@Override
	public String getEnumPackageName() {
		return enumPackageName;
	}

	@Override
	public String getServicePackageName() {
		return servicePackageName;
	}

    @Override
    public String getDataSourceName() {
        return dataSourceName;
    }

	@Override
	public String getURL() {
		return url;
	}

	@Override
	public String getUsername() {
		return username;
	}

	@Override
	public String getPassword() {
		return password;
	}

	@Override
	public List<String> getIgnoreTableNamePrefixes() {
		return ignoreTableNamePrefixes;
	}

	@Override
	public String getBeanTableFilterRegex() {
		return beanTableFilterRegex;
	}

	@Override
	public String getEnumTableFilterRegex() {
		return enumTableFilterRegex;
	}

	@Override
	public Properties getEnumTable2DisplayColumnMapping() {
		return enumTable2DisplayColumnMapping;
	}

	@Override
	public Properties getOracleTable2SequenceMapping() {
		return oracleTable2SequenceMapping;
	}

	@Override
	public Properties getOptimisticLockColumnMapping() {
		return optimisticLockColumnMapping;
	}

	@Override
	public Properties getViewName2PrimaryKeyMapping() {
		return viewName2PrimaryKeyMapping;
	}

	@Override
	public String getPackageFilterRegex() {
		return packageFilterRegex;
	}

	@Override
	public String getSingleRowFinderRegex() {
		return singleRowFinderRegex;
	}

	@Override
	public String getPageableProcedureNameRegex() {
		return pageableProcedureNameRegex;
	}

	@Override
	public String getServiceInterfacePackageName() {
		return serviceInterfacePackageName;
	}

	@Override
	public Properties getExtendedBeans() {
		return extendedBeans;
	}

    @Override
    public List<QueryDeclaration> getQueryDeclarations() {
        return queryDeclarations;
    }

    @Override
    public boolean generatePKBasedEqualsAndHashCode() {
        return generatePKBasedEqualsAndHashCode;
    }
}
