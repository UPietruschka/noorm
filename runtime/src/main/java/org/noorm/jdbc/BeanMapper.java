package org.noorm.jdbc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Generic mapper for mapping a JDBC ResultSet to a Bean or for mapping a Bean
 * to a parameter map. This class is primarily used by the JDBCProcedureProcessor
 * to convert the JDBC ResultSets into Beans, resp. Lists of Beans.
 * Internally, the BeanMapper uses reflection to find the correct mapping with help
 * of the JDBCColumn annotations for the distinct attributes of the Beans.
 * Attributes without JDBCColumn annotation are considered transient and they are
 * omitted from the mapping procedure.
 *
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 */
public class BeanMapper<T> {

	private static BeanMapper mapper = new BeanMapper();
	private static final Logger log = LoggerFactory.getLogger(BeanMapper.class);

	public static <T> BeanMapper<T> getInstance() {

		return mapper;
	}

	private BeanMapper() {
	}

	/**
	 * Maps the given ResultSet to a list of Beans.
	 *
	 * @param pResultSet the ResultSet subject to conversion to a Bean list
	 * @param pBeanClass the type of the Bean
	 * @param pFetchPagingTotal indicates, whether all rows should be fetched at once
	 * @return the Bean list filled with the data from the ResultSet
	 * @throws SQLException JDBC driver exception
	 */
	public List<T> toBeanList(final ResultSet pResultSet,
							  final Class<T> pBeanClass,
							  final boolean pFetchPagingTotal) throws SQLException {

        if (log.isTraceEnabled()) {
            log.trace("Converting database results to list of Bean class ".concat(pBeanClass.getName()));
        }
		final List<T> beanList = new ArrayList<T>();
		T bean;
		final Field[] fields = BeanMetaDataUtil.getDeclaredFieldsInclParent(pBeanClass);
		if (fields == null || fields.length == 0) {
			return beanList;
		}
		while (pResultSet.next()) {
			try {
				if (pBeanClass.equals(Long.class)) {
					// Support for num_array based on java.lang.Long.
					bean = (T) new Long(pResultSet.getLong(1));
				} else {
					bean = pBeanClass.newInstance();
					populateFields(pResultSet, bean, fields, pFetchPagingTotal);
				}
			} catch (InstantiationException ex) {
				throw new DataAccessException(ex);
			} catch (IllegalAccessException ex) {
				throw new DataAccessException(ex);
			}
			beanList.add(bean);
		}

		return beanList;
	}

	/**
	 * Converts the given Bean to a Map, containing a mapping from the attribute name to
	 * the attributes value. The attribute name used is the database column name.
	 *
	 * @param pBean the Bean subject to conversion
	 * @return a map containing the content of the bean.
	 */
	public Map<String, Object> toMap(final T pBean) {

        if (log.isTraceEnabled()) {
            log.trace("Converting Bean to parameter map.");
        }
		final Map<String, Object> fieldMap = new HashMap<String, Object>();
		final Field[] fields = BeanMetaDataUtil.getDeclaredFieldsInclParent(pBean.getClass());
		if (fields == null || fields.length == 0) {
			return fieldMap;
		}

		String fieldName;
		for (final Field field : fields) {
			// Ignore serialVersionUID
			if (BeanMetaDataUtil.SERIAL_VERSION_UID.equals(field.getName())) {
				continue;
			}
			field.setAccessible(true);
            final JDBCColumn colAnn = BeanMetaDataUtil.getJDBCColumnAnnotation(field);
            if (colAnn != null) {
            	// All non-insertable columns are also non-updatable, so no more distinction required
                if (!colAnn.insertable()) {
                    continue;
                }
                fieldName = colAnn.name();
            } else {
				// Ignore fields without JDBCColumn annotation (interpreted transient)
				continue;
			}

			try {
				fieldMap.put(fieldName, field.get(pBean));
			} catch (IllegalAccessException ex) {
				throw new DataAccessException(ex);
			}
		}

		return fieldMap;
	}

	private void populateFields(final ResultSet pResultSet,
								final T pBean,
								final Field[] pFields,
								final boolean pFetchPagingTotal) throws IllegalAccessException, SQLException {

		String fieldName;
		for (final Field field : pFields) {
			// Ignore serialVersionUID
			if (BeanMetaDataUtil.SERIAL_VERSION_UID.equals(field.getName())) {
				continue;
			}
			field.setAccessible(true);
			final Class fieldType = field.getType();
            final JDBCColumn colAnn = BeanMetaDataUtil.getJDBCColumnAnnotation(field);
            if (colAnn != null) {
                fieldName = colAnn.name();
            } else {
                // Ignore fields without JDBCColumn annotation (interpreted transient)
                continue;
            }
			if (!pFetchPagingTotal && fieldName.equals(IBean.PAGING_TOTAL)) {
            	// Column PAGING_TOTAL is only available for paging queries.
				continue;
			}

			if (log.isTraceEnabled()) {
				StringBuilder logMessage = new StringBuilder();
				logMessage.append("Mapping database field : ");
				logMessage.append(fieldName);
				logMessage.append(" to Bean field ");
				logMessage.append(field.getName());
				logMessage.append(":");
				logMessage.append(fieldType.getName());
				log.trace(logMessage.toString());
			}

			// Principally, for matching types, using "field.set(beans, pResultSet.getObject(fieldName))" would
			// work. However, for non-matching types subject to automatic conversion by means of the
			// JDBC driver, we would run into problems.
			// The explicit casting based on the types of the Bean as follows is the most flexible approach
			// in providing a zero-configuration way to choose custom (but compatible!) types in the
			// Bean specification.

			if (fieldType == String.class) {
                final String value = pResultSet.getString(fieldName);
                if (value != null) {
                    field.set(pBean, value.trim());
                }
                continue;
			}

            if (fieldType == Long.class) {
                final Long value = pResultSet.getLong(fieldName);
                if (!pResultSet.wasNull()) {
                    field.set(pBean, value);
                }
                continue;
            }

			if (fieldType == Integer.class) {
				final Integer value = pResultSet.getInt(fieldName);
				if (!pResultSet.wasNull()) {
					field.set(pBean, value);
				}
                continue;
			}

			if (fieldType == Double.class) {
				final Double value = pResultSet.getDouble(fieldName);
				if (!pResultSet.wasNull()) {
					field.set(pBean, value);
				}
                continue;
			}
			if (fieldType == java.util.Date.class || fieldType == Timestamp.class) {
				field.set(pBean, pResultSet.getTimestamp(fieldName));
                continue;
			}

            if (fieldType == java.sql.Date.class) {
                Timestamp timestamp = pResultSet.getTimestamp(fieldName);
                if (!pResultSet.wasNull()) {
                    field.set(pBean, new java.sql.Date(timestamp.getTime()));
                }
                continue;
            }

			if (fieldType == BigDecimal.class) {
				field.set(pBean, pResultSet.getBigDecimal(fieldName));
                continue;
			}

			if (fieldType == Boolean.class) {
				field.set(pBean, pResultSet.getBoolean(fieldName));
                continue;
			}

			if (fieldType == Float.class) {
				final Float value = pResultSet.getFloat(fieldName);
				if (!pResultSet.wasNull()) {
					field.set(pBean, value);
				}
                continue;
			}

			if (fieldType == Short.class) {
				final Short value = pResultSet.getShort(fieldName);
				if (!pResultSet.wasNull()) {
					field.set(pBean, value);
				}
                continue;
			}

			if (fieldType == byte[].class) {
                field.set(pBean, pResultSet.getBytes(fieldName));
                continue;
			}
            if (fieldType == Clob.class) {
                final Clob value = pResultSet.getClob(fieldName);
                if (!pResultSet.wasNull()) {
                    field.set(pBean, value);
                }
                continue;
            }
            if (fieldType == NClob.class) {
                final NClob value = pResultSet.getNClob(fieldName);
                if (!pResultSet.wasNull()) {
                    field.set(pBean, value);
                }
                continue;
            }
            if (fieldType == Blob.class) {
                final Blob value = pResultSet.getBlob(fieldName);
                if (!pResultSet.wasNull()) {
                    field.set(pBean, value);
                }
                continue;
            }
            if (fieldType == SQLXML.class) {
                final SQLXML value = pResultSet.getSQLXML(fieldName);
                if (!pResultSet.wasNull()) {
                    field.set(pBean, value);
                }
                continue;
            }
            final String errMsg = "Datatype conversion failed for [".concat(fieldName)
                    .concat(" / ").concat(fieldType.getName()).concat("].");
            throw new DataAccessException(DataAccessException.Type.UNSUPPORTED_DATATYPE, errMsg);
		}
	}
}
