package org.noorm.jdbc;

import java.io.Serializable;

/**
 * Generic Bean class managing ID lists.
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 */
public class IDBean implements Serializable {

	private static final long serialVersionUID = 3528474153713464561L;

	@JDBCColumn(name="ID")
	private Long id;

	public Long getId() {
		return id;
	}

	public void setId(final Long pId) {
		this.id = pId;
	}
}
