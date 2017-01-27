import kafka.serializer.StringDecoder

import org.apache.spark.streaming._
import org.apache.spark.streaming.kafka._
import org.apache.spark.SparkConf
import org.apache.spark.rdd.RDD
import org.apache.spark.SparkContext
import org.apache.spark.sql._
import com.typesafe.config._
import scredis._
import scala.util.{ Success, Failure }

import scala.math
import scala.collection
import collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global

// Consumes Kafka stream and outputs running totals into Redis as state engine
object LleaStreaming extends Serializable {
  def main(args: Array[String]) {

    val conf = ConfigFactory.load()
    val masterdns = conf.getString("llea.masterhost")
    val brokers = masterdns + ":9092"
    val topics = conf.getString("llea.topics")
    val topicsSet = topics.split(",").toSet

    // Debugging only
    //println("masterdns read from config: " + masterdns)
    //println("topics read from config: " + topics)
    //println("llea.db host: " + conf.getString("llea.dbhost"))
    //println("llea.db password: " + conf.getString("llea.dbpassword"))

    // Create context with 5 second batch interval
    val sparkConf = new SparkConf().setAppName("llea-stream")
    val ssc = new StreamingContext(sparkConf, new Duration(5000))
    ssc.checkpoint("hdfs://" + masterdns + ":9000/user/checkpoint")

    // Create direct kafka stream with brokers and topics
    val kafkaParams = Map[String, String]("metadata.broker.list" -> brokers)
    val messages = KafkaUtils.createDirectStream[String, String, StringDecoder, StringDecoder](ssc, kafkaParams, topicsSet)

    // function to convert a timestamp to 30 second time slot
    def convert_to_30sec(timestamp: String): String = {
      (timestamp.toDouble.toLong/30*30).toString
    }

    // Singleton object to keep one Redis connection each open on each worker node
    object RedisConnection extends Serializable {
      val redishost = conf.getString("llea.dbhost")
      //val mysecret = conf.getString("llea.dbpassword")
      //lazy val client: Redis = new Redis(host = redishost, passwordOpt = Option(mysecret))
      lazy val client: Redis = new Redis(host = redishost)
    }

    // map each record into a tuple consisting of (key, ID, epochtime), total for each key+epochtime_to_30sec
    val sensorReadings = messages
      .mapPartitions( it =>
        it.map(tuple => {
          val record = tuple._2.split(";")
          record(0) + ";" + convert_to_30sec(record(2))
        })
        // essentially a word count in mapPartitions form
        .foldLeft(new mutable.HashMap[String, Int])(
          (count, key) => count += (key -> (count.getOrElse(key, 0) + 1))
        ).toIterator
      )
      // output to Redis: current time on worker node and running totals.
      // Use incrBy to accumulate atomically, creates key if it doesn't exist
      .foreachRDD( rdd => {
        rdd.mapPartitionsWithIndex( (index, it) => {
          val currtime: String = "%.2f".format(System.currentTimeMillis / 1000.0)
          RedisConnection.client.set("currtime-" + index.toString, currtime)

          val output = it.map( kv => {
            RedisConnection.client.incrBy(kv._1, kv._2) onComplete {
              case Success(flag) => {}
              case Failure(e) => e.printStackTrace()
            }
            kv
          })
          output
        })
        // force DAG execution
        .count()
      })

    // Start the computation
    ssc.start()
    ssc.awaitTermination()
  }
}
