package kafka;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class ProducerAsyncCustomCB {

    public static final Logger logger = LoggerFactory.getLogger(ProducerAsyncCustomCB.class.getName());
    public static void main(String[] args) {

        String topicName = "multipart-topic";

        // EC2 public IP : 13.125.174.166, UnknownHostException -> /etc/hosts 수정
        Properties props = new Properties();
        props.setProperty("bootstrap.servers", "13.125.174.166:9092");
        props.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "13.125.174.166:9092");
        props.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, IntegerSerializer.class.getName());
        props.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        // Kafka producer 객체
        KafkaProducer<Integer, String> kafkaProducer = new KafkaProducer<>(props);

        for (int seq = 0; seq < 20; seq++) {
            // ProducerRecord 객체 생성
            ProducerRecord<Integer, String> producerRecord = new ProducerRecord<>(topicName, seq, "Hello world" + seq);
            Callback callback = new CustomCallback(seq);

            // Message send
            kafkaProducer.send(producerRecord, callback);
        }

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        kafkaProducer.close();
    }
}
