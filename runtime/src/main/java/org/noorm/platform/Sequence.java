package org.noorm.platform;

/**
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 29.03.11
 *         Time: 17:47
 */
public class Sequence {

	private String name;
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
