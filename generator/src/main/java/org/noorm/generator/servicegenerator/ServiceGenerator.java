package org.noorm.generator.servicegenerator;

import org.noorm.generator.GeneratorException;
import org.noorm.generator.GeneratorUtil;
import org.noorm.generator.ValidatorClassDescriptor;
import org.noorm.metadata.MetadataService;
import org.noorm.jdbc.Utils;
import org.noorm.metadata.beans.NameBean;
import org.noorm.metadata.beans.ParameterBean;
import org.noorm.jdbc.JDBCStatementProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.Properties;

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
	private static final String IGNORE_PACKAGE_FILTER_REGEX = "(NOORM_METADATA|DYNAMIC_SQL)";
	private static final String DEFAULT_PAGEABLE_PROC_NAME_REGEX = "(find_pageable.*)";
	private static ServiceGenerator serviceGenerator;

	/**
	 * Destination directory for generated source files.
	 */
	private File destinationDirectory;

	protected ServiceGenerator() {
	}

	/**
	 * Package name for generated Service source files.
	 */
	private String servicePackageName;

	/**
	 * Package name for generated Bean source files.
	 */
	private String beanPackageName;

	/**
	 * Regular expression to filter packages for service generation.
	 */
	private String packageFilterRegex;

	/**
	 * List of table name prefixes to be ignored for java class name construction.
	 * Some data modelers use a common table name prefix to identify tables of a
	 * given schema or group. When those prefixes are not desired in the constructed
	 * java class name, they should be listed here.
	 */
	private List<String> ignoreTableNamePrefixes;

	/**
	 * Accessing data mapped to Beans always uses an Oracle reference cursor to
	 * retrieve the data. However, using the PL/SQL procedures signature does
	 * not provide any hint, if the reference cursor being returned is limited
	 * to a single row (which in turn changes the signature of the generated Java
	 * code, instead of a List a single Bean is returned).
	 * <p/>
	 * Use this parameter to specify a regular expression matching all procedure
	 * names subject to single row retrieval.
	 */
	private String singleRowFinderRegex;

	/**
	 * Large query results can be mapped into a PageableBeanList to provide efficient
	 * access to the data by loading the full record only for the requested page.
	 *
	 * @parameter
	 */
	private String pageableProcedureNameRegex;

	/**
	 * Beans generated from database entities are often subject to data enrichment in
	 * the service utilizing the bean data. One option to add additional data to the
	 * bean is the generic (generated) bean property "auxiliaryData". However, some
	 * data consumers may require data provided in a single bean without nested data
	 * (i.e., the additional data is available using standard bean properties).
	 * As an alternative approach to the auxiliary data property, the user may create
	 * a subclass for the generated bean with additional bean properties. To utilize
	 * this inherited bean classes, the generated services using the originally
	 * generated class should use the subclass. This parameter allows for a mapping
	 * of originally generated bean classes to data enriched subclasses. Note that
	 * the subclass must be fully classified.
	 */
	private Properties extendedBeans;

	public void setDestinationDirectory(final File pDestinationDirectory) {
		destinationDirectory = pDestinationDirectory;
	}

	public void setServicePackageName(final String pServicePackageName) {
		servicePackageName = pServicePackageName;
	}

	public void setBeanPackageName(final String pBeanPackageName) {
		beanPackageName = pBeanPackageName;
	}

	public void setPackageFilterRegex(final String pPackageFilterRegex) {
		packageFilterRegex = pPackageFilterRegex;
	}

	public void setIgnoreTableNamePrefixes(final List<String> pIgnoreTableNamePrefixes) {
		ignoreTableNamePrefixes = pIgnoreTableNamePrefixes;
	}

	public void setSingleRowFinderRegex(final String pSingleRowFinderRegex) {
		singleRowFinderRegex = pSingleRowFinderRegex;
	}

	public void setPageableProcedureNameRegex(final String pPageableProcedureNameRegex) {
		pageableProcedureNameRegex = pPageableProcedureNameRegex;
	}

	public void setExtendedBeans(final Properties pExtendedBeans) {
		extendedBeans = pExtendedBeans;
	}

	public static ServiceGenerator getInstance() {

		synchronized (ServiceGenerator.class) {
			if (serviceGenerator == null) {
				serviceGenerator = new ServiceGenerator();
			}
		}
		return serviceGenerator;
	}

	public void execute() {

		if (servicePackageName == null || servicePackageName.isEmpty()) {
			throw new IllegalArgumentException("Parameter [servicePackageName] is null.");
		}
		if (beanPackageName == null || beanPackageName.isEmpty()) {
			throw new IllegalArgumentException("Parameter [beanPackageName] is null.");
		}
		if (destinationDirectory == null || !destinationDirectory.exists()) {
			throw new IllegalArgumentException("Parameter [destinationDirectory] is null or mis-configured.");
		}

		ValidatorClassDescriptor validatorClassDescriptor = new ValidatorClassDescriptor();
		validatorClassDescriptor.setPackageName(servicePackageName);

		log.info("Generating NoORM Service classes.");
		final File servicePackageDir =
				new File(destinationDirectory, servicePackageName.replace(".", File.separator));
		if (!servicePackageDir.exists()) {
			if (!servicePackageDir.mkdirs()) {
				throw new GeneratorException("Could not create directory ".concat(servicePackageDir.toString()));
			}
		}

		final MetadataService metadataService = MetadataService.getInstance();
		if (packageFilterRegex == null || packageFilterRegex.isEmpty()) {
			setPackageFilterRegex(DEFAULT_PACKAGE_FILTER_REGEX);
		}
		final List<NameBean> packageNames = metadataService.findPackageNames(packageFilterRegex);
		for (final NameBean packageName : packageNames) {
			if (packageName.getName().matches(IGNORE_PACKAGE_FILTER_REGEX)) {
				// Ignore the NoORM packages
				continue;
			}
			final ServiceClassDescriptor serviceClassDescriptor = new ServiceClassDescriptor();
			final String javaClassName = Utils.convertDBName2JavaName(packageName.getName(), true);
			serviceClassDescriptor.setJavaName(javaClassName);
			serviceClassDescriptor.setDatabasePackageName(packageName.getName().toLowerCase());
			serviceClassDescriptor.setPackageName(servicePackageName);
			serviceClassDescriptor.setBeanPackageName(beanPackageName);
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
						final String oracleType = parameter.getTypeName();
						if (oracleType.equals(JDBCStatementProcessor.NOORM_ID_LIST_ORACLE_TYPE_NAME)) {
							parameterDescriptor.setJavaType(NOORM_ID_LIST_JAVA_TYPE_NAME);
						} else {
							final String javaType = Utils.convertOracleType2JavaType(oracleType, null, null);
							parameterDescriptor.setJavaType(javaType);
						}
						procedureDescriptor.addParameter(parameterDescriptor);
						if (pageableProcedureNameRegex != null && !pageableProcedureNameRegex.isEmpty()) {
							if (procedureName.getName().toLowerCase().matches(pageableProcedureNameRegex)) {
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
						if (parameter.getTypeName().equals(ORACLE_REF_CURSOR_TYPE_NAME)) {
							final String rowTypeName = metadataService.getParameterRowtype
									(packageName.getName(), procedureName.getName(), parameter.getName());
							if (rowTypeName.equals(NOORM_METADATA_ID_RECORD)) {
								procedureDescriptor.setOutParamJavaType(Long.class.getSimpleName());
							} else {
								String javaBeanName = Utils.convertTableName2BeanName
										(rowTypeName.toUpperCase(), ignoreTableNamePrefixes);
								if (extendedBeans != null) {
									final String extJavaBeanName = Utils.getPropertyString(javaBeanName, extendedBeans);
									if (!extJavaBeanName.isEmpty()) {
										javaBeanName = extJavaBeanName;
									}
								}
								procedureDescriptor.setOutParamJavaType(javaBeanName);
								String javaShortName = Utils.convertTableName2JavaName
										(rowTypeName.toUpperCase(), ignoreTableNamePrefixes);
								serviceClassDescriptor.addReturnTypeName(javaShortName, javaBeanName);
							}
							procedureDescriptor.setOutParamRefCursor(true);
							if (singleRowFinderRegex != null && !singleRowFinderRegex.isEmpty()) {
								if (procedureName.getName().toLowerCase().matches(singleRowFinderRegex)) {
									procedureDescriptor.setSingleRowFinder(true);
								}
							}
						} else {
							final String javaType =
									Utils.convertOracleType2JavaType(parameter.getTypeName(), null, null);
							procedureDescriptor.setOutParamJavaType(javaType);
							procedureDescriptor.setOutParamScalar(true);
						}
					}
				}
				serviceClassDescriptor.addProcedure(procedureDescriptor);
			}
			GeneratorUtil.generateFile(servicePackageDir, SERVICE_VM_TEMPLATE_FILE,
					serviceClassDescriptor.getJavaName(), serviceClassDescriptor);
		}
		if (!validatorClassDescriptor.getClassNames().isEmpty()) {
			GeneratorUtil.generateFile(servicePackageDir, SERVICE_VALIDATOR_VM_TEMPLATE_FILE,
					SERVICE_VALIDATOR_CLASS_NAME, validatorClassDescriptor);
		}
	}
}
