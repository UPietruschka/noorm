package org.noorm.test;

import org.junit.Test;
import org.noorm.jdbc.DataAccessException;
import org.noorm.jdbc.DataSourceProvider;
import org.noorm.jdbc.JDBCStatementProcessor;
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
		final OptLockLongBean newOptLockLongBean = new OptLockLongBean();
		newOptLockLongBean.setText(SOME_TEXT);
		JDBCStatementProcessor<OptLockLongBean> jdbcStatementProcessor = JDBCStatementProcessor.getInstance();
		OptLockLongBean insertedOptLockLongBean =  jdbcStatementProcessor.insert(newOptLockLongBean);
        assertEquals(SOME_TEXT, insertedOptLockLongBean.getText());
        insertedOptLockLongBean.setText(SOME_NEW_TEXT);
		jdbcStatementProcessor.update(insertedOptLockLongBean);
		jdbcStatementProcessor.delete(insertedOptLockLongBean);
		DataSourceProvider.commit();
	}

	@Test
	public void testFailOptLockLongCRUD() {

		DataSourceProvider.begin();
		final OptLockLongBean newOptLockLongBean = new OptLockLongBean();
		newOptLockLongBean.setText(SOME_TEXT);
		JDBCStatementProcessor<OptLockLongBean> jdbcStatementProcessor = JDBCStatementProcessor.getInstance();
		jdbcStatementProcessor.insert(newOptLockLongBean);
		try {
			newOptLockLongBean.setVersion(0L);
			jdbcStatementProcessor.update(newOptLockLongBean);
			fail();
		} catch (DataAccessException e) {
			DataSourceProvider.rollback();
		}
	}
}
