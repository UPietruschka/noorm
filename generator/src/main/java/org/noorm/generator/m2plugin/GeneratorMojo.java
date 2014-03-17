package org.noorm.generator.m2plugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.velocity.app.Velocity;
import org.noorm.generator.GeneratorUtil;
import org.noorm.generator.beangenerator.BeanGenerator;
import org.noorm.generator.enumgenerator.EnumGenerator;
import org.noorm.generator.querygenerator.QueryGenerator;
import org.noorm.generator.schema.*;
import org.noorm.generator.servicegenerator.ServiceGenerator;
import org.noorm.jdbc.DataSourceProvider;
import org.noorm.platform.IPlatform;
import org.noorm.platform.PlatformFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.sql.DataSource;
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
     * The platform to use for metadata retrieval
     */
    @Parameter(required = true)
    protected String platformName;

    /**
	 * JDBC connection URL for the database schema containing the tables, views and stored procedures
	 * subject to Java code generation.
	 */
    @Parameter(required = true)
	protected String url;

	/**
	 * Username for the database schema.
	 */
    @Parameter(required = true)
	protected String username;

	/**
	 * Password for the database schema.
	 */
    @Parameter(required = true)
	protected String password;

	private DataSource initializeDataSource() throws SQLException {

        final IPlatform platform = PlatformFactory.createPlatform(platformName);
        final DataSource dataSource = platform.getDataSource(url, username, password);
        return dataSource;
	}

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {

		try {
            // Generate an arbitrary data source name. Since the generator can be called more than once within
            // a single Maven build, we should at least generate a unique name, since DataSourceProvider does
            // not allow to replace an already added data source (with the same name).
            final String dataSourceName = Long.toString(System.currentTimeMillis());
			DataSourceProvider.addDataSource(initializeDataSource(), dataSourceName, platformName, true);
		} catch (SQLException e) {
			throw new MojoExecutionException("Initializing DataSource failed.", e);
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
		if (GeneratorUtil.hasEnumPackageName(configuration)) {
			final EnumGenerator enumGenerator = new EnumGenerator(this, configuration);
			enumGenerator.execute();
		}

		// Generate Services
        if (GeneratorUtil.hasServicePackageName(configuration)) {
			final ServiceGenerator serviceGenerator = new ServiceGenerator(this, configuration);
			serviceGenerator.execute();
		}

        // Generate Declared Queries
        if (configuration.getQueryDeclarations() != null && !configuration.getQueryDeclarations().isEmpty()) {
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
    public String getPlatformName() {
        return platformName;
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
            for (final QueryColumn queryColumn : queryDeclaration.getQueryColumn()) {
                if (queryColumn.getOperator() == null) {
                    queryColumn.setOperator(OperatorName.EQUAL_TO);
                }
            }
        }
    }
}
