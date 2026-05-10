package com.parkease.notifanalytics.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Booking exchange + queues (declared here so they exist even if booking-service starts later)
    @Bean
    public TopicExchange bookingExchange() {
        return new TopicExchange("parkease.booking.exchange", true, false);
    }

    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable("parkease.notification.queue").build();
    }

    @Bean
    public Queue analyticsQueue() {
        return QueueBuilder.durable("parkease.analytics.queue").build();
    }

    @Bean
    public Binding notificationBinding(TopicExchange bookingExchange) {
        return BindingBuilder.bind(notificationQueue()).to(bookingExchange).with("booking.*");
    }

    @Bean
    public Binding analyticsBinding(TopicExchange bookingExchange) {
        return BindingBuilder.bind(analyticsQueue()).to(bookingExchange).with("booking.*");
    }

    // Payment exchange + queue (declared here so they exist even if payment-service starts later)
    @Bean
    public TopicExchange paymentExchange() {
        return new TopicExchange("parkease.payment.exchange", true, false);
    }

    @Bean
    public Queue paymentNotificationQueue() {
        return QueueBuilder.durable("parkease.payment.notification.queue").build();
    }

    @Bean
    public Binding paymentNotificationBinding(TopicExchange paymentExchange) {
        return BindingBuilder.bind(paymentNotificationQueue()).to(paymentExchange).with("payment.*");
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory factory) {
        RabbitTemplate template = new RabbitTemplate(factory);
        template.setMessageConverter(messageConverter());
        return template;
    }

    // Prevent fatal crash if a queue is temporarily missing on startup
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory factory) {
        SimpleRabbitListenerContainerFactory f = new SimpleRabbitListenerContainerFactory();
        f.setConnectionFactory(factory);
        f.setMessageConverter(messageConverter());
        f.setMissingQueuesFatal(false);
        return f;
    }
}
