package org.noorm.generator.servicegenerator;

import org.noorm.generator.GeneratorUtil;
import org.noorm.generator.ParameterDescriptor;
import org.noorm.generator.ValidatorClassDescriptor;
import org.noorm.generator.m2plugin.IParameters;
import org.noorm.generator.schema.GeneratorConfiguration;
import org.noorm.metadata.MetadataService;
import org.noorm.jdbc.Utils;
import org.noorm.metadata.beans.NameBean;
import org.noorm.metadata.beans.ParameterBean;
import org.noorm.jdbc.JDBCStatementProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
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
	private static final String ORACLE_REF_CURSOR_TYPE_NAME = "REF CURSOR";
	private static final String NOORM_ID_LIST_JAVA_TYPE_NAME = "Long[]";
	private static final String NOORM_METADATA_ID_RECORD = "NOORM_METADATA_ID_RECORD";
	private static final String SERVICE_VM_TEMPLATE_FILE = "/service.vm";
	private static final String SERVICE_VALIDATOR_VM_TEMPLATE_FILE = "/service_validator.vm";
	private static final String SERVICE_VALIDATOR_CLASS_NAME = "GenericServiceValidator";
	private static final String DEFAULT_PACKAGE_FILTER_REGEX = ".*";
	private static final String IGNORE_PACKAGE_FILTER_REGEX = "(NOORM_METADATA|NOORM_DYNAMIC_SQL)";
	private static final String DEFAULT_PAGEABLE_PROC_NAME_REGEX = "(find_pageable.*)";

	private IParameters parameters;
    private GeneratorConfiguration configuration;

    public ServiceGenerator(final IParameters pParameters, final GeneratorConfiguration pConfiguration) {
        parameters = pParameters;
        configuration = pConfiguration;
    }

	public void execute() {

		if (configuration.getServicePackageName() == null || configuration.getServicePackageName().isEmpty()) {
			throw new IllegalArgumentException("Parameter [servicePackageName] is null.");
		}
		if (configuration.getBeanPackageName() == null || configuration.getBeanPackageName().isEmpty()) {
			throw new IllegalArgumentException("Parameter [beanPackageName] is null.");
		}
		if (parameters.getDestinationDirectory() == null || !parameters.getDestinationDirectory().exists()) {
			throw new IllegalArgumentException("Parameter [destinationDirectory] is null or mis-configured.");
		}

		ValidatorClassDescriptor validatorClassDescriptor = new ValidatorClassDescriptor();
		validatorClassDescriptor.setPackageName(configuration.getServicePackageName());
        if (configuration.getDataSourceName() != null && !configuration.getDataSourceName().isEmpty()) {
            validatorClassDescriptor.setDataSourceName(configuration.getDataSourceName());
        }

        log.info("Generating NoORM Service classes.");
		final File servicePackageDir = GeneratorUtil.createPackageDir
				(parameters.getDestinationDirectory(), configuration.getServicePackageName());
		File serviceInterfacePackageDir = null;
		if (configuration.getServiceInterfacePackageName() != null &&
				!configuration.getServiceInterfacePackageName().isEmpty()) {
			serviceInterfacePackageDir = GeneratorUtil.createPackageDir
					(parameters.getDestinationDirectory(), configuration.getServiceInterfacePackageName());
		}

		String packageFilterRegex = configuration.getPackageFilterRegex();
		if (packageFilterRegex == null || packageFilterRegex.isEmpty()) {
			packageFilterRegex = DEFAULT_PACKAGE_FILTER_REGEX;
		}
		final MetadataService metadataService = MetadataService.getInstance();
		final List<NameBean> packageNames = metadataService.findPackageNames(packageFilterRegex);
		for (final NameBean packageName : packageNames) {
			if (packageName.getName().matches(IGNORE_PACKAGE_FILTER_REGEX)) {
				// Ignore the NoORM packages
				continue;
			}
			final ServiceClassDescriptor serviceClassDescriptor = new ServiceClassDescriptor();
			final String javaClassName = Utils.convertDBName2JavaName(packageName.getName(), true);
            if (configuration.getDataSourceName() != null && !configuration.getDataSourceName().isEmpty()) {
                serviceClassDescriptor.setDataSourceName(configuration.getDataSourceName());
            }
            serviceClassDescriptor.setJavaName(javaClassName);
			serviceClassDescriptor.setDatabasePackageName(packageName.getName().toLowerCase());
			serviceClassDescriptor.setInterfacePackageName(configuration.getServiceInterfacePackageName());
			serviceClassDescriptor.setPackageName(configuration.getServicePackageName());
			serviceClassDescriptor.setBeanPackageName(configuration.getBeanPackageName());
			final Integer codeHashValue = metadataService.getPackageHashValue(packageName.getName());
			serviceClassDescriptor.setCodeHashValue(codeHashValue);
			validatorClassDescriptor.getClassNames().add(javaClassName);
			final List<NameBean> procedureNames = metadataService.findProcedureNames(packageName.getName());
			for (NameBean procedureName : procedureNames) {
				final ProcedureDescriptor procedureDescriptor = new ProcedureDescriptor();
				procedureDescriptor.setOracleName(procedureName.getName().toLowerCase());
				final String javaMethodName = Utils.convertDBName2JavaName(procedureName.getName(), false);
				procedureDescriptor.setJavaName(javaMethodName);
				final List<ParameterBean> parameterList =
						metadataService.findProcedureParameters(packageName.getName(), procedureName.getName());
				for (final ParameterBean parameter : parameterList) {
					if (parameter.getDirection().equals(INPUT_PARAMETER)) {
						final ParameterDescriptor parameterDescriptor = new ParameterDescriptor();
						parameterDescriptor.setOracleName(parameter.getName().toLowerCase());
						final String javaParameterName = Utils.convertDBName2JavaName(parameter.getName(), false);
						parameterDescriptor.setJavaName(javaParameterName);
						final String oracleType = parameter.getDataType();
						final String oracleTypeName = parameter.getTypeName();
						if (oracleTypeName != null &&
								oracleTypeName.equals(JDBCStatementProcessor.NOORM_ID_LIST_ORACLE_TYPE_NAME)) {
							parameterDescriptor.setJavaType(NOORM_ID_LIST_JAVA_TYPE_NAME);
						} else {
							final String javaType = GeneratorUtil.convertOracleType2JavaType (oracleType,
                                    parameterDescriptor.getOracleName(), configuration.getCustomTypeMappings());
							parameterDescriptor.setJavaType(javaType);
						}
						procedureDescriptor.addParameter(parameterDescriptor);
						if (configuration.getPageableProcedureNameRegex() != null &&
								!configuration.getPageableProcedureNameRegex().isEmpty()) {
							if (procedureName.getName().toLowerCase().matches
									(configuration.getPageableProcedureNameRegex())) {
								procedureDescriptor.setPageableFinder(true);
							}
						} else {
							if (procedureName.getName().toLowerCase().matches(DEFAULT_PAGEABLE_PROC_NAME_REGEX)) {
								procedureDescriptor.setPageableFinder(true);
							}
						}
					} else { // OUT parameter
						procedureDescriptor.setHasOutParam(true);
						procedureDescriptor.setOutParamOracleName(parameter.getName().toLowerCase());
						if (parameter.getDataType().equals(ORACLE_REF_CURSOR_TYPE_NAME)) {
							final String rowTypeName = metadataService.getParameterRowtype
									(packageName.getName(), procedureName.getName(), parameter.getName());
							if (rowTypeName.equals(NOORM_METADATA_ID_RECORD)) {
								procedureDescriptor.setOutParamJavaType(Long.class.getSimpleName());
							} else {
								String javaBeanName = GeneratorUtil.convertTableName2BeanName
										(rowTypeName.toUpperCase(), configuration.getIgnoreTableNamePrefixes());
								if (configuration.getExtendedBeans() != null) {
									final String extJavaBeanName = GeneratorUtil.getPropertyString
                                            (javaBeanName, configuration.getExtendedBeans());
									if (!extJavaBeanName.isEmpty()) {
										javaBeanName = extJavaBeanName;
									}
								}
								procedureDescriptor.setOutParamJavaType(javaBeanName);
							}
							procedureDescriptor.setOutParamRefCursor(true);
							if (configuration.getSingleRowFinderRegex() != null &&
									!configuration.getSingleRowFinderRegex().isEmpty()) {
								if (procedureName.getName().toLowerCase().matches
										(configuration.getSingleRowFinderRegex())) {
									procedureDescriptor.setSingleRowFinder(true);
								}
							}
						} else {
							final String javaType = GeneratorUtil.convertOracleType2JavaType (parameter.getDataType(),
                                    parameter.getName(), configuration.getCustomTypeMappings());
							procedureDescriptor.setOutParamJavaType(javaType);
							procedureDescriptor.setOutParamScalar(true);
						}
					}
				}
				serviceClassDescriptor.addProcedure(procedureDescriptor);
			}
			GeneratorUtil.generateFile(servicePackageDir, SERVICE_VM_TEMPLATE_FILE,
					serviceClassDescriptor.getJavaName(), serviceClassDescriptor);
			if (configuration.getServiceInterfacePackageName() != null &&
					!configuration.getServiceInterfacePackageName().isEmpty()) {
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
