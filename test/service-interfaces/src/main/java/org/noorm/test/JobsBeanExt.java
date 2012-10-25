package org.noorm.test;

import org.noorm.test.hr1.beans.JobsBean;

/**
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 17.02.12
 *         Time: 22:21
 */
public class JobsBeanExt extends JobsBean {

    /**
     * This is an example for an arbitrary extension to the information contained in
     * the database entity. The basic job data is stored in the database, but in the
     * business layer, other data source may be available providing additional information
     * about the current record.
     */
    private Long expenses;

    public Long getExpenses() {
        return expenses;
    }

    public void setExpenses(final Long pExpenses) {
        expenses = pExpenses;
    }
}
