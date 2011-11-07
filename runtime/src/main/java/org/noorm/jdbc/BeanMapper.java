package org.noorm.jdbc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Generic mapper for mapping a JDBC ResultSet to a Bean or for mapping a Bean
 * to a parameter map. This class is primarily used by the JDBCStatementProcessor
 * to convert the JDBC ResultSets into Beans, resp. Lists of Beans.
 * Internally, the BeanMapper uses reflection to find the correct mapping with help
 * of the JDBCColumn annotations for the distinct attributes of the Beans.
 * Attributes without JDBCColumn annotation are considered transient and they are
 * omitted from the mapping procedure.
 *
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 * @version 1.0.0
 */
public class BeanMapper<T> {

	private static BeanMapper mapper;
	private static final Logger log = LoggerFactory.getLogger(BeanMapper.class);

	public static <T> BeanMapper<T> getInstance() {

		synchronized (BeanMapper.class) {
			if (mapper == null) {
				mapper = new BeanMapper<T>();
			}
		}
		return mapper;
	}

	private BeanMapper() {
	}

	/**
	 * Maps the record, the given ResultSet currently points to, to a Bean.
	 *
	 * @param pResultSet the ResultSet subject to conversion to a Bean
	 * @param pBeanClass the type of the Bean
	 * @return the Bean filled with the data from the ResultSet
	 * @throws SQLException
	 */
	public T toBean(final ResultSet pResultSet, final Class<T> pBeanClass) throws SQLException {

		T bean = null;
		log.debug("Converting database results to single Bean class ".concat(pBeanClass.getName()));
		final Field[] fields = BeanMetaDataUtil.getDeclaredFieldsInclParent(pBeanClass);
		if (pResultSet.next() && fields != null && fields.length > 0) {
			try {
				bean = pBeanClass.newInstance();
				populateFields(pResultSet, bean, fields);
			} catch (InstantiationException ex) {
				throw new DataAccessException(ex);
			} catch (IllegalAccessException ex) {
				throw new DataAccessException(ex);
			}
		}

		return bean;
	}

	/**
	 * Maps the given ResultSet to a list of Beans.
	 *
	 * @param pResultSet the ResultSet subject to conversion to a Bean list
	 * @param pBeanClass the type of the Bean
	 * @return the Bean list filled with the data from the ResultSet
	 * @throws SQLException
	 */
	public List<T> toBeanList(final ResultSet pResultSet, final Class<T> pBeanClass) throws SQLException {

		log.debug("Converting database results to list of Bean class ".concat(pBeanClass.getName()));
		final List<T> beanList = new ArrayList<T>();
		T bean;
		final Field[] fields = BeanMetaDataUtil.getDeclaredFieldsInclParent(pBeanClass);
		if (fields == null || fields.length == 0) {
			return beanList;
		}
		while (pResultSet.next()) {
			try {
				bean = pBeanClass.newInstance();
				populateFields(pResultSet, bean, fields);
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
	 * @return
	 */
	public Map<String, Object> toMap(final T pBean) {

		log.debug("Converting Bean to parameter map.");
		final Map<String, Object> fieldMap = new LinkedHashMap<String, Object>();
		final Field[] fields = BeanMetaDataUtil.getDeclaredFieldsInclParent(pBean.getClass());
		if (fields == null || fields.length == 0) {
			return fieldMap;
		}

		String fieldName = null;
		for (final Field field : fields) {
			// Ignore serialVersionUID
			if (BeanMetaDataUtil.SERIAL_VERSION_UID.equals(field.getName())) {
				continue;
			}
			field.setAccessible(true);
			final Annotation[] annotations = field.getDeclaredAnnotations();
			if (annotations != null && annotations.length > 0) {
				if (annotations[0].annotationType() == JDBCColumn.class) {
					final JDBCColumn colAnn = (JDBCColumn) annotations[0];
					if (!colAnn.updatable()) {
						continue;
					}
					fieldName = colAnn.name();
				}
			} else {
				// Ignore fields without JDBCColumn annotation (interpreted transient)
				continue;
			}

			try {
				fieldMap.put(fieldName, field.get(pBean));
			} catch (IllegalArgumentException ex) {
				throw new DataAccessException(ex);
			} catch (IllegalAccessException ex) {
				throw new DataAccessException(ex);
			}
		}

		return fieldMap;
	}

	private void populateFields(final ResultSet pResultSet, final T pBean, final Field[] pFields)
			throws IllegalAccessException, SQLException {

		String fieldName = null;
		for (final Field field : pFields) {
			// Ignore serialVersionUID
			if (BeanMetaDataUtil.SERIAL_VERSION_UID.equals(field.getName())) {
				continue;
			}
			field.setAccessible(true);
			final Class fieldType = field.getType();
			final Annotation[] annotations = field.getDeclaredAnnotations();
			if (annotations != null && annotations.length > 0) {
				if (annotations[0].annotationType() == JDBCColumn.class) {
					final JDBCColumn colAnn = (JDBCColumn) annotations[0];
					fieldName = colAnn.name();
				}
			} else {
				// Ignore pFields without JDBCColumn annotation (interpreted transient)
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
			// The explicit casting based on the types of the Bean as flows is the most flexible approach
			// in providing a zero-configuration way to choose custom (but compatible!) types in the
			// Bean specification.

			if (fieldType == Long.class) {
				final Long value = pResultSet.getLong(fieldName);
				if (!pResultSet.wasNull()) {
					field.set(pBean, value);
				}
			}

			if (fieldType == String.class) {
				final String value = pResultSet.getString(fieldName);
				if (value != null) {
					field.set(pBean, value.trim());
				}
			}

			if (fieldType == Integer.class) {
				final Integer value = pResultSet.getInt(fieldName);
				if (!pResultSet.wasNull()) {
					field.set(pBean, value);
				}
			}

			if (fieldType == Double.class) {
				final Double value = pResultSet.getDouble(fieldName);
				if (!pResultSet.wasNull()) {
					field.set(pBean, value);
				}
			}

			if (fieldType == java.util.Date.class || fieldType == java.sql.Date.class) {
				java.util.Date sqlDate = pResultSet.getDate(fieldName);
				if (sqlDate != null) {
					field.set(pBean, new java.util.Date(sqlDate.getTime()));
				}
			}
			if (fieldType == BigDecimal.class) {
				field.set(pBean, pResultSet.getBigDecimal(fieldName));
			}

			if (fieldType == Boolean.class) {
				field.set(pBean, pResultSet.getBoolean(fieldName));
			}

			if (fieldType == Float.class) {
				final Float value = pResultSet.getFloat(fieldName);
				if (!pResultSet.wasNull()) {
					field.set(pBean, value);
				}
			}

			if (fieldType == Short.class) {
				final Short value = pResultSet.getShort(fieldName);
				if (!pResultSet.wasNull()) {
					field.set(pBean, value);
				}
			}

			if (fieldType == Timestamp.class) {
				field.set(pBean, pResultSet.getTimestamp(fieldName));
			}

			if (fieldType == byte[].class) {
				field.set(pBean, pResultSet.getBytes(fieldName));
			}
		}
	}
}
