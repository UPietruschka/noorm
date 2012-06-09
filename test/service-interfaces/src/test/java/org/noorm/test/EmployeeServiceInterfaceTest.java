package org.noorm.test;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.noorm.jdbc.DataSourceProvider;
import org.noorm.test.hr.beans.EmpDetailsViewBean;
import org.noorm.test.hr.beans.EmployeesBean;
import org.noorm.test.hr.beans.HistoryBean;
import org.noorm.test.hr.services.IBeanDML;
import org.noorm.test.hr.services.IEmployeeService;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 20.12.11
 *         Time: 20:21
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/META-INF/services.xml"})
public class EmployeeServiceInterfaceTest {

	@Resource
	private IBeanDML beanDML;
	@Resource
    private IEmployeeService employeeService;

	@Test
	public void testFindEmployees() {

		// Using explicit transaction handling is actually not required for this test case, but since we
		// do not use a connection pool here, the loop over all employees could easily exhaust the maximum
		// number of available connections of the Oracle test database.
		DataSourceProvider.begin();
		final List<EmployeesBean> employeesBeanList = employeeService.findAllEmployees();
		for (final EmployeesBean employeesBean : employeesBeanList) {

			final Long employeeId = employeesBean.getEmployeeId();
			final EmployeesBean employeesBean1 = employeeService.findUniqueEmployeeById(employeeId);
			assertEquals(employeesBean1, employeesBean);

			final String employeeLastName = employeesBean.getLastName();
			final List<EmployeesBean> employeesBeanList2 = employeeService.findEmployeesByLastname(employeeLastName);
			// Multiple employees with the same last name may exist. Just check, if the desired employee is contained
			boolean employeeFound = false;
			for (final EmployeesBean employeesBean2 : employeesBeanList2) {
				if (employeesBean.equals(employeesBean2)) {
					employeeFound = true;
				}
			}
			assertEquals(employeeFound, true);
		}
		DataSourceProvider.commit();
	}

	/**
	 * This test focuses on a table with primary key and sequence, but without version column
	 */
	@Test
	public void testEmployeesCRUD() {

		DataSourceProvider.begin();

		// Create a new record and insert it into the database
		final EmployeesBean employeesBean = new EmployeesBean();
		employeesBean.setFirstName("John");
		employeesBean.setLastName("Doe");
		employeesBean.setEmail("JDOE");
		employeesBean.setPhoneNumber("123.456.7890");
		employeesBean.setHireDate(new java.util.Date(1200000000000L));  // January 10, 2008, 22:20
		employeesBean.setJobId("IT_PROG");
		employeesBean.setSalary(5000.00D);
		employeesBean.setCommissionPct(0.28D);
		employeesBean.setManagerId(108L);
		employeesBean.setDepartmentId(60L);
		final EmployeesBean newEmployeesBean = beanDML.insertEmployees(employeesBean);
		final List<EmployeesBean> employeesBeanList1 = employeeService.findEmployeesByLastname("Doe");
		assertEquals(employeesBeanList1.size(), 1);
		assertEquals(employeesBeanList1.get(0), newEmployeesBean);

		// Modify the just inserted record
		newEmployeesBean.setLastName("Public");
		beanDML.updateEmployees(newEmployeesBean);
		final List<EmployeesBean> employeesBeanList2 = employeeService.findEmployeesByLastname("Doe");
		assertEquals(employeesBeanList2.size(), 0);
		final List<EmployeesBean> employeesBeanList3 = employeeService.findEmployeesByLastname("Public");
		assertEquals(employeesBeanList3.size(), 1);
		assertEquals(employeesBeanList3.get(0), newEmployeesBean);

		// Delete the just inserted record. We have to delete the job history record inserted by the trigger, too
		final List<HistoryBean> jobHistoryBeanList =
				employeeService.findJobHistoryByEmpId(newEmployeesBean.getEmployeeId());
		beanDML.deleteHistoryList(jobHistoryBeanList);
		beanDML.deleteEmployees(newEmployeesBean);
		final List<EmployeesBean> employeesBeanList4 = employeeService.findEmployeesByLastname("Public");
		assertEquals(employeesBeanList4.size(), 0);

		DataSourceProvider.commit();
	}

	@Test
	public void testFindEmployeeDetails() {

		// Using explicit transaction handling is actually not required for this test case, but since we
		// do not use a connection pool here, the loop over all employees could easily exhaust the maximum
		// number of available connections of the Oracle test database.
		DataSourceProvider.begin();
		final List<EmpDetailsViewBean> empDetailsBeanList = employeeService.findAllEmployeeDetails();
		for (final EmpDetailsViewBean empDetailsBean : empDetailsBeanList) {

			final Long employeeId = empDetailsBean.getEmployeeId();
			final EmployeesBean employeesBean1 = employeeService.findUniqueEmployeeById(employeeId);
			assertNotNull(employeesBean1);
		}
		DataSourceProvider.commit();
	}

	/**
	 * This test focuses on a table with non-numeric primary key without sequence and version column
	 */
	@Test
	public void testJobsCRUD() {

		DataSourceProvider.begin();

		// Create a new record and insert it into the database
		final JobsBeanExt jobsBean = new JobsBeanExt();
		jobsBean.setJobId("CT_MGR");
		jobsBean.setJobTitle("Controller");
		jobsBean.setMinSalary(8000L);
		jobsBean.setMaxSalary(12000L);
		final JobsBeanExt newJobsBean =  beanDML.insertJobs(jobsBean);
		final List<JobsBeanExt> jobsBeanList = employeeService.findJobById("CT_MGR");
		assertEquals(jobsBeanList.size(), 1);
		assertEquals(jobsBeanList.get(0), newJobsBean);

		// Modify the just inserted record
		newJobsBean.setJobTitle("Controlling");
		beanDML.updateJobs(newJobsBean);
		final List<JobsBeanExt> jobsBeanList2 = employeeService.findJobById("CT_MGR");
		assertEquals(jobsBeanList2.size(), 1);
		assertEquals(jobsBeanList2.get(0).getJobTitle(), "Controlling");

		// Delete the just inserted record.
		beanDML.deleteJobs(newJobsBean);
		final List<JobsBeanExt> jobsBeanList3 = employeeService.findJobById("CT_MGR");
		assertEquals(jobsBeanList3.size(), 0);

		DataSourceProvider.commit();
	}
}
