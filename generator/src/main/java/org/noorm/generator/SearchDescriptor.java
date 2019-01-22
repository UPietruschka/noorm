package org.noorm.generator;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 21.01.19
 *         Time: 19:51
 */
public class SearchDescriptor {

    protected ISearchDeclaration searchDeclaration;
    protected final List<ParameterDescriptor> parameters = new ArrayList<>();
    protected String beanName;

    public ISearchDeclaration getSearchDeclaration() {
        return searchDeclaration;
    }

    public void setSearchDeclaration(final ISearchDeclaration pSearchDeclaration) {
        searchDeclaration = pSearchDeclaration;
    }

    public String getTableName() {
        return searchDeclaration.getTableName();
    }

    public String getMethodName() {
        return searchDeclaration.getGeneratedMethodName();
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
}
