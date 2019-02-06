package org.noorm.jdbc;

import java.sql.JDBCType;
import java.util.HashMap;

/**
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 20.05.11
 *         Time: 14:27
 */
public interface IBean<T> {

	/**
	 * Declared queries with FilterExtension and paging can provide a total count for the
	 * underlying query. This total is provided in the virtual column PAGING_TOTAL.
	 */
	public static final String PAGING_TOTAL = "PAGING_TOTAL";

	/**
	 * When paging is used (FilterExtension), one may want to know the total number of rows
	 * retrieved. All NoORM generated beans contain an additional column, which is populate with
	 * the total number of rows.
	 *
	 * @return the total number of rows for a paged query
	 */
	Integer getPagingTotal();

	/**
	 * Generic object to enrich the Bean with additional information.
	 * @return the generic object.
	 */
	T getAuxiliaryData();

	/**
	 * Generic object to enrich the Bean with additional information.
	 * @param pAuxiliaryData the generic object
	 */
	void setAuxiliaryData(final T pAuxiliaryData);

    /**
     * Returns all fields and values, which have been changed after this bean has been loaded and populated.
     * This method provides support for optimistic locking, when optimistic locking is realized by comparing
     * the full pre-change image of the row against the current database row.
     * Note that this method returns null, when this kind of optimistic locking is not enabled for the
     * associated table.
     * @return all fields and values, which have been changed
     */
    HashMap<String, Object> getModifiedFieldsInitialValue();

	/**
	 * The database table name of this Bean.
	 * @return the database name
	 */
	String getTableName();

	/**
	 * The database view name of this Bean.
	 * @return the database name
	 */
	String getViewName();

    /**
     * Indicates, whether the table name is case sensitive.
     * @return true, when the table name is case sensitive
     */
    boolean isTableNameCaseSensitive();

	/**
	 * The name of the primary key columns.
	 * @return the primary key column names.
	 */
	String[] getPrimaryKeyColumnNames();

    /**
     * The name of the primary key Java attributes.
     * @return the primary key column names.
     */
    String[] getPrimaryKeyJavaNames();

	/**
	 * The sequence name used to generate numeric primary keys for this table or view.
	 * @return the sequence name.
	 */
	String getSequenceName();

    /**
     * The increment for the sequence used to generate primary key values. Using an increment larger than 1
     * allows the DataSourceProvider to cache sequence values without the need for a database round-trip.
     * @return the sequence increment
     */
    Long getSequenceIncrement();

    /**
     * Determines, whether sequence values for primary keys should be generated inline
     * @return true, when the sequence value generation should be inlined with the INSERT statement.
     */
    boolean useInlineSequenceValueGeneration();

	/**
	 * The table column used for modification checking for optimistic locking.
	 * @return the version column name.
	 */
	String getVersionColumnName();

    /**
     * The name of the Java instance variable holding the version column.
     * @return the version column name.
     */
    String getVersionColumnJavaName();

    /**
     * The table column type used for modification checking for optimistic locking.
     * @return the version column type.
     */
    JDBCType getVersionColumnType();
}
