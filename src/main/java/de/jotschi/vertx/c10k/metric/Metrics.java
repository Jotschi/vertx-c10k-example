package de.jotschi.vertx.c10k.metric;

import java.util.EnumSet;
import java.util.stream.Stream;

import de.jotschi.vertx.c10k.ServerVerticle;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.vertx.core.metrics.MetricsOptions;
import io.vertx.micrometer.Label;
import io.vertx.micrometer.Match;
import io.vertx.micrometer.MetricsDomain;
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.VertxPrometheusOptions;

public class Metrics {

	public static final String REGISTRY_NAME = "c10k";

	public static MeterRegistry meterRegistry() {
		return new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
	}

	public static MetricsOptions getOptions() {
		MicrometerMetricsOptions metricsOptions = new MicrometerMetricsOptions()
			.setMicrometerRegistry(meterRegistry())
			.setRegistryName(REGISTRY_NAME)
			.setJvmMetricsEnabled(true)
			.setLabels(EnumSet.of(Label.HTTP_CODE, Label.HTTP_METHOD, Label.LOCAL, Label.HTTP_PATH))
			.setPrometheusOptions(new VertxPrometheusOptions().setEnabled(true))
			.setEnabled(true);

		labelMatches().forEach(metricsOptions::addLabelMatch);

		return metricsOptions;
	}

	private static Stream<Match> labelMatches() {
		return Stream.of(
			new Match()
				.setDomain(MetricsDomain.HTTP_SERVER)
				.setLabel("local")
				.setAlias("restapi")
				.setValue(ServerVerticle.SERVER_HOST + ":" + ServerVerticle.SERVER_PORT),
			new Match()
				.setDomain(MetricsDomain.HTTP_SERVER)
				.setLabel("local")
				.setAlias("monitoring")
				.setValue(MetricsVerticle.SERVER_HOST + ":" + MetricsVerticle.SERVER_PORT));
	}

}
