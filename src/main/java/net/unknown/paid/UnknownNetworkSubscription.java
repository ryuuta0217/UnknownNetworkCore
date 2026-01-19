/*
 * Copyright (c) 2023 Unknown Network Developers and contributors.
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

package net.unknown.paid;

import net.unknown.shared.SharedConstants;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class UnknownNetworkSubscription {
    private static final File DATA_FOLDER = new File(SharedConstants.DATA_FOLDER, "active_subscription");
    private static final Logger LOGGER = LoggerFactory.getLogger("UNC/Subscription");
    private static final Map<UUID, SubscriptionInfo> PLAYER_ACTIVE_SUBSCRIPTION = new HashMap<>();

    public static synchronized void loadActiveSubscription(UUID playerUniqueId) {
        LOGGER.info("Loading active subscription for player {}", playerUniqueId);
        File playerActiveSubscriptionsFile = new File(DATA_FOLDER, playerUniqueId.toString() + ".json");
        JSONObject json;
        try {
            json = new JSONObject(String.join("\n", Files.readAllLines(playerActiveSubscriptionsFile.toPath())));
        } catch(IOException e) {
            json = new JSONObject();
        }

        SubscriptionInfo loadedInfo = SubscriptionInfo.read(json);
        if (!loadedInfo.equals(SubscriptionInfo.NO_SUBSCRIPTION)) {
            LOGGER.info("Loaded active subscription for player {} with type {}, since {}, expires in {}", playerUniqueId, loadedInfo.getType(), loadedInfo.getSince(), loadedInfo.getUntil());
            PLAYER_ACTIVE_SUBSCRIPTION.put(playerUniqueId, loadedInfo);
        } else {
            LOGGER.info("No active subscription found for player {}", playerUniqueId);
        }
    }

    public static synchronized void writeActiveSubscription(UUID playerUniqueId, boolean unload) {
        LOGGER.info("Writing active subscription for player {}", playerUniqueId);
        File playerActiveSubscriptionsFile = new File(DATA_FOLDER, playerUniqueId.toString() + ".json");
        SubscriptionInfo activeSubscriptionInfo = PLAYER_ACTIVE_SUBSCRIPTION.getOrDefault(playerUniqueId, SubscriptionInfo.NO_SUBSCRIPTION);
        try {
            if (DATA_FOLDER.exists() || DATA_FOLDER.mkdirs()) {
                Files.writeString(playerActiveSubscriptionsFile.toPath(), activeSubscriptionInfo.toJSON().toString(), StandardOpenOption.CREATE);
            } else {
                throw new IOException("Failed to create data folder at " + DATA_FOLDER.getAbsolutePath());
            }
            LOGGER.info("Successfully wrote active subscription for player {}", playerUniqueId);
        } catch (IOException e) {
            LOGGER.error("Failed to write active subscription for player {}", playerUniqueId, e);
        }

        if (unload) {
            unloadActiveSubscription(playerUniqueId);
        }
    }

    public static synchronized void unloadActiveSubscription(UUID playerUniqueId) {
        PLAYER_ACTIVE_SUBSCRIPTION.remove(playerUniqueId);
    }

    public static SubscriptionInfo getActiveSubscriptionInfo(UUID playerUniqueId) {
        return PLAYER_ACTIVE_SUBSCRIPTION.getOrDefault(playerUniqueId, SubscriptionInfo.NO_SUBSCRIPTION);
    }
}
