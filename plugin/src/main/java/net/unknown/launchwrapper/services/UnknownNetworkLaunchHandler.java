/*
 * Copyright (c) 2021 Unknown Network Developers and contributors.
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
 *     loss of use data or profits; or business interpution) however caused and on any theory of liability,
 *     whether in contract, strict liability, or tort (including negligence or otherwise)
 *     arising in any way out of the use of this source code, event if advised of the possibility of such damage.
 */

package net.unknown.launchwrapper.services;

import cpw.mods.modlauncher.TransformingClassLoader;
import cpw.mods.modlauncher.api.ILaunchHandlerService;
import cpw.mods.modlauncher.api.ITransformingClassLoader;
import cpw.mods.modlauncher.api.ITransformingClassLoaderBuilder;
import net.unknown.launchwrapper.util.ClassLoaderUtil;
import net.unknown.launchwrapper.LaunchConstants;
import net.unknown.launchwrapper.Main;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.jar.JarFile;

public class UnknownNetworkLaunchHandler implements ILaunchHandlerService {
    private static final List<String> TRANSFORMING_EXCLUDE_PACKAGES = new ArrayList<>() {{
        add("org.spongepowered.asm.");
        add("org.spongepowered.configurate.");
        add("org.apache.logging.log4j.");
        add("org.jline.");
        add("org.fusesource.");
        add("net.minecrell.terminalconsole.");
        add("com.google.inject.");
    }};
    private final Logger logger = LogManager.getLogger("LaunchHandler");

    @Override
    public String name() {
        return "un-lh";
    }

    @Override
    public void configureTransformationClassLoader(ITransformingClassLoaderBuilder builder) {
        for (URL url : ClassLoaderUtil.getSystemClassPathURLs()) {
            try {
                URI uri = url.toURI();
                if(!this.isTransformable(uri)) {
                    this.logger.info("Skipping to transforming \"" + url + "\"");
                    continue;
                }

                builder.addTransformationPath(Paths.get(uri));
                this.logger.info("Adding transforming to \"" + url + "\"");
            } catch (URISyntaxException | IOException e) {
                this.logger.error("Failed to add \"" + url + "\" to transformation");
            }
        }
    }

    @Override
    public Callable<Void> launchService(String[] arguments, ITransformingClassLoader launchClassLoader) {
        launchClassLoader.addTargetPackageFilter((s) -> TRANSFORMING_EXCLUDE_PACKAGES.stream().noneMatch(s::startsWith));

        return () -> {
            if(Files.exists(Path.of(LaunchConstants.LAUNCH_JAR))) {
                Class.forName(LaunchConstants.LAUNCH_TARGET, true, launchClassLoader.getInstance())
                        .getMethod("main", String[].class)
                        .invoke(null, (Object[]) arguments);
            } else throw new FileNotFoundException(LaunchConstants.LAUNCH_JAR + " not found");
            return null;
        };
    }

    private boolean isTransformable(URI uri) throws URISyntaxException, IOException {
        File mainClass = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        File targetFile = new File(uri);

        // JVMクラスはTransformできないので常にfalseを返す
        if(targetFile.getAbsolutePath().startsWith(System.getProperty("java.home"))) return false;

        if(!mainClass.toPath().toAbsolutePath().normalize().equals(targetFile.toPath().toAbsolutePath().normalize())) {
            if (targetFile.isDirectory()) {
                if(new File(targetFile, "org/spongepowered/asm/").exists()) return false;
            } else if (targetFile.isFile()) {
                if(new JarFile(new File(uri)).getEntry("org/spongepowered/asm/") != null) return false;
            }
        }

        return true;
    }
}
