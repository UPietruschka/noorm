package org.noorm.test;

import org.junit.Ignore;
import org.junit.Test;
import org.noorm.jdbc.DataSourceProvider;
import org.noorm.test.hr.beans.CountriesBean;
import org.noorm.test.hr.beans.DepartmentsBean;
import org.noorm.test.hr.beans.EmployeesBean;
import org.noorm.test.hr.services.BeanDML;
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

    private BeanDML beanDML = BeanDML.getInstance();

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

    @Ignore
    @Test
    public void testTrimCHARColumn() {

        final String countryId = "X";
        final DeclaredQueries declaredQueries = DeclaredQueries.getInstance();
        final CountriesBean country = new CountriesBean();
        country.setCountryId(countryId);
        try {
            DataSourceProvider.begin();
            beanDML.insertCountries(country);
            final List<CountriesBean> countries = declaredQueries.findCountriesByCountryId(countryId);
            assertEquals(1, countries.size());
            beanDML.deleteCountries(country);
            DataSourceProvider.commit();
        } catch (Exception e) {
            DataSourceProvider.rollback();
            fail(e.getMessage());
        }
    }
}
