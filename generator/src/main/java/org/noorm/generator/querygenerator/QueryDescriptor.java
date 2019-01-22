package org.noorm.generator.querygenerator;

import org.noorm.generator.ParameterDescriptor;
import org.noorm.generator.SearchDescriptor;
import org.noorm.generator.schema.QueryDeclaration;

import java.util.List;

/**
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 21.01.13
 *         Time: 19:53
 */
public class QueryDescriptor extends SearchDescriptor {

    public boolean isSingleRowQuery() {
        return ((QueryDeclaration) searchDeclaration).isSingleRowQuery();
    }

    public boolean useFilterExtension() {
        return ((QueryDeclaration) searchDeclaration).isUseFilterExtension();
    }

    public boolean isAcquireLock() {
        return ((QueryDeclaration) searchDeclaration).isAcquireLock();
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
