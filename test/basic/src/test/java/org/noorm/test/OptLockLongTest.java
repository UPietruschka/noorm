package org.noorm.test;

import org.junit.Assert;
import org.junit.Test;
import org.noorm.jdbc.DataAccessException;
import org.noorm.jdbc.DataSourceProvider;
import org.noorm.test.hr.beans.OptLockLongBean;
import org.noorm.test.hr.services.BeanDML;

import static org.junit.Assert.*;
import static junit.framework.Assert.fail;

/**
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 14.04.12
 *         Time: 13:35
 */
public class OptLockLongTest {

	private static final String SOME_TEXT = "SOME_TEXT";
	private static final String SOME_NEW_TEXT = "SOME_NEW_TEXT";

    private BeanDML beanDML = BeanDML.getInstance();

	@Test
	public void testOptLockLongCRUD() {

		DataSourceProvider.begin();
        try {
            final OptLockLongBean newOptLockLongBean = new OptLockLongBean();
            newOptLockLongBean.setText(SOME_TEXT);
            final OptLockLongBean insertedOptLockLongBean =  beanDML.insertOptLockLong(newOptLockLongBean);
            assertEquals(SOME_TEXT, insertedOptLockLongBean.getText());
            insertedOptLockLongBean.setText(SOME_NEW_TEXT);
            beanDML.updateOptLockLong(insertedOptLockLongBean);
            beanDML.deleteOptLockLong(insertedOptLockLongBean);
            DataSourceProvider.commit();
        } catch (Throwable e) {
            DataSourceProvider.rollback();
            Assert.fail(e.getMessage());
        }
	}

	@Test
	public void testFailOptLockLongCRUD() {

		DataSourceProvider.begin();
		final OptLockLongBean newOptLockLongBean = new OptLockLongBean();
		newOptLockLongBean.setText(SOME_TEXT);
        beanDML.insertOptLockLong(newOptLockLongBean);
		try {
			newOptLockLongBean.setVersion(0L);
            beanDML.updateOptLockLong(newOptLockLongBean);
			fail();
		} catch (DataAccessException e) {
            if (!e.getType().equals(DataAccessException.Type.OPTIMISTIC_LOCK_CONFLICT)) {
                fail(e.getMessage());
            }
		} finally {
            DataSourceProvider.rollback();
        }
	}
}
