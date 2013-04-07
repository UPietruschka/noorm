package org.noorm.metadata;

import org.noorm.jdbc.DataAccessException;
import org.noorm.jdbc.IBean;
import org.noorm.jdbc.JDBCColumn;
import org.noorm.jdbc.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Convenience class to provide access to Bean metadata. Other classes and functionalities
 * like the BeanMapper require metadata information about the Bean, in particular information
 * about the non-transient fields, which are annotated with the JDBCColumn annotation.
 *
 * Since declared fields and annotations are determined at compile time, they are cached here to
 * improve performance.
 *
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 26.07.11
 *         Time: 11:37
 */
public class BeanMetaDataUtil {

	private static final Logger log = LoggerFactory.getLogger(BeanMetaDataUtil.class);

	public static final String NOT_UPDATABLE = "NO";
	public static final String NOT_NULLABLE = "N";
	public static final String SERIAL_VERSION_UID = "serialVersionUID";

    private static Map<Field, Annotation[]> declaredAnnotationsCache = new HashMap<Field, Annotation[]>();
    private static Map<Class, Field[]> declaredFieldInclParentCache = new HashMap<Class, Field[]>();

	/**
	 * Using Class.getDeclaredFields does not return fields declared in a potentially existing super-class.
	 * This method extends the retrieval of declared fields to the super-class, if any. Note that there is
	 * no recursive mechanism to detect declarations of the super-class of the super-class.
	 * @param pClass
	 * @return all declared fields of the provided class and its super-class, if any.
	 */
	public static Field[] getDeclaredFieldsInclParent(final Class pClass) {

        if (log.isTraceEnabled()) {
            log.trace("Retrieving declared fields by reflection for class ".concat(pClass.getName()));
        }

        Field[] allFields = declaredFieldInclParentCache.get(pClass);
        if (allFields == null) {
            final Field[] fields = pClass.getDeclaredFields();
            Field[] sFields = new Field[0];
            final Class superClass = pClass.getSuperclass();
            if (superClass != null) {
                sFields = superClass.getDeclaredFields();
            }
            allFields = Arrays.copyOf(fields, fields.length + sFields.length);
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
            declaredFieldInclParentCache.put(pClass, allFields);
        }

		return allFields;
	}

    /**
     * Returns the annotations for a given field.
     * The results are stored in a local cache, since method Field.getDeclaredAnnotations has a VERY BAD
     * performance.
     *
     * @param pField the field
     * @return the annotations for the given field
     */
    public static Annotation[] getDeclaredAnnotations(final Field pField) {

        Annotation[] annotations = declaredAnnotationsCache.get(pField);
        if (annotations == null) {
            annotations = pField.getDeclaredAnnotations();
            declaredAnnotationsCache.put(pField, annotations);
        }
        return annotations;
    }

	/**
	 * Returns a Map containing a mapping from the Bean attribute names to their JDBCColumn
	 * annotations.
	 *
	 * @param pClass the Bean type
	 * @return a map containing all columns, resp. fields with their associated JDBCColumn.
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

		if (pBean.getPrimaryKeyColumnNames().length != 1) {
			throw new DataAccessException(DataAccessException.Type.OPERATION_NOT_SUPPORTED_WITH_COMPOSITE_PK);
		}
		final String primaryKeyColumnName = pBean.getPrimaryKeyColumnNames()[0];
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

	public static Class getBeanPropertyType(final Object pObject, final String pPropertyName) {

        Field property = getDeclaredFieldInclParent(pObject, pPropertyName);
        return property.getType();
	}

	public static Object getBeanPropertyByName(final Object pObject, final String pPropertyName) {

        Field property = getDeclaredFieldInclParent(pObject, pPropertyName);
		try {
			return property.get(pObject);
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

		if (pBean.getPrimaryKeyColumnNames().length != 1) {
			throw new DataAccessException(DataAccessException.Type.OPERATION_NOT_SUPPORTED_WITH_COMPOSITE_PK);
		}
		final String primaryKeyColumnName = pBean.getPrimaryKeyColumnNames()[0];
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

        Field property = getDeclaredFieldInclParent(pBean, pPropertyName);
		try {
			property.set(pBean, pPKValue);
		} catch (IllegalAccessException e) {
			throw new DataAccessException(DataAccessException.Type.COULD_NOT_ACCESS_PROPERTY_BY_REFLECTION, e);
		}
	}

    private static Field getDeclaredFieldInclParent(final Object pObject, final String pPropertyName) {

        final Field[] fields = getDeclaredFieldsInclParent(pObject.getClass());
        final String javaPropertyName = Utils.convertDBName2JavaName(pPropertyName, false);
        Field property = null;
        for (final Field field : fields) {
            if (field.getName().equals(javaPropertyName)) {
                property = field;
            }
        }
        if (property == null) {
            throw new DataAccessException(DataAccessException.Type.COULD_NOT_ACCESS_PROPERTY_BY_REFLECTION,
                    pObject.getClass().getName().concat(".").concat(javaPropertyName));
        }
        property.setAccessible(true);
        return property;
    }
}
