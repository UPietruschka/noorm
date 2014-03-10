package org.noorm.platform.mssql;

import org.noorm.jdbc.DataAccessException;
import org.noorm.jdbc.DataSourceProvider;
import org.noorm.jdbc.JDBCQueryProcessor;
import org.noorm.metadata.beans.NameBean;
import org.noorm.metadata.beans.ParameterBean;
import org.noorm.metadata.beans.PrimaryKeyColumnBean;
import org.noorm.metadata.beans.SequenceBean;
import org.noorm.metadata.beans.TableMetadataBean;
import org.noorm.platform.IMetadata;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Ulf Pietruschka / ulf.pietruschka@ext.secunet.com
 *         Date: 21.02.14
 *         Time: 13:44
 *         <p/>
 *         Project class implementation
 */
public class JDBCMetadata implements IMetadata {

    private JDBCQueryProcessor queryProcessor = JDBCQueryProcessor.getInstance();

    private static JDBCMetadata ourInstance = new JDBCMetadata();

    public static JDBCMetadata getInstance() {
        return ourInstance;
    }

    private JDBCMetadata() { }


    /**
     * Returns the version of the proprietary metadata retrieval implementation.
     *
     * @return the version
     */
    @Override
    public String getVersion() {
        return "1.1.0-SNAPSHOT";
    }

    /**
     * Returns the list of table/column metadata accessible for the authenticated database user.
     *
     * @return the requested
     */
    @Override
    public Map<String, List<TableMetadataBean>> findTableMetadata() {

        throw new UnsupportedOperationException();
//        final Map<String, List<TableMetadataBean>> tableMetaData = new HashMap<String, List<TableMetadataBean>>();
//        boolean success = true;
//        Connection con = null;
//        try {
//            con = DataSourceProvider.getConnection();
//            final DatabaseMetaData databaseMetaData = con.getMetaData();
//            final ResultSet columns = databaseMetaData.getColumns(null, "identa", null, null);
//            while (columns.next()) {
//                final String tableName = columns.getString("TABLE_NAME");
//                final String columnName = columns.getString("COLUMN_NAME");
//                final int dataType = columns.getInt("DATA_TYPE");
//                final String typeName = columns.getString("TYPE_NAME");
//                final int columnSize = columns.getInt("COLUMN_SIZE");
//                final int decimalDigits = columns.getInt("DECIMAL_DIGITS");
//                final int nullable = columns.getInt("NULLABLE");
//                final int charOctetLength = columns.getInt("CHAR_OCTET_LENGTH");
//                final String isNullable = columns.getString("IS_NULLABLE");
//                System.out.println(tableName + ":" + columnName + ":" + dataType + ":" + typeName + ":"
//                        + columnSize + ":" + decimalDigits + ":" + nullable + ":" + charOctetLength + ":"
//                        + isNullable);
//            }
//            return tableMetaData;
//        } catch (Exception e) {
//            success = false;
//            throw new DataAccessException(DataAccessException.Type.COULD_NOT_ACCESS_JDBC_METADATA, e);
//        } finally {
//            try {
//                if (con != null && !con.isClosed()) {
//                    DataSourceProvider.returnConnection(success);
//                }
//            } catch (SQLException ignored) {
//            } // Nothing to do
//        }
//
//
//
//
////        final String tableQuery = "SELECT object_id, name FROM sys.tables";
////        final List<Map<String, Object>> results = queryProcessor.executeGenericSelect(tableQuery);
////        for (final Map<String, Object> table : results) {
//            final String tableName = (String) table.get("name");
//            final Long objectId = (Long) table.get("object_id");
//            final String objectIdS = String.valueOf(objectId);
//            final String columnQuery = "SELECT name, column_id, system_type_id, max_length, precision, scale, is_nullable "
//                                     + "FROM sys.columns WHERE object_id = " + objectIdS;
//            final List<Map<String, Object>> results0 = queryProcessor.executeGenericSelect(columnQuery);
//            final List<TableMetadataBean> metadataBeanList = new ArrayList<TableMetadataBean>();
//            for (final Map<String, Object> column : results0) {
//                final TableMetadataBean metadataBean = new TableMetadataBean();
//                metadataBean.setTableName(tableName);
//                metadataBean.setColumnName((String) column.get("name"));
//                metadataBean.setColumnId((Long) column.get("column_id"));
//                metadataBean.setCharLength((Long) column.get("max_length"));
//                final Long sysTypeId = (Long) column.get("system_type_id");
//                String dataType = "VARCHAR2";
//                if (sysTypeId == 40) {
//                    dataType = "DATE";
//                }
//                if (sysTypeId == 41) {
//                    dataType = "TIMESTAMP";
//                }
//                if (sysTypeId == 42) {
//                    dataType = "TIMESTAMP";
//                }
//                if (sysTypeId == 48) {
//                    dataType = "NUMBER";
//                }
//                if (sysTypeId == 52) {
//                    dataType = "NUMBER";
//                }
//                if (sysTypeId == 56) {
//                    dataType = "NUMBER";
//                }
//                if (sysTypeId == 59) {
//                    dataType = "NUMBER";
//                }
//                if (sysTypeId == 62) {
//                    dataType = "NUMBER";
//                }
//                if (sysTypeId == 106) {
//                    dataType = "NUMBER";
//                }
//                if (sysTypeId == 108) {
//                    dataType = "NUMBER";
//                }
//                if (sysTypeId == 175) {
//                    dataType = "CHAR";
//                }
//                if (sysTypeId == 189) {
//                    dataType = "TIMESTAMP";
//                }
//                metadataBean.setDataType(dataType);
//                metadataBean.setDataScale((Long) column.get("scale"));
//                metadataBean.setDataPrecision((Long) column.get("precision"));
//                String nullable = "YES";
//                if (((Long) column.get("scale")) == 0) {
//                    nullable = "NO";
//                }
//                metadataBean.setNullable(nullable);
//                metadataBean.setInsertable("YES");
//                metadataBean.setUpdatable("YES");
//                metadataBeanList.add(metadataBean);
//            }
//            tableMetaData.put(tableName, metadataBeanList);
//        }
//        return tableMetaData;
    }

    /**
     * Returns the list of packages of stored procedures subject to Java code generation.
     *
     * @param pSearchRegex a regular expression to filter the list of packages
     * @return the list of packages
     */
    @Override
    public List<NameBean> findPackageNames(String pSearchRegex) {

        final List<NameBean> packageNames = new ArrayList<NameBean>();
        return packageNames;
    }

    /**
     * Returns the list of procedures contained in the given package.
     *
     * @param pPackageName the package name
     * @return the list of procedures
     */
    @Override
    public List<NameBean> findProcedureNames(String pPackageName) {

        final List<NameBean> procedureNames = new ArrayList<NameBean>();
        return procedureNames;
    }

    /**
     * Returns all accessible sequences.
     *
     * @return the list of sequence names
     */
    @Override
    public List<SequenceBean> findSequenceNames() {

        final String sequenceQuery = "SELECT name, CONVERT(int, increment) FROM sys.sequences";
        final List<Map<String, Object>> results = queryProcessor.executeGenericSelect(sequenceQuery);
        final List<SequenceBean> sequences = new ArrayList<SequenceBean>();
        for (final Map<String, Object> sequence : results) {
            final SequenceBean sequenceBean = new SequenceBean();
            sequenceBean.setName((String) sequence.get("name"));
            sequenceBean.setIncrementBy((Long) sequence.get("increment"));
            sequences.add(sequenceBean);
        }
        return sequences;
    }

    /**
     * Returns all primary key columns.
     *
     * @return the list of primary key columns
     */
    @Override
    public List<PrimaryKeyColumnBean> findPkColumns() {

        final List<PrimaryKeyColumnBean> pkColumns = new ArrayList<PrimaryKeyColumnBean>();
        return pkColumns;
    }

    /**
     * Returns the parameters for a given stored procedure.
     *
     * @param pPackageName   the package name
     * @param pProcedureName the procedure name
     * @return the list of parameters for the given procedure
     */
    @Override
    public List<ParameterBean> findProcedureParameters(String pPackageName, String pProcedureName) {

        final List<ParameterBean> parameterBeans = new ArrayList<ParameterBean>();
        return parameterBeans;
    }

    /**
     * Returns the hash value for the source code of a given stored procedure package.
     *
     * @param pPackageName the package name
     * @return the has value
     */
    @Override
    public Integer getPackageHashValue(String pPackageName) {
        return 0;
    }

    /**
     * Returns the type for a given parameter of a stored procedure.
     *
     * @param pPackageName   the package name
     * @param pProcedureName the procedure name
     * @param pParameterName the parameter name
     * @return the type
     */
    @Override
    public String getParameterRowtype(String pPackageName, String pProcedureName, String pParameterName) {

        return "Unknown";
    }

    /**
     * Returns metadata for a database type definition.
     *
     * @return the list of record metadata
     */
    @Override
    public Map<String, List<TableMetadataBean>> findRecordMetadata() {

        final Map<String, List<TableMetadataBean>> recordMetadata = new HashMap<String, List<TableMetadataBean>>();
        return recordMetadata;
    }
}
