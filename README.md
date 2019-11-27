# Vert.x C10K

This example project contains a very basic http server which demonstrates the basics that are need to build a Vert.x server which can handle 10k concurrent connections.

## Test Server

The example program starts Vert.x and deploys multiple http verticles.


Noteworthy aspects:

* The `netty-transport-native-epoll` library adds [epoll support](https://netty.io/wiki/native-transports.html).

* `HttpServer` options improve connection handling

```java
options
    .setPort(SERVER_PORT)
    .setHost(SERVER_HOST)
    .setCompressionSupported(true)
    .setHandle100ContinueAutomatically(true)
    .setTcpFastOpen(true)
    .setTcpNoDelay(true)
    .setTcpQuickAck(true);
```

* `VertxOptions` to prefer native transports

```java
VertxOptions vertxOptions = new VertxOptions();
vertxOptions.setPreferNativeTransport(true);
```

* Deploy multiple verticles

```java
DeploymentOptions options = new DeploymentOptions();
int nVerticles = Runtime.getRuntime().availableProcessors();
options.setInstances(nVerticles);
```

## Build & Run

```bash
mvn clean package
java -jar target/vertx-c10k-example-0.0.1-SNAPSHOT.jar
```

### Endpoints

* http://localhost:8080/0k - Returns empty response
* http://localhost:8080/4k - Returns 4k response
* http://localhost:8080/8k - Returns 8k response
* http://localhost:8080/64k - Return 64k response
* http://localhost:8080/sendFile/test - Returns response which uses NIO [sendfile()](http://man7.org/linux/man-pages/man2/sendfile.2.html) call.

## Kernel Options

```bash
# Increase amount of open files. This will allow the creation of more network sockets.
sysctl -w fs.file-max=110000
sysctl -w fs.nr_open=110000
ulimit -n 110000

# Increase tcp buffers
sysctl -w net.ipv4.tcp_mem="100000000 100000000 100000000"

# Increase socket connection limit
sysctl -w net.core.somaxconn=10000

# Increase backlog size for tcp connections in SYN state
sysctl -w net.ipv4.tcp_max_syn_backlog=10000
```

## Tests

via [wrk](https://github.com/wg/wrk)

```
./wrk -c10000 -d32s -t8 http://localhost:8080/4
```

via [apache benchmark](https://httpd.apache.org/docs/2.4/programs/ab.html)

```
ab -n 200000 -c 10000 http://localhost:8080/4k
```