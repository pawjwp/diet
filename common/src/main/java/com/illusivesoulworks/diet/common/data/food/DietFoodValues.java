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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
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

    // An individual item's entry takes priority over that same item as part of a tag
    if (raw == null) {

      for (TagEntry entry : tagEntries) {

        if (stack.is(entry.tag)) {
          raw = entry.values;
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
    Map<String, FileValues> filesByName = new LinkedHashMap<>();
    Set<String> warnedUnknownGroups = new HashSet<>();
    Set<String> knownGroupNames = new HashSet<>();

    for (IDietGroup group : DietGroups.SERVER.getGroups()) {
      knownGroupNames.add(group.getName());
    }
    
    // Load diet namespace food data files first, then all others in sorted order
    List<Map.Entry<ResourceLocation, JsonElement>> orderedFiles = new ArrayList<>();
    List<Map.Entry<ResourceLocation, JsonElement>> otherFiles = new ArrayList<>();

    for (Map.Entry<ResourceLocation, JsonElement> entry : new TreeMap<>(object).entrySet()) {

      if (entry.getKey().getNamespace().equals(DietConstants.MOD_ID)) {
        orderedFiles.add(entry);
      } else {
        otherFiles.add(entry);
      }
    }
    orderedFiles.addAll(otherFiles);

    for (Map.Entry<ResourceLocation, JsonElement> entry : orderedFiles) {
      ResourceLocation resourcelocation = entry.getKey();

      try {
        JsonObject top = GsonHelper.convertToJsonObject(entry.getValue(), "top element");
        // Files with the same name share their values, which move to the end of the load order
        // so that later files override earlier ones. Replace discards the earlier files' values.
        FileValues earlier = filesByName.remove(resourcelocation.getPath());
        FileValues file = earlier == null || GsonHelper.getAsBoolean(top, "replace", false)
            ? new FileValues(new HashMap<>(), new ArrayList<>()) : earlier;
        filesByName.put(resourcelocation.getPath(), file);
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
            file.tags.add(new TagEntry(tagKey, parsed));
          } else {
            ResourceLocation itemId = new ResourceLocation(key);
            Services.REGISTRY.getItem(itemId)
                .ifPresentOrElse(item -> file.items.put(item, parsed),
                    () -> DietConstants.LOG.warn(
                        "Unknown item '{}' referenced in food_values file {}; ignoring",
                        itemId, resourcelocation));
          }
        }
      } catch (IllegalArgumentException | JsonParseException e) {
        DietConstants.LOG.error("Parsing error loading diet food values {}", resourcelocation, e);
      }
    }
    Map<Item, Map<String, Float>> items = new HashMap<>();
    List<TagEntry> tags = new ArrayList<>();

    for (FileValues file : filesByName.values()) {
      items.putAll(file.items);
      tags.addAll(file.tags);
    }
    this.itemEntries = items;
    this.tagEntries = tags;
    DietConstants.LOG.info("Loaded {} diet food value entries ({} item, {} tag)",
        items.size() + tags.size(), items.size(), tags.size());
  }

  private record TagEntry(TagKey<Item> tag, Map<String, Float> values) {
  }

  private record FileValues(Map<Item, Map<String, Float>> items, List<TagEntry> tags) {
  }
}
