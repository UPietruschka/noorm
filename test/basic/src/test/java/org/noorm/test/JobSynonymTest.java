package org.noorm.test;

import org.junit.Test;
import org.noorm.jdbc.DataSourceProvider;
import org.noorm.test.hr.beans.JobsSynonym;
import org.noorm.test.hr.services.DeclaredQueries;
import org.noorm.test.hr.services.JobsSynonymDML;

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
        final List<JobsSynonym> jobs = queries.findJobsSynonymsBySalary(3000L, 10000L);
        assertEquals(JOB_COUNT, jobs.size());
    }

    @Test
    public void testJobSynonymCRUD() {

        DataSourceProvider.begin();
        try {
            final JobsSynonym job = new JobsSynonym();
            job.setJobId("TEST_ID");
            job.setJobTitle("Test Title");
            job.setMinSalary(0L);
            job.setMaxSalary(1L);
            JobsSynonymDML jobsSynonymDML = JobsSynonymDML.getInstance();
            final JobsSynonym job0 = jobsSynonymDML.insertJobsSynonym(job);
            job0.setMaxSalary(2L);
            jobsSynonymDML.updateJobsSynonym(job0);
            jobsSynonymDML.deleteJobsSynonym(job0);
            DataSourceProvider.commit();
        } catch (Throwable e) {
            DataSourceProvider.rollback();
            fail(e.getMessage());
        }
    }
}
