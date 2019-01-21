package org.noorm.generator.beangenerator;

import org.noorm.generator.GeneratorException;
import org.noorm.generator.GeneratorUtil;
import org.noorm.generator.IParameters;
import org.noorm.generator.ParameterDescriptor;
import org.noorm.generator.schema.DeleteDeclaration;
import org.noorm.generator.schema.GeneratorConfiguration;
import org.noorm.generator.schema.OperatorName;
import org.noorm.generator.schema.QueryColumn;
import org.noorm.jdbc.DataSourceProvider;
import org.noorm.jdbc.Utils;
import org.noorm.jdbc.platform.IMetadata;
import org.noorm.jdbc.platform.TableMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Generator for database related Bean source files based on Velocity templates.
 *
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 19.04.11
 *         Time: 14:48
 */
public class BeanDMLGenerator {

	private static final Logger log = LoggerFactory.getLogger(BeanDMLGenerator.class);
	private static final String DEFAULT_METHOD_NAME_PREFIX = "delete";
	private static final String DEFAULT_METHOD_NAME_PART3 = "By";
	private static final String PARAMETER_PREFIX = "p";
	private static final String BEAN_DML_VM_TEMPLATE_FILE = "/bean_dml.vm";

	private IParameters parameters;
    private GeneratorConfiguration configuration;

	public BeanDMLGenerator(final IParameters pParameters, final GeneratorConfiguration pConfiguration) {
		parameters = pParameters;
        configuration = pConfiguration;
	}

	public void execute() throws GeneratorException {

        if (!GeneratorUtil.hasServicePackageName(configuration)) {
            throw new IllegalArgumentException("Parameter [servicePackageName] is null.");
        }

        final IMetadata metadata = DataSourceProvider.getPlatform().getMetadata();

		log.info("Retrieving table metadata from database.");
        String beanTableFilterRegex = null;
        if (configuration.getBeanTableFilter() != null) {
            beanTableFilterRegex = configuration.getBeanTableFilter().getRegex();
        }
        final String schemaFilterRegex = configuration.getSchemaFilter().getRegex();
		final Map<String, List<TableMetadata>> tableColumnMap =
				metadata.findTableMetadata(schemaFilterRegex, beanTableFilterRegex);

		log.info("Generating NoORM DML classes.");
		final File servicePackageDir = GeneratorUtil.createPackageDir
				(parameters.getDestinationDirectory(), configuration.getServiceJavaPackage().getName());
		File serviceInterfacePackageDir = null;
        if (GeneratorUtil.hasServiceInterfacePackageName(configuration)) {
			serviceInterfacePackageDir = GeneratorUtil.createPackageDir
					(parameters.getDestinationDirectory(), configuration.getServiceInterfaceJavaPackage().getName());
		}

		for (final String tableName0 : tableColumnMap.keySet()) {
			final String javaBeanName =
					GeneratorUtil.convertTableName2JavaName(tableName0, configuration.getTableNameMappings());
			final BeanDMLClassDescriptor beanDMLClassDescriptor = new BeanDMLClassDescriptor();
			beanDMLClassDescriptor.setBeanPackageName(configuration.getBeanJavaPackage().getName());
			beanDMLClassDescriptor.setName(javaBeanName);
			beanDMLClassDescriptor.setJavaName(javaBeanName + "DML");
			if (configuration.getExtendedBeanMappings() != null) {
				final String extJavaBeanName =
						GeneratorUtil.getMappedString(javaBeanName, configuration.getExtendedBeanMappings());
				if (!extJavaBeanName.isEmpty()) {
					beanDMLClassDescriptor.setExtendedName(extJavaBeanName);
				}
			}
			if (GeneratorUtil.hasServiceInterfacePackageName(configuration)) {
				beanDMLClassDescriptor.setInterfacePackageName
                        (configuration.getServiceInterfaceJavaPackage().getName());
			}
			beanDMLClassDescriptor.setPackageName(configuration.getServiceJavaPackage().getName());

			if (GeneratorUtil.hasDataSourceName(configuration)) {
				beanDMLClassDescriptor.setDataSourceName(configuration.getDataSource().getName());
			}

			for (final DeleteDeclaration deleteDeclaration : configuration.getDeleteDeclarations()) {
				if (deleteDeclaration.getTableName().equals(tableName0)) {
					generateMethodName(deleteDeclaration);
					final DeleteDescriptor deleteDescriptor = new DeleteDescriptor();
					String t0 = deleteDeclaration.getTableName();
					List<TableMetadata> baseTableMetadataList = tableColumnMap.get(t0);
					List<TableMetadata> tableMetadataList = tableColumnMap.get(t0);
					if (baseTableMetadataList == null) {
						throw new GeneratorException("Invalid delete declaration: no metadata found for table "
								.concat(deleteDeclaration.getTableName()));
					}
					deleteDescriptor.setDeleteDeclaration(deleteDeclaration);
					String beanName = GeneratorUtil.convertTableName2JavaName(t0, configuration.getTableNameMappings());
					if (configuration.getExtendedBeanMappings() != null) {
						final String extBeanName =
								GeneratorUtil.getMappedString(beanName, configuration.getExtendedBeanMappings());
						if (!extBeanName.isEmpty()) {
							beanName = extBeanName;
						}
					}
					deleteDescriptor.setBeanName(beanName);
					int paramIndex = 1;
					for (final QueryColumn queryColumn : deleteDeclaration.getQueryColumn()) {
						final ParameterDescriptor parameterDescriptor = new ParameterDescriptor();
						final String columnName = queryColumn.getName();
						final String index = String.format("%02d", paramIndex++);
						parameterDescriptor.setJavaName
								(PARAMETER_PREFIX + index + Utils.convertDBName2JavaName(columnName, true));
						parameterDescriptor.setDbParamName(columnName);
						final String customExpression = queryColumn.getCustomExpression();
						parameterDescriptor.setCustomExpression(customExpression);
						if (customExpression == null && queryColumn.getOperator() == OperatorName.CUSTOM) {
							throw new GeneratorException("Invalid delete declaration: custom expression missing for table "
									.concat(t0).concat(" and column ").concat(columnName));
						}
						if (customExpression != null && queryColumn.getOperator() != OperatorName.CUSTOM) {
							throw new GeneratorException("Invalid delete declaration: custom expression not allowed for table "
									.concat(t0).concat(" and column ").concat(columnName));
						}
						String javaType = null;
						for (final TableMetadata tableMetadata : tableMetadataList) {
							if (tableMetadata.getColumnName().equals(columnName)) {
								javaType = GeneratorUtil.convertDatabaseType2JavaType(
										tableMetadata.getJDBCType(),
										tableMetadata.getDecimalDigits(),
										tableMetadata.getTableName(),
										tableMetadata.getColumnName(),
										configuration.getTypeMappings());
							}
						}
						if (javaType == null) {
							throw new GeneratorException("Invalid delete declaration: no metadata found for table "
									.concat(t0).concat(" and column ").concat(columnName));
						}
						parameterDescriptor.setJavaType(javaType);
						parameterDescriptor.setOperator(queryColumn.getOperator());
						if (queryColumn.getOperator().equals(OperatorName.IS_NULL)) {
							parameterDescriptor.setUnaryOperator(true);
						}
						if (queryColumn.getOperator().equals(OperatorName.IS_NOT_NULL)) {
							parameterDescriptor.setUnaryOperator(true);
						}
						if (queryColumn.getOperator().equals(OperatorName.IN)) {
							parameterDescriptor.setIsList(true);
						}
						deleteDescriptor.addParameter(parameterDescriptor);
					}
					beanDMLClassDescriptor.addDelete(deleteDescriptor);
				}
			}

			GeneratorUtil.generateFile(servicePackageDir, BEAN_DML_VM_TEMPLATE_FILE,
					beanDMLClassDescriptor.getJavaName(), beanDMLClassDescriptor);
			if (GeneratorUtil.hasServiceInterfacePackageName(configuration)) {
				beanDMLClassDescriptor.setInterface(true);
				GeneratorUtil.generateFile(serviceInterfacePackageDir, BEAN_DML_VM_TEMPLATE_FILE,
						"I" + beanDMLClassDescriptor.getJavaName(), beanDMLClassDescriptor);
			}
		}
	}

	private void generateMethodName(final DeleteDeclaration pQueryDeclaration) {

		if (pQueryDeclaration.getGeneratedMethodName() == null
				|| pQueryDeclaration.getGeneratedMethodName().isEmpty()) {
			final StringBuilder methodName = new StringBuilder();
			methodName.append(DEFAULT_METHOD_NAME_PREFIX);
			String t0 = pQueryDeclaration.getTableName();
			final String javaTableName =
					GeneratorUtil.convertTableName2JavaName(t0, configuration.getTableNameMappings());
			methodName.append(javaTableName);
			if (!pQueryDeclaration.getQueryColumn().isEmpty()) {
				methodName.append(DEFAULT_METHOD_NAME_PART3);
				// With an increasing number of parameters, we use a substring of decreasing length of the
				// parameter name for method name construction
				int substringLength = 16;
				if (pQueryDeclaration.getQueryColumn().size() > 1) { substringLength = 8; }
				if (pQueryDeclaration.getQueryColumn().size() > 2) { substringLength = 4; }
				if (pQueryDeclaration.getQueryColumn().size() > 4) { substringLength = 2; }
				if (pQueryDeclaration.getQueryColumn().size() > 8) { substringLength = 1; }
				for (final QueryColumn queryColumn : pQueryDeclaration.getQueryColumn()) {
					final String javaColumnName = Utils.convertDBName2JavaName(queryColumn.getName(), true);
					methodName.append(javaColumnName, 0, Math.min(substringLength, javaColumnName.length()));
				}
			}
			pQueryDeclaration.setGeneratedMethodName(methodName.toString());
		}
	}
}
