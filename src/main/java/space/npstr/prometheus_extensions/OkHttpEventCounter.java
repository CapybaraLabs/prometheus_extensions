/*
 * MIT License
 *
 * Copyright (c) 2019 Dennis Neufeld
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

package space.npstr.prometheus_extensions;

import io.prometheus.client.Counter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.List;
import okhttp3.Call;
import okhttp3.Connection;
import okhttp3.EventListener;
import okhttp3.Handshake;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;

public class OkHttpEventCounter extends EventListener {

	private static final Counter httpEventCounter = Counter.build()
		.name("okhttp_events_total")
		.help("Total okhttp events")
		.labelNames("instance", "event")
		.register();

	private final String instanceLabel;

	/**
	 * @param instanceLabel meta information about the okhttp client where this counter will be registered
	 */
	public OkHttpEventCounter(final String instanceLabel) {
		this.instanceLabel = instanceLabel;
	}

	@Override
	public void callStart(final Call call) {
		httpEventCounter.labels(this.instanceLabel, "callStart").inc();
	}

	@Override
	public void dnsStart(final Call call, final String domainName) {
		httpEventCounter.labels(this.instanceLabel, "dnsStart").inc();
	}

	@Override
	public void dnsEnd(final Call call, final String domainName, final List<InetAddress> inetAddressList) {
		httpEventCounter.labels(this.instanceLabel, "dnsEnd").inc();
	}

	@Override
	public void connectStart(final Call call, final InetSocketAddress inetSocketAddress, final Proxy proxy) {
		httpEventCounter.labels(this.instanceLabel, "connectStart").inc();
	}

	@Override
	public void secureConnectStart(final Call call) {
		httpEventCounter.labels(this.instanceLabel, "secureConnectStart").inc();
	}

	@Override
	public void secureConnectEnd(final Call call, final Handshake handshake) {
		httpEventCounter.labels(this.instanceLabel, "secureConnectEnd").inc();
	}

	@Override
	public void connectEnd(final Call call, final InetSocketAddress inetSocketAddress, final Proxy proxy, final Protocol protocol) {
		httpEventCounter.labels(this.instanceLabel, "connectEnd").inc();
	}

	@Override
	public void connectFailed(final Call call, final InetSocketAddress inetSocketAddress, final Proxy proxy, final Protocol protocol, final IOException ioe) {
		httpEventCounter.labels(this.instanceLabel, "connectFailed").inc();
	}

	@Override
	public void connectionAcquired(final Call call, final Connection connection) {
		httpEventCounter.labels(this.instanceLabel, "connectionAcquired").inc();
	}

	@Override
	public void connectionReleased(final Call call, final Connection connection) {
		httpEventCounter.labels(this.instanceLabel, "connectionReleased").inc();
	}

	@Override
	public void requestHeadersStart(final Call call) {
		httpEventCounter.labels(this.instanceLabel, "requestHeadersStart").inc();
	}

	@Override
	public void requestHeadersEnd(final Call call, final Request request) {
		httpEventCounter.labels(this.instanceLabel, "requestHeadersEnd").inc();
	}

	@Override
	public void requestBodyStart(final Call call) {
		httpEventCounter.labels(this.instanceLabel, "requestBodyStart").inc();
	}

	@Override
	public void requestBodyEnd(final Call call, final long byteCount) {
		httpEventCounter.labels(this.instanceLabel, "requestBodyEnd").inc();
	}

	@Override
	public void responseHeadersStart(final Call call) {
		httpEventCounter.labels(this.instanceLabel, "responseHeadersStart").inc();
	}

	@Override
	public void responseHeadersEnd(final Call call, final Response response) {
		httpEventCounter.labels(this.instanceLabel, "responseHeadersEnd").inc();
	}

	@Override
	public void responseBodyStart(final Call call) {
		httpEventCounter.labels(this.instanceLabel, "responseBodyStart").inc();
	}

	@Override
	public void responseBodyEnd(final Call call, final long byteCount) {
		httpEventCounter.labels(this.instanceLabel, "responseBodyEnd").inc();
	}

	@Override
	public void callEnd(final Call call) {
		httpEventCounter.labels(this.instanceLabel, "callEnd").inc();
	}

	@Override
	public void callFailed(final Call call, final IOException ioe) {
		httpEventCounter.labels(this.instanceLabel, "callFailed").inc();
	}
}

