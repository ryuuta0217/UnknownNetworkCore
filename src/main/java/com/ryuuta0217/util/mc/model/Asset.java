/*
 * Copyright (C) 2023 Ryuta Iwakura (ryuuta0217)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.ryuuta0217.util.mc.model;

import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.util.Map;

public record Asset(String hash, int size) {
    public Asset(Map<String, Object> tree) {
        this((String) tree.get("hash"), ((Double) tree.get("size")).intValue());
    }

    public String url() {
        return "https://resources.download.minecraft.net/" + hash.substring(0, 2) + "/" + hash;
    }

    public InputStream content() {
        try {
            return new URL(this.url()).openStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return InputStream.nullInputStream();
    }
}
