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

### API Endpoints

* GET http://localhost:8080/0k - Returns empty response

* GET http://localhost:8080/static/4k - Returns fixed 4k response
* GET http://localhost:8080/static/8k - Returns fixed 8k response
* GET http://localhost:8080/static/64k - Return fixed 64k response

* GET http://localhost:8080/cached/4k - Returns 4k response from caffeine cache
* GET http://localhost:8080/cached/8k - Returns 8k response from caffeine cache
* GET http://localhost:8080/cached/64k - Return 64k response from caffeine cache

* GET http://localhost:8080/sendFile/test - Returns response which uses NIO [sendfile()](http://man7.org/linux/man-pages/man2/sendfile.2.html) call.
* POST http://localhost:8080/upload - Upload a file, empty response will be returned

### Metrics

* GET http://localhost:8081/metrics - Return micrometer metrics for prometheus

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
./wrk -c10000 -d32s -t8 http://localhost:8080/4k
```

via [apache benchmark](https://httpd.apache.org/docs/2.4/programs/ab.html)

```
ab -n 200000 -c 10000 http://localhost:8080/4k
```

## Example Results

* On Intel i7 7770k @ Linux 4.19.0-5-amd64

```
 ./wrk -c10000 -d32s -t16 http://localhost:8080/static/4k
Running 32s test @ http://localhost:8080/static/4k
  16 threads and 10000 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency    85.68ms   29.71ms   1.03s    75.52%
    Req/Sec     7.04k     1.27k   19.43k    85.71%
  3588359 requests in 32.10s, 13.83GB read
Requests/sec: 111789.39
Transfer/sec:    441.05MB
```

## Disclaimer

Please note that the results should not be used as a performance reference.
The server response is static. The tests are run locally.
There are many other factors which will affect real world performance.

## Resources

* http://www.kegel.com/c10k.html
* [Real world HTTP performance benchmarking, lessons learned by Julien Viet](https://www.youtube.com/watch?v=2lzvsyoooTk&list=PLRsbF2sD7JVqPgMvdC-bARnJ9bALLIM3Q)
