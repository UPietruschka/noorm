package org.noorm.jdbc;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Helper class for JDBCProcedureProcessor and JDBCQueryProcessor for the textual creation of SQL statements.
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
	private static final String CALL_ASG2 = " => ?";
	private static final String CALL_POSTFIX = " }";

	public String buildProcedureCall(final String pCallable,
                                     final String pOutParamName,
                                     final Map<String, Object> pInParameters) {

		final StringBuilder call = new StringBuilder();
		call.append(CALL_PREFIX).append(pCallable);
		String delim = CALL_DELIM_1;
		if (pOutParamName != null) {
            call.append(delim).append(pOutParamName).append(CALL_ASG2);
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
                call.append(delim).append(paramName).append(CALL_ASG2);
				delim = CALL_DELIM_2;
			}
		}
		if (delim.equals(CALL_DELIM_2)) {
			// When no parameters are used, PL/SQL does not even expect empty brackets,
			// but nothing behind the callables name
			call.append(CALL_DELIM_3);
		}
		call.append(CALL_POSTFIX);
		return call.toString();
	}

    private static final String SELECT_PREFIX = "SELECT * FROM ";
    private static final String SELECT_LOCK = " FOR UPDATE";

    public String buildSQLStatement(final String pTableName,
                                    final Map<QueryColumn, Object> pInParameters,
                                    final boolean pUseNamedParameters,
                                    final boolean pAcquireLock) {

        final StringBuilder pSQLStatement = new StringBuilder();
        if (!pTableName.equals(pTableName.toUpperCase())) {
            pSQLStatement.append(SELECT_PREFIX).append("\"".concat(pTableName).concat("\""));
        } else {
            pSQLStatement.append(SELECT_PREFIX).append(pTableName);
        }
        if (pInParameters.size() > 0) {
            String delim = WHERE;
            final Map<QueryColumn, Object> orderedParameters = new TreeMap<QueryColumn, Object>(pInParameters);
            for (final QueryColumn queryColumn : orderedParameters.keySet()) {
                if (orderedParameters.get(queryColumn) != null || queryColumn.getOperator().isUnary()) {
                    pSQLStatement.append(delim);
                    pSQLStatement.append(queryColumn.getColumnName());
                    pSQLStatement.append(queryColumn.getOperator().getOperatorSyntax());
                    if (!queryColumn.getOperator().isUnary()) {
                        if (pUseNamedParameters) {
                            pSQLStatement.append(ASG).append(queryColumn.getColumnName());
                        } else {
                            pSQLStatement.append(ASG2);
                        }
                    }
                    delim = AND;
                }
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

	public String buildInsert(final IBean pBean, final Map pField2ParameterIndex, final boolean pUseNamedParameters) {

        final String tableName = pBean.getTableName();
        final String[] primaryKeyColumnNames = pBean.getPrimaryKeyColumnNames();
        final String sequenceName = pBean.getSequenceName();
        final boolean useInlineSequenceValueGeneration = pBean.useInlineSequenceValueGeneration();
        final Field[] fields = BeanMetaDataUtil.getDeclaredFieldsInclParent(pBean.getClass());
		final StringBuilder insert = new StringBuilder();
        if (pBean.isTableNameCaseSensitive()) {
            insert.append(INSERT_PREFIX).append("\"".concat(tableName).concat("\""));
        } else {
            insert.append(INSERT_PREFIX).append(tableName);
        }
		String delim = INSERT_DELIM_1;
        for (final Field field : fields) {
            final JDBCColumn colAnn = BeanMetaDataUtil.getJDBCColumnAnnotation(field);
            if (colAnn != null && colAnn.updatable()) {
                if (colAnn.caseSensitiveName()) {
                    insert.append(delim).append("\"".concat(colAnn.name()).concat("\""));
                } else {
                    insert.append(delim).append(colAnn.name());
                }
                delim = INSERT_DELIM_2;
            }
        }
		insert.append(INSERT_VALUES);
		delim = INSERT_DELIM_1;

        Integer parameterIndex = 1;
        for (final Field field : fields) {
            final JDBCColumn colAnn = BeanMetaDataUtil.getJDBCColumnAnnotation(field);
            if (colAnn != null && colAnn.updatable()) {
                insert.append(delim);
                boolean isPKColumn = false;
                for (final String pkColumnName : primaryKeyColumnNames) {
                    if (colAnn.name().equals(pkColumnName)) {
                        isPKColumn = true;
                    }
                }
                if (isPKColumn && sequenceName != null && !sequenceName.isEmpty() && useInlineSequenceValueGeneration) {
                    if (!sequenceName.equals(sequenceName.toUpperCase())) {
                        insert.append("\"".concat(sequenceName).concat("\"")).append(INSERT_NEXT_PK_VAL);
                    } else {
                        insert.append(sequenceName).append(INSERT_NEXT_PK_VAL);
                    }
                    delim = INSERT_DELIM_2;
                } else {
                    pField2ParameterIndex.put(colAnn.name(), parameterIndex++);
                    if (pUseNamedParameters) {
                        insert.append(ASG).append(colAnn.name());
                    } else {
                        insert.append(ASG2);
                    }
                    delim = INSERT_DELIM_2;
                }
            }
		}
		insert.append(INSERT_DELIM_3);
		return insert.toString();
	}

	private static final String UPDATE_PREFIX = "UPDATE ";
	private static final String UPDATE_DELIM_1 = " SET ";
	private static final String UPDATE_DELIM_2 = ",";

	public String buildUpdate(final IBean pBean,
                              final boolean pUseOptLockFullRowCompare,
                              final Map pField2ParameterIndex,
                              final boolean pUseNamedParameters) {

        final String tableName = pBean.getTableName();
        final String[] primaryKeyColumnNames = pBean.getPrimaryKeyColumnNames();
        final Field[] fields = BeanMetaDataUtil.getDeclaredFieldsInclParent(pBean.getClass());
		final StringBuilder update = new StringBuilder();
        if (pBean.isTableNameCaseSensitive()) {
            update.append(UPDATE_PREFIX).append("\"".concat(tableName).concat("\""));
        } else {
            update.append(UPDATE_PREFIX).append(tableName);
        }
		String delim = UPDATE_DELIM_1;

        Integer parameterIndex = 1;
        for (final Field field : fields) {
            final JDBCColumn colAnn = BeanMetaDataUtil.getJDBCColumnAnnotation(field);
            if (colAnn != null && colAnn.updatable()) {
                boolean isPKColumn = false;
                for (final String pkColumnName : primaryKeyColumnNames) {
                    if (colAnn.name().equals(pkColumnName)) {
                        isPKColumn = true;
                    }
                }
                if (!isPKColumn) {
                    String fieldName = colAnn.name();
                    if (colAnn.caseSensitiveName()) {
                        fieldName = "\"".concat(colAnn.name()).concat("\"");
                    }
                    pField2ParameterIndex.put(colAnn.name(), parameterIndex++);
                    update.append(delim).append(fieldName).append(EQUALS);
                    if (pUseNamedParameters) {
                        update.append(ASG).append(colAnn.name());
                    } else {
                        update.append(ASG2);
                    }
                    delim = UPDATE_DELIM_2;
                }
            }
		}
        buildWhereClause(pBean, update, pUseOptLockFullRowCompare,
                pField2ParameterIndex, parameterIndex, pUseNamedParameters);
		return update.toString();
	}

	private static final String DELETE_PREFIX = "DELETE FROM ";

	public String buildDelete(final IBean pBean,
                              final boolean pUseOptLockFullRowCompare,
                              final Map pField2ParameterIndex,
                              final boolean pUseNamedParameters) {

        final String tableName = pBean.getTableName();
		final StringBuilder delete = new StringBuilder();
        if (pBean.isTableNameCaseSensitive()) {
            delete.append(DELETE_PREFIX).append("\"".concat(tableName).concat("\""));
        } else {
            delete.append(DELETE_PREFIX).append(tableName);
        }
        Integer parameterIndex = 1;
        buildWhereClause(pBean, delete, pUseOptLockFullRowCompare,
                pField2ParameterIndex, parameterIndex, pUseNamedParameters);
		return delete.toString();
	}

    private static final String WHERE = " WHERE ";
    private static final String AND = " AND ";
    private static final String EQUALS = " = ";
    private static final String ASG = ":";
    private static final String ASG2 = "?";
    private static final String IS_NULL = " IS NULL ";

    private StringBuilder buildWhereClause(final IBean pBean,
                                           final StringBuilder pDML,
                                           final boolean pUseOptLockFullRowCompare,
                                           final Map pField2ParameterIndex,
                                           Integer pParameterIndex,
                                           final boolean pUseNamedParameters) {

        final String[] primaryKeyColumnNames = pBean.getPrimaryKeyColumnNames();
        final String versionColumnName = pBean.getVersionColumnName();
        final BeanMapper<IBean> mapper = BeanMapper.getInstance();
        final Map<String, Object> fieldMap = mapper.toMap(pBean);
        final Field[] fields = BeanMetaDataUtil.getDeclaredFieldsInclParent(pBean.getClass());
        final List<String> caseSensitiveFields = new ArrayList<String>();
        for (final Field field : fields) {
            final JDBCColumn colAnn = BeanMetaDataUtil.getJDBCColumnAnnotation(field);
            if (colAnn != null && colAnn.caseSensitiveName()) {
                caseSensitiveFields.add(colAnn.name());
            }
        }

        String delim = WHERE;
        for (final String pkColumnName : primaryKeyColumnNames) {
            pDML.append(delim);
            if (caseSensitiveFields.contains(pkColumnName)) {
                pDML.append("\"".concat(pkColumnName).concat("\""));
            } else {
                pDML.append(pkColumnName);
            }
            pDML.append(EQUALS);
            pField2ParameterIndex.put(pkColumnName, pParameterIndex++);
            if (pUseNamedParameters) {
                pDML.append(ASG).append(pkColumnName);
            } else {
                pDML.append(ASG2);
            }
            delim = AND;
        }
        if (versionColumnName != null && !versionColumnName.isEmpty()) {
            pDML.append(delim);
            if (caseSensitiveFields.contains(versionColumnName)) {
                pDML.append("\"".concat(versionColumnName).concat("\""));
            } else {
                pDML.append(versionColumnName);
            }
            pDML.append(EQUALS);
            final String parameterName = versionColumnName.concat(OLD_VERSION_APPENDIX);
            pField2ParameterIndex.put(parameterName, pParameterIndex++);
            if (pUseNamedParameters) {
                pDML.append(ASG).append(parameterName);
            } else {
                pDML.append(ASG2);
            }
        }
        if (pUseOptLockFullRowCompare) {
            for (final String fieldName : fieldMap.keySet()) {
                boolean isPKColumn = false;
                for (final String pkColumnName : primaryKeyColumnNames) {
                    if (fieldName.equals(pkColumnName)) {
                        isPKColumn = true;
                    }
                }
                if (!isPKColumn) {
                    pDML.append(delim);
                    if (caseSensitiveFields.contains(fieldName)) {
                        pDML.append("\"".concat(fieldName).concat("\""));
                    } else {
                        pDML.append(fieldName);
                    }
                    if (fieldMap.get(fieldName) != null) {
                        final String parameterName = fieldName.concat(OLD_VERSION_APPENDIX);
                        pField2ParameterIndex.put(parameterName, pParameterIndex++);
                        pDML.append(EQUALS);
                        if (pUseNamedParameters) {
                            pDML.append(ASG).append(parameterName);
                        } else {
                            pDML.append(ASG2);
                        }
                    } else {
                        pDML.append(IS_NULL);
                    }
                }
            }
        }
        return pDML;
    }
}
