# COEN317FinalProject
Final Project implementation of Kademlia protocol for distributed hash table

# Compile and Run
This project uses maven and Java 10.
```shell
# Compile with mvn
mvn clean package
# After compiling, start the RMI Registry
./rmiregistry_start.sh
# Run result
java -jar target/Kademlia-jar-with-dependencies.jar
# After running, stop the RMI Registry for a clean future startup
./rmiregistry_stop.sh
```

You may also run within in IntelliJ, the result should be the same