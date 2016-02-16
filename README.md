Building your repository and other useful commands
./gradlew clean build will build your project (and run unit tests)

./gradlew clean build installPackage will build and install your project

./gradlew test will run your unit tests

./gradlew idea will generate IntelliJ project files (after that you can simply open this repository in IntelliJ)

./gradlew clean build installPackage -Dhost=(install host) -Dport=(install port) will build and install your project to a specific host and port that you define in the command. The default values if you do not set these variables are localhost for host and 4502 for port.

 Create a custom Replication Agent for Solr for Indexing Data to Solr on Replication

 1. Transport URI as localhost:8983/solr/solrpoc?commit=true
 2. Replication Agent as  POC Solr XML Serializer
