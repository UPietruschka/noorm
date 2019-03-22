package org.noorm.test;

import org.junit.Test;
import org.noorm.test.hr1.beans.GenericBeanValidator;
import org.noorm.test.hr1.enums.GenericEnumValidator;
import org.noorm.test.hr1.services.impl.GenericServiceValidator;

/**
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 21.12.11
 *         Time: 18:08
 */
public class ValidationTestHR1 {

	@Test
	public void testBeanValidation() {
		GenericBeanValidator beanValidator = new GenericBeanValidator();
		beanValidator.validateDatabase();
	}

	@Test
	public void testEnumValidation() {
		GenericEnumValidator enumValidator = new GenericEnumValidator();
		enumValidator.validateDatabase();
	}

	@Test
	public void testServiceValidation() {
		GenericServiceValidator serviceValidator = new GenericServiceValidator();
		serviceValidator.validateDatabase();
	}
}
