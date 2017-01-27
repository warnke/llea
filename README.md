# LLEA (is under construction)

## Low Latency Event Alerts

1. [Introduction](README.md#introduction)
2. [Installation](README.md#installation)
3. [Running Instructions](README.md#running-instructions)

##Introduction

### Out of Order Snippets

* Example setup:
  * 4 nodes: Spark master, 3 workers. Kafka on workers, python producers running locally on each worker
  * 1 node: redis DB

* Spark prototype borrowed, to be benchmarked against. Credit: Dr. A. Markmann

* Setup with simple deployment tool pegasus is recommended (https://github.com/InsightDataScience/pegasus), but not necessary

* Look into install folder: scripts contain instructions for using pegasus to start up cluster, install tools, start and stop services

* pegasus configuration .yml config files are not part of the repo. See sample file and list of expected configuration files

* python_producer: needs to be on each worker, will connect to kafka's default port, see source code for details

* spark_consumer: application.conf.sample needs to be copied to application.conf and edited.

* create topic from any node running Kafka: ``kafka-console-consumer.sh --zookeeper localhost:2181 --topic pipeline``

* check for existing topics ``$ kafka-topics.sh --list --zookeeper localhost:2181``

* Redis opens port on machine running cache key-object store. Access can be restricted with either password (need to edit llea-stream.scala and application.conf)

* Versions: see file ``build.sbt`` for details. Versions in build.sbt have to match the versions of spark/kafka/scala/hadoop available on your system. Working example: Spark 1.6.1 build for Hadoop2.6 with Scala 2.10.4 and Hadoop 2.6.4 HDFS.

* test your producer and Kafka:
  * ``$ ./producer.py    # topic name "pipeline" hardcoded in producer. Change if necessary``
  * On same node, start a kafka command line consumer, i.e. ``kafka-console-consumer.sh --zookeeper localhost:2181 --topic pipeline``

* compile Spark code: ``sbt package assembly``

* run Spark code: ``spark-submit --class LleaStreaming --master spark://`hostname`:7077 --conf spark.streaming.blockInterval=2500ms --jars target/scala-2.10/llea_stream-assembly-1.0.jar target/scala-2.10/llea_stream_2.10-1.0.jar``

<!-- 
* Intro complemented by slides in [llea.halfwheeler.com/deck](https://docs.google.com/deck here)

* Framework for scalable, distributed processing of sensory data that aims at low latency

* Partial sensory data is interpreted before complete, knowing that final measurements arrive at processing unit delayed (event time vs. processing time). Delay is Gaussian distributed

* Redis key/value storage for maintaining and updating state (alternative would be Spark stateful streaming. network shuffle during stateful streaming).


* continue 
-->

##Installation
[Back to Top](README.md#table-of-contents)

### Prerequisites
* Scripts and tools used in this project assume you are running this package on Amazon Elastic Compute instances.
* list hardware used
* list required software


### Notes on Cluster Configuration 

##Running Instructions
[Back to Top](README.md#table-of-contents)

