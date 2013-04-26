package org.noorm.generator.m2plugin;

import org.apache.maven.plugins.annotations.Parameter;

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
	 */
	File getDestinationDirectory();

	/**
	 * Package name for generated Bean source files.
	 */
	String getBeanPackageName();

	/**
	 * Package name for generated Enum source files.
	 */
	String getEnumPackageName();

	/**
	 * Package name for generated Service / DAO source files.
	 */
	String getServicePackageName();

    /**
     * When multiple data sources are used, the data source name as configured in the NoORM configuration file is
     * used to identify different data sources. To specify the data source used for the following statements, one
     * can either set the data source explicitly using DataSourceProvider.setActiveDataSource, or one can specify
     * the data source name here. Note that for explicit transaction handling, one still have to specify the data
     * source name.
     */
    String getDataSourceName();

    /**
	 * JDBC connection URL for the Oracle schema containing the tables, views and stored procedures
	 * subject to Java code generation.
	 */
	String getURL();

	/**
	 * Username for the Oracle schema.
	 */
	String getUsername();

	/**
	 * Password for the Oracle schema.
	 */
	String getPassword();

	/**
	 * List of table name prefixes to be ignored for java class name construction.
	 * Some data modelers use a common table name prefix to identify tables of a
	 * given schema or group. When those prefixes are not desired in the constructed
	 * java class name, they should be listed here.
	 * This setting applies to the bean generator and the enum generator.
	 */
	List<String> getIgnoreTableNamePrefixes();

    /**
     * List of column name prefixes to be ignored for java method name construction.
     * Some data modelers use a common column name prefix to identify columns uniquely
     * for a given table. When those prefixes are not desired in the constructed
     * java method name, they should be listed here.
     * This setting applies to the bean generator and the enum generator.
     */
    List<String> getIgnoreColumnNamePrefixes();

	/**
	 * Regular expression to filter tables and views for Bean generation.
	 */
	String getBeanTableFilterRegex();

	/**
	 * Regular expression to filter tables and views for Enum generation.
	 */

	String getEnumTableFilterRegex();

	/**
	 * To generate Enums from database tables, NoORM must now, which table column should be used
	 * for the enums constant type generation. Typically, a table with constant content has a column
	 * with a code or denominator in uppercase letters, which uniquely identifies the row.
	 */
	Properties getEnumTable2DisplayColumnMapping();

	/**
	 * Primary key generation for new records being inserted into the database is based on
	 * a numeric ID column and an Oracle sequence. Oracle sequences are not tied to a table
	 * by definition, so associating a table with a sequence is done using this property
	 * list. Note that the association TABLE_NAME/SEQUENCE_NAME can either be done on a per
	 * table basis, or using one or more regular expressions to specify a mapping rule like
	 * "TBL_(.*)" -> "SEQ_$1" (This rule would map TBL_PRODUCT to SEQ_PRODUCT, for example).
	 */
	Properties getOracleTable2SequenceMapping();

	/**
	 * Concurrency control can be based on optimistic locking. To identify the version column,
	 * a mapping from the table-name to the version column should be specified. Dependent on
	 * how specific the column-names are with respect to the table-names, one or more
	 * mapping are required. In case of a unique name of the version column for all tables,
	 * one simple rule like ".*" -> "VERSION" is sufficient.
	 */
	Properties getOptLockVersionColumnMapping();

    /**
     * Concurrency control can be based on optimistic locking.
     * When no version column is available for the tables subject to optimistic locking, the complete
     * pre-change image of the row is used to determine database changes, which have occurred in between.
     * The tables listed here are subject to this type of optimistic locking (Do not use both available
     * types of optimistic locking simultaneously).
     */
    String getOptLockFullRowCompareTableRegex();

	/**
	 * The Oracle data dictionary does not provide unambiguous information for the primary key
	 * of a view (for tables, this information is available). When the intended use of a view
	 * includes DML operations (which requires the view to contain one and only one key-preserved
	 * table) or data access with the PageableBeanList, NoORM needs a key to uniquely distinguish
	 * the records of this view. Use this parameter to specify the column name of the key used
	 * for a given view. Typically, this key is the primary key of the single key-preserved table
	 * contained in the view definition.
	 */
	Properties getViewName2PrimaryKeyMapping();

	/**
	 * Regular expression to filter packages for service generation.
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
	 */
	String getSingleRowFinderRegex();

	/**
	 * Large query results can be mapped into a PageableBeanList to provide efficient
	 * access to the data by loading the full record only for the requested page.
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
	 */
	Properties getExtendedBeans();

    /**
     * The NoORM query declaration is intentionally much simpler than most other approaches to specify alternatives
     * to original SQL. While specifications like the JPA 2.0 criteria API aim to cover most of the capabilities of
     * SQL, this approach consequently follows the paradigm to move database-centric functionality to the database.
     * In particular, this means that the complexity of an SQL statement should be implemented inside the database
     * using views. Following this approach, it is almost always possible to reduce the query declaration for
     * automatic Java code generation to a single entity (table, view), the columns subject to the where-conditions
     * and the operators used for the columns in the where-conditions.
     */
    List<QueryDeclaration> getQueryDeclarations();

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
    boolean generatePKBasedEqualsAndHashCode();
}
