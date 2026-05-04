/*
 * Copyright (C) 2021-2023 Illusive Soulworks
 *
 * Diet is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * Diet is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Diet.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.illusivesoulworks.diet.common.command;

import com.illusivesoulworks.diet.api.type.IDietEffect;
import com.illusivesoulworks.diet.api.type.IDietNotification;
import com.illusivesoulworks.diet.common.data.suite.DietSuites;
import com.illusivesoulworks.diet.platform.Services;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.level.ServerPlayer;

public class DietNotificationIdArgument implements ArgumentType<String> {
  private static final Collection<String> EXAMPLES = Arrays.asList("fruits_low", "balanced");

  public static final SuggestionProvider<CommandSourceStack> SUGGESTIONS = (ctx, builder) -> {
    ServerPlayer player = ctx.getSource().getPlayer();

    if (player == null) {
      return builder.buildFuture();
    }
    List<String> ids = new ArrayList<>();
    Services.CAPABILITY.get(player).ifPresent(diet ->
        DietSuites.getSuite(player.level(), diet.getSuite()).ifPresent(suite -> {
          for (IDietEffect effect : suite.getEffects()) {
            IDietNotification n = effect.getNotification().orElse(null);

            if (n != null) {
              ids.add(n.getId());
            }
          }
        }));
    return SharedSuggestionProvider.suggest(ids, builder);
  };

  public static DietNotificationIdArgument id() {
    return new DietNotificationIdArgument();
  }

  public static String get(CommandContext<?> context, String name) {
    return context.getArgument(name, String.class);
  }

  @Override
  public String parse(StringReader input) throws CommandSyntaxException {
    return input.readUnquotedString();
  }

  @Override
  public Collection<String> getExamples() {
    return EXAMPLES;
  }
}
