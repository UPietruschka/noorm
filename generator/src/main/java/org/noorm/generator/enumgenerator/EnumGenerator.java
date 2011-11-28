package org.noorm.generator.enumgenerator;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.noorm.generator.GeneratorException;
import org.noorm.metadata.MetadataService;
import org.noorm.jdbc.Utils;
import org.noorm.metadata.beans.TableMetadataBean;
import org.noorm.jdbc.JDBCStatementProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Generator for database related Enum source files based on Velocity templates.
 *
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 19.04.11
 *         Time: 14:48
 */
public class EnumGenerator {

	private static final Logger log = LoggerFactory.getLogger(EnumGenerator.class);
	private static final String ENUM_VM_TEMPLATE_FILE = "/enum.vm";
	private static EnumGenerator enumGenerator;

	/**
	 * Destination directory for generated source files.
	 */
	private File destinationDirectory;

	/**
	 * Package name for generated Enum source files.
	 */
	private String enumPackageName;

	/**
	 * List of table name prefixes to be ignored for java class name construction.
	 * Some data modelers use a common table name prefix to identify tables of a
	 * given schema or group. When those prefixes are not desired in the constructed
	 * java class name, they should be listed here.
	 */
	private List<String> ignoreTableNamePrefixes;

	/**
	 * Regular expression to filter tables and views for Enum generation.
	 */
	private String enumTableFilterRegex;

	/**
	 * To generate Enums from database tables, NoORM must now, which table column should be used
	 * for the enums constant type generation. Typically, a table with constant content has a column
	 * with a code or denominator in uppercase letters, which uniquely identifies the row.
	 */
	private Properties enumTable2TypeColumnMapping;

	protected EnumGenerator() {
	}

	public void setDestinationDirectory(final File pDestinationDirectory) {
		destinationDirectory = pDestinationDirectory;
	}

	public void setEnumPackageName(final String pEnumPackageName) {
		enumPackageName = pEnumPackageName;
	}

	public void setIgnoreTableNamePrefixes(final List<String> pIgnoreTableNamePrefixes) {
		ignoreTableNamePrefixes = pIgnoreTableNamePrefixes;
	}

	public void setEnumTableFilterRegex(final String pEnumTableFilterRegex) {
		enumTableFilterRegex = pEnumTableFilterRegex;
	}

	public void setEnumTable2TypeColumnMapping(final Properties pEnumTable2TypeColumnMapping) {
		enumTable2TypeColumnMapping = pEnumTable2TypeColumnMapping;
	}

	public static EnumGenerator getInstance() {

		synchronized (EnumGenerator.class) {
			if (enumGenerator == null) {
				enumGenerator = new EnumGenerator();
			}
		}
		return enumGenerator;
	}

	public void execute() throws GeneratorException {

		if (enumPackageName == null || enumPackageName.isEmpty()) {
			throw new IllegalArgumentException("Parameter [enumPackageName] is null.");
		}
		if (destinationDirectory == null || !destinationDirectory.exists()) {
			throw new IllegalArgumentException("Parameter [destinationDirectory] is null or mis-configured.");
		}

		log.info("Retrieving table metadata from Oracle database.");
		final MetadataService metadataService = MetadataService.getInstance();
		final Map<String, List<TableMetadataBean>> tableColumnMap = metadataService.findTableMetadata();

		log.info("Generating NoORM Enum classes.");
		final File enumPackageDir = new File(destinationDirectory, enumPackageName.replace(".", File.separator));
		if (!enumPackageDir.exists()) {
			enumPackageDir.mkdirs();
		}

		for (final String tableName0 : tableColumnMap.keySet()) {
			if (enumTableFilterRegex != null && !tableName0.matches(enumTableFilterRegex)) {
				log.info("Exclude table ".concat(tableName0)
						.concat(", table name does not match regex '")
						.concat(enumTableFilterRegex)
						.concat("'"));
				continue;
			}
			final String javaEnumName = Utils.convertTableName2JavaName(tableName0, ignoreTableNamePrefixes);
			final List<TableMetadataBean> tableMetadataBeanList1 = tableColumnMap.get(tableName0);
			final EnumClassDescriptor enumClassDescriptor = new EnumClassDescriptor();
			enumClassDescriptor.setName(javaEnumName);
			enumClassDescriptor.setTableName(tableName0);
			enumClassDescriptor.setPackageName(enumPackageName);
			String typeColumnName = "";
			if (enumTable2TypeColumnMapping != null) {
				typeColumnName = Utils.getPropertyString(tableName0, enumTable2TypeColumnMapping);
			} else {
				throw new GeneratorException
						("Parameter [enumTable2TypeColumnMapping] must be set to enable enum generation.");
			}
			if (typeColumnName.isEmpty()) {
				throw new GeneratorException
						("Could not resolve the enum type column name using [enumTable2TypeColumnMapping].");
			}
			enumClassDescriptor.setTypeColumnName(typeColumnName);
			for (TableMetadataBean tableMetadataBean : tableMetadataBeanList1) {
				final EnumAttributeDescriptor enumAttributeDescriptor = new EnumAttributeDescriptor();
				final String javaColumnName = Utils.convertDBName2JavaName(tableMetadataBean.getColumnName(), false);
				enumAttributeDescriptor.setName(javaColumnName);
				final String javaType = Utils.convertOracleType2JavaType(tableMetadataBean.getDataType(),
						tableMetadataBean.getDataPrecision(), tableMetadataBean.getDataScale());
				enumAttributeDescriptor.setType(javaType);
				enumAttributeDescriptor.setColumnName(tableMetadataBean.getColumnName());
				enumClassDescriptor.addAttribute(enumAttributeDescriptor);
			}
			final JDBCStatementProcessor jdbcStatementProcessor = JDBCStatementProcessor.getInstance();
			final String query = "SELECT * FROM ".concat(tableName0);
			final List<Map<String, Object>> recordList = jdbcStatementProcessor.executeGenericSelect(query);
			if (recordList.isEmpty()) {
				log.info("Cannot generate enum: specified enum database table does not contain any data.");
				continue;
			}
			for (Map<String, Object> record : recordList) {
				final EnumRecordDescriptor enumRecordDescriptor = new EnumRecordDescriptor();
				final String typeColumnValue = (String) record.get(typeColumnName);
				if (typeColumnValue == null || typeColumnValue.isEmpty()) {
					String errMsg = "Could not resolve the enum type column value using [enumTable2TypeColumnMapping]"
							.concat(" for table [".concat(tableName0)
							.concat("] and type column name [").concat(typeColumnName).concat("]."));
					throw new GeneratorException(errMsg);
				}
				enumRecordDescriptor.setTypeColumnValue(typeColumnValue);
				final List<EnumAttributeDescriptor> enumAttributeDescriptorList = enumClassDescriptor.getAttributes();
				for (EnumAttributeDescriptor enumAttributeDescriptor : enumAttributeDescriptorList) {
					final String columnName = enumAttributeDescriptor.getColumnName();
					final Object value = record.get(columnName);
					enumRecordDescriptor.setAttributeValue(enumAttributeDescriptor, value);
				}
				enumClassDescriptor.addRecord(enumRecordDescriptor);
			}
			generateEnum(enumPackageDir, enumClassDescriptor);
		}
	}

	private void generateEnum(final File pEnumPackageDir,
							  final EnumClassDescriptor pEnumClassDescriptor) throws GeneratorException {

		final File javaSourceFile =
				new File(pEnumPackageDir, pEnumClassDescriptor.getName() + Utils.JAVA_SOURCE_FILE_APPENDIX);
		try {
			final VelocityContext context = new VelocityContext();
			context.put("class", pEnumClassDescriptor);
			// The following macro is used as a workaround for an un-intentional Velocity behaviour.
			// Usually, Velocity just takes the newlines of the template as they occur in the template.
			// However, when a line ends with a velocity command like "#if(...)" or "#end", Velocity
			// omits the newline. When a newline is desired here, we need to append something to the
			// lines end to force a newline. Since this addendum should not be visible in the generated
			// code, we define a macro here, which is visible in the template, but not in the generated
			// code (just an empty macro).
			context.put("force_newline", "");
			final Template template = Velocity.getTemplate(ENUM_VM_TEMPLATE_FILE);
			final BufferedWriter writer = new BufferedWriter(new FileWriter(javaSourceFile));
			template.merge(context, writer);
			writer.flush();
			writer.close();
		} catch (IOException e) {
			throw new GeneratorException("Writing Java Enum source file failed.", e);
		}
	}
}
