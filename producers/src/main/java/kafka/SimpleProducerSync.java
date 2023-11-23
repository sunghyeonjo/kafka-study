package kafka;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.ExecutionException;

public class SimpleProducerSync {

    public static final Logger logger = LoggerFactory.getLogger(SimpleProducerSync.class.getName());
    public static void main(String[] args) {

        String topicName = "simple-topic";

        // EC2 public IP : 13.125.174.166, UnknownHostException -> /etc/hosts 수정
        Properties props = new Properties();
        props.setProperty("bootstrap.servers", "13.125.174.166:9092");
        props.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "13.125.174.166:9092");
        props.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        // Kafka producer 객체
        KafkaProducer<String, String> kafkaProducer = new KafkaProducer<>(props);

        // ProducerRecord 객체 생성
        ProducerRecord<String, String> producerRecord = new ProducerRecord<>(topicName, "Hello world");

        // Message send
        try {
            RecordMetadata recordMetadata = kafkaProducer.send(producerRecord).get();
            logger.info("\n##### record metadata received ##### \n" +
                    "partition:" + recordMetadata.partition() + "\n" +
                    "offset:" + recordMetadata.offset() + "\n" +
                    "timestamp:" + recordMetadata.timestamp());
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        } finally {
            kafkaProducer.close();
        }
        kafkaProducer.flush();
    }
}
