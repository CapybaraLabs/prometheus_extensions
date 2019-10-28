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

package space.npstr.prometheus_extensions.jda;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import net.dv8tion.jda.internal.requests.Route;

import static java.lang.reflect.Modifier.isStatic;

/**
 * BiG uGlY hAcKs in here
 */
public class RouteNamer {

    private final List<Field> staticRouteFields;

    public RouteNamer() {
        this.staticRouteFields = List.of(
                Route.Misc.class,
                Route.Applications.class,
                Route.Self.class,
                Route.Users.class,
                Route.Relationships.class,
                Route.Guilds.class,
                Route.Emotes.class,
                Route.Webhooks.class,
                Route.Roles.class,
                Route.Channels.class,
                Route.Messages.class,
                Route.Invites.class,
                Route.Custom.class
        )
                .stream()
                .flatMap(c -> Arrays.stream(c.getDeclaredFields()))
                .filter(f -> isStatic(f.getModifiers()))
                .collect(Collectors.toList());
    }

    public Optional<String> lookUpRouteName(final Route route) {
        return this.staticRouteFields.stream()
                .filter(f -> {
                    try {
                        return f.get(null) == route;
                    } catch (Exception e) {
                        return false;
                    }
                })
                .map(Field::getName)
                .findFirst();
    }
}
