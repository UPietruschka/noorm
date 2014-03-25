package org.noorm.generator.m2plugin;

import java.io.File;

/**
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 07.06.12
 *         Time: 15:19
 */
public interface IParameters {

    /**
     * Destination directory for generated source files.
     * @return the destination directory
     */
	File getDestinationDirectory();

    /**
     * File with configuration settings for the NoORM code generator.
     * @return the generator configuration file
     */
    File getGeneratorConfiguration();

    /**
     * The database platform
     * @return the database platform name
     */
    String getPlatformName();

    /**
	 * JDBC connection URL for the database schema containing the tables, views and stored procedures
	 * subject to Java code generation.
     * @return the JDBC connection URL
	 */
	String getURL();

	/**
	 * Username for the database schema.
     * @return the username
	 */
	String getUsername();

	/**
	 * Password for the database schema.
     * @return the password
	 */
	String getPassword();
}
