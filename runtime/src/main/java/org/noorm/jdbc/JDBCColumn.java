package org.noorm.jdbc;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation specification for database column name mapping
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface JDBCColumn {

	String name() default "";
	String dataType() default "";
	boolean updatable() default true;
	boolean nullable() default true;
    boolean caseSensitiveName() default false;
	int maxLength() default 0;
}
