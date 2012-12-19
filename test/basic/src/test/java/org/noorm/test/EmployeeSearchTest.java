package org.noorm.test;

import org.junit.Test;
import org.noorm.jdbc.PageableBeanList;
import org.noorm.test.hr.beans.EmployeesBean;
import org.noorm.test.hr.services.EmployeeSearch;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 27.01.12
 *         Time: 13:20
 */
public class EmployeeSearchTest {

	@Test
	public void testSearchEmployees() {

		String jobTitle = null;
		String lastName = "King";
		java.util.Date hireDateFrom = null;
		java.util.Date hireDateTo = null;
		Long salaryFrom = null;
		Long salaryTo = null;
		final EmployeeSearch employeeSearch = EmployeeSearch.getInstance();

		List<EmployeesBean> employeesBeanList = employeeSearch.findEmployeesByFilter
				(jobTitle, lastName, hireDateFrom, hireDateTo, salaryFrom, salaryTo);
		assertEquals(2, employeesBeanList.size());

		jobTitle = "President";
		lastName = "King";
		hireDateFrom = null;
		hireDateTo = null;
		salaryFrom = null;
		salaryTo = null;
		employeesBeanList = employeeSearch.findEmployeesByFilter
				(jobTitle, lastName, hireDateFrom, hireDateTo, salaryFrom, salaryTo);
		assertEquals(1, employeesBeanList.size());

		jobTitle = null;
		lastName = null;
		hireDateFrom = getDate(2004, 01, 01);
		hireDateTo = getDate(2005, 06, 30);
		salaryFrom = null;
		salaryTo = null;
		employeesBeanList = employeeSearch.findEmployeesByFilter
				(jobTitle, lastName, hireDateFrom, hireDateTo, salaryFrom, salaryTo);
		assertEquals(24, employeesBeanList.size());

		jobTitle = null;
		lastName = null;
		hireDateFrom = getDate(2004, 01, 01);
		hireDateTo = getDate(2005, 06, 30);
		salaryFrom = 9000L;
		salaryTo = 11000L;
		employeesBeanList = employeeSearch.findEmployeesByFilter
				(jobTitle, lastName, hireDateFrom, hireDateTo, salaryFrom, salaryTo);
		assertEquals(6, employeesBeanList.size());
	}

    @Test
    public void testPageableSearch() {

        final EmployeeSearch employeeSearch = EmployeeSearch.getInstance();
        final List<Long> idBeanList = employeeSearch.findEmployeeIds(50L);
        Long[] dataIdArray = idBeanList.toArray(new Long[idBeanList.size()]);
        final PageableBeanList<EmployeesBean> empBeanList =
                (PageableBeanList<EmployeesBean>) employeeSearch.findPageableEmpsByIdlist(dataIdArray);
        assertEquals(45, empBeanList.size());
        List<EmployeesBean> subList = empBeanList.subList(10, 20);
        assertEquals(10, subList.size());
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
