package org.noorm.test;

import org.junit.Test;
import org.noorm.jdbc.DataSourceProvider;
import org.noorm.jdbc.JDBCDMLProcessor;
import org.noorm.test.hr.beans.ComplexDataTypes;
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
            final ComplexDataTypes complexDataTypes = new ComplexDataTypes();
            complexDataTypes.setBooleanFlag(true);
            final JDBCDMLProcessor<ComplexDataTypes> dmlProcessor = JDBCDMLProcessor.getInstance();
            final ComplexDataTypes inserted = dmlProcessor.insert(complexDataTypes);
            final ComplexDataTypes inserted0 = complexDataService.findUniqueCdtById(inserted.getId());
            assertEquals(inserted, inserted0);
            assertEquals(Boolean.TRUE, inserted.getBooleanFlag());
            inserted.setBooleanFlag(false);
            dmlProcessor.update(inserted);
            final ComplexDataTypes inserted1 = complexDataService.findUniqueCdtById(inserted.getId());
            assertEquals(Boolean.FALSE, inserted1.getBooleanFlag());
            dmlProcessor.delete(inserted1);
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
            final ComplexDataTypes complexDataTypes = new ComplexDataTypes();
            complexDataTypes.setConvertedNumber("55");
            final JDBCDMLProcessor<ComplexDataTypes> dmlProcessor = JDBCDMLProcessor.getInstance();
            final ComplexDataTypes inserted = dmlProcessor.insert(complexDataTypes);
            final ComplexDataTypes inserted0 = complexDataService.findUniqueCdtById(inserted.getId());
            assertEquals(inserted, inserted0);
            assertEquals("55", inserted0.getConvertedNumber());
            dmlProcessor.delete(inserted0);
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
            final ComplexDataTypes complexDataTypes = new ComplexDataTypes();
            final long millis = System.currentTimeMillis();
            java.sql.Date date = new java.sql.Date(millis);
            Timestamp timestamp = new Timestamp(millis);
            complexDataTypes.setConvertedDate(date);
            complexDataTypes.setConvertedTimestamp(timestamp);
            final JDBCDMLProcessor<ComplexDataTypes> dmlProcessor = JDBCDMLProcessor.getInstance();
            final ComplexDataTypes inserted = dmlProcessor.insert(complexDataTypes);
            final ComplexDataTypes inserted0 = complexDataService.findUniqueCdtById(inserted.getId());
            assertEquals(inserted, inserted0);
            // The Oracle DATE type does not support milliseconds, so we cut them off
            final long secondsOnlyMillis = date.getTime() / 1000 * 1000;
            assertEquals(secondsOnlyMillis, inserted0.getConvertedDate().getTime());
            assertEquals(timestamp, inserted0.getConvertedTimestamp());
            dmlProcessor.delete(inserted0);
            DataSourceProvider.commit();
        } catch (Throwable e) {
            DataSourceProvider.rollback();
            e.printStackTrace();
            fail(e.getMessage());
        }
    }
}
