package org.noorm.jdbc;

/**
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 29.11.11
 *         Time: 11:49
 */
public interface IService {

	/**
	 * Returns the PL/SQL package name, the generated Java Service is based on.
	 * @return the PL/SQL package name.
	 */
	String getDatabasePackageName();

	/**
	 * Validation of the generated service against the database is performed using a hash value calculated on
	 * basis of the full text of the PL/SQL package body used for this service.
	 * @return the calculated, reproducible hash value.
	 */
	int getCodeHashValue();
}
