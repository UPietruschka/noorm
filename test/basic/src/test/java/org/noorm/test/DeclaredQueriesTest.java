package org.noorm.test;

import org.junit.Test;
import org.noorm.test.hr.beans.DepartmentsBean;
import org.noorm.test.hr.beans.EmployeesBean;
import org.noorm.test.hr.services.DeclaredQueries;

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
        DepartmentsBean department = declaredQueries.findDepartmentsByDepartmentId(departmentId);
        assertNotNull(department);
        assertEquals(department.getDepartmentName(), departmentName);
    }

    @Test
    public void testFindEmployees1() {

        final String departmentName = "Sales";
        final String city = "Oxford";
        final DeclaredQueries declaredQueries = DeclaredQueries.getInstance();
        List<EmployeesBean> employees = declaredQueries.findEmployeesByDepartmentCity(departmentName, city);
        assertEquals(34, employees.size());
    }

    @Test
    public void testFindEmployees2() {

        final String departmentName = "S%"; // Shipping, Sales
        final DeclaredQueries declaredQueries = DeclaredQueries.getInstance();
        List<EmployeesBean> employees = declaredQueries.findEmployeesByDepartmentName(departmentName);
        assertEquals(79, employees.size());
    }
}
