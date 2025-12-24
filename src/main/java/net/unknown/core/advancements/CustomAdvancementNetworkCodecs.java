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

package net.unknown.core.advancements;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.*;
import net.minecraft.core.ClientAsset;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.Optional;

public class CustomAdvancementNetworkCodecs {
    public static final Codec<DisplayInfo> DISPLAY_INFO_CODEC = RecordCodecBuilder.create( /* Supports positioning: x, y */
            instance -> instance.group(
                            ItemStack.STRICT_CODEC.fieldOf("icon").forGetter(DisplayInfo::getIcon),
                            ComponentSerialization.CODEC.fieldOf("title").forGetter(DisplayInfo::getTitle),
                            ComponentSerialization.CODEC.fieldOf("description").forGetter(DisplayInfo::getDescription),
                            ClientAsset.ResourceTexture.CODEC.optionalFieldOf("background").forGetter(DisplayInfo::getBackground),
                            AdvancementType.CODEC.optionalFieldOf("frame", AdvancementType.TASK).forGetter(DisplayInfo::getType),
                            Codec.BOOL.optionalFieldOf("show_toast", true).forGetter(DisplayInfo::shouldShowToast),
                            Codec.BOOL.optionalFieldOf("announce_to_chat", true).forGetter(DisplayInfo::shouldAnnounceChat),
                            Codec.BOOL.optionalFieldOf("hidden", false).forGetter(DisplayInfo::isHidden),
                            Codec.FLOAT.optionalFieldOf("x", 0f).forGetter(DisplayInfo::getX),
                            Codec.FLOAT.optionalFieldOf("y", 0f).forGetter(DisplayInfo::getY)
                    )
                    .apply(instance, (icon, title, description, background, frame, showToast, announceToChat, hidden, x, y) -> {
                        DisplayInfo displayInfo = new DisplayInfo(icon, title, description, background, frame, showToast, announceToChat, hidden);
                        displayInfo.setLocation(x, y);
                        return displayInfo;
                    })
    );
    private static final Codec<Map<String, Criterion<?>>> CRITERIA_CODEC = Codec.unboundedMap(Codec.STRING, Criterion.CODEC)
            .validate(criteria -> criteria.isEmpty() ? DataResult.error(() -> "Advancement criteria cannot be empty") : DataResult.success(criteria));
    public static final Codec<Advancement> ADVANCEMENT_CODEC = RecordCodecBuilder.<Advancement>create( /* Supports positioning: DISPLAY_INFO_CODEC */
                    instance -> instance.group(
                                    Identifier.CODEC.optionalFieldOf("parent").forGetter(Advancement::parent),
                                    DISPLAY_INFO_CODEC.optionalFieldOf("display").forGetter(Advancement::display),
                                    AdvancementRewards.CODEC.optionalFieldOf("rewards", AdvancementRewards.EMPTY).forGetter(Advancement::rewards),
                                    CRITERIA_CODEC.fieldOf("criteria").forGetter(Advancement::criteria),
                                    AdvancementRequirements.CODEC.optionalFieldOf("requirements").forGetter(advancement -> Optional.of(advancement.requirements())),
                                    Codec.BOOL.optionalFieldOf("sends_telemetry_event", false).forGetter(Advancement::sendsTelemetryEvent)
                            )
                            .apply(instance, (parent, displayInfo, rewards, criteria, requirements, sendsTelemetryEvent) -> {
                                AdvancementRequirements advancementRequirements = requirements.orElseGet(() -> AdvancementRequirements.allOf(criteria.keySet()));
                                return new Advancement(parent, displayInfo, rewards, criteria, advancementRequirements, sendsTelemetryEvent);
                            })
            )
            .validate(advancement -> advancement.requirements().validate(advancement.criteria().keySet()).map(requirements -> advancement));
}