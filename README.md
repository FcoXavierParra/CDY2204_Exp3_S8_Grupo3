# Sistema Asíncrono de Gestión de Pedidos y Guías de Despacho — CDY2204 (Semana 8)

Backend **Spring Boot** securitizado para una empresa transportista, ahora con **procesamiento asíncrono mediante colas RabbitMQ** y persistencia en **Oracle Cloud**. Continúa el caso de las Experiencias 1 y 2. Integra:

- **RabbitMQ** (en Docker) → **2 colas**: cola principal + cola de errores (Dead Letter Queue).
- **Oracle Cloud (Autonomous DB)** → persistencia de las guías (2 tablas).
- **Azure AD B2C** (IDaaS) → autenticación con JWT y 2 roles (`gestor` / `consulta`).
- **AWS API Gateway** → publica y securitiza los endpoints.
- **AWS S3** → almacena los PDF de las guías de despacho.
- **Docker + EC2** → despliegue en contenedores.
- **GitHub Actions** → CI/CD (build → **Trivy** → push a Docker Hub → deploy en EC2).

## Arquitectura asíncrona

```
Cliente (Postman)
   │  Authorization: Bearer <JWT de Azure AD B2C>
   ▼
AWS API Gateway (HTTP API + Autorizador JWT)
   ▼
Spring Boot en Docker (EC2)          ┌─────────────── RabbitMQ (Docker) ───────────────┐
   ├─ Spring Security (roles)        │                                                  │
   ├─ POST /api/guias ──publica────► │ guias.exchange ─(rk)─► guias.queue      (COLA 1) │
   │      (ProductorGuiaService)     │                          │ (si falla: dead-letter)│
   │                                 │ guias.dlx.exchange ─► guias.error.queue (COLA 2) │
   ├─ @RabbitListener(COLA 1) ◄──────┘                                                  │
   │      (GuiaConsumerListener) ──► guarda en Oracle Cloud (tabla GUIA_PROCESADA)      │
   ├─ JPA (Oracle): GUIA_DESPACHO + GUIA_PROCESADA                                      │
   └─ AWS S3 (PDF de las guías)                                                         │
```

**Flujo:** al crear una guía se guarda en `GUIA_DESPACHO` y se **publica en la COLA 1**. El consumidor la lee y la persiste en la tabla **nueva** `GUIA_PROCESADA` de Oracle Cloud. Si el procesamiento falla (o se envía `simularError=true`), el mensaje se deriva a la **COLA 2** de errores.

## Endpoints (todos securitizados)

| # | Método | Ruta | Rol | Descripción |
|---|--------|------|-----|-------------|
| 1 | POST | `/api/guias?simularError=false` | GESTOR | Crear guía **y enviarla a la COLA 1** |
| 2 | POST | `/api/guias/{id}/s3` | GESTOR | Generar PDF y subir a S3 |
| 3 | GET | `/api/guias/{id}/descargar` | GESTOR, CONSULTA | Descargar el PDF desde S3 |
| 4 | PUT | `/api/guias/{id}` | GESTOR | Actualizar / modificar guía |
| 5 | DELETE | `/api/guias/{id}` | GESTOR | Eliminar guía (y su PDF en S3) |
| 6 | GET | `/api/guias?transportista=&fecha=` | GESTOR | Consultar por transportista y fecha |
| 7 | GET | `/api/guias-procesadas` | GESTOR | Listar las guías consumidas y guardadas en Oracle |

`GET /health` es público. `CONSULTA` **solo** puede descargar; cualquier otro endpoint responde **403**. Sin token → **401**.

| Rol (claim `extension_rol`) | Permisos |
|---|---|
| `gestor` | Todos los endpoints |
| `consulta` | Solo descargar guías |

---

## 1. Ejecución local

```bash
cp .env.example .env               # completa Azure, AWS, Oracle
# coloca el wallet de Oracle descomprimido en ./wallet  (ignorado por git)
docker compose up --build          # levanta RabbitMQ + backend
curl http://localhost:8080/health
```

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Consola RabbitMQ: `http://localhost:15672` (usuario `guest` / clave `guest`)

---

## 2. Colas RabbitMQ

Definidas en [`RabbitMQConfig`](src/main/java/com/duoc/gestionpedidos/config/RabbitMQConfig.java):

| Elemento | Nombre | Descripción |
|---|---|---|
| Exchange principal | `guias.exchange` (Direct) | Recibe las guías publicadas por el productor |
| **COLA 1** | `guias.queue` | Cola principal; el consumidor guarda en Oracle. Declara Dead Letter Exchange |
| Dead Letter Exchange | `guias.dlx.exchange` (Direct) | Enruta los mensajes rechazados hacia la COLA 2 |
| **COLA 2** | `guias.error.queue` | Almacena los mensajes con errores |

- **Productor:** [`ProductorGuiaService`](src/main/java/com/duoc/gestionpedidos/service/ProductorGuiaService.java) — publica a la COLA 1; ante fallo, deriva a la COLA 2.
- **Consumidor:** [`GuiaConsumerListener`](src/main/java/com/duoc/gestionpedidos/listener/GuiaConsumerListener.java) — `@RabbitListener` sobre la COLA 1; si el mensaje es inválido o falla la persistencia, lo rechaza y RabbitMQ lo envía a la COLA 2.

Para **evidenciar la COLA 2** en la demo: `POST /api/guias?simularError=true`.

---

## 3. Oracle Cloud (Autonomous Database)

1. En OCI → *Autonomous Database* → **Create** (Transaction Processing, **Always Free**). Define la password del usuario `ADMIN`.
2. **Database connection → Download wallet** (Instance Wallet). Descomprime el wallet en `./wallet` (local) o móntalo en `/app/wallet` (Docker/EC2).
3. El alias TNS sale de `tnsnames.ora` dentro del wallet (ej. `oraclecloudfparradb_tp`).
4. Configura en `.env`: `ORACLE_TNS_ALIAS`, `ORACLE_USER=ADMIN`, `ORACLE_PASSWORD`.

Tablas: `GUIA_DESPACHO` (metadatos) y `GUIA_PROCESADA` (nueva, guías consumidas). Con `ddl-auto=update` Hibernate las crea solo; el script de respaldo está en [`src/main/resources/sql/create_tables_oracle.sql`](src/main/resources/sql/create_tables_oracle.sql).

> ⚠️ El **wallet y las contraseñas NUNCA se versionan** (ver `.gitignore`). En CI/CD el wallet viaja como secret base64.

---

## 4. Azure AD B2C (IDaaS)

Tenant, app registration, custom claim `rol` (viaja como `extension_rol`), User Flow `sign up and sign in` y 2 usuarios (`gestor` / `consulta`). Mapea en `.env`:

| Valor de Azure | Variable | Propiedad Spring |
|---|---|---|
| `issuer` | `AZURE_ISSUER_URI` | `...jwt.issuer-uri` |
| `jwks_uri` | `AZURE_JWKS_URI` | `...jwt.jwk-set-uri` |
| Application (client) ID | `AZURE_AUDIENCE` | `azure.b2c.audience` |

---

## 5. AWS S3 y API Gateway

- **S3:** bucket en `us-east-1`; credenciales de AWS Academy (`AWS_ACCESS_KEY_ID/SECRET/SESSION_TOKEN`) — se refrescan en cada *Start Lab*.
- **API Gateway:** HTTP API con las 7 rutas apuntando a `http://<IP_EC2>/api/...`, autorizador JWT (Issuer + Audience de B2C) en todas las rutas.

---

## 6. CI/CD (GitHub Actions) + análisis de seguridad Trivy

Pipeline en [.github/workflows/deploy.yml](.github/workflows/deploy.yml): **build & test → Security Scan (Trivy) → Docker build & push → deploy EC2** (RabbitMQ + backend + wallet). El escaneo Trivy (dependencias, config e imagen) aplica la recomendación del docente de la S6.

### Secrets / variables de GitHub

| Secret | Descripción | ¿Obligatorio? |
|---|---|---|
| `DOCKERHUB_USERNAME` / `DOCKERHUB_TOKEN` | Docker Hub (token Read & Write) | Sí (build & push) |
| `ORACLE_WALLET_B64` | Wallet .zip en base64 | Solo deploy EC2 |
| `ORACLE_TNS_ALIAS` / `ORACLE_USER` / `ORACLE_PASSWORD` | Conexión Oracle | Solo deploy EC2 |
| `AZURE_ISSUER_URI` / `AZURE_JWKS_URI` / `AZURE_AUDIENCE` | Azure AD B2C | Solo deploy EC2 |
| `AWS_S3_BUCKET` | Bucket S3 | Solo deploy EC2 |
| `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` / `AWS_SESSION_TOKEN` | AWS Academy (refrescar) | Solo deploy EC2 |
| `EC2_HOST` / `EC2_USER` / `EC2_SSH_KEY` | SSH al EC2 | Solo deploy EC2 |

| Variable (Actions → Variables) | Valor | Efecto |
|---|---|---|
| `DEPLOY_EC2` | `true` | Activa el job de despliegue en EC2 |

Para generar el secret del wallet:
```bash
base64 -w0 Wallet_OracleCloudFparraDB.zip > wallet_b64.txt   # pega el contenido en ORACLE_WALLET_B64
```

---

## 7. Pruebas (Postman)

Importa [postman/GestionPedidos.postman_collection.json](postman/GestionPedidos.postman_collection.json) y define `baseUrl`, `token` (GESTOR) y `tokenConsulta`. Incluye el flujo asíncrono (crear → verificar en RabbitMQ → listar procesadas) y la cola de errores (`simularError=true`).

## Estructura

```
src/main/java/com/duoc/gestionpedidos/
├── config/      SecurityConfig, RoleClaimConverter, AudienceValidator, AwsS3Config, OpenApiConfig, RabbitMQConfig
├── controller/  GuiaDespachoController (7 endpoints), GuiaProcesadaController, HealthController
├── dto/         GuiaRequestDTO, GuiaResponseDTO, GuiaMensaje
├── exception/   GlobalExceptionHandler, ResourceNotFoundException
├── listener/    GuiaConsumerListener (@RabbitListener COLA 1)
├── model/       GuiaDespacho, EstadoGuia, GuiaProcesada
├── repository/  GuiaDespachoRepository, GuiaProcesadaRepository
└── service/     GuiaDespachoService, PdfService, S3Service, ProductorGuiaService, GuiaProcesadaService
```
