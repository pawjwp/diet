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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.illusivesoulworks.diet.DietConstants;
import com.illusivesoulworks.diet.api.type.IDietGroup;
import com.illusivesoulworks.diet.common.data.group.DietGroups;
import com.illusivesoulworks.diet.platform.Services;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nonnull;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class DietFoodValues
    extends SimplePreparableReloadListener<List<DietFoodValues.FoodValueFile>> {

  private static final FileToIdConverter FILE_TO_ID = FileToIdConverter.json("diet/food_values");

  public static final DietFoodValues SERVER = Services.CAPABILITY.getFoodValuesListener();
  public static final DietFoodValues CLIENT = Services.CAPABILITY.getFoodValuesListener();

  private Map<Item, Map<String, Float>> itemEntries = new HashMap<>();
  private List<TagEntry> tagEntries = new ArrayList<>();

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

  // Reads every datapack's version of each food values file, based on vanilla's TagLoader
  // Versions of the same file are combined from the lowest priority datapack up, replace discards the lower priority versions
  @Nonnull
  @Override
  protected List<FoodValueFile> prepare(@Nonnull ResourceManager resourceManager,
                                        @Nonnull ProfilerFiller profilerFiller) {
    List<String> packOrder = resourceManager.listPacks().map(PackResources::packId).toList();
    Map<ResourceLocation, List<FoodValueFile>> versionsById = new LinkedHashMap<>();

    for (Map.Entry<ResourceLocation, List<Resource>> entry :
        FILE_TO_ID.listMatchingResourceStacks(resourceManager).entrySet()) {
      ResourceLocation id = FILE_TO_ID.fileToId(entry.getKey());

      for (Resource resource : entry.getValue()) {

        try (Reader reader = resource.openAsReader()) {
          JsonObject json = GsonHelper.parse(reader);
          List<FoodValueEntry> entries = new ArrayList<>();

          for (Map.Entry<String, JsonElement> valueEntry :
              GsonHelper.getAsJsonObject(json, "values", new JsonObject()).entrySet()) {
            String key = valueEntry.getKey();
            Map<String, Float> values = new HashMap<>();

            for (Map.Entry<String, JsonElement> groupEntry :
                GsonHelper.convertToJsonObject(valueEntry.getValue(), key).entrySet()) {
              values.put(groupEntry.getKey(),
                  GsonHelper.convertToFloat(groupEntry.getValue(), groupEntry.getKey()));
            }
            boolean tag = key.startsWith("#");
            entries.add(
                new FoodValueEntry(new ResourceLocation(tag ? key.substring(1) : key), tag, values));
          }
          List<FoodValueFile> versions = versionsById.computeIfAbsent(id, k -> new ArrayList<>());

          if (GsonHelper.getAsBoolean(json, "replace", false)) {
            versions.clear();
          }
          versions.add(new FoodValueFile(id, packOrder.indexOf(resource.sourcePackId()), entries));
        } catch (Exception e) {
          DietConstants.LOG.error("Couldn't read diet food values {} from {} in data pack {}", id,
              entry.getKey(), resource.sourcePackId(), e);
        }
      }
    }
    // Later files override earlier ones, the highest priority is applied last
    return versionsById.values().stream().flatMap(List::stream)
        .sorted(Comparator.comparingInt(FoodValueFile::packIndex)).toList();
  }

  @Override
  protected void apply(@Nonnull List<FoodValueFile> files, @Nonnull ResourceManager resourceManager, @Nonnull ProfilerFiller profilerFiller) {
    Map<Item, Map<String, Float>> items = new HashMap<>();
    List<TagEntry> tags = new ArrayList<>();
    Set<String> warnedUnknownGroups = new HashSet<>();
    Set<String> knownGroupNames = new HashSet<>();

    for (IDietGroup group : DietGroups.SERVER.getGroups()) {
      knownGroupNames.add(group.getName());
    }

    for (FoodValueFile file : files) {

      for (FoodValueEntry entry : file.entries) {

        for (String groupName : entry.values.keySet()) {
          
          if (!knownGroupNames.contains(groupName) && warnedUnknownGroups.add(groupName)) {
            DietConstants.LOG.warn(
                "Unknown diet group '{}' referenced in food_values file {}; ignoring", groupName, file.id
            );
          }
        }

        if (entry.tag) {
          tags.add(new TagEntry(TagKey.create(Registries.ITEM, entry.id), entry.values));
        } else {
          Services.REGISTRY.getItem(entry.id)
              .ifPresentOrElse(item -> items.put(item, entry.values),
                  () -> DietConstants.LOG.warn(
                      "Unknown item '{}' referenced in food_values file {}; ignoring",
                      entry.id, file.id)
              );
        }
      }
    }

    this.itemEntries = items;
    this.tagEntries = tags;
    DietConstants.LOG.info("Loaded {} diet food value entries ({} item, {} tag)",
        items.size() + tags.size(), items.size(), tags.size()
    );
  }

  record FoodValueFile(ResourceLocation id, int packIndex, List<FoodValueEntry> entries) {
  }

  // One key in a file's values, which is either an item or, when tag is true, an item tag
  record FoodValueEntry(ResourceLocation id, boolean tag, Map<String, Float> values) {
  }

  private record TagEntry(TagKey<Item> tag, Map<String, Float> values) {
  }
}
