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
import org.noorm.generator.schema.*;
import org.noorm.generator.servicegenerator.ServiceGenerator;
import org.noorm.jdbc.DataSourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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

    private static final String XML_SCHEMA_PACKAGE = "org.noorm.generator.schema";
    private static final String XML_SCHEMA_URL = "/xsd/noorm-generator-configuration.xsd";
    private GeneratorConfiguration configuration;

	/**
	 * Destination directory for generated source files.
	 */
    @Parameter(defaultValue = "${project.build.directory}/generated-sources/noorm")
	protected File destinationDirectory;

    /**
     * Destination directory for generated source files.
     */
    @Parameter(defaultValue = "${project.basedir}/src/noorm/configuration.xml")
    protected File generatorConfiguration;

	/**
	 * Maven project name.
	 */
    @Parameter(defaultValue = "${project}")
	protected MavenProject project;

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

        log.info("Reading and parsing generator configuration file.");
        if (generatorConfiguration == null) {
            throw new MojoFailureException("No configuration file has been specified.");
        }
        final String fileName = generatorConfiguration.toString();
        if (!generatorConfiguration.exists()) {
            throw new MojoFailureException("Configuration file not found, ".concat(fileName));
        }
        FileInputStream inputStream = null;
        try {
            final SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            final Schema schema = schemaFactory.newSchema(GeneratorMojo.class.getResource(XML_SCHEMA_URL));
            inputStream = new FileInputStream(generatorConfiguration);
            final JAXBContext jaxbContext = JAXBContext.newInstance(XML_SCHEMA_PACKAGE);
            final Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            unmarshaller.setSchema(schema);
            configuration = (GeneratorConfiguration) unmarshaller.unmarshal(inputStream);
        } catch (JAXBException e) {
            throw new MojoFailureException("Could not open configuration ".concat(fileName), e);
        } catch (IOException e) {
            throw new MojoFailureException("Could not parse configuration ".concat(fileName), e);
        } catch (SAXException e) {
            throw new MojoFailureException("Could not instantiate the NoORM generator XML schema.", e);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    log.warn("Configuration file could not be closed, ".concat(fileName), e);
                }
            }
        }
        setConfigurationDefaults();

		// Initialize Velocity and configure Velocity to load resources from the classpath
		Velocity.setProperty("resource.loader", "class");
		Velocity.setProperty("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
		Velocity.init();

		// To avoid acquiring a new database connection for every single access to the MetadataService, the generators
		// run in one single transaction. Exception handling with clean rollback handling has been omitted, since all
		// database operations are read-only.
		DataSourceProvider.begin();

		// Generate Beans
		final BeanGenerator beanGenerator = new BeanGenerator(this, configuration);
		beanGenerator.execute();

		// Generate Enums
		if (configuration.getEnumPackageName() != null && !configuration.getEnumPackageName().isEmpty()) {
			final EnumGenerator enumGenerator = new EnumGenerator(this, configuration);
			enumGenerator.execute();
		}

		// Generate Services
		if (configuration.getServicePackageName() !=null && !configuration.getServicePackageName().isEmpty()) {
			final ServiceGenerator serviceGenerator = new ServiceGenerator(this, configuration);
			serviceGenerator.execute();
		}

        // Generate Declared Queries
        if (configuration.getQueryDeclarations() !=null && !configuration.getQueryDeclarations().isEmpty()) {
            final QueryGenerator queryGenerator = new QueryGenerator(this, configuration);
            queryGenerator.execute();
        }

		DataSourceProvider.commit();
	}

	@Override
	public File getDestinationDirectory() {
		return destinationDirectory;
	}

    @Override
    public File getGeneratorConfiguration() {
        return generatorConfiguration;
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

    /**
     * JAXB/XJC does not yet support default values for elements, so it is meaningless to specify defaults
     * in the XML schema and we set defaults here.
     */
    private void setConfigurationDefaults() {

        if (configuration.isGeneratePKBasedEqualsAndHashCode() == null) {
            configuration.setGeneratePKBasedEqualsAndHashCode(Boolean.TRUE);
        }
        for (final QueryDeclaration queryDeclaration : configuration.getQueryDeclarations()) {
            if (queryDeclaration.isAcquireLock() == null) {
                queryDeclaration.setAcquireLock(Boolean.FALSE);
            }
            if (queryDeclaration.isSingleRowQuery() == null) {
                queryDeclaration.setSingleRowQuery(Boolean.FALSE);
            }
            for (final QueryColumn queryColumn : queryDeclaration.getQueryColumns()) {
                if (queryColumn.getOperator() == null) {
                    queryColumn.setOperator(OperatorName.EQUAL_TO);
                }
            }
        }
    }
}
