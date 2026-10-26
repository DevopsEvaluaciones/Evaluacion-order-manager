# Order Manager

API REST para administrar pedidos de un e-commerce. La aplicación está construida
con Spring Boot 4.0.9, Java 17 y H2 en memoria para el entorno local y de pruebas.

## Ejecución local

Requisitos:

- Java 17
- Maven 3.9 o superior

```bash
mvn spring-boot:run
```

La API queda disponible en `http://localhost:8098`. El endpoint de salud de
Actuator es `GET /actuator/health`.

## Contenedores

El [Dockerfile](./Dockerfile) usa dos etapas:

1. Maven compila el proyecto con Java 17.
2. Una imagen `eclipse-temurin:17-jre-alpine` ejecuta únicamente el JAR generado.

El `pom.xml` se copia antes de `src/` para reutilizar la caché de dependencias.
La imagen final ejecuta con el usuario sin privilegios `app`, expone el puerto
8080 y consulta `/actuator/health` mediante un `HEALTHCHECK`. El puerto se
configura con `SERVER_PORT`, sin fijarlo en el código de la aplicación.

La etapa final ejecuta `apk upgrade --no-cache` para incorporar las correcciones
de seguridad de los paquetes de Alpine. Esta línea corrigió la vulnerabilidad
`CVE-2026-111497` en `brotli-libs` (ver PR #31).

Construcción y ejecución local:

```bash
docker build -t order-manager:local .
docker run --rm --name order-manager -p 8080:8080 order-manager:local
docker ps
```

El contenedor debe aparecer como `healthy` después del período inicial del
healthcheck.

### GitHub Container Registry

El pipeline publica la imagen en GitHub Container Registry con estas etiquetas:

- `main`: la última imagen construida desde `main`.
- `sha-<siete caracteres>`: la imagen de cada commit.
- `v1.0.0` y etiquetas `v*`: las versiones publicadas con tag.

```bash
docker pull ghcr.io/devopsevaluaciones/evaluacion-order-manager:main
docker run --rm -p 8080:8080 ghcr.io/devopsevaluaciones/evaluacion-order-manager:main
```

## Orquestación con Docker Compose

`docker-compose.yml` construye el servicio localmente, lo conecta a una red
propia y configura un límite de 0.50 CPU y 512 MB de memoria. La aplicación
utiliza H2 en memoria, por lo que no requiere un servicio PostgreSQL externo.

```bash
docker compose up -d
docker compose ps
docker compose down
```

El puerto publicado puede cambiarse con `APP_PORT`, y la imagen puede
reemplazarse con `ORDER_MANAGER_IMAGE`:

```bash
APP_PORT=8090 ORDER_MANAGER_IMAGE=ghcr.io/devopsevaluaciones/evaluacion-order-manager:main docker compose up -d
```

## Orquestación con Kubernetes

La carpeta `k8s/` contiene:

- `deployment.yaml`: dos réplicas, recursos solicitados y limitados, y probes
  de liveness/readiness.
- `service.yaml`: servicio `ClusterIP` en el puerto 8080.
- `hpa.yaml`: autoescalado entre dos y cuatro réplicas cuando el uso promedio de
  CPU alcanza 70%.

Con un clúster local de kind:

```bash
docker build -t order-manager:local .
kind load docker-image order-manager:local --name evaluacion
kubectl apply -f k8s/
kubectl get pods
kubectl get svc
kubectl port-forward svc/order-manager 8080:8080
```

El HPA requiere que el clúster tenga disponible Metrics Server.

## Pipeline de integración y despliegue continuo

El workflow `.github/workflows/ci-cd.yml` se ejecuta en cada `push` a `main` y
en cada tag `v*`, y en cada `pull_request` hacia `main` o `dev`. Sus jobs se
ejecutan en este orden:

1. **Test and coverage:** ejecuta las pruebas y valida la cobertura con JaCoCo.
2. **Snyk security scan:** analiza las dependencias del `pom.xml` con
   `--severity-threshold=high`. Requiere el secret `SNYK_TOKEN`.
3. **Build and publish Docker image:** construye la imagen, la escanea con
   Trivy y solo la publica en GHCR si el escaneo pasa. Solo corre en `push`.
4. **Deploy with Docker Compose:** levanta la aplicación con la imagen publicada
   y verifica `/actuator/health`. Solo corre en `push`. Usa el Environment
   `staging` para `main` y `production` para tags `v*`.

Los jobs `build-image` y `deploy` aparecen como *skipped* en los PRs, porque
solo se ejecutan cuando el cambio llega a una rama o a un tag.

## Seguridad y control de cambios

### Análisis de dependencias con Snyk

El job `security` de `.github/workflows/ci-cd.yml` ejecuta `snyk test --file=pom.xml --severity-threshold=high` después del job `test` y antes de `deploy`. Usa el secret `SNYK_TOKEN`. El CLI se ejecuta directamente porque la acción Docker de Snyk no encontró `JAVA_HOME` en el runner.

Evolución de los hallazgos durante la migración:

| Versión del proyecto | Issues de Snyk |
| --- | --- |
| Spring Boot 3.3.4 (inicial) | 69 |
| Spring Boot 3.5.15, con versiones fijadas de Tomcat, Jackson y Spring Framework | 16 |
| Spring Boot 4.0.8 | 13 |
| Spring Boot 4.0.9 | 1 (sin parche disponible) |

### Excepción documentada

La vulnerabilidad XXE de `jaxb-core` (`SNYK-JAVA-ORGGLASSFISHJAXB-20883142`) no tiene parche disponible. El proyecto no procesa XML de entrada: una búsqueda en `src/` no encontró `javax.xml`, `jakarta.xml` ni `DocumentBuilder`. La excepción está en el archivo `.snyk`, con motivo y fecha de expiración (2027-01-31). Con ella, el resultado final no tiene vulnerabilidades altas ni críticas.

### Escaneo de la imagen con Trivy

El job `build-image` construye la imagen localmente (`push: false`, `load: true`), la escanea con `aquasecurity/trivy-action@v0.36.0` y solo la publica en `ghcr.io` si el escaneo pasa. Trivy falla con severidad `CRITICAL` o `HIGH`, y `ignore-unfixed: true` evita bloquear por vulnerabilidades que aún no tienen parche.

La primera ejecución en `main` falló por la vulnerabilidad `CVE-2026-111497` (HIGH) en `brotli-libs` 1.2.0-r1, que tiene parche en 1.2.1-r0. El paso bloqueó la publicación, y eso fue la prueba del control. La corrección, en el PR #31, agregó `RUN apk upgrade --no-cache` en la etapa final del `Dockerfile`. La ejecución posterior en `main` pasó Trivy y publicó la imagen.

### Protección de ramas

`main` y `dev` tienen una regla de protección que exige:

- Pull Request antes de fusionar.
- Una aprobación de un revisor con permisos de escritura.
- Los checks obligatorios `Test and coverage` y `Snyk security scan`.

Con esta regla, la prueba de bloqueo con una dependencia vulnerable (`log4j-core` 2.14.1, en una rama de prueba) dejó el check de Snyk en rojo y el merge bloqueado. Esa rama se cerró sin fusionar.

### Dependabot

`.github/dependabot.yml` vigila dependencias de Maven, imágenes Docker y acciones de GitHub. Las tres entradas usan `target-branch: "dev"`, para que las actualizaciones lleguen a `dev` y no directamente a `main`. Las alertas de vulnerabilidad están activadas.

Durante el proceso, GitHub reportó una alerta crítica en la rama principal, que hoy aparece como cerrada en **Security → Dependabot**.

### Flujo de cambios y trazabilidad

Todos los cambios entraron por Pull Request. Los PRs más relevantes de mi parte son:

| PR | Contenido | Estado |
| --- | --- | --- |
| #9 | `dependabot.yml` con `target-branch: "dev"` | Fusionado en `dev` sin aprobación previa de otro integrante. |
| #13 | Migración a Spring Boot 4.0.9 y excepción XXE | Aprobado por Maxi y fusionado en `dev` |
| #23 | Job `security` con Snyk | Fusionado en `dev` |
| #25 | Publicación de `dev` en `main` | Fusionado en `main` |
| #28 | Publicación de Trivy en `main` | Fusionado en `main`; la primera ejecución falló por la vulnerabilidad en `brotli-libs` |
| #29 | Corrección de la versión de `trivy-action` | Fusionado en `main` |
| #31 | Actualización de paquetes de Alpine para corregir `brotli-libs` | Fusionado en `main`; la ejecución posterior pasó |

El tag `v1.0.0` corresponde a la publicación inicial de `main`. Los Environments
`staging` y `production` se usan en el job de despliegue.

## Política de calidad

- **Cobertura mínima:** 80% de instrucciones y 70% de ramas, verificadas con JaCoCo (`jacoco:check` en `pom.xml`). Si no se cumplen, el pipeline falla.
- **Pruebas:** si una prueba falla, el job `test` falla y el pipeline se detiene antes de `security`, `build-image` y `deploy`.
- **Merges:** ningún cambio entra a `main` sin una aprobación de otro integrante y con los checks `Test and coverage` y `Snyk security scan` en verde.