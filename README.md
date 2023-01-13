## Containerizing Java EE 8 microservices

### Step 1: Building and running containerized Java EE 8 microservices locally

Create a new file called `Dockerfile` and add the following content:
```
FROM payara/micro:latest
COPY target/javaee8-service.war /opt/payara/deployments/
```

Then issue the following commands to build and run the image.
```
docker build -t javaee8-service:1.0 .
docker run -it -p 8080:8080 javaee8-service:1.0
```

### Step 2: Using multi-stage Docker builds for Java EE 8 microservices

You can use Docker to build your service when building the images. This maybe useful in containerized CI environments.
Create a new file called `Builderfile` and add the following content:
```
FROM mcr.microsoft.com/java/maven:11-zulu-debian10 as builder

RUN mkdir /codebase
COPY . /codebase/

WORKDIR /codebase
RUN mvn compile

FROM payara/micro:latest

COPY --from=builder /codebase/target/javaee8-service.war /opt/payara/deployments/
```

Then issue the following command to build and run the image.
```
docker build -t javaee8-service:1.1 -f Builderfile .
docker run -it -p 8080:8080 javaee8-service:1.1
```

### Step 3: Tuning the JVM to run in a containerized environment 

Careful when putting the JVM into a container. You may have to tune it for for JVMs prior to JDK10.
The following `ENTRYPOINT` shows some of the parameters.
``` 
ENTRYPOINT ["java", "-server", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseCGroupMemoryLimitForHeap", "-XX:MaxRAMFraction=3", "-XX:ThreadStackSize=256", "-XX:MaxMetaspaceSize=128m", "-XX:+UseG1GC", "-XX:ParallelGCThreads=2", "-XX:CICompilerCount=2", "-XX:+UseStringDeduplication", "-jar", "/opt/payara/payara-micro.jar"]
CMD ["--deploymentDir", "/opt/payara/deployments"]
```


## Infrastructure Composition

### Step 1: Writing a `docker-compose.yml` file for Java EE 8 microservice

Create a new file called `docker-compose.yml` and add the following content:
```yaml
version: "3"

services:
  javaee8-service:
    build:
      context: .
    image: javaee8-service:1.0
    ports:
    - "8080:8080"
    networks:
    - jee8net
    
networks:
  jee8net:
    driver: bridge
```

### Step 2: Building and running with Docker Compose locally

You can use Docker Compose during the local development, using the following commands:
```
docker-compose build
docker-compose up --build

docker-compose up -d --build
docker ps
docker stats
docker-compose logs -f
```

### Additional infrastructure composition

Add the following YAML to your `docker-compose.yml` to add a message queue and a database:
```yaml
  message-queue:
    image: ibmcom/activemq-ppc64le:latest
    environment:
    - ENABLE_JMX_EXPORTER=true
    expose:
    - "61616"       # the JMS port
    - "1883"        # the MQTT port
    - "5672"        # the AMQP port
    ports:
    - "8161:8161"   # the admin web UI
    networks:
    - jee8net
    
  postgres-db:
    image: "postgres:9.6.3"
    environment:
    - POSTGRES_USER=javaee8
    - POSTGRES_PASSWORD=1234
    ports:
    - "5432:5432"
    networks:
    - jee8net
```


## Deploying and Running Java EE on Kubernetes

### Step 1: Use Docker and Docker Compose to deploy to local Kubernetes

Add the following `deploy` section to each service in your `docker-compose.yml`:
```
deploy:
  replicas: 1
  resources:
    limits:
      memory: 640M
    reservations:
      memory: 640M
```

Then enter the following commands in your console to deploy and run everything:
```
docker stack deploy --compose-file docker-compose.yml javaee8

kubectl get deployments
kubectl get pods
kubectl get services

docker stack rm javaee8
```

### Step 2: Go from Docker Compose to Kubernetes with http://kompose.io

Download the latest release of Kompose from Github and put the binary on your `PATH`.
Then issue the following command to convert the `docker-compose.yml` into Kubernetes YAMLs.
```
kompose convert -f docker-compose.yml -o src/main/kubernetes/
```

### Step 3: Deploy and Run everything on Kubernetes

Use the generated YAML files to deploy and run everything on Kubernetes.
```
kubectl apply -f src/main/kubernetes/

kubectl get deployments
kubectl get pods
kubectl get services

kubectl rm -f src/main/kubernetes/
```