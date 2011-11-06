package org.noorm.generator.enumgenerator;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 27.07.11
 *         Time: 16:56
 */
public class EnumRecordDescriptor {

	private String typeColumnValue;
	private Map<EnumAttributeDescriptor, Object> attributeValues = new HashMap<EnumAttributeDescriptor, Object>();

	public String getTypeColumnValue() {
		return typeColumnValue.trim().toUpperCase().replaceAll("[ /\\-\\,\\.;]", "_");
	}

	public void setTypeColumnValue(final String pTypeColumnValue) {
		typeColumnValue = pTypeColumnValue;
	}

	public void setAttributeValue(final EnumAttributeDescriptor pAttributeDescriptor, final Object pValue) {
		attributeValues.put(pAttributeDescriptor, pValue);
	}

	public String getFormattedValue(final EnumAttributeDescriptor pAttributeDescriptor) {
		Object value = attributeValues.get(pAttributeDescriptor);
		// Only String and Long supported yet
		if (pAttributeDescriptor.getType().equals("String")) {
			return "\"".concat((String) value).trim().concat("\"");
		}
		if (pAttributeDescriptor.getType().equals("Long")) {
			if (value instanceof BigDecimal) {
				value = ((BigDecimal) value).longValue();
			}
			return ((Long) value).toString().concat("L");
		}
		throw new UnsupportedOperationException("EnumGenerator only supports String and Long types.");
	}
}
