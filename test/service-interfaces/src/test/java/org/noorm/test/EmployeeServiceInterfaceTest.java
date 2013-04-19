package org.noorm.test;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.noorm.jdbc.DataSourceProvider;
import org.noorm.test.hr1.beans.EmpDetailsViewBean;
import org.noorm.test.hr1.beans.EmployeesBean;
import org.noorm.test.hr1.beans.HistoryBean;
import org.noorm.test.hr1.services.IEmployeeService;
import org.noorm.test.hr2.beans.DepartmentsBean;
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

    private static final String HR1_DATA_SOURCE = "HR1";
    private static final String HR2_DATA_SOURCE = "HR2";

    @Resource
    private org.noorm.test.hr1.services.IBeanDML beanDML_HR1;
    @Resource
    private org.noorm.test.hr2.services.IBeanDML beanDML_HR2;
    @Resource
    private IEmployeeService employeeService;

    @Test
    public void testFindEmployees() {

        // Using explicit transaction handling is actually not required for this test case, but since we
        // do not use a connection pool here, the loop over all employees could easily exhaust the maximum
        // number of available connections of the Oracle test database.
        try {
            DataSourceProvider.setActiveDataSource(HR1_DATA_SOURCE);
            DataSourceProvider.begin();
            final List<EmployeesBean> employeesBeanList = employeeService.findAllEmployees();
            for (final EmployeesBean employeesBean : employeesBeanList) {

                final Long employeeId = employeesBean.getEmployeeId();
                final EmployeesBean employeesBean1 = employeeService.findUniqueEmployeeById(employeeId);
                assertEquals(employeesBean, employeesBean1);

                final String employeeLastName = employeesBean.getLastName();
                final List<EmployeesBean> employeesBeanList2 = employeeService.findEmployeesByLastname(employeeLastName);
                // Multiple employees with the same last name may exist. Just check, if the desired employee is contained
                boolean employeeFound = false;
                for (final EmployeesBean employeesBean2 : employeesBeanList2) {
                    if (employeesBean.equals(employeesBean2)) {
                        employeeFound = true;
                    }
                }
                assertEquals(true, employeeFound);
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

        try {
            DataSourceProvider.setActiveDataSource(HR1_DATA_SOURCE);
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
            final EmployeesBean newEmployeesBean = beanDML_HR1.insertEmployees(employeesBean);
            final List<EmployeesBean> employeesBeanList1 = employeeService.findEmployeesByLastname("Doe");
            assertEquals(1, employeesBeanList1.size());
            assertEquals(newEmployeesBean, employeesBeanList1.get(0));

            // Modify the just inserted record
            newEmployeesBean.setLastName("Public");
            beanDML_HR1.updateEmployees(newEmployeesBean);
            final List<EmployeesBean> employeesBeanList2 = employeeService.findEmployeesByLastname("Doe");
            assertEquals(0, employeesBeanList2.size());
            final List<EmployeesBean> employeesBeanList3 = employeeService.findEmployeesByLastname("Public");
            assertEquals(1, employeesBeanList3.size());
            assertEquals(newEmployeesBean, employeesBeanList3.get(0));

            DataSourceProvider.setActiveDataSource(HR1_DATA_SOURCE);
            // Delete the just inserted record. We have to delete the job history record inserted by the trigger, too
            final List<HistoryBean> jobHistoryBeanList =
                    employeeService.findJobHistoryByEmpId(newEmployeesBean.getEmployeeId());
            beanDML_HR1.deleteHistoryList(jobHistoryBeanList);
            beanDML_HR1.deleteEmployees(newEmployeesBean);
            final List<EmployeesBean> employeesBeanList4 = employeeService.findEmployeesByLastname("Public");
            assertEquals(0, employeesBeanList4.size());

            DataSourceProvider.commit();
        } catch (Throwable e) {
            DataSourceProvider.rollback();
            fail(e.getMessage());
        }
    }

    @Test
    public void testSecondaryDataSource() {

        try {
            DataSourceProvider.setActiveDataSource(HR2_DATA_SOURCE);
            DataSourceProvider.begin();
            // Create a new record and insert it into the database
            final DepartmentsBean department = new DepartmentsBean();
            department.setDepartmentName("New Department");
            final DepartmentsBean newDepartment = beanDML_HR2.insertDepartments(department);

            // Modify the just inserted record
            newDepartment.setDepartmentName("Updated Department");
            beanDML_HR2.updateDepartments(newDepartment);
            beanDML_HR2.deleteDepartments(newDepartment);
            DataSourceProvider.commit();
        } catch (Throwable e) {
            DataSourceProvider.rollback();
            fail(e.getMessage());
        }
    }

    @Test
    public void testFindEmployeeDetails() {

        try {
            // Using explicit transaction handling is actually not required for this test case, but since we
            // do not use a connection pool here, the loop over all employees could easily exhaust the maximum
            // number of available connections of the Oracle test database.
            DataSourceProvider.setActiveDataSource(HR1_DATA_SOURCE);
            DataSourceProvider.begin();
            final List<EmpDetailsViewBean> empDetailsBeanList = employeeService.findAllEmployeeDetails();
            for (final EmpDetailsViewBean empDetailsBean : empDetailsBeanList) {

                final Long employeeId = empDetailsBean.getEmployeeId();
                final EmployeesBean employeesBean1 = employeeService.findUniqueEmployeeById(employeeId);
                assertNotNull(employeesBean1);
            }
            DataSourceProvider.commit();
        } catch (Throwable e) {
            DataSourceProvider.rollback();
            fail(e.getMessage());
        }
    }

    /**
     * This test focuses on a table with non-numeric primary key without sequence and version column
     */
    @Test
    public void testJobsCRUD() {

        try {
            DataSourceProvider.setActiveDataSource(HR1_DATA_SOURCE);
            DataSourceProvider.begin();

            // Create a new record and insert it into the database
            final JobsBeanExt jobsBean = new JobsBeanExt();
            jobsBean.setJobId("CT_MGR");
            jobsBean.setJobTitle("Controller");
            jobsBean.setMinSalary(8000L);
            jobsBean.setMaxSalary(12000L);
            final JobsBeanExt newJobsBean = beanDML_HR1.insertJobs(jobsBean);
            final List<JobsBeanExt> jobsBeanList = employeeService.findJobById("CT_MGR");
            assertEquals(1, jobsBeanList.size());
            assertEquals(newJobsBean, jobsBeanList.get(0));

            // Modify the just inserted record
            newJobsBean.setJobTitle("Controlling");
            beanDML_HR1.updateJobs(newJobsBean);
            final List<JobsBeanExt> jobsBeanList2 = employeeService.findJobById("CT_MGR");
            assertEquals(1, jobsBeanList2.size());
            assertEquals("Controlling", jobsBeanList2.get(0).getJobTitle());

            // Delete the just inserted record.
            beanDML_HR1.deleteJobs(newJobsBean);
            final List<JobsBeanExt> jobsBeanList3 = employeeService.findJobById("CT_MGR");
            assertEquals(0, jobsBeanList3.size());

            DataSourceProvider.commit();
        } catch (Throwable e) {
            DataSourceProvider.rollback();
            fail(e.getMessage());
        }
    }
}
