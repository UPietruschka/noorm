package org.noorm.generator.servicegenerator;

import org.noorm.generator.GeneratorUtil;
import org.noorm.generator.ParameterDescriptor;
import org.noorm.generator.ValidatorClassDescriptor;
import org.noorm.generator.IParameters;
import org.noorm.generator.schema.GeneratorConfiguration;
import org.noorm.generator.schema.Regex;
import org.noorm.jdbc.DataSourceProvider;
import org.noorm.jdbc.Utils;
import org.noorm.jdbc.platform.IMetadata;
import org.noorm.jdbc.platform.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.JDBCType;
import java.util.List;

/**
 * Generator for database related service source files based on Velocity templates.
 *
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 19.04.11
 *         Time: 14:48
 */
public class ServiceGenerator {

	private static final Logger log = LoggerFactory.getLogger(ServiceGenerator.class);
	private static final String INPUT_PARAMETER = "IN";
	private static final String NOORM_ID_LIST_JAVA_TYPE_NAME = "Long[]";
	private static final String NOORM_METADATA_ID_RECORD = "NOORM_METADATA_ID_RECORD";
	private static final String SERVICE_VM_TEMPLATE_FILE = "/service.vm";
	private static final String SERVICE_VALIDATOR_VM_TEMPLATE_FILE = "/service_validator.vm";
	private static final String SERVICE_VALIDATOR_CLASS_NAME = "GenericServiceValidator";
	private static final String DEFAULT_PACKAGE_FILTER_REGEX = ".*";
	private static final String IGNORE_PACKAGE_FILTER_REGEX = "(NOORM_METADATA)";
    private static final String NOORM_ID_LIST_DB_TYPE_NAME = "NUM_ARRAY";

	private IParameters parameters;
    private GeneratorConfiguration configuration;

    public ServiceGenerator(final IParameters pParameters, final GeneratorConfiguration pConfiguration) {
        parameters = pParameters;
        configuration = pConfiguration;
    }

	public void execute() {

        if (!GeneratorUtil.hasServicePackageName(configuration)) {
			throw new IllegalArgumentException("Parameter [servicePackageName] is null.");
		}
        if (!GeneratorUtil.hasBeanPackageName(configuration)) {
			throw new IllegalArgumentException("Parameter [beanPackageName] is null.");
		}
		if (parameters.getDestinationDirectory() == null || !parameters.getDestinationDirectory().exists()) {
			throw new IllegalArgumentException("Parameter [destinationDirectory] is null or mis-configured.");
		}

		ValidatorClassDescriptor validatorClassDescriptor = new ValidatorClassDescriptor();
		validatorClassDescriptor.setPackageName(configuration.getServiceJavaPackage().getName());
        if (GeneratorUtil.hasDataSourceName(configuration)) {
            validatorClassDescriptor.setDataSourceName(configuration.getDataSource().getName());
        }

        log.info("Generating NoORM Service classes.");
		final File servicePackageDir = GeneratorUtil.createPackageDir
				(parameters.getDestinationDirectory(), configuration.getServiceJavaPackage().getName());
		File serviceInterfacePackageDir = null;
        if (GeneratorUtil.hasServiceInterfacePackageName(configuration)) {
			serviceInterfacePackageDir = GeneratorUtil.createPackageDir
					(parameters.getDestinationDirectory(), configuration.getServiceInterfaceJavaPackage().getName());
		}

        String packageFilterRegex = DEFAULT_PACKAGE_FILTER_REGEX;
        final Regex packageFilter = configuration.getPackageFilter();
		if (packageFilter != null && packageFilter.getRegex() != null) {
			packageFilterRegex = packageFilter.getRegex();
		}
        final IMetadata metadata = DataSourceProvider.getPlatform().getMetadata();
		final List<String> packageNames = metadata.findPackageNames(packageFilterRegex);
		for (final String packageName : packageNames) {
			if (packageName.matches(IGNORE_PACKAGE_FILTER_REGEX)) {
				// Ignore the NoORM packages
				continue;
			}
			final ServiceClassDescriptor serviceClassDescriptor = new ServiceClassDescriptor();
			final String javaClassName = Utils.convertDBName2JavaName(packageName, true);
            if (GeneratorUtil.hasDataSourceName(configuration)) {
                serviceClassDescriptor.setDataSourceName(configuration.getDataSource().getName());
            }
            serviceClassDescriptor.setJavaName(javaClassName);
			serviceClassDescriptor.setDatabasePackageName(packageName.toLowerCase());
            if (GeneratorUtil.hasServiceInterfacePackageName(configuration)) {
                serviceClassDescriptor.setInterfacePackageName
                        (configuration.getServiceInterfaceJavaPackage().getName());
            }
			serviceClassDescriptor.setPackageName(configuration.getServiceJavaPackage().getName());
			serviceClassDescriptor.setBeanPackageName(configuration.getBeanJavaPackage().getName());
			final Integer codeHashValue = metadata.getPackageHashValue(packageName);
			serviceClassDescriptor.setCodeHashValue(codeHashValue);
			validatorClassDescriptor.getClassNames().add(javaClassName);
			final List<String> procedureNames = metadata.findProcedureNames(packageName);
			for (String procedureName : procedureNames) {
				final ProcedureDescriptor procedureDescriptor = new ProcedureDescriptor();
				procedureDescriptor.setDbProcedureName(procedureName.toLowerCase());
				final String javaMethodName = Utils.convertDBName2JavaName(procedureName, false);
				procedureDescriptor.setJavaName(javaMethodName);
				final List<Parameter> parameterList =
                        metadata.findProcedureParameters(packageName, procedureName);
				for (final Parameter parameter : parameterList) {
					if (parameter.getDirection().equals(INPUT_PARAMETER)) {
						final ParameterDescriptor parameterDescriptor = new ParameterDescriptor();
						parameterDescriptor.setDbParamName(parameter.getName().toLowerCase());
						final String javaParameterName = Utils.convertDBName2JavaName(parameter.getName(), false);
						parameterDescriptor.setJavaName(javaParameterName);
						final JDBCType jdbcType = parameter.getJDBCType();
						final String databaseTypeName = parameter.getTypeName();
						if (databaseTypeName != null &&
								databaseTypeName.equals(NOORM_ID_LIST_DB_TYPE_NAME)) {
							parameterDescriptor.setJavaType(NOORM_ID_LIST_JAVA_TYPE_NAME);
						} else {
							final String javaType = GeneratorUtil.convertDatabaseType2JavaType(jdbcType,
                                    parameterDescriptor.getDbParamName(), configuration.getTypeMappings());
							parameterDescriptor.setJavaType(javaType);
						}
						procedureDescriptor.addParameter(parameterDescriptor);
					} else { // OUT parameter
						procedureDescriptor.setHasOutParam(true);
						procedureDescriptor.setOutDbParamName(parameter.getName().toLowerCase());
						if (parameter.getJDBCType().equals(JDBCType.REF_CURSOR)) {
							final String rowTypeName = metadata.getParameterRowtype
									(packageName, procedureName, parameter.getName());
							if (rowTypeName.equals(NOORM_METADATA_ID_RECORD)) {
								procedureDescriptor.setOutParamJavaType(Long.class.getSimpleName());
							} else {
								String javaBeanName = GeneratorUtil.convertTableName2JavaName
                                        (rowTypeName.toUpperCase(), configuration.getTableNameMappings());
								if (configuration.getExtendedBeanMappings() != null) {
									final String extJavaBeanName = GeneratorUtil.getMappedString
                                            (javaBeanName, configuration.getExtendedBeanMappings());
									if (!extJavaBeanName.isEmpty()) {
										javaBeanName = extJavaBeanName;
									}
								}
								procedureDescriptor.setOutParamJavaType(javaBeanName);
							}
							procedureDescriptor.setOutParamRefCursor(true);
                            final Regex singleRowFinder = configuration.getSingleRowFinderProcedureFilter();
							if (singleRowFinder != null) {
								if (procedureName.toLowerCase().matches(singleRowFinder.getRegex())) {
									procedureDescriptor.setSingleRowFinder(true);
								}
							}
						} else {
							final String javaType = GeneratorUtil.convertDatabaseType2JavaType(parameter.getJDBCType(),
                                    parameter.getName(), configuration.getTypeMappings());
							procedureDescriptor.setOutParamJavaType(javaType);
							procedureDescriptor.setOutParamScalar(true);
						}
					}
				}
				serviceClassDescriptor.addProcedure(procedureDescriptor);
			}
			GeneratorUtil.generateFile(servicePackageDir, SERVICE_VM_TEMPLATE_FILE,
					serviceClassDescriptor.getJavaName(), serviceClassDescriptor);
            if (GeneratorUtil.hasServiceInterfacePackageName(configuration)) {
				serviceClassDescriptor.setInterface(true);
				GeneratorUtil.generateFile(serviceInterfacePackageDir, SERVICE_VM_TEMPLATE_FILE,
						serviceClassDescriptor.getJavaInterfaceName(), serviceClassDescriptor);
			}
		}
		if (!validatorClassDescriptor.getClassNames().isEmpty()) {
			GeneratorUtil.generateFile(servicePackageDir, SERVICE_VALIDATOR_VM_TEMPLATE_FILE,
					SERVICE_VALIDATOR_CLASS_NAME, validatorClassDescriptor);
		}
	}
}
