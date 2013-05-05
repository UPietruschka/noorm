package org.noorm.test;

import org.junit.Assert;
import org.junit.Test;
import org.noorm.jdbc.DataAccessException;
import org.noorm.jdbc.DataSourceProvider;
import org.noorm.test.hr.beans.OptLockDateBean;
import org.noorm.test.hr.services.BeanDML;

import java.sql.Timestamp;

import static org.junit.Assert.*;
import static junit.framework.Assert.fail;

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
            final OptLockDateBean newOptLockDateBean = new OptLockDateBean();
            newOptLockDateBean.setText(SOME_TEXT);
            final OptLockDateBean insertedOptLockDateBean =  beanDML.insertOptLockDate(newOptLockDateBean);
            assertEquals(SOME_TEXT, insertedOptLockDateBean.getText());
            insertedOptLockDateBean.setText(SOME_NEW_TEXT);
            beanDML.updateOptLockDate(insertedOptLockDateBean);
            beanDML.deleteOptLockDate(insertedOptLockDateBean);
            DataSourceProvider.commit();
        } catch (Throwable e) {
            DataSourceProvider.rollback();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testFailOptLockDateCRUD() {

        DataSourceProvider.begin();
        final OptLockDateBean newOptLockDateBean = new OptLockDateBean();
        newOptLockDateBean.setText(SOME_TEXT);
        beanDML.insertOptLockDate(newOptLockDateBean);
        try {
            newOptLockDateBean.setVersion(new Timestamp(1200000000000L)); // January 10, 2008, 22:20
            beanDML.updateOptLockDate(newOptLockDateBean);
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
