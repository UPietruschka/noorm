package org.noorm.test;

import org.junit.Test;
import org.noorm.jdbc.DataSourceProvider;
import org.noorm.test.hr.beans.CaseSensitiveTable;
import org.noorm.test.hr.services.BeanDML;
import org.noorm.test.hr.services.DeclaredQueries;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 29.07.13
 *         Time: 12:03
 */
public class CaseSensitiveTest {

    private BeanDML beanDML = BeanDML.getInstance();

    @Test
    public void testCaseSensitiveCRUD() {

        DataSourceProvider.begin();
        try {
            final DeclaredQueries declaredQueries = DeclaredQueries.getInstance();
            final List<CaseSensitiveTable> csList = new ArrayList<CaseSensitiveTable>();
            final CaseSensitiveTable cs1 = new CaseSensitiveTable();
            cs1.setC1(1L);
            cs1.setC2("A");
            csList.add(cs1);
            final CaseSensitiveTable cs2 = new CaseSensitiveTable();
            cs2.setC1(2L);
            cs2.setC2("B");
            csList.add(cs2);
            beanDML.insertCaseSensitiveTableList(csList);
            final List<CaseSensitiveTable> csList1 = declaredQueries.findCaseSensitiveTable();
            assertEquals(2L, csList1.size());
            final CaseSensitiveTable newCS1 = csList1.get(0);
            newCS1.setC2("C");
            beanDML.updateCaseSensitiveTable(newCS1);
            beanDML.deleteCaseSensitiveTableList(csList1);
            final List<CaseSensitiveTable> csList2 = declaredQueries.findCaseSensitiveTable();
            assertEquals(0L, csList2.size());
            DataSourceProvider.commit();
        } catch (Throwable e) {
            DataSourceProvider.rollback();
            fail(e.getMessage());
        }
    }
}
