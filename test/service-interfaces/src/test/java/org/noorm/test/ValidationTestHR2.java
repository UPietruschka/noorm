package org.noorm.test;

import org.junit.Test;
import org.noorm.test.hr2.beans.GenericBeanValidator;

/**
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 21.12.11
 *         Time: 18:08
 */
public class ValidationTestHR2 {

	@Test
	public void testBeanValidation() {
		GenericBeanValidator beanValidator = new GenericBeanValidator();
		beanValidator.validateDatabase();
	}
}
