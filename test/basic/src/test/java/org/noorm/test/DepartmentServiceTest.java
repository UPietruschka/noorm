package org.noorm.test;

import org.junit.Test;
import org.noorm.jdbc.DataSourceProvider;
import org.noorm.test.hr.beans.Departments;
import org.noorm.test.hr.beans.VDepartments;
import org.noorm.test.hr.services.DepartmentService;
import org.noorm.test.hr.services.DepartmentsDML;

import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 19.02.12
 *         Time: 12:25
 */
public class DepartmentServiceTest {

    private static final Integer SHIPPING_DEPARTMENT_ID = 50;
    private static final String SHIPPING_DEPARTMENT_NAME = "Shipping";
    private static final String SHIPPING_DEPARTMENT_CITY = "South San Francisco";
    private static final String SHIPPING_DEPARTMENT_MANAGER_LAST_NAME = "Fripp";
    private static final Long SHIPPING_DEPARTMENT_EMPLOYEE_COUNT = 45L;
    private static final String NEW_DEPARTMENT_NAME = "Training";
    private static final Integer NEW_DEPARTMENT_LOCATION_ID = 3200;
    private static final Integer NEW_DEPARTMENT_LOCATION_ID1 = 3100;
    private static final Long NEW_DEPARTMENT_MANAGER_ID = 205L;

	private DepartmentsDML departmentsDML = DepartmentsDML.getInstance();

    @Test
    public void testFindVDepartmentById() {

        final DepartmentService departmentService = DepartmentService.getInstance();
        final List<VDepartments> vDepartmentsList =
                departmentService.findDepartmentsById(SHIPPING_DEPARTMENT_ID);
        assertEquals(1L, vDepartmentsList.size());
        final VDepartments departments = vDepartmentsList.get(0);
        assertEquals(SHIPPING_DEPARTMENT_NAME, departments.getDepartmentName());
        assertEquals(SHIPPING_DEPARTMENT_CITY, departments.getCity());
        assertEquals(SHIPPING_DEPARTMENT_MANAGER_LAST_NAME, departments.getLastName());
        assertEquals(SHIPPING_DEPARTMENT_EMPLOYEE_COUNT, departments.getEmployeeCount());
    }

    @Test
    public void testVDepartmentCRUD() {

        DataSourceProvider.begin();
        try {
            final DepartmentService departmentService = DepartmentService.getInstance();
            final Departments newDepartments = new Departments();
            newDepartments.setDepartmentName(NEW_DEPARTMENT_NAME);
            newDepartments.setLocationId(NEW_DEPARTMENT_LOCATION_ID);
            newDepartments.setManagerId(NEW_DEPARTMENT_MANAGER_ID);
            final Departments newDepartments1 = departmentsDML.insertDepartments(newDepartments);
            assertNotNull(newDepartments1);
            final List<VDepartments> departmentsList =
                    departmentService.findDepartmentsById(newDepartments1.getDepartmentId());
            assertEquals(1L, departmentsList.size());
            final VDepartments newDepartment2 = departmentsList.get(0);
            newDepartment2.setLocationId(NEW_DEPARTMENT_LOCATION_ID1);
            departmentsDML.updateDepartments(newDepartment2);
            departmentsDML.deleteDepartments(newDepartment2);
            final List<VDepartments> departmentsList1 =
                    departmentService.findDepartmentsById(newDepartments1.getDepartmentId());
            assertEquals(0L, departmentsList1.size());
            DataSourceProvider.commit();
        } catch (Throwable e) {
            DataSourceProvider.rollback();
            fail(e.getMessage());
        }
    }
}
