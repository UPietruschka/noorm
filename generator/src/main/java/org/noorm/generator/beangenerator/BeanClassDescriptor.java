package org.noorm.generator.beangenerator;

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
	private String shortName;
	private String tableName;
    private boolean isTableNameCaseSensitive = false;
    private String[] primaryKeyColumnNames;
    private String[] primaryKeyJavaNames;
	private String sequenceName;
    private Integer sequenceIncrement;
    private boolean useInlineSequenceValueGeneration = false;
    private String versionColumnName;
    private String versionColumnJavaName = "";
    private String versionColumnType;
	private String packageName;
	private final List<BeanAttributeDescriptor> attributes = new ArrayList<>();
    private boolean generatePKBasedEqualsAndHashCode;
    private boolean enableOptLockFullRowCompare = false;
    private String customInterfaceName;
	private String superClassName;

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

	public void setShortName(final String pShortName) {
		shortName = pShortName;
	}

	public String getShortName() {
		return shortName;
	}

	public String getTableName() {
		return tableName;
	}

	public void setTableName(final String pTableName) {
		tableName = pTableName;
	}

    public boolean isTableNameCaseSensitive() {
        return isTableNameCaseSensitive;
    }

    public void setTableNameCaseSensitive(final boolean pTableNameCaseSensitive) {
        isTableNameCaseSensitive = pTableNameCaseSensitive;
    }

	public String[] getPrimaryKeyColumnNames() {
		return primaryKeyColumnNames;
	}

	public void setPrimaryKeyColumnNames(final String[] pPrimaryKeyColumnNames) {
		primaryKeyColumnNames = pPrimaryKeyColumnNames;
	}

    public String[] getPrimaryKeyJavaNames() {
        return primaryKeyJavaNames;
    }

    public void setPrimaryKeyJavaNames(final String[] pPrimaryKeyJavaNames) {
        primaryKeyJavaNames = pPrimaryKeyJavaNames;
    }

	public boolean hasPrimaryKey() {
		return primaryKeyColumnNames.length > 0;
	}

	public String getSequenceName() {
		return sequenceName;
	}

	public void setSequenceName(final String pSequenceName) {
		sequenceName = pSequenceName;
	}

    public Integer getSequenceIncrement() {
        return sequenceIncrement;
    }

    public void setSequenceIncrement(final Integer pSequenceIncrement) {
        sequenceIncrement = pSequenceIncrement;
    }

    public boolean useInlineSequenceValueGeneration() {
        return useInlineSequenceValueGeneration;
    }

    public void setUseInlineSequenceValueGeneration(final boolean pUseInlineSequenceValueGeneration) {
        useInlineSequenceValueGeneration = pUseInlineSequenceValueGeneration;
    }

	public String getVersionColumnName() {
		return versionColumnName;
	}

	public void setVersionColumnName(final String pVersionColumnName) {
		versionColumnName = pVersionColumnName;
	}

    public String getVersionColumnJavaName() {
        return versionColumnJavaName;
    }

    public void setVersionColumnJavaName(final String pVersionColumnJavaName) {
        versionColumnJavaName = pVersionColumnJavaName;
    }

    public String getVersionColumnType() {
        return versionColumnType;
    }

    public void setVersionColumnType(final String pVersionColumnType) {
        versionColumnType = pVersionColumnType;
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

	public boolean isUpdatable() {
		boolean isUpdatable = false;
		for (final BeanAttributeDescriptor beanAttributeDescriptor : attributes) {
			if (beanAttributeDescriptor.isUpdatable()) {
				isUpdatable = true;
			}
		}
		return isUpdatable;
	}

    public boolean generatePKBasedEqualsAndHashCode() {
        return generatePKBasedEqualsAndHashCode;
    }

    public void setGeneratePKBasedEqualsAndHashCode(final boolean pGeneratePKBasedEqualsAndHashCode) {
        generatePKBasedEqualsAndHashCode = pGeneratePKBasedEqualsAndHashCode;
    }

    public boolean enableOptLockFullRowCompare() {
        return enableOptLockFullRowCompare;
    }

    public void setEnableOptLockFullRowCompare(final boolean pEnableOptLockFullRowCompare) {
        enableOptLockFullRowCompare = pEnableOptLockFullRowCompare;
    }

	public String getCustomInterfaceName() {
		return customInterfaceName;
	}

	public void setCustomInterfaceName(final String pCustomInterfaceName) {
		customInterfaceName = pCustomInterfaceName;
	}

	public String getSuperClassName() {
		return superClassName;
	}

	public void setSuperClassName(final String pSuperClassName) {
		superClassName = pSuperClassName;
	}
}
