package org.noorm.generator.servicegenerator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 16.05.11
 *         Time: 18:12
 */
public class ServiceClassDescriptor {

	private static final String INTERFACE_PREFIX = "I";

	private int codeHashValue;
	private String javaName;
	private String databasePackageName;
	private String packageName;
	private String interfacePackageName;
	private String beanPackageName;
	private final List<ProcedureDescriptor> procedures = new ArrayList<ProcedureDescriptor>();
	private final Map<String, String> returnTypeNames = new HashMap<String, String>();
	private boolean isInterface = false;

	public int getCodeHashValue() {
		return codeHashValue;
	}

	public void setCodeHashValue(final int pCodeHashValue) {

		codeHashValue = pCodeHashValue;
	}

	public void setJavaName(final String pJavaName) {
		javaName = pJavaName;
	}

	public String getJavaName() {
		return javaName;
	}

	public String getJavaInterfaceName() {
		return INTERFACE_PREFIX.concat(javaName);
	}

	public String getDatabasePackageName() {
		return databasePackageName;
	}

	public void setDatabasePackageName(final String pDatabasePackageName) {
		databasePackageName = pDatabasePackageName;
	}

	public String getPackageName() {
		return packageName;
	}

	public void setPackageName(final String pPackageName) {
		packageName = pPackageName;
	}

	public String getBeanPackageName() {
		return beanPackageName;
	}

	public void setBeanPackageName(final String pBeanPackageName) {
		beanPackageName = pBeanPackageName;
	}

	public void addProcedure(final ProcedureDescriptor pProcedure) {
		procedures.add(pProcedure);
	}

	public List<ProcedureDescriptor> getProcedures() {
		return procedures;
	}

	public void addReturnTypeName(final String pReturnTypeShortName, final String pReturnTypeBeanName) {
		returnTypeNames.put(pReturnTypeShortName, pReturnTypeBeanName);
	}

	public Set<String> getReturnTypeShortNames() {
		return returnTypeNames.keySet();
	}

	public String getReturnTypeBeanName(final String pReturnTypeShortName) {
		return returnTypeNames.get(pReturnTypeShortName);
	}

	public String getFirstLowerName() {
		return javaName.toLowerCase().substring(0, 1).concat(javaName.substring(1));
	}

	public String getInterfacePackageName() {
		return interfacePackageName;
	}

	public void setInterfacePackageName(final String pInterfacePackageName) {
		interfacePackageName = pInterfacePackageName;
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
}
