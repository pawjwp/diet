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

package com.illusivesoulworks.diet.api.type;

import java.util.Locale;

public enum NotificationTrigger {
  RISE_INTO,
  FALL_INTO,
  ENTER,
  RISE_OUT,
  FALL_OUT,
  EXIT,
  RISE_THROUGH,
  FALL_THROUGH,
  ALL;

  public boolean firesOnRiseInto() {
    return this == RISE_INTO || this == ENTER || this == ALL;
  }

  public boolean firesOnFallInto() {
    return this == FALL_INTO || this == ENTER || this == ALL;
  }

  public boolean firesOnRiseOut() {
    return this == RISE_OUT || this == EXIT || this == ALL;
  }

  public boolean firesOnFallOut() {
    return this == FALL_OUT || this == EXIT || this == ALL;
  }

  public boolean firesOnRiseThrough() {
    return this == RISE_THROUGH || this == ALL;
  }

  public boolean firesOnFallThrough() {
    return this == FALL_THROUGH || this == ALL;
  }

  public static NotificationTrigger findOrDefault(String val, NotificationTrigger def) {
    if (val == null || val.isEmpty()) {
      return def;
    }
    try {
      return NotificationTrigger.valueOf(val.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      return def;
    }
  }
}
