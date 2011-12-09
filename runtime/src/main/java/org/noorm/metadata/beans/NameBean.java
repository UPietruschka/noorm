package org.noorm.metadata.beans;

import org.noorm.jdbc.JDBCColumn;

/**
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 15.05.11
 *         Time: 17:47
 */
public class NameBean {

	@JDBCColumn(name="NAME", updatable=false)
	private String name;

	public String getName() {
		return name;
	}

	public void setName(final String pName) {
		name = pName;
	}
}