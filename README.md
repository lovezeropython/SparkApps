## Spark应用程序（陆续更新）
###  一. 基于Spark streaming的 模拟 网站日志实时分析系统
#### 说明
1. <code>Scala</code>和<code>Java</code>两种实现.
#### 环境依赖
- hadoop-2.6.2
- spark-1.5.2-bin-hadoop2.6
- scala-2.10.5
- sbt
1. 基本描述:
- 日志来源:用Python脚本随机生成Nginx访问日志，并通过Bash脚本上传至HDFS
- Spark Streaming应用监控HDFS目录
- 每10s产生的日志文件作为一个批次
- 输出的流量分析包括指定时间内:
  + 总的PV
  + 各IP的PV
  + 来自各搜索引擎和关键字的PV
  + 终端类型(PC, ios, Android)PV

2. 使用：
- 项目根目录下:<code>sbt clean package</code>
- 启动hadoop hdfs
- 根据自身Spark和Hadoop配置情况，修改log_analysis.sh
- 项目根目录下，打开两个终端窗口，分别运行
  + <code>./create_log.sh</code>
  + <code>./log_analysis.sh</code>

## 二. Kafka + Spark Streaming实现男女淘宝购物数量动态展示DashBoard 
#### 说明
1. <code>Scala</code>和<code>Java</code>实现，<code>Java</code>实现中对流中无效数据项进行过滤。
2. <code>Scala</code>实现采用了<code>json4</code>作为序列化方式,在<code>Java</code>中用<code>json4</code>遇到了问题，自己实现的了序列化方案。
#### 环境依赖
在上面基础上增加:
- spark-streaming-kafka_2.10
- python相关(建议用virtualenv 创建python3虚拟环境)
<code>pip3 install flask flask-socketio kafka-python</code>

1. 基本描述:
- <code>KafkaProducer</code>动态从<code>pykafka/data/user_log.csv</code>(自己创建data目录并下载user_log.csv)[来源](https://pan.baidu.com/s/1cs02Nc).
文件动态处理消费记录，发送消费记录中的性别代号(0或１)到Kafka. 主题为'gender'
- Spark Streaming从Kafka主题'gender'读取处理消息,时间窗口为7秒
- Spark Streaming将处理后的数据发送给Kafka，主题为'result'
- Flask Web应用接受Kafka主题为'result'的消息，并利用flask-socketio将数据推送到浏览器
- 浏览器采用[highcharts.js](https://www.highcharts.com/)库动态展示结果

2. 使用
- <code>sbt clean package assembly</code>(需要添加sbt-assembly插件，见./project/plugins.sbt)
- <code>cd pykafka</code>，启动virtualenv环境<code>virtualenv python3 venv</code>
- <code>pip3 install flask flask-socketio kafka-python</code>
- 安装[Kafka](http://www-eu.apache.org/dist/kafka/0.10.2.1/kafka_2.10-0.10.2.1.tgz)
- kafka安装目录 <code>bin/zookeeper-server-start.sh config/zookeeper.properties &
                    bin/kafka-server-start.sh config/server.properties</code>
- <code>cd pykafka</code>，<code>python scripts/producer.py</code>
-  <code>dashboard.sh</code>(根据Spark和hadoop配置情况自行调整)
-  <code>cd pykafka</code>, <code>python app.py</code>
- 访问 localhost:5000

3. 结果
<img src="out.gif">

  


