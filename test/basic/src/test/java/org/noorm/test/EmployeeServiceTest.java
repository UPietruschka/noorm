package org.noorm.test;

import org.junit.Test;
import org.noorm.jdbc.DataSourceProvider;
import org.noorm.test.hr.beans.EmpDetailsView;
import org.noorm.test.hr.beans.Employees;
import org.noorm.test.hr.beans.JobHistory;
import org.noorm.test.hr.beans.Jobs;
import org.noorm.test.hr.beans.SalaryGroupRecord;
import org.noorm.test.hr.services.BeanDML;
import org.noorm.test.hr.services.EmployeeService;

import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 20.12.11
 *         Time: 20:21
 */
public class EmployeeServiceTest {

	private BeanDML beanDML = BeanDML.getInstance();

	@Test
	public void testFindEmployees() {

		// Using explicit transaction handling is actually not required for this test case, but since we
		// do not use a connection pool here, the loop over all employees could easily exhaust the maximum
		// number of available connections of the Oracle test database.
		DataSourceProvider.begin();
        try {
            final EmployeeService employeeService = EmployeeService.getInstance();
            final List<Employees> employeesList = employeeService.findAllEmployees();
            // For brevity and readability of the log, we do not iterate over the whole list
            int i = 0;
            for (final Employees employees : employeesList) {

                final Long employeeId = employees.getEmployeeId();
                final Employees employees0 = employeeService.findUniqueEmployeeById(employeeId);
                assertEquals(employees0, employees);

                final String employeeLastName = employees.getLastName();
                final List<Employees> employeesList2 = employeeService.findEmployeesByLastname(employeeLastName);
                // Multiple employees with the same last name may exist. Just check, if the desired employee is contained
                boolean employeeFound = false;
                for (final Employees employees1 : employeesList2) {
                    if (employees.equals(employees1)) {
                        employeeFound = true;
                    }
                }
                assertEquals(employeeFound, true);
                if (i++ >= 20) { break; }
            }
            DataSourceProvider.commit();
        } catch (Throwable e) {
            DataSourceProvider.rollback();
            fail(e.getMessage());
        }
	}

	/**
	 * This test focuses on a table with primary key and sequence, but without version column
	 */
	@Test
	public void testEmployeesCRUD() {

		DataSourceProvider.begin();
        try {
            // Create a new record and insert it into the database
            final EmployeeService employeeService = EmployeeService.getInstance();
            final Employees employees = new Employees();
            employees.setFirstName("John");
            employees.setLastName("Doe");
            employees.setEmail("JDOE");
            employees.setPhoneNumber("123.456.7890");
            employees.setHireDate(new java.util.Date(1200000000000L));  // January 10, 2008, 22:20
            employees.setJobId("IT_PROG");
            employees.setSalary(5000.00D);
            employees.setCommissionPct(new java.math.BigDecimal(0.28D));
            employees.setManagerId(108L);
            employees.setDepartmentId(60);
            final Employees newEmployees = beanDML.insertEmployees(employees);
            final List<Employees> employeesList1 = employeeService.findEmployeesByLastname("Doe");
            assertEquals(employeesList1.size(), 1);
            assertEquals(employeesList1.get(0), newEmployees);

            // Modify the just inserted record
            newEmployees.setLastName("Public");
            beanDML.updateEmployees(newEmployees);
            final List<Employees> employeesList2 = employeeService.findEmployeesByLastname("Doe");
            assertEquals(employeesList2.size(), 0);
            final List<Employees> employeesList3 = employeeService.findEmployeesByLastname("Public");
            assertEquals(employeesList3.size(), 1);
            assertEquals(employeesList3.get(0), newEmployees);

            // Delete the just inserted record. We have to delete the job history record inserted by the trigger, too
            final List<JobHistory> jobHistoryList =
                    employeeService.findJobHistoryByEmpId(newEmployees.getEmployeeId());
            beanDML.deleteJobHistoryList(jobHistoryList);
            beanDML.deleteEmployees(newEmployees);
            final List<Employees> employeesList4 = employeeService.findEmployeesByLastname("Public");
            assertEquals(employeesList4.size(), 0);

            DataSourceProvider.commit();
        } catch (Throwable e) {
            DataSourceProvider.rollback();
            fail(e.getMessage());
        }
	}

	@Test
	public void testFindEmployeeDetails() {

		// Using explicit transaction handling is actually not required for this test case, but since we
		// do not use a connection pool here, the loop over all employees could easily exhaust the maximum
		// number of available connections of the Oracle test database.
		DataSourceProvider.begin();
        try {
            final EmployeeService employeeService = EmployeeService.getInstance();
            final List<EmpDetailsView> empDetailsList = employeeService.findAllEmployeeDetails();
            // For brevity and readability of the log, we do not iterate over the whole list
            int i = 0;
            for (final EmpDetailsView empDetails : empDetailsList) {

                final Long employeeId = empDetails.getEmployeeId();
                final Employees employees0 = employeeService.findUniqueEmployeeById(employeeId);
                assertNotNull(employees0);
                if (i++ >= 20) { break; }
            }
            DataSourceProvider.commit();
        } catch (Throwable e) {
            DataSourceProvider.rollback();
            fail(e.getMessage());
        }
	}

	@Test
	public void testSalaryGroups() {

		final EmployeeService employeeService = EmployeeService.getInstance();
		final List<SalaryGroupRecord> salaryGroupList = employeeService.findSalaryGroups();
		assertEquals(15, salaryGroupList.size());
		SalaryGroupRecord lowestSalaryGroup = salaryGroupList.get(0);
		Long salaryGroupUpperLimit = lowestSalaryGroup.getSalaryGroup();
		assertEquals(2000L, salaryGroupUpperLimit.longValue());
		Long count = lowestSalaryGroup.getCnt();
		assertEquals(5L, count.longValue());
	}

	/**
	 * This test focuses on a table with non-numeric primary key without sequence and version column
	 */
	@Test
	public void testJobsCRUD() {

		DataSourceProvider.begin();
        try {
            // Create a new record and insert it into the database
            final EmployeeService employeeService = EmployeeService.getInstance();
            final Jobs jobs = new Jobs();
            jobs.setJobId("CT_MGR");
            jobs.setJobTitle("Controller");
            jobs.setMinSalary(8000L);
            jobs.setMaxSalary(12000L);
            final Jobs newJobs =  beanDML.insertJobs(jobs);
            final List<Jobs> jobsList = employeeService.findJobById("CT_MGR");
            assertEquals(jobsList.size(), 1);
            assertEquals(jobsList.get(0), newJobs);

            // Modify the just inserted record
            newJobs.setJobTitle("Controlling");
            beanDML.updateJobs(newJobs);
            final List<Jobs> jobsList2 = employeeService.findJobById("CT_MGR");
            assertEquals(jobsList2.size(), 1);
            assertEquals(jobsList2.get(0).getJobTitle(), "Controlling");

            // Delete the just inserted record.
            beanDML.deleteJobs(newJobs);
            final List<Jobs> jobsList3 = employeeService.findJobById("CT_MGR");
            assertEquals(jobsList3.size(), 0);

            DataSourceProvider.commit();
        } catch (Throwable e) {
            DataSourceProvider.rollback();
            fail(e.getMessage());
        }
	}
}
