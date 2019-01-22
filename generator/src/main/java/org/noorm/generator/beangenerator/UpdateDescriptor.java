package org.noorm.generator.beangenerator;

import org.noorm.generator.ParameterDescriptor;
import org.noorm.generator.SearchDescriptor;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 21.01.19
 *         Time: 19:51
 */
public class UpdateDescriptor extends SearchDescriptor {

    private final List<ParameterDescriptor> updateParameters = new ArrayList<>();

    public List<ParameterDescriptor> getUpdateParameters() {
        return updateParameters;
    }

    public void addUpdateParameter(final ParameterDescriptor pUpdateParameter) {
        updateParameters.add(pUpdateParameter);
    }
}
