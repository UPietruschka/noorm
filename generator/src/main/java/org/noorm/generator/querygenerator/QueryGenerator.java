package org.noorm.generator.querygenerator;

import org.noorm.generator.*;
import org.noorm.generator.schema.GeneratorConfiguration;
import org.noorm.generator.schema.OperatorName;
import org.noorm.generator.schema.QueryColumn;
import org.noorm.generator.schema.QueryDeclaration;
import org.noorm.jdbc.DataSourceProvider;
import org.noorm.jdbc.Utils;
import org.noorm.jdbc.platform.IMetadata;
import org.noorm.jdbc.platform.TableMetadata;
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
    private static final String QUERY_VM_TEMPLATE_FILE = "/declared_queries.vm";

    private IParameters parameters;
    private GeneratorConfiguration configuration;

    public QueryGenerator(final IParameters pParameters, final GeneratorConfiguration pConfiguration) {
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

        log.info("Generating NoORM Query Declaration class.");
        final File servicePackageDir = GeneratorUtil.createPackageDir
                (parameters.getDestinationDirectory(), configuration.getServiceJavaPackage().getName());
        File serviceInterfacePackageDir = null;
        if (GeneratorUtil.hasServiceInterfacePackageName(configuration)) {
            serviceInterfacePackageDir = GeneratorUtil.createPackageDir
                    (parameters.getDestinationDirectory(), configuration.getServiceInterfaceJavaPackage().getName());
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

        final Map<String, QueryClassDescriptor> queryClasses = new HashMap<String, QueryClassDescriptor>();
        for (final QueryDeclaration queryDeclaration : configuration.getQueryDeclarations()) {
            if (queryDeclaration.getGeneratedMethodName() == null) {
                queryDeclaration.setGeneratedMethodName(GeneratorUtil.generateMethodName(queryDeclaration,
                        queryDeclaration.getBaseTable(), DEFAULT_METHOD_NAME_PREFIX, configuration));
            }
            final QueryDescriptor queryDescriptor = new QueryDescriptor();
            String t0 = queryDeclaration.getTableName();
            List<TableMetadata> baseTableMetadataList = tableColumnMap.get(t0);
            List<TableMetadata> tableMetadataList = tableColumnMap.get(t0);
            if (queryDeclaration.getBaseTable() != null && !queryDeclaration.getBaseTable().isEmpty()) {
                t0 = queryDeclaration.getBaseTable();
                if (tableColumnMap.get(t0) == null) {
                    throw new GeneratorException("Invalid query declaration: no metadata found for table ".concat(t0));
                }
                baseTableMetadataList = tableColumnMap.get(t0);
                // With a base table definition, we do not necessarily have the required metadata for the query
                // view in the set of filtered table metadata. Thus, we issue the metadata query again for the
                // view metadata.
                final String viewName = queryDeclaration.getTableName();
                final Map<String, List<TableMetadata>> viewColumnMap =
                        metadata.findTableMetadata(schemaFilterRegex, viewName);
                tableMetadataList = viewColumnMap.get(viewName);
            } else {
                if (baseTableMetadataList == null) {
                    throw new GeneratorException("Invalid query declaration: no metadata found for table "
                            .concat(queryDeclaration.getTableName()));
                }
            }
            queryDescriptor.setQueryDeclaration(queryDeclaration);
            String beanName = GeneratorUtil.convertTableName2JavaName(t0, configuration.getTableNameMappings());
            if (configuration.getExtendedBeanMappings() != null) {
                final String extBeanName =
                        GeneratorUtil.getMappedString(beanName, configuration.getExtendedBeanMappings());
                if (!extBeanName.isEmpty()) {
                    beanName = extBeanName;
                }
            }
            queryDescriptor.setBeanName(beanName);
            int paramIndex = 1;
            for (final QueryColumn queryColumn : queryDeclaration.getQueryColumn()) {
                final ParameterDescriptor parameterDescriptor = new ParameterDescriptor();
                final String columnName = queryColumn.getName();
                final String index = String.format("%02d", paramIndex++);
                parameterDescriptor.setJavaName
                        (PARAMETER_PREFIX + index + Utils.convertDBName2JavaName(columnName, true));
                parameterDescriptor.setDbParamName(columnName);
                final String customExpression = queryColumn.getCustomExpression();
                parameterDescriptor.setCustomExpression(customExpression);
                if (customExpression == null && queryColumn.getOperator() == OperatorName.CUSTOM) {
                    throw new GeneratorException("Invalid query declaration: custom expression missing for table "
                            .concat(t0).concat(" and column ").concat(columnName));
                }
                if (customExpression != null && queryColumn.getOperator() != OperatorName.CUSTOM) {
                    throw new GeneratorException("Invalid query declaration: custom expression not allowed for table "
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
                    throw new GeneratorException("Invalid query declaration: no metadata found for table "
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
                queryDescriptor.addParameter(parameterDescriptor);
            }
            String javaName = queryDeclaration.getGeneratedClassName();
            if (javaName == null || javaName.isEmpty()) {
                javaName = DECLARED_QUERIES_DEFAULT_CLASS_NAME;
            }
            QueryClassDescriptor queryClassDescriptor = queryClasses.get(javaName);
            if (queryClassDescriptor == null) {
                queryClassDescriptor = new QueryClassDescriptor();
                if (GeneratorUtil.hasDataSourceName(configuration)) {
                    queryClassDescriptor.setDataSourceName(configuration.getDataSource().getName());
                }
                if (GeneratorUtil.hasServiceInterfacePackageName(configuration)) {
                    queryClassDescriptor.setInterfacePackageName
                            (configuration.getServiceInterfaceJavaPackage().getName());
                }
                queryClassDescriptor.setPackageName(configuration.getServiceJavaPackage().getName());
                queryClassDescriptor.setBeanPackageName(configuration.getBeanJavaPackage().getName());
                queryClassDescriptor.setJavaName(javaName);
                queryClasses.put(javaName, queryClassDescriptor);
            }
            queryClassDescriptor.addQuery(queryDescriptor);
        }
        for (final QueryClassDescriptor queryClassDescriptor : queryClasses.values()) {
            GeneratorUtil.generateFile(servicePackageDir, QUERY_VM_TEMPLATE_FILE,
                    queryClassDescriptor.getJavaName(), queryClassDescriptor);
            if (GeneratorUtil.hasServiceInterfacePackageName(configuration)) {
                queryClassDescriptor.setInterface(true);
                GeneratorUtil.generateFile(serviceInterfacePackageDir,
                        QUERY_VM_TEMPLATE_FILE, queryClassDescriptor.getJavaInterfaceName(), queryClassDescriptor);
            }
        }
    }
}
