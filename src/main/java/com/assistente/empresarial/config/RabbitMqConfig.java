package com.assistente.empresarial.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
public class RabbitMqConfig {

    public static final String QUEUE_DOCUMENTO_RAG = "documento.rag.processamento.queue";
    public static final String EXCHANGE_DOCUMENTO_RAG = "documento.rag.exchange";
    public static final String ROUTING_KEY_DOCUMENTO_RAG = "documento.rag.processar";

    @Bean
    public Queue documentoRagQueue() {
        return QueueBuilder.durable(QUEUE_DOCUMENTO_RAG).build();
    }

    @Bean
    public DirectExchange documentoRagExchange() {
        return new DirectExchange(EXCHANGE_DOCUMENTO_RAG);
    }

    @Bean
    public Binding documentoRagBinding(Queue documentoRagQueue, DirectExchange documentoRagExchange) {
        return BindingBuilder.bind(documentoRagQueue).to(documentoRagExchange).with(ROUTING_KEY_DOCUMENTO_RAG);
    }

    @Bean
    public MessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
