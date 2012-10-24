package org.noorm.test;

import org.junit.Test;
import org.noorm.jdbc.DataSourceProvider;
import org.noorm.test.hr.beans.EmployeesBean;
import org.noorm.test.hr.services.BeanDML;
import org.noorm.test.hr.services.EmployeeService;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 24.10.12
 *         Time: 09:22
 */
public class MultiThreadTest {

    private static final int TEST_LOOP_COUNT = 30;

    private BeanDML beanDML = BeanDML.getInstance();
    private EmployeeService employeeService = EmployeeService.getInstance();

    // Test executed with testMultiThreadTCConnection
    public void testMergeEmployees(final String pLastName) {

        final List<EmployeesBean> employeeList = new ArrayList<EmployeesBean>();
        for (int i = 0; i < TEST_LOOP_COUNT; i++) {
            final EmployeesBean employee = new EmployeesBean();
            employee.setFirstName("John");
            employee.setLastName(pLastName);
            employee.setEmail("JOHN." + pLastName + "." + i + "@DOE.COM");
            employee.setPhoneNumber("123.456.7890");
            employee.setHireDate(new java.util.Date(1200000000000L));  // January 10, 2008, 22:20
            employee.setJobId("IT_PROG");
            employee.setSalary(5000.00D);
            employee.setCommissionPct(0.28D);
            employee.setManagerId(108L);
            employee.setDepartmentId(60L);
            employeeList.add(employee);
        }
        beanDML.insertEmployeesList(employeeList);

        // Wait a short while to guarantee that for the multi-thread test different connections are used
        try {
            Thread.sleep(100L);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }

        final List<EmployeesBean> employeesList1 = employeeService.findEmployeesByLastname(pLastName);
        assertEquals(employeesList1.size(), TEST_LOOP_COUNT);
        for (final EmployeesBean employee : employeesList1) {
            beanDML.deleteEmployees(employee);
        }
    }

    @Test
    public void testMultiThreadTCConnection() {

        final Thread thread1 = new TCCOnnectionThread1();
        final Thread thread2 = new TCCOnnectionThread2();
        final UncaughtThreadExceptionHandler t1Handler = new UncaughtThreadExceptionHandler();
        final UncaughtThreadExceptionHandler t2Handler = new UncaughtThreadExceptionHandler();
        thread1.setUncaughtExceptionHandler(t1Handler);
        thread2.setUncaughtExceptionHandler(t2Handler);
        thread1.start();
        thread2.start();
        try {
            thread1.join();
            thread2.join();
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        if (t1Handler.getThrowable() != null) {
            fail(t1Handler.getThrowable().getMessage());
        }
        if (t2Handler.getThrowable() != null) {
            fail(t2Handler.getThrowable().getMessage());
        }
    }

    class TCCOnnectionThread1 extends Thread {
        public void run() {
//            DataSourceProvider.begin();
            testMergeEmployees("Lastname2");
//            DataSourceProvider.commit();
        }
    }

    class TCCOnnectionThread2 extends Thread {
        public void run() {
            DataSourceProvider.begin();
            testMergeEmployees("Lastname1");
            DataSourceProvider.commit();
        }
    }

    class UncaughtThreadExceptionHandler implements Thread.UncaughtExceptionHandler {

        private Throwable t0 = null;

        public void uncaughtException(final Thread pThread, final Throwable pThrowable) {
            t0 = pThrowable;
        }

        public Throwable getThrowable() {
            return t0;
        }
    }
}
