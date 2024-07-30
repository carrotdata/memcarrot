1. pull recent changes from carrot-cache repo
2. build carrot-cache: mvn install -DskipTests
3. pull recent chabges from memcarrot
4. build memcarrot: mvn package -DskipTests
5. start memcarrot server: ./bin/memcarrot.sh start
6. stop server: ./bin/memcarrot.sh stop
7. server creates log fiel in local logs directory: tail -f logs/file
