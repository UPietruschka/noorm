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
     * Resolves the JDBC datatype on basis of the platform specific type information found in JDBC metadata
     *
     * @param pDataType      the numeric data type code
     * @param pDataTypeName  the data type name
     * @param pDecimalDigits the number of decimal digits for non-integer like numeric types
     * @return the mapped JDBC standard type
     */
    @Override
    public JDBCType findJDBCType(int pDataType, String pDataTypeName, int pDecimalDigits) {

        final JDBCType jdbcType = JDBCType.valueOf(pDataType);
        return jdbcType;
    }

    /**
     * Returns the list of table/column metadata accessible for the authenticated database user.
     *
     * @param pSchemaPattern a regular expression narrowing the set of schemas subject to metadata retrieval
     * @param pTableNamePattern a regular expression narrowing the set of tables subject to metadata retrieval
     * @return the requested
     */
    @Override
    public Map<String, List<TableMetadata>> findTableMetadata(final String pSchemaPattern,
                                                              final String pTableNamePattern) {

        final Map<String, List<TableMetadata>> tableMetaDataMap = new HashMap<>();
        boolean success = true;
        Connection con = null;
        try {
            con = DataSourceProvider.getConnection();
            final DatabaseMetaData databaseMetaData = con.getMetaData();
            log.info("Retrieving JDBC metadata.");
            final ResultSet columns = databaseMetaData.getColumns
                    (null, pSchemaPattern, null, null);
            while (columns.next()) {
                final String tableName = columns.getString("TABLE_NAME");
                if (pTableNamePattern != null && !tableName.matches(pTableNamePattern)) {
                    continue;
                }
                final String columnName = columns.getString("COLUMN_NAME");
                final int dataType = columns.getInt("DATA_TYPE");
                final String typeName = columns.getString("TYPE_NAME");
                final int columnSize = columns.getInt("COLUMN_SIZE");
                final int decimalDigits = columns.getInt("DECIMAL_DIGITS");
                final int nullable = columns.getInt("NULLABLE");
                final JDBCType jdbcType = findJDBCType(dataType, typeName, decimalDigits);
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
                tableMetadata.setJDBCType(jdbcType);
                tableMetadataList.add(tableMetadata);
                tableMetaDataMap.put(tableName, tableMetadataList);
            }
            if (tableMetaDataMap.isEmpty()) {
                log.error("No JDBC table metadata found for schema pattern '{}' and table name pattern '{}'",
                        pSchemaPattern, pTableNamePattern);
                throw new DataAccessException(DataAccessException.Type.REFERENCED_JDBC_METADATA_NOT_FOUND);
            } else {
                log.info("JDBC table metadata found for " + tableMetaDataMap.size() + " tables.");
            }
            return tableMetaDataMap;
        } catch (Exception e) {
            if (e instanceof DataAccessException) {
                throw ((DataAccessException) e);
            }
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
