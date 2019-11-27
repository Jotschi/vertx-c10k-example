package de.jotschi.vertx.c10k;

import java.util.Random;
import java.util.function.Function;

import org.apache.commons.jcs.JCS;
import org.apache.commons.jcs.access.CacheAccess;
import org.apache.commons.lang3.RandomStringUtils;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

public class ServerVerticle extends AbstractVerticle {

	public static final int SERVER_PORT = 8080;
	public static final String SERVER_HOST = "localhost";

	public static final int n4K = 4 * 1024;
	public static final int n8K = 8 * 1024;
	public static final int n64K = 64 * 1024;

	public static final int MAX_RANGE = 5000;

	public HttpServer server;

	public CacheAccess<Integer, String> jcsCache4k = JCS.getInstance("default");

	public static Cache<Integer, Buffer> cache4k = Caffeine.newBuilder()
		.maximumSize(10_000).build();
	public static Cache<Integer, Buffer> cache8k = Caffeine.newBuilder()
		.maximumSize(10_000).build();
	public static Cache<Integer, Buffer> cache64k = Caffeine.newBuilder()
		.maximumSize(10_000).build();

	@Override
	public void start(Promise<Void> startPromise) throws Exception {
		HttpServerOptions options = new HttpServerOptions();
		options
			.setPort(SERVER_PORT)
			.setHost(SERVER_HOST)
			.setCompressionSupported(true)
			.setHandle100ContinueAutomatically(true)
			.setTcpFastOpen(true)
			.setTcpNoDelay(true)
			.setTcpQuickAck(true);

		server = vertx.createHttpServer(options);
		Router router = createRouter();

		server.requestHandler(router::handle);
		server.listen(lh -> {
			if (lh.failed()) {
				startPromise.fail(lh.cause());
			} else {
				System.out.println("Server started..");
				startPromise.complete();
			}
		});
	}

	@Override
	public void stop(Promise<Void> stopPromise) throws Exception {
		if (server != null) {
			server.close(ch -> {
				if (ch.failed()) {
					stopPromise.fail(ch.cause());
				} else {
					stopPromise.complete();
				}
			});
		}
	}

	private Router createRouter() {
		Buffer buffer4k = createBuffer(n4K);
		Buffer buffer8k = createBuffer(n8K);
		Buffer buffer64k = createBuffer(n64K);

		Router router = Router.router(vertx);
		router.route().failureHandler(fh -> {
			if (fh.failed()) {
				fh.failure().printStackTrace();
				fh.response().setStatusCode(500).end();
			}
		});

		// Static responses
		router.route("/static/4k").handler(rh -> {
			rh.response().end(buffer4k);
		});
		router.route("/static/8k").handler(rh -> {
			rh.response().end(buffer8k);
		});
		router.route("/static/64k").handler(rh -> {
			rh.response().end(buffer64k);
		});

		// Cached responses (Caffeine)
		router.route("/cached/4k").handler(rh -> {
			rh.response().end(fromCaffeineCache(n4K));
		});
		router.route("/cached/8k").handler(rh -> {
			rh.response().end(fromCaffeineCache(n8K));
		});
		router.route("/cached/64k").handler(rh -> {
			rh.response().end(fromCaffeineCache(n64K));
		});

		// Disk cached responses (JCS)
		router.route("/jcs/4k").handler(fromJCSCache());

		// Empty response
		router.route("/0k").handler(rh -> {
			rh.response().end();
		});

		// sendfile()
		router.route("/sendFile/test").handler(rh -> {
			rh.response().sendFile("test");
		});

		// Upload test
		router.route("/upload").method(HttpMethod.POST).handler(BodyHandler.create());
		router.route("/upload").method(HttpMethod.POST).handler(rh -> {
			if (rh.fileUploads().size() == 0) {
				rh.fail(new RuntimeException("No upload found.."));
			} else {
				rh.response().end();
			}
		});

		return router;
	}

	private Handler<RoutingContext> fromJCSCache() {
		int random = new Random().nextInt(MAX_RANGE);
		int len = n4K;
		return rc -> {
			vertx.executeBlocking((Promise<String> bc) -> {
				String entry = jcsCache4k.get(random);
				if (entry != null) {
					bc.complete(entry);
				} else {
					System.out.println("Creating jcs cache entry...");
					String str = RandomStringUtils.randomAlphanumeric(len);
					jcsCache4k.put(random, str);
					bc.complete(entry);
				}
			}, false, (AsyncResult<String> rh) -> {
				rc.response().end(rh.result());
			});
		};
	}

	private Buffer fromCaffeineCache(int len) {
		int random = new Random().nextInt(MAX_RANGE);
		Function<Integer, Buffer> mapper = rnd -> {
			System.out.println("Creating caffeine cache entry...");
			return createBuffer(len);
		};
		switch (len) {
		case n4K:
			return cache4k.get(random, mapper);
		case n8K:
			return cache8k.get(random, mapper);
		case n64K:
			return cache64k.get(random, mapper);
		}
		return null;
	}

	private static Buffer createBuffer(int len) {
		String str = RandomStringUtils.randomAlphanumeric(len);
		return Buffer.buffer(str);
	}
}
