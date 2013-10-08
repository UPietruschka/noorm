package org.noorm.test;

import org.junit.Assert;
import org.junit.Test;
import org.noorm.jdbc.DataAccessException;
import org.noorm.jdbc.DataSourceProvider;
import org.noorm.test.hr.beans.OptLockTimestamp;
import org.noorm.test.hr.services.BeanDML;

import java.sql.Timestamp;

import static org.junit.Assert.*;
import static junit.framework.Assert.fail;

/**
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 01.05.13
 *         Time: 13:35
 */
public class OptLockTimestampTest {

    private static final String SOME_TEXT = "SOME_TEXT";
    private static final String SOME_NEW_TEXT = "SOME_NEW_TEXT";

    private BeanDML beanDML = BeanDML.getInstance();

    @Test
    public void testOptLockTimestampCRUD() {

        DataSourceProvider.begin();
        try {
            final OptLockTimestamp newOptLockTimestamp = new OptLockTimestamp();
            newOptLockTimestamp.setText(SOME_TEXT);
            final OptLockTimestamp insertedOptLockTimestamp =
                    beanDML.insertOptLockTimestamp(newOptLockTimestamp);
            assertEquals(SOME_TEXT, insertedOptLockTimestamp.getText());
            insertedOptLockTimestamp.setText(SOME_NEW_TEXT);
            beanDML.updateOptLockTimestamp(insertedOptLockTimestamp);
            beanDML.deleteOptLockTimestamp(insertedOptLockTimestamp);
            DataSourceProvider.commit();
        } catch (Throwable e) {
            DataSourceProvider.rollback();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testFailOptLockTimestampCRUD() {

        DataSourceProvider.begin();
        final OptLockTimestamp newOptLockTimestamp = new OptLockTimestamp();
        newOptLockTimestamp.setText(SOME_TEXT);
        beanDML.insertOptLockTimestamp(newOptLockTimestamp);
        try {
            newOptLockTimestamp.setVersion(new Timestamp(1200000000000L)); // January 10, 2008, 22:20
            beanDML.updateOptLockTimestamp(newOptLockTimestamp);
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
