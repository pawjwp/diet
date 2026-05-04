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

package com.illusivesoulworks.diet.common.data.notification;

import com.illusivesoulworks.diet.DietConstants;
import com.illusivesoulworks.diet.api.type.IDietCondition;
import com.illusivesoulworks.diet.api.type.IDietEffect;
import com.illusivesoulworks.diet.api.type.IDietGroup;
import com.illusivesoulworks.diet.api.type.IDietNotification;
import com.illusivesoulworks.diet.api.type.IDietSuite;
import com.illusivesoulworks.diet.api.type.IDietTracker;
import com.illusivesoulworks.diet.api.type.NotificationFrequency;
import com.illusivesoulworks.diet.common.config.DietConfig;
import com.illusivesoulworks.diet.common.data.group.DietGroups;
import com.illusivesoulworks.diet.common.data.suite.DietSuites;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.BiPredicate;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class DietNotificationDispatcher {

  private DietNotificationDispatcher() {
  }

  public static NotificationFrequency resolveEffective(IDietTracker tracker,
                                                       IDietNotification notification) {
    if (!DietConfig.SERVER.notificationsEnabled.get()) {
      return NotificationFrequency.NEVER;
    }
    NotificationFrequency override =
        tracker.getNotificationOverrides().get(notification.getId());

    if (override != null) {
      return override;
    }
    return notification.getDefaultFrequency()
        .orElseGet(() -> DietConfig.SERVER.notificationsDefaultFrequency.get());
  }

  public static void tryFire(IDietTracker tracker, IDietEffect effect,
                             IDietNotification notification) {
    if (!DietConfig.SERVER.notificationsEnabled.get()) {
      return;
    }

    if (!(tracker.getPlayer() instanceof ServerPlayer sp)) {
      return;
    }
    NotificationFrequency effective = resolveEffective(tracker, notification);

    if (effective == NotificationFrequency.NEVER) {
      return;
    }
    sp.sendSystemMessage(buildChatLine(effect, notification, effective));

    if (effective == NotificationFrequency.ONCE) {
      tracker.setNotificationOverride(notification.getId(), NotificationFrequency.NEVER);
    }
  }

  public static void forceFire(ServerPlayer player, IDietEffect effect,
                               IDietNotification notification) {
    player.sendSystemMessage(buildChatLine(effect, notification, NotificationFrequency.ALWAYS));
  }

  private static Component buildChatLine(IDietEffect effect, IDietNotification notification,
                                         NotificationFrequency effective) {
    Object[] args = collectGroupArgs(effect);
    MutableComponent body = Component.translatable(notification.getMessage(), args);

    if (effective == NotificationFrequency.ONCE) {
      MutableComponent notice = Component.translatable("notification.diet.once_notice")
          .withStyle(s -> s.withColor(ChatFormatting.GRAY).withItalic(true));
      return body.append(Component.literal("\n")).append(notice);
    }
    MutableComponent muteBtn = Component.translatable("notification.diet.mute_button")
        .withStyle(s -> s
            .withColor(ChatFormatting.YELLOW)
            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                "/diet notifications options " + notification.getId()))
            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                Component.translatable("notification.diet.mute_button.hover"))));
    return body.append(Component.literal(" ")).append(muteBtn);
  }

  private static Object[] collectGroupArgs(IDietEffect effect) {
    Set<String> groups = new LinkedHashSet<>();

    for (IDietCondition condition : effect.getConditions()) {
      groups.addAll(condition.getGroups());
    }
    return groups.stream()
        .map(g -> Component.translatable("groups." + DietConstants.MOD_ID + "." + g + ".name"))
        .toArray();
  }

  public static Component buildOptionsMenu(IDietEffect effect, IDietNotification notification) {
    MutableComponent line = Component.translatable("notification.diet.options.header",
            notificationLabel(notification))
        .withStyle(s -> s.withColor(ChatFormatting.WHITE));

    line.append(Component.literal("\n"));
    line.append(Component.translatable("notification.diet.mute_message.label")
        .withStyle(s -> s.withColor(ChatFormatting.WHITE)));
    line.append(Component.literal(" ").withStyle(s -> s.withColor(ChatFormatting.WHITE)));
    line.append(button(
        Component.translatable("notification.diet.mute_message.entry",
            notificationLabel(notification)),
        Component.translatable("notification.diet.mute_message.hover"),
        "/diet notifications message " + notification.getId() + " never",
        ChatFormatting.YELLOW.getColor()));

    if (!notification.getSets().isEmpty()) {
      line.append(Component.literal("\n"));
      line.append(Component.translatable("notification.diet.mute_set.label")
          .withStyle(s -> s.withColor(ChatFormatting.WHITE)));

      for (String setId : notification.getSets()) {
        line.append(Component.literal(" ").withStyle(s -> s.withColor(ChatFormatting.WHITE)));
        line.append(button(
            Component.translatable("notification.diet.mute_set.entry", setLabel(setId)),
            Component.translatable("notification.diet.mute_set.hover"),
            "/diet notifications set " + setId + " never",
            ChatFormatting.YELLOW.getColor()));
      }
    }
    Set<String> groupNames = new LinkedHashSet<>();

    for (IDietCondition condition : effect.getConditions()) {
      groupNames.addAll(condition.getGroups());
    }

    if (!groupNames.isEmpty()) {
      line.append(Component.literal("\n"));
      line.append(Component.translatable("notification.diet.mute_group.label")
          .withStyle(s -> s.withColor(ChatFormatting.WHITE)));

      for (String g : groupNames) {
        Integer color = DietGroups.SERVER.getGroup(g)
            .map(IDietGroup::getColor)
            .map(c -> ((c.red() & 0xFF) << 16) | ((c.blue() & 0xFF) << 8) | (c.green() & 0xFF))
            .orElse(null);
        line.append(Component.literal(" ").withStyle(s -> s.withColor(ChatFormatting.WHITE)));
        line.append(button(
            Component.translatable("notification.diet.mute_group.entry",
                Component.translatable("groups." + DietConstants.MOD_ID + "." + g + ".name")),
            Component.translatable("notification.diet.mute_group.hover"),
            "/diet notifications group " + g + " never",
            color));
      }
    }
    line.append(Component.literal("\n"));
    line.append(Component.translatable("notification.diet.mute_all.label")
        .withStyle(s -> s.withColor(ChatFormatting.WHITE)));
    line.append(Component.literal(" ").withStyle(s -> s.withColor(ChatFormatting.WHITE)));
    line.append(button(
        Component.translatable("notification.diet.mute_all"),
        Component.translatable("notification.diet.mute_all.hover"),
        "/diet notifications all never",
        ChatFormatting.RED.getColor()));
    return line;
  }

  private static MutableComponent button(Component label, Component hover, String command,
                                         Integer rgb) {
    return label.copy().withStyle(s -> s
        .withColor(rgb == null ? TextColor.fromLegacyFormat(ChatFormatting.YELLOW) : TextColor.fromRgb(rgb))
        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover)));
  }

  public static Component notificationLabel(IDietNotification notification) {
    return Component.translatable(
        "notification." + DietConstants.MOD_ID + ".id." + notification.getId() + ".name");
  }

  public static Component setLabel(String setId) {
    return Component.translatable(
        "notification." + DietConstants.MOD_ID + ".set." + setId + ".name");
  }

  public static Component groupLabel(String groupName) {
    return Component.translatable("groups." + DietConstants.MOD_ID + "." + groupName + ".name");
  }

  public static int applyToGroup(IDietTracker tracker, String groupName,
                                 NotificationFrequency frequency) {
    return walkSuite(tracker, (effect, notification) ->
        effect.getConditions().stream().anyMatch(c -> c.getGroups().contains(groupName)),
        frequency);
  }

  public static int applyToSet(IDietTracker tracker, String setId,
                               NotificationFrequency frequency) {
    return walkSuite(tracker, (effect, notification) -> notification.getSets().contains(setId),
        frequency);
  }

  public static int applyToAll(IDietTracker tracker, NotificationFrequency frequency) {
    return walkSuite(tracker, (effect, notification) -> true, frequency);
  }

  private static int walkSuite(IDietTracker tracker,
                               BiPredicate<IDietEffect, IDietNotification> filter,
                               NotificationFrequency frequency) {
    Player player = tracker.getPlayer();
    IDietSuite suite =
        DietSuites.getSuite(player.level(), tracker.getSuite()).orElse(null);

    if (suite == null) {
      return 0;
    }
    int count = 0;

    for (IDietEffect effect : suite.getEffects()) {
      IDietNotification n = effect.getNotification().orElse(null);

      if (n == null) {
        continue;
      }

      if (filter.test(effect, n)) {
        tracker.setNotificationOverride(n.getId(), frequency);
        count++;
      }
    }
    return count;
  }
}
