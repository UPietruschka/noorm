package org.noorm.generator.beangenerator;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 07.06.12
 *         Time: 10:50
 */
public class BeanDMLClassDescriptor {

	private static String BEAN_DML_INTERFACE_NAME = "IBeanDML";
    private static final String BEAN_DML_CLASS_NAME = "BeanDML";

	private String packageName;
	private String interfacePackageName;
	private String beanPackageName;
	private final List<BeanClassDescriptor> beans = new ArrayList<BeanClassDescriptor>();
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

	public void addBean(final BeanClassDescriptor pBean) {
		beans.add(pBean);
	}

	public List<BeanClassDescriptor> getBeans() {
		return beans;
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
        return BEAN_DML_CLASS_NAME;
    }

	public String getJavaInterfaceName() {
		return BEAN_DML_INTERFACE_NAME;
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
