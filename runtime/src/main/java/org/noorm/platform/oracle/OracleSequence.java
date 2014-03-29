package org.noorm.platform.oracle;

import org.noorm.jdbc.JDBCColumn;

/**
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 15.05.11
 *         Time: 17:47
 */
public class OracleSequence {

	@JDBCColumn(name="SEQUENCE_NAME", updatable=false)
	private String name;

    @JDBCColumn(name="INCREMENT_BY", updatable=false)
    private Long incrementBy;

    public String getName() {
		return name;
	}

	public void setName(final String pName) {
		name = pName;
	}

    public Long getIncrementBy() {
        return incrementBy;
    }

    public void setIncrementBy(final Long pIncrementBy) {
        incrementBy = pIncrementBy;
    }
}
