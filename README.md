# core-gateway
Core component for routing and security

## Built in API

1. `/_api/health/info` - basic information about router
2. `/_api/health/live` - liveness health check
3. `/_api/health/ready` - readiness health check

## Configuration

The component expects yaml configuration defining routes.
Path to config is defined in `APP_GATEWAY_ROUTER_CONFIGPATH` env. variable

```yaml
routes:
# static content container
  - context: /theme/*
    url: http://localhost:8760
  - context: /test-spa1/*
    url: http://localhost:8760

# 3party services
  - context: /api/*
    url: http://localhost:8770
```

## How to run

It's expected to have installed [Java 11](https://adoptopenjdk.net/installation.html).

Once the app is running you can hit health check info

### Local Java

Build the package

```shell script
./mvnw package
```

Run
```shell script
export APP_GATEWAY_ROUTER_CONFIGPATH=src/test/resources/gateway-config-test.yaml
java -jar target/gateway-1.0.0-SNAPSHOT-runner.jar
```

#### Native executable

You can create a native executable using: 
```shell script
./mvnw package -Pnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using: 
```shell script
./mvnw package -Pnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/gateway-1.0.0-SNAPSHOT-runner`


### Local Docker

Choose the preferred docker image style and follow instructions in [src/main/docker](src/main/docker) directory.

1. [Docker JVM](src/main/docker/Dockerfile.jvm)
2. [Docker Fast Jar JVM](src/main/docker/Dockerfile.fast-jar)
3. [Docker Native](src/main/docker/Dockerfile.native)

The config file path needs to be defined via `APP_GATEWAY_ROUTER_CONFIGPATH` variable and then 
the config file mounted.

### Minikube

```shell script
minikube start
```
#### Deploy hello world backends

```shell script
kubectl create deployment hello-app1 --image=gcr.io/google-samples/hello-app:1.0
kubectl expose deployment hello-app1 --type=NodePort --port=8080

kubectl create deployment hello-app2 --image=gcr.io/google-samples/hello-app:2.0
kubectl expose deployment hello-app2 --type=NodePort --port=8080
```

#### Deploy gateway

```shell script
kubectl create secret generic core-gateway-config --from-file=src/main/k8s/core-gateway-config.yaml
kubectl apply -f src/main/k8s/core-gateway.yaml
```

Tunnel router:
```shell script
minikube service core-gateway
```

hit one of hello app urls e.g. `http://127.0.0.1:53735/hello-app1/`

