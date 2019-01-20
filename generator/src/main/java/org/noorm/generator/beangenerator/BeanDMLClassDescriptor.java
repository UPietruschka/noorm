package org.noorm.generator.beangenerator;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 07.06.12
 *         Time: 10:50
 */
public class BeanDMLClassDescriptor {

	private String name;
	private String javaName;
	private String extendedName;
	private String packageName;
	private String interfacePackageName;
	private String beanPackageName;
	private final List<BeanClassDescriptor> beans = new ArrayList<>();
	private final List<DeleteDescriptor> deletes = new ArrayList<>();
	private boolean isInterface = false;
    private String dataSourceName;

	public String getName() {
		return name;
	}

	public void setName(final String pName) {
		name = pName;
	}

	public String getExtendedName() {
		return extendedName;
	}

	public void setExtendedName(final String pExtendedName) {
		extendedName = pExtendedName;
	}

	public String getDMLName() {
		if (extendedName != null && !extendedName.isEmpty()) {
			return extendedName;
		}
		return name;
	}

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

	public void addDelete(final DeleteDescriptor pDelete) {
		deletes.add(pDelete);
	}

	public List<DeleteDescriptor> getDeletes() {
		return deletes;
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

	public void setJavaName(final String pJavaName) {
        javaName = pJavaName;
    }

    public String getJavaName() {
        return javaName;
    }

    public String getFirstLowerName() {
        return javaName.toLowerCase().substring(0, 1).concat(javaName.substring(1));
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
