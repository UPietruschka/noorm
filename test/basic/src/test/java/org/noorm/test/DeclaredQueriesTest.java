package org.noorm.test;

import org.junit.Test;
import org.noorm.test.hr.beans.DepartmentsBean;
import org.noorm.test.hr.beans.EmployeesBean;
import org.noorm.test.hr.services.DeclaredQueries;
import org.noorm.test.hr.services.EmployeeFinder;

import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 23.01.13
 *         Time: 13:20
 */
public class DeclaredQueriesTest {

    @Test
    public void testFindDepartment() {

        final Long departmentId = 10L;
        final String departmentName = "Administration";
        final DeclaredQueries declaredQueries = DeclaredQueries.getInstance();
        final DepartmentsBean department = declaredQueries.findDepartmentsByDepartmentId(departmentId);
        assertNotNull(department);
        assertEquals(department.getDepartmentName(), departmentName);
    }

    @Test
    public void testFindDoubleEqual() {

        final DeclaredQueries declaredQueries = DeclaredQueries.getInstance();
        final List<EmployeesBean> employees = declaredQueries.findEmployeesByCommissionPct(0.25d);
        assertEquals(6, employees.size());
    }

    @Test
    public void testFindIsNull() {

        final DeclaredQueries declaredQueries = DeclaredQueries.getInstance();
        final List<EmployeesBean> employees = declaredQueries.findEmployeesWithoutCommission();
        assertEquals(72, employees.size());
    }

    @Test
    public void testFindIsNotNull() {

        final DeclaredQueries declaredQueries = DeclaredQueries.getInstance();
        final List<EmployeesBean> employees = declaredQueries.findEmployeesWithCommission();
        assertEquals(35, employees.size());
    }

    @Test
    public void testFindEmployees1() {

        final String departmentName = "Sales";
        final String city = "Oxford";
        final EmployeeFinder employeeFinder = EmployeeFinder.getInstance();
        final List<EmployeesBean> employees = employeeFinder.findEmployeesByDepartmentCity(departmentName, city);
        assertEquals(34, employees.size());
    }

    @Test
    public void testFindEmployees2() {

        final String departmentName = "S%"; // Shipping, Sales
        final EmployeeFinder employeeFinder = EmployeeFinder.getInstance();
        final List<EmployeesBean> employees = employeeFinder.findEmployeesByDepartmentName(departmentName);
        assertEquals(79, employees.size());
    }
}
