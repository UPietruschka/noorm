package org.noorm.test;

import org.junit.Test;
import org.noorm.jdbc.DataSourceProvider;
import org.noorm.test.hr.beans.JobsSynonymBean;
import org.noorm.test.hr.services.BeanDML;
import org.noorm.test.hr.services.DeclaredQueries;

import java.util.List;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 14.03.13
 *         Time: 14:23
 */
public class JobSynonymTest {

    private static final int JOB_COUNT = 7;

    @Test
    public void testGetEmployeeCount() {

        DeclaredQueries queries = DeclaredQueries.getInstance();
        final List<JobsSynonymBean> jobs = queries.findJobsSynonymsBySalary(3000L, 10000L);
        assertEquals(JOB_COUNT, jobs.size());
    }

    @Test
    public void testJobSynonymCRUD() {

        DataSourceProvider.begin();
        try {
            final JobsSynonymBean job = new JobsSynonymBean();
            job.setJobId("TEST_ID");
            job.setJobTitle("Test Title");
            job.setMinSalary(0L);
            job.setMaxSalary(1L);
            BeanDML beanDML = BeanDML.getInstance();
            final JobsSynonymBean job0 = beanDML.insertJobsSynonym(job);
            job0.setMaxSalary(2L);
            beanDML.updateJobsSynonym(job0);
            beanDML.deleteJobsSynonym(job0);
            DataSourceProvider.commit();
        } catch (Error e) {
            DataSourceProvider.rollback();
            fail(e.getMessage());
        }
    }
}
