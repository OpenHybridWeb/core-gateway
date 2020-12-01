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

The config file is expected to be mounted to `/deployments/config/gateway-config.yaml` and is defined as `- v ` parameter.
Don't forget to change to your local path.

### Minikube

```shell script
minikube start --insecure-registry "10.0.0.0/24"
```

#### Enable local registry
[Docs](https://minikube.sigs.k8s.io/docs/handbook/registry/#docker-on-macos)

For Mac:
```shell script
eval $(minikube -p minikube docker-env)
minikube addons enable registry
```
in a different window:
```shell script
docker run --rm -it --network=host alpine ash -c "apk add socat && socat TCP-LISTEN:5000,reuseaddr,fork TCP:$(minikube ip):5000"
```

#### Build

```shell script
mvn package
docker build -f src/main/docker/Dockerfile.jvm -t openhybridweb/core-gateway-jvm .
docker tag openhybridweb/core-gateway-jvm localhost:5000/core-gateway
docker push localhost:5000/core-gateway
```

#### Deploy hello world backends

```shell script
kubectl create deployment hello-app1 --image=gcr.io/google-samples/hello-app:1.0
kubectl expose deployment hello-app1 --type=NodePort --port=8080

kubectl create deployment hello-app2 --image=gcr.io/google-samples/hello-app:2.0
kubectl expose deployment hello-app2 --type=NodePort --port=8080
```

#### Deploy

```shell script
kubectl create secret generic core-gateway-config --from-file=src/main/k8s/core-gateway-config.yaml
kubectl apply -f src/main/k8s/core-gateway.yaml
```

Tunnel router:
```shell script
minikube service core-gateway
```

hit one of hello app urls e.g. `http://127.0.0.1:53735/hello-app1/`

