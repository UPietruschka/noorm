package org.noorm.jdbc;

import org.noorm.jdbc.platform.IMetadata;
import org.noorm.jdbc.platform.PrimaryKeyColumn;
import org.noorm.jdbc.platform.TableMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.JDBCType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provisioning of standard JDBC metadata.
 *
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 07.04.14
 *         Time: 10:21
 */
public abstract class JDBCMetadata implements IMetadata {

    private static final Logger log = LoggerFactory.getLogger(JDBCQueryProcessor.class);

    /**
     * Returns the list of table/column metadata accessible for the authenticated database user.
     *
     * @param pTableSearchPattern a regular expression narrowing the set of table subject to metadata retrieval
     * @return the requested
     */
    public Map<String, List<TableMetadata>> findTableMetadata(final String pTableSearchPattern) {

        final Map<String, List<TableMetadata>> tableMetaDataMap = new HashMap<>();
        boolean success = true;
        Connection con = null;
        try {
            con = DataSourceProvider.getConnection();
            final DatabaseMetaData databaseMetaData = con.getMetaData();
            log.info("Retrieving JDBC metadata.");
            final ResultSet columns = databaseMetaData.getColumns(null, null, null, null);
            while (columns.next()) {
                final String tableName = columns.getString("TABLE_NAME");
                if (pTableSearchPattern != null && !tableName.matches(pTableSearchPattern)) {
                    continue;
                }
                final String columnName = columns.getString("COLUMN_NAME");
                final int dataType = columns.getInt("DATA_TYPE");
                final int columnSize = columns.getInt("COLUMN_SIZE");
                final int decimalDigits = columns.getInt("DECIMAL_DIGITS");
                final int nullable = columns.getInt("NULLABLE");
                final JDBCType jdbcType = JDBCType.valueOf(dataType);
                final boolean isNullable = nullable == DatabaseMetaData.columnNullable;
                log.debug("Retrieving JDBC database metadata for table/column : " + tableName + "/" + columnName
                        + "\n" + " JDBC type      : " + jdbcType.getName()
                        + "\n" + " Column Size    : " + columnSize
                        + "\n" + " Decimal Digits : " + decimalDigits
                        + "\n" + " Nullable       : " + isNullable);
                List<TableMetadata> tableMetadataList = tableMetaDataMap.get(tableName);
                if (tableMetadataList == null) {
                    tableMetadataList = new ArrayList<>();
                }
                final TableMetadata tableMetadata = new TableMetadata();
                tableMetadata.setTableName(tableName);
                tableMetadata.setColumnName(columnName);
                tableMetadata.setColumnSize(columnSize);
                tableMetadata.setDecimalDigits(decimalDigits);
                tableMetadata.setNullable(isNullable);
                tableMetadata.setUpdatable(true);
                tableMetadata.setJDBCType(jdbcType);
                tableMetadataList.add(tableMetadata);
                tableMetaDataMap.put(tableName, tableMetadataList);
            }
            if (tableMetaDataMap.isEmpty()) {
                log.info("No JDBC table metadata found.");
            } else {
                log.info("JDBC table metadata found for " + tableMetaDataMap.size() + " tables.");
            }
            return tableMetaDataMap;
        } catch (Exception e) {
            log.error(DataAccessException.Type.COULD_NOT_ACCESS_JDBC_METADATA.getDescription(), e);
            success = false;
            throw new DataAccessException(DataAccessException.Type.COULD_NOT_ACCESS_JDBC_METADATA, e);
        } finally {
            try {
                if (con != null && !con.isClosed()) {
                    DataSourceProvider.returnConnection(success);
                }
            } catch (SQLException ignored) {
            } // Nothing to do
        }
    }

    /**
     * Returns all primary key columns.
     *
     * @param pTableName the table name
     * @return the list of primary key columns for the given table
     */
    public List<PrimaryKeyColumn> findPkColumns(final String pTableName) {

        boolean success = true;
        Connection con = null;
        try {
            con = DataSourceProvider.getConnection();
            final DatabaseMetaData databaseMetaData = con.getMetaData();
            final ResultSet primaryKeys = databaseMetaData.getPrimaryKeys(null, null, pTableName);
            final List<PrimaryKeyColumn> primaryKeyColumnList = new ArrayList<>();
            log.info("Retrieving primary key metadata from JDBC database metadata.");
            while (primaryKeys.next()) {
                final String tableName = primaryKeys.getString("TABLE_NAME");
                final String columnName = primaryKeys.getString("COLUMN_NAME");
                log.debug("Found primary key metadata for table/column : " + tableName + "/" + columnName);
                final PrimaryKeyColumn primaryKeyColumn = new PrimaryKeyColumn();
                primaryKeyColumn.setTableName(tableName);
                primaryKeyColumn.setColumnName(columnName);
                primaryKeyColumnList.add(primaryKeyColumn);
            }
            return primaryKeyColumnList;
        } catch (Exception e) {
            log.error(DataAccessException.Type.COULD_NOT_ACCESS_JDBC_METADATA.getDescription(), e);
            success = false;
            throw new DataAccessException(DataAccessException.Type.COULD_NOT_ACCESS_JDBC_METADATA, e);
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
