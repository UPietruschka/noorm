package org.noorm.test;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.noorm.jdbc.DataSourceProvider;
import org.noorm.test.hr2.beans.EmployeesBean;
import org.noorm.test.hr2.services.IEmployeeDAO;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 23.01.13
 *         Time: 13:20
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/META-INF/services.xml"})
public class DeclaredQueriesTest {

    private static final String HR2_DATA_SOURCE = "HR2";

    @Resource
    private IEmployeeDAO employeeDAO;

    @Test
    public void testFindEmployees1() {

        DataSourceProvider.setActiveDataSource(HR2_DATA_SOURCE);
        final Double salaryLimit = 4000.0;
        final Double commissionLimit = 0.2;
        List<EmployeesBean> employees = employeeDAO.findEmployees(salaryLimit, commissionLimit);
        assertEquals(10, employees.size());
    }
}
