REM Create Maven artifact bundles for NoORM
REM Pass the version of the release subject to Maven/Sonatype deployment as command line parameter
REM Upload the generated bundles at https://oss.sonatype.org (Staging Upload)

cd %HOMEPATH%\.m2\repository\org\noorm\noorm\%1
gpg --yes -ab noorm-%1.pom
"%JAVA_HOME%\bin\jar" -cvf noorm-bundle.jar *.asc *.pom

cd %HOMEPATH%\.m2\repository\org\noorm\noorm-runtime\%1
gpg --yes -ab noorm-runtime-%1-javadoc.jar
gpg --yes -ab noorm-runtime-%1-sources.jar
gpg --yes -ab noorm-runtime-%1.jar
gpg --yes -ab noorm-runtime-%1.pom
"%JAVA_HOME%\bin\jar" -cvf noorm-runtime-bundle.jar *.jar *.asc *.pom

cd %HOMEPATH%\.m2\repository\org\noorm\noorm-generator\%1
gpg --yes -ab noorm-generator-%1-javadoc.jar
gpg --yes -ab noorm-generator-%1-sources.jar
gpg --yes -ab noorm-generator-%1.jar
gpg --yes -ab noorm-generator-%1.pom
"%JAVA_HOME%\bin\jar" -cvf noorm-generator-bundle.jar *.jar *.asc *.pom

cd %HOMEPATH%\.m2\repository\org\noorm\noorm-platform\%1
gpg --yes -ab noorm-platform-%1.pom
"%JAVA_HOME%\bin\jar" -cvf noorm-platform-bundle.jar *.asc *.pom

cd %HOMEPATH%\.m2\repository\org\noorm\noorm-oracle-platform\%1
gpg --yes -ab noorm-oracle-platform-%1-javadoc.jar
gpg --yes -ab noorm-oracle-platform-%1-sources.jar
gpg --yes -ab noorm-oracle-platform-%1.jar
gpg --yes -ab noorm-oracle-platform-%1.pom
"%JAVA_HOME%\bin\jar" -cvf noorm-oracle-platform-bundle.jar *.jar *.asc *.pom

cd %HOMEPATH%\.m2\repository\org\noorm\noorm-mssql-platform\%1
gpg --yes -ab noorm-mssql-platform-%1-javadoc.jar
gpg --yes -ab noorm-mssql-platform-%1-sources.jar
gpg --yes -ab noorm-mssql-platform-%1.jar
gpg --yes -ab noorm-mssql-platform-%1.pom
"%JAVA_HOME%\bin\jar" -cvf noorm-mssql-platform-bundle.jar *.jar *.asc *.pom
