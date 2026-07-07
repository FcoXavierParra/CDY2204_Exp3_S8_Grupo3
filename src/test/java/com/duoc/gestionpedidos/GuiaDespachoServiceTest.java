package com.duoc.gestionpedidos;

import com.duoc.gestionpedidos.dto.GuiaRequestDTO;
import com.duoc.gestionpedidos.exception.ResourceNotFoundException;
import com.duoc.gestionpedidos.model.EstadoGuia;
import com.duoc.gestionpedidos.model.GuiaDespacho;
import com.duoc.gestionpedidos.repository.GuiaDespachoRepository;
import com.duoc.gestionpedidos.service.GuiaDespachoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GuiaDespachoServiceTest {

    @Mock
    private GuiaDespachoRepository repository;

    @InjectMocks
    private GuiaDespachoService service;

    private GuiaRequestDTO nuevaGuiaDto() {
        GuiaRequestDTO dto = new GuiaRequestDTO();
        dto.setNumeroGuia("G-001");
        dto.setTransportista("TransporteX");
        dto.setOrigen("Santiago");
        dto.setDestino("Valparaiso");
        dto.setDescripcion("10 cajas");
        dto.setFechaDespacho(LocalDate.of(2026, 6, 27));
        return dto;
    }

    @Test
    void crear_guardaConEstadoGenerada() {
        when(repository.existsByNumeroGuia("G-001")).thenReturn(false);
        when(repository.save(any(GuiaDespacho.class))).thenAnswer(inv -> inv.getArgument(0));

        GuiaDespacho guia = service.crear(nuevaGuiaDto());

        assertThat(guia.getNumeroGuia()).isEqualTo("G-001");
        assertThat(guia.getEstado()).isEqualTo(EstadoGuia.GENERADA);
        verify(repository).save(any(GuiaDespacho.class));
    }

    @Test
    void crear_rechazaNumeroDuplicado() {
        when(repository.existsByNumeroGuia("G-001")).thenReturn(true);

        assertThatThrownBy(() -> service.crear(nuevaGuiaDto()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ya existe");

        verify(repository, never()).save(any());
    }

    @Test
    void obtenerPorId_lanzaNotFoundCuandoNoExiste() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerPorId(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void registrarSubidaS3_actualizaEstadoYKey() {
        GuiaDespacho guia = GuiaDespacho.builder()
                .id(1L).numeroGuia("G-001").estado(EstadoGuia.GENERADA).build();
        when(repository.findById(1L)).thenReturn(Optional.of(guia));
        when(repository.save(any(GuiaDespacho.class))).thenAnswer(inv -> inv.getArgument(0));

        GuiaDespacho result = service.registrarSubidaS3(1L, "guias/G-001/guia_G-001.pdf");

        assertThat(result.getEstado()).isEqualTo(EstadoGuia.ALMACENADA_S3);
        assertThat(result.getS3Key()).isEqualTo("guias/G-001/guia_G-001.pdf");
    }
}
