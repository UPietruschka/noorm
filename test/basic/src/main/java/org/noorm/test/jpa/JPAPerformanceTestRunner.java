package org.noorm.test.jpa;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 09.04.13
 *         Time: 14:43
 *         <p/>
 * Runner for the NoORM/JPA performance comparison.
 * The utilization of the sequence associated with entity EmployeesEntity assumes a sequence increment of 100.
 * The sample schema HR does not have this setting and must be adjusted accordingly to make this test running
 * successfully.
 */
public class JPAPerformanceTestRunner {

    private static EntityManager em;
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
        final EntityManagerFactory emf = Persistence.createEntityManagerFactory("hr");
        em = emf.createEntityManager();
        em.getTransaction().begin();
        try {
            testEmployeeInsert();
            final List<EmployeesEntity> employeesBeanList = testEmployeeSearch();
            testEmployeeUpdate(employeesBeanList);
            testEmployeeDelete(employeesBeanList);
            em.getTransaction().commit();
            final long endTime = System.currentTimeMillis();
            final double totalSeconds = (endTime - startTime) / 1000d;
            System.out.println("Elapsed time (seconds total) : " + totalSeconds);
        } catch (Exception e) {
            e.printStackTrace();
            em.getTransaction().rollback();
            System.exit(1);
        }
    }

    private static void testEmployeeInsert() {

        final List<EmployeesEntity> employeeList = new ArrayList<EmployeesEntity>();
        for (int i = 0; i < TEST_LOOP; i++) {
            final String lastName = "Doe";
            final String email = "JDOE" + i;
            final String jobId = JOB_IDS[i % JOB_IDS.length];
            final Double salary = 1000D + i / 4;
            final Long departmentId = ((i % 27) + 1) * 10L;
            final EmployeesEntity employeesBean = assembleEmployee(lastName, email, jobId, salary, departmentId);
            employeeList.add(employeesBean);
        }
        for (int i = 0; i < TEST_LOOP; i++) {
            em.persist(employeeList.get(i));
        }
    }

    private static List<EmployeesEntity> testEmployeeSearch() {

        final Query query = em.createNativeQuery("SELECT * FROM employees WHERE last_name = ?", EmployeesEntity.class);
        query.setParameter(1, "Doe");
        final List<EmployeesEntity> employeesBeanList = query.getResultList();
        return employeesBeanList;
    }

    private static void testEmployeeUpdate(List<EmployeesEntity> pEmployeesBeanList) {

        for (final EmployeesEntity employee : pEmployeesBeanList) {
            employee.setCommissionPct(0.22D);
        }
        for (final EmployeesEntity employee : pEmployeesBeanList) {
            em.merge(employee);
        }
    }

    private static void testEmployeeDelete(List<EmployeesEntity> pEmployeesBeanList) {

        for (final EmployeesEntity employee : pEmployeesBeanList) {
            em.remove(employee);
        }
    }

    private static EmployeesEntity assembleEmployee(final String pLastName,
                                                  final String pEmail,
                                                  final String pJobId,
                                                  final Double pSalary,
                                                  final Long pDepartmentId) {

        final EmployeesEntity employeesBean = new EmployeesEntity();
        employeesBean.setFirstName("John");
        employeesBean.setLastName(pLastName);
        employeesBean.setEmail(pEmail);
        employeesBean.setPhoneNumber("123.456.7890");
        employeesBean.setHireDate(new Timestamp(new java.util.Date(1200000000000L).getTime()));  // January 10, 2008, 22:20
        employeesBean.setJobId(pJobId);
        employeesBean.setSalary(pSalary);
        employeesBean.setCommissionPct(0.21D);
        employeesBean.setManagerId(108L);
        employeesBean.setDepartmentId(pDepartmentId);
        return employeesBean;
    }
}
