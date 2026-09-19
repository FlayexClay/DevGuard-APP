package com.devguard.shared.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;

@Component
public class EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);

    private final KafkaSender<String, String> sender;
    private final ObjectMapper mapper;

    public EventPublisher(KafkaSender<String, String> sender, ObjectMapper eventObjectMapper) {
        this.sender = sender;
        this.mapper = eventObjectMapper;
    }

    public Mono<Void> publish(String topic, String partitionKey, Object payload) {
        return Mono.fromCallable(() -> mapper.writeValueAsString(payload))
                .map(json -> SenderRecord.create(
                        new ProducerRecord<>(topic, partitionKey, json),
                        partitionKey))
                .flatMap(record -> sender.send(Mono.just(record))
                        .next()
                        .flatMap(result -> result.exception() != null
                                ? Mono.error(result.exception())
                                : Mono.empty()))
                .doOnSuccess(v -> log.debug("Evento publicado en {} con clave {}",
                        topic, partitionKey))
                .then();
    }

}
