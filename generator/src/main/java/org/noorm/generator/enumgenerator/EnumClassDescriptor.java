package org.noorm.generator.enumgenerator;

import java.util.ArrayList;
import java.util.List;

/**
 * Velocity class descriptor for the Enum generator
 *
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 03.05.11
 *         Time: 17:10
 */
public class EnumClassDescriptor {

	private String name;
	private String tableName;
	private String typeColumnName;
	private String packageName;
	private List<EnumAttributeDescriptor> attributes = new ArrayList<EnumAttributeDescriptor>();
	private List<EnumRecordDescriptor> records = new ArrayList<EnumRecordDescriptor>();

	public void setName(final String pName) {
		name = pName;
	}

	public String getName() {
		return name;
	}

	public String getTableName() {
		return tableName;
	}

	public void setTableName(final String pTableName) {
		tableName = pTableName;
	}

	public String getTypeColumnName() {
		return typeColumnName;
	}

	public void setTypeColumnName(final String pTypeColumnName) {
		typeColumnName = pTypeColumnName;
	}

	public String getPackageName() {
		return packageName;
	}

	public void setPackageName(final String pPackageName) {
		packageName = pPackageName;
	}

	public void addAttribute(final EnumAttributeDescriptor pAttribute) {
		attributes.add(pAttribute);
	}

	public List<EnumAttributeDescriptor> getAttributes() {
		return attributes;
	}

	public void addRecord(final EnumRecordDescriptor pRecord) {
		records.add(pRecord);
	}

	public List<EnumRecordDescriptor> getRecords() {
		return records;
	}
}
