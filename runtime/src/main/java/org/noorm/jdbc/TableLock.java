package org.noorm.jdbc;

import oracle.jdbc.OracleConnection;
import oracle.jdbc.OraclePreparedStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

/**
 * Utility class for acquiring table locks. Only works and makes with explicit (user managed) transaction
 * handling. Acquiring a lock without explicitly starting a transaction before will cause an exception.
 *
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 23.05.13
 *         Time: 11:01
 */
public class TableLock {

    private static final Logger log = LoggerFactory.getLogger(TableLock.class);

    public static void acquire(final IBean pBean, final LockMode pLockMode) {
        acquire(pBean, pLockMode, "");
    }

    public static void acquireNoWait(final IBean pBean, final LockMode pLockMode) {
        acquire(pBean, pLockMode, " NOWAIT");
    }

    private static void acquire(final IBean pBean, final LockMode pLockMode, final String pWaitClause) {

        if (!DataSourceProvider.activeUserManagedTransaction()) {
            log.error(DataAccessException.Type.ISOLATED_TABLE_LOCK.getDescription());
            throw new DataAccessException(DataAccessException.Type.ISOLATED_TABLE_LOCK);
        }
        final String lockStmt = "LOCK TABLE ".concat(pBean.getTableName())
                .concat(" IN ").concat(pLockMode.getMode()).concat(" MODE").concat(pWaitClause);
        boolean success = true;
        OracleConnection con = null;
        OraclePreparedStatement stmt = null;
        try {
            con = DataSourceProvider.getConnection();
            stmt = (OraclePreparedStatement) con.prepareStatement(lockStmt);
            stmt.execute();
        } catch (Exception e) {
            log.error(DataAccessException.Type.COULD_NOT_ACQUIRE_TABLE_LOCK.getDescription(), e);
            success = false;
            throw new DataAccessException(DataAccessException.Type.COULD_NOT_ACQUIRE_TABLE_LOCK, e);
        } finally {
            try {
                if (stmt != null) {
                    stmt.close();
                }
                if (con != null && !con.isClosed()) {
                    DataSourceProvider.returnConnection(success);
                }
            } catch (SQLException ignored) {
            } // Nothing to do
        }
    }

    public static enum LockMode {

        ROW_SHARE("ROW SHARE"),
        ROW_EXCLUSIVE("ROW EXCLUSIVE"),
        SHARE("SHARE"),
        SHARE_ROW_EXCLUSIVE("SHARE ROW EXCLUSIVE"),
        EXCLUSIVE("EXCLUSIVE");

        private String mode;

        private LockMode(final String pMode) {
            mode = pMode;
        }

        public String getMode() {
            return mode;
        }
    }
}
