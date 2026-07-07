package com.duoc.gestionpedidos.service;

import com.duoc.gestionpedidos.dto.GuiaMensaje;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Componente PRODUCTOR: transmite las guias de despacho hacia las colas RabbitMQ.
 *
 * Es el "un (1) componente que transmite mensajes en ambas colas" que pide la
 * actividad de la Semana 8:
 *   - En operacion normal publica hacia la COLA 1 (guias.queue) a traves del
 *     exchange principal.
 *   - Si la publicacion a la COLA 1 falla, captura el error y publica el mensaje
 *     directamente en la COLA 2 (guias.error.queue) a traves de la Dead Letter
 *     Exchange, para que ninguna guia se pierda.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductorGuiaService {

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.exchange}")
    private String exchangeName;

    @Value("${app.rabbitmq.routingkey}")
    private String routingKey;

    @Value("${app.rabbitmq.dlx}")
    private String dlxName;

    @Value("${app.rabbitmq.error-routingkey}")
    private String errorRoutingKey;

    /**
     * Publica la guia en la COLA 1. Si el envio falla, la deriva a la COLA 2.
     *
     * @return true si se envio a la cola principal, false si termino en la de errores.
     */
    public boolean enviarGuia(GuiaMensaje mensaje) {
        try {
            log.info("Publicando guia {} en la COLA 1 ('{}')", mensaje.getNumeroGuia(), routingKey);
            rabbitTemplate.convertAndSend(exchangeName, routingKey, mensaje);
            log.info("Guia {} enviada correctamente a la cola principal", mensaje.getNumeroGuia());
            return true;
        } catch (Exception e) {
            log.error("Fallo al publicar la guia {} en la COLA 1. Se deriva a la COLA 2 de errores.",
                    mensaje.getNumeroGuia(), e);
            enviarACola2Error(mensaje);
            return false;
        }
    }

    /** Publica un mensaje directamente en la COLA 2 (errores) via la Dead Letter Exchange. */
    public void enviarACola2Error(GuiaMensaje mensaje) {
        rabbitTemplate.convertAndSend(dlxName, errorRoutingKey, mensaje);
        log.warn("Guia {} almacenada en la COLA 2 de errores ('{}')",
                mensaje.getNumeroGuia(), errorRoutingKey);
    }
}
