package com.duoc.gestionpedidos.listener;

import com.duoc.gestionpedidos.dto.GuiaMensaje;
import com.duoc.gestionpedidos.service.GuiaProcesadaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Componente CONSUMIDOR: escucha permanentemente la COLA 1 (guias.queue) y, por
 * cada mensaje, guarda la guia en la tabla GUIA_PROCESADA de Oracle Cloud.
 *
 * Manejo de errores: si el mensaje es invalido o su procesamiento falla, se lanza
 * {@link AmqpRejectAndDontRequeueException}. Como la COLA 1 declara una Dead Letter
 * Exchange (ver {@code RabbitMQConfig}), RabbitMQ reenvia automaticamente ese
 * mensaje a la COLA 2 (guias.error.queue), donde quedan almacenados los mensajes
 * con errores. Asi ninguna guia se pierde.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GuiaConsumerListener {

    private final GuiaProcesadaService guiaProcesadaService;

    @RabbitListener(queues = "${app.rabbitmq.queue}")
    public void recibirGuia(GuiaMensaje mensaje) {
        log.info("[COLA 1] Mensaje recibido: guia {} (transportista {})",
                mensaje.getNumeroGuia(), mensaje.getTransportista());

        // Bandera de demostracion: fuerza el envio a la COLA 2 de errores.
        if (mensaje.isSimularError()) {
            log.warn("[COLA 1] Guia {} marcada con simularError=true -> se rechaza hacia la COLA 2",
                    mensaje.getNumeroGuia());
            throw new AmqpRejectAndDontRequeueException(
                    "Error simulado para evidenciar la cola de errores (COLA 2)");
        }

        // Validacion basica: un mensaje sin numero de guia se considera fallido.
        if (mensaje.getNumeroGuia() == null || mensaje.getNumeroGuia().isBlank()) {
            log.error("[COLA 1] Mensaje sin numero de guia -> se rechaza hacia la COLA 2");
            throw new AmqpRejectAndDontRequeueException("Mensaje invalido: numeroGuia vacio");
        }

        try {
            guiaProcesadaService.guardarDesdeMensaje(mensaje);
        } catch (Exception e) {
            log.error("[COLA 1] Error al persistir la guia {} en Oracle. Se deriva a la COLA 2.",
                    mensaje.getNumeroGuia(), e);
            // No re-encolar: enviar a la Dead Letter Queue (COLA 2).
            throw new AmqpRejectAndDontRequeueException("Fallo al guardar en Oracle Cloud", e);
        }
    }
}
