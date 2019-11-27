package de.jotschi.vertx.c10k;

import org.apache.commons.lang3.RandomStringUtils;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Router;

public class ServerVerticle extends AbstractVerticle {

	public static final int SERVER_PORT = 8080;
	public static final String SERVER_HOST = "localhost";

	public HttpServer server;

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
		Buffer buffer4k = createBuffer(4 * 1024);
		Buffer buffer8k = createBuffer(8 * 1024);
		Buffer buffer64k = createBuffer(64 * 1024);

		Router router = Router.router(vertx);
		router.route("/64k").handler(rh -> {
			rh.response().end(buffer64k);
		});

		router.route("/8k").handler(rh -> {
			rh.response().end(buffer8k);
		});

		router.route("/4k").handler(rh -> {
			rh.response().end(buffer4k);
		});

		router.route("/0k").handler(rh -> {
			rh.response().end();
		});

		router.route("/sendFile/test").handler(rh -> {
			rh.response().sendFile("test");
		});
		return router;
	}

	private Buffer createBuffer(int len) {
		String str = RandomStringUtils.randomAlphanumeric(len);
		return Buffer.buffer(str);
	}
}
