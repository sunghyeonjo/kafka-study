package kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public class ConsumerMTopicRebalance {
    public static final Logger logger = LoggerFactory.getLogger(ConsumerMTopicRebalance.class.getName());

    public static void main(String[] args) {
        String topicName = "pizza-topic";

        Properties props = new Properties();
        props.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:9092");
        props.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
//        props.setProperty(ConsumerConfig.GROUP_ID_CONFIG, "group-mtopic");
        props.setProperty(ConsumerConfig.GROUP_ID_CONFIG, "group-assign");

        KafkaConsumer<String, String> kafkaConsumer = new KafkaConsumer<>(props);
        kafkaConsumer.subscribe(List.of("topic-p3-t1", "topic-p3-t2"));

        // main thread
        Thread mainThread = Thread.currentThread();

        // main 쓰레드 종료 시 별도 thread로 kafkaConsumer wakeup() 메서드를 호출하게 한다.
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                logger.info("main program starts to exit by calling wakeup");
                kafkaConsumer.wakeup();

                try {
                    mainThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        try {
            while (true) {
                ConsumerRecords<String, String> consumerRecords = kafkaConsumer.poll(Duration.ofMillis(1000));
                for (ConsumerRecord<String, String> record : consumerRecords) {
                    logger.info("topic : {}, record key : {}, record value : {}, partition : {}", record.topic(), record.key(), record.value(), record.partition());

                }
            }
        } catch (WakeupException e) {
            logger.error("Wakeup exception has been called");
        } finally {
            logger.info("finally consumer is closing");
            kafkaConsumer.close();
        }
    }
}
