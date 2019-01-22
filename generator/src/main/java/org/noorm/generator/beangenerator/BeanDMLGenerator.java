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

			String beanName = GeneratorUtil.convertTableName2JavaName(tableName0, configuration.getTableNameMappings());
			final BeanDMLClassDescriptor beanDMLClassDescriptor = new BeanDMLClassDescriptor();
			beanDMLClassDescriptor.setBeanPackageName(configuration.getBeanJavaPackage().getName());
			beanDMLClassDescriptor.setName(beanName);
			beanDMLClassDescriptor.setJavaName(beanName + "DML");
			if (configuration.getExtendedBeanMappings() != null) {
				final String extJavaBeanName =
						GeneratorUtil.getMappedString(beanName, configuration.getExtendedBeanMappings());
				if (!extJavaBeanName.isEmpty()) {
					beanDMLClassDescriptor.setExtendedName(extJavaBeanName);
					beanName = extJavaBeanName;
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

			final List<TableMetadata> tableMetadataList = tableColumnMap.get(tableName0);

			for (final UpdateDeclaration updateDeclaration : configuration.getUpdateDeclarations()) {
				if (updateDeclaration.getTableName().equals(tableName0)) {
					final UpdateDescriptor updateDescriptor = new UpdateDescriptor();
					int paramIndex = 1;
					for (final UpdateColumn updateColumn : updateDeclaration.getUpdateColumn()) {
						final ParameterDescriptor parameterDescriptor = new ParameterDescriptor();
						final String updateColumnName = updateColumn.getName();
						final String index = String.format("%02d", paramIndex++);
						parameterDescriptor.setJavaName(GeneratorUtil.PARAMETER_PREFIX + index
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
									.concat(tableName0).concat(" and column ").concat(updateColumnName));
						}
						parameterDescriptor.setJavaType(javaType);
						updateDescriptor.addUpdateParameter(parameterDescriptor);
					}
					if (updateDeclaration.getGeneratedMethodName() == null) {
						updateDeclaration.setGeneratedMethodName(GeneratorUtil.generateMethodName(updateDeclaration,
								null, UPDATE_METHOD_NAME_PREFIX, configuration));
					}
					updateDescriptor.setSearchDeclaration(updateDeclaration);
					updateDescriptor.setBeanName(beanName);
					GeneratorUtil.processSearchColumns
							(updateDescriptor, tableMetadataList, configuration.getTypeMappings(), paramIndex);
					beanDMLClassDescriptor.addUpdate(updateDescriptor);
				}
			}

			for (final DeleteDeclaration deleteDeclaration : configuration.getDeleteDeclarations()) {
				if (deleteDeclaration.getTableName().equals(tableName0)) {
					final SearchDescriptor deleteDescriptor = new SearchDescriptor();
					if (deleteDeclaration.getGeneratedMethodName() == null) {
						deleteDeclaration.setGeneratedMethodName(GeneratorUtil.generateMethodName(deleteDeclaration,
								null, DELETE_METHOD_NAME_PREFIX, configuration));
					}
					deleteDescriptor.setSearchDeclaration(deleteDeclaration);
					deleteDescriptor.setBeanName(beanName);
					GeneratorUtil.processSearchColumns
							(deleteDescriptor, tableMetadataList, configuration.getTypeMappings(), 1);
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
