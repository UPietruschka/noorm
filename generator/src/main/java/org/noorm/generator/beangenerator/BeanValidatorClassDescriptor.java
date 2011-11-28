package org.noorm.generator.beangenerator;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 28.11.11
 *         Time: 17:00
 */
public class BeanValidatorClassDescriptor {

	private String packageName;
	private List<String> beanClassNames = new ArrayList<String>();

	public String getPackageName() {
		return packageName;
	}

	public void setPackageName(final String pPackageName) {
		packageName = pPackageName;
	}

	public List<String> getBeanClassNames() {
		return beanClassNames;
	}

	public void setBeanClassNames(final List<String> pBeanClassNames) {
		beanClassNames = pBeanClassNames;
	}
}
