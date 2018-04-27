package org.noorm.test;

import org.junit.Assert;
import org.junit.Test;
import org.noorm.jdbc.DataAccessException;
import org.noorm.jdbc.DataSourceProvider;
import org.noorm.test.hr.beans.OptLockDate;
import org.noorm.test.hr.services.BeanDML;

import java.sql.Timestamp;

import static org.junit.Assert.*;

/**
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 01.05.13
 *         Time: 15:35
 */
public class OptLockDateTest {

    private static final String SOME_TEXT = "SOME_TEXT";
    private static final String SOME_NEW_TEXT = "SOME_NEW_TEXT";

    private BeanDML beanDML = BeanDML.getInstance();

    @Test
    public void testOptLockDateCRUD() {

        DataSourceProvider.begin();
        try {
            final OptLockDate newOptLockDate = new OptLockDate();
            newOptLockDate.setText(SOME_TEXT);
            final OptLockDate insertedOptLockDate =  beanDML.insertOptLockDate(newOptLockDate);
            assertEquals(SOME_TEXT, insertedOptLockDate.getText());
            insertedOptLockDate.setText(SOME_NEW_TEXT);
            beanDML.updateOptLockDate(insertedOptLockDate);
            beanDML.deleteOptLockDate(insertedOptLockDate);
            DataSourceProvider.commit();
        } catch (Throwable e) {
            DataSourceProvider.rollback();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testFailOptLockDateCRUD() {

        DataSourceProvider.begin();
        final OptLockDate newOptLockDate = new OptLockDate();
        newOptLockDate.setText(SOME_TEXT);
        beanDML.insertOptLockDate(newOptLockDate);
        try {
            newOptLockDate.setVersion(new Timestamp(1200000000000L)); // January 10, 2008, 22:20
            beanDML.updateOptLockDate(newOptLockDate);
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
