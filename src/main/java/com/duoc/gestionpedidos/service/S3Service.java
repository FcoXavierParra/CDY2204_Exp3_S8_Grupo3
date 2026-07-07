package com.duoc.gestionpedidos.service;

import com.duoc.gestionpedidos.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;

/**
 * Operaciones de los PDF de guias de despacho en AWS S3.
 * Estructura del bucket: {bucketName}/guias/{numeroGuia}/guia_{numeroGuia}.pdf
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class S3Service {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    /** Sube (o reemplaza) el PDF generado de la guia. */
    public String subirGuia(String numeroGuia, byte[] contenidoPdf, String contentType) {
        String key = buildKey(numeroGuia);
        log.info("Subiendo guia a S3: {}/{}", bucketName, key);

        s3Client.putObject(
            PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(contentType)
                .contentLength((long) contenidoPdf.length)
                .build(),
            RequestBody.fromBytes(contenidoPdf)
        );
        return key;
    }

    /** Descarga el PDF de la guia desde S3. */
    public byte[] descargarGuia(String numeroGuia) {
        String key = buildKey(numeroGuia);
        log.info("Descargando guia desde S3: {}/{}", bucketName, key);
        try {
            ResponseBytes<GetObjectResponse> response = s3Client.getObjectAsBytes(
                GetObjectRequest.builder().bucket(bucketName).key(key).build());
            return response.asByteArray();
        } catch (NoSuchKeyException e) {
            throw new ResourceNotFoundException(
                "PDF de la guia no encontrado en S3: " + numeroGuia);
        }
    }

    /** Reemplaza el PDF de la guia en S3 con el archivo subido por el usuario. */
    public String modificarGuia(String numeroGuia, MultipartFile archivo) throws IOException {
        String key = buildKey(numeroGuia);
        log.info("Modificando guia en S3: {}/{}", bucketName, key);
        verificarExistencia(numeroGuia, key);

        s3Client.putObject(
            PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(archivo.getContentType())
                .contentLength(archivo.getSize())
                .build(),
            RequestBody.fromBytes(archivo.getBytes())
        );
        return key;
    }

    /** Elimina el PDF de la guia de S3. */
    public void borrarGuia(String numeroGuia) {
        String key = buildKey(numeroGuia);
        log.info("Borrando guia de S3: {}/{}", bucketName, key);
        verificarExistencia(numeroGuia, key);
        s3Client.deleteObject(
            DeleteObjectRequest.builder().bucket(bucketName).key(key).build());
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private String buildKey(String numeroGuia) {
        return "guias/" + numeroGuia + "/guia_" + numeroGuia + ".pdf";
    }

    private void verificarExistencia(String numeroGuia, String key) {
        try {
            s3Client.headObject(
                HeadObjectRequest.builder().bucket(bucketName).key(key).build());
        } catch (NoSuchKeyException e) {
            throw new ResourceNotFoundException(
                "PDF de la guia no encontrado en S3: " + numeroGuia);
        }
    }
}
