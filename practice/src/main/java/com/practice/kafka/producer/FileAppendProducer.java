package com.practice.kafka.producer;

import com.practice.kafka.event.EventHandler;
import com.practice.kafka.event.FileEventHandler;
import com.practice.kafka.event.FileEventSource;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Properties;

public class FileAppendProducer {

    public static final Logger logger = LoggerFactory.getLogger(FileAppendProducer.class.getName());


    public static void main(String[] args) {
        String topicName = "file-topic";

        // EC2 public IP : 13.125.174.166, UnknownHostException -> /etc/hosts 수정
        Properties props = new Properties();
        props.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:9092");
        props.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        // Kafka producer 객체
        KafkaProducer<String, String> kafkaProducer = new KafkaProducer<>(props);
        boolean sync = false;

        File file = new File("/Users/irteam/Desktop/projects/de/kafka/kafka-project/practice/src/main/resources/pizza_append.txt");
        EventHandler eventHandler = new FileEventHandler(kafkaProducer, topicName, sync);
        FileEventSource fileEventSource = new FileEventSource(1000, file, eventHandler);

        Thread fileEventSourceThread = new Thread(fileEventSource);

        fileEventSourceThread.start();

        try {
            fileEventSourceThread.join();
        } catch (InterruptedException e) {
            logger.error(e.getMessage());
        } finally {
            kafkaProducer.close();
        }

    }
}
