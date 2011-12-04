package org.noorm.jdbc;

/**
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 30.11.11
 *         Time: 13:32
 */
public interface IEnum {

	/**
	 * The database name of this Enum. Can either be a table or a view.
	 * @return the database name
	 */
	String getTableName();
	/**
	 * The name of the database column used to give the particular enum its display name.
	 * @return the database name
	 */
	String getTypeColumnName();
}
