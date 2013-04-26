package org.noorm.jdbc;

import java.util.HashMap;

/**
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 20.05.11
 *         Time: 14:27
 */
public interface IBean<T> {

	/**
	 * Generic object to enrich the Bean with additional information.
	 * @return the generic object.
	 */
	T getAuxiliaryData();

	/**
	 * Generic object to enrich the Bean with additional information.
	 * @param pAuxiliaryData
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
	 * The database name of this Bean. Can either be a table or an updateable view.
	 * @return the database name
	 */
	String getTableName();

	/**
	 * The Oracle name of the primary key columns.
	 * @return the primary key column names.
	 */
	String[] getPrimaryKeyColumnNames();

	/**
	 * The Oracle sequence name used to generate numeric primary keys for this table or view.
	 * @return the sequence name.
	 */
	String getSequenceName();

	/**
	 * The table column used for modification checking for optimistic locking.
	 * @return the version column name.
	 */
	String getVersionColumnName();
}
