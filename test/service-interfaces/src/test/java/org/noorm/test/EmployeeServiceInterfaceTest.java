package org.noorm.test;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.noorm.jdbc.DataSourceProvider;
import org.noorm.test.hr1.beans.EmpDetailsView;
import org.noorm.test.hr1.beans.Employees;
import org.noorm.test.hr1.beans.History;
import org.noorm.test.hr1.services.IEmployeeService;
import org.noorm.test.hr1.services.IEmployeesDML;
import org.noorm.test.hr1.services.IHistoryDML;
import org.noorm.test.hr1.services.IJobsDML;
import org.noorm.test.hr2.beans.Departments;
import org.noorm.test.hr2.services.IDepartmentsDML;
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
    private IEmployeesDML employeesDML;
    @Resource
    private IDepartmentsDML departmentsDML;
    @Resource
    private IJobsDML jobsDML;
    @Resource
    private IHistoryDML historyDML;
    @Resource
    private IEmployeeService employeeService;

    @Test
    public void testFindEmployees() {

        // Using explicit transaction handling is actually not required for this test case, but since we
        // do not use a connection pool here, the loop over all employees could easily exhaust the maximum
        // number of available connections of the test database.
        try {
            DataSourceProvider.setActiveDataSource(HR1_DATA_SOURCE);
            DataSourceProvider.begin();
            final List<Employees> employeesList = employeeService.findAllEmployees();
            for (final Employees employees : employeesList) {

                final Long employeeId = employees.getEmployeeId();
                final Employees employees1 = employeeService.findUniqueEmployeeById(employeeId);
                assertEquals(employees, employees1);

                final String employeeLastName = employees.getLastName();
                final List<Employees> employeesList2 = employeeService.findEmployeesByLastname(employeeLastName);
                // Multiple employees with the same last name may exist. Just check, if the desired employee is contained
                boolean employeeFound = false;
                for (final Employees employees2 : employeesList2) {
                    if (employees.equals(employees2)) {
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
            final Employees employees = new Employees();
            employees.setFirstName("John");
            employees.setLastName("Doe");
            employees.setEmail("JDOE");
            employees.setPhoneNumber("123.456.7890");
            employees.setHireDate(new java.util.Date(1200000000000L));  // January 10, 2008, 22:20
            employees.setJobId("IT_PROG");
            employees.setSalary(5000.00D);
            employees.setCommissionPct(0.28D);
            employees.setManagerId(108L);
            employees.setDepartmentId(60L);
            final Employees newEmployees = employeesDML.insertEmployees(employees);
            final List<Employees> employeesList1 = employeeService.findEmployeesByLastname("Doe");
            assertEquals(1, employeesList1.size());
            assertEquals(newEmployees, employeesList1.get(0));

            // Modify the just inserted record
            newEmployees.setLastName("Public");
            employeesDML.updateEmployees(newEmployees);
            final List<Employees> employeesList2 = employeeService.findEmployeesByLastname("Doe");
            assertEquals(0, employeesList2.size());
            final List<Employees> employeesList3 = employeeService.findEmployeesByLastname("Public");
            assertEquals(1, employeesList3.size());
            assertEquals(newEmployees, employeesList3.get(0));

            DataSourceProvider.setActiveDataSource(HR1_DATA_SOURCE);
            // Delete the just inserted record. We have to delete the job history record inserted by the trigger, too
            final List<History> jobHistoryList =
                    employeeService.findJobHistoryByEmpId(newEmployees.getEmployeeId());
            historyDML.deleteHistoryList(jobHistoryList);
            employeesDML.deleteEmployees(newEmployees);
            final List<Employees> employeesList4 = employeeService.findEmployeesByLastname("Public");
            assertEquals(0, employeesList4.size());

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
            final Departments department = new Departments();
            department.setDepartmentName("New Department");
            final Departments newDepartment = departmentsDML.insertDepartments(department);

            // Modify the just inserted record
            newDepartment.setDepartmentName("Updated Department");
            departmentsDML.updateDepartments(newDepartment);
            departmentsDML.deleteDepartments(newDepartment);
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
            // number of available connections of the test database.
            DataSourceProvider.setActiveDataSource(HR1_DATA_SOURCE);
            DataSourceProvider.begin();
            final List<EmpDetailsView> empDetailsList = employeeService.findAllEmployeeDetails();
            for (final EmpDetailsView empDetails : empDetailsList) {

                final Long employeeId = empDetails.getEmployeeId();
                final Employees employees1 = employeeService.findUniqueEmployeeById(employeeId);
                assertNotNull(employees1);
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
            final JobsExt jobs = new JobsExt();
            jobs.setJobId("CT_MGR");
            jobs.setJobTitle("Controller");
            jobs.setMinSalary(8000L);
            jobs.setMaxSalary(12000L);
            final JobsExt newJobs = jobsDML.insertJobs(jobs);
            final List<JobsExt> jobsList = employeeService.findJobById("CT_MGR");
            assertEquals(1, jobsList.size());
            assertEquals(newJobs, jobsList.get(0));

            // Modify the just inserted record
            newJobs.setJobTitle("Controlling");
            jobsDML.updateJobs(newJobs);
            final List<JobsExt> jobsList2 = employeeService.findJobById("CT_MGR");
            assertEquals(1, jobsList2.size());
            assertEquals("Controlling", jobsList2.get(0).getJobTitle());

            // Delete the just inserted record.
            jobsDML.deleteJobs(newJobs);
            final List<JobsExt> jobsList3 = employeeService.findJobById("CT_MGR");
            assertEquals(0, jobsList3.size());

            DataSourceProvider.commit();
        } catch (Throwable e) {
            DataSourceProvider.rollback();
            fail(e.getMessage());
        }
    }
}
