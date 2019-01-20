package org.noorm.generator.beangenerator;

import org.noorm.generator.GeneratorException;
import org.noorm.generator.GeneratorUtil;
import org.noorm.generator.IParameters;
import org.noorm.generator.schema.GeneratorConfiguration;
import org.noorm.jdbc.DataSourceProvider;
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
