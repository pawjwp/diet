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

package com.illusivesoulworks.diet.client.screen;

import com.illusivesoulworks.diet.DietConstants;
import com.illusivesoulworks.diet.api.DietApi;
import com.illusivesoulworks.diet.api.type.IDietAttribute;
import com.illusivesoulworks.diet.api.type.IDietCondition;
import com.illusivesoulworks.diet.api.type.IDietEffect;
import com.illusivesoulworks.diet.api.type.IDietStatusEffect;
import com.illusivesoulworks.diet.api.type.IDietSuite;
import com.illusivesoulworks.diet.common.data.effect.DietEffect;
import com.illusivesoulworks.diet.common.data.effect.DietEffectsInfo;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class DietTooltip {

  public static List<Component> getEffects() {
    List<DietEffectsInfo.AttributeModifier> modifiers = DietScreen.tooltip.getModifiers();
    List<DietEffectsInfo.StatusEffect> effects = DietScreen.tooltip.getEffects();

    if (modifiers.isEmpty() && effects.isEmpty()) {
      return new ArrayList<>();
    }
    List<Component> tooltips = new ArrayList<>();
    tooltips.add(Component.translatable("tooltip.diet.effects"));
    tooltips.add(Component.empty());
    Map<Attribute, AttributeTooltip> mergedAttributes = new HashMap<>();

    for (DietEffectsInfo.AttributeModifier modifier : modifiers) {
      mergedAttributes.computeIfAbsent(modifier.getAttribute(), (k) -> new AttributeTooltip())
          .merge(modifier);
    }

    for (Map.Entry<Attribute, AttributeTooltip> attribute : mergedAttributes.entrySet()) {
      AttributeTooltip info = attribute.getValue();
      Attribute key = attribute.getKey();

      if (key == DietApi.getInstance().getNaturalRegeneration()) {
        float val = (info.added + info.added * info.baseMultiplier) * info.totalMultiplier;

        if (val < 1.0f) {
          tooltips.add(Component.translatable("attribute.diet.modifier.disabled",
              Component.translatable(key.getDescriptionId())).withStyle(ChatFormatting.RED));
        }
      } else {
        addAttributeTooltip(tooltips, info.added, AttributeModifier.Operation.ADDITION, key);
        addAttributeTooltip(tooltips, info.baseMultiplier,
            AttributeModifier.Operation.MULTIPLY_BASE, key);
        addAttributeTooltip(tooltips, info.totalMultiplier - 1.0f,
            AttributeModifier.Operation.MULTIPLY_TOTAL, key);
      }
    }
    Map<MobEffect, Integer> mergedEffects = new HashMap<>();

    for (DietEffectsInfo.StatusEffect effect : effects) {
      mergedEffects.compute(effect.getEffect(),
          (k, v) -> v == null ? effect.getAmplifier() : Math.max(v, effect.getAmplifier()));
    }

    for (Map.Entry<MobEffect, Integer> effect : mergedEffects.entrySet()) {
      tooltips.add(formatStatusEffect(effect.getKey(), effect.getValue()));
    }
    return tooltips;
  }

  public static List<Component> getEffectsForGroup(String groupName, IDietSuite suite,
                                                   Map<String, Float> values, Player player) {
    List<Component> lines = new ArrayList<>();

    for (IDietEffect effect : suite.getEffects()) {
      boolean allMatch = true;

      for (IDietCondition condition : effect.getConditions()) {
        if (condition.getMatches(player, values) == 0) {
          allMatch = false;
          break;
        }
      }

      if (!allMatch) {
        continue;
      }
      boolean everyStyle = false;

      for (IDietCondition condition : effect.getConditions()) {

        if (condition.getMatchMethod() == DietEffect.MatchMethod.EVERY) {
          everyStyle = true;
          break;
        }
      }

      if (everyStyle) {
        boolean qualifies = false;

        for (IDietCondition condition : effect.getConditions()) {

          if (condition.getMatchMethod() != DietEffect.MatchMethod.EVERY) {
            continue;
          }

          if (!condition.getGroups().contains(groupName)) {
            continue;
          }
          Float v = values.get(groupName);

          if (v != null && v >= condition.getAbove() && v <= condition.getBelow()) {
            qualifies = true;
            break;
          }
        }

        if (!qualifies) {
          continue;
        }
        emitEffectLines(lines, effect, null);
      } else {
        Set<String> others = new LinkedHashSet<>();
        boolean includes = false;

        for (IDietCondition condition : effect.getConditions()) {

          if (condition.getMatchMethod() == DietEffect.MatchMethod.NONE) {
            continue;
          }

          for (String g : condition.getGroups()) {

            if (g.equals(groupName)) {
              includes = true;
            } else {
              others.add(g);
            }
          }
        }

        if (!includes) {
          continue;
        }
        Component suffix = others.isEmpty() ? null : buildWithSuffix(others);
        emitEffectLines(lines, effect, suffix);
      }
    }

    if (lines.isEmpty()) {
      List<Component> out = new ArrayList<>();
      out.add(Component.translatable("tooltip.diet.no_active_effects")
          .withStyle(ChatFormatting.GRAY));
      return out;
    }
    List<Component> out = new ArrayList<>();
    out.add(Component.translatable("tooltip.diet.group_effects",
        Component.translatable("groups." + DietConstants.MOD_ID + "." + groupName + ".name")));
    out.add(Component.empty());
    out.addAll(lines);
    return out;
  }

  private static void emitEffectLines(List<Component> lines, IDietEffect effect, Component suffix) {

    for (IDietAttribute attribute : effect.getAttributes()) {
      MutableComponent line = formatAttributeLine(attribute.getAttribute(),
          attribute.getOperation(), (float) attribute.getBaseAmount());

      if (line == null) {
        continue;
      }

      if (suffix != null) {
        line.append(Component.literal(" ")).append(suffix);
      }
      lines.add(line);
    }

    for (IDietStatusEffect statusEffect : effect.getStatusEffects()) {
      MutableComponent line = formatStatusEffect(statusEffect.getEffect(),
          statusEffect.getBasePower());

      if (suffix != null) {
        line.append(Component.literal(" ")).append(suffix);
      }
      lines.add(line);
    }
  }

  private static MutableComponent formatStatusEffect(MobEffect effect, int amplifier) {
    MutableComponent name = Component.translatable(effect.getDescriptionId());

    if (amplifier > 0) {
      name = Component.translatable("potion.withAmplifier", name,
          Component.translatable("potion.potency." + amplifier));
    }
    return name.withStyle(effect.getCategory().getTooltipFormatting());
  }

  private static Component buildWithSuffix(Set<String> others) {
    MutableComponent joined = Component.empty();
    int i = 0;

    for (String g : others) {

      if (i > 0) {
        joined.append(Component.literal(", "));
      }
      joined.append(Component.translatable(
          "groups." + DietConstants.MOD_ID + "." + g + ".name"));
      i++;
    }
    return Component.translatable("tooltip.diet.with_groups", joined)
        .withStyle(ChatFormatting.GRAY);
  }

  private static MutableComponent formatAttributeLine(Attribute attribute,
                                                      AttributeModifier.Operation operation,
                                                      float amount) {
    double formattedAmount;

    if (operation != AttributeModifier.Operation.MULTIPLY_BASE &&
        operation != AttributeModifier.Operation.MULTIPLY_TOTAL) {

      if (attribute.equals(Attributes.KNOCKBACK_RESISTANCE)) {
        formattedAmount = amount * 10.0D;
      } else {
        formattedAmount = amount;
      }
    } else {
      formattedAmount = amount * 100.0D;
    }

    if (amount > 0.0D) {
      return Component.translatable("attribute.modifier.plus." + operation.toValue(),
          ItemStack.ATTRIBUTE_MODIFIER_FORMAT.format(formattedAmount),
          Component.translatable(attribute.getDescriptionId())).withStyle(ChatFormatting.BLUE);
    } else if (amount < 0.0D) {
      formattedAmount = formattedAmount * -1.0D;
      return Component.translatable("attribute.modifier.take." + operation.toValue(),
          ItemStack.ATTRIBUTE_MODIFIER_FORMAT.format(formattedAmount),
          Component.translatable(attribute.getDescriptionId())).withStyle(ChatFormatting.RED);
    }
    return null;
  }

  private static void addAttributeTooltip(List<Component> tooltips, float amount,
                                          AttributeModifier.Operation operation,
                                          Attribute attribute) {
    MutableComponent line = formatAttributeLine(attribute, operation, amount);

    if (line != null) {
      tooltips.add(line);
    }
  }

  private static class AttributeTooltip {

    float added = 0;
    float baseMultiplier = 0.0f;
    float totalMultiplier = 1.0f;

    void merge(DietEffectsInfo.AttributeModifier modifier) {
      float amount = modifier.getAmount();

      if (modifier.getOperation() == AttributeModifier.Operation.MULTIPLY_BASE) {
        baseMultiplier += amount;
      } else if (modifier.getOperation() == AttributeModifier.Operation.MULTIPLY_TOTAL) {
        totalMultiplier *= 1.0f + amount;
      } else {
        added += amount;
      }
    }
  }
}
