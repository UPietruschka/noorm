package org.noorm.test;

import org.junit.Assert;
import org.junit.Test;
import org.noorm.jdbc.DataAccessException;
import org.noorm.jdbc.DataSourceProvider;
import org.noorm.test.hr.beans.OptLockLong;
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
            final OptLockLong newOptLockLong = new OptLockLong();
            newOptLockLong.setText(SOME_TEXT);
            final OptLockLong insertedOptLockLong =  beanDML.insertOptLockLong(newOptLockLong);
            assertEquals(SOME_TEXT, insertedOptLockLong.getText());
            insertedOptLockLong.setText(SOME_NEW_TEXT);
            beanDML.updateOptLockLong(insertedOptLockLong);
            beanDML.deleteOptLockLong(insertedOptLockLong);
            DataSourceProvider.commit();
        } catch (Throwable e) {
            DataSourceProvider.rollback();
            Assert.fail(e.getMessage());
        }
	}

	@Test
	public void testFailOptLockLongCRUD() {

		DataSourceProvider.begin();
		final OptLockLong newOptLockLong = new OptLockLong();
		newOptLockLong.setText(SOME_TEXT);
        beanDML.insertOptLockLong(newOptLockLong);
		try {
			newOptLockLong.setVersion(0L);
            beanDML.updateOptLockLong(newOptLockLong);
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
