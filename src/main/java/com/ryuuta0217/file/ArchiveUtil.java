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

package com.ryuuta0217.file;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.zstandard.ZstdCompressorOutputStream;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ArchiveUtil {
    public static void createArchiveWithZstd(List<File> files, File archive, @Nullable File baseDir) throws IOException {
        if (archive.exists()) throw new IOException("Archive file \"" + archive.getName() + "\" already exists.");

        if ((archive.getParentFile() == null || archive.getParentFile().exists() || archive.getParentFile().mkdirs()) && archive.createNewFile()) {
            Path absoluteArchiveParentPath = baseDir == null ? archive.toPath().getParent().toAbsolutePath() : baseDir.toPath().toAbsolutePath();

            FileOutputStream fileOut = new FileOutputStream(archive, false);
            ZstdCompressorOutputStream compressorOut = new ZstdCompressorOutputStream(fileOut, 22);
            TarArchiveOutputStream archiveOut = new TarArchiveOutputStream(compressorOut);

            files.stream()
                    .map(f -> new TarArchiveEntry(f, absoluteArchiveParentPath.relativize(f.toPath().toAbsolutePath()).toString()))
                    .forEach(e -> {
                        try {
                            System.out.println("Adding " + e.getName() + " to " + e.getPath());
                            archiveOut.putArchiveEntry(e);
                            Files.copy(e.getPath(), archiveOut);
                            archiveOut.closeArchiveEntry();
                        } catch(IOException ex) {
                            ex.printStackTrace();
                        }
                    });

            archiveOut.close();
            compressorOut.close();
            fileOut.close();
        }
    }
}
