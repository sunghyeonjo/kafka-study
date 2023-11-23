package kafka;

import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

public class ConsumerCommit {
    public static final Logger logger = LoggerFactory.getLogger(ConsumerCommit.class.getName());

    public static void main(String[] args) {
        String topicName = "sunghyeon-topic";

        Properties props = new Properties();
        props.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:9092");
        props.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.setProperty(ConsumerConfig.GROUP_ID_CONFIG, "group-03");
        props.setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");


        KafkaConsumer<String, String> kafkaConsumer = new KafkaConsumer<>(props);
        kafkaConsumer.subscribe(Collections.singletonList(topicName));

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

//        pollAutoCommit(kafkaConsumer);
//        pollCommitSync(kafkaConsumer);
        pollCommitAsync(kafkaConsumer);
    }

    private static void pollCommitAsync(KafkaConsumer<String, String> kafkaConsumer) {

        int loopCount = 0;
        try {
            while (true) {
                ConsumerRecords<String, String> consumerRecords = kafkaConsumer.poll(Duration.ofMillis(1000));
                logger.info("####### loopCnt : {} consumerRecords count : {}", loopCount++, consumerRecords.count());
                for (ConsumerRecord<String, String> record : consumerRecords) {
                    logger.info("record key : {}, record value : {}, partition : {}, offset : {}",
                            record.key(), record.value(), record.partition(), record.offset());
                }
                kafkaConsumer.commitAsync(new OffsetCommitCallback() {
                    @Override
                    public void onComplete(Map<TopicPartition, OffsetAndMetadata> offsets, Exception exception) {
                        if (exception != null) {
                            logger.error("offsets {} is not completed, error : {}", offsets, exception);
                        }
                    }
                });
            }
        } catch (WakeupException e) {
            logger.error("Wakeup exception has been called");
        } finally {
            logger.info("finally consumer is closing");
            kafkaConsumer.close();
        }
    }

    private static void pollCommitSync(KafkaConsumer<String, String> kafkaConsumer) {

        int loopCount = 0;
        try {
            while (true) {
                ConsumerRecords<String, String> consumerRecords = kafkaConsumer.poll(Duration.ofMillis(1000));
                logger.info("####### loopCnt : {} consumerRecords count : {}", loopCount++, consumerRecords.count());
                for (ConsumerRecord<String, String> record : consumerRecords) {
                    logger.info("record key : {}, record value : {}, partition : {}, offset : {}",
                            record.key(), record.value(), record.partition(), record.offset());
                }
                try {
                    kafkaConsumer.commitSync();
                    logger.info("commit sync has been called");
                } catch (CommitFailedException e) {
                    logger.error(e.getMessage());
                }
            }
        } catch (WakeupException e) {
            logger.error("Wakeup exception has been called");
        } finally {
            logger.info("finally consumer is closing");
            kafkaConsumer.close();
        }
    }

    public static void pollAutoCommit(KafkaConsumer<String, String> kafkaConsumer) {

        int loopCount = 0;

        try {
            while (true) {
                ConsumerRecords<String, String> consumerRecords = kafkaConsumer.poll(Duration.ofMillis(1000));
                logger.info("####### loopCnt : {} consumerRecords count : {}", loopCount++, consumerRecords.count());
                for (ConsumerRecord<String, String> record : consumerRecords) {
                    logger.info("record key : {}, record value : {}, partition : {}, offset : {}",
                            record.key(), record.value(), record.partition(), record.offset());
                }
                try{
                    logger.info("main thread is sleeping {}ms during while loop", 7000);
                    Thread.sleep(7000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
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
