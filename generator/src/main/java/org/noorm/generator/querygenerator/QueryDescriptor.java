package org.noorm.generator.querygenerator;

import org.noorm.generator.ParameterDescriptor;
import org.noorm.generator.m2plugin.QueryDeclaration;
import org.noorm.jdbc.QueryColumn;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 21.01.13
 *         Time: 19:53
 */
public class QueryDescriptor {

    private QueryDeclaration queryDeclaration;
    private final List<ParameterDescriptor> parameters = new ArrayList<ParameterDescriptor>();
    private String beanName;
    private String beanShortName;

    public QueryDeclaration getQueryDeclaration() {
        return queryDeclaration;
    }

    public void setQueryDeclaration(final QueryDeclaration pQueryDeclaration) {
        queryDeclaration = pQueryDeclaration;
    }

    public String getTableName() {
        return queryDeclaration.getTableName();
    }

    public String getMethodName() {
        return queryDeclaration.getMethodName();
    }

    public boolean isSingleRowQuery() {
        return queryDeclaration.isSingleRowQuery();
    }

    public List<QueryColumn> getQueryColumns() {
        return queryDeclaration.getQueryColumns();
    }

    public List<ParameterDescriptor> getParameters() {
        return parameters;
    }

    public void addParameter(final ParameterDescriptor pParameter) {
        parameters.add(pParameter);
    }

    public String getBeanName() {
        return beanName;
    }

    public void setBeanName(final String pBeanName) {
        beanName = pBeanName;
    }

    public String getBeanShortName() {
        return beanShortName;
    }

    public void setBeanShortName(final String pBeanShortName) {
        beanShortName = pBeanShortName;
    }
}
