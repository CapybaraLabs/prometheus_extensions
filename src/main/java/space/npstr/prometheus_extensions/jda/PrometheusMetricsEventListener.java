/*
 * MIT License
 *
 * Copyright (c) 2019-2021 Dennis Neufeld
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package space.npstr.prometheus_extensions.jda;

import io.prometheus.metrics.core.metrics.Counter;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import java.util.Optional;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.http.HttpRequestEvent;
import net.dv8tion.jda.api.events.session.SessionDisconnectEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.Response;
import net.dv8tion.jda.api.requests.Route;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.npstr.prometheus_extensions.DiscordMetrics;

/**
 * Collect metrics from events happening on the shard
 */
class PrometheusMetricsEventListener extends ListenerAdapter {

	private static final Logger log = LoggerFactory.getLogger(PrometheusMetricsEventListener.class);

	public static final int NO_RESPONSE_CODE = 442;

	private final RouteNamer routeNamer = new RouteNamer();
	private final DiscordMetrics discordMetrics;
	private final Counter httpRequests;

	PrometheusMetricsEventListener(PrometheusRegistry registry, DiscordMetrics discordMetrics) {
		this.discordMetrics = discordMetrics;
		this.httpRequests = Counter.builder()
			.name("jda_restactions_total")
			.help("JDA restactions and their HTTP responses")
			.labelNames("status", "route")
			.register(registry);
	}

	@Override
	public void onGenericEvent(GenericEvent event) {
		this.discordMetrics.getEvents().labelValues(event.getClass().getSimpleName()).inc();
	}

	@Override
	public void onSessionDisconnect(SessionDisconnectEvent event) {
		if (!event.isClosedByServer()) {
			return;
		}
		String code = Optional.ofNullable(event.getServiceCloseFrame()).stream()
			.peek(frame -> log.info("Shard {} websocket closed by server with {} {}",
				event.getJDA().getShardInfo().getShardId(),
				frame.getCloseCode(), frame.getCloseReason())
			)
			.map(frame -> Integer.toString(frame.getCloseCode()))
			.findAny()
			.orElse("null");

		this.discordMetrics.getCloseCodes().labelValues(code).inc();
	}

	@Override
	public void onHttpRequest(final HttpRequestEvent event) {
		final Response response = event.getResponse();

		final String code = Optional.ofNullable(response)
			.map(r -> r.code)
			.orElse(NO_RESPONSE_CODE)
			.toString();
		final Route route = event.getRoute().getBaseRoute();

		final String routeName = this.routeNamer.lookUpRouteName(route);

		this.httpRequests.labelValues(code, routeName).inc();
	}


}
