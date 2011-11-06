package org.noorm.generator.beangenerator;

import org.noorm.jdbc.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Velocity class descriptor for the Bean generator
 *
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 03.05.11
 *         Time: 17:10
 */
public class BeanClassDescriptor {

	private long serialVersionUID;
	private String name;
	private String tableName;
	private String primaryKeyColumnName;
	private String sequenceName;
	private String versionColumnName;
	private String packageName;
	private List<BeanAttributeDescriptor> attributes = new ArrayList<BeanAttributeDescriptor>();

	public long getSerialVersionUID() {
		return serialVersionUID;
	}

	public void setSerialVersionUID(final long pSerialVersionUID) {
		serialVersionUID = pSerialVersionUID;
	}

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

	public String getPrimaryKeyColumnName() {
		return primaryKeyColumnName;
	}

	public void setPrimaryKeyColumnName(final String pPrimaryKeyColumnName) {
		primaryKeyColumnName = pPrimaryKeyColumnName;
	}

	public boolean hasPrimaryKey() {
		return !primaryKeyColumnName.isEmpty();
	}

	public String getFirstUpperPrimaryKeyColumnName() {
		return Utils.convertDBName2JavaName(primaryKeyColumnName, true);
	}

	public String getSequenceName() {
		return sequenceName;
	}

	public void setSequenceName(final String pSequenceName) {
		sequenceName = pSequenceName;
	}

	public String getVersionColumnName() {
		return versionColumnName;
	}

	public void setVersionColumnName(final String pVersionColumnName) {
		versionColumnName = pVersionColumnName;
	}

	public void addAttribute(final BeanAttributeDescriptor pAttribute) {
		attributes.add(pAttribute);
	}

	public List<BeanAttributeDescriptor> getAttributes() {
		return attributes;
	}

	public String getPackageName() {
		return packageName;
	}

	public void setPackageName(final String pPackageName) {
		packageName = pPackageName;
	}
}
