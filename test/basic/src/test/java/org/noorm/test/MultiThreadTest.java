package org.noorm.test;

import org.junit.Test;
import org.noorm.jdbc.DataSourceProvider;
import org.noorm.test.hr.beans.EmployeesBean;
import org.noorm.test.hr.beans.JobHistoryBean;
import org.noorm.test.hr.services.BeanDML;
import org.noorm.test.hr.services.EmployeeService;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
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
    public void testCreateDeleteJobHistory(final int pAddMonth) {

        final Calendar startC = Calendar.getInstance();
        final Calendar searchC = Calendar.getInstance();
        final Calendar endC = Calendar.getInstance();
        startC.add(Calendar.MONTH, 1 + pAddMonth);
        searchC.add(Calendar.MONTH, 2 + pAddMonth);
        endC.add(Calendar.MONTH, 3 + pAddMonth);
        final Date start = startC.getTime();
        final Date search = searchC.getTime();
        final Date end = endC.getTime();
        final List<JobHistoryBean> jobHistoryList = new ArrayList<JobHistoryBean>();
        final List<EmployeesBean> employeeList = employeeService.findAllEmployees();
        int i = 0;
        for (final EmployeesBean employee : employeeList) {
            final JobHistoryBean jobHistory = new JobHistoryBean();
            jobHistory.setEmployeeId(employee.getEmployeeId());
            jobHistory.setJobId("IT_PROG");
            jobHistory.setStartDate(start);
            jobHistory.setEndDate(end);
            jobHistoryList.add(jobHistory);
            if (++i >= TEST_LOOP_COUNT) {
                break;
            }
        }
        beanDML.insertJobHistoryList(jobHistoryList);

        // Wait a short while to guarantee that for the multi-thread test different connections are used
        try {
            Thread.sleep(100L);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }

        final List<JobHistoryBean> jobHistoryList1 = employeeService.findJobHistoryByDate(search);
        assertEquals(jobHistoryList1.size(), TEST_LOOP_COUNT);
        for (final JobHistoryBean jobHistory : jobHistoryList1) {
            beanDML.deleteJobHistory(jobHistory);
        }
    }

    @Test
    public void testMultiThreadTCConnection() {

        final Thread thread1 = new TCCOnnectionThread(0);
        final Thread thread2 = new TCCOnnectionThread(3);
        final Thread thread3 = new TCCOnnectionThread(6);
        final Thread thread4 = new TCCOnnectionThread(9);
        final UncaughtThreadExceptionHandler t1Handler = new UncaughtThreadExceptionHandler();
        final UncaughtThreadExceptionHandler t2Handler = new UncaughtThreadExceptionHandler();
        final UncaughtThreadExceptionHandler t3Handler = new UncaughtThreadExceptionHandler();
        final UncaughtThreadExceptionHandler t4Handler = new UncaughtThreadExceptionHandler();
        thread1.setUncaughtExceptionHandler(t1Handler);
        thread2.setUncaughtExceptionHandler(t2Handler);
        thread3.setUncaughtExceptionHandler(t3Handler);
        thread4.setUncaughtExceptionHandler(t4Handler);
        thread1.start();
        thread2.start();
        thread3.start();
        thread4.start();
        try {
            thread1.join();
            thread2.join();
            thread3.join();
            thread4.join();
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        if (t1Handler.getThrowable() != null) {
            fail(t1Handler.getThrowable().getMessage());
        }
        if (t2Handler.getThrowable() != null) {
            fail(t2Handler.getThrowable().getMessage());
        }
        if (t3Handler.getThrowable() != null) {
            fail(t3Handler.getThrowable().getMessage());
        }
        if (t4Handler.getThrowable() != null) {
            fail(t4Handler.getThrowable().getMessage());
        }
    }

    class TCCOnnectionThread extends Thread {

        private int addMonth;

        public TCCOnnectionThread(final int pAddMonth) {
            addMonth = pAddMonth;
        }

        public void run() {
            DataSourceProvider.begin();
            try {
                testCreateDeleteJobHistory(addMonth);
                DataSourceProvider.commit();
            } catch (Throwable e) {
                DataSourceProvider.rollback();
                fail(e.getMessage());
            }
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
