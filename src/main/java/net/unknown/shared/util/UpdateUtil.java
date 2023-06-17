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

package net.unknown.shared.util;

import com.ryuuta0217.api.github.repository.actions.WorkflowRun;
import com.ryuuta0217.api.github.repository.actions.WorkflowRunJob;
import com.ryuuta0217.api.github.repository.branch.Branch;
import com.ryuuta0217.api.github.GitHubAPI;
import com.ryuuta0217.api.github.repository.check.CheckRun;
import com.ryuuta0217.api.github.repository.commit.interfaces.Commit;
import com.ryuuta0217.api.github.repository.interfaces.Repository;
import com.ryuuta0217.api.github.repository.shared.Conclusion;
import com.ryuuta0217.api.github.repository.shared.Status;
import com.ryuuta0217.api.github.repository.commit.CommitImpl;
import com.ryuuta0217.api.github.user.interfaces.PublicUser;
import com.ryuuta0217.util.HTTPFetch;
import net.unknown.shared.SharedConstants;
import net.unknown.shared.VersionInfo;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.*;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class UpdateUtil {
    public static final String GITHUB_API_BASE_URL = "https://api.github.com";
    private static final String GITHUB_ACCESS_TOKEN;

    public static final GitHubAPI GITHUB_API;

    private static Thread UNC_UPDATE_SHUTDOWN_HOOK;
    private static Thread PAPER_UPDATE_SHUTDOWN_HOOK;

    static {
        String gitHubAccessToken = null;
        try {
            File tokensFile = new File(SharedConstants.DATA_FOLDER, "tokens.txt");
            if ((tokensFile.getParentFile().exists() || tokensFile.getParentFile().mkdirs()) && (tokensFile.exists() || tokensFile.createNewFile())) {
                gitHubAccessToken = Files.readAllLines(tokensFile.toPath())
                        .stream()
                        .filter(line -> line.matches("^github-access-token: ?ghp_[a-zA-Z0-9]{36}$"))
                        .map(line -> line.replaceFirst("^github-access-token: ?", ""))
                        .findFirst()
                        .orElse(null);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        GITHUB_ACCESS_TOKEN = gitHubAccessToken;
        if (GITHUB_ACCESS_TOKEN == null) {
            System.out.println("GitHub access token not set! UpdateUtil maybe not completely working.");
            GITHUB_API = null;
        } else {
            GITHUB_API = new GitHubAPI(GITHUB_ACCESS_TOKEN);
        }
    }

    public static final String PAPER_API_BASE_URL = "https://api.papermc.io";

    public static final String UNC_FILE_PATTERN = "https://repo.yncrypt.net/repository/unknown-network/net/unknown/UnknownNetworkCore/%1$s/UnknownNetworkCore-%1$s.jar";

    public static void main(String[] args) {
        GitHubAPI api = new GitHubAPI(GITHUB_ACCESS_TOKEN);
        Branch branch = api.getBranch("ryuuta0217", "UnknownNetworkCore", "production");
        List<CheckRun> checkRuns = branch.getCommit().tryGetCommit().tryGetCheckRuns();
        if (checkRuns != null) {
            checkRuns.forEach(checkRun -> {
                WorkflowRun workflowRun = checkRun.tryGetWorkflowRun();
                if (workflowRun != null) {
                    System.out.println(workflowRun.getStatus());
                    List<WorkflowRunJob> jobs = workflowRun.getJobs();
                    if (jobs != null) {
                        jobs.forEach(job -> {
                            System.out.println(job.getConclusion());
                        });
                    }
                }
            });
        }
        //Commit currentCommit = api.getCommit("ryuuta0217", "UnknownNetworkCore", "f8eb54ac");
        //Commit latestCommit = api.getCommit("ryuuta0217", "UnknownNetworkCore", "production");
        //if (latestCommit == null) return;
        //api.getWorkflowRunJobs(api.getRepository("ryuuta0217", "UnknownNetworkCore"), 5280989681L);
    }

    public static void updateUNC(String targetBranch, @Nullable File downloadAs, boolean replaceOnShutdown, @Nonnull File replaceAs, Logger logger) {
        logger.info(targetBranch + " ブランチのアップデートを確認しています...");
        GitHubAPI api = new GitHubAPI(GITHUB_ACCESS_TOKEN);
        Branch branch = api.getBranch("ryuuta0217", "UnknownNetworkCore", targetBranch == null ? "production" : targetBranch);
        if (branch != null) {
            String commitSha = branch.getCommit().getId().substring(0, 8);
            logger.info("ブランチ " + targetBranch + " の取得に成功しました。最新コミットハッシュは " + commitSha + " です。");

            String downloadUrl = String.format(UNC_FILE_PATTERN, commitSha);
            if (downloadAs == null) downloadAs = new File("./plugins/UnknownNetworkCore.jar.new");
            try {
                logger.info("アップデートファイルを " + downloadUrl + " から " + downloadAs.getCanonicalPath() + " にダウンロードします");
                File downloadAsFinal = downloadAs;
                downloadFileAsync(downloadUrl, downloadAs, () -> {
                    logger.info("ファイルのダウンロードが完了しました。");
                    if (replaceOnShutdown) {
                        logger.info("「停止時にファイルを置換」が有効になりました。JVMのシャットダウンフックに登録しています...");
                        if (UNC_UPDATE_SHUTDOWN_HOOK != null) {
                            logger.info("すでに登録されているシャットダウンフックが見つかりました。登録を解除します。");
                            Runtime.getRuntime().removeShutdownHook(UNC_UPDATE_SHUTDOWN_HOOK);
                        }

                        UNC_UPDATE_SHUTDOWN_HOOK = newUpdateThread(logger, downloadAsFinal, replaceAs, "Update UnknownNetworkCore");
                        Runtime.getRuntime().addShutdownHook(UNC_UPDATE_SHUTDOWN_HOOK);
                        logger.info("シャットダウンフックを登録しました。サーバーを再起動すると、アップデートが適用されます。");
                    }
                }, (e) -> {
                    System.out.println("Download Failed.");
                    e.printStackTrace();
                });
            } catch (IOException ignored) {}
        }
    }

    public static VersionInfo getUNCLatestVersion() {
        return getUNCLatestVersion("production");
    }

    @Nullable
    public static VersionInfo getUNCLatestVersion(String branchName) {
        GitHubAPI api = new GitHubAPI(GITHUB_ACCESS_TOKEN);
        Branch branch = api.getBranch("ryuuta0217", "UnknownNetworkCore" , branchName);
        if (branch != null) {
            Optional<Commit> first = branch.getCommits().stream().filter(commit -> {
                List<CheckRun> checkRuns = commit.tryGetCheckRuns();
                if (checkRuns.size() == 0) return false;
                return checkRuns.stream().anyMatch(checkRun -> checkRun.tryGetWorkflowRun().getJobs().stream().anyMatch(job -> job.getConclusion() == Conclusion.SUCCESS));
            }).findFirst();

            if (first.isPresent()) {
                return new VersionInfo(branchName, first.get().getSha().substring(0, 8));
            }
        }
        return null;
    }

    public static void updatePaper(String minecraftVersion, @Nullable String paperVersion, @Nullable File downloadAs, boolean replaceOnShutdown, @Nonnull File replaceAs, Logger logger) {
        if (paperVersion == null) paperVersion = getPaperLatestVersion(minecraftVersion);
        logger.info("Minecraft " + minecraftVersion + "向けの Paper-" + paperVersion + " にアップデートしています...");
        if (paperVersion != null) {
            try {
                JSONObject buildInfo = new JSONObject(HTTPFetch.fetchGet(PAPER_API_BASE_URL + "/v2/projects/paper/versions/" + minecraftVersion + "/builds/" + paperVersion)
                        .addHeader("Accept", "application/json")
                        .sentAndReadAsString());

                if (buildInfo.has("downloads")) {
                    logger.info("Paper-" + paperVersion + " のビルド情報の取得に成功しました");
                    String fileName = buildInfo.getJSONObject("downloads").getJSONObject("application").getString("name");
                    String downloadUrl = PAPER_API_BASE_URL + "/v2/projects/paper/versions/" + minecraftVersion + "/builds/" + paperVersion + "/downloads/" + fileName;
                    if (downloadAs == null) downloadAs = new File("./paper.jar.new");
                    logger.info("アップデートファイルを " + downloadUrl + " から " + downloadAs.getCanonicalPath() + " にダウンロードします");

                    File downloadAsFinal = downloadAs;
                    downloadFileAsync(downloadUrl, downloadAs, () -> {
                        logger.info("ファイルのダウンロードが完了しました。");
                        if (replaceOnShutdown) {
                            logger.info("「停止時にファイルを置換」が有効になりました。JVMのシャットダウンフックに登録しています...");
                            if (PAPER_UPDATE_SHUTDOWN_HOOK != null) {
                                logger.info("すでに登録されているシャットダウンフックが見つかりました。登録を解除します。");
                                Runtime.getRuntime().removeShutdownHook(PAPER_UPDATE_SHUTDOWN_HOOK);
                            }

                            PAPER_UPDATE_SHUTDOWN_HOOK = newUpdateThread(logger, downloadAsFinal, replaceAs, "Update Paper");
                            Runtime.getRuntime().addShutdownHook(PAPER_UPDATE_SHUTDOWN_HOOK);
                            logger.info("シャットダウンフックを登録しました。サーバーを再起動すると、アップデートが適用されます。");
                        }
                    }, (e) -> {
                        logger.severe("ファイルのダウンロードに失敗しました。");
                        e.printStackTrace();
                    });
                } else {
                    logger.warning("指定されたPaperのバージョン " + paperVersion + " に該当するダウンロードが見つかりませんでした。");
                }
            } catch (IOException e) {
                logger.severe("Paperのビルド情報の取得に失敗しました。");
                e.printStackTrace();
            }
        }
    }

    public static String getPaperLatestVersion(@Nonnull String minecraftVersion) {
        String[] versions = getPaperVersions(minecraftVersion);
        if (versions == null || versions.length == 0) return null;
        return versions[versions.length - 1];
    }

    public static String[] getPaperVersions(@Nonnull String minecraftVersion) {
        try {
            JSONObject versionInfo = new JSONObject(HTTPFetch.fetchGet(PAPER_API_BASE_URL + "/v2/projects/paper/versions/" + minecraftVersion)
                    .addHeader("Accept", "application/json")
                    .sentAndReadAsString());
            return versionInfo.getJSONArray("builds").toList()
                    .stream()
                    .filter(raw -> raw instanceof Integer)
                    .map(String::valueOf)
                    .toArray(String[]::new);
        } catch (IOException e) {
            return null;
        }
    }

    public static String getPaperAvailableLatestMinecraftVersion() {
        String[] availableVersion = getPaperAvailableMinecraftVersions();
        if (availableVersion == null || availableVersion.length == 0) return null;
        return availableVersion[availableVersion.length - 1];
    }

    public static String[] getPaperAvailableMinecraftVersions() {
        try {
            JSONObject projectAbout = new JSONObject(HTTPFetch.fetchGet(PAPER_API_BASE_URL + "/v2/projects/paper")
                    .addHeader("Accept", "application/json")
                    .sentAndReadAsString());
            return projectAbout.getJSONArray("versions").toList()
                    .stream()
                    .filter(raw -> raw instanceof String)
                    .map(raw -> (String) raw)
                    .toArray(String[]::new);
        } catch (IOException e) {
            return null;
        }
    }

    public static void downloadFileAsync(String url, File saveTo, Runnable onComplete, Consumer<Exception> onError) throws MalformedURLException {
        HTTPFetch.fetchGet(url)
                .setOnError((connection, exception) -> onError.accept(exception))
                .setOnSuccess((connection, inputStream) -> {
                    try {
                        Files.copy(inputStream, saveTo.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        onComplete.run();
                    } catch (Exception e) {
                        onError.accept(e);
                    }
                })
                .sentAsync();
    }

    private static Thread newUpdateThread(Logger logger, File newFile, File updateTarget, String threadName) {
        Thread thread = new Thread(() -> {
            try {
                if (updateTarget.exists()) Files.move(updateTarget.toPath(), new File(updateTarget.getAbsolutePath() + ".old").toPath(), StandardCopyOption.REPLACE_EXISTING);
                Files.move(newFile.toPath(), updateTarget.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ignored) {}
        });
        thread.setName(threadName);
        return thread;
    }

    public static boolean isWaitingRestart() {
        return UNC_UPDATE_SHUTDOWN_HOOK != null || PAPER_UPDATE_SHUTDOWN_HOOK != null;
    }
}
