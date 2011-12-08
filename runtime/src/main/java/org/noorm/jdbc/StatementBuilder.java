package org.noorm.jdbc;

import java.util.Map;
import java.util.TreeMap;

/**
 * Helper class for JDBCStatementProcessor for the textual creation of SQL statements.
 *
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 02.06.11
 *         Time: 16:31
 */
class StatementBuilder {

	public static final String OLD_VERSION_APPENDIX = "_OLD";

	private static final String CALL_PREFIX = "{ call ";
	private static final String CALL_DELIM_1 = "(";
	private static final String CALL_DELIM_2 = ",";
	private static final String CALL_DELIM_3 = ")";
	private static final String CALL_PLSQL_ASG = " => :";
	private static final String CALL_PLSQL_ASG2 = " => ?";
	private static final String CALL_POSTFIX = " }";

	public String buildPLSQLCall(final String pPLSQLCallable,
								 final String pOutParamName,
								 final Map<String, Object> pInParameters,
								 final boolean useNamedParameters) {

		StringBuilder plSQLCall = new StringBuilder();
		plSQLCall.append(CALL_PREFIX).append(pPLSQLCallable);
		String delim = CALL_DELIM_1;
		if (pOutParamName != null) {
			if (useNamedParameters) {
				plSQLCall.append(delim).append(pOutParamName).append(CALL_PLSQL_ASG).append(pOutParamName);
			} else {
				plSQLCall.append(delim).append(pOutParamName).append(CALL_PLSQL_ASG2);
			}
			delim = CALL_DELIM_2;
		}
		if (pInParameters != null) {
			Map<String, Object> orderedParameters = new TreeMap<String, Object>(pInParameters);
			for (String paramName : orderedParameters.keySet()) {
				Object value = orderedParameters.get(paramName);
				if (value == null) {
					continue;
				}
				if (value instanceof String) {
					if ((value).equals("")) {
						continue;
					}
				}
				if (value instanceof byte[]) {
					if (((byte[]) value).length == 0) {
						continue;
					}
				}
				if (useNamedParameters) {
					plSQLCall.append(delim).append(paramName).append(CALL_PLSQL_ASG).append(paramName);
				} else {
					plSQLCall.append(delim).append(paramName).append(CALL_PLSQL_ASG2);
				}
				delim = CALL_DELIM_2;
			}
		}
		if (delim.equals(CALL_DELIM_2)) {
			// When no parameters are used, PL/SQL does not even expect empty brackets,
			// but nothing behind the callables name
			plSQLCall.append(CALL_DELIM_3);
		}
		plSQLCall.append(CALL_POSTFIX);
		return plSQLCall.toString();
	}

	private static final String INSERT_PREFIX = "INSERT INTO ";
	private static final String INSERT_DELIM_1 = " (";
	private static final String INSERT_DELIM_2 = ",";
	private static final String INSERT_VALUES = ") VALUES ";
	private static final String INSERT_ASG = ":";
	private static final String INSERT_NEXT_PK_VAL = ".NEXTVAL";
	private static final String INSERT_DELIM_3 = ")";

	public String buildInsert(final String pTableName,
							  final String pPrimaryKeyColumnName,
							  final String pSequenceName,
							  final Map<String, Object> fieldMap) {

		StringBuilder insert = new StringBuilder();
		insert.append(INSERT_PREFIX).append(pTableName);
		String delim = INSERT_DELIM_1;
		for (final String fieldName : fieldMap.keySet()) {
			if (!fieldName.toUpperCase().equals(pPrimaryKeyColumnName) || pSequenceName != null) {
				insert.append(delim).append(fieldName);
				delim = INSERT_DELIM_2;
			}
		}
		insert.append(INSERT_VALUES);
		delim = INSERT_DELIM_1;
		for (final String fieldName : fieldMap.keySet()) {
			insert.append(delim);
			if (fieldName.toUpperCase().equals(pPrimaryKeyColumnName) &&
					pSequenceName != null && !pSequenceName.isEmpty()) {
				insert.append(pSequenceName).append(INSERT_NEXT_PK_VAL);
				delim = INSERT_DELIM_2;
			} else {
				insert.append(INSERT_ASG).append(fieldName);
				delim = INSERT_DELIM_2;
			}
		}
		insert.append(INSERT_DELIM_3);
		return insert.toString();
	}

	private static final String UPDATE_PREFIX = "UPDATE ";
	private static final String UPDATE_DELIM_1 = " SET ";
	private static final String UPDATE_DELIM_2 = ",";
	private static final String UPDATE_WHERE = " WHERE ";
	private static final String UPDATE_AND = " AND ";
	private static final String UPDATE_EQUALS = " = ";
	private static final String UPDATE_ASG = ":";

	public String buildUpdate(final String pTableName,
							  final String pPrimaryKeyColumnName,
							  final String pVersionColumnName,
							  final Map<String, Object> fieldMap) {

		StringBuilder update = new StringBuilder();
		update.append(UPDATE_PREFIX).append(pTableName);
		String delim = UPDATE_DELIM_1;
		for (final String fieldName : fieldMap.keySet()) {
			if (!fieldName.equals(pPrimaryKeyColumnName)) {
				update.append(delim).append(fieldName).append(UPDATE_EQUALS).append(INSERT_ASG).append(fieldName);
				delim = UPDATE_DELIM_2;
			}
		}
		update.append(UPDATE_WHERE);
		update.append(pPrimaryKeyColumnName);
		update.append(UPDATE_EQUALS);
		update.append(UPDATE_ASG).append(pPrimaryKeyColumnName);
		if (pVersionColumnName != null && !pVersionColumnName.isEmpty()) {
			update.append(UPDATE_AND);
			update.append(pVersionColumnName);
			update.append(UPDATE_EQUALS);
			update.append(UPDATE_ASG).append(pVersionColumnName).append(OLD_VERSION_APPENDIX);
		}
		return update.toString();
	}

	private static final String DELETE_PREFIX = "DELETE FROM ";
	private static final String DELETE_WHERE = " WHERE ";
	private static final String DELETE_AND = " AND ";
	private static final String DELETE_EQUALS = " = ";
	private static final String DELETE_ASG = ":";

	public String buildDelete(final String pTableName,
							  final String pPrimaryKeyColumnName,
							  final String pVersionColumnName) {

		StringBuilder delete = new StringBuilder();
		delete.append(DELETE_PREFIX).append(pTableName);
		delete.append(DELETE_WHERE);
		delete.append(pPrimaryKeyColumnName);
		delete.append(DELETE_EQUALS);
		delete.append(DELETE_ASG).append(pPrimaryKeyColumnName);
		if (pVersionColumnName != null && !pVersionColumnName.isEmpty()) {
			delete.append(DELETE_AND);
			delete.append(pVersionColumnName);
			delete.append(DELETE_EQUALS);
			delete.append(DELETE_ASG).append(pVersionColumnName).append(OLD_VERSION_APPENDIX);
		}
		return delete.toString();
	}
}
