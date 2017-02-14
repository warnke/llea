#!/bin/bash

CURRENT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
# get name from config
CLUSTER_NAME=$(<${CURRENT_DIR}/../config/cluster_name.txt)

peg fetch ${CLUSTER_NAME}
# uncomment to include db node in automatic setup
#peg fetch ${CLUSTER_NAME}.db

## start hdfs and spark services on the master (node 1) remotely
#peg sshcmd-node ${CLUSTER_NAME} 1 "source .profile && \$HADOOP_HOME/sbin/start-dfs.sh && \$SPARK_HOME/sbin/start-all.sh"

peg service ${CLUSTER_NAME} hadoop start
peg service ${CLUSTER_NAME} flink start
peg service ${CLUSTER_NAME} zookeeper start
echo "Starting kafka service..."
peg service ${CLUSTER_NAME} kafka start > /dev/null 2>&1 &

