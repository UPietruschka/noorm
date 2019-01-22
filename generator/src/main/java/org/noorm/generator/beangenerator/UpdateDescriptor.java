package org.noorm.generator.beangenerator;

import org.noorm.generator.ParameterDescriptor;
import org.noorm.generator.schema.UpdateDeclaration;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 21.01.19
 *         Time: 19:51
 */
public class UpdateDescriptor {

    private UpdateDeclaration updateDeclaration;
    private final List<ParameterDescriptor> updateParameters = new ArrayList<>();
    private final List<ParameterDescriptor> parameters = new ArrayList<>();
    private String beanName;
    private String beanShortName;

    public UpdateDeclaration getUpdateDeclaration() {
        return updateDeclaration;
    }

    public void setUpdateDeclaration(final UpdateDeclaration pUpdateDeclaration) {
        updateDeclaration = pUpdateDeclaration;
    }

    public String getTableName() {
        return updateDeclaration.getTableName();
    }

    public String getMethodName() {
        return updateDeclaration.getGeneratedMethodName();
    }

    public List<ParameterDescriptor> getUpdateParameters() {
        return updateParameters;
    }

    public void addUpdateParameter(final ParameterDescriptor pUpdateParameter) {
        updateParameters.add(pUpdateParameter);
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
