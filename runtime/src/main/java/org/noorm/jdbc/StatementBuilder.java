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

		final StringBuilder plSQLCall = new StringBuilder();
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
			final Map<String, Object> orderedParameters = new TreeMap<String, Object>(pInParameters);
			for (final String paramName : orderedParameters.keySet()) {
				final Object value = orderedParameters.get(paramName);
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

    private static final String SELECT_PREFIX = "SELECT * FROM ";
    private static final String SELECT_ASG = "?";
    private static final String SELECT_LOCK = " FOR UPDATE";

    public String buildSQLStatement(final String pTableName,
                                    final Map<QueryColumn, Object> pInParameters,
                                    final boolean pUseNamedParameters,
                                    final boolean pAcquireLock) {

        final StringBuilder pSQLStatement = new StringBuilder();
        pSQLStatement.append(SELECT_PREFIX).append(pTableName);
        if (pInParameters.size() > 0) {
            String delim = WHERE;
            final Map<QueryColumn, Object> orderedParameters = new TreeMap<QueryColumn, Object>(pInParameters);
            for (final QueryColumn queryColumn : orderedParameters.keySet()) {
                pSQLStatement.append(delim);
                pSQLStatement.append(queryColumn.getColumnName());
                pSQLStatement.append(queryColumn.getOperator().getOperatorSyntax());
                if (!queryColumn.getOperator().isUnary()) {
                    if (pUseNamedParameters) {
                        pSQLStatement.append(ASG).append(queryColumn.getColumnName());
                    } else {
                        pSQLStatement.append(SELECT_ASG);
                    }
                }
                delim = AND;
            }
        }
        if (pAcquireLock) {
            pSQLStatement.append(SELECT_LOCK);
        }
        return pSQLStatement.toString();
    }

	private static final String INSERT_PREFIX = "INSERT INTO ";
	private static final String INSERT_DELIM_1 = " (";
	private static final String INSERT_DELIM_2 = ",";
	private static final String INSERT_VALUES = ") VALUES ";
	private static final String INSERT_NEXT_PK_VAL = ".NEXTVAL";
	private static final String INSERT_DELIM_3 = ")";

	public String buildInsert(final String pTableName,
							  final String[] pPrimaryKeyColumnNames,
							  final String pSequenceName,
							  final Map<String, Object> fieldMap) {

		final StringBuilder insert = new StringBuilder();
		insert.append(INSERT_PREFIX).append(pTableName);
		String delim = INSERT_DELIM_1;
		for (final String fieldName : fieldMap.keySet()) {
			insert.append(delim).append(fieldName);
			delim = INSERT_DELIM_2;
		}
		insert.append(INSERT_VALUES);
		delim = INSERT_DELIM_1;
		for (final String fieldName : fieldMap.keySet()) {
			insert.append(delim);
			boolean isPKColumn = false;
			for (final String pkColumnName : pPrimaryKeyColumnNames) {
				if (fieldName.equals(pkColumnName)) {
					isPKColumn = true;
				}
			}
			if (isPKColumn && pSequenceName != null && !pSequenceName.isEmpty()) {
				insert.append(pSequenceName).append(INSERT_NEXT_PK_VAL);
				delim = INSERT_DELIM_2;
			} else {
				insert.append(ASG).append(fieldName);
				delim = INSERT_DELIM_2;
			}
		}
		insert.append(INSERT_DELIM_3);
		return insert.toString();
	}

	private static final String UPDATE_PREFIX = "UPDATE ";
	private static final String UPDATE_DELIM_1 = " SET ";
	private static final String UPDATE_DELIM_2 = ",";

	public String buildUpdate(final String pTableName,
							  final String[] pPrimaryKeyColumnNames,
							  final String pVersionColumnName,
							  final Map<String, Object> fieldMap,
                              final boolean pUseOptLockFullRowCompare) {

		final StringBuilder update = new StringBuilder();
		update.append(UPDATE_PREFIX).append(pTableName);
		String delim = UPDATE_DELIM_1;
		for (final String fieldName : fieldMap.keySet()) {
			boolean isPKColumn = false;
			for (final String pkColumnName : pPrimaryKeyColumnNames) {
				if (fieldName.equals(pkColumnName)) {
					isPKColumn = true;
				}
			}
			if (!isPKColumn) {
				update.append(delim).append(fieldName).append(EQUALS).append(ASG).append(fieldName);
				delim = UPDATE_DELIM_2;
			}
		}
        buildWhereClause(update, pPrimaryKeyColumnNames, pVersionColumnName, fieldMap, pUseOptLockFullRowCompare);
		return update.toString();
	}

	private static final String DELETE_PREFIX = "DELETE FROM ";

	public String buildDelete(final String pTableName,
							  final String[] pPrimaryKeyColumnNames,
							  final String pVersionColumnName,
                              final Map<String, Object> fieldMap,
                              final boolean pUseOptLockFullRowCompare) {

		final StringBuilder delete = new StringBuilder();
		delete.append(DELETE_PREFIX).append(pTableName);
        buildWhereClause(delete, pPrimaryKeyColumnNames, pVersionColumnName, fieldMap, pUseOptLockFullRowCompare);
		return delete.toString();
	}

    private static final String WHERE = " WHERE ";
    private static final String AND = " AND ";
    private static final String EQUALS = " = ";
    private static final String ASG = ":";
    private static final String IS_NULL = " IS NULL ";

    private StringBuilder buildWhereClause(final StringBuilder pDML,
                                           final String[] pPrimaryKeyColumnNames,
                                           final String pVersionColumnName,
                                           final Map<String, Object> fieldMap,
                                           final boolean pUseOptLockFullRowCompare) {

        String delim = WHERE;
        for (final String pkColumnName : pPrimaryKeyColumnNames) {
            pDML.append(delim);
            pDML.append(pkColumnName);
            pDML.append(EQUALS);
            pDML.append(ASG).append(pkColumnName);
            delim = AND;
        }
        if (pVersionColumnName != null && !pVersionColumnName.isEmpty()) {
            pDML.append(delim);
            pDML.append(pVersionColumnName);
            pDML.append(EQUALS);
            pDML.append(ASG).append(pVersionColumnName).append(OLD_VERSION_APPENDIX);
        }
        if (pUseOptLockFullRowCompare) {
            for (final String fieldName : fieldMap.keySet()) {
                boolean isPKColumn = false;
                for (final String pkColumnName : pPrimaryKeyColumnNames) {
                    if (fieldName.equals(pkColumnName)) {
                        isPKColumn = true;
                    }
                }
                if (!isPKColumn) {
                    pDML.append(delim).append(fieldName);
                    if (fieldMap.get(fieldName) != null) {
                        pDML.append(EQUALS).append(ASG).append(fieldName).append(OLD_VERSION_APPENDIX);
                    } else {
                        pDML.append(IS_NULL);
                    }
                }
            }
        }
        return pDML;
    }
}
