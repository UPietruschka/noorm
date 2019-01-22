package org.noorm.generator.beangenerator;

import org.noorm.generator.*;
import org.noorm.generator.schema.*;
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
	private static final String UPDATE_METHOD_NAME_PREFIX = "update";
	private static final String DELETE_METHOD_NAME_PREFIX = "delete";
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

			for (final UpdateDeclaration updateDeclaration : configuration.getUpdateDeclarations()) {
				if (updateDeclaration.getTableName().equals(tableName0)) {
					if (updateDeclaration.getGeneratedMethodName() == null) {
						updateDeclaration.setGeneratedMethodName(GeneratorUtil.generateMethodName(updateDeclaration,
								null, UPDATE_METHOD_NAME_PREFIX, configuration));
					}
					final UpdateDescriptor updateDescriptor = new UpdateDescriptor();
					String t0 = updateDeclaration.getTableName();
					List<TableMetadata> baseTableMetadataList = tableColumnMap.get(t0);
					List<TableMetadata> tableMetadataList = tableColumnMap.get(t0);
					if (baseTableMetadataList == null) {
						throw new GeneratorException("Invalid update declaration: no metadata found for table "
								.concat(updateDeclaration.getTableName()));
					}
					updateDescriptor.setUpdateDeclaration(updateDeclaration);
					String beanName = GeneratorUtil.convertTableName2JavaName(t0, configuration.getTableNameMappings());
					if (configuration.getExtendedBeanMappings() != null) {
						final String extBeanName =
								GeneratorUtil.getMappedString(beanName, configuration.getExtendedBeanMappings());
						if (!extBeanName.isEmpty()) {
							beanName = extBeanName;
						}
					}
					updateDescriptor.setBeanName(beanName);
					int paramIndex = 1;
					for (final UpdateColumn updateColumn : updateDeclaration.getUpdateColumn()) {
						final ParameterDescriptor parameterDescriptor = new ParameterDescriptor();
						final String updateColumnName = updateColumn.getName();
						final String index = String.format("%02d", paramIndex++);
						parameterDescriptor.setJavaName(PARAMETER_PREFIX + index
								+ Utils.convertDBName2JavaName(updateColumnName, true));
						parameterDescriptor.setDbParamName(updateColumnName);
						String javaType = null;
						for (final TableMetadata tableMetadata : tableMetadataList) {
							if (tableMetadata.getColumnName().equals(updateColumnName)) {
								javaType = GeneratorUtil.convertDatabaseType2JavaType(
										tableMetadata.getJDBCType(),
										tableMetadata.getDecimalDigits(),
										tableMetadata.getTableName(),
										tableMetadata.getColumnName(),
										configuration.getTypeMappings());
							}
						}
						if (javaType == null) {
							throw new GeneratorException("Invalid update declaration: no metadata found for table "
									.concat(t0).concat(" and column ").concat(updateColumnName));
						}
						parameterDescriptor.setJavaType(javaType);
						updateDescriptor.addUpdateParameter(parameterDescriptor);
					}
					for (final QueryColumn queryColumn : updateDeclaration.getQueryColumn()) {
						final ParameterDescriptor parameterDescriptor = new ParameterDescriptor();
						final String columnName = queryColumn.getName();
						final String index = String.format("%02d", paramIndex++);
						parameterDescriptor.setJavaName
								(PARAMETER_PREFIX + index + Utils.convertDBName2JavaName(columnName, true));
						parameterDescriptor.setDbParamName(columnName);
						final String customExpression = queryColumn.getCustomExpression();
						parameterDescriptor.setCustomExpression(customExpression);
						if (customExpression == null && queryColumn.getOperator() == OperatorName.CUSTOM) {
							throw new GeneratorException("Invalid update declaration: custom expression missing for table "
									.concat(t0).concat(" and column ").concat(columnName));
						}
						if (customExpression != null && queryColumn.getOperator() != OperatorName.CUSTOM) {
							throw new GeneratorException("Invalid update declaration: custom expression not allowed for table "
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
							throw new GeneratorException("Invalid update declaration: no metadata found for table "
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
						updateDescriptor.addParameter(parameterDescriptor);
					}
					beanDMLClassDescriptor.addUpdate(updateDescriptor);
				}
			}

			for (final DeleteDeclaration deleteDeclaration : configuration.getDeleteDeclarations()) {
				if (deleteDeclaration.getTableName().equals(tableName0)) {
					if (deleteDeclaration.getGeneratedMethodName() == null) {
						deleteDeclaration.setGeneratedMethodName(GeneratorUtil.generateMethodName(deleteDeclaration,
								null, DELETE_METHOD_NAME_PREFIX, configuration));
					}
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
}
