package org.noorm.test;

import org.junit.Assert;
import org.junit.Test;
import org.noorm.jdbc.DataAccessException;
import org.noorm.jdbc.DataSourceProvider;
import org.noorm.jdbc.JDBCDMLProcessor;
import org.noorm.test.hr.beans.OptLockLongBean;

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

	@Test
	public void testOptLockLongCRUD() {

		DataSourceProvider.begin();
        try {
            final OptLockLongBean newOptLockLongBean = new OptLockLongBean();
            newOptLockLongBean.setText(SOME_TEXT);
            JDBCDMLProcessor<OptLockLongBean> dmlProcessor = JDBCDMLProcessor.getInstance();
            OptLockLongBean insertedOptLockLongBean =  dmlProcessor.insert(newOptLockLongBean);
            assertEquals(SOME_TEXT, insertedOptLockLongBean.getText());
            insertedOptLockLongBean.setText(SOME_NEW_TEXT);
            dmlProcessor.update(insertedOptLockLongBean);
            dmlProcessor.delete(insertedOptLockLongBean);
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
        JDBCDMLProcessor<OptLockLongBean> dmlProcessor = JDBCDMLProcessor.getInstance();
        dmlProcessor.insert(newOptLockLongBean);
		try {
			newOptLockLongBean.setVersion(0L);
            dmlProcessor.update(newOptLockLongBean);
			fail();
		} catch (DataAccessException e) {
            // Bad case test. Do nothing.
		} finally {
            DataSourceProvider.rollback();
        }
	}
}
