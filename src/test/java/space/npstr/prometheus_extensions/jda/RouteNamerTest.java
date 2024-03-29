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

import net.dv8tion.jda.api.requests.Method;
import net.dv8tion.jda.api.requests.Route;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RouteNamerTest {

	private final RouteNamer routeNamer = new RouteNamer();

	@Test
	void routeNames() {

		assertThat(this.routeNamer.lookUpRouteName(Route.Misc.GET_VOICE_REGIONS))
			.isEqualTo("GET_VOICE_REGIONS");
		assertThat(this.routeNamer.lookUpRouteName(Route.Applications.UPDATE_ROLE_CONNECTION_METADATA))
			.isEqualTo("UPDATE_ROLE_CONNECTION_METADATA");
		assertThat(this.routeNamer.lookUpRouteName(Route.Interactions.EDIT_ALL_COMMAND_PERMISSIONS))
			.isEqualTo("EDIT_ALL_COMMAND_PERMISSIONS");
		assertThat(this.routeNamer.lookUpRouteName(Route.Self.LEAVE_GUILD))
			.isEqualTo("LEAVE_GUILD");
		assertThat(this.routeNamer.lookUpRouteName(Route.Users.GET_USER))
			.isEqualTo("GET_USER");
		assertThat(this.routeNamer.lookUpRouteName(Route.Guilds.BAN))
			.isEqualTo("BAN");
		assertThat(this.routeNamer.lookUpRouteName(Route.Emojis.MODIFY_EMOJI))
			.isEqualTo("MODIFY_EMOJI");
		assertThat(this.routeNamer.lookUpRouteName(Route.Stickers.DELETE_GUILD_STICKER))
			.isEqualTo("DELETE_GUILD_STICKER");
		assertThat(this.routeNamer.lookUpRouteName(Route.Webhooks.EXECUTE_WEBHOOK_GITHUB))
			.isEqualTo("EXECUTE_WEBHOOK_GITHUB");
		assertThat(this.routeNamer.lookUpRouteName(Route.Roles.CREATE_ROLE))
			.isEqualTo("CREATE_ROLE");
		assertThat(this.routeNamer.lookUpRouteName(Route.Channels.MODIFY_PERM_OVERRIDE))
			.isEqualTo("MODIFY_PERM_OVERRIDE");
		assertThat(this.routeNamer.lookUpRouteName(Route.StageInstances.UPDATE_INSTANCE))
			.isEqualTo("UPDATE_INSTANCE");
		assertThat(this.routeNamer.lookUpRouteName(Route.Messages.GET_MESSAGE_HISTORY))
			.isEqualTo("GET_MESSAGE_HISTORY");
		assertThat(this.routeNamer.lookUpRouteName(Route.Invites.GET_CHANNEL_INVITES))
			.isEqualTo("GET_CHANNEL_INVITES");
		assertThat(this.routeNamer.lookUpRouteName(Route.Templates.MODIFY_TEMPLATE))
			.isEqualTo("MODIFY_TEMPLATE");
		assertThat(this.routeNamer.lookUpRouteName(Route.custom(Method.PATCH, "/")))
			.isEqualTo("CUSTOM_PATCH");

	}

}
