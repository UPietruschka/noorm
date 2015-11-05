package org.noorm.test;

import org.junit.Test;
import org.noorm.jdbc.DataSourceProvider;
import org.noorm.jdbc.JDBCDMLProcessor;
import org.noorm.jdbc.LOBHelper;
import org.noorm.test.hr.beans.ComplexDataTypes;
import org.noorm.test.hr.services.ComplexDataService;
import org.noorm.test.hr.services.DeclaredQueries;

import java.sql.Blob;
import java.sql.Clob;
import java.util.ArrayList;
import java.util.List;

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
    /*
     Test large BLOBs with 20MB. Larger test BLOB sizes may be suitable, but 20MB should run out of the
     box for fresh IDE setups without the need to increase heap size.
     */
    private static final int TWENTY_MB = 20971520;

    private ComplexDataService complexDataService = ComplexDataService.getInstance();
    private DeclaredQueries declaredQueries = DeclaredQueries.getInstance();

    @Test
    public void testRawCRUD() {

        DataSourceProvider.begin();
        try {
            final ComplexDataTypes complexDataTypes = new ComplexDataTypes();
            complexDataTypes.setRawTypeColumn(SOME_BYTE_ARRAY);
            final JDBCDMLProcessor<ComplexDataTypes> dmlProcessor = JDBCDMLProcessor.getInstance();
            final ComplexDataTypes inserted = dmlProcessor.insert(complexDataTypes);
            final ComplexDataTypes inserted0 = complexDataService.findUniqueCdtById(inserted.getId());
            assertEquals(inserted, inserted0);
            assertArrayEquals(SOME_BYTE_ARRAY, inserted0.getRawTypeColumn());
            final ComplexDataTypes inserted1 = declaredQueries.findComplexDataTypesByRawTypeColumn(SOME_BYTE_ARRAY);
            assertEquals(inserted, inserted1);
            inserted.setRawTypeColumn(SOME_NEW_BYTE_ARRAY);
            dmlProcessor.update(inserted0);
            dmlProcessor.delete(inserted0);
            DataSourceProvider.commit();
        } catch (Throwable e) {
            DataSourceProvider.rollback();
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testBlobCRUD() {

        DataSourceProvider.begin();
        try {
            final ComplexDataTypes complexDataTypes = new ComplexDataTypes();
            final Blob blob0 = LOBHelper.createBlob();
            blob0.setBytes(1, SOME_BYTE_ARRAY);
            complexDataTypes.setBlobColumn(blob0);
            final JDBCDMLProcessor<ComplexDataTypes> dmlProcessor = JDBCDMLProcessor.getInstance();
            final ComplexDataTypes inserted = dmlProcessor.insert(complexDataTypes);
            final ComplexDataTypes inserted0 = complexDataService.findUniqueCdtById(inserted.getId());
            assertEquals(inserted, inserted0);
            final Blob readBlob = inserted0.getBlobColumn();
            final int length = (int) readBlob.length();
            assertArrayEquals(SOME_BYTE_ARRAY, readBlob.getBytes(1, length));
            final Blob blob1 = LOBHelper.createBlob();
            blob1.setBytes(1, SOME_NEW_BYTE_ARRAY);
            inserted.setBlobColumn(blob1);
            dmlProcessor.update(inserted0);
            dmlProcessor.delete(inserted0);
            DataSourceProvider.commit();
        } catch (Throwable e) {
            DataSourceProvider.rollback();
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    /**
     * We do not utilize Oracle proprietary APIs for LOB handling, since with Oracle 11 and JDBC 4.0 support,
     * usage of the standard JDBC 4.0 API is sufficient and recommended. However, Oracle uses proprietary
     * functionality behind the scenes, which does not support JDBC batches in all situations (apparently, a
     * vendors JDBC driver uses proprietary functionality, but the more specific this functionality is, the
     * higher is the risk for incompatibilities).
     */
    @Test
    public void testBlobBatch() {

        DataSourceProvider.begin();
        try {
            final List<ComplexDataTypes> beanList = new ArrayList<ComplexDataTypes>();
            final ComplexDataTypes complexDataTypes = new ComplexDataTypes();
            complexDataTypes.setGroupId(1L);
            final Blob blob = LOBHelper.createBlob();
            blob.setBytes(1, SOME_BYTE_ARRAY);
            complexDataTypes.setBlobColumn(blob);
            beanList.add(complexDataTypes);
            beanList.add(complexDataTypes);
            beanList.add(complexDataTypes);
            final JDBCDMLProcessor<ComplexDataTypes> dmlProcessor = JDBCDMLProcessor.getInstance();
            dmlProcessor.insert(beanList);
            final List<ComplexDataTypes> beanList0 = complexDataService.findCdtByGroupId(1L);
            dmlProcessor.delete(beanList0);
            DataSourceProvider.commit();
        } catch (Throwable e) {
            DataSourceProvider.rollback();
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testClobCRUD() {

        DataSourceProvider.begin();
        try {
            final ComplexDataTypes complexDataTypes = new ComplexDataTypes();
            final Clob clob0 = LOBHelper.createClob();
            clob0.setString(1, SOME_TEXT);
            complexDataTypes.setClobColumn(clob0);
            final JDBCDMLProcessor<ComplexDataTypes> dmlProcessor = JDBCDMLProcessor.getInstance();
            final ComplexDataTypes inserted = dmlProcessor.insert(complexDataTypes);
            final ComplexDataTypes inserted0 = complexDataService.findUniqueCdtById(inserted.getId());
            assertEquals(inserted, inserted0);
            final Clob readClob = inserted0.getClobColumn();
            final int length = (int) readClob.length();
            assertEquals(SOME_TEXT, readClob.getSubString(1, length));
            final Clob clob1 = LOBHelper.createClob();
            clob1.setString(1, SOME_NEW_TEXT);
            inserted.setClobColumn(clob1);
            dmlProcessor.update(inserted0);
            dmlProcessor.delete(inserted0);
            DataSourceProvider.commit();
        } catch (Throwable e) {
            DataSourceProvider.rollback();
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testLargeBlob() {

        DataSourceProvider.begin();
        try {
            final ComplexDataTypes complexDataTypes = new ComplexDataTypes();
            final byte[] largeByteArray = new byte[TWENTY_MB];
            final Blob blob = LOBHelper.createBlob();
            blob.setBytes(1, largeByteArray);
            complexDataTypes.setBlobColumn(blob);
            final JDBCDMLProcessor<ComplexDataTypes> dmlProcessor = JDBCDMLProcessor.getInstance();
            final ComplexDataTypes inserted = dmlProcessor.insert(complexDataTypes);
            final ComplexDataTypes inserted0 = complexDataService.findUniqueCdtById(inserted.getId());
            assertEquals(inserted, inserted0);
            dmlProcessor.delete(inserted);
            DataSourceProvider.commit();
        } catch (Throwable e) {
            DataSourceProvider.rollback();
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testLargeClob() {

        DataSourceProvider.begin();
        try {
            final ComplexDataTypes complexDataTypes = new ComplexDataTypes();
            final String largeString = createLargeString(TWENTY_MB);
            final Clob clob = LOBHelper.createClob();
            clob.setString(1, largeString);
            complexDataTypes.setClobColumn(clob);
            final JDBCDMLProcessor<ComplexDataTypes> dmlProcessor = JDBCDMLProcessor.getInstance();
            final ComplexDataTypes inserted = dmlProcessor.insert(complexDataTypes);
            final ComplexDataTypes inserted0 = complexDataService.findUniqueCdtById(inserted.getId());
            assertEquals(inserted, inserted0);
            dmlProcessor.delete(inserted0);
            DataSourceProvider.commit();
        } catch (Throwable e) {
            DataSourceProvider.rollback();
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    private static String createLargeString(final int pSize) {
        final StringBuilder sb = new StringBuilder(pSize);
        for (int i=0; i < pSize; i++) {
            sb.append('a');
        }
        return sb.toString();
    }
}
