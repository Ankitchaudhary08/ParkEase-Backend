package com.parkease.booking.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String BOOKING_EXCHANGE = "parkease.booking.exchange";
    public static final String NOTIFICATION_QUEUE = "parkease.notification.queue";
    public static final String ANALYTICS_QUEUE = "parkease.analytics.queue";
    public static final String PAYMENT_QUEUE = "parkease.payment.queue";

    @Bean
    public TopicExchange bookingExchange() {
        return new TopicExchange(BOOKING_EXCHANGE, true, false);
    }

    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(NOTIFICATION_QUEUE).build();
    }

    @Bean
    public Queue analyticsQueue() {
        return QueueBuilder.durable(ANALYTICS_QUEUE).build();
    }

    @Bean
    public Queue paymentQueue() {
        return QueueBuilder.durable(PAYMENT_QUEUE).build();
    }

    @Bean
    public Binding notificationBinding(TopicExchange bookingExchange) {
        return BindingBuilder.bind(notificationQueue()).to(bookingExchange).with("booking.*");
    }

    @Bean
    public Binding analyticsBinding(TopicExchange bookingExchange) {
        return BindingBuilder.bind(analyticsQueue()).to(bookingExchange).with("booking.*");
    }

    @Bean
    public Binding paymentBinding(TopicExchange bookingExchange) {
        return BindingBuilder.bind(paymentQueue()).to(bookingExchange).with("booking.checkout");
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
}
