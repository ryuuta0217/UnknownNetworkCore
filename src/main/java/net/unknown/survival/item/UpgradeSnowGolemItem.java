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

package net.unknown.survival.item;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.unknown.core.define.DefinedTextColor;
import net.unknown.core.item.UnknownNetworkItem;
import net.unknown.core.item.UnknownNetworkItemStack;
import net.unknown.core.util.MinecraftAdapter;
import net.unknown.core.util.NewMessageUtil;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.Snowman;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class UpgradeSnowGolemItem extends UnknownNetworkItem implements Listener {
    private static final int MAX_UPGRADE_LEVEL = 5;
    private static final int BASE_SNOWBALL_DAMAGE = 3; // 最大までアップグレードした時に15ダメージになるマ？やばない？

    private static final NamespacedKey SNOW_GOLEM_UPGRADE_LEVEL_KEY = new NamespacedKey("survival", "upgrade_level");
    private static final NamespacedKey SNOW_BALL_KNOCKBACK_MODIFIER_KEY = new NamespacedKey("survival", "knockback_modifier");

    public UpgradeSnowGolemItem() {
        super(new NamespacedKey("survival", "upgrade_snow_golem"));
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!this.equals(event.getPlayer().getInventory().getItem(event.getHand()))) return;
        Stack stack = new Stack(event.getPlayer().getInventory().getItem(event.getHand()));

        if (event.getRightClicked() instanceof Snowman snowman) {
            // Item use to Snowman, Upgrade
            int currentUpgradeLevel = getUpgradeLevel(snowman);
            int newUpgradeLevel = stack.getTargetUpgradeLevel();

            if (newUpgradeLevel > MAX_UPGRADE_LEVEL) {
                NewMessageUtil.sendErrorMessage(event.getPlayer(), Component.text("スノウゴーレムをアップグレードできる最大レベルは " + MAX_UPGRADE_LEVEL + " です。非正規品ですか？"));
                return;
            }

            if (newUpgradeLevel <= currentUpgradeLevel) {
                NewMessageUtil.sendErrorMessage(event.getPlayer(), Component.empty()
                        .append(snowman.name())
                        .appendSpace()
                        .append(Component.text("は既にレベル" + currentUpgradeLevel + "にアップグレードされています")));
                return;
            }

            if (event.getPlayer().getGameMode() != GameMode.CREATIVE && event.getPlayer().getGameMode() != GameMode.SPECTATOR) {
                MinecraftAdapter.player(event.getPlayer()).getMainHandItem().shrink(1); // このイベントのコールは、先頭でインタラクトをメインハンドのみに限定しているため、メインハンドを直接指定している。
            }

            upgradeSnowman(snowman, newUpgradeLevel);
            MinecraftAdapter.player(event.getPlayer()).getCooldowns().addCooldown(net.minecraft.world.item.Items.IRON_BLOCK, 20 * newUpgradeLevel);
            NewMessageUtil.sendMessage(event.getPlayer(), Component.empty()
                    .append(snowman.name())
                    .appendSpace()
                    .append(Component.text("をレベル" + newUpgradeLevel + "にアップグレードしました")));
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity().getShooter() instanceof Snowman snowman)) return;
        if (!(MinecraftAdapter.entity(snowman) instanceof SnowGolem minecraftSnowman)) return;

        int snowmanUpgradeLevel = getUpgradeLevel(snowman);

        if (snowmanUpgradeLevel < 1) return; // アップグレードされていない場合は何もしない

        if (!(event.getEntity() instanceof Snowball snowball)) return;
        if (!(MinecraftAdapter.entity(snowball) instanceof net.minecraft.world.entity.projectile.Snowball minecraftSnowball)) return;

        if (!(event.getHitEntity() instanceof LivingEntity hitEntity)) return;
        if (!(MinecraftAdapter.entity(hitEntity) instanceof net.minecraft.world.entity.LivingEntity minecraftHitEntity)) return;

        // hurt hit entity
        doHurtTargetBySnowGolemWithSnowball(minecraftSnowman, minecraftSnowball, minecraftHitEntity);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        // アップグレードレベルが2以上の場合は、スノウゴーレムが雨や水によるダメージを受けないようにする
        if (event.getEntity() instanceof Snowman snowman) {
            if (event.getCause() == EntityDamageEvent.DamageCause.DROWNING) {
                if (getUpgradeLevel(snowman) >= 2) {
                    event.setCancelled(true);
                }
            }
        }
    }

    // Obtained from net.minecraft.world.entity.Mob#doHurtTarget(Entity)
    private static void doHurtTargetBySnowGolemWithSnowball(SnowGolem source, net.minecraft.world.entity.projectile.Snowball weapon, Entity target) {
        float damage = (float) source.getAttributeValue(Attributes.ATTACK_DAMAGE);
        float knockback = (float) source.getAttributeValue(Attributes.ATTACK_KNOCKBACK);

        if (target instanceof net.minecraft.world.entity.LivingEntity) {
            damage += EnchantmentHelper.getDamageBonus(source.getMainHandItem(), ((net.minecraft.world.entity.LivingEntity) target).getMobType());
            knockback += EnchantmentHelper.getKnockbackBonus(source);
        }

        int fireAspect = EnchantmentHelper.getFireAspect(source);

        if (fireAspect > 0) {
            // CraftBukkit start - Call a combust event when somebody hits with a fire enchanted item
            EntityCombustByEntityEvent combustEvent = new EntityCombustByEntityEvent(source.getBukkitEntity(), target.getBukkitEntity(), fireAspect * 4);
            org.bukkit.Bukkit.getPluginManager().callEvent(combustEvent);

            if (!combustEvent.isCancelled()) {
                target.setSecondsOnFire(combustEvent.getDuration(), false);
            }
            // CraftBukkit end
        }

        // Use #thrown, Yes, weapon is snowball.
        boolean hurtSuccess = target.hurt(weapon.damageSources().thrown(weapon, source), damage);

        if (hurtSuccess) {
            if (knockback > 0.0F && target instanceof net.minecraft.world.entity.LivingEntity livingTarget) {
                livingTarget.knockback(knockback * 0.5F, Mth.sin(source.getYRot() * 0.017453292F), -Mth.cos(source.getYRot() * 0.017453292F), source); // Paper
                source.setDeltaMovement(source.getDeltaMovement().multiply(0.6D, 1.0D, 0.6D));
            }

            if (target instanceof Player player) {
                // source.maybeDisableShield(player, source.getMainHandItem(), player.isUsingItem() ? player.getUseItem() : net.minecraft.world.item.ItemStack.EMPTY);
                // method is private access, copied.
                net.minecraft.world.item.ItemStack mobStack = source.getMainHandItem();
                net.minecraft.world.item.ItemStack playerStack = player.isUsingItem() ? player.getUseItem() : net.minecraft.world.item.ItemStack.EMPTY;

                if (!mobStack.isEmpty() && !playerStack.isEmpty() && mobStack.getItem() instanceof AxeItem && playerStack.is(net.minecraft.world.item.Items.SHIELD)) {
                    float f = 0.25F + (float) EnchantmentHelper.getBlockEfficiency(source) * 0.05F;

                    if (source.getRandom().nextFloat() < f) {
                        player.getCooldowns().addCooldown(net.minecraft.world.item.Items.SHIELD, 100);
                        source.level().broadcastEntityEvent(player, (byte) 30);
                    }
                }
            }

            source.doEnchantDamageEffects(source, target);
            source.setLastHurtMob(target);
        }
    }

    private static void upgradeSnowman(Snowman snowman, int upgradeLevel) { // Call when upgrading snowman
        if (MinecraftAdapter.entity(snowman) instanceof SnowGolem golem) {
            setUpgradeLevel(snowman, upgradeLevel);

            /* Damage */
            String damageAttributeModifierName = "UNC:DamageModifier";

            if (!golem.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE)) {
                golem.getAttributes().registerAttribute(Attributes.ATTACK_DAMAGE);
            }

            AttributeInstance damageAttr = golem.getAttribute(Attributes.ATTACK_DAMAGE);

            if (damageAttr != null) {
                damageAttr.getModifiers(AttributeModifier.Operation.ADDITION).forEach(multiplyModifier -> {
                    if (multiplyModifier.getName().equals(damageAttributeModifierName)) {
                        damageAttr.removeModifier(multiplyModifier);
                    }
                });

                // ATTACK_DAMAGEは、デフォルトではBaseが0なので、Additionを使用する。これにより、Base: 0 に、valueの値(upgradeLevel = 1 の場合、 3 * 1 = 3) が加算され、最終的にダメージは 3 になる。
                AttributeModifier modifier = new AttributeModifier(damageAttributeModifierName, BASE_SNOWBALL_DAMAGE * upgradeLevel, AttributeModifier.Operation.ADDITION);
                damageAttr.addPermanentModifier(modifier);
            }
            /* End of Damage */

            /* KnockBack */
            String kbAttributeModifierName = "UNC:KBModifier";

            if (!golem.getAttributes().hasAttribute(Attributes.ATTACK_KNOCKBACK)) {
                golem.getAttributes().registerAttribute(Attributes.ATTACK_KNOCKBACK);
            }

            AttributeInstance kbAttr = golem.getAttribute(Attributes.ATTACK_KNOCKBACK);

            if (kbAttr != null) {
                kbAttr.getModifiers(AttributeModifier.Operation.ADDITION).forEach(multiplyModifier -> {
                    if (multiplyModifier.getName().equals(kbAttributeModifierName)) {
                        kbAttr.removeModifier(multiplyModifier);
                    }
                });

                // ATTACK_KNOCKBACKは、デフォルトではBaseが0なので、Additionを使用する。これにより、Base: 0 に、valueの値(upgradeLevel = 1 の場合、 1 + 1 = 2) が加算され、最終的にx2のノックバックの値になる。
                AttributeModifier modifier = new AttributeModifier(kbAttributeModifierName, 1 + upgradeLevel, AttributeModifier.Operation.ADDITION);
                kbAttr.addPermanentModifier(modifier);
            }
            /* End of KnockBack */

            /* Armors */
            // TODO: UnknownNetworkBootstrap側での対応が必要。 SnowGolem#performRangedAttack() を AbstractSkeleton を参考に改造する。何も持っていない時(item == null)に雪玉を投げるようにする。
            /* End of Armors */
        }
    }

    private static void setUpgradeLevel(Snowman snowman, int level) {
        if (level < 0) throw new IllegalArgumentException("Level can't be lower than 0!");
        snowman.getPersistentDataContainer().set(SNOW_GOLEM_UPGRADE_LEVEL_KEY, PersistentDataType.INTEGER, level);
    }

    private static int getUpgradeLevel(Snowman snowman) {
        if (snowman.getPersistentDataContainer().has(SNOW_GOLEM_UPGRADE_LEVEL_KEY, PersistentDataType.INTEGER)) {
            Integer level = snowman.getPersistentDataContainer().get(SNOW_GOLEM_UPGRADE_LEVEL_KEY, PersistentDataType.INTEGER);
            return level != null ? level : 0;
        }
        return 0;
    }

    public Stack createItemStack(int targetUpgradeLevel) {
        Stack item = this.createItemStack();
        item.setTargetUpgradeLevel(targetUpgradeLevel);
        return item;
    }

    @Override
    @Deprecated
    public Stack createItemStack() {
        Stack item = new Stack(this.createItemStackBuilder(Material.IRON_BLOCK)
                .displayName(Component.text("スノーゴーレムアップグレード"))
                .lore(Component.text("スノーゴーレムをアップグレードすることができます。", DefinedTextColor.GREEN),
                        Component.empty(),
                        Component.text("スノーゴーレムに向かって右クリックで使用", DefinedTextColor.YELLOW))
                .build());
        item.setTargetUpgradeLevel(1);
        return item;
    }

    public static class Stack extends UnknownNetworkItemStack<UpgradeSnowGolemItem> {
        private static final NamespacedKey TARGET_UPGRADE_LEVEL_KEY = new NamespacedKey("survival", "target_upgrade_level");

        private int targetUpgradeLevel;

        public Stack(ItemStack handle) {
            super(handle, Items.UPGRADE_SNOW_GOLEM_ITEM);
            this.load();
        }

        private void load() {
            if (this.getHandle().getItemMeta().getPersistentDataContainer().has(TARGET_UPGRADE_LEVEL_KEY, PersistentDataType.INTEGER)) {
                Integer level = getHandle().getItemMeta().getPersistentDataContainer().get(TARGET_UPGRADE_LEVEL_KEY, PersistentDataType.INTEGER);
                this.targetUpgradeLevel = level != null ? level : 0;
            } else {
                this.targetUpgradeLevel = 1;
                this.save();
            }
            this.updateName();
        }

        private void save() {
            this.getHandle().editMeta(meta -> meta.getPersistentDataContainer().set(TARGET_UPGRADE_LEVEL_KEY, PersistentDataType.INTEGER, targetUpgradeLevel));
        }

        private String buildName() {
            return "スノウゴーレムアップグレード (Lv" + this.targetUpgradeLevel + ")";
        }

        private void updateName() {
            if (!this.getHandle().getItemMeta().hasDisplayName() || !PlainTextComponentSerializer.plainText().serialize(this.getHandle().getItemMeta().displayName()).equals(this.buildName())) {
                this.getHandle().editMeta(meta -> meta.displayName(Component.text(this.buildName()).decoration(TextDecoration.ITALIC, false)));
            }
        }

        public int getTargetUpgradeLevel() {
            return this.targetUpgradeLevel;
        }

        public void setTargetUpgradeLevel(int targetUpgradeLevel) {
            if (targetUpgradeLevel < 0) throw new IllegalArgumentException("Level can't be lower than 0!");
            this.targetUpgradeLevel = targetUpgradeLevel;
            this.save();
            this.updateName();
        }
    }
}
