package org.noorm.validation;

import org.noorm.jdbc.JDBCProcedureProcessor;
import org.noorm.platform.oracle.OracleMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 06.12.11
 *         Time: 18:56
 */
public class NoORMValidator {

	private static final Logger log = LoggerFactory.getLogger(NoORMValidator.class);

	private static final String POM_PROPERTIES_PATH = "/META-INF/maven/org.noorm/noorm-runtime/pom.properties";

	private static final Properties pomProperties = new Properties();

	/**
	 * Validates the NoORM PL/SQL package NOORM_DYNAMIC_SQL against the Java software version
	 * of the NoORM runtime.
	 * Validation is done using a version string infiltrated into the PL/SQL creation scripts.
	 */
	public static void validateNoORMDynamicSQLVersion() {

        final String noormJavaVersion = getNoORMJavaVersion();
        // There is no explicit Java wrapper for the NOORM_DYNAMIC_SQL package, since this package is not to
		// be used from Java directly. To retrieve the NoORM version, we use the JDBCProcedureProcessor API.
		log.info("Validating NoORM runtime version [".concat(noormJavaVersion)
				.concat("] against PL/SQL package NOORM_DYNAMIC_SQL."));
		final JDBCProcedureProcessor<String> statementProcessor = JDBCProcedureProcessor.getInstance();
		final Map<String, Object> filterParameters = new HashMap<String, Object>();
		final String dynamicSQLNoORMVersion = statementProcessor.callPLSQL
				("noorm_dynamic_sql.get_version", "p_version", filterParameters, String.class);
		if (!dynamicSQLNoORMVersion.equals(noormJavaVersion)) {
			final String errMsg = "NoORM Java runtime version ".concat(noormJavaVersion)
					.concat(" does not match version ").concat(dynamicSQLNoORMVersion)
					.concat(" of PL/SQL package NOORM_DYNAMIC_SQL.");
			log.error(errMsg);
			throw new ValidationException(errMsg);
		}
	}

    /**
     * Validates the NoORM PL/SQL package NOORM_METADATA against the Java software version
     * of the NoORM runtime.
     * Validation is done using a version string infiltrated into the PL/SQL creation scripts.
     */
    public static void validateNoORMMetadataVersion() {

        final String noormJavaVersion = getNoORMJavaVersion();
        log.info("Validating NoORM runtime version [".concat(noormJavaVersion)
                .concat("] against PL/SQL package NOORM_METADATA."));
        final String metaDataServiceNoORMVersion = OracleMetadata.getInstance().getVersion();
        if (!metaDataServiceNoORMVersion.equals(noormJavaVersion)) {
            final String errMsg = "NoORM Java runtime version ".concat(noormJavaVersion)
                    .concat(" does not match version ").concat(metaDataServiceNoORMVersion)
                    .concat(" of PL/SQL package NOORM_METADATA.");
            log.error(errMsg);
            throw new ValidationException(errMsg);
        }
        log.info("Validation of NoORM runtime version [".concat(noormJavaVersion)
                .concat("] against PL/SQL packages successful."));
    }

    private static String getNoORMJavaVersion() {

        final InputStream resourceStream = NoORMValidator.class.getResourceAsStream(POM_PROPERTIES_PATH);
        try {
            pomProperties.load(resourceStream);
        } catch (IOException e) {
            log.error(e.getMessage());
            throw new ValidationException("Could not read POM property file ".concat(POM_PROPERTIES_PATH), e);
        }
        return pomProperties.getProperty("version");
    }
}
