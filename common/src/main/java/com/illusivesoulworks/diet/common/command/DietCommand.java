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
import com.illusivesoulworks.diet.api.type.IDietEffect;
import com.illusivesoulworks.diet.api.type.IDietGroup;
import com.illusivesoulworks.diet.api.type.IDietNotification;
import com.illusivesoulworks.diet.api.type.NotificationFrequency;
import com.illusivesoulworks.diet.common.config.DietConfig;
import com.illusivesoulworks.diet.common.data.group.DietGroups;
import com.illusivesoulworks.diet.common.data.notification.DietNotificationDispatcher;
import com.illusivesoulworks.diet.common.data.suite.DietSuites;
import com.illusivesoulworks.diet.platform.Services;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;

public class DietCommand {

  private static final int OP_PERMISSION_LEVEL = 2;

  private static final SuggestionProvider<CommandSourceStack> SUGGEST_GROUPS =
      (ctx, builder) -> SharedSuggestionProvider.suggest(
          DietGroups.SERVER.getGroups().stream().map(IDietGroup::getName), builder);

  private static final SuggestionProvider<CommandSourceStack> SUGGEST_NOTIFICATION_IDS =
      (ctx, builder) -> {
        ServerPlayer player = ctx.getSource().getPlayer();

        if (player == null) {
          return builder.buildFuture();
        }
        Set<String> ids = new HashSet<>();
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

  private static final SuggestionProvider<CommandSourceStack> SUGGEST_NOTIFICATION_SETS =
      (ctx, builder) -> {
        ServerPlayer player = ctx.getSource().getPlayer();

        if (player == null) {
          return builder.buildFuture();
        }
        Set<String> sets = new HashSet<>();
        Services.CAPABILITY.get(player).ifPresent(diet ->
            DietSuites.getSuite(player.level(), diet.getSuite()).ifPresent(suite -> {
              for (IDietEffect effect : suite.getEffects()) {
                IDietNotification n = effect.getNotification().orElse(null);

                if (n != null) {
                  sets.addAll(n.getSets());
                }
              }
            }));
        return SharedSuggestionProvider.suggest(sets, builder);
      };

  private static final SuggestionProvider<CommandSourceStack> SUGGEST_FREQUENCIES =
      (ctx, builder) -> SharedSuggestionProvider.suggest(
          Arrays.stream(NotificationFrequency.values())
              .map(f -> f.name().toLowerCase(Locale.ROOT)),
          builder);

  public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
    LiteralArgumentBuilder<CommandSourceStack> dietCommand = Commands.literal("diet");

    dietCommand.then(Commands.literal("get")
        .requires(p -> p.hasPermission(OP_PERMISSION_LEVEL))
        .then(Commands.argument("player", EntityArgument.player())
            .then(Commands.argument("group", StringArgumentType.word())
                .suggests(SUGGEST_GROUPS)
                .executes(ctx -> {
                  IDietGroup group = resolveGroup(ctx.getSource(),
                      StringArgumentType.getString(ctx, "group"));
                  if (group == null) {
                    return 0;
                  }
                  return get(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), group);
                }))));

    dietCommand.then(Commands.literal("set")
        .requires(p -> p.hasPermission(OP_PERMISSION_LEVEL))
        .then(Commands.argument("player", EntityArgument.player())
            .then(Commands.argument("group", StringArgumentType.word())
                .suggests(SUGGEST_GROUPS)
                .then(Commands.argument("value", FloatArgumentType.floatArg(0.0f, 1.0f))
                    .executes(ctx -> {
                      IDietGroup group = resolveGroup(ctx.getSource(),
                          StringArgumentType.getString(ctx, "group"));
                      if (group == null) {
                        return 0;
                      }
                      return set(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"),
                          FloatArgumentType.getFloat(ctx, "value"), group);
                    })))));

    dietCommand.then(Commands.literal("add")
        .requires(p -> p.hasPermission(OP_PERMISSION_LEVEL))
        .then(Commands.argument("player", EntityArgument.player())
            .then(Commands.argument("group", StringArgumentType.word())
                .suggests(SUGGEST_GROUPS)
                .then(Commands.argument("value", FloatArgumentType.floatArg(0.0f, 1.0f))
                    .executes(ctx -> {
                      IDietGroup group = resolveGroup(ctx.getSource(),
                          StringArgumentType.getString(ctx, "group"));
                      if (group == null) {
                        return 0;
                      }
                      return modify(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"),
                          FloatArgumentType.getFloat(ctx, "value"), group);
                    })))));

    dietCommand.then(Commands.literal("subtract")
        .requires(p -> p.hasPermission(OP_PERMISSION_LEVEL))
        .then(Commands.argument("player", EntityArgument.player())
            .then(Commands.argument("group", StringArgumentType.word())
                .suggests(SUGGEST_GROUPS)
                .then(Commands.argument("value", FloatArgumentType.floatArg(0.0f, 1.0f))
                    .executes(ctx -> {
                      IDietGroup group = resolveGroup(ctx.getSource(),
                          StringArgumentType.getString(ctx, "group"));
                      if (group == null) {
                        return 0;
                      }
                      return modify(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"),
                          -1 * FloatArgumentType.getFloat(ctx, "value"), group);
                    })))));

    dietCommand.then(Commands.literal("reset")
        .requires(p -> p.hasPermission(OP_PERMISSION_LEVEL))
        .then(Commands.argument("player", EntityArgument.player())
            .executes(ctx -> reset(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))));

    dietCommand.then(Commands.literal("pause")
        .requires(p -> p.hasPermission(OP_PERMISSION_LEVEL))
        .then(Commands.argument("player", EntityArgument.player())
            .executes(
                ctx -> active(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), false))));

    dietCommand.then(Commands.literal("resume")
        .requires(p -> p.hasPermission(OP_PERMISSION_LEVEL))
        .then(Commands.argument("player", EntityArgument.player())
            .executes(
                ctx -> active(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), true))));

    dietCommand.then(Commands.literal("clear")
        .requires(p -> p.hasPermission(OP_PERMISSION_LEVEL))
        .then(Commands.argument("player", EntityArgument.player())
            .executes(ctx -> clear(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))));

    LiteralArgumentBuilder<CommandSourceStack> exportArg = Commands.literal("export")
        .requires(p -> p.hasPermission(OP_PERMISSION_LEVEL))
        .executes(ctx -> export(ctx.getSource(), DietCsv.ExportMode.ALL));

    exportArg.then(Commands.literal("group").then(
        Commands.argument("group", StringArgumentType.word())
            .suggests(SUGGEST_GROUPS)
            .executes(ctx -> {
              IDietGroup group = resolveGroup(ctx.getSource(),
                  StringArgumentType.getString(ctx, "group"));
              if (group == null) {
                return 0;
              }
              return export(ctx.getSource(), group);
            })));

    exportArg.then(Commands.literal("mod_id").then(
        Commands.argument("mod_id", Services.REGISTRY.getModIdArgument())
            .executes(ctx -> export(ctx.getSource(), DietCsv.ExportMode.MOD_ID,
                StringArgumentType.getString(ctx, "mod_id")))));

    exportArg.then(Commands.literal("uncategorized")
        .executes(ctx -> export(ctx.getSource(), DietCsv.ExportMode.UNCATEGORIZED)));

    exportArg.then(Commands.literal("trails")
        .executes(ctx -> export(ctx.getSource(), DietCsv.ExportMode.TRAILS)));

    dietCommand.then(exportArg);

    dietCommand.then(Commands.literal("notify")
        .requires(p -> p.hasPermission(OP_PERMISSION_LEVEL))
        .then(Commands.argument("player", EntityArgument.player())
            .then(Commands.argument("notification_id", StringArgumentType.word())
                .suggests(SUGGEST_NOTIFICATION_IDS)
                .executes(ctx -> notifyTest(ctx.getSource(),
                    EntityArgument.getPlayer(ctx, "player"),
                    StringArgumentType.getString(ctx, "notification_id"))))));

    dietCommand.then(buildNotificationsTree());

    dispatcher.register(dietCommand);
  }

  private static LiteralArgumentBuilder<CommandSourceStack> buildNotificationsTree() {
    LiteralArgumentBuilder<CommandSourceStack> notifications = Commands.literal("notifications");

    notifications.then(Commands.literal("message")
        .then(Commands.argument("notification_id", StringArgumentType.word())
            .suggests(SUGGEST_NOTIFICATION_IDS)
            .then(Commands.argument("frequency", StringArgumentType.word())
                .suggests(SUGGEST_FREQUENCIES)
                .executes(ctx -> {
                  NotificationFrequency f = resolveFrequency(ctx.getSource(),
                      StringArgumentType.getString(ctx, "frequency"));
                  if (f == null) {
                    return 0;
                  }
                  return setNotificationFrequency(ctx.getSource(),
                      StringArgumentType.getString(ctx, "notification_id"), f);
                }))));

    notifications.then(Commands.literal("group")
        .then(Commands.argument("group", StringArgumentType.word())
            .suggests(SUGGEST_GROUPS)
            .then(Commands.argument("frequency", StringArgumentType.word())
                .suggests(SUGGEST_FREQUENCIES)
                .executes(ctx -> {
                  IDietGroup group = resolveGroup(ctx.getSource(),
                      StringArgumentType.getString(ctx, "group"));
                  if (group == null) {
                    return 0;
                  }
                  NotificationFrequency f = resolveFrequency(ctx.getSource(),
                      StringArgumentType.getString(ctx, "frequency"));
                  if (f == null) {
                    return 0;
                  }
                  return setGroup(ctx.getSource(), group, f);
                }))));

    notifications.then(Commands.literal("set")
        .then(Commands.argument("set_id", StringArgumentType.word())
            .suggests(SUGGEST_NOTIFICATION_SETS)
            .then(Commands.argument("frequency", StringArgumentType.word())
                .suggests(SUGGEST_FREQUENCIES)
                .executes(ctx -> {
                  NotificationFrequency f = resolveFrequency(ctx.getSource(),
                      StringArgumentType.getString(ctx, "frequency"));
                  if (f == null) {
                    return 0;
                  }
                  return setSet(ctx.getSource(),
                      StringArgumentType.getString(ctx, "set_id"), f);
                }))));

    notifications.then(Commands.literal("all")
        .then(Commands.argument("frequency", StringArgumentType.word())
            .suggests(SUGGEST_FREQUENCIES)
            .executes(ctx -> {
              NotificationFrequency f = resolveFrequency(ctx.getSource(),
                  StringArgumentType.getString(ctx, "frequency"));
              if (f == null) {
                return 0;
              }
              return setAll(ctx.getSource(), f);
            })));

    notifications.then(Commands.literal("list")
        .executes(ctx -> list(ctx.getSource())));

    notifications.then(Commands.literal("reset")
        .executes(ctx -> resetOverrides(ctx.getSource())));

    notifications.then(Commands.literal("options")
        .then(Commands.argument("notification_id", StringArgumentType.word())
            .suggests(SUGGEST_NOTIFICATION_IDS)
            .executes(ctx -> options(ctx.getSource(),
                StringArgumentType.getString(ctx, "notification_id")))));

    return notifications;
  }

  private static IDietGroup resolveGroup(CommandSourceStack source, String name) {
    IDietGroup group = DietGroups.SERVER.getGroup(name).orElse(null);

    if (group == null) {
      source.sendFailure(Component.translatable(
          "commands." + DietConstants.MOD_ID + ".group.unknown", name));
    }
    return group;
  }

  private static NotificationFrequency resolveFrequency(CommandSourceStack source, String name) {
    try {
      return NotificationFrequency.valueOf(name.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      source.sendFailure(Component.translatable(
          "commands." + DietConstants.MOD_ID + ".notifications.frequency.unknown", name));
      return null;
    }
  }

  private static int get(CommandSourceStack sender, ServerPlayer player, IDietGroup group) {
    Services.CAPABILITY.get(player).ifPresent(diet -> {
      float amount = diet.getValue(group.getName());
      sender.sendSuccess(
          () -> Component.translatable("commands." + DietConstants.MOD_ID + ".get.success",
              Component.translatable(
                  "groups." + DietConstants.MOD_ID + "." + group.getName() + ".name"), amount * 100,
              player.getName()), true);
    });
    return Command.SINGLE_SUCCESS;
  }

  private static int set(CommandSourceStack sender, ServerPlayer player, float value,
                         IDietGroup group) {
    Services.CAPABILITY.get(player).ifPresent(diet -> {
      if (diet.getValues().containsKey(group.getName())) {
        diet.setValue(group.getName(), value);
        diet.sync();
        sender.sendSuccess(
            () -> Component.translatable("commands." + DietConstants.MOD_ID + ".set.success",
                Component.translatable(
                    "groups." + DietConstants.MOD_ID + "." + group.getName() + ".name"),
                value * 100, player.getName()), true);
      }
    });
    return Command.SINGLE_SUCCESS;
  }

  private static int modify(CommandSourceStack sender, ServerPlayer player, float amount,
                            IDietGroup group) {

    if (amount != 0) {
      Services.CAPABILITY.get(player).ifPresent(diet -> {
        if (diet.getValues().containsKey(group.getName())) {
          diet.setValue(group.getName(), diet.getValue(group.getName()) + amount);
          diet.sync();
          String arg = amount > 0 ? "add" : "remove";
          sender.sendSuccess(() -> Component.translatable(
              "commands." + DietConstants.MOD_ID + "." + arg + ".success", Component.translatable(
                  "groups." + DietConstants.MOD_ID + "." + group.getName() + ".name"), amount * 100,
              player.getName()), true);
        }
      });
    }
    return Command.SINGLE_SUCCESS;
  }

  private static int reset(CommandSourceStack sender, ServerPlayer player) {
    Services.CAPABILITY.get(player).ifPresent(diet -> {
      DietSuites.getSuite(player.level(), diet.getSuite()).ifPresent(suite -> {

        for (IDietGroup group : suite.getGroups()) {
          diet.setValue(group.getName(), group.getDefaultValue());
        }
      });
      diet.sync();
      sender.sendSuccess(
          () -> Component.translatable("commands." + DietConstants.MOD_ID + ".reset.success",
              player.getName()), true);
    });
    return Command.SINGLE_SUCCESS;
  }

  private static int active(CommandSourceStack sender, ServerPlayer player, boolean flag) {
    Services.CAPABILITY.get(player).ifPresent(diet -> {
      diet.setActive(flag);
      diet.sync();
      String arg = flag ? "resume" : "pause";
      sender.sendSuccess(
          () -> Component.translatable("commands." + DietConstants.MOD_ID + "." + arg + ".success",
              player.getName()), true);
    });
    return Command.SINGLE_SUCCESS;
  }

  private static int clear(CommandSourceStack sender, ServerPlayer player) {

    for (AttributeInstance instance : player.getAttributes().getDirtyAttributes()) {

      for (AttributeModifier attributeModifier : instance.getModifiers()) {

        if (attributeModifier.getName().equals("Diet group effect")) {
          instance.removeModifier(attributeModifier.getId());
        }
      }
    }
    sender.sendSuccess(
        () -> Component.translatable("commands." + DietConstants.MOD_ID + ".clear.success",
            player.getName()), true);
    return Command.SINGLE_SUCCESS;
  }

  private static int export(CommandSourceStack sender, IDietGroup group) {

    if (sender.getEntity() instanceof Player) {
      sender.sendSuccess(
          () -> Component.translatable("commands." + DietConstants.MOD_ID + ".export.started"),
          true);
      DietCsv.writeGroup((Player) sender.getEntity(), group);
      sender.sendSuccess(
          () -> Component.translatable("commands." + DietConstants.MOD_ID + ".export.finished"),
          true);
    }
    return Command.SINGLE_SUCCESS;
  }

  private static int export(CommandSourceStack sender, DietCsv.ExportMode mode, String... args) {

    if (sender.getEntity() instanceof Player player) {
      sender.sendSuccess(
          () -> Component.translatable("commands." + DietConstants.MOD_ID + ".export.started"),
          true);

      if (mode == DietCsv.ExportMode.ALL) {
        DietCsv.write(player, "");
      } else if (mode == DietCsv.ExportMode.MOD_ID) {
        DietCsv.write(player, args[0]);
      } else if (mode == DietCsv.ExportMode.UNCATEGORIZED) {
        DietCsv.writeUncategorized(player);
      } else if (mode == DietCsv.ExportMode.TRAILS) {
        DietCsv.writeTrails(player);
      }
      sender.sendSuccess(
          () -> Component.translatable("commands." + DietConstants.MOD_ID + ".export.finished"),
          true);
    }
    return Command.SINGLE_SUCCESS;
  }

  private static int setNotificationFrequency(CommandSourceStack source, String notificationId,
                                              NotificationFrequency frequency) {
    ServerPlayer player = playerFrom(source);

    if (player == null) {
      return 0;
    }
    Services.CAPABILITY.get(player).ifPresent(diet -> {
      diet.setNotificationOverride(notificationId, frequency);
      source.sendSuccess(() -> Component.translatable(
          "commands.diet.notifications.message.success",
          notificationId, freqLabel(frequency)), false);
    });
    return Command.SINGLE_SUCCESS;
  }

  private static int setGroup(CommandSourceStack source, IDietGroup group,
                              NotificationFrequency frequency) {
    ServerPlayer player = playerFrom(source);

    if (player == null) {
      return 0;
    }
    Services.CAPABILITY.get(player).ifPresent(diet -> {
      int count = DietNotificationDispatcher.applyToGroup(diet, group.getName(), frequency);
      source.sendSuccess(() -> Component.translatable(
          "commands.diet.notifications.group.success",
          DietNotificationDispatcher.groupLabel(group.getName()),
          freqLabel(frequency), count), false);
    });
    return Command.SINGLE_SUCCESS;
  }

  private static int setSet(CommandSourceStack source, String setId,
                            NotificationFrequency frequency) {
    ServerPlayer player = playerFrom(source);

    if (player == null) {
      return 0;
    }
    Services.CAPABILITY.get(player).ifPresent(diet -> {
      int count = DietNotificationDispatcher.applyToSet(diet, setId, frequency);
      source.sendSuccess(() -> Component.translatable(
          "commands.diet.notifications.set.success",
          DietNotificationDispatcher.setLabel(setId), freqLabel(frequency), count), false);
    });
    return Command.SINGLE_SUCCESS;
  }

  private static int setAll(CommandSourceStack source, NotificationFrequency frequency) {
    ServerPlayer player = playerFrom(source);

    if (player == null) {
      return 0;
    }
    Services.CAPABILITY.get(player).ifPresent(diet -> {
      int count = DietNotificationDispatcher.applyToAll(diet, frequency);
      source.sendSuccess(() -> Component.translatable(
          "commands.diet.notifications.all.success",
          freqLabel(frequency), count), false);
    });
    return Command.SINGLE_SUCCESS;
  }

  private static int list(CommandSourceStack source) {
    ServerPlayer player = playerFrom(source);

    if (player == null) {
      return 0;
    }
    Services.CAPABILITY.get(player).ifPresent(diet ->
        DietSuites.getSuite(player.level(), diet.getSuite()).ifPresent(suite -> {
          source.sendSuccess(() -> Component.translatable(
              "commands.diet.notifications.list.header"), false);
          NotificationFrequency configDefault =
              DietConfig.SERVER.notificationsDefaultFrequency.get();

          for (IDietEffect effect : suite.getEffects()) {
            IDietNotification n = effect.getNotification().orElse(null);

            if (n == null) {
              continue;
            }
            NotificationFrequency override = diet.getNotificationOverrides().get(n.getId());
            NotificationFrequency suiteDefault = n.getDefaultFrequency().orElse(configDefault);
            NotificationFrequency effective =
                DietNotificationDispatcher.resolveEffective(diet, n);
            Component overrideLabel = override == null
                ? Component.translatable("commands.diet.notifications.list.no_override")
                : freqLabel(override);
            source.sendSuccess(() -> Component.translatable(
                "commands.diet.notifications.list.entry",
                DietNotificationDispatcher.notificationLabel(n),
                freqLabel(suiteDefault), overrideLabel, freqLabel(effective)), false);
          }
        }));
    return Command.SINGLE_SUCCESS;
  }

  private static int resetOverrides(CommandSourceStack source) {
    ServerPlayer player = playerFrom(source);

    if (player == null) {
      return 0;
    }
    Services.CAPABILITY.get(player).ifPresent(diet -> {
      diet.clearNotificationOverrides();
      source.sendSuccess(() -> Component.translatable(
          "commands.diet.notifications.reset.success"), false);
    });
    return Command.SINGLE_SUCCESS;
  }

  private static int options(CommandSourceStack source, String notificationId) {
    ServerPlayer player = playerFrom(source);

    if (player == null) {
      return 0;
    }
    Services.CAPABILITY.get(player).ifPresent(diet ->
        DietSuites.getSuite(player.level(), diet.getSuite()).ifPresent(suite -> {
          for (IDietEffect effect : suite.getEffects()) {
            IDietNotification n = effect.getNotification().orElse(null);

            if (n != null && n.getId().equals(notificationId)) {
              player.sendSystemMessage(
                  DietNotificationDispatcher.buildOptionsMenu(effect, n));
              return;
            }
          }
        }));
    return Command.SINGLE_SUCCESS;
  }

  private static int notifyTest(CommandSourceStack source, ServerPlayer player,
                                String notificationId) {
    Services.CAPABILITY.get(player).ifPresent(diet ->
        DietSuites.getSuite(player.level(), diet.getSuite()).ifPresent(suite -> {
          for (IDietEffect effect : suite.getEffects()) {
            IDietNotification n = effect.getNotification().orElse(null);

            if (n != null && n.getId().equals(notificationId)) {
              DietNotificationDispatcher.forceFire(player, effect, n);
              source.sendSuccess(() -> Component.translatable(
                  "commands.diet.notify.success", notificationId,
                  player.getName()), true);
              return;
            }
          }
        }));
    return Command.SINGLE_SUCCESS;
  }

  private static ServerPlayer playerFrom(CommandSourceStack source) {
    try {
      return source.getPlayerOrException();
    } catch (Exception e) {
      source.sendFailure(Component.translatable(
          "commands.diet.notifications.no_player"));
      return null;
    }
  }

  private static Component freqLabel(NotificationFrequency frequency) {
    return Component.translatable(
        "commands.diet.notifications.frequency." + frequency.name().toLowerCase(Locale.ROOT));
  }
}