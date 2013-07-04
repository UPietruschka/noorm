package org.noorm.test;

import org.junit.Test;
import org.noorm.jdbc.DataSourceProvider;
import org.noorm.jdbc.JDBCDMLProcessor;
import org.noorm.test.hr.beans.ComplexDataTypesBean;
import org.noorm.test.hr.services.ComplexDataService;

import java.sql.Timestamp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 28.06.13
 *         Time: 19:35
 */
public class CustomTypeMappingTest {

    private ComplexDataService complexDataService = ComplexDataService.getInstance();

    @Test
    public void testBooleanCustomTypeMapping() {

        DataSourceProvider.begin();
        try {
            final ComplexDataTypesBean complexDataTypesBean = new ComplexDataTypesBean();
            complexDataTypesBean.setBooleanFlag(true);
            final JDBCDMLProcessor<ComplexDataTypesBean> dmlProcessor = JDBCDMLProcessor.getInstance();
            final ComplexDataTypesBean insertedBean = dmlProcessor.insert(complexDataTypesBean);
            final ComplexDataTypesBean insertedBean0 = complexDataService.findUniqueCdtById(insertedBean.getId());
            assertEquals(insertedBean, insertedBean0);
            assertEquals(Boolean.TRUE, insertedBean.getBooleanFlag());
            insertedBean.setBooleanFlag(false);
            dmlProcessor.update(insertedBean);
            final ComplexDataTypesBean insertedBean1 = complexDataService.findUniqueCdtById(insertedBean.getId());
            assertEquals(Boolean.FALSE, insertedBean1.getBooleanFlag());
            dmlProcessor.delete(insertedBean1);
            DataSourceProvider.commit();
        } catch (Throwable e) {
            DataSourceProvider.rollback();
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testNumber2StringCustomTypeMapping() {

        DataSourceProvider.begin();
        try {
            final ComplexDataTypesBean complexDataTypesBean = new ComplexDataTypesBean();
            complexDataTypesBean.setConvertedNumber("55");
            final JDBCDMLProcessor<ComplexDataTypesBean> dmlProcessor = JDBCDMLProcessor.getInstance();
            final ComplexDataTypesBean insertedBean = dmlProcessor.insert(complexDataTypesBean);
            final ComplexDataTypesBean insertedBean0 = complexDataService.findUniqueCdtById(insertedBean.getId());
            assertEquals(insertedBean, insertedBean0);
            assertEquals("55", insertedBean0.getConvertedNumber());
            dmlProcessor.delete(insertedBean0);
            DataSourceProvider.commit();
        } catch (Throwable e) {
            DataSourceProvider.rollback();
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testDateCustomTypeMapping() {

        DataSourceProvider.begin();
        try {
            final ComplexDataTypesBean complexDataTypesBean = new ComplexDataTypesBean();
            final long millis = System.currentTimeMillis();
            java.sql.Date date = new java.sql.Date(millis);
            Timestamp timestamp = new Timestamp(millis);
            complexDataTypesBean.setConvertedDate(date);
            complexDataTypesBean.setConvertedTimestamp(timestamp);
            final JDBCDMLProcessor<ComplexDataTypesBean> dmlProcessor = JDBCDMLProcessor.getInstance();
            final ComplexDataTypesBean insertedBean = dmlProcessor.insert(complexDataTypesBean);
            final ComplexDataTypesBean insertedBean0 = complexDataService.findUniqueCdtById(insertedBean.getId());
            assertEquals(insertedBean, insertedBean0);
            // The Oracle DATE type does not support milliseconds, so we cut them off
            final long secondsOnlyMillis = date.getTime() / 1000 * 1000;
            assertEquals(secondsOnlyMillis, insertedBean0.getConvertedDate().getTime());
            assertEquals(timestamp, insertedBean0.getConvertedTimestamp());
            dmlProcessor.delete(insertedBean0);
            DataSourceProvider.commit();
        } catch (Throwable e) {
            DataSourceProvider.rollback();
            e.printStackTrace();
            fail(e.getMessage());
        }
    }
}
