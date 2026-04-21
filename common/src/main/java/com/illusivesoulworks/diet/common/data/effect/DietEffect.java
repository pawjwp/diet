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

import com.illusivesoulworks.diet.DietConstants;
import com.illusivesoulworks.diet.api.type.IDietAttribute;
import com.illusivesoulworks.diet.api.type.IDietCondition;
import com.illusivesoulworks.diet.api.type.IDietEffect;
import com.illusivesoulworks.diet.api.type.IDietStatusEffect;
import com.illusivesoulworks.diet.platform.Services;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;

public class DietEffect implements IDietEffect {

  public final List<IDietAttribute> attributes;
  public final List<IDietStatusEffect> statusEffects;
  public final List<IDietCondition> conditions;
  public final UUID uuid;

  public DietEffect(UUID uuid, List<IDietAttribute> attributes,
                    List<IDietStatusEffect> statusEffects, List<IDietCondition> conditions) {
    this.attributes = attributes;
    this.statusEffects = statusEffects;
    this.conditions = conditions;
    this.uuid = uuid;
  }

  @Override
  public List<IDietCondition> getConditions() {
    return this.conditions;
  }

  @Override
  public List<IDietAttribute> getAttributes() {
    return this.attributes;
  }

  @Override
  public List<IDietStatusEffect> getStatusEffects() {
    return this.statusEffects;
  }

  @Override
  public UUID getUuid() {
    return this.uuid;
  }

  @Override
  public CompoundTag save() {
    CompoundTag tag = new CompoundTag();
    tag.putUUID("UUID", this.uuid);
    tag.putInt("Quality", this.quality);
    ListTag attributesList = new ListTag();

    for (IDietAttribute attribute : this.attributes) {
      attributesList.add(attribute.save());
    }
    tag.put("Attributes", attributesList);
    ListTag statusList = new ListTag();

    for (IDietStatusEffect statusEffect : this.statusEffects) {
      statusList.add(statusEffect.save());
    }
    tag.put("StatusEffects", statusList);
    ListTag conditionsList = new ListTag();

    for (IDietCondition condition : this.conditions) {
      conditionsList.add(condition.save());
    }
    tag.put("Conditions", conditionsList);
    return tag;
  }

  public static DietEffect load(CompoundTag tag) {
    UUID uuid = tag.getUUID("UUID");
    int quality = tag.getInt("Quality");
    List<IDietAttribute> attributes = new ArrayList<>();
    ListTag attributesList = tag.getList("Attributes", Tag.TAG_COMPOUND);

    for (int i = 0; i < attributesList.size(); i++) {
      DietAttribute attribute = DietAttribute.load(attributesList.getCompound(i));

      if (attribute != null) {
        attributes.add(attribute);
      }
    }
    List<IDietStatusEffect> statusEffects = new ArrayList<>();
    ListTag statusList = tag.getList("StatusEffects", Tag.TAG_COMPOUND);

    for (int i = 0; i < statusList.size(); i++) {
      DietStatusEffect statusEffect = DietStatusEffect.load(statusList.getCompound(i));

      if (statusEffect != null) {
        statusEffects.add(statusEffect);
      }
    }
    List<IDietCondition> conditions = new ArrayList<>();
    ListTag conditionsList = tag.getList("Conditions", Tag.TAG_COMPOUND);

    for (int i = 0; i < conditionsList.size(); i++) {
      conditions.add(DietCondition.load(conditionsList.getCompound(i)));
    }
    return new DietEffect(uuid, attributes, statusEffects, conditions, quality);
  }

  public static class DietAttribute implements IDietAttribute {

    public final Attribute attribute;
    public final AttributeModifier.Operation operation;
    public final double amount;
    public final double increment;

    public DietAttribute(Attribute attribute, AttributeModifier.Operation operation,
                         double amount) {
      this(attribute, operation, amount, amount);
    }

    public DietAttribute(Attribute attribute, AttributeModifier.Operation operation,
                         double amount, double increment) {
      this.attribute = attribute;
      this.operation = operation;
      this.amount = amount;
      this.increment = increment;
    }

    @Override
    public Attribute getAttribute() {
      return this.attribute;
    }

    @Override
    public AttributeModifier.Operation getOperation() {
      return this.operation;
    }

    @Override
    public double getBaseAmount() {
      return this.amount;
    }

    @Override
    public double getIncrement() {
      return this.increment;
    }

    @Override
    public CompoundTag save() {
      CompoundTag tag = new CompoundTag();
      tag.putString("Name",
          Objects.requireNonNull(Services.REGISTRY.getAttributeKey(this.attribute)).toString());
      tag.putInt("Op", this.operation.toValue());
      tag.putDouble("Amount", this.amount);
      tag.putDouble("Increment", this.increment);
      return tag;
    }

    public static DietAttribute load(CompoundTag tag) {
      Attribute attribute = Services.REGISTRY
          .getAttribute(new ResourceLocation(tag.getString("Name"))).orElse(null);

      if (attribute == null) {
        return null;
      }
      AttributeModifier.Operation op = AttributeModifier.Operation.fromValue(tag.getInt("Op"));
      return new DietAttribute(attribute, op, tag.getDouble("Amount"), tag.getDouble("Increment"));
    }
  }

  public static class DietStatusEffect implements IDietStatusEffect {

    public final MobEffect effect;
    public final int power;
    public final int increment;

    public DietStatusEffect(MobEffect effect, int power) {
      this(effect, power, power);
    }

    public DietStatusEffect(MobEffect effect, int power, int increment) {
      this.effect = effect;
      this.power = power;
      this.increment = increment;
    }

    @Override
    public MobEffect getEffect() {
      return this.effect;
    }

    @Override
    public int getBasePower() {
      return this.power;
    }

    @Override
    public int getIncrement() {
      return this.increment;
    }

    @Override
    public CompoundTag save() {
      CompoundTag tag = new CompoundTag();
      tag.putString("Name",
          Objects.requireNonNull(Services.REGISTRY.getStatusEffectKey(this.effect)).toString());
      tag.putInt("Power", this.power);
      tag.putInt("Increment", this.increment);
      return tag;
    }

    public static DietStatusEffect load(CompoundTag tag) {
      MobEffect effect = Services.REGISTRY
          .getStatusEffect(new ResourceLocation(tag.getString("Name"))).orElse(null);

      if (effect == null) {
        return null;
      }
      return new DietStatusEffect(effect, tag.getInt("Power"), tag.getInt("Increment"));
    }
  }

  public static class DietCondition implements IDietCondition {

    public final Set<String> groups;
    public final MatchMethod match;
    public final double above;
    public final double below;

    public DietCondition(Set<String> groups, MatchMethod match, double above, double below) {
      this.groups = groups;
      this.match = match;
      this.above = above;
      this.below = below;
    }

    @Override
    public MatchMethod getMatchMethod() {
      return this.match;
    }

    public int getMatches(Player player, Map<String, Float> values) {
      return this.match.getMatches(this.groups, values, (float) this.above, (float) this.below);
    }

    @Override
    public Set<String> getGroups() {
      return this.groups;
    }

    @Override
    public double getAbove() {
      return this.above;
    }

    @Override
    public double getBelow() {
      return this.below;
    }

    @Override
    public CompoundTag save() {
      CompoundTag tag = new CompoundTag();
      ListTag groupList = new ListTag();

      for (String group : this.groups) {
        groupList.add(StringTag.valueOf(group));
      }
      tag.put("Groups", groupList);
      tag.putString("Match", this.match.name());
      tag.putDouble("Above", this.above);
      tag.putDouble("Below", this.below);
      return tag;
    }

    public static DietCondition load(CompoundTag tag) {
      Set<String> groups = new HashSet<>();
      ListTag groupList = tag.getList("Groups", Tag.TAG_STRING);

      for (int i = 0; i < groupList.size(); i++) {
        groups.add(groupList.getString(i));
      }
      MatchMethod match = MatchMethod.findOrDefault(tag.getString("Match"), MatchMethod.ANY);
      return new DietCondition(groups, match, tag.getDouble("Above"), tag.getDouble("Below"));
    }
  }

  public enum MatchMethod {
    EVERY {
      @Override
      int getMatches(Set<String> groups, Map<String, Float> values, float above, float below) {
        int count = 0;

        for (String group : groups) {
          Float value = values.get(group);

          if (value != null && MatchMethod.inRange(value, above, below)) {
            count++;
          }
        }
        return count;
      }
    },
    ANY {
      @Override
      int getMatches(Set<String> groups, Map<String, Float> values, float above, float below) {

        for (String group : groups) {
          Float value = values.get(group);

          if (value != null && MatchMethod.inRange(value, above, below)) {
            return 1;
          }
        }
        return 0;
      }
    },
    AVERAGE {
      @Override
      int getMatches(Set<String> groups, Map<String, Float> values, float above, float below) {
        float sum = 0;

        for (String group : groups) {
          Float value = values.get(group);

          if (value != null) {
            sum += value;
          }
        }
        return MatchMethod.inRange(sum / (float) groups.size(), above, below) ? 1 : 0;
      }
    },
    ALL {
      @Override
      int getMatches(Set<String> groups, Map<String, Float> values, float above, float below) {

        for (String group : groups) {
          Float value = values.get(group);

          if (value == null || !MatchMethod.inRange(value, above, below)) {
            return 0;
          }
        }
        return 1;
      }
    },
    NONE {
      @Override
      int getMatches(Set<String> groups, Map<String, Float> values, float above, float below) {

        for (String group : groups) {
          Float value = values.get(group);

          if (value != null && MatchMethod.inRange(value, above, below)) {
            return 0;
          }
        }
        return 1;
      }
    };

    abstract int getMatches(Set<String> groups, Map<String, Float> values, float above,
                            float below);

    public static MatchMethod findOrDefault(String val, MatchMethod def) {
      try {
        return MatchMethod.valueOf(val.toUpperCase(Locale.ROOT));
      } catch (IllegalArgumentException e) {
        DietConstants.LOG.error("No such match method " + val);
      }
      return def;
    }

    private static boolean inRange(float value, float above, float below) {
      return value >= above && value <= below;
    }
  }
}
