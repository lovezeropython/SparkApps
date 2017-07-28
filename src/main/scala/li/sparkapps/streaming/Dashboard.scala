package li.sparkapps.streaming

import java.util.HashMap

import org.apache.kafka.clients.producer.{KafkaProducer, ProducerConfig, ProducerRecord}
import org.apache.spark.SparkConf
import org.apache.spark.streaming._
import org.apache.spark.streaming.kafka._
import org.json4s._
import org.json4s.jackson.Serialization.write
import com.google.gson.Gson

object Dashboard {
  implicit val formats = DefaultFormats//数据格式化时需要
  def main(args: Array[String]): Unit={
    if (args.length < 4) {
      System.err.println("Usage: Dashboard <zkQuorum> <group> <topics> <numThreads>")
      System.exit(1)
    }
    //StreamingExamples.setStreamingLogLevels()
    /* 输入的四个参数分别代表着
    * 1. zkQuorum 为zookeeper地址
    * 2. group为消费者所在的组
    * 3. topics该消费者所消费的topics
    * 4. numThreads开启消费topic线程的个数
    */
    val Array(zkQuorum, group, topics, numThreads) = args
    val sparkConf = new SparkConf().setAppName("KafkaWordCount").setMaster("local[4]")
    val ssc = new StreamingContext(sparkConf, Seconds(1))
    ssc.checkpoint("./checkpoint") // 设置checkpoint directory
    // 将topics转换成(topic,numThreads)的map
    //val topicMap = topics.split(",").map((_, numThreads.toInt)).toMap

    val f = (x: String) => new Tuple2[String, Int](x, numThreads.toInt)
    val topicMap = topics.split(",").map(f).toMap


    // 创建连接Kafka的消费者链接

    val lines = KafkaUtils.createStream(ssc, zkQuorum, group, topicMap).map(_._2)
    val words = lines.flatMap(_.split(" "))//将输入的每行用空格分割成一个个word
    // 对每一秒的输入数据进行reduce，然后将reduce后的数据发送给Kafka


   // words.print() // 调试用

    //val wordX = words.map(x => (x, 1L)).print() // 调试用

    //words.map( x => (x, 1L)).reduceByKeyAndWindow(_+_, _-_, Seconds(1), Seconds(1), 1).print()  // debug

    // 过滤掉无意义项，只保留男女
    //val filterFunc = (tuple: Tuple2[String, Long]) => {tuple._1.equals("1") || tuple._1.equals("0")}
    val wordCounts = words.map(x => (x, 1L))
      .reduceByKeyAndWindow(_+_,_-_, Seconds(1), Seconds(1), 1).foreachRDD(rdd => {
      if(rdd.count !=0 ){
        val props = new HashMap[String, Object]()
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:9092")
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
          "org.apache.kafka.common.serialization.StringSerializer")
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
          "org.apache.kafka.common.serialization.StringSerializer")
        // 实例化一个Kafka生产者
        val producer = new KafkaProducer[String, String](props)
        // rdd.colect即将rdd中数据转化为数组，然后write函数将rdd内容转化为json格式



        val str = write(rdd.collect)
        // 封装成Kafka消息，topic为"result"
        val message = new ProducerRecord[String, String]("result", null, str)
        // 给Kafka发送消息
        producer.send(message)
      }
    })
    ssc.start()
    ssc.awaitTermination()
  }
}