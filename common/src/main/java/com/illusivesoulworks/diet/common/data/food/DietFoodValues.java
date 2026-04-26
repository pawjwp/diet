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

package com.illusivesoulworks.diet.common.data.food;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.illusivesoulworks.diet.DietConstants;
import com.illusivesoulworks.diet.api.type.IDietGroup;
import com.illusivesoulworks.diet.common.data.group.DietGroups;
import com.illusivesoulworks.diet.platform.Services;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nonnull;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class DietFoodValues extends SimpleJsonResourceReloadListener {

  private static final Gson GSON =
      (new GsonBuilder()).setPrettyPrinting().disableHtmlEscaping().create();

  public static final DietFoodValues SERVER = Services.CAPABILITY.getFoodValuesListener();
  public static final DietFoodValues CLIENT = Services.CAPABILITY.getFoodValuesListener();

  private Map<Item, Map<String, Float>> itemEntries = new HashMap<>();
  private List<TagEntry> tagEntries = new ArrayList<>();

  public DietFoodValues() {
    super(GSON, "diet/food_values");
  }

  public Optional<Map<IDietGroup, Float>> lookup(ItemStack stack, Set<IDietGroup> availableGroups) {

    if (stack.isEmpty() || (itemEntries.isEmpty() && tagEntries.isEmpty())) {
      return Optional.empty();
    }
    Map<String, Float> raw = itemEntries.get(stack.getItem());

    if (raw == null) {

      for (TagEntry entry : tagEntries) {

        if (stack.is(entry.tag)) {
          raw = entry.values;
          break;
        }
      }
    }

    if (raw == null) {
      return Optional.empty();
    }
    Map<IDietGroup, Float> resolved = new HashMap<>();

    for (IDietGroup group : availableGroups) {
      Float value = raw.get(group.getName());

      if (value != null) {
        resolved.put(group, value);
      }
    }

    if (resolved.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(resolved);
  }

  @Override
  protected void apply(@Nonnull Map<ResourceLocation, JsonElement> object,
                       @Nonnull ResourceManager resourceManager,
                       @Nonnull ProfilerFiller profilerFiller) {
    Map<Item, Map<String, Float>> items = new HashMap<>();
    List<TagEntry> tags = new ArrayList<>();
    Set<String> warnedUnknownGroups = new HashSet<>();
    Set<String> knownGroupNames = new HashSet<>();

    for (IDietGroup group : DietGroups.SERVER.getGroups()) {
      knownGroupNames.add(group.getName());
    }

    for (Map.Entry<ResourceLocation, JsonElement> entry : object.entrySet()) {
      ResourceLocation resourcelocation = entry.getKey();

      try {
        JsonObject top = GsonHelper.convertToJsonObject(entry.getValue(), "top element");
        boolean replace = GsonHelper.getAsBoolean(top, "replace", false);

        if (replace) {
          items.clear();
          tags.clear();
        }
        JsonObject values = GsonHelper.getAsJsonObject(top, "values", new JsonObject());

        for (Map.Entry<String, JsonElement> valueEntry : values.entrySet()) {
          String key = valueEntry.getKey();
          JsonObject groupValues =
              GsonHelper.convertToJsonObject(valueEntry.getValue(), key);
          Map<String, Float> parsed = new HashMap<>();

          for (Map.Entry<String, JsonElement> groupEntry : groupValues.entrySet()) {
            String groupName = groupEntry.getKey();

            if (!knownGroupNames.contains(groupName) && warnedUnknownGroups.add(groupName)) {
              DietConstants.LOG.warn(
                  "Unknown diet group '{}' referenced in food_values file {}; ignoring",
                  groupName, resourcelocation);
            }
            parsed.put(groupName, groupEntry.getValue().getAsFloat());
          }

          if (key.startsWith("#")) {
            ResourceLocation tagId = new ResourceLocation(key.substring(1));
            TagKey<Item> tagKey = TagKey.create(Registries.ITEM, tagId);
            tags.add(new TagEntry(tagKey, parsed));
          } else {
            ResourceLocation itemId = new ResourceLocation(key);
            Services.REGISTRY.getItem(itemId)
                .ifPresentOrElse(item -> items.put(item, parsed),
                    () -> DietConstants.LOG.warn(
                        "Unknown item '{}' referenced in food_values file {}; ignoring",
                        itemId, resourcelocation));
          }
        }
      } catch (IllegalArgumentException | JsonParseException e) {
        DietConstants.LOG.error("Parsing error loading diet food values {}", resourcelocation, e);
      }
    }
    this.itemEntries = items;
    this.tagEntries = tags;
    DietConstants.LOG.info("Loaded {} diet food value entries ({} item, {} tag)",
        items.size() + tags.size(), items.size(), tags.size());
  }

  private record TagEntry(TagKey<Item> tag, Map<String, Float> values) {
  }
}
