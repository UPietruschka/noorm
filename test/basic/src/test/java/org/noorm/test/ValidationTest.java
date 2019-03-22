package org.noorm.test;

import org.junit.Test;
import org.noorm.test.hr.beans.GenericBeanValidator;
import org.noorm.test.hr.enums.GenericEnumValidator;
import org.noorm.test.hr.services.GenericServiceValidator;

/**
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 21.12.11
 *         Time: 18:08
 */
public class ValidationTest {

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
