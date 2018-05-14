package org.noorm.generator;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 28.11.11
 *         Time: 22:00
 */
public class ValidatorClassDescriptor {

	private String packageName;
	private List<String> classNames = new ArrayList<>();
    private String dataSourceName;
    private String schemaPattern;
    private String tableNamePattern;

    public String getPackageName() {
		return packageName;
	}

	public void setPackageName(final String pPackageName) {
		packageName = pPackageName;
	}

	public List<String> getClassNames() {
		return classNames;
	}

	public void setClassNames(final List<String> pClassNames) {
		classNames = pClassNames;
	}

    public String getDataSourceName() {
        return dataSourceName;
    }

    public void setDataSourceName(final String pDataSourceName) {
        dataSourceName = pDataSourceName;
    }

    public String getSchemaPattern() {
        return schemaPattern;
    }

    public void setSchemaPattern(final String pSchemaPattern) {
        schemaPattern = pSchemaPattern;
    }

    public String getTableNamePattern() {
        return tableNamePattern;
    }

    public void setTableNamePattern(final String pTableNamePattern) {
        tableNamePattern = pTableNamePattern;
    }

    public boolean hasDataSourceName() {
        return dataSourceName != null && !dataSourceName.isEmpty();
    }
}
