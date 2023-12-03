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

package space.npstr.prometheus_extensions;

import io.prometheus.metrics.core.metrics.CounterWithCallback;
import io.prometheus.metrics.core.metrics.GaugeWithCallback;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class ThreadPoolCollector {

	protected final ConcurrentMap<String, ThreadPoolExecutor> pools = new ConcurrentHashMap<>();

	/**
	 * Add or replace the pool with the given name.
	 * <p>
	 * Any references to any previous pool with this name are invalidated.
	 *
	 * @param poolName The name of the pool, will be the metrics label value
	 * @param pool     The pool being monitored
	 */
	public void addPool(String poolName, ThreadPoolExecutor pool) {
		this.pools.put(poolName, pool);
	}

	/**
	 * Remove the pool with the given name.
	 * <p>
	 * Any references to the pool are invalidated.
	 *
	 * @param poolName pool to be removed
	 */
	public ThreadPoolExecutor removePool(String poolName) {
		return this.pools.remove(poolName);
	}

	/**
	 * Remove all pools.
	 * <p>
	 * Any references to all pools are invalidated.
	 */
	public void clear() {
		this.pools.clear();
	}

	public ThreadPoolCollector(PrometheusRegistry registry) {
		String[] labelNames = {"name"};

		GaugeWithCallback.builder()
			.name("threadpool_active_threads_current")
			.help("Amount of active threads in a thread pool")
			.labelNames(labelNames)
			.callback(callback -> collect(callback::call, ThreadPoolExecutor::getActiveCount))
			.register(registry);

		GaugeWithCallback.builder()
			.name("threadpool_queue_size_current")
			.help("Size of queue of a thread pool (including scheduled tasks)")
			.labelNames(labelNames)
			.callback(callback -> collect(callback::call, pool -> pool.getQueue().size()))
			.register(registry);

		CounterWithCallback.builder()
			.name("threadpool_completed_tasks_total")
			.help("Total completed tasks by a thread pool")
			.labelNames(labelNames)
			.callback(callback -> collect(callback::call, ThreadPoolExecutor::getCompletedTaskCount))
			.register(registry);
	}

	private void collect(BiConsumer<Double, String[]> callback, Function<ThreadPoolExecutor, Number> counter) {
		for (var entry : this.pools.entrySet()) {
			String poolName = entry.getKey();
			String[] labels = {poolName};
			ThreadPoolExecutor pool = entry.getValue();
			double value = counter.apply(pool).doubleValue();

			callback.accept(value, labels);
		}
	}
}
