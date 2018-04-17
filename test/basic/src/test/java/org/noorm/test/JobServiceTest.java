package org.noorm.test;

import org.junit.Test;
import org.noorm.jdbc.DataSourceProvider;
import org.noorm.test.hr.beans.Employees;
import org.noorm.test.hr.services.EmployeeFinder;
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
		
		final JobService jobService = JobService.getInstance();
		final Long employeeCount = jobService.getEmployeeCount();
		assertEquals(EMPLOYEE_COUNT, employeeCount);
	}

	@Test
	public void testIncreaseSalary() {

		DataSourceProvider.begin();
        try {
            final JobService jobService = JobService.getInstance();
            // Increase the salary for the given department by 8%
            jobService.increaseSalary(90, 0.08d);

            final EmployeeFinder employeeFinder = EmployeeFinder.getInstance();
            // Search for an exact match of the first employee with increased salary
            final List<Employees> employeesList0 = employeeFinder.findEmployeesDynamic
                    (null, null, null,
                            25920D, 25920D, null);
            assertEquals(1, employeesList0.size());
            // Search for an exact match of the second and third employee with increased salary
            final List<Employees> employeesList1 = employeeFinder.findEmployeesDynamic
                    (null, null, null,
                            18360D, 18360D, null);
            assertEquals(2, employeesList1.size());
        } finally {
            DataSourceProvider.rollback();
        }
	}
}
