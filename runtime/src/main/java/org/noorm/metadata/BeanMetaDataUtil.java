package org.noorm.metadata;

import org.noorm.jdbc.DataAccessException;
import org.noorm.jdbc.IBean;
import org.noorm.jdbc.JDBCColumn;
import org.noorm.jdbc.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Convenience class to provide access to Bean metadata. Other classes and functionalities
 * like the BeanMapper require metadata information about the Bean, in particular information
 * about the non-transient fields, which are annotated with the JDBCColumn annotation.
 *
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 26.07.11
 *         Time: 11:37
 */
public class BeanMetaDataUtil {

	private static final Logger log = LoggerFactory.getLogger(BeanMetaDataUtil.class);
	private static final String BEAN_GETTER_METHOD_NAME_PREFIX = "get";
	private static final String BEAN_SETTER_METHOD_NAME_PREFIX = "set";

	public static final String NOT_UPDATABLE = "NO";
	public static final String NOT_NULLABLE = "N";
	public static final String SERIAL_VERSION_UID = "serialVersionUID";

	/**
	 * Using Class.getDeclaredFields does not return fields declared in a potentially existing super-class.
	 * This method extends the retrieval of declared fields to the super-class, if any. Note that there is
	 * no recursive mechanism to detect declarations of the super-class of the super-class.
	 * @param pClass
	 * @return all declared fields of the provided class and its super-class, if any.
	 */
	public static Field[] getDeclaredFieldsInclParent(final Class pClass) {

		log.debug("Retrieving declared fields by reflection for class ".concat(pClass.getName()));
		final Field[] fields = pClass.getDeclaredFields();
		Field[] sFields = new Field[0];
		final Class superClass = pClass.getSuperclass();
		if (superClass != null) {
			sFields = superClass.getDeclaredFields();
		}
		final Field[] allFields = Arrays.copyOf(fields, fields.length + sFields.length);
		System.arraycopy(sFields, 0, allFields, fields.length, sFields.length);

		if (log.isTraceEnabled()) {
			int i = 0;
			for (final Field field : allFields) {
				final StringBuilder logMessage = new StringBuilder();
				logMessage.append("Field ");
				logMessage.append(i++);
				logMessage.append(" : ");
				logMessage.append(field.getName());
				logMessage.append(":");
				logMessage.append(field.getType().getName());
				log.trace(logMessage.toString());
			}
		}

		return allFields;
	}

	/**
	 * Returns a Map containing a mapping from the Bean attribute names to their JDBCColumn
	 * annotations.
	 *
	 * @param pClass the Bean type
	 * @return
	 */
	public static Map<String, JDBCColumn> getColumnMetaData(final Class pClass) {

		final Map<String, JDBCColumn> columnMetaDataMap = new HashMap<String, JDBCColumn>();
		final Field[] fields = BeanMetaDataUtil.getDeclaredFieldsInclParent(pClass);
		for (final Field field : fields) {
			// Ignore serialVersionUID
			if (SERIAL_VERSION_UID.equals(field.getName())) {
				continue;
			}
			final Annotation[] annotations = field.getDeclaredAnnotations();
			if (annotations != null && annotations.length > 0) {
				if (annotations[0].annotationType() == JDBCColumn.class) {
					final JDBCColumn colAnn = (JDBCColumn) annotations[0];
					columnMetaDataMap.put(field.getName(), colAnn);
				}
			}
			// Ignore fields without JDBCColumn annotation (interpreted transient)
		}
		return columnMetaDataMap;
	}

	/**
	 * Retrieves the value of the (primary) key of the bean passed to this method. The data is retrieved
	 * by reflection based on the bean metadata provided by the bean itself.
	 * @param pBean the bean instance.
	 * @return the primary key value for the passed bean.
	 */
	public static Long getPrimaryKeyValue(final IBean pBean) {

		final String primaryKeyColumnName = pBean.getPrimaryKeyColumnName();
		final Object property = getBeanPropertyByName(pBean, primaryKeyColumnName);
		if (!(property instanceof Long)) {
			throw new DataAccessException(DataAccessException.Type.UNSUPPORTED_DATATYPE);
		}
		return (Long) property;
	}

	/**
	 * Retrieves the value of the version column of the bean passed to this method. The data is retrieved
	 * by reflection based on the bean metadata provided by the bean itself.
	 * @param pBean the bean instance.
	 * @return the version column value for the passed bean.
	 */
	public static Long getVersionColumnValue(final IBean pBean) {

		final String versionColumnName = pBean.getVersionColumnName();
		final Object property = getBeanPropertyByName(pBean, versionColumnName);
		if (!(property instanceof Long)) {
			throw new DataAccessException(DataAccessException.Type.UNSUPPORTED_VERSION_COLUMN_TYPE);
		}
		return (Long) property;
	}

	public static Object getBeanPropertyByName(final Object pObject, final String pPropertyName) {

		Method propertyGetter;
		try {
			final String propertyGetterMethodName = BEAN_GETTER_METHOD_NAME_PREFIX
					.concat(Utils.convertDBName2JavaName(pPropertyName, true));
			propertyGetter = pObject.getClass().getMethod(propertyGetterMethodName);
		} catch (NoSuchMethodException e) {
			throw new DataAccessException(DataAccessException.Type.COULD_NOT_ACCESS_PROPERTY_BY_REFLECTION, e);
		}

		try {
			return propertyGetter.invoke(pObject);
		} catch (InvocationTargetException e) {
			throw new DataAccessException(DataAccessException.Type.COULD_NOT_ACCESS_PROPERTY_BY_REFLECTION, e);
		} catch (IllegalAccessException e) {
			throw new DataAccessException(DataAccessException.Type.COULD_NOT_ACCESS_PROPERTY_BY_REFLECTION, e);
		}
	}

	/**
	 * Sets the value of the (primary) key of the bean passed to this method. The data is set
	 * by reflection based on the bean metadata provided by the bean itself.
	 * @param pBean the bean instance.
	 * @param pPKValue the value of the primary key.
	 */
	public static void setPrimaryKeyValue(final IBean pBean, final Long pPKValue) {

		final String primaryKeyColumnName = pBean.getPrimaryKeyColumnName();
		setBeanPropertyByName(pBean, primaryKeyColumnName, pPKValue);
	}

	/**
	 * Sets the value of the version column of the bean passed to this method. The data is set
	 * by reflection based on the bean metadata provided by the bean itself.
	 * @param pBean the bean instance.
	 * @param pVersionColumnValue the value of the version column.
	 */
	public static void setVersionColumnValue(final IBean pBean, final Long pVersionColumnValue) {

		final String versionColumnName = pBean.getVersionColumnName();
		setBeanPropertyByName(pBean, versionColumnName, pVersionColumnValue);
	}

	private static void setBeanPropertyByName(final IBean pBean, final String pPropertyName, final Long pPKValue) {

		Method propertySetter;
		try {
			final String propertySetterMethodName = BEAN_SETTER_METHOD_NAME_PREFIX
					.concat(Utils.convertDBName2JavaName(pPropertyName, true));
			propertySetter = pBean.getClass().getMethod(propertySetterMethodName, Long.class);
		} catch (NoSuchMethodException e) {
			throw new DataAccessException(DataAccessException.Type.COULD_NOT_ACCESS_PROPERTY_BY_REFLECTION, e);
		}

		try {
			propertySetter.invoke(pBean, pPKValue);
		} catch (InvocationTargetException e) {
			throw new DataAccessException(DataAccessException.Type.COULD_NOT_ACCESS_PROPERTY_BY_REFLECTION, e);
		} catch (IllegalAccessException e) {
			throw new DataAccessException(DataAccessException.Type.COULD_NOT_ACCESS_PROPERTY_BY_REFLECTION, e);
		}
	}
}
