package org.noorm.generator.beangenerator;

import org.noorm.generator.ParameterDescriptor;
import org.noorm.generator.schema.DeleteDeclaration;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 21.01.19
 *         Time: 19:51
 */
public class DeleteDescriptor {

    private DeleteDeclaration deleteDeclaration;
    private final List<ParameterDescriptor> parameters = new ArrayList<>();
    private String beanName;
    private String beanShortName;

    public DeleteDeclaration getDeleteDeclaration() {
        return deleteDeclaration;
    }

    public void setDeleteDeclaration(final DeleteDeclaration pDeleteDeclaration) {
        deleteDeclaration = pDeleteDeclaration;
    }

    public String getTableName() {
        return deleteDeclaration.getTableName();
    }

    public String getMethodName() {
        return deleteDeclaration.getGeneratedMethodName();
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
