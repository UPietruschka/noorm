package org.noorm.test;

import org.junit.Ignore;
import org.junit.Test;
import org.noorm.jdbc.DataSourceProvider;
import org.noorm.jdbc.JDBCDMLProcessor;
import org.noorm.jdbc.LOBHelper;
import org.noorm.test.hr.beans.ComplexDataTypesBean;
import org.noorm.test.hr.services.ComplexDataService;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.NClob;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * These tests combine custom type mappings and streaming features of the LOB types Clob, NClob, Blob and
 * SQLXML (The LOB types are available through custom type mappings only, since NoORM by default maps LOBs
 * to simple types like String and byte[]).
 *
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 04.07.13
 *         Time: 19:03
 */
public class LOBStreamingTest {

    private static final byte[] SOME_BYTE_ARRAY = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 0 };
    private static final byte[] SOME_NEW_BYTE_ARRAY = new byte[] { 0, 9, 8, 7, 6, 5, 4, 3, 2, 1 };
    private static final String SOME_TEXT = "SOME_TEXT";
    private static final String SOME_NEW_TEXT = "SOME_NEW_TEXT";
    private static final String SOME_XML = "<root type=\"INSERT\"><e1>TEST1</e1><e2>TEST2</e2></root>";
    private static final String SOME_NEW_XML = "<root type=\"UPDATE\"><e1>TEST3</e1><e2>TEST4</e2></root>";

    private ComplexDataService complexDataService = ComplexDataService.getInstance();

    @Test
    public void testClobCRUD() {

        DataSourceProvider.begin();
        try {

            // Creation
            final ComplexDataTypesBean complexDataTypesBean = new ComplexDataTypesBean();
            final Clob clob = LOBHelper.createClob();
            final Writer writer1 = clob.setCharacterStream(1);
            writer1.append(SOME_TEXT);
            writer1.close();
            complexDataTypesBean.setConvertedClobColumn(clob);
            final JDBCDMLProcessor<ComplexDataTypesBean> dmlProcessor = JDBCDMLProcessor.getInstance();
            final ComplexDataTypesBean insertedBean = dmlProcessor.insert(complexDataTypesBean);
            final ComplexDataTypesBean insertedBean0 = complexDataService.findUniqueCdtById(insertedBean.getId());
            assertEquals(insertedBean, insertedBean0);
            final Reader reader1 = insertedBean0.getConvertedClobColumn().getCharacterStream();
            final char[] buffer1 = new char[SOME_TEXT.length()];
            reader1.read(buffer1);
            reader1.close();
            assertEquals(SOME_TEXT, String.copyValueOf(buffer1));

            // Update
            final Writer writer2 = insertedBean0.getConvertedClobColumn().setCharacterStream(1);
            writer2.append(SOME_NEW_TEXT);
            writer2.close();
            dmlProcessor.update(insertedBean0);
            final ComplexDataTypesBean insertedBean1 = complexDataService.findUniqueCdtById(insertedBean0.getId());
            assertEquals(insertedBean0, insertedBean1);
            final Reader reader2 = insertedBean1.getConvertedClobColumn().getCharacterStream();
            final char[] buffer2 = new char[SOME_NEW_TEXT.length()];
            reader2.read(buffer2);
            reader2.close();
            assertEquals(SOME_NEW_TEXT, String.copyValueOf(buffer2));

            // Delete
            dmlProcessor.delete(insertedBean0);
            DataSourceProvider.commit();

        } catch (Throwable e) {
            DataSourceProvider.rollback();
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testNClobCRUD() {

        DataSourceProvider.begin();
        try {

            // Creation
            final ComplexDataTypesBean complexDataTypesBean = new ComplexDataTypesBean();
            final NClob nclob = LOBHelper.createNClob();
            final Writer writer1 = nclob.setCharacterStream(1);
            writer1.append(SOME_TEXT);
            writer1.close();
            complexDataTypesBean.setConvertedNclobColumn(nclob);
            final JDBCDMLProcessor<ComplexDataTypesBean> dmlProcessor = JDBCDMLProcessor.getInstance();
            final ComplexDataTypesBean insertedBean = dmlProcessor.insert(complexDataTypesBean);
            final ComplexDataTypesBean insertedBean0 = complexDataService.findUniqueCdtById(insertedBean.getId());
            assertEquals(insertedBean, insertedBean0);
            final Reader reader1 = insertedBean0.getConvertedNclobColumn().getCharacterStream();
            final char[] buffer1 = new char[SOME_TEXT.length()];
            reader1.read(buffer1);
            reader1.close();
            assertEquals(SOME_TEXT, String.copyValueOf(buffer1));

            // Update
            final Writer writer2 = insertedBean0.getConvertedNclobColumn().setCharacterStream(1);
            writer2.append(SOME_NEW_TEXT);
            writer2.close();
            dmlProcessor.update(insertedBean0);
            final ComplexDataTypesBean insertedBean1 = complexDataService.findUniqueCdtById(insertedBean0.getId());
            assertEquals(insertedBean0, insertedBean1);
            final Reader reader2 = insertedBean1.getConvertedNclobColumn().getCharacterStream();
            final char[] buffer2 = new char[SOME_NEW_TEXT.length()];
            reader2.read(buffer2);
            reader2.close();
            assertEquals(SOME_NEW_TEXT, String.copyValueOf(buffer2));

            // Delete
            dmlProcessor.delete(insertedBean0);
            DataSourceProvider.commit();

        } catch (Throwable e) {
            DataSourceProvider.rollback();
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Ignore
    @Test
    public void testBlobCRUD() {

        DataSourceProvider.begin();
        try {

            // Creation
            final ComplexDataTypesBean complexDataTypesBean = new ComplexDataTypesBean();
            final Blob blob = LOBHelper.createBlob();
            final OutputStream oStream1 = blob.setBinaryStream(1);
            oStream1.write(SOME_BYTE_ARRAY);
            oStream1.close();
            complexDataTypesBean.setConvertedBlobColumn(blob);
            final JDBCDMLProcessor<ComplexDataTypesBean> dmlProcessor = JDBCDMLProcessor.getInstance();
            final ComplexDataTypesBean insertedBean = dmlProcessor.insert(complexDataTypesBean);
            final ComplexDataTypesBean insertedBean0 = complexDataService.findUniqueCdtById(insertedBean.getId());
            assertEquals(insertedBean, insertedBean0);
            final InputStream iStream = insertedBean0.getConvertedBlobColumn().getBinaryStream();
            final byte[] buffer1 = new byte[SOME_BYTE_ARRAY.length];
            iStream.read(buffer1);
            iStream.close();
            assertEquals(SOME_BYTE_ARRAY, buffer1);

            // Update
            final OutputStream oStream2 = insertedBean0.getConvertedBlobColumn().setBinaryStream(1);
            oStream2.write(SOME_NEW_BYTE_ARRAY);
            oStream2.close();
            dmlProcessor.update(insertedBean0);
            final ComplexDataTypesBean insertedBean1 = complexDataService.findUniqueCdtById(insertedBean0.getId());
            assertEquals(insertedBean0, insertedBean1);

            final InputStream iStream2 = insertedBean1.getConvertedBlobColumn().getBinaryStream();
            final byte[] buffer2 = new byte[SOME_NEW_BYTE_ARRAY.length];
            iStream2.read(buffer2);
            iStream2.close();
            assertEquals(SOME_NEW_BYTE_ARRAY, buffer2);

            // Delete
            dmlProcessor.delete(insertedBean0);
            DataSourceProvider.commit();

        } catch (Throwable e) {
            DataSourceProvider.rollback();
            e.printStackTrace();
            fail(e.getMessage());
        }
    }
}
