package org.noorm.validation;

import org.noorm.jdbc.DataSourceProvider;
import org.noorm.jdbc.IService;
import org.noorm.jdbc.platform.IMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 29.11.11
 *         Time: 20:57
 */
public class ServiceValidator {

	private static final Logger log = LoggerFactory.getLogger(ServiceValidator.class);

	public void validateService(final IService pService) {

        final IMetadata metadata = DataSourceProvider.getPlatform().getMetadata();

		final String databasePackageName = pService.getDatabasePackageName().toUpperCase();
		log.debug("Retrieving PL/SQL package code checksum (hash) from database.");
		final String codeHashValue = metadata.getPackageHashValue(databasePackageName);
		if (codeHashValue == null) {
			validationError("Service ".concat(pService.getClass().getName())
					.concat(" could not be validated against PL/SQL package code for ")
					.concat(databasePackageName).concat(" (No database package found with this name)."));
		}
		if (codeHashValue.equals(pService.getCodeHashValue())) {
			log.info("Service ".concat(pService.getClass().getName())
					.concat(" successfully validated against PL/SQL database code for package ")
					.concat(databasePackageName));
		} else {
			validationError("Service ".concat(pService.getClass().getName())
					.concat(" could not be validated against PL/SQL database code for package ")
					.concat(databasePackageName).concat(" (Checksum (hash) for package code has changed)."));
		}
	}

	/*
	 Validation errors result in a validation exception.
	 For a web application, the validators may get called from inside of a ServletContextListener implementation,
	 which does not write to the NoORM logger or application logger. Thus, validation errors are explicitly logged.
	 */
	private void validationError(final String pErrorMessage) {

		log.error(pErrorMessage);
		throw new ValidationException(pErrorMessage);
	}
}
