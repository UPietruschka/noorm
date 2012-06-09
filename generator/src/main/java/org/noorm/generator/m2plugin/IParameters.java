package org.noorm.generator.m2plugin;

import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.List;
import java.util.Properties;

/**
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 07.06.12
 *         Time: 15:19
 */
public interface IParameters {

	/**
	 * Destination directory for generated source files.
	 *
	 * @parameter expression="${project.build.directory}/generated-sources/noorm"
	 */
	File getDestinationDirectory();

	/**
	 * Package name for generated Bean source files.
	 *
	 * @parameter
	 * @required
	 */
	String getBeanPackageName();

	/**
	 * Package name for generated Enum source files.
	 *
	 * @parameter
	 */
	String getEnumPackageName();

	/**
	 * Package name for generated Service / DAO source files.
	 *
	 * @parameter
	 * @required
	 */
	String getServicePackageName();

	/**
	 * JDBC connection URL for the Oracle schema containing the tables, views and stored procedures
	 * subject to Java code generation.
	 *
	 * @parameter
	 * @required
	 */
	String getURL();

	/**
	 * Username for the Oracle schema.
	 *
	 * @parameter
	 * @required
	 */
	String getUsername();

	/**
	 * Password for the Oracle schema.
	 *
	 * @parameter
	 * @required
	 */
	String getPassword();

	/**
	 * List of table name prefixes to be ignored for java class name construction.
	 * Some data modelers use a common table name prefix to identify tables of a
	 * given schema or group. When those prefixes are not desired in the constructed
	 * java class name, they should be listed here.
	 * This setting applies to the bean generator and the enum generator.
	 *
	 * @parameter
	 */
	List<String> getIgnoreTableNamePrefixes();

	/**
	 * Regular expression to filter tables and views for Bean generation.
	 *
	 * @parameter
	 */
	String getBeanTableFilterRegex();

	/**
	 * Regular expression to filter tables and views for Enum generation.
	 *
	 * @parameter
	 */

	String getEnumTableFilterRegex();

	/**
	 * To generate Enums from database tables, NoORM must now, which table column should be used
	 * for the enums constant type generation. Typically, a table with constant content has a column
	 * with a code or denominator in uppercase letters, which uniquely identifies the row.
	 *
	 * @parameter
	 */
	Properties getEnumTable2DisplayColumnMapping();

	/**
	 * Primary key generation for new records being inserted into the database is based on
	 * a numeric ID column and an Oracle sequence. Oracle sequences are not tied to a table
	 * by definition, so associating a table with a sequence is done using this property
	 * list. Note that the association TABLE_NAME/SEQUENCE_NAME can either be done on a per
	 * table basis, or using one or more regular expressions to specify a mapping rule like
	 * "TBL_(.*)" -> "SEQ_$1" (This rule would map TBL_PRODUCT to SEQ_PRODUCT, for example).
	 *
	 * @parameter
	 */
	Properties getOracleTable2SequenceMapping();

	/**
	 * Concurrency control is based on optimistic locking. To identify the version column,
	 * a mapping from the table-name to the version column should be specified. Dependent on
	 * how specific the column-names are with respect to the table-names, one or more
	 * mapping are required. In case of a unique name of the version column for all tables,
	 * one simple rule like ".*" -> "VERSION" is sufficient.
	 *
	 * @parameter
	 */
	Properties getOptimisticLockColumnMapping();

	/**
	 * The Oracle data dictionary does not provide unambiguous information for the primary key
	 * of a view (for tables, this information is available). When the intended use of a view
	 * includes DML operations (which requires the view to contain one and only one key-preserved
	 * table) or data access with the PageableBeanList, NoORM needs a key to uniquely distinguish
	 * the records of this view. Use this parameter to specify the column name of the key used
	 * for a given view. Typically, this key is the primary key of the single key-preserved table
	 * contained in the view definition.
	 *
	 * @parameter
	 */
	Properties getViewName2PrimaryKeyMapping();

	/**
	 * Regular expression to filter packages for service generation.
	 *
	 * @parameter
	 */
	String getPackageFilterRegex();

	/**
	 * Accessing data mapped to Beans always uses an Oracle reference cursor to
	 * retrieve the data. However, using the PL/SQL procedures signature does
	 * not provide any hint, if the reference cursor being returned is limited
	 * to a single row (which in turn changes the signature of the generated Java
	 * code, instead of a List a single Bean is returned).
	 *
	 * Use this parameter to specify a regular expression matching all procedure
	 * names subject to single row retrieval.
	 *
	 * @parameter
	 */
	String getSingleRowFinderRegex();

	/**
	 * Large query results can be mapped into a PageableBeanList to provide efficient
	 * access to the data by loading the full record only for the requested page.
	 *
	 * @parameter
	 */
	String getPageableProcedureNameRegex();

	/**
	 * Services and DAOs generated by NoORM are singletons. By default, the services
	 * and DAOs are instantiated right after class loading and provided by the class method
	 * "getInstance".
	 * Alternatively, class instantiation can be delegated to a dependency injection framework
	 * like Spring. By specifying parameter serviceInterfacePackageName, the service generator
	 * is directed to omit the in-class singleton implementation and generate appropriate
	 * interfaces for every service, resp. DAO in the given package.
	 *
	 * @parameter
	 */
	String getServiceInterfacePackageName();

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
	 *
	 * @parameter
	 */
	Properties getExtendedBeans();
}
