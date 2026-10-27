# Order Manager 🎓

API REST en **Java 17 + Spring Boot 4.0.9** para la gestión de pedidos de un
comercio electrónico. Permite crear órdenes, agregar productos, consultar por
ID o estado, controlar sus transiciones, cancelarlas y eliminarlas.

El proyecto incluye validaciones, manejo centralizado de excepciones,
persistencia con Spring Data JPA, pruebas automatizadas, cobertura con JaCoCo,
contenedores Docker, orquestación con Docker Compose y Kubernetes, además de
un pipeline de integración y despliegue continuo con GitHub Actions.

Este repositorio es la base de trabajo para la Evaluación Parcial N°2 de
Ingeniería DevOps (DOY0101). El objetivo es aplicar buenas prácticas de
desarrollo colaborativo, control de versiones, automatización, seguridad y
despliegue.

---

## 🚀 Cómo levantar el proyecto localmente

### Requisitos

Se necesita:

- JDK 17.
- Maven 3.9 o superior.
- Docker y Docker Compose, si se desea ejecutar el proyecto en contenedores.

El repositorio no incluye `mvnw`, por lo que los comandos Maven requieren una
instalación local de Maven.

### Ejecutar la aplicación

Desde la raíz del proyecto:

```bash
mvn spring-boot:run
```

La aplicación queda disponible en:

- API: `http://localhost:8098`
- Healthcheck: `http://localhost:8098/actuator/health`
- Consola H2: `http://localhost:8098/h2-console`

La configuración local se encuentra en
[`application.properties`](./src/main/resources/application.properties).

La base de datos utilizada localmente es H2 en memoria:

```text
JDBC URL: jdbc:h2:mem:orderdb
Usuario: sa
Contraseña: vacía
```

El esquema se crea y elimina automáticamente con `spring.jpa.hibernate.ddl-auto=create-drop`.
Por este motivo, los datos se reinician cada vez que se detiene la aplicación.

### Ejecutar las pruebas

Para ejecutar las pruebas unitarias:

```bash
mvn test
```

Para ejecutar las pruebas, generar el informe de cobertura y validar los
umbrales definidos en JaCoCo:

```bash
mvn clean verify
```

Los reportes se generan en:

```text
target/surefire-reports/
target/site/jacoco/
```

---

## 📁 Estructura del proyecto

```text
order-manager/
├── .github/
│   ├── dependabot.yml             → configuración de actualizaciones
│   └── workflows/
│       └── ci-cd.yml              → pipeline de pruebas, seguridad y despliegue
├── docs/
│   └── fases/                     → documentación de las fases de la evaluación
├── k8s/
│   ├── deployment.yaml            → Deployment de Kubernetes
│   ├── hpa.yaml                   → autoescalado horizontal
│   └── service.yaml               → Service interno del clúster
├── src/
│   ├── main/
│   │   ├── java/com/example/ordermanager/
│   │   │   ├── controller/       → endpoints REST
│   │   │   ├── dto/              → objetos de entrada y validaciones
│   │   │   ├── exception/        → excepciones y respuestas de error
│   │   │   ├── model/            → entidades y estados del dominio
│   │   │   ├── repository/       → repositorios Spring Data JPA
│   │   │   └── service/           → reglas de negocio
│   │   └── resources/
│   │       ├── application.properties
│   │       └── application-docker.properties
│   └── test/
│       └── java/com/example/ordermanager/
│           ├── controller/       → pruebas de los endpoints
│           ├── model/            → pruebas de las entidades
│           └── service/           → pruebas de la lógica de negocio
├── .dockerignore                 → archivos excluidos de la imagen
├── .gitignore                    → archivos excluidos de Git
├── Dockerfile                    → imagen Docker multi-stage
├── docker-compose.yml            → ejecución local en contenedores
├── pom.xml                       → dependencias y configuración Maven
├── sonar-project.properties      → configuración de análisis Sonar
└── README.md                     → documentación principal
```

El flujo interno de una petición es:

```text
Cliente HTTP
    ↓
OrderController
    ↓
DTO + Bean Validation
    ↓
OrderService
    ↓
OrderRepository
    ↓
H2 o PostgreSQL
```

---

## 🧾 Funcionalidad principal de la API

La ruta base de la API es `/api/orders`.

| Método | Endpoint | Descripción |
| --- | --- | --- |
| `GET` | `/api/orders` | Lista todas las órdenes |
| `GET` | `/api/orders/{id}` | Busca una orden por ID |
| `GET` | `/api/orders/status/{status}` | Filtra órdenes por estado |
| `POST` | `/api/orders` | Crea una nueva orden |
| `POST` | `/api/orders/{id}/items` | Agrega un producto a una orden pendiente |
| `PUT` | `/api/orders/{id}/confirm` | Confirma una orden |
| `PUT` | `/api/orders/{id}/ship` | Marca una orden como enviada |
| `PUT` | `/api/orders/{id}/deliver` | Marca una orden como entregada |
| `PUT` | `/api/orders/{id}/cancel` | Cancela una orden no entregada |
| `DELETE` | `/api/orders/{id}` | Elimina una orden |

Los estados disponibles son:

```text
PENDING
CONFIRMED
SHIPPED
DELIVERED
CANCELLED
```

El flujo válido de una orden es:

```text
PENDING → CONFIRMED → SHIPPED → DELIVERED
    ↓          ↓          ↓
 CANCELLED  CANCELLED  CANCELLED
```

Una orden `DELIVERED` es terminal y no puede cancelarse. Solo las órdenes
`PENDING` pueden modificarse para agregar nuevos productos.

### Ejemplo de creación de una orden

```bash
curl -X POST http://localhost:8098/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerName": "Ana Pérez",
    "items": [
      {
        "productName": "Teclado mecánico",
        "quantity": 2,
        "unitPrice": 45000.00
      }
    ]
  }'
```

El total se calcula automáticamente sumando `quantity * unitPrice` de todos
los productos de la orden.

### Validaciones y manejo de errores

- El nombre del cliente es obligatorio y admite hasta 100 caracteres.
- El nombre del producto es obligatorio y admite hasta 150 caracteres.
- La cantidad debe estar entre 1 y 1000.
- El precio unitario debe ser mayor que cero.
- No se permiten transiciones de estado inválidas.
- No se puede cancelar una orden entregada o ya cancelada.

El `GlobalExceptionHandler` genera respuestas JSON con `timestamp`, `status`,
`error` y `message`. Los errores de validación incluyen también el detalle de
cada campo en `fields`.

| Situación | Código HTTP |
| --- | --- |
| Orden no encontrada | `404 Not Found` |
| Transición de estado inválida | `409 Conflict` |
| Datos inválidos o JSON incorrecto | `400 Bad Request` |
| Error interno no controlado | `500 Internal Server Error` |

---

## 🌳 Estrategia de ramificación

Como equipo utilizamos una estrategia basada en GitFlow, adaptada al tamaño
del proyecto. Se mantienen `main` como rama estable y `dev` como rama de
integración. Las funcionalidades y correcciones se desarrollan en ramas
temporales y llegan a las ramas principales mediante Pull Requests.

### Ramas principales

- **`main`**: contiene el código estable y listo para evaluación o publicación.
- **`dev`**: rama de integración donde se reúnen los cambios antes de llegar a
  `main`.
- **`feature/*`**: nueva funcionalidad desarrollada a partir de `dev`.
- **`fix/*`**: corrección de un error existente.
- **`docs/*`**: cambios exclusivamente relacionados con documentación.
- **`chore/*`**: mantenimiento, configuración o tareas técnicas.

El flujo de integración es:

```text
feature/* / fix/* / docs/* / chore/*
                    ↓
                   dev
                    ↓
                  main
```

En el repositorio remoto se encuentran ramas de trabajo como:

```text
BXQ23-patch-1
feature/analisis
feature/benja-cobertura
feature/benja-tests-error
feature/ci-cd
feature/dockerfile
feature/seguridad
```

La estrategia permite trabajar en paralelo sin modificar directamente el
código estable y facilita revisar cada cambio antes de integrarlo.

---

## 📝 Convenciones de commits

Utilizamos un formato inspirado en Conventional Commits, con prefijos que
indican el tipo de cambio:

- **`feat:`** — nueva funcionalidad.
  Ejemplo: `feat: agrega filtro de órdenes por estado`
- **`fix:`** — corrección de errores.
  Ejemplo: `fix: corrige transición inválida de una orden`
- **`test:`** — creación o modificación de pruebas.
  Ejemplo: `test: agrega pruebas para cancelar órdenes`
- **`docs:`** — documentación.
  Ejemplo: `docs: actualiza instrucciones de Docker`
- **`ci:`** — configuración del pipeline.
  Ejemplo: `ci: agrega escaneo de la imagen con Trivy`
- **`chore:`** — mantenimiento que no modifica la lógica de negocio.
  Ejemplo: `chore: actualiza configuración de Maven`

Elegimos este formato porque hace que el historial sea fácil de leer y
permite identificar rápidamente la finalidad de cada commit sin abrir el
diff completo.

---

## 🔀 Convenciones de naming de ramas

Usamos el formato:

```text
tipo/nombre-descriptivo-en-minusculas
```

Ejemplos:

```text
feature/filtro-por-estado
feature/calculo-total-orden
fix/error-en-cancelacion
test/cobertura-controller
docs/actualiza-readme
chore/configuracion-maven
```

El nombre debe describir brevemente el cambio para que cualquier integrante
pueda entender el objetivo de la rama desde el listado de GitHub.

---

## 🔍 Estrategia de revisión (Pull Requests)

Los cambios deben integrarse mediante Pull Request y no mediante push directo
a las ramas principales.

Antes de aprobar un Pull Request revisamos:

1. Que el proyecto compile correctamente.
2. Que las pruebas unitarias pasen.
3. Que se cumplan los mínimos de cobertura de JaCoCo.
4. Que el análisis de seguridad de Snyk no detecte vulnerabilidades bloqueantes.
5. Que la imagen Docker pase el escaneo de Trivy, cuando corresponda.
6. Que exista revisión cruzada de otro integrante.
7. Que el cambio no rompa funcionalidades existentes.
8. Que la descripción explique el problema, la solución y las pruebas
   realizadas.

La protección de ramas exige Pull Request, aprobación de un revisor y checks
obligatorios en verde antes de fusionar.

---

## ⚙️ Automatización (CI/CD)

El workflow [`ci-cd.yml`](./.github/workflows/ci-cd.yml) se ejecuta:

- En cada `push` a `main`.
- En tags cuyo nombre comienza con `v`.
- En Pull Requests hacia `main` o `dev`.

El pipeline está compuesto por los siguientes jobs:

### 1. Test and coverage

- Configura Java 17 Temurin.
- Ejecuta `mvn -B clean verify`.
- Ejecuta las pruebas y JaCoCo.
- Publica los reportes de pruebas y cobertura como artefactos.

El `pom.xml` exige:

- 80% mínimo de cobertura de instrucciones.
- 70% mínimo de cobertura de ramas.

### 2. Snyk security scan

- Depende de que el job de pruebas termine correctamente.
- Configura Node.js 20 e instala Snyk CLI.
- Ejecuta `snyk test --file=pom.xml --severity-threshold=high`.
- Utiliza el secreto `SNYK_TOKEN`.

### 3. Build and publish Docker image

- Se ejecuta en eventos `push`.
- Construye la imagen mediante Docker Buildx.
- La analiza con Trivy.
- Rechaza vulnerabilidades `CRITICAL` o `HIGH` detectadas.
- Publica la imagen en GitHub Container Registry (GHCR) si el escaneo pasa.

Las imágenes se publican con etiquetas asociadas a la rama, al commit y a los
tags semánticos.

### 4. Deploy with Docker Compose

- Se ejecuta después de las pruebas, Snyk y la construcción de la imagen.
- Descarga la imagen desde GHCR.
- Levanta la aplicación con Docker Compose.
- Ejecuta un smoke test contra `/actuator/health`.
- Usa el environment `staging` para `main`.
- Usa el environment `production` para tags `v*`.

La imagen publicada puede ejecutarse con:

```bash
docker pull ghcr.io/devopsevaluaciones/evaluacion-order-manager:main
docker run --rm -p 8080:8080 \
  ghcr.io/devopsevaluaciones/evaluacion-order-manager:main
```

---

## 🐳 Docker

El proyecto incluye un [`Dockerfile`](./Dockerfile) multi-stage:

1. La primera etapa utiliza `maven:3.9-eclipse-temurin-17` para descargar
   dependencias y compilar el proyecto.
2. La segunda etapa utiliza `eclipse-temurin:17-jre-alpine` para ejecutar solo
   el JAR generado.

La imagen final:

- Expone el puerto `8080`.
- Ejecuta con el usuario no privilegiado `app`.
- Actualiza los paquetes Alpine con `apk upgrade --no-cache`.
- Incluye un `HEALTHCHECK` sobre `/actuator/health`.
- Permite configurar el puerto mediante `SERVER_PORT`.

Para construir y ejecutar:

```bash
docker build -t order-manager:local .
docker run --rm --name order-manager -p 8080:8080 order-manager:local
```

El contenedor debe aparecer como `healthy` luego del período inicial del
healthcheck.

### Docker Compose

El archivo [`docker-compose.yml`](./docker-compose.yml):

- Construye o utiliza la imagen de la aplicación.
- Publica el puerto interno `8080`.
- Activa el perfil `docker`.
- Conecta el servicio a la red `backend`.
- Limita el uso a `0.50` CPU y `512M` de memoria.
- Reinicia el servicio salvo que se detenga manualmente.
- Comprueba la salud mediante Actuator.

Comandos:

```bash
docker compose up -d
docker compose ps
docker compose logs -f app
docker compose down
```

Variables disponibles:

| Variable | Valor predeterminado | Uso |
| --- | --- | --- |
| `APP_PORT` | `8080` | Puerto publicado en el host |
| `ORDER_MANAGER_IMAGE` | `order-manager:local` | Imagen que se ejecutará |

En PowerShell:

```powershell
$env:APP_PORT="8090"
$env:ORDER_MANAGER_IMAGE="order-manager:local"
docker compose up -d
```

El despliegue definido utiliza H2 en memoria. No se levanta automáticamente
un contenedor PostgreSQL, aunque el proyecto incluye el driver de PostgreSQL
para permitir una configuración externa.

---

## ☸️ Kubernetes

La carpeta [`k8s/`](./k8s/) contiene:

- `deployment.yaml`: dos réplicas, variables de entorno, recursos y probes de
  liveness/readiness.
- `service.yaml`: servicio `ClusterIP` en el puerto `8080`.
- `hpa.yaml`: autoescalado entre dos y cuatro réplicas cuando el uso promedio
  de CPU alcanza el 70%.

Los manifiestos esperan una imagen local llamada `order-manager:local` y usan
`imagePullPolicy: Never`.

Ejemplo utilizando kind:

```bash
docker build -t order-manager:local .
kind load docker-image order-manager:local --name evaluacion
kubectl apply -f k8s/
kubectl get deployments
kubectl get pods
kubectl get svc
kubectl port-forward svc/order-manager 8080:8080
```

El HPA requiere Metrics Server:

```bash
kubectl get hpa
kubectl describe hpa order-manager
```

---

## 📊 Calidad y seguridad

- **JaCoCo:** exige 80% de instrucciones y 70% de ramas.
- **Pruebas automatizadas:** se ejecutan localmente y en GitHub Actions.
- **Snyk:** analiza las dependencias definidas en `pom.xml`.
- **Trivy:** analiza la imagen Docker antes de publicarla.
- **Dependabot:** revisa dependencias Maven, imágenes Docker y acciones de
  GitHub.
- **Contenedor seguro:** la aplicación se ejecuta con el usuario no
  privilegiado `app`.
- **Secretos:** `SNYK_TOKEN` y `GITHUB_TOKEN` se administran mediante GitHub
  Actions y no deben guardarse en el repositorio.

---

## 💭 Reflexiones individuales

### Maximiliano

Como equipo utilizamos una estrategia basada en GitFlow, con `dev` como rama
de integración y `main` como rama estable. Al principio me pareció que usar
varias ramas podía hacer el trabajo más lento, pero después entendí que nos
ayudaba a separar las funcionalidades y a evitar que un cambio incompleto
afectara la versión principal. Las ramas `feature/*`, `fix/*` y `docs/*`
también permitieron identificar mejor el objetivo de cada modificación.


### Benjamin

En lo personal, este trabajo me resultó más desafiante que la primera evaluación, porque tuve que prestar más atención a la parte técnica y a la coordinación con mis compañeros. Una de las cosas que más aprendí fue sobre las pruebas de errores 400, 404 y 409. Por ejemplo, al revisar el caso del JSON inválido, entendí que devolver un error 500 cuando corresponde un 400 puede confundir, ya que el problema está en los datos enviados y no en el servidor.

También me pareció interesante trabajar con la cobertura de las pruebas y el despliegue automático. Comprendí que la cobertura mínima ayuda a evitar que se incorporen cambios sin suficientes pruebas, mientras que identificar las imágenes con el SHA del commit permite saber qué versión del código se está desplegando.

Durante el trabajo cometí algunos errores, como trabajar en una carpeta equivocada del repositorio, pero aprendí a revisar mejor dónde estoy trabajando antes de ejecutar cambios. Además, los PR fueron un desafío, porque dependíamos de las aprobaciones de nuestros compañeros y a veces teníamos que esperar para continuar. Esto me hizo darme cuenta de lo importante que es mantener una buena comunicación y organización dentro del equipo. En general, fue una buena experiencia y pude aprender tanto de los aspectos técnicos como de mis propios errores.

### Bastian



---

## 👥 Autores

- Maximiliano Romero
- Benjamin Perez
- Bastian Bravo
