package org.noorm.jdbc;

import oracle.jdbc.OracleConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

/**
 * Provides creation methods for LOB type java.sql.Clob, java.sql.NClob, java.sql.Blob and
 * java.sql.SQLXML
 *
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 01.07.13
 *         Time: 17:55
 */
public class LOBHelper {

    private static final Logger log = LoggerFactory.getLogger(LOBHelper.class);

    public static Clob createClob() {
        return (Clob) createLOB(Clob.class);
    }

    public static NClob createNClob() {
        return (NClob) createLOB(NClob.class);
    }

    public static Blob createBlob() {
        return (Blob) createLOB(Blob.class);
    }

    public static SQLXML createSQLXML() {
        return (SQLXML) createLOB(SQLXML.class);
    }

    private static Object createLOB(final Class pLOBType) {

        if (!DataSourceProvider.activeUserManagedTransaction()) {
            log.error(DataAccessException.Type.ISOLATED_LOB_CREATION.getDescription());
            throw new DataAccessException(DataAccessException.Type.ISOLATED_LOB_CREATION);
        }
        boolean success = true;
        OracleConnection con = null;
        try {
            con = DataSourceProvider.getConnection();
            if (pLOBType.equals(Clob.class)) {
                return con.createClob();
            }
            if (pLOBType.equals(NClob.class)) {
                return con.createNClob();
            }
            if (pLOBType.equals(Blob.class)) {
                return con.createBlob();
            }
            if (pLOBType.equals(SQLXML.class)) {
                return con.createSQLXML();
            }
            // Should not happen
            throw new DataAccessException(DataAccessException.Type.COULD_NOT_CREATE_LOB);
        } catch (Exception e) {
            log.error(DataAccessException.Type.COULD_NOT_CREATE_LOB.getDescription(), e);
            success = false;
            throw new DataAccessException(DataAccessException.Type.COULD_NOT_CREATE_LOB, e);
        } finally {
            try {
                if (con != null && !con.isClosed()) {
                    DataSourceProvider.returnConnection(success);
                }
            } catch (SQLException ignored) {
            } // Nothing to do
        }
    }
}
