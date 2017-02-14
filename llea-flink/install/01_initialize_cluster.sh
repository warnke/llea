#!/bin/bash

CURRENT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

## read in cluster name from config file cluster_name.txt
## cluster_name content should correspond to tag_name in pegasus .yml files
CLUSTER_NAME=$(<${CURRENT_DIR}/../config/cluster_name.txt)

### pegasus up starts cluster. configured in yml files
peg up ${CURRENT_DIR}/../config/namenode.yml &
peg up ${CURRENT_DIR}/../config/datanodes.yml &
## Using an already existing dedicated db node
# peg up ${CURRENT_DIR}/../config/db.yml &
