1. pull recent changes from carrot-cache repo
2. build carrot-cache: mvn install -DskipTests
3. pull recent chabges from memcarrot
4. build memcarrot: mvn package -DskipTests
5. cp target/memcarrot - xxxx- with-dependencies.jar to lib/memcarrot-0.10-all.jar
6. start memcarrot server: ./bin/memcarrot.sh start
7. stop server: ./bin/memcarrot.sh stop
8. server creates log fiel in local logs directory: tail -f logs/file
