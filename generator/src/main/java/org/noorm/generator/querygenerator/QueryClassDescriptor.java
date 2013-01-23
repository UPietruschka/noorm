package org.noorm.generator.querygenerator;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 21.01.13
 *         Time: 19:52
 */
public class QueryClassDescriptor {

    private static String DECLARED_QUERIES_INTERFACE_NAME = "IDeclaredQueries";
    private static final String DECLARED_QUERIES_CLASS_NAME = "DeclaredQueries";

    private String packageName;
    private String interfacePackageName;
    private String beanPackageName;
    private final List<QueryDescriptor> queries = new ArrayList<QueryDescriptor>();
    private boolean isInterface = false;
    private String dataSourceName;

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(final String pPackageName) {
        packageName = pPackageName;
    }

    public String getInterfacePackageName() {
        return interfacePackageName;
    }

    public void setInterfacePackageName(final String pInterfacePackageName) {
        interfacePackageName = pInterfacePackageName;
    }

    public String getBeanPackageName() {
        return beanPackageName;
    }

    public void setBeanPackageName(final String pBeanPackageName) {
        beanPackageName = pBeanPackageName;
    }

    public void addQuery(final QueryDescriptor pQuery) {
        queries.add(pQuery);
    }

    public List<QueryDescriptor> getQueries() {
        return queries;
    }

    public boolean isInterface() {
        return isInterface;
    }

    public void setInterface(final boolean pInterface) {
        isInterface = pInterface;
    }

    public boolean hasInterface() {
        return interfacePackageName != null && !interfacePackageName.isEmpty();
    }

    public String getJavaName() {
        return DECLARED_QUERIES_CLASS_NAME;
    }

    public String getJavaInterfaceName() {
        return DECLARED_QUERIES_INTERFACE_NAME;
    }

    public String getDataSourceName() {
        return dataSourceName;
    }

    public void setDataSourceName(final String pDataSourceName) {
        dataSourceName = pDataSourceName;
    }

    public boolean hasDataSourceName() {
        return dataSourceName != null && !dataSourceName.isEmpty();
    }
}
