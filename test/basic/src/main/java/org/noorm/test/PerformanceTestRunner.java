package org.noorm.test;

import org.noorm.jdbc.DataSourceProvider;
import org.noorm.test.hr.beans.EmployeesBean;
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
        testEmployeeInsert(true);
        testEmployeeSearch();
        final long endTime = System.currentTimeMillis();
        final double totalSeconds = (endTime - startTime) / 1000d;
        System.out.println("Elapsed time (seconds total) : " + totalSeconds);
    }

    private static void testEmployeeInsert(final boolean pUseBatchMode) {

        DataSourceProvider.begin();
        final List<EmployeesBean> employeeList = new ArrayList<EmployeesBean>();
        try {
            for (int i = 0; i < TEST_LOOP; i++) {
                final String lastName = "Doe" + i;
                final String email = "JDOE" + i;
                final String jobId = JOB_IDS[i % JOB_IDS.length];
                final Double salary = 1000D + i / 4;
                final Long departmentId = ((i % 27) + 1) * 10L;
                final EmployeesBean employeesBean = assembleEmployee(lastName, email, jobId, salary, departmentId);
                employeeList.add(employeesBean);
            }
            if (pUseBatchMode) {
                beanDML.insertEmployeesList(employeeList);
            } else {
                for (int i = 0; i < TEST_LOOP; i++) {
                    final EmployeesBean newEmployeesBean = beanDML.insertEmployees(employeeList.get(i));
                }
            }
            DataSourceProvider.commit();
        } catch (Exception e) {
            e.printStackTrace();
            DataSourceProvider.rollback();
            System.exit(1);
        }
    }

    private static void testEmployeeSearch() {

        final EmployeeService employeeService = EmployeeService.getInstance();
        final List<EmployeesBean> employeesBeanList = employeeService.findAllEmployees();
    }

    private static EmployeesBean assembleEmployee(final String pLastName,
                                                  final String pEmail,
                                                  final String pJobId,
                                                  final Double pSalary,
                                                  final Long pDepartmentId) {

        final EmployeesBean employeesBean = new EmployeesBean();
        employeesBean.setFirstName("John");
        employeesBean.setLastName(pLastName);
        employeesBean.setEmail(pEmail);
        employeesBean.setPhoneNumber("123.456.7890");
        employeesBean.setHireDate(new java.util.Date(1200000000000L));  // January 10, 2008, 22:20
        employeesBean.setJobId(pJobId);
        employeesBean.setSalary(pSalary);
        employeesBean.setCommissionPct(0.21D);
        employeesBean.setManagerId(108L);
        employeesBean.setDepartmentId(pDepartmentId);
        return employeesBean;
    }
}
