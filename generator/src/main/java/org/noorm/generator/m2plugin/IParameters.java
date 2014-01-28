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
	 */
	File getDestinationDirectory();

    /**
     * File with configuration settings for the NoORM code generator.
     */
    File getGeneratorConfiguration();

    /**
	 * JDBC connection URL for the database schema containing the tables, views and stored procedures
	 * subject to Java code generation.
	 */
	String getURL();

	/**
	 * Username for the database schema.
	 */
	String getUsername();

	/**
	 * Password for the database schema.
	 */
	String getPassword();
}
