/*
 * MIT License
 *
 * Copyright (c) 2021 Dennis Neufeld
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

package space.npstr.prometheus_extensions.d4j;

import discord4j.common.ReactorResources;
import discord4j.rest.http.client.ClientException;
import discord4j.rest.http.client.ClientResponse;
import discord4j.rest.http.client.DiscordWebClient;
import discord4j.rest.json.response.ErrorResponse;
import discord4j.rest.request.DiscordWebRequest;
import discord4j.rest.request.DiscordWebResponse;
import discord4j.rest.request.Router;
import discord4j.rest.request.RouterOptions;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.util.context.ContextView;
import space.npstr.prometheus_extensions.DiscordMetrics;

/**
 * Collect metrics about the executed web requests against the Discord API
 */
public class InstrumentedRouter implements Router {

	private static final Logger log = LoggerFactory.getLogger(InstrumentedRouter.class);
	private static final double MILLIS_PER_SECOND = 1000.0;

	private final DiscordMetrics discordMetrics;
	private final Router delegate;
	private final ReactorResources reactorResources;

	public InstrumentedRouter(DiscordMetrics discordMetrics, Router delegate, RouterOptions routerOptions) {
		this.discordMetrics = discordMetrics;
		this.delegate = delegate;
		this.reactorResources = routerOptions.getReactorResources();
	}

	@Override
	public DiscordWebResponse exchange(DiscordWebRequest request) {
		DiscordWebResponse exchange = delegate.exchange(request);

		Mono<ClientResponse> instrumentedResponse = exchange.mono()
			.doOnNext(response -> instrumentNext(response, request))
			.doOnError(error -> instrumentError(error, request));

		return new DiscordWebResponse(instrumentedResponse, reactorResources);
	}

	private void instrumentNext(ClientResponse response, DiscordWebRequest request) {
		HttpResponseStatus status = response.getHttpResponse().status();
		HttpMethod method = request.getRoute().getMethod();
		String uriTemplate = request.getRoute().getUriTemplate();
		ContextView contextView = response.getHttpResponse().currentContextView();
		Instant requestStarted = Instant.ofEpochMilli(contextView.get(DiscordWebClient.KEY_REQUEST_TIMESTAMP));
		long responseTimeMillis = Duration.between(requestStarted, Instant.now()).toMillis();
		double responseTimeSeconds = responseTimeMillis / MILLIS_PER_SECOND;

		log.trace("{} {} {}ms {}", method, uriTemplate, responseTimeMillis, status.code());
		this.discordMetrics.getDiscordRestRequests()
			.labelValues(method.name(), uriTemplate, Integer.toString(status.code()), "")
			.observe(responseTimeSeconds);
		this.discordMetrics.getDiscordRestRequestResponseTime()
			.observe(responseTimeSeconds);
	}

	private void instrumentError(Throwable throwable, DiscordWebRequest request) {
		HttpMethod method = request.getRoute().getMethod();
		String uriTemplate = request.getRoute().getUriTemplate();
		if (throwable instanceof ClientException) {
			ClientException error = (ClientException) throwable;
			HttpResponseStatus status = error.getStatus();
			Optional<Map<String, Object>> errorResponseContent = error.getErrorResponse().map(ErrorResponse::getFields);
			int errorCode = errorResponseContent.map(it -> (Integer) it.get("code")).orElse(-1);
			String errorMessage = errorResponseContent.map(it -> (String) it.get("message")).orElse(null);

			ContextView contextView = error.getResponse().currentContextView();
			Instant requestStarted = Instant.ofEpochMilli(contextView.get(DiscordWebClient.KEY_REQUEST_TIMESTAMP));
			long responseTimeMillis = Duration.between(requestStarted, Instant.now()).toMillis();
			double responseTimeSeconds = responseTimeMillis / MILLIS_PER_SECOND;

			log.trace("{} {} {}ms {} {} {}", method, uriTemplate, responseTimeMillis, status.code(), errorCode, errorMessage);
			this.discordMetrics.getDiscordRestRequests()
				.labelValues(method.name(), uriTemplate, Integer.toString(status.code()), Integer.toString(errorCode))
				.observe(responseTimeSeconds);
			this.discordMetrics.getDiscordRestRequestResponseTime()
				.observe(responseTimeSeconds);

			if (status.code() == 401) {
				log.warn("Encountered invalid token on route {} {} with message {}, request: {}",
					method, uriTemplate, errorMessage, request, throwable);
			}
		} else {
			log.warn("Failed request to {} {}", method, uriTemplate, throwable);
			this.discordMetrics.getDiscordRestHardFailures()
				.labelValues(method.name(), uriTemplate)
				.inc();
		}
	}
}
