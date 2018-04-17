package org.noorm.generator.servicegenerator;

import org.noorm.generator.ParameterDescriptor;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 16.05.11
 *         Time: 18:18
 */
public class ProcedureDescriptor {

	private String javaName;
	private String dbProcedureName;
	private final List<ParameterDescriptor> parameters = new ArrayList<ParameterDescriptor>();
	private String outDbParamName;
	private String outParamJavaType;
	private boolean hasOutParam;
	private boolean isOutParamRefCursor = false;
	private boolean isOutParamScalar = false;
	private boolean isSingleRowFinder = false;

	public void setJavaName(final String pJavaName) {
		javaName = pJavaName;
	}

	public String getJavaName() {
		return javaName;
	}

	public String getDbProcedureName() {
		return dbProcedureName;
	}

	public void setDbProcedureName(final String pDbProcedureName) {
		dbProcedureName = pDbProcedureName;
	}

	public List<ParameterDescriptor> getParameters() {
		return parameters;
	}

	public void addParameter(final ParameterDescriptor pParameter) {
		parameters.add(pParameter);
	}

	public String getOutDbParamName() {
		return outDbParamName;
	}

	public void setOutDbParamName(final String pOutDbParamName) {
		outDbParamName = pOutDbParamName;
	}

	public String getOutParamJavaType() {
		return outParamJavaType;
	}

	public void setOutParamJavaType(final String pOutParamJavaType) {
		outParamJavaType = pOutParamJavaType;
	}

	public boolean hasOutParam() {
		return hasOutParam;
	}

	public void setHasOutParam(final boolean pHasOutParam) {
		hasOutParam = pHasOutParam;
	}

	public boolean isOutParamRefCursor() {
		return isOutParamRefCursor;
	}

	public void setOutParamRefCursor(final boolean pOutParamRefCursor) {
		isOutParamRefCursor = pOutParamRefCursor;
	}

	public boolean isOutParamScalar() {
		return isOutParamScalar;
	}

	public void setOutParamScalar(final boolean pOutParamScalar) {
		isOutParamScalar = pOutParamScalar;
	}

	public boolean isSingleRowFinder() {
		return isSingleRowFinder;
	}

	public void setSingleRowFinder(final boolean pSingleRowFinder) {
		isSingleRowFinder = pSingleRowFinder;
	}
}
