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

package net.unknown.survival.update;

import com.ryuuta0217.api.github.repository.commit.Commit;
import com.ryuuta0217.api.github.repository.commit.CompareResult;
import com.ryuuta0217.api.github.repository.commit.CompareStatus;
import com.ryuuta0217.api.github.repository.commit.Stats;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.unknown.UnknownNetworkCore;
import net.unknown.core.define.DefinedTextColor;
import net.unknown.core.managers.ListenerManager;
import net.unknown.core.managers.RunnableManager;
import net.unknown.shared.VersionInfo;
import net.unknown.shared.util.UpdateUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;

public class UNCUpdateCheckTask extends BukkitRunnable implements Listener {
    private static final Logger LOGGER = Logger.getLogger("UNCUpdateCheckTask");

    private static final UNCUpdateCheckTask INSTANCE = new UNCUpdateCheckTask();
    private static BukkitTask TASK;

    public static void start() {
        if (TASK != null && !TASK.isCancelled()) {
            LOGGER.info("Task is already running, cancel this.");
            TASK.cancel();
            ListenerManager.unregisterListener(INSTANCE);
        }

        if (UpdateUtil.GITHUB_API == null) return;
        TASK = INSTANCE.runTaskTimerAsynchronously(UnknownNetworkCore.getInstance(), 20 * 60, 20 * 60);
        ListenerManager.registerListener(INSTANCE);
    }

    private Component updateMessage = null;
    private String lastCheckedSha = "";

    @Override
    public void run() {
        if (UpdateUtil.GITHUB_API == null) {
            LOGGER.warning("GitHub APIが利用できません。APIトークンの設定を忘れましたか？");
            this.cancel();
            return;
        }

        VersionInfo current = UnknownNetworkCore.getVersion();
        if (current == null) {
            LOGGER.warning("現在実行中のUnknownNetworkCoreのバージョンを取得できませんでした。自家製、またはGitのセットアップを完了してください。");
            this.cancel();
            return;
        }

        VersionInfo latest = UpdateUtil.getUNCLatestVersion("production");
        if (latest == null) {
            LOGGER.warning("最新のUnknownNetworkCoreのバージョンを取得できませんでした。リポジトリが消去されているか、アクセス権がありません。(またはブランチ名が変わりましたか？)");
            this.cancel();
            return;
        }

        if (latest.commitSha().equals(this.lastCheckedSha)) {
            // if latest commit is already checked, tasks run before, skip this.
            return;
        }

        this.lastCheckedSha = latest.commitSha();

        if (current.sameBranch(latest)) {
            CompareResult compare = UpdateUtil.GITHUB_API.getCompareResult(UpdateUtil.GITHUB_API.getRepository("ryuuta0217", "UnknownNetworkCore"), current.commitSha(), latest.commitSha());
            if (compare.getStatus() == CompareStatus.AHEAD) {
                LOGGER.info("新しいUnknownNetworkCoreが利用可能になりました！あなたがアップデートしていない間に " + compare.getAheadBy() + " コミット進みました！");
                sendUpdateInformationToOps(current, latest, compare);
                LOGGER.info("アップデートをダウンロードしています...");
                UpdateUtil.updateUNC("production", null, true, new File("./plugins/UnknownNetworkCore.jar"), LOGGER);
            } else if (compare.getStatus() == CompareStatus.IDENTICAL) {
                LOGGER.info("最新版を利用中です。");
            } else if (compare.getStatus() == CompareStatus.BEHIND) {
                LOGGER.warning("productionブランチには居るようですが...時代の先を行くバージョンを使用しているようです。GitへのPushを忘れましたか？");
            } else {
                LOGGER.info(compare.getStatus().name());
            }
        } else {
            LOGGER.warning("productionブランチ以外のバージョンのようです。自家製、または開発中ですか？");
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (this.updateMessage == null) return;
        RunnableManager.runDelayed(() -> {
            Player player = event.getPlayer();
            if (player.isOp()) {
                player.sendMessage(this.updateMessage);
                if (UpdateUtil.isWaitingRestart()) {
                    player.sendMessage(Component.text("サーバーの再起動を待機しています。サーバーを再起動すると、アップデートが適用されます。", DefinedTextColor.YELLOW, TextDecoration.BOLD));
                }
            }
        }, 1L);
    }

    private void sendUpdateInformationToOps(VersionInfo current, VersionInfo latest, CompareResult compare) {
        Component message = Component.empty()
                .appendNewline()
                .append(Component.text("UNCの新しいバージョンが利用可能になりました", DefinedTextColor.GOLD, TextDecoration.BOLD))
                .appendNewline()
                .append(Component.text(current.commitSha(), DefinedTextColor.YELLOW).append(Component.text(" -> ", DefinedTextColor.LIGHT_PURPLE)).append(Component.text(latest.commitSha(), DefinedTextColor.GREEN)))
                .appendNewline()
                .append(Component.text("変更履歴:", DefinedTextColor.AQUA));

        for (Commit commitSimple : compare.getCommits()) {
            Commit commit = commitSimple.tryGetCompleteData();
            String commitMessage = commit.getCommit().getMessage();
            Component hoverMessage = Component.text(commit.getCommit().getAuthor().getName(), DefinedTextColor.GREEN)
                    .appendNewline()
                    .append(Component.text(commitMessage, DefinedTextColor.AQUA));

            if (commit.getStats() != null) {
                Stats stats = commit.getStats();
                hoverMessage = hoverMessage.appendNewline().appendNewline()
                        .append(Component.text("+" + stats.getAdditions(), DefinedTextColor.GREEN))
                        .append(Component.text(" | ", DefinedTextColor.WHITE))
                        .append(Component.text("-" + stats.getDeletions(), DefinedTextColor.RED))
                        .append(Component.text(" | ", DefinedTextColor.WHITE))
                        .append(Component.text("Total " + stats.getTotal() + " line(s) changed", DefinedTextColor.YELLOW));
            } else {
                hoverMessage = hoverMessage.appendNewline().appendNewline();
            }

            hoverMessage = hoverMessage.appendNewline()
                    .append(Component.text(commit.getSha(), DefinedTextColor.GRAY))
                    .appendNewline()
                    .append(Component.text(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss").format(commit.getCommit().getAuthor().getDate().withZoneSameInstant(ZoneId.of("Asia/Tokyo"))), DefinedTextColor.GRAY));

            HoverEvent<Component> commitMessageHoverEvent = HoverEvent.showText(hoverMessage);

            Component commitLine = Component.empty()
                    .append(Component.text("[" + commit.getSha().substring(0, 4) + "]", DefinedTextColor.LIGHT_PURPLE))
                    .appendSpace()
                    .append(Component.text(commitMessage.replaceAll("\\n.*", ""), getCommitMessageColor(commitMessage)))
                    .hoverEvent(commitMessageHoverEvent);

            message = message.appendNewline().append(commitLine);
        }

        this.updateMessage = message;

        final Component messageFinal = message;
        Bukkit.getOnlinePlayers()
                .stream()
                .filter(Player::isOp)
                .forEach(operator -> operator.sendMessage(messageFinal));
    }

    private static TextColor getCommitMessageColor(String commitMessage) {
        if (commitMessage.startsWith("Merge branch")) {
            return DefinedTextColor.YELLOW;
        } else if (commitMessage.startsWith("feat: ")) {
            return DefinedTextColor.GOLD;
        } else if (commitMessage.startsWith("[skip ci]")) {
            return DefinedTextColor.GRAY;
        } else if (commitMessage.contains("Fix")) {
            return DefinedTextColor.RED;
        } else {
            return DefinedTextColor.AQUA;
        }
    }
}
