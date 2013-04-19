package org.noorm.test;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.noorm.jdbc.DataSourceProvider;
import org.noorm.test.hr1.services.IEmployeeService;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 01.11.12
 *         Time: 20:21
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/META-INF/services.xml"})
public class NestedTransactionTest {

    private static final String HR1_DATA_SOURCE = "HR1";

    @Resource
    private org.noorm.test.hr1.services.IBeanDML beanDML_HR1;
    @Resource
    private IEmployeeService employeeService;

    @Test
    public void testNestedTSLevel1() {

        try {
            DataSourceProvider.setActiveDataSource(HR1_DATA_SOURCE);
            DataSourceProvider.begin();
            changeJob();
            tsLevel2();
            DataSourceProvider.commit();
        } catch (Throwable e) {
            DataSourceProvider.rollback();
            fail(e.getMessage());
        }
    }

    private void tsLevel2() {

        try {
            DataSourceProvider.setActiveDataSource(HR1_DATA_SOURCE);
            DataSourceProvider.begin();
            changeJob();
            tsLevel3();
            DataSourceProvider.commit();
        } catch (Throwable e) {
            DataSourceProvider.rollback();
            fail(e.getMessage());
        }
    }

    private void tsLevel3() {

        try {
            DataSourceProvider.setActiveDataSource(HR1_DATA_SOURCE);
            DataSourceProvider.begin();
            changeJob();
            DataSourceProvider.commit();
        } catch (Throwable e) {
            DataSourceProvider.rollback();
            fail(e.getMessage());
        }
    }

    private void changeJob() {

        final List<JobsBeanExt> jobsBeanList = employeeService.findJobById("IT_PROG");
        assertEquals(1, jobsBeanList.size());
        JobsBeanExt job = jobsBeanList.get(0);
        job.setMaxSalary(10000L);
        beanDML_HR1.updateJobs(job);
    }
}
