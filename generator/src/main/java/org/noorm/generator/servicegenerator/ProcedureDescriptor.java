package org.noorm.generator.servicegenerator;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 16.05.11
 *         Time: 18:18
 */
public class ProcedureDescriptor {

	private String javaName;
	private String oracleName;
	private List<ParameterDescriptor> parameters = new ArrayList<ParameterDescriptor>();
	private String outParamOracleName;
	private String outParamJavaType;
	private boolean hasOutParam;
	private boolean isOutParamRefCursor = false;
	private boolean isOutParamScalar = false;
	private boolean isSingleRowFinder = false;
	private boolean isIdListFinder = false;

	public void setJavaName(final String pJavaName) {
		javaName = pJavaName;
	}

	public String getJavaName() {
		return javaName;
	}

	public String getOracleName() {
		return oracleName;
	}

	public void setOracleName(final String pOracleName) {
		oracleName = pOracleName;
	}

	public List<ParameterDescriptor> getParameters() {
		return parameters;
	}

	public void addParameter(final ParameterDescriptor pParameter) {
		parameters.add(pParameter);
	}

	public String getOutParamOracleName() {
		return outParamOracleName;
	}

	public void setOutParamOracleName(final String pOutParamOracleName) {
		outParamOracleName = pOutParamOracleName;
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

	public boolean isIdListFinder() {
		return isIdListFinder;
	}

	public void setIdListFinder(final boolean pIdListFinder) {
		isIdListFinder = pIdListFinder;
	}
}
