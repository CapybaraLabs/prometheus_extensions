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
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import io.prometheus.metrics.model.snapshots.Unit;
import java.util.function.Function;
import net.ttddyy.dsproxy.QueryCount;
import net.ttddyy.dsproxy.listener.SingleQueryCountHolder;

public class QueryCountCollector {

	private static final double MILLIS_PER_SECOND = 1000.0;

	private final SingleQueryCountHolder queryCountHolder;

	public QueryCountCollector(SingleQueryCountHolder queryCountHolder, PrometheusRegistry registry) {
		this.queryCountHolder = queryCountHolder;
		String[] labelNames = {"datasource"};

		CounterWithCallback.builder()
			.name("jdbc_query_select_total")
			.help("Total select queries")
			.labelNames(labelNames)
			.callback(callback -> collect(callback, QueryCount::getSelect))
			.register(registry);
		CounterWithCallback.builder()
			.name("jdbc_query_insert_total")
			.help("Total insert queries")
			.labelNames(labelNames)
			.callback(callback -> collect(callback, QueryCount::getInsert))
			.register(registry);
		CounterWithCallback.builder()
			.name("jdbc_query_update_total")
			.help("Total update queries")
			.labelNames(labelNames)
			.callback(callback -> collect(callback, QueryCount::getUpdate))
			.register(registry);
		CounterWithCallback.builder()
			.name("jdbc_query_delete_total")
			.help("Total delete queries")
			.labelNames(labelNames)
			.callback(callback -> collect(callback, QueryCount::getDelete))
			.register(registry);
		CounterWithCallback.builder()
			.name("jdbc_query_other_total")
			.help("Total other queries")
			.labelNames(labelNames)
			.callback(callback -> collect(callback, QueryCount::getOther))
			.register(registry);
		CounterWithCallback.builder()
			.name("jdbc_query_total")
			.help("Total queries")
			.labelNames(labelNames)
			.callback(callback -> collect(callback, QueryCount::getTotal))
			.register(registry);

		CounterWithCallback.builder()
			.name("jdbc_statement_total")
			.help("Total statements")
			.labelNames(labelNames)
			.callback(callback -> collect(callback, QueryCount::getStatement))
			.register(registry);
		CounterWithCallback.builder()
			.name("jdbc_prepared_total")
			.help("Total prepared statements")
			.labelNames(labelNames)
			.callback(callback -> collect(callback, QueryCount::getPrepared))
			.register(registry);
		CounterWithCallback.builder()
			.name("jdbc_callable_total")
			.help("Total callable statements")
			.labelNames(labelNames)
			.callback(callback -> collect(callback, QueryCount::getCallable))
			.register(registry);

		CounterWithCallback.builder()
			.name("jdbc_success_total")
			.help("Total successful queries")
			.labelNames(labelNames)
			.callback(callback -> collect(callback, QueryCount::getSuccess))
			.register(registry);
		CounterWithCallback.builder()
			.name("jdbc_failure_total")
			.help("Total failed queries")
			.labelNames(labelNames)
			.callback(callback -> collect(callback, QueryCount::getFailure))
			.register(registry);

		CounterWithCallback.builder()
			.name("jdbc_time_total")
			.help("Total query execution time in seconds")
			.unit(Unit.SECONDS)
			.labelNames(labelNames)
			.callback(callback -> collect(callback, qc -> qc.getTime() / MILLIS_PER_SECOND))
			.register(registry);
	}

	private void collect(CounterWithCallback.Callback callback, Function<QueryCount, Number> counter) {
		for (var entry : this.queryCountHolder.getQueryCountMap().entrySet()) {
			String datasourceName = entry.getKey();
			QueryCount queryCount = entry.getValue();

			double value = counter.apply(queryCount).doubleValue();

			callback.call(value, datasourceName);
		}
	}
}

