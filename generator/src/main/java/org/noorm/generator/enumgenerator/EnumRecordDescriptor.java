package org.noorm.generator.enumgenerator;

import org.noorm.jdbc.Utils;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 27.07.11
 *         Time: 16:56
 */
class EnumRecordDescriptor {

	private String displayColumnValue;
	private final Map<EnumAttributeDescriptor, Object> attributeValues = new HashMap<EnumAttributeDescriptor, Object>();

	public String getDisplayColumnValue() {
		return Utils.getNormalizedDisplayColumnValue(displayColumnValue);
	}

	public void setDisplayColumnValue(final String pDisplayColumnValue) {
		displayColumnValue = pDisplayColumnValue;
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
			return value.toString().concat("L");
		}
		throw new UnsupportedOperationException("EnumGenerator only supports String and Long types.");
	}
}
