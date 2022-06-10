/*
 * Copyright (c) 2022 Unknown Network Developers and contributors.
 *
 * All rights reserved.
 *
 * NOTICE: This license is subject to change without prior notice.
 *
 * Redistribution and use in source and binary forms, *without modification*,
 *     are permitted provided that the following conditions are met:
 *
 * I. Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *
 * II. Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 * III. Neither the name of Unknown Network nor the names of its contributors may be used to
 *     endorse or promote products derived from this software without specific prior written permission.
 *
 * IV. This source code and binaries is provided by the copyright holders and contributors "AS-IS" and
 *     any express or implied warranties, including, but not limited to, the implied warranties of
 *     merchantability and fitness for a particular purpose are disclaimed.
 *     In not event shall the copyright owner or contributors be liable for
 *     any direct, indirect, incidental, special, exemplary, or consequential damages
 *     (including but not limited to procurement of substitute goods or services;
 *     loss of use data or profits; or business interruption) however caused and on any theory of liability,
 *     whether in contract, strict liability, or tort (including negligence or otherwise)
 *     arising in any way out of the use of this source code, event if advised of the possibility of such damage.
 */

package net.unknown.survival.antivillagerlag.util;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.entity.schedule.Schedule;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftVillager;
import org.bukkit.entity.Villager;
import org.bukkit.entity.memory.MemoryKey;

import javax.annotation.Nonnull;
import java.util.Set;

public class ActivityUtils {
    public static void setActivitiesNormal(Villager villager) {
        clearActivities(villager);
        addActivities(villager, Activity.CORE);
        addActivities(villager, getCurrentActivity(villager));
    }

    public static void setActivitiesEmpty(Villager villager) {
        clearActivities(villager);
    }

    public static void setScheduleNormal(Villager villager) {
        Schedule schedule;
        if (villager.isAdult()) {
            if (villager.getProfession() == Villager.Profession.NITWIT) schedule = Schedule.SIMPLE;
            else schedule = Schedule.VILLAGER_DEFAULT;
        } else {
            schedule = Schedule.VILLAGER_BABY;
        }

        getBehaviorController(getNMSVillager(villager)).setSchedule(schedule);
    }

    public static void setScheduleEmpty(Villager villager) {
        getBehaviorController(getNMSVillager(villager)).setSchedule(Schedule.EMPTY);
    }

    public static boolean badCurrentActivity(Villager villager) {
        Schedule currentSchedule = getSchedule(villager);
        Activity currentActivity = getCurrentActivityFor(villager, currentSchedule);
        return badActivity(currentActivity, villager);
    }

    public static boolean wouldBeBadActivity(Villager bukkitVillager) {
        Schedule schedule;
        if (bukkitVillager.isAdult()) {
            if (bukkitVillager.getProfession() == Villager.Profession.NITWIT) schedule = Schedule.SIMPLE;
            else schedule = Schedule.VILLAGER_DEFAULT;
        } else {
            schedule = Schedule.VILLAGER_BABY;
        }

        return badActivity(getCurrentActivityFor(bukkitVillager, schedule), bukkitVillager);
    }

    private static boolean badActivity(Activity nmsActivity, Villager bukkitVillager) {
        if (nmsActivity == Activity.REST) {
            return bukkitVillager.getMemory(MemoryKey.HOME) == null || isPlaceholderMemory(bukkitVillager, MemoryKey.HOME);
        }

        if (nmsActivity == Activity.WORK) {
            return isPlaceholderMemory(bukkitVillager, MemoryKey.JOB_SITE);
        }

        if (nmsActivity == Activity.MEET) {
            return bukkitVillager.getMemory(MemoryKey.MEETING_POINT) == null || isPlaceholderMemory(bukkitVillager, MemoryKey.MEETING_POINT);
        }

        return false;
    }

    public static void replaceBadMemories(Villager bukkitVillager) {
        Location loc = bukkitVillager.getLocation();
        loc.setY(-10000);

        if (bukkitVillager.getMemory(MemoryKey.HOME) == null) {
            bukkitVillager.setMemory(MemoryKey.HOME, loc);
        }

        if (bukkitVillager.getMemory(MemoryKey.MEETING_POINT) == null) {
            bukkitVillager.setMemory(MemoryKey.MEETING_POINT, loc);
        }
    }

    public static boolean isPlaceholderMemory(Villager bukkitVillager, MemoryKey<Location> memoryKey) {
        Location memoryLoc = bukkitVillager.getMemory(memoryKey);
        return memoryLoc != null && memoryLoc.getY() < 0;
    }

    public static void clearPlaceholderMemories(Villager bukkitVillager) {
        if (bukkitVillager.getMemory(MemoryKey.HOME) != null && isPlaceholderMemory(bukkitVillager, MemoryKey.HOME)) {
            bukkitVillager.setMemory(MemoryKey.HOME, null);
        }

        if (bukkitVillager.getMemory(MemoryKey.JOB_SITE) != null && isPlaceholderMemory(bukkitVillager, MemoryKey.JOB_SITE)) {
            bukkitVillager.setMemory(MemoryKey.JOB_SITE, null);
        }

        if (bukkitVillager.getMemory(MemoryKey.MEETING_POINT) != null && isPlaceholderMemory(bukkitVillager, MemoryKey.MEETING_POINT)) {
            bukkitVillager.setMemory(MemoryKey.MEETING_POINT, null);
        }
    }

    public static boolean isScheduleNormal(Villager bukkitVillager) {
        Schedule schedule;
        if (bukkitVillager.isAdult()) {
            if (bukkitVillager.getProfession() == Villager.Profession.NITWIT) schedule = Schedule.SIMPLE;
            else schedule = Schedule.VILLAGER_DEFAULT;
        } else {
            schedule = Schedule.VILLAGER_BABY;
        }

        return getSchedule(bukkitVillager) == schedule;
    }

    private static Activity getCurrentActivity(Villager bukkitVillager) {
        return getSchedule(bukkitVillager).getActivityAt((int) bukkitVillager.getWorld().getTime());
    }

    private static Activity getCurrentActivityFor(Villager bukkitVillager, Schedule nmsSchedule) {
        return nmsSchedule.getActivityAt((int) bukkitVillager.getWorld().getTime());
    }

    private static void addActivities(Villager bukkitVillager, Activity nmsActivity) {
        getActivities(getBehaviorController(getNMSVillager(bukkitVillager))).add(nmsActivity);
    }

    private static void clearActivities(Villager bukkitVillager) {
        getActivities(getBehaviorController(getNMSVillager(bukkitVillager))).clear();
    }

    private static Schedule getSchedule(Villager bukkitVillager) {
        return getBehaviorController(getNMSVillager(bukkitVillager)).getSchedule();
    }

    private static <T extends LivingEntity> Brain<T> getBehaviorController(T entity) {
        return (Brain<T>) entity.getBrain();
    }

    private static net.minecraft.world.entity.npc.Villager getNMSVillager(Villager bukkitVillager) {
        return ((CraftVillager) bukkitVillager).getHandle();
    }

    @Nonnull
    private static Set<Activity> getActivities(Brain<net.minecraft.world.entity.npc.Villager> behaviorController) {
        return behaviorController.getActiveActivities();
    }
}
