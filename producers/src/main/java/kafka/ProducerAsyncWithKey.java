package kafka;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class ProducerAsyncWithKey {

    public static final Logger logger = LoggerFactory.getLogger(ProducerAsyncWithKey.class.getName());
    public static void main(String[] args) {

        String topicName = "multipart-topic";

        // EC2 public IP : 13.125.174.166, UnknownHostException -> /etc/hosts 수정
        Properties props = new Properties();
        props.setProperty("bootstrap.servers", "13.125.174.166:9092");
        props.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "13.125.174.166:9092");
        props.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        // Kafka producer 객체
        KafkaProducer<String, String> kafkaProducer = new KafkaProducer<>(props);

        for (int seq = 0; seq < 20; seq++) {
            // ProducerRecord 객체 생성
            ProducerRecord<String, String> producerRecord = new ProducerRecord<>(topicName, String.valueOf(seq), "Hello world" + seq);

            // Message send
            kafkaProducer.send(producerRecord, (metadata, exception) -> {
                if (exception == null) {
                    logger.info("\n##### record metadata received ##### \n" +
                            "partition:" + metadata.partition() + "\n" +
                            "offset:" + metadata.offset() + "\n" +
                            "timestamp:" + metadata.timestamp());
                } else {
                    logger.error("exception error from broker :" + exception.getMessage());
                }
            });
        }

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        kafkaProducer.close();
    }
}
