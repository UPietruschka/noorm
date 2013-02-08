package org.noorm.generator.querygenerator;

import org.noorm.generator.GeneratorException;
import org.noorm.generator.GeneratorUtil;
import org.noorm.generator.ParameterDescriptor;
import org.noorm.generator.m2plugin.IParameters;
import org.noorm.generator.m2plugin.QueryDeclaration;
import org.noorm.jdbc.QueryColumn;
import org.noorm.jdbc.Utils;
import org.noorm.metadata.MetadataService;
import org.noorm.metadata.beans.TableMetadataBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 21.01.13
 *         Time: 19:50
 */
public class QueryGenerator {

    private static final Logger log = LoggerFactory.getLogger(QueryGenerator.class);
    private static final String DECLARED_QUERIES_DEFAULT_CLASS_NAME = "DeclaredQueries";
    private static final String PARAMETER_PREFIX = "p";
    private static final String DEFAULT_METHOD_NAME_PREFIX = "find";
    private static final String DEFAULT_METHOD_NAME_PART3 = "By";
    private static final String QUERY_VM_TEMPLATE_FILE = "/declared_queries.vm";
    private static final String QUERY_VALIDATOR_VM_TEMPLATE_FILE = "/declared_queries_validator.vm";
    private static final String QUERY_VALIDATOR_CLASS_NAME = "GenericQueryValidator";

    private IParameters parameters;

    public QueryGenerator(final IParameters pParameters) {
        parameters = pParameters;
    }

    public void execute() {

        if (parameters.getServicePackageName() == null || parameters.getServicePackageName().isEmpty()) {
            throw new IllegalArgumentException("Parameter [servicePackageName] is null.");
        }
        if (parameters.getBeanPackageName() == null || parameters.getBeanPackageName().isEmpty()) {
            throw new IllegalArgumentException("Parameter [beanPackageName] is null.");
        }
        if (parameters.getDestinationDirectory() == null || !parameters.getDestinationDirectory().exists()) {
            throw new IllegalArgumentException("Parameter [destinationDirectory] is null or mis-configured.");
        }

        log.info("Generating NoORM Query Declaration class.");
        final File servicePackageDir = GeneratorUtil.createPackageDir
                (parameters.getDestinationDirectory(), parameters.getServicePackageName());
        File serviceInterfacePackageDir = null;
        if (parameters.getServiceInterfacePackageName() != null &&
                !parameters.getServiceInterfacePackageName().isEmpty()) {
            serviceInterfacePackageDir = GeneratorUtil.createPackageDir
                    (parameters.getDestinationDirectory(), parameters.getServiceInterfacePackageName());
        }

        final MetadataService metadataService = MetadataService.getInstance();
        log.info("Retrieving table metadata from Oracle database.");
        final Map<String, List<TableMetadataBean>> tableColumnMap = metadataService.findTableMetadata();

        final Map<String, QueryClassDescriptor> queryClasses = new HashMap<String, QueryClassDescriptor>();
        for (final QueryDeclaration queryDeclaration : parameters.getQueryDeclarations()) {
            generateMethodName(queryDeclaration);
            final QueryDescriptor queryDescriptor = new QueryDescriptor();
            String t0 = queryDeclaration.getTableName();
            final List<TableMetadataBean> tableMetadataBeanList = tableColumnMap.get(t0.toUpperCase());
            if (queryDeclaration.getBaseTable() != null && !queryDeclaration.getBaseTable().isEmpty()) {
                t0 = queryDeclaration.getBaseTable();
            }
            if (tableMetadataBeanList == null) {
                throw new GeneratorException("Illegal query declaration: no metadata found for table ".concat(t0));
            }
            queryDescriptor.setQueryDeclaration(queryDeclaration);
            queryDescriptor.setBeanName(Utils.convertTableName2BeanName
                    (t0, parameters.getIgnoreTableNamePrefixes()));
            queryDescriptor.setBeanShortName(Utils.convertTableName2ShortName
                    (t0, parameters.getIgnoreTableNamePrefixes()));
            for (final QueryColumn queryColumn : queryDeclaration.getQueryColumns()) {
                final ParameterDescriptor parameterDescriptor = new ParameterDescriptor();
                final String columnName = queryColumn.getColumnName().toUpperCase();
                parameterDescriptor.setJavaName(PARAMETER_PREFIX + Utils.convertDBName2JavaName(columnName, true));
                parameterDescriptor.setOracleName(columnName);
                String javaType = null;
                for (final TableMetadataBean tableMetadataBean : tableMetadataBeanList) {
                    if (tableMetadataBean.getColumnName().equals(columnName))
                    javaType = Utils.convertOracleType2JavaType(tableMetadataBean.getDataType(),
                            tableMetadataBean.getDataPrecision(), tableMetadataBean.getDataScale());
                }
                if (javaType == null) {
                    throw new GeneratorException("Illegal query declaration: no metadata found for table ".concat(t0)
                            .concat(" and column ").concat(columnName));
                }
                parameterDescriptor.setJavaType(javaType);
                queryDescriptor.addParameter(parameterDescriptor);
            }
            String javaName = queryDeclaration.getClassName();
            if (javaName == null || javaName.isEmpty()) {
                javaName = DECLARED_QUERIES_DEFAULT_CLASS_NAME;
            }
            QueryClassDescriptor queryClassDescriptor = queryClasses.get(javaName);
            if (queryClassDescriptor == null) {
                queryClassDescriptor = new QueryClassDescriptor();
                if (parameters.getDataSourceName() != null && !parameters.getDataSourceName().isEmpty()) {
                    queryClassDescriptor.setDataSourceName(parameters.getDataSourceName());
                }
                queryClassDescriptor.setInterfacePackageName(parameters.getServiceInterfacePackageName());
                queryClassDescriptor.setPackageName(parameters.getServicePackageName());
                queryClassDescriptor.setBeanPackageName(parameters.getBeanPackageName());
                queryClassDescriptor.setJavaName(javaName);
                queryClasses.put(javaName, queryClassDescriptor);
            }
            queryClassDescriptor.addQuery(queryDescriptor);
        }
        for (final QueryClassDescriptor queryClassDescriptor : queryClasses.values()) {
            GeneratorUtil.generateFile(servicePackageDir, QUERY_VM_TEMPLATE_FILE,
                    queryClassDescriptor.getJavaName(), queryClassDescriptor);
            if (parameters.getServiceInterfacePackageName() != null &&
                    !parameters.getServiceInterfacePackageName().isEmpty()) {
                queryClassDescriptor.setInterface(true);
                GeneratorUtil.generateFile(serviceInterfacePackageDir,
                        QUERY_VM_TEMPLATE_FILE, queryClassDescriptor.getJavaInterfaceName(), queryClassDescriptor);
            }
        }
    }

    private void generateMethodName(final QueryDeclaration pQueryDeclaration) {

        if (pQueryDeclaration.getMethodName() == null || pQueryDeclaration.getMethodName().isEmpty()) {
            final StringBuilder methodName = new StringBuilder();
            methodName.append(DEFAULT_METHOD_NAME_PREFIX);
            String t0 = pQueryDeclaration.getTableName();
            if (pQueryDeclaration.getBaseTable() != null && !pQueryDeclaration.getBaseTable().isEmpty()) {
                t0 = pQueryDeclaration.getBaseTable();
            }
            final String javaTableName = Utils.convertTableName2JavaName(t0, parameters.getIgnoreTableNamePrefixes());
            methodName.append(javaTableName);
            if (!pQueryDeclaration.getQueryColumns().isEmpty()) {
                methodName.append(DEFAULT_METHOD_NAME_PART3);
                // With an increasing number of parameters, we use a substring of decreasing length of the
                // parameter name for method name construction
                int substringLength = 16;
                if (pQueryDeclaration.getQueryColumns().size() > 1) { substringLength = 8; }
                if (pQueryDeclaration.getQueryColumns().size() > 2) { substringLength = 4; }
                if (pQueryDeclaration.getQueryColumns().size() > 4) { substringLength = 2; }
                if (pQueryDeclaration.getQueryColumns().size() > 8) { substringLength = 1; }
                for (final QueryColumn queryColumn : pQueryDeclaration.getQueryColumns()) {
                    final String javaColumnName = Utils.convertDBName2JavaName(queryColumn.getColumnName(), true);
                    methodName.append(javaColumnName, 0, Math.min(substringLength, javaColumnName.length()));
                }
            }
            pQueryDeclaration.setMethodName(methodName.toString());
        }
    }
}