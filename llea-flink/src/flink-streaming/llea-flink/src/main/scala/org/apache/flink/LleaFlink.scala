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
//
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.functions.AssignerWithPeriodicWatermarks
import org.apache.flink.streaming.api.watermark.Watermark

//import utils.{ConfigurationManager}
//import src.main.scala.utils.{LleaRedisMapper}

/////////////////////////////////////////////////////////////////////////
////class LleaRedisMapper extends RedisMapper[(String, String)]{
//class LleaRedisMapper extends RedisMapper[String]{
//  override def getCommandDescription: RedisCommandDescription = {
//    new RedisCommandDescription(RedisCommand.HSET, "HASH_NAME")
//    // new RedisCommandDescription(RedisCommand.HSET, "pipeline")
//  }
//  //override def getKeyFromData(data: (String, String)): String = ("asdf", "qwert)" //data._1
//  //override def getValueFromData(data: (String, String)): String = "123" //data._2
//  override def getKeyFromData(data: (String)): String = "qwer" //data._1
//  override def getValueFromData(data: (String)): String = data //data._2
//}
/////////////////////////////////////////////////////////////////////////

// TODO: move class definition to utils
class LleaRedisMapper extends RedisMapper[String]{
  override def getCommandDescription: RedisCommandDescription = {
    //new RedisCommandDescription(RedisCommand.SET)
    new RedisCommandDescription(RedisCommand.INCRBY)
  }
  //temp override def getKeyFromData(data: (String)): String = "key_w_int_value" //data._1
  override def getKeyFromData(data: (String)): String = data.split(",")(0).dropWhile(_ == '(') + ";" + data.split(",")(1) //data._1
  // Value needs to be a string that can be typecast to an integer
  override def getValueFromData(data: (String)): String = data.split(",")(2).dropRight(1) //data.split(";")(2) //data._2
}


object Main {
 
  class TimestampExtractor extends AssignerWithPeriodicWatermarks[String] with Serializable {
    override def extractTimestamp(e: String, prevElementTimestamp: Long) = {
      e.split(";")(2).toFloat.floor.toLong //(1).toLong maybe multiply by something before floor to get milliseconds?
    }
   override def getCurrentWatermark(): Watermark = {
        new Watermark(System.currentTimeMillis)
    }
  }

  def main(args: Array[String]) {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    val properties = new Properties()

    //// TODO: Use config files
    // use public dns of first worker, kafka will not be on flink master anymore in final config
    properties.setProperty("bootstrap.servers", "ec2-35-167-63-252.us-west-2.compute.amazonaws.com:9092")
    properties.setProperty("zookeeper.connect", "ec2-35-167-63-252.us-west-2.compute.amazonaws.com:2181")
    properties.setProperty("group.id", "org.apache.flink")

    ///// TODO: Use config files later
    // Configure Flink to Redis connector
    // val conf = new FlinkJedisPoolConfig.Builder().setHost(config.get("redis.host")).build()
    val conf = new FlinkJedisPoolConfig.Builder().setHost("52.25.10.79").build()

    // function to convert a timestamp to 30 second time slot
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

    //.addSink(new RedisSinkHeff[(String)](conf, new LleaRedisMapper))

      //.keyBy()
      // add algorithm here....
      // in the end: data must be kv._1
      // in the end: data must be kv._2
      //.addSink(new RedisSink[(String)](conf, new LleaRedisMapper))
      //.addSink(new FlinkKafkaProducer09[String]("localhost:9092", "flink-to-kafka", new SimpleStringSchema()))
     // 
     //.print 

    env.execute("Flink Kafka Example")
  }
}
