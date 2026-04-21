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

package com.illusivesoulworks.diet.common.data.suite;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.illusivesoulworks.diet.api.type.IDietCondition;
import com.illusivesoulworks.diet.api.type.IDietEffect;
import com.illusivesoulworks.diet.api.type.IDietGroup;
import com.illusivesoulworks.diet.api.type.IDietSuite;
import com.illusivesoulworks.diet.api.type.QualitySegment;
import com.illusivesoulworks.diet.common.data.effect.DietEffect;
import com.illusivesoulworks.diet.common.data.group.DietGroup;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

public final class DietSuite implements IDietSuite {

  private final String name;
  private final Set<IDietGroup> groups;
  private final List<IDietEffect> effects;
  private final Map<String, List<QualitySegment>> qualitySegments;

  private DietSuite(String name, Set<IDietGroup> groups, List<IDietEffect> effects,
                    Map<String, List<QualitySegment>> qualitySegments) {
    this.name = name;
    TreeSet<IDietGroup> sorted = new TreeSet<>(
        Comparator.comparing(IDietGroup::getOrder).thenComparing(IDietGroup::getName));
    sorted.addAll(groups);
    this.groups = ImmutableSet.copyOf(sorted);
    this.effects = ImmutableList.copyOf(effects);
    this.qualitySegments = ImmutableMap.copyOf(qualitySegments);
  }

  public static IDietSuite load(CompoundTag tag) {
    Set<IDietGroup> set = new HashSet<>();
    CompoundTag groups = (CompoundTag) tag.get("Groups");

    if (groups != null) {

      for (String key : groups.getAllKeys()) {
        set.add(DietGroup.load((CompoundTag) Objects.requireNonNull(groups.get(key))));
      }
    }
    Map<String, List<QualitySegment>> segments = new HashMap<>();
    CompoundTag segmentsTag = (CompoundTag) tag.get("QualitySegments");

    if (segmentsTag != null) {

      for (String groupName : segmentsTag.getAllKeys()) {
        ListTag list = segmentsTag.getList(groupName, Tag.TAG_COMPOUND);
        List<QualitySegment> groupSegments = new ArrayList<>();

        for (int i = 0; i < list.size(); i++) {
          CompoundTag segTag = list.getCompound(i);
          groupSegments.add(new QualitySegment(
              segTag.getFloat("S"), segTag.getFloat("E"), segTag.getInt("Q")));
        }
        segments.put(groupName, groupSegments);
      }
    }
    List<IDietEffect> effects = new ArrayList<>();
    ListTag effectsList = tag.getList("Effects", Tag.TAG_COMPOUND);

    for (int i = 0; i < effectsList.size(); i++) {
      effects.add(DietEffect.load(effectsList.getCompound(i)));
    }
    return new DietSuite(tag.getString("Name"), set, effects, segments);
  }

  @Override
  public String getName() {
    return this.name;
  }

  @Override
  public Set<IDietGroup> getGroups() {
    return this.groups;
  }

  @Override
  public List<IDietEffect> getEffects() {
    return this.effects;
  }

  @Override
  public List<QualitySegment> getQualitySegments(String groupName) {
    return this.qualitySegments.getOrDefault(groupName, List.of());
  }

  @Override
  public CompoundTag save() {
    CompoundTag tag = new CompoundTag();
    tag.putString("Name", this.name);
    CompoundTag groups = new CompoundTag();

    for (IDietGroup group : this.groups) {
      groups.put(group.getName(), group.save());
    }
    tag.put("Groups", groups);
    CompoundTag segmentsTag = new CompoundTag();

    for (Map.Entry<String, List<QualitySegment>> entry : this.qualitySegments.entrySet()) {
      ListTag list = new ListTag();

      for (QualitySegment seg : entry.getValue()) {
        CompoundTag segTag = new CompoundTag();
        segTag.putFloat("S", seg.start());
        segTag.putFloat("E", seg.end());
        segTag.putInt("Q", seg.quality());
        list.add(segTag);
      }
      segmentsTag.put(entry.getKey(), list);
    }
    tag.put("QualitySegments", segmentsTag);
    ListTag effectsList = new ListTag();

    for (IDietEffect effect : this.effects) {
      effectsList.add(effect.save());
    }
    tag.put("Effects", effectsList);
    return tag;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DietSuite dietSuite = (DietSuite) o;
    return Objects.equals(name, dietSuite.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name);
  }

  public static class Builder {

    private final String name;
    private final Set<IDietGroup> groups;
    private final List<IDietEffect> effects;

    public Builder(String name) {
      this.name = name;
      this.groups = new TreeSet<>(
          Comparator.comparingInt(IDietGroup::getOrder).thenComparing(IDietGroup::getName));
      this.effects = new ArrayList<>();
    }

    public Builder group(IDietGroup group) {
      this.groups.add(group);
      return this;
    }

    public Builder groups(Set<IDietGroup> groups) {
      this.groups.addAll(groups);
      return this;
    }

    public Builder effect(IDietEffect effect) {
      this.effects.add(effect);
      return this;
    }

    public Builder effects(List<IDietEffect> effects) {
      this.effects.addAll(effects);
      return this;
    }

    public Builder clear() {
      this.groups.clear();
      this.effects.clear();
      return this;
    }

    public IDietSuite build() {
      return new DietSuite(this.name, this.groups, this.effects,
          computeQualitySegments(this.groups, this.effects));
    }
  }

  private static Map<String, List<QualitySegment>> computeQualitySegments(
      Set<IDietGroup> groups, List<IDietEffect> effects) {
    Map<String, List<QualitySegment>> result = new HashMap<>();

    for (IDietGroup group : groups) {
      List<QualitySegment> segments = computeSegmentsForGroup(group.getName(), effects);

      if (!segments.isEmpty()) {
        result.put(group.getName(), segments);
      }
    }
    return result;
  }

  private static List<QualitySegment> computeSegmentsForGroup(String groupName,
                                                              List<IDietEffect> effects) {
    List<ColoredRange> ranges = new ArrayList<>();

    for (IDietEffect effect : effects) {
      if (effect.getQuality() == 0xFFFFFF) {
        continue;
      }

      for (IDietCondition condition : effect.getConditions()) {
        DietEffect.DietCondition dc = (DietEffect.DietCondition) condition;

        if (dc.groups.contains(groupName)) {
          ranges.add(new ColoredRange((float) dc.above, (float) dc.below, effect.getQuality()));
        }
      }
    }

    if (ranges.isEmpty()) {
      return List.of();
    }
    TreeSet<Float> breakpoints = new TreeSet<>();

    for (ColoredRange range : ranges) {
      breakpoints.add(range.above());
      breakpoints.add(range.below());
    }
    List<QualitySegment> segments = new ArrayList<>();
    Float prev = null;

    for (Float bp : breakpoints) {

      if (prev != null) {
        float midpoint = (prev + bp) / 2.0f;
        int rSum = 0;
        int gSum = 0;
        int bSum = 0;
        int count = 0;

        for (ColoredRange range : ranges) {

          if (midpoint >= range.above() && midpoint <= range.below()) {
            rSum += (range.color() >> 16) & 0xFF;
            gSum += (range.color() >> 8) & 0xFF;
            bSum += range.color() & 0xFF;
            count++;
          }
        }

        if (count > 0) {
          int avg = ((rSum / count) << 16) | ((gSum / count) << 8) | (bSum / count);

          if (avg != 0xFFFFFF) {
            segments.add(new QualitySegment(prev, bp, avg));
          }
        }
      }
      prev = bp;
    }
    return segments;
  }

  private record ColoredRange(float above, float below, int color) {
  }
}
