package kafka;

import com.github.javafaker.Faker;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ExecutionException;

public class PizzaProducer {

    public static final Logger logger = LoggerFactory.getLogger(PizzaProducer.class.getName());

    public static void sendPizzaMessage(KafkaProducer<String, String> kafkaProducer,
                                        String topicName, int iterCount,
                                        int interIntervalMillis, int intervalMillis,
                                        int intervalCount, boolean sync) {

        PizzaMessage pizzaMessage = new PizzaMessage();
        int iterSeq = 0;
        long seed = 2023;
        Random random = new Random(seed);
        Faker faker = Faker.instance(random);

        long startTime = System.currentTimeMillis();

        while( iterSeq++ != iterCount ) {
            HashMap<String, String> pMessage = pizzaMessage.produce_msg(faker, random, iterSeq);
            ProducerRecord<String, String> producerRecord = new ProducerRecord<>(topicName, pMessage.get("key"), pMessage.get("message"));
            sendMessage(kafkaProducer, producerRecord, pMessage, sync);

            if((intervalCount > 0) && (iterSeq % intervalCount == 0)) {
                try {
                    logger.info("####### IntervalCount: {} intervalMillis: {} #########", intervalCount, intervalMillis);
                    Thread.sleep(intervalMillis);
                } catch (InterruptedException e) {
                    logger.error(e.getMessage());
                }
            }

            if(interIntervalMillis > 0) {
                try {
                    logger.info("interIntervalMillis: {}", interIntervalMillis);
                    Thread.sleep(interIntervalMillis);
                } catch (InterruptedException e) {
                    logger.error(e.getMessage());
                }
            }

        }
        long endTime = System.currentTimeMillis();
        long timeElapsed = endTime - startTime;

        logger.info("{} millisecond elapsed for {} iterations", timeElapsed, iterCount);

    }

    public static void sendMessage(KafkaProducer<String, String> kafkaProducer,
                                   ProducerRecord<String, String> producerRecord,
                                   HashMap<String, String> pMessage, boolean sync) {
        if(!sync) {
            kafkaProducer.send(producerRecord, (metadata, exception) -> {
                if (exception == null) {
                    logger.info("async message: {} partition: {} offset: {}", pMessage.get("key"), metadata.partition(), metadata.offset());
                } else {
                    logger.error("exception error from broker " + exception.getMessage());
                }
            });
        } else {
            try {
                RecordMetadata metadata = kafkaProducer.send(producerRecord).get();
                logger.info("sync message: {} partition: {} offset: {}", pMessage.get("key"), metadata.partition(), metadata.offset());
            } catch (ExecutionException | InterruptedException e) {
                logger.error(e.getMessage());
            }
        }

    }

    public static void main(String[] args) {

        String topicName = "sunghyeon-topic";

        //KafkaProducer configuration setting
        // null, "hello world"

        Properties props  = new Properties();
        props.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:9092");
        props.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        // delivery.timeout.ms should be equal to or larger than linger.ms + request.timeout.ms
        props.setProperty(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, "50000");
        props.setProperty(ProducerConfig.ACKS_CONFIG, "all");

        // When set to ‘true’, the producer will ensure that exactly one copy of each message is written in the stream.
        // If ‘false’, producer retries due to broker failures, etc., may write duplicates of the retried message in the stream.
        // Note that enabling idempotence requires max.in.flight.requests.per.connection to be less than or equal to 5 (with message ordering preserved for any allowable value),
        // retries to be greater than 0, and acks must be ‘all’.
        props.setProperty(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");


        //KafkaProducer object creation
        KafkaProducer<String, String> kafkaProducer = new KafkaProducer<>(props);

        sendPizzaMessage(kafkaProducer, topicName, -1, 1000,0,0, true);

        kafkaProducer.close();

    }
}