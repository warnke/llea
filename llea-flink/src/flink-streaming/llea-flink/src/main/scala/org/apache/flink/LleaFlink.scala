import java.util.Properties

import org.apache.flink.api.scala._
import org.apache.flink.streaming.util.serialization.SimpleStringSchema
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer09
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer09
//import org.apache.flink.streaming.connectors.redis.RedisSink
import org.apache.flink.streaming.connectors.redis.RedisSinkHeff
import org.apache.flink.streaming.connectors.redis.common.config.FlinkJedisPoolConfig
import org.apache.flink.streaming.connectors.redis.common.mapper.RedisCommand;
import org.apache.flink.streaming.connectors.redis.common.mapper.RedisCommandDescription;
import org.apache.flink.streaming.connectors.redis.common.mapper.RedisMapper;

import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.functions.AssignerWithPeriodicWatermarks
import org.apache.flink.streaming.api.watermark.Watermark

//import utils.{ConfigurationManager}

// RedisMapper needs to know data format in stream after transformation
class LleaRedisMapper extends RedisMapper[String]{
  override def getCommandDescription: RedisCommandDescription = {
    new RedisCommandDescription(RedisCommand.INCRBY)
  }
  override def getKeyFromData(data: (String)): String = data.split(",")(0).dropWhile(_ == '(') + ";" + data.split(",")(1)
  override def getValueFromData(data: (String)): String = data.split(",")(2).dropRight(1) //data.split(";")(2)
}

object Main {

  // Extract event time from stream
  class TimestampExtractor extends AssignerWithPeriodicWatermarks[String] with Serializable {
    override def extractTimestamp(e: String, prevElementTimestamp: Long) = {
      e.split(";")(2).toFloat.floor.toLong 
    }
    override def getCurrentWatermark(): Watermark = {
      new Watermark(System.currentTimeMillis)
    }
  }

  def main(args: Array[String]) {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    val properties = new Properties()

    
    // use first worker
    properties.setProperty("bootstrap.servers", "replace-by-public-dns:9092")
    properties.setProperty("zookeeper.connect", "replace-by-public-dns:2181")
    properties.setProperty("group.id", "org.apache.flink")

    // Configure Flink to Redis connector
    // val conf = new FlinkJedisPoolConfig.Builder().setHost(config.get("redis.host")).build()
    val conf = new FlinkJedisPoolConfig.Builder().setHost("xx.xx.xx.xx").build()

    // Convert timestamp to 30 second event time window
    def convertTo30Sec(timestamp: String): String = {
      (timestamp.toDouble.toLong/30*30).toString
    }

    val stream = env
      .addSource(new FlinkKafkaConsumer09[String]("pipeline", new SimpleStringSchema(), properties))
      .assignTimestampsAndWatermarks(new TimestampExtractor)

    val windowedCount = stream.map(value => value.split(";") match { case Array(x,y,z) => (x + "," + convertTo30Sec(z), 1) })
                       .keyBy(0)
                       .timeWindow(Time.milliseconds(5000), Time.milliseconds(5000))
                       .sum(1)

    windowedCount.map(value => value.toString())
                       .addSink(new RedisSinkHeff[(String)](conf, new LleaRedisMapper))

    env.execute("Llea Flink")
  }
}
