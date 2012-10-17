package org.noorm.test;

import org.junit.Test;
import org.noorm.jdbc.DataSourceProvider;
import org.noorm.jdbc.JDBCStatementProcessor;
import org.noorm.test.hr.beans.ComplexDataTypesBean;
import org.noorm.test.hr.services.ComplexDataService;

import static org.junit.Assert.*;

/**
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 16.10.12
 *         Time: 19:35
 */
public class ComplexDataTypeTest {

    private static final byte[] SOME_BYTE_ARRAY = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 0 };
    private static final byte[] SOME_NEW_BYTE_ARRAY = new byte[] { 0, 9, 8, 7, 6, 5, 4, 3, 2, 1 };
    private static final String SOME_TEXT = "SOME_TEXT";
    private static final String SOME_NEW_TEXT = "SOME_NEW_TEXT";

    private ComplexDataService complexDataService = ComplexDataService.getInstance();

    @Test
    public void testRawCRUD() {

        DataSourceProvider.begin();
        final ComplexDataTypesBean complexDataTypesBean = new ComplexDataTypesBean();
        complexDataTypesBean.setRawTypeColumn(SOME_BYTE_ARRAY);
        final JDBCStatementProcessor<ComplexDataTypesBean> jdbcStmtProcessor = JDBCStatementProcessor.getInstance();
        final ComplexDataTypesBean insertedBean = jdbcStmtProcessor.insert(complexDataTypesBean);
        final ComplexDataTypesBean insertedBean0 = complexDataService.findUniqueCdtById(insertedBean.getId());
        assertEquals(insertedBean, insertedBean0);
        assertArrayEquals(SOME_BYTE_ARRAY, insertedBean.getRawTypeColumn());
        insertedBean.setRawTypeColumn(SOME_NEW_BYTE_ARRAY);
        jdbcStmtProcessor.update(insertedBean);
        jdbcStmtProcessor.delete(insertedBean);
        DataSourceProvider.commit();
    }

    @Test
    public void testBlobCRUD() {

        DataSourceProvider.begin();
        final ComplexDataTypesBean complexDataTypesBean = new ComplexDataTypesBean();
        complexDataTypesBean.setBlobColumn(SOME_BYTE_ARRAY);
        final JDBCStatementProcessor<ComplexDataTypesBean> jdbcStmtProcessor = JDBCStatementProcessor.getInstance();
        final ComplexDataTypesBean insertedBean = jdbcStmtProcessor.insert(complexDataTypesBean);
        final ComplexDataTypesBean insertedBean0 = complexDataService.findUniqueCdtById(insertedBean.getId());
        assertEquals(insertedBean, insertedBean0);
        assertArrayEquals(SOME_BYTE_ARRAY, insertedBean.getBlobColumn());
        insertedBean.setBlobColumn(SOME_NEW_BYTE_ARRAY);
        jdbcStmtProcessor.update(insertedBean);
        jdbcStmtProcessor.delete(insertedBean);
        DataSourceProvider.commit();
    }

    @Test
    public void testClobCRUD() {

        DataSourceProvider.begin();
        final ComplexDataTypesBean complexDataTypesBean = new ComplexDataTypesBean();
        complexDataTypesBean.setClobColumn(SOME_TEXT);
        final JDBCStatementProcessor<ComplexDataTypesBean> jdbcStmtProcessor = JDBCStatementProcessor.getInstance();
        final ComplexDataTypesBean insertedBean = jdbcStmtProcessor.insert(complexDataTypesBean);
        final ComplexDataTypesBean insertedBean0 = complexDataService.findUniqueCdtById(insertedBean.getId());
        assertEquals(insertedBean, insertedBean0);
        assertEquals(SOME_TEXT, insertedBean.getClobColumn());
        insertedBean.setClobColumn(SOME_NEW_TEXT);
        jdbcStmtProcessor.update(insertedBean);
        jdbcStmtProcessor.delete(insertedBean);
        DataSourceProvider.commit();
    }

    @Test
    public void testLargeBlob() {

        DataSourceProvider.begin();
        final ComplexDataTypesBean complexDataTypesBean = new ComplexDataTypesBean();
        final byte[] largeByteArray = new byte[104857600];
        complexDataTypesBean.setBlobColumn(largeByteArray);
        final JDBCStatementProcessor<ComplexDataTypesBean> jdbcStmtProcessor = JDBCStatementProcessor.getInstance();
        final ComplexDataTypesBean insertedBean = jdbcStmtProcessor.insert(complexDataTypesBean);
        final ComplexDataTypesBean insertedBean0 = complexDataService.findUniqueCdtById(insertedBean.getId());
        assertEquals(insertedBean, insertedBean0);
        jdbcStmtProcessor.delete(insertedBean);
        DataSourceProvider.commit();
    }
}
