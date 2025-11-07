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

package net.unknown.survival.economy.transaction;

import org.bukkit.configuration.ConfigurationSection;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class TransactionHistory {
    private static final Logger LOGGER = LoggerFactory.getLogger("UNC/TransactionHistory");

    private final File file;
    private JSONObject json;
    private final Map<Long, Transaction> histories;

    public TransactionHistory() {
        this(Collections.emptyMap());
    }

    public TransactionHistory(File file) throws IOException {
        this.file = file;
        this.histories = new HashMap<>();
        LOGGER.info("Start parse for file " + file);
        long start = System.nanoTime();
        this.json = new JSONObject(String.join("\n", Files.readAllLines(file.toPath())));
        this.json.keys().forEachRemaining(timestampStr -> {
            if (timestampStr.matches("\\d+")) {
                long timestamp = Long.parseLong(timestampStr);
                JSONObject transactionRaw = this.json.getJSONObject(timestampStr);
                if (transactionRaw != null) {
                    Transaction transaction = Transaction.load(transactionRaw);
                    if (transaction != null) {
                        this.histories.put(timestamp, transaction);
                    } else {
                        LOGGER.warn("Failed to parse transaction at timestamp " + timestamp);
                    }
                } else {
                    LOGGER.warn("Failed to retrieve transaction at timestamp " + timestamp);
                }
            } else {
                LOGGER.warn("Invalid key detected (Not a valid timestamp): " + timestampStr);
            }
        });
        long end = System.nanoTime();
        LOGGER.info("End parse for file " + file + ", elapsed " + ((end - start) / 1_000_000));
    }

    public TransactionHistory(Map<Long, Transaction> histories) {
        this.file = null;
        this.histories = new HashMap<>(histories);
    }

    public Map<Long, Transaction> getHistories() {
        return Collections.unmodifiableMap(this.histories);
    }

    public void add(long timestamp, Transaction transaction) {
        this.histories.put(timestamp, transaction);
    }

    @Nullable
    public Transaction remove(long timestamp) {
        return this.histories.remove(timestamp);
    }

    public void writeToFile() {
        if (this.file == null) throw new IllegalStateException("This instance is not initialized with file. If you want, use writeFile(File) to write to file.");
        else this.writeToFile(this.file);
    }

    public void writeToFile(File file) {
        this.histories.forEach((timestamp, transaction) -> {
            JSONObject transactionJson = new JSONObject();
            transaction.save(transactionJson);
            this.json.put(timestamp.toString(), transactionJson);
        });
        try {
            Files.writeString(file.toPath(), this.json.toString(2), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        } catch(IOException e) {
            LOGGER.warn("Failed to save to " + file, e);
        }
    }

    public void writeToConfigurationSection(ConfigurationSection section) {
        this.histories.forEach((timestamp, transaction) -> transaction.save(section.createSection(String.valueOf(timestamp))));
    }
}
