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

package com.illusivesoulworks.diet.common.data.effect;

import com.google.common.collect.ImmutableSet;
import com.illusivesoulworks.diet.api.type.IDietNotification;
import com.illusivesoulworks.diet.api.type.NotificationFrequency;
import com.illusivesoulworks.diet.api.type.NotificationTrigger;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

public class DietNotification implements IDietNotification {

  private final String id;
  private final Set<String> sets;
  private final String message;
  private final NotificationTrigger trigger;
  @Nullable
  private final NotificationFrequency defaultFrequency;

  public DietNotification(String id, Set<String> sets, String message, NotificationTrigger trigger,
                          @Nullable NotificationFrequency defaultFrequency) {
    this.id = id;
    this.sets = ImmutableSet.copyOf(sets);
    this.message = message;
    this.trigger = trigger;
    this.defaultFrequency = defaultFrequency;
  }

  @Override
  public String getId() {
    return this.id;
  }

  @Override
  public Set<String> getSets() {
    return this.sets;
  }

  @Override
  public String getMessage() {
    return this.message;
  }

  @Override
  public NotificationTrigger getTrigger() {
    return this.trigger;
  }

  @Override
  public Optional<NotificationFrequency> getDefaultFrequency() {
    return Optional.ofNullable(this.defaultFrequency);
  }

  @Override
  public CompoundTag save() {
    CompoundTag tag = new CompoundTag();
    tag.putString("Id", this.id);
    tag.putString("Message", this.message);
    tag.putString("Trigger", this.trigger.name());

    if (this.defaultFrequency != null) {
      tag.putString("DefaultFrequency", this.defaultFrequency.name());
    }
    ListTag setsList = new ListTag();

    for (String s : this.sets) {
      setsList.add(StringTag.valueOf(s));
    }
    tag.put("Sets", setsList);
    return tag;
  }

  public static DietNotification load(CompoundTag tag) {
    String id = tag.getString("Id");
    String message = tag.getString("Message");
    NotificationTrigger trigger =
        NotificationTrigger.findOrDefault(tag.getString("Trigger"), NotificationTrigger.ENTER);
    NotificationFrequency defaultFrequency = tag.contains("DefaultFrequency", Tag.TAG_STRING)
        ? NotificationFrequency.findOrDefault(tag.getString("DefaultFrequency"), null)
        : null;
    Set<String> sets = new HashSet<>();
    ListTag setsList = tag.getList("Sets", Tag.TAG_STRING);

    for (int i = 0; i < setsList.size(); i++) {
      sets.add(setsList.getString(i));
    }
    return new DietNotification(id, sets, message, trigger, defaultFrequency);
  }
}
