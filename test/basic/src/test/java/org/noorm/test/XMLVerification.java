package org.noorm.test;

import org.junit.Test;
import org.noorm.jdbc.DataSourceProvider;
import org.noorm.jdbc.JDBCDMLProcessor;
import org.noorm.jdbc.LOBHelper;
import org.noorm.test.hr.beans.ComplexDataTypes;
import org.noorm.test.hr.services.ComplexDataService;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.sql.SQLXML;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * This test is not enabled by default (activate Maven profile "xml-test" o activate this test), since two
 * additional dependencies are required to run the tests (Oracle xdb and xmlparserv2).
 *
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 05.07.13
 *         Time: 15:29
 */
public class XMLVerification {

    private static final String SOME_XML = "<root type=\"INSERT\"><e1>TEST1</e1><e2>TEST2</e2></root>";
    private static final String SOME_NEW_XML = "<root type=\"UPDATE\"><e1>TEST3</e1><e2>TEST4</e2></root>";

    private ComplexDataService complexDataService = ComplexDataService.getInstance();

    @Test
    public void testXMLTypeCRUD() {

        DataSourceProvider.begin();
        try {
            final ComplexDataTypes complexDataTypes = new ComplexDataTypes();
            complexDataTypes.setXmlColumn(SOME_XML);
            final JDBCDMLProcessor<ComplexDataTypes> dmlProcessor = JDBCDMLProcessor.getInstance();
            final ComplexDataTypes inserted = dmlProcessor.insert(complexDataTypes);
            final ComplexDataTypes inserted0 = complexDataService.findUniqueCdtById(inserted.getId());
            assertEquals(inserted, inserted0);
            // The XML is returned from the database with a different formatting, which causes a simple
            // compare to fail. Thus, we remove all whitespace prior to comparison
            final String normalizedSOME_XML = SOME_XML.replaceAll("\\s","");
            final String normalizedOracleValues = inserted0.getXmlColumn().replaceAll("\\s","");
            assertEquals(normalizedSOME_XML, normalizedOracleValues);
            inserted.setXmlColumn(SOME_NEW_XML);
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
    public void testSQLXMLCRUD() {

        DataSourceProvider.begin();
        try {

            // Creation
            final ComplexDataTypes complexDataTypes = new ComplexDataTypes();
            final SQLXML sqlxml1 = LOBHelper.createSQLXML();
            final Writer writer1 = sqlxml1.setCharacterStream();
            writer1.append(SOME_XML);
            writer1.close();
            complexDataTypes.setConvertedXmltype(sqlxml1);
            final JDBCDMLProcessor<ComplexDataTypes> dmlProcessor = JDBCDMLProcessor.getInstance();
            final ComplexDataTypes inserted = dmlProcessor.insert(complexDataTypes);
            sqlxml1.free();
            final ComplexDataTypes inserted0 = complexDataService.findUniqueCdtById(inserted.getId());
            assertEquals(inserted, inserted0);
            final Reader reader1 = inserted0.getConvertedXmltype().getCharacterStream();
            final String xml1 = readFully(reader1);
            // The XML is returned from the database with a different formatting, which causes a simple
            // compare to fail. Thus, we remove all whitespace prior to comparison
            final String normalizedSOME_XML = SOME_XML.replaceAll("\\s", "");
            final String normalizedOracleValues1 = xml1.replaceAll("\\s", "");
            assertEquals(normalizedSOME_XML, normalizedOracleValues1);

            // Update
            final SQLXML sqlxml2 = LOBHelper.createSQLXML();
            final Writer writer2 = sqlxml2.setCharacterStream();
            writer2.append(SOME_NEW_XML);
            writer2.close();
            inserted0.setConvertedXmltype(sqlxml2);
            dmlProcessor.update(inserted0);
            sqlxml2.free();
            final ComplexDataTypes inserted1 = complexDataService.findUniqueCdtById(inserted0.getId());
            assertEquals(inserted0, inserted1);
            final Reader reader2 = inserted1.getConvertedXmltype().getCharacterStream();
            final String xml2 = readFully(reader2);
            // The XML is returned from the database with a different formatting, which causes a simple
            // compare to fail. Thus, we remove all whitespace prior to comparison
            final String normalizedSOME_NEW_XML = SOME_NEW_XML.replaceAll("\\s","");
            final String normalizedOracleValues2 = xml2.replaceAll("\\s","");
            assertEquals(normalizedSOME_NEW_XML, normalizedOracleValues2);

            // Delete
            dmlProcessor.delete(inserted0);
            DataSourceProvider.commit();

        } catch (Throwable e) {
            DataSourceProvider.rollback();
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    private String readFully(final Reader pReader) throws IOException {

        final char[] cBuf = new char[64];
        final StringBuffer sBuf = new StringBuffer();
        int numChars;
        while ((numChars = pReader.read(cBuf, 0, cBuf.length)) > 0) {
            sBuf.append(cBuf, 0, numChars);
        }
        return sBuf.toString();
    }
}
