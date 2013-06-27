package org.noorm.test;

import org.junit.Test;
import org.noorm.jdbc.DataSourceProvider;
import org.noorm.test.hr.beans.VDepartmentsBean;
import org.noorm.test.hr.services.BeanDML;
import org.noorm.test.hr.services.DepartmentService;

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

	private BeanDML beanDML = BeanDML.getInstance();

    @Test
    public void testFindVDepartmentById() {

        final DepartmentService departmentService = DepartmentService.getInstance();
        final List<VDepartmentsBean> vDepartmentsBeanList =
                departmentService.findDepartmentsById(SHIPPING_DEPARTMENT_ID);
        assertEquals(1L, vDepartmentsBeanList.size());
        final VDepartmentsBean departmentsBean = vDepartmentsBeanList.get(0);
        assertEquals(SHIPPING_DEPARTMENT_NAME, departmentsBean.getDepartmentName());
        assertEquals(SHIPPING_DEPARTMENT_CITY, departmentsBean.getCity());
        assertEquals(SHIPPING_DEPARTMENT_MANAGER_LAST_NAME, departmentsBean.getLastName());
        assertEquals(SHIPPING_DEPARTMENT_EMPLOYEE_COUNT, departmentsBean.getEmployeeCount());
    }

    @Test
    public void testVDepartmentCRUD() {

        DataSourceProvider.begin();
        try {
            final DepartmentService departmentService = DepartmentService.getInstance();
            final VDepartmentsBean newDepartmentsBean = new VDepartmentsBean();
            newDepartmentsBean.setDepartmentName(NEW_DEPARTMENT_NAME);
            newDepartmentsBean.setLocationId(NEW_DEPARTMENT_LOCATION_ID);
            newDepartmentsBean.setManagerId(NEW_DEPARTMENT_MANAGER_ID);
            final VDepartmentsBean newDepartmentsBean1 = beanDML.insertVDepartments(newDepartmentsBean);
            assertNotNull(newDepartmentsBean1);
            final List<VDepartmentsBean> vDepartmentsBeanList =
                    departmentService.findDepartmentsById(newDepartmentsBean1.getDepartmentId());
            assertEquals(1L, vDepartmentsBeanList.size());
            newDepartmentsBean1.setLocationId(NEW_DEPARTMENT_LOCATION_ID1);
            beanDML.updateVDepartments(newDepartmentsBean1);
            beanDML.deleteVDepartments(newDepartmentsBean1);
            final List<VDepartmentsBean> vDepartmentsBeanList1 =
                    departmentService.findDepartmentsById(newDepartmentsBean1.getDepartmentId());
            assertEquals(0L, vDepartmentsBeanList1.size());
            DataSourceProvider.commit();
        } catch (Throwable e) {
            DataSourceProvider.rollback();
            fail(e.getMessage());
        }
    }
}
