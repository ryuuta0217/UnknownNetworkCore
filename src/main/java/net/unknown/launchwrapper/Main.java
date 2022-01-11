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

package net.unknown.launchwrapper;

import cpw.mods.modlauncher.Launcher;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings("removal")
public class Main {
    public static void main(String[] args) {
        List<String> launchArguments = new ArrayList<>(Arrays.asList(args));

        launchArguments.add("--launchTarget");
        launchArguments.add("un-lh");

        try {
            Agent.addJar(new File(LaunchConstants.LAUNCH_JAR));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to add main jar " + LaunchConstants.LAUNCH_JAR);
        }

        SecurityManager org = System.getSecurityManager();
        try {
            System.setProperty("paperclip.patchonly", "true");
            try {
                System.setSecurityManager(new SecurityManager() {
                    @Override
                    public void checkPermission(Permission perm){

                    }

                    @Override
                    public void checkExit(int status) {
                        throw new SecurityException(String.valueOf(status));
                    }
                });
                Class.forName("io.papermc.paperclip.Paperclip").getMethod("main", String[].class).invoke(null, (Object) new String[0]);
                System.setSecurityManager(org);
            } catch(SecurityException ignored) {}
            Agent.addJar(new File("./versions/1.18.1/paper-1.18.1.jar"));
            System.getProperties().remove("paperclip.patchonly");
        } catch(ClassNotFoundException | InvocationTargetException | IllegalAccessException | NoSuchMethodException | IOException ignored) {}
        System.setSecurityManager(org);

        if(Files.exists(LaunchConstants.LIBRARIES_DIR)) {
            try {
                Files.walk(LaunchConstants.LIBRARIES_DIR).forEach(path -> {
                    if(!path.toString().endsWith(".jar")) return;
                    try {
                        Agent.addJar(path);
                    } catch (IOException e) {
                        throw new IllegalStateException("Failed to add library jar \"" + path.toAbsolutePath() + "\"");
                    }
                });
            } catch (IOException e) {
                throw new IllegalArgumentException("Failed to get libraries");
            }
        }

        Agent.updateSecurity();

        Launcher.main(launchArguments.toArray(new String[0]));
    }
}
