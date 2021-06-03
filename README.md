# COEN317FinalProject
Final Project implementation of Kademlia protocol for distributed hash table

# Compile and Run
This project uses maven and Java 10.
```shell
# Compile with mvn
mvn clean package
# Run result
java -jar target/Kademlia-jar-with-dependencies.jar
```
You may also run within in IntelliJ, the result should be the same

# Remote Registry
Perform the following commands after compilation and before running the program:
```shell
./rmiregistry_start.sh
```

Perform the following commands after program run completion:
```shell
./rmiregistry_stop.sh
```

# RMI test
1) Uncomment the `testRPC()` call in `public static void main` of `Main.java`.
2) Run the program on from two separate java execution environments (IDEs, command line, etc)
3) Have fun writing values to different hosts that are in the network