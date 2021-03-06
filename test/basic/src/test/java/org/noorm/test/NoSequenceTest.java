package org.noorm.test;

import org.junit.Assert;
import org.junit.Test;
import org.noorm.jdbc.DataAccessException;
import org.noorm.jdbc.DataSourceProvider;
import org.noorm.test.hr.beans.NoSeqNoPk;
import org.noorm.test.hr.beans.NoSeqWithPk;
import org.noorm.test.hr.services.NoSeqNoPkDML;
import org.noorm.test.hr.services.NoSeqWithPkDML;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 14.04.18
 *         Time: 13:35
 */
public class NoSequenceTest {

    private static final String SOME_TEXT = "SOME_TEXT";
    private static final String SOME_NEW_TEXT = "SOME_NEW_TEXT";

    private NoSeqWithPkDML noSeqWithPkDML = NoSeqWithPkDML.getInstance();
    private NoSeqNoPkDML noSeqNoPkDML = NoSeqNoPkDML.getInstance();

    @Test
    public void testNoSeqWithPkCRUD() {

        DataSourceProvider.begin();
        try {
            final NoSeqWithPk noSeqWithPk = new NoSeqWithPk();
            noSeqWithPk.setId(1L);
            noSeqWithPk.setText(SOME_TEXT);
            final NoSeqWithPk insertedNoSeqWithPk =  noSeqWithPkDML.insertNoSeqWithPk(noSeqWithPk);
            assertEquals(SOME_TEXT, insertedNoSeqWithPk.getText());
            insertedNoSeqWithPk.setText(SOME_NEW_TEXT);
            noSeqWithPkDML.updateNoSeqWithPk(insertedNoSeqWithPk);
            noSeqWithPkDML.deleteNoSeqWithPk(insertedNoSeqWithPk);
            DataSourceProvider.commit();
        } catch (Throwable e) {
            DataSourceProvider.rollback();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testFailNoSeqNoPkCRUD() {

        DataSourceProvider.begin();
        try {
            final NoSeqNoPk noSeqNoPk = new NoSeqNoPk();
            noSeqNoPk.setId(1L);
            noSeqNoPk.setText(SOME_TEXT);
            final NoSeqNoPk insertedNoSeqNoPk = noSeqNoPkDML.insertNoSeqNoPk(noSeqNoPk);
            assertEquals(SOME_TEXT, insertedNoSeqNoPk.getText());
            insertedNoSeqNoPk.setText(SOME_NEW_TEXT);
            noSeqNoPkDML.updateNoSeqNoPk(insertedNoSeqNoPk);
            DataSourceProvider.commit();
        } catch (DataAccessException e) {
            if (!e.getType().equals(DataAccessException.Type.GENERIC_UPDATE_NOT_SUPPORTED_WITHOUT_PK)) {
                fail(e.getMessage());
            }
        } finally {
            DataSourceProvider.rollback();
        }
    }
}
