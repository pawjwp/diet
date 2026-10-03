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

package com.illusivesoulworks.diet.common.network.server;

import com.illusivesoulworks.diet.client.DietClientPacketReceiver;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

public record SPacketFoodValues(CompoundTag foodValues) {

  public static void encode(SPacketFoodValues msg, FriendlyByteBuf buf) {
    buf.writeNbt(msg.foodValues());
  }

  public static SPacketFoodValues decode(FriendlyByteBuf buf) {
    return new SPacketFoodValues(buf.readNbt());
  }

  public static void handle(SPacketFoodValues msg) {
    DietClientPacketReceiver.handleFoodValues(msg.foodValues());
  }
}
