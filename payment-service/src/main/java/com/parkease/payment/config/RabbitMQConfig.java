package com.parkease.payment.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Bean
    public TopicExchange paymentExchange() {
        return new TopicExchange("parkease.payment.exchange", true, false);
    }

    @Bean
    public Queue notificationPaymentQueue() {
        return QueueBuilder.durable("parkease.payment.notification.queue").build();
    }

    @Bean
    public Binding notificationPaymentBinding(TopicExchange paymentExchange) {
        return BindingBuilder.bind(notificationPaymentQueue()).to(paymentExchange).with("payment.*");
    }

    // Declare the queue that BookingService publishes checkout events to.
    // Booking-service also declares this queue, but declaring it here ensures
    // it exists regardless of which service starts first.
    @Bean
    public Queue paymentInboundQueue() {
        return QueueBuilder.durable("parkease.payment.queue").build();
    }

    @Bean
    public TopicExchange bookingExchange() {
        return new TopicExchange("parkease.booking.exchange", true, false);
    }

    @Bean
    public Binding paymentInboundBinding() {
        return BindingBuilder.bind(paymentInboundQueue()).to(bookingExchange()).with("booking.checkout");
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
