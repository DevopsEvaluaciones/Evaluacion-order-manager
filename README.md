# Order Manager

API REST para administrar pedidos de un e-commerce. La aplicación está construida
con Spring Boot 3.3, Java 17 y H2 en memoria para el entorno local y de pruebas.

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

Construcción y ejecución local:

```bash
docker build -t order-manager:local .
docker run --rm --name order-manager -p 8080:8080 order-manager:local
docker ps
```

El contenedor debe aparecer como `healthy` después del período inicial del
healthcheck.

### GitHub Container Registry

El pipeline publica la imagen en GitHub Container Registry. Reemplace
`<usuario>/<repositorio>` por los valores del repositorio y `<sha>` por la
etiqueta del commit publicado:

```bash
docker pull ghcr.io/<usuario>/<repositorio>:<sha>
docker run --rm -p 8080:8080 ghcr.io/<usuario>/<repositorio>:<sha>
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
APP_PORT=8090 ORDER_MANAGER_IMAGE=ghcr.io/<usuario>/<repositorio>:<sha> docker compose up -d
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