package org.noorm.test;

import org.junit.Test;
import org.noorm.jdbc.DataSourceProvider;
import org.noorm.test.hr.beans.EmployeesBean;
import org.noorm.test.hr.services.EmployeeSearch;
import org.noorm.test.hr.services.JobService;

import java.util.List;

import static junit.framework.Assert.assertEquals;

/**
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 31.01.12
 *         Time: 18:25
 */
public class JobServiceTest {

	private static final Long EMPLOYEE_COUNT = 107L;

	@Test
	public void testGetEmployeeCount() {
		
		JobService jobService = JobService.getInstance();
		final Long employeeCount = jobService.getEmployeeCount();
		assertEquals(EMPLOYEE_COUNT, employeeCount);
	}

	@Test
	public void testIncreaseSalary() {

		DataSourceProvider.begin();
        try {
            JobService jobService = JobService.getInstance();
            // Increase the salary for the given department by 8%
            jobService.increaseSalary(90, 0.08d);
            EmployeeSearch employeeSearch = EmployeeSearch.getInstance();
            // Search for an exact match of the first employee with increased salary
            List<EmployeesBean> employeesBeanList0 =
                    employeeSearch.findEmployeesByFilter(null, null, null, null, 25920L, 25920L);
            assertEquals(1, employeesBeanList0.size());
            // Search for an exact match of the second and third employee with increased salary
            List<EmployeesBean> employeesBeanList1 =
                    employeeSearch.findEmployeesByFilter(null, null, null, null, 18360L, 18360L);
            assertEquals(2, employeesBeanList1.size());
        } finally {
            DataSourceProvider.rollback();
        }
	}
}
