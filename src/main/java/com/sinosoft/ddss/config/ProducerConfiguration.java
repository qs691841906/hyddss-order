package com.sinosoft.ddss.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

/**
 * 创建rabbitmq 动态绑定exchange，routingkey，queue
 * 时间：2018年5月14日14:41:52
 */
public class ProducerConfiguration {
	private String queueName;
	private String routingKey;
	private RabbitTemplate rabbitTemplate;
	private String exchange;

	public ProducerConfiguration() {

	}

	public ProducerConfiguration(String exchange, String queueName, String routingKey) {
		try {
			this.queueName = queueName;
			this.routingKey = routingKey;
			this.exchange = exchange;
			this.rabbitTemplate = rabbitTemplate();
			RabbitAdmin admin = new RabbitAdmin(this.rabbitTemplate.getConnectionFactory());//GQ 3
			admin.declareQueue(new Queue(this.queueName));
			admin.declareExchange(new TopicExchange(exchange));
			// 
			admin.setAutoStartup(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String getExchange() {
		return exchange;
	}

	public void setExchange(String exchange) {
		this.exchange = exchange;
	}

	public void setQueueName(String queueName) {
		this.queueName = queueName;
	}

	public void setRoutingKey(String routingKey) {
		this.routingKey = routingKey;
	}

	public String getQueueName() {
		return queueName;
	}

	public String getRoutingKey() {
		return routingKey;
	}

	public RabbitTemplate rabbitTemplate() {
		RabbitTemplate template = new RabbitTemplate(connectionFactory());
		// The routing key is set to the name of the queue by the broker for the
		// default exchange.
		template.setRoutingKey(this.routingKey);
		// Where we will synchronously receive messages from
		template.setQueue(this.queueName);
		// template.setMessageConverter(new JsonMessageConverter());
		return template;
	}

	public ConnectionFactory connectionFactory() {
		CachingConnectionFactory connectionFactory = new CachingConnectionFactory("172.16.25.104");
		connectionFactory.setUsername("hyj");
		connectionFactory.setPassword("hyj");
		return connectionFactory;
	}

	public void send(String s) {

		this.rabbitTemplate.convertAndSend(s);
	}

	public void send(String exchange, String routingKey, Object msg) {

		this.rabbitTemplate.convertAndSend(exchange, routingKey, msg);
	}
}