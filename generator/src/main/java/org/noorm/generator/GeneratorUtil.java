package org.noorm.generator;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.noorm.jdbc.Utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 30.11.11
 *         Time: 12:18
 */
public class GeneratorUtil {

	public static void generateFile(final File pDir,
									final String pVelocityTemplateFile,
									final String pJavaName,
									final Object pClassDescriptor) throws GeneratorException {

		final File javaSourceFile = new File(pDir, pJavaName + Utils.JAVA_SOURCE_FILE_APPENDIX);
		try {
			final VelocityContext context = new VelocityContext();
			context.put("class", pClassDescriptor);
			context.put("nl", "\n");
			context.put("subindent", "\t\t\t\t\t");
			// The following macro is used as a workaround for an un-intentional Velocity behaviour.
			// Usually, Velocity just takes the newlines of the template as they occur in the template.
			// However, when a line ends with a velocity command like "#if(...)" or "#end", Velocity
			// omits the newline. When a newline is desired here, we need to append something to the
			// lines end to force a newline. Since this addendum should not be visible in the generated
			// code, we define a macro here, which is visible in the template, but not in the generated
			// code (just an empty macro).
			context.put("force_newline", "");
			final Template template = Velocity.getTemplate(pVelocityTemplateFile);
			final BufferedWriter writer = new BufferedWriter(new FileWriter(javaSourceFile));
			template.merge(context, writer);
			writer.flush();
			writer.close();
		} catch (IOException e) {
			throw new GeneratorException("Writing Java source file failed.", e);
		}
	}

	public static File createPackageDir(final File pDestinationDirectory,
										final String pPackageName) throws GeneratorException {

		final File packageDir =	new File(pDestinationDirectory, pPackageName.replace(".", File.separator));
		if (!packageDir.exists()) {
			if (!packageDir.mkdirs()) {
				throw new GeneratorException("Could not create directory ".concat(packageDir.toString()));
			}
		}
		return packageDir;
	}
}
