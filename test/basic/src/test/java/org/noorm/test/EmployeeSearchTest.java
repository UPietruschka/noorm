package org.noorm.test;

import org.junit.Test;
import org.noorm.test.hr.beans.Employees;
import org.noorm.test.hr.services.EmployeeFinder;

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

	/**
	 * This test runs dynamic SQL queries against the code generated by the query generator
	 */
	@Test
	public void testFindEmployeesDynamic() {

		String jobTitle = null;
		String lastName = "King";
		java.util.Date hireDateFrom = null;
		java.util.Date hireDateTo = null;
		Double salaryFrom = null;
		Double salaryTo = null;
		final EmployeeFinder employeeFinder = EmployeeFinder.getInstance();

		List<Employees> employeesList = employeeFinder.findEmployeesDynamic
				(lastName, hireDateFrom, hireDateTo, salaryFrom, salaryTo, jobTitle);
		assertEquals(2, employeesList.size());

		jobTitle = "President";
		lastName = "King";
		hireDateFrom = null;
		hireDateTo = null;
		salaryFrom = null;
		salaryTo = null;
		employeesList = employeeFinder.findEmployeesDynamic
				(lastName, hireDateFrom, hireDateTo, salaryFrom, salaryTo, jobTitle);
		assertEquals(1, employeesList.size());

		jobTitle = null;
		lastName = null;
		hireDateFrom = getDate(2004, 01, 01);
		hireDateTo = getDate(2005, 06, 30);
		salaryFrom = null;
		salaryTo = null;
		employeesList = employeeFinder.findEmployeesDynamic
				(lastName, hireDateFrom, hireDateTo, salaryFrom, salaryTo, jobTitle);
		assertEquals(24, employeesList.size());

		jobTitle = null;
		lastName = null;
		hireDateFrom = getDate(2004, 01, 01);
		hireDateTo = getDate(2005, 06, 30);
		salaryFrom = 9000.0;
		salaryTo = 11000.0;
		employeesList = employeeFinder.findEmployeesDynamic
				(lastName, hireDateFrom, hireDateTo, salaryFrom, salaryTo, jobTitle);
		assertEquals(6, employeesList.size());
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
