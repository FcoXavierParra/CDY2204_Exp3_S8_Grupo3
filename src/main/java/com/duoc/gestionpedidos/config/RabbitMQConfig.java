package com.duoc.gestionpedidos.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Infraestructura de RabbitMQ para el sistema asincrono de guias de despacho.
 *
 * Se definen DOS colas y UN componente productor (ver {@code ProductorGuiaService})
 * que transmite mensajes hacia ambas, cumpliendo el requerimiento de la Semana 8:
 *
 *   - COLA 1 (principal): {@code guias.queue}
 *       Recibe todas las guias creadas. El consumidor ({@code GuiaConsumerListener})
 *       las lee y guarda en la tabla GUIA_PROCESADA de Oracle Cloud.
 *       Esta cola declara una Dead Letter Exchange: si el consumidor rechaza un
 *       mensaje (fallo de proceso), RabbitMQ lo reenvia automaticamente a la COLA 2.
 *
 *   - COLA 2 (errores / Dead Letter Queue): {@code guias.error.queue}
 *       Almacena todos los mensajes que fallaron. El productor tambien puede
 *       publicar aqui explicitamente cuando detecta un fallo antes de encolar.
 *
 * Flujo de enrutamiento:
 *   Productor -> guias.exchange --(guias.routingkey)--> guias.queue  (COLA 1)
 *   Fallo en consumidor          --(dead-letter)------> guias.dlx.exchange
 *   guias.dlx.exchange --(guias.error.routingkey)-----> guias.error.queue (COLA 2)
 */
@Configuration
public class RabbitMQConfig {

    @Value("${app.rabbitmq.exchange}")
    private String exchangeName;

    @Value("${app.rabbitmq.queue}")
    private String queueName;

    @Value("${app.rabbitmq.routingkey}")
    private String routingKey;

    @Value("${app.rabbitmq.dlx}")
    private String dlxName;

    @Value("${app.rabbitmq.error-queue}")
    private String errorQueueName;

    @Value("${app.rabbitmq.error-routingkey}")
    private String errorRoutingKey;

    // ── Exchanges ────────────────────────────────────────────────────────────

    /** Exchange principal (Direct) por el que el productor publica las guias. */
    @Bean
    public DirectExchange guiasExchange() {
        return new DirectExchange(exchangeName, true, false);
    }

    /** Dead Letter Exchange: recibe los mensajes rechazados por el consumidor. */
    @Bean
    public DirectExchange guiasDlxExchange() {
        return new DirectExchange(dlxName, true, false);
    }

    // ── Colas ────────────────────────────────────────────────────────────────

    /**
     * COLA 1 (principal). Durable y con Dead Letter Exchange configurada:
     * cuando un mensaje es rechazado sin re-encolar, RabbitMQ lo enruta al DLX
     * usando la routing key de error, dejandolo en la COLA 2.
     */
    @Bean
    public Queue guiasQueue() {
        return QueueBuilder.durable(queueName)
                .withArgument("x-dead-letter-exchange", dlxName)
                .withArgument("x-dead-letter-routing-key", errorRoutingKey)
                .build();
    }

    /** COLA 2 (errores / DLQ). Durable: conserva todos los mensajes fallidos. */
    @Bean
    public Queue guiasErrorQueue() {
        return QueueBuilder.durable(errorQueueName).build();
    }

    // ── Bindings ───────────────────────────────────────────────────────────────

    @Bean
    public Binding guiasBinding(Queue guiasQueue, DirectExchange guiasExchange) {
        return BindingBuilder.bind(guiasQueue).to(guiasExchange).with(routingKey);
    }

    @Bean
    public Binding guiasErrorBinding(Queue guiasErrorQueue, DirectExchange guiasDlxExchange) {
        return BindingBuilder.bind(guiasErrorQueue).to(guiasDlxExchange).with(errorRoutingKey);
    }

    // ── Conversor JSON + RabbitTemplate ──────────────────────────────────────

    /** Permite enviar/recibir objetos Java (POJO) serializados como JSON. */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         MessageConverter jsonMessageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter);
        return rabbitTemplate;
    }
}
