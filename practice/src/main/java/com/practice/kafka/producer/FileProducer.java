package com.practice.kafka.producer;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

public class FileProducer {


    public static final Logger logger = LoggerFactory.getLogger(FileProducer.class.getName());

    public static void main(String[] args) {

        String topicName = "file-topic";

        // EC2 public IP : 13.125.174.166, UnknownHostException -> /etc/hosts 수정
        Properties props = new Properties();
        props.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:9092");
        props.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        // Kafka producer 객체
        KafkaProducer<String, String> kafkaProducer = new KafkaProducer<>(props);

        String filePath = "/Users/irteam/Desktop/projects/de/kafka/kafka-project/practice/src/main/resources/pizza_sample.txt";
        sendFileMessages(kafkaProducer, topicName, filePath);


        kafkaProducer.close();
    }

    private static void sendFileMessages(KafkaProducer<String, String> kafkaProducer, String topicName, String filePath) {
        String line = "";
        final String delimiter = ",";
        try (FileReader fileReader = new FileReader(filePath)) {
            BufferedReader bufferedReader = new BufferedReader(fileReader);

            while ((line = bufferedReader.readLine()) != null) {
                String[] tokens = line.split(delimiter);
                String key = tokens[0];
                StringBuffer value = new StringBuffer();

                for (int i = 1; i < tokens.length; i++) {
                    if (i != tokens.length - 1) {
                        value.append(tokens[i] + ",");
                    } else {
                        value.append(tokens[i]);
                    }
                }

                sendMessage(kafkaProducer, topicName, key, value.toString());
            }

        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void sendMessage(KafkaProducer<String, String> kafkaProducer, String topicName, String key, String value) {
        ProducerRecord<String, String> producerRecord = new ProducerRecord<>(topicName, key, value);
        logger.info("key : {}, value : {}", key, value);

        kafkaProducer.send(producerRecord, ((metadata, exception) -> {
            if (exception == null) {
                logger.info("\n##### record metadata received ##### \n" +
                        "partition:" + metadata.partition() + "\n" +
                        "offset:" + metadata.offset() + "\n" +
                        "timestamp:" + metadata.timestamp());
            } else {
                logger.error("exception error from broker :" + exception.getMessage());
            }
        }));

    }
}
