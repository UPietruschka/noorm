package org.noorm.jdbc.platform;

/**
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 29.03.11
 *         Time: 17:47
 */
public class Sequence {

	private String name;
    private Integer incrementBy;

    public String getName() {
		return name;
	}

	public void setName(final String pName) {
		name = pName;
	}

    public Integer getIncrementBy() {
        return incrementBy;
    }

    public void setIncrementBy(final Integer pIncrementBy) {
        incrementBy = pIncrementBy;
    }
}
