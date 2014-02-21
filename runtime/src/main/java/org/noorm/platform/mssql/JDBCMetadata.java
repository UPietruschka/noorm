package org.noorm.platform.mssql;

import org.noorm.metadata.beans.NameBean;
import org.noorm.metadata.beans.ParameterBean;
import org.noorm.metadata.beans.PrimaryKeyColumnBean;
import org.noorm.metadata.beans.SequenceBean;
import org.noorm.metadata.beans.TableMetadataBean;
import org.noorm.platform.IMetadata;

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
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the list of table/column metadata accessible for the authenticated database user.
     *
     * @return the requested
     */
    @Override
    public Map<String, List<TableMetadataBean>> findTableMetadata() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the list of packages of stored procedures subject to Java code generation.
     *
     * @param pSearchRegex a regular expression to filter the list of packages
     * @return the list of packages
     */
    @Override
    public List<NameBean> findPackageNames(String pSearchRegex) {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the list of procedures contained in the given package.
     *
     * @param pPackageName the package name
     * @return the list of procedures
     */
    @Override
    public List<NameBean> findProcedureNames(String pPackageName) {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns all accessible sequences.
     *
     * @return the list of sequence names
     */
    @Override
    public List<SequenceBean> findSequenceNames() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns all primary key columns.
     *
     * @return the list of primary key columns
     */
    @Override
    public List<PrimaryKeyColumnBean> findPkColumns() {
        throw new UnsupportedOperationException();
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
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the hash value for the source code of a given stored procedure package.
     *
     * @param pPackageName the package name
     * @return the has value
     */
    @Override
    public Integer getPackageHashValue(String pPackageName) {
        throw new UnsupportedOperationException();
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
        throw new UnsupportedOperationException();
    }

    /**
     * Returns metadata for a database type definition.
     *
     * @return the list of record metadata
     */
    @Override
    public Map<String, List<TableMetadataBean>> findRecordMetadata() {
        throw new UnsupportedOperationException();
    }
}
