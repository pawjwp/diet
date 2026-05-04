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

import com.illusivesoulworks.diet.DietConstants;
import com.illusivesoulworks.diet.api.type.NotificationFrequency;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

public class NotificationFrequencyArgument implements ArgumentType<NotificationFrequency> {
  private static final Collection<String> EXAMPLES = Arrays.asList("once", "always", "never");

  public static final DynamicCommandExceptionType FREQUENCY_UNKNOWN =
      new DynamicCommandExceptionType((freq) -> Component.translatable(
          "commands." + DietConstants.MOD_ID + ".notifications.frequency.unknown", freq));

  public static NotificationFrequencyArgument frequency() {
    return new NotificationFrequencyArgument();
  }

  public static NotificationFrequency get(CommandContext<?> context, String name) {
    return context.getArgument(name, NotificationFrequency.class);
  }

  @Override
  public NotificationFrequency parse(StringReader input) throws CommandSyntaxException {
    String name = input.readUnquotedString();
    try {
      return NotificationFrequency.valueOf(name.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      throw FREQUENCY_UNKNOWN.create(name);
    }
  }

  @Override
  public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> ctx,
                                                            SuggestionsBuilder builder) {
    return SharedSuggestionProvider.suggest(
        Arrays.stream(NotificationFrequency.values())
            .map(f -> f.name().toLowerCase(Locale.ROOT)),
        builder);
  }

  @Override
  public Collection<String> getExamples() {
    return EXAMPLES;
  }
}
