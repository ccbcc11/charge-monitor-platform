package com.ccbcc.charge.monitor.config;

import com.ccbcc.charge.monitor.common.constants.RabbitMqConstants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置
 *
 * 当前用于设备数据异步告警链路：
 *
 * DeviceDataServiceImpl
 *   -> 发送设备数据上报消息
 *   -> RabbitMQ
 *   -> DeviceDataAlarmConsumer
 *   -> AlarmDetectService
 */
@EnableRabbit
@Configuration
public class RabbitMqConfig {

    /**
     * JSON 消息转换器
     *
     * 作用：
     * 让 RabbitTemplate 发送对象时自动转 JSON；
     * 让 @RabbitListener 消费消息时自动反序列化成 Java 对象。
     */
    @Bean
    public MessageConverter jacksonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * RabbitTemplate 配置
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         MessageConverter jacksonMessageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jacksonMessageConverter);
        return rabbitTemplate;
    }

    /**
     * RabbitListener 容器工厂配置
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter jacksonMessageConverter
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jacksonMessageConverter);
        return factory;
    }

    /**
     * 设备数据告警交换机
     */
    @Bean
    public DirectExchange deviceDataAlarmExchange() {
        return ExchangeBuilder
                .directExchange(RabbitMqConstants.DEVICE_DATA_ALARM_EXCHANGE)
                .durable(true)
                .build();
    }

    /**
     * 设备数据告警队列
     */
    @Bean
    public Queue deviceDataAlarmQueue() {
        return QueueBuilder
                .durable(RabbitMqConstants.DEVICE_DATA_ALARM_QUEUE)
                .build();
    }

    /**
     * 绑定关系：
     * exchange + routingKey -> queue
     */
    @Bean
    public Binding deviceDataAlarmBinding(Queue deviceDataAlarmQueue,
                                          DirectExchange deviceDataAlarmExchange) {
        return BindingBuilder
                .bind(deviceDataAlarmQueue)
                .to(deviceDataAlarmExchange)
                .with(RabbitMqConstants.DEVICE_DATA_ALARM_ROUTING_KEY);
    }
}