package org.noorm.test;

import org.junit.Test;
import org.noorm.jdbc.DataSourceProvider;
import org.noorm.jdbc.FilterExtension;
import org.noorm.jdbc.TableLock;
import org.noorm.test.hr.beans.Countries;
import org.noorm.test.hr.beans.Departments;
import org.noorm.test.hr.beans.Employees;
import org.noorm.test.hr.services.BeanDML;
import org.noorm.test.hr.services.DeclaredQueries;
import org.noorm.test.hr.services.EmployeeFinder;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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

        final Integer departmentId = 10;
        final String departmentName = "Administration";
        final DeclaredQueries declaredQueries = DeclaredQueries.getInstance();
        final Departments department = declaredQueries.findDepartmentsByDepartmentId(departmentId);
        assertNotNull(department);
        assertEquals(department.getDepartmentName(), departmentName);
    }

    @Test
    public void testFindDoubleEqual() {

        final DeclaredQueries declaredQueries = DeclaredQueries.getInstance();
        final List<Employees> employees =
                declaredQueries.findEmployeesByCommissionPct(new java.math.BigDecimal(0.25D));
        assertEquals(6, employees.size());
    }

    @Test
    public void testFindIsNull() {

        final DeclaredQueries declaredQueries = DeclaredQueries.getInstance();
        final List<Employees> employees = declaredQueries.findEmployeesWithoutCommission();
        assertEquals(72, employees.size());
    }

    @Test
    public void testFindIsNotNull() {

        final DeclaredQueries declaredQueries = DeclaredQueries.getInstance();
        final List<Employees> employees = declaredQueries.findEmployeesWithCommission();
        assertEquals(35, employees.size());
    }

    @Test
    public void testFindEmployees1() {

        final String departmentName = "Sales";
        final String city = "Oxford";
        final EmployeeFinder employeeFinder = EmployeeFinder.getInstance();
        final List<Employees> employees = employeeFinder.findEmployeesByDepartmentCity(departmentName, city);
        assertEquals(34, employees.size());
    }

    @Test
    public void testFindEmployees2() {

        final String departmentName = "S%"; // Shipping, Sales
        final EmployeeFinder employeeFinder = EmployeeFinder.getInstance();
        final List<Employees> employees = employeeFinder.findEmployeesByDepartmentName(departmentName);
        assertEquals(79, employees.size());
    }

    @Test
    public void testFindEmployees3() {

        final FilterExtension filterExtension = new FilterExtension();
        filterExtension.setIndex(10);
        filterExtension.setCount(30);
        filterExtension.addSortCriteria("salary", FilterExtension.Direction.DESC);
        filterExtension.addSortCriteria("hireDate");
        final Date hireDateFrom = getDate(2005, 01, 01);
        final DeclaredQueries declaredQueries = DeclaredQueries.getInstance();
        final List<Employees> employees = declaredQueries.findEmployeesByHireDate(hireDateFrom, filterExtension);
        assertEquals(30, employees.size());
    }

    @Test
    public void testTrimCHARColumn() {

        final String countryId = "X";
        final DeclaredQueries declaredQueries = DeclaredQueries.getInstance();
        final Countries country = new Countries();
        country.setCountryId(countryId);
        try {
            DataSourceProvider.begin();
            TableLock.acquire(country, TableLock.LockMode.ROW_EXCLUSIVE);
            beanDML.insertCountries(country);
            final List<String> countryIds = new ArrayList<>();
            countryIds.add(countryId);
            countryIds.add("QQ"); // Non existent value
            countryIds.add("XX"); // Non existent value
            final List<Countries>countries = declaredQueries.findCountriesByCountryId(countryIds);
            assertEquals(1, countries.size());
            beanDML.deleteCountries(country);
            DataSourceProvider.commit();
        } catch (Throwable e) {
            DataSourceProvider.rollback();
            fail(e.getMessage());
        }
    }

    private Date getDate(final int pYear, final int pMonth, final int pDay) {

        final String dateString = pYear + "/" + pMonth + "/" + pDay;
        java.util.Date date = null;

        try {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd");
            date = formatter.parse(dateString);
        } catch (ParseException e) {
            fail();
        }
        return date;
    }
}
