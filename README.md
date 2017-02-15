# LLEA

## Low Latency Event Alerts Pipeline

1. [Introduction](README.md#introduction)
2. [Installation](README.md#installation)
3. [Running Instructions](README.md#running-instructions)

##Introduction

A slideshow introducing the project can be found at [halfwheeler.com/slides](http://www.halfwheeler.com/slides).

Llea is a distributed and scalable IOT pipeline for monitoring sensor data streams.
The central stream processing part is implemented twice, using two different open source tools 
for data stream processing:
* [Apache Flink](https://flink.apache.org/) (native streaming)
* [Apache Spark Streaming](http://spark.apache.org/streaming/) (based on microbatches)

Current implementation of Llea ingests in a stream of event messages coming from a simulated set of sensors.
The framework then calculates and displays the frequencies at which the events occur at real-time.

The pipeline was built with the idea in mind to study the two streaming frameworks and to provide the ability to
compare their performance.

##Installation
[Back to Top](README.md#table-of-contents)

The open source technologies Llea relies on and that need to be installed are
* [Kafka](https://kafka.apache.org/) - ingestion
* Flink (alternatively Spark) - stream processing
* [Redis](https://redis.io) - state engine

In principle, Llea can be installed and run in a variety of hardware
configurations. The installation scripts in folders
``llea/llea-flink/install/`` and ``llea/llea-spark/install/`` assume that
llea will be installed and run on EC2 instances of Amazon's cloud 
[Amazon Web Services](https://aws.amazon.com/). Installation relies on the
deployment tool [pegasus](https://github.com/InsightDataScience/pegasus).
Sample yml configuration files for pegasus specifying
versions and cluster setup reside in ``llea/llea-flink/config/`` as well as in
``llea/llea-spark/config/``.

From folder ``llea/llea-XXX/install``, run the numbered setup and bash
scripts to   
1. spin up the cluster  
2. install the required tools  
3. start the services

### Security
Please note that strict security group settings of the AWS cluster are important to prevent
[vulnerabilities](https://redislabs.com/blog/3-critical-points-about-security/) of the Spark cluster
and the server running the Redis key value store. Alternatively, Redis access may be restricted with a password.

### Source
Source folders in ``llea/llea-flink/`` and ``llea/llea-spark/`` contain the source of the respective 
Flink and Spark implementations.
The python producers require the python library pykafka. To install, run
`pip install pykafka`.

### Spark Implementation
To configure the spark app, rename and edit file   
``llea/llea-spark/src/spark-consumer/src/main/resources/application.conf.sample``

Compile from the `src/spark-consumer` issuing  
``$ sbt package assembly``

### Flink Implementation and the Flink-Redis Connector
The current version of Apache's [Flink-Redis-Connector](https://github.com/apache/bahir-flink/tree/master/flink-connector-redis)
does not support Redis' increment functionality ``incrBy(key, value)``.
In order for my project to work, I implemented this functionality within the connector project and am planning on contributing it
to Apache's Flink-Redis connector repository. In the mean time, please contact me for details on how to build and compile the connector into a jar-file.

Compile the Flink project  
``llea-flink/src/flink-streaming/llea-flink$ mvn clean install -Pbuild-jar``  

### User Interface Installation
The UI requires a python [flask](flask.pocoo.org) webserver. Make sure to install the 
[python-redis connector](https://redis.io/clients#python)  
``$ sudo pip install redis-py``

## Running Instructions
[Back to Top](README.md#table-of-contents)

### Spark Implementation
To start the Spark Streaming job, 
``src/spark-consumer$ spark-submit --class LleaStreaming --master spark://`hostname`:7077 --conf spark.streaming.blockInterval=2500ms --jars target/scala-2.10/llea_stream-assembly-1.0.jar target/scala-2.10/llea_stream_2.10-1.0.jar 2> /dev/null``

### Flink Implementation
To start the Flink job,
``llea-flink/src/flink-streaming/llea-flink$flink run -c Main target/llea-flink-1.1.4.jar``

### Frontend
The Flask/Tornado frontend periodically queries the Redis key value store for state-updates for visualization.
