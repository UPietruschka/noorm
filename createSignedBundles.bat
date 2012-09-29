# Create Maven artifact bundles for NoORM
# Pass the version of the release subject to Maven/Sonatype deployment as command line parameter

cd %HOMEPATH%\.m2\repository\org\noorm\noorm\%1
gpg --yes -ab noorm-%1.pom
%JAVA_HOME%\bin\jar -cvf noorm-bundle.jar *.asc *.pom

cd %HOMEPATH%\.m2\repository\org\noorm\noorm-runtime\%1
gpg --yes -ab noorm-runtime-%1-javadoc.jar
gpg --yes -ab noorm-runtime-%1-sources.jar
gpg --yes -ab noorm-runtime-%1.jar
gpg --yes -ab noorm-runtime-%1.pom
%JAVA_HOME%\bin\jar -cvf noorm-runtime-bundle.jar *.jar *.asc *.pom

cd %HOMEPATH%\.m2\repository\org\noorm\noorm-generator\%1
gpg --yes -ab noorm-generator-%1-javadoc.jar
gpg --yes -ab noorm-generator-%1-sources.jar
gpg --yes -ab noorm-generator-%1.jar
gpg --yes -ab noorm-generator-%1.pom
%JAVA_HOME%\bin\jar -cvf noorm-generator-bundle.jar *.jar *.asc *.pom
