package org.noorm.test;

import org.junit.Test;
import org.noorm.jdbc.DataSourceProvider;
import org.noorm.test.hr.beans.CaseSensitiveTableBean;
import org.noorm.test.hr.services.BeanDML;
import org.noorm.test.hr.services.DeclaredQueries;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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
            final List<CaseSensitiveTableBean> csBeanList = new ArrayList<CaseSensitiveTableBean>();
            final CaseSensitiveTableBean csBean1 = new CaseSensitiveTableBean();
            csBean1.setC1(1L);
            csBean1.setC2("A");
            csBeanList.add(csBean1);
            final CaseSensitiveTableBean csBean2 = new CaseSensitiveTableBean();
            csBean2.setC1(2L);
            csBean2.setC2("B");
            csBeanList.add(csBean2);
            beanDML.insertCaseSensitiveTableList(csBeanList);
            final List<CaseSensitiveTableBean> csBeanBeanList = declaredQueries.findCaseSensitiveTable();
            assertEquals(2L, csBeanBeanList.size());
            final CaseSensitiveTableBean newCSBean1 = csBeanBeanList.get(0);
            newCSBean1.setC2("C");
            beanDML.updateCaseSensitiveTable(newCSBean1);
            beanDML.deleteCaseSensitiveTableList(csBeanBeanList);
            final List<CaseSensitiveTableBean> csBeanBeanList1 = declaredQueries.findCaseSensitiveTable();
            assertEquals(0L, csBeanBeanList1.size());
            DataSourceProvider.commit();
        } catch (Throwable e) {
            DataSourceProvider.rollback();
            fail(e.getMessage());
        }
    }
}
