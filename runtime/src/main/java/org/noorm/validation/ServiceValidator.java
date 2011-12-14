package org.noorm.validation;

import org.noorm.jdbc.IService;
import org.noorm.metadata.MetadataService;
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

		final MetadataService metadataService = MetadataService.getInstance();

		final String databasePackageName = pService.getDatabasePackageName().toUpperCase();
		log.debug("Retrieving PL/SQL package code checksum (hash) from database.");
		final int codeHashValue = metadataService.getPackageHashValue(databasePackageName);
		if (codeHashValue == -1) {
			throw new ValidationException("Service ".concat(pService.getClass().getName())
					.concat(" could not be validated against PL/SQL package code for ")
					.concat(databasePackageName).concat(" (No database package found with this name)."));
		}
		if (codeHashValue == pService.getCodeHashValue()) {
			log.info("Service ".concat(pService.getClass().getName())
					.concat(" successfully validated against PL/SQL database code for package ")
					.concat(databasePackageName));
		} else {
			throw new ValidationException("Service ".concat(pService.getClass().getName())
					.concat(" could not be validated against PL/SQL database code for package ")
					.concat(databasePackageName).concat(" (Checksum (hash) for package code has changed)."));
		}
	}
}
