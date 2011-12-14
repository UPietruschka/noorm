package org.noorm.generator.beangenerator;

import org.noorm.generator.GeneratorException;
import org.noorm.generator.GeneratorUtil;
import org.noorm.generator.ValidatorClassDescriptor;
import org.noorm.metadata.BeanMetaDataUtil;
import org.noorm.metadata.MetadataService;
import org.noorm.metadata.beans.NameBean;
import org.noorm.metadata.beans.PrimaryKeyColumnBean;
import org.noorm.metadata.beans.TableMetadataBean;
import org.noorm.jdbc.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

/**
 * Generator for database related Bean source files based on Velocity templates.
 *
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 19.04.11
 *         Time: 14:48
 */
public class BeanGenerator {

	private static final Logger log = LoggerFactory.getLogger(BeanGenerator.class);
	private static final String BEAN_VM_TEMPLATE_FILE = "/bean.vm";
	private static final String BEAN_VALIDATOR_VM_TEMPLATE_FILE = "/bean_validator.vm";
	private static final String BEAN_VALIDATOR_CLASS_NAME = "GenericBeanValidator";
	private static BeanGenerator beanGenerator;

	/**
	 * Destination directory for generated source files.
	 */
	private File destinationDirectory;

	/**
	 * Package name for generated Bean source files.
	 */
	private String beanPackageName;

	/**
	 * List of table name prefixes to be ignored for java class name construction.
	 * Some data modelers use a common table name prefix to identify tables of a
	 * given schema or group. When those prefixes are not desired in the constructed
	 * java class name, they should be listed here.
	 */
	private List<String> ignoreTableNamePrefixes;

	/**
	 * Regular expression to filter tables and views for Bean generation.
	 */
	private String beanTableFilterRegex;

	/**
	 * Primary key generation for new records being inserted into the database is based on
	 * a numeric ID column and an Oracle sequence. Oracle sequences are not tied to a table
	 * by definition, so associating a table with a sequence is done using this property
	 * list. Note that the association TABLE_NAME/SEQUENCE_NAME can either be done on a per
	 * table basis, or using one or more regular expressions to specify a mapping rule like
	 * "TBL_(.*)" / "SEQ_$1".
	 */
	private Properties oracleTable2SequenceMapping;

	/**
	 * Concurrency control is based on optimistic locking. To identify the version column,
	 * a mapping from the table-name to the version column should be specified. Dependent on
	 * how specific the column-names are with respect to the table-names, one or more
	 * mapping are required. In case of a unique name of the version column for all tables,
	 * one simple rule like ".*" -> "VERSION" is sufficient.
	 * Note that using the Oracle pseudo-column "ORA_ROWSCN" for optimistic locking is also
	 * supported. To enable optimistic locking by using "ORA_ROWSCN", specify the mapping
	 * rule ".*" -> "ORA_ROWSCN".
	 */
	private Properties optimisticLockColumnMapping;

	/**
	 * The Oracle data dictionary does not provide unambiguous information for the primary key
	 * of a view (for tables, this information is available). When the intended use of a view
	 * includes DML operations (which requires the view to contain one and only one key-preserved
	 * table) or data access with the PageableBeanList, NoORM needs a key to uniquely distinguish
	 * the records of this view. Use this parameter to specify the column name of the key used
	 * for a given view. Typically, this key is the primary key of the single key-preserved table
	 * contained in the view definition.
	 */
	private Properties viewName2PrimaryKeyMapping;

	protected BeanGenerator() {
	}

	public void setDestinationDirectory(final File pDestinationDirectory) {
		destinationDirectory = pDestinationDirectory;
	}

	public void setBeanPackageName(final String pBeanPackageName) {
		beanPackageName = pBeanPackageName;
	}

	public void setIgnoreTableNamePrefixes(final List<String> pIgnoreTableNamePrefixes) {
		ignoreTableNamePrefixes = pIgnoreTableNamePrefixes;
	}

	public void setBeanTableFilterRegex(String pBeanTableFilterRegex) {
		beanTableFilterRegex = pBeanTableFilterRegex;
	}

	public void setOracleTable2SequenceMapping(final Properties pOracleTable2SequenceMapping) {
		oracleTable2SequenceMapping = pOracleTable2SequenceMapping;
	}

	public void setOptimisticLockColumnMapping(final Properties pOptimisticLockColumnMapping) {
		optimisticLockColumnMapping = pOptimisticLockColumnMapping;
	}

	public void setViewName2PrimaryKeyMapping(final Properties pViewName2PrimaryKeyMapping) {
		viewName2PrimaryKeyMapping = pViewName2PrimaryKeyMapping;
	}

	public static BeanGenerator getInstance() {

		synchronized (BeanGenerator.class) {
			if (beanGenerator == null) {
				beanGenerator = new BeanGenerator();
			}
		}
		return beanGenerator;
	}

	public void execute() throws GeneratorException {

		if (beanPackageName == null || beanPackageName.isEmpty()) {
			throw new IllegalArgumentException("Parameter [beanPackageName] is null.");
		}
		if (destinationDirectory == null || !destinationDirectory.exists()) {
			throw new IllegalArgumentException("Parameter [destinationDirectory] is null or mis-configured.");
		}

		ValidatorClassDescriptor validatorClassDescriptor = new ValidatorClassDescriptor();
		validatorClassDescriptor.setPackageName(beanPackageName);

		final MetadataService metadataService = MetadataService.getInstance();

		log.info("Retrieving table metadata from Oracle database.");
		final Map<String, List<TableMetadataBean>> tableColumnMap = metadataService.findTableMetadata();

		log.info("Retrieving primary key metadata from Oracle database.");
		final List<PrimaryKeyColumnBean> pkColumnNameList = metadataService.findPkColumns();

		log.info("Retrieving sequence metadata from Oracle database.");
		final List<NameBean> sequenceDBNameList = metadataService.findSequenceNames();

		log.info("Generating NoORM Bean classes.");
		final File beanPackageDir = new File(destinationDirectory, beanPackageName.replace(".", File.separator));
		if (!beanPackageDir.exists()) {
			if (!beanPackageDir.mkdirs()) {
				throw new GeneratorException("Could not create directory ".concat(beanPackageDir.toString()));
			}
		}

		for (final String tableName0 : tableColumnMap.keySet()) {
			if (beanTableFilterRegex != null && !tableName0.matches(beanTableFilterRegex)) {
				log.info("Exclude table ".concat(tableName0)
						.concat(", table name does not match regex '")
						.concat(beanTableFilterRegex)
						.concat("'"));
				continue;
			}
			final String javaBeanName = Utils.convertTableName2BeanName(tableName0, ignoreTableNamePrefixes);
			final List<TableMetadataBean> tableMetadataBeanList1 = tableColumnMap.get(tableName0);
			final BeanClassDescriptor beanClassDescriptor = new BeanClassDescriptor();
			beanClassDescriptor.setName(javaBeanName);
			validatorClassDescriptor.getClassNames().add(javaBeanName);
			beanClassDescriptor.setTableName(tableName0);
			String[] primaryKeyColumnNames = getPrimaryKeyColumnNames(tableName0, pkColumnNameList);
			beanClassDescriptor.setPrimaryKeyColumnNames(primaryKeyColumnNames);
			final String sequenceName = getSequenceName(tableName0, sequenceDBNameList);
			beanClassDescriptor.setSequenceName(sequenceName);
			final String versionColumnName = getVersionColumnName(tableName0, tableMetadataBeanList1);
			beanClassDescriptor.setVersionColumnName(versionColumnName);
			beanClassDescriptor.setPackageName(beanPackageName);
			// Use a unique seed for serialVersionUID generation to guarantee the generation of a reproducible
			// serialVersionUID with every new source code generation cycle.
			final Random random = new Random(javaBeanName.hashCode());
			final long serialVersionUID = random.nextLong();
			beanClassDescriptor.setSerialVersionUID(serialVersionUID);
			for (TableMetadataBean tableMetadataBean : tableMetadataBeanList1) {
				final BeanAttributeDescriptor beanAttributeDescriptor = new BeanAttributeDescriptor();
				final String javaColumnName = Utils.convertDBName2JavaName(tableMetadataBean.getColumnName(), false);
				beanAttributeDescriptor.setName(javaColumnName);
				final String javaType = Utils.convertOracleType2JavaType(tableMetadataBean.getDataType(),
						tableMetadataBean.getDataPrecision(), tableMetadataBean.getDataScale());
				if (tableMetadataBean.getUpdatable().equals(BeanMetaDataUtil.NOT_UPDATABLE) ||
						tableMetadataBean.getInsertable().equals(BeanMetaDataUtil.NOT_UPDATABLE)) {
					beanAttributeDescriptor.setUpdatable(false);
				}
				beanAttributeDescriptor.setType(javaType);
				beanAttributeDescriptor.setDataType(tableMetadataBean.getDataType());
				beanAttributeDescriptor.setColumnName(tableMetadataBean.getColumnName());
				beanAttributeDescriptor.setMaxLength(tableMetadataBean.getCharLength().intValue());
				if (tableMetadataBean.getNullable().equals(BeanMetaDataUtil.NOT_NULLABLE)) {
					beanAttributeDescriptor.setNullable(false);
				}
				beanClassDescriptor.addAttribute(beanAttributeDescriptor);
			}
			GeneratorUtil.generateFile(beanPackageDir, BEAN_VM_TEMPLATE_FILE,
					beanClassDescriptor.getName(), beanClassDescriptor);
		}
		GeneratorUtil.generateFile(beanPackageDir, BEAN_VALIDATOR_VM_TEMPLATE_FILE,
				BEAN_VALIDATOR_CLASS_NAME, validatorClassDescriptor);
	}

	private String getSequenceName(final String pTableName,
								   final List<NameBean> pSequenceDBNameList) {

		String sequenceName = Utils.getPropertyString(pTableName, oracleTable2SequenceMapping);
		if (sequenceName.isEmpty()) {
			log.info("No matching sequence-name has been found for table-name ".concat(pTableName));
			return sequenceName;
		}
		// Check the matched sequence name against the sequence names retrieved from the database
		for (NameBean sequenceDBName : pSequenceDBNameList) {
			if (sequenceName.equals(sequenceDBName.getName())) {
				return sequenceName;
			}
		}
		log.info("Matching sequence-name ".concat(sequenceName).concat(" has been found for table-name ")
				.concat(pTableName).concat(", but no database sequence with this name could be found."));
		return sequenceName;
	}

	private String[] getPrimaryKeyColumnNames(final String pTableName,
										      final List<PrimaryKeyColumnBean> pPrimaryKeyColumnList) {

		final List<String> pkColumnNames = new ArrayList<String>();
		for (PrimaryKeyColumnBean primaryKeyBean : pPrimaryKeyColumnList) {
			if (pTableName.equals(primaryKeyBean.getTableName())) {
				log.info("Primary key column ".concat(primaryKeyBean.getColumnName())
						.concat(" found for table ").concat(pTableName));
				pkColumnNames.add(primaryKeyBean.getColumnName());
			}
		}
		if (pkColumnNames.isEmpty()) {
			if (viewName2PrimaryKeyMapping != null) {
				final String viewPKName = Utils.getPropertyString(pTableName, viewName2PrimaryKeyMapping);
				if (!viewPKName.isEmpty()) {
					pkColumnNames.add(viewPKName);
				}
			}
			if (pkColumnNames.isEmpty()) {
				log.info("No primary key found."
						.concat("Automatic support for DML will not be available for table ")
						.concat(pTableName));
			}
		}
		return pkColumnNames.toArray(new String[pkColumnNames.size()]);
	}

	private String getVersionColumnName(final String pTableName,
										final List<TableMetadataBean> pTableMetadataBeanList) {

		String versionColumnName = Utils.getPropertyString(pTableName, optimisticLockColumnMapping);
		if (versionColumnName.isEmpty()) {
			log.info("No matching version-column-name has been found for table-name ".concat(pTableName));
			return versionColumnName;
		}
		// Check the matched version column name against the column names of this table
		for (TableMetadataBean tableMetadataBean : pTableMetadataBeanList) {
			if (versionColumnName.equals(tableMetadataBean.getColumnName())) {
				return versionColumnName;
			}
		}
		log.info("Matching version-column-name ".concat(versionColumnName).concat(" has been found for ")
				.concat("table-name ").concat(pTableName).concat(", but no table-column with this name could be found."));
		return "";
	}
}
