package org.noorm.test;

import org.noorm.jdbc.DataSourceProvider;
import org.noorm.test.hr.beans.Employees;
import org.noorm.test.hr.services.BeanDML;
import org.noorm.test.hr.services.EmployeeService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 05.04.13
 *         Time: 15:09
 *         <p/>
 * Performance tests.
 * Stand-alone test to simplify profiling load- and performance metrics.
 * Note that this test does not work without modifications to the original sample schema HR (The
 * triggers should be disabled and, if the test is running multiple times, PK column EMPLOYEE_ID, which is
 * a NUMBER(6) field, may cause problems, when the sequence reaches larger numbers).
 */
public class PerformanceTestRunner {

    private static BeanDML beanDML = BeanDML.getInstance();
    private static int TEST_LOOP = 50000;
    private static String[] JOB_IDS = new String[] {"AC_ACCOUNT", "AC_MGR", "AD_ASST", "AD_PRES", "AD_VP",
            "FI_ACCOUNT", "FI_MGR", "HR_REP", "IT_PROG", "MK_MAN", "MK_REP", "PR_REP", "PU_CLERK", "PU_MAN",
            "SA_MAN", "SA_REP", "SH_CLERK", "ST_CLERK", "ST_MAN" };

    public static void main (final String args[]) {

        System.out.println("Press any key to start the load- and performance tests.");
        try {
            System.in.read();
        } catch (IOException e) {
            // Do nothing
        }
        final long startTime = System.currentTimeMillis();
        DataSourceProvider.begin();
        try {
            testEmployeeInsert(true);
            final List<Employees> employeesBeanList = testEmployeeSearch();
            testEmployeeUpdate(employeesBeanList);
            testEmployeeDelete(employeesBeanList);
            DataSourceProvider.commit();
            final long endTime = System.currentTimeMillis();
            final double totalSeconds = (endTime - startTime) / 1000d;
            System.out.println("Elapsed time (seconds total) : " + totalSeconds);
        } catch (Exception e) {
            e.printStackTrace();
            DataSourceProvider.rollback();
            System.exit(1);
        }
    }

    private static void testEmployeeInsert(final boolean pUseBatchMode) {

        final List<Employees> employeeList = new ArrayList<Employees>();
        for (int i = 0; i < TEST_LOOP; i++) {
            final String lastName = "Doe";
            final String email = "JDOE" + i;
            final String jobId = JOB_IDS[i % JOB_IDS.length];
            final Double salary = 1000D + i / 4;
            final Integer departmentId = ((i % 27) + 1) * 10;
            final Employees employees = assembleEmployee(lastName, email, jobId, salary, departmentId);
            employeeList.add(employees);
        }
        if (pUseBatchMode) {
            beanDML.insertEmployeesList(employeeList);
        } else {
            for (int i = 0; i < TEST_LOOP; i++) {
                beanDML.insertEmployees(employeeList.get(i));
            }
        }
    }

    private static List<Employees> testEmployeeSearch() {

        final EmployeeService employeeService = EmployeeService.getInstance();
        final List<Employees> employeesBeanList = employeeService.findEmployeesByLastname("Doe");
        return employeesBeanList;
    }

    private static void testEmployeeUpdate(List<Employees> pEmployeesBeanList) {

        for (final Employees employee : pEmployeesBeanList) {
            employee.setCommissionPct(new java.math.BigDecimal(0.22D));
        }
        beanDML.updateEmployeesList(pEmployeesBeanList);
    }

    private static void testEmployeeDelete(List<Employees> pEmployeesBeanList) {

        beanDML.deleteEmployeesList(pEmployeesBeanList);
    }

    private static Employees assembleEmployee(final String pLastName,
                                              final String pEmail,
                                              final String pJobId,
                                              final Double pSalary,
                                              final Integer pDepartmentId) {

        final Employees employeesBean = new Employees();
        employeesBean.setFirstName("John");
        employeesBean.setLastName(pLastName);
        employeesBean.setEmail(pEmail);
        employeesBean.setPhoneNumber("123.456.7890");
        employeesBean.setHireDate(new java.util.Date(1200000000000L));  // January 10, 2008, 22:20
        employeesBean.setJobId(pJobId);
        employeesBean.setSalary(pSalary);
        employeesBean.setCommissionPct(new java.math.BigDecimal(0.21D));
        employeesBean.setManagerId(108L);
        employeesBean.setDepartmentId(pDepartmentId);
        return employeesBean;
    }
}
