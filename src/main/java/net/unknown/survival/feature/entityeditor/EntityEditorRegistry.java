/*
 * Copyright (c) 2026 Unknown Network Developers and contributors.
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

package net.unknown.survival.feature.entityeditor;

import net.unknown.survival.feature.entityeditor.handlers.*;
import org.bukkit.entity.*;
import org.bukkit.entity.minecart.ExplosiveMinecart;
import org.bukkit.material.Colorable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import org.bukkit.util.Transformation;

public class EntityEditorRegistry {
    private static final Map<Class<?>, EntityEditorHandler<?>> HANDLERS = new LinkedHashMap<>();
    private static final Map<String, ToolAction<?>> TOOL_ACTIONS = new HashMap<>();

    public static void init() {
        registerHandler(Entity.class, new EntityHandler());
        registerHandler(LivingEntity.class, new LivingEntityHandler());
        registerHandler(Mob.class, new MobHandler());
        registerHandler(Damageable.class, new DamageableHandler());
        registerHandler(Ageable.class, new AgeableHandler());
        registerHandler(Breedable.class, new BreedableHandler());
        registerHandler(Animals.class, new AnimalsHandler());
        registerHandler(Tameable.class, new TameableHandler());
        registerHandler(Sittable.class, new SittableHandler());
        registerHandler(Steerable.class, new SteerableHandler());
        registerHandler(Vehicle.class, new VehicleHandler());
        registerHandler(Colorable.class, new ColorableHandler());

        registerHandler(HumanEntity.class, new HumanEntityHandler());
        registerHandler(Player.class, new PlayerHandler());
        registerHandler(Sheep.class, new SheepHandler());
        registerHandler(Bogged.class, new BoggedHandler());
        registerHandler(Snowman.class, new SnowmanHandler());
        registerHandler(Piglin.class, new PiglinHandler());
        registerHandler(Hoglin.class, new HoglinHandler());
        registerHandler(Goat.class, new GoatHandler());
        registerHandler(Bee.class, new BeeHandler());
        registerHandler(Bat.class, new BatHandler());
        registerHandler(Enderman.class, new EndermanHandler());
        registerHandler(PolarBear.class, new PolarBearHandler());
        registerHandler(Turtle.class, new TurtleHandler());
        registerHandler(Vex.class, new VexHandler());
        registerHandler(Wither.class, new WitherHandler());
        registerHandler(PufferFish.class, new PufferFishHandler());
        registerHandler(Shulker.class, new ShulkerHandler());
        registerHandler(ChestedHorse.class, new ChestedHorseHandler());
        registerHandler(Dolphin.class, new DolphinHandler());
        registerHandler(Creaking.class, new CreakingHandler());
        registerHandler(Warden.class, new WardenHandler());
        registerHandler(PigZombie.class, new PigZombieHandler());
        registerHandler(ExperienceOrb.class, new ExperienceOrbHandler());
        registerHandler(Endermite.class, new EndermiteHandler());
        registerHandler(Zombie.class, new ZombieHandler());
        registerHandler(Husk.class, new HuskHandler());
        registerHandler(TNTPrimed.class, new TNTPrimedHandler());
        registerHandler(ExplosiveMinecart.class, new ExplosiveMinecartHandler());
        registerHandler(ArmorStand.class, new ArmorStandHandler());
        registerHandler(ItemFrame.class, new ItemFrameHandler());
        registerHandler(Minecart.class, new MinecartHandler());
        registerHandler(EnderCrystal.class, new EnderCrystalHandler());
        registerHandler(Display.class, new DisplayHandler());
        registerHandler(TextDisplay.class, new TextDisplayHandler());
        registerHandler(ItemDisplay.class, new ItemDisplayHandler());
        registerHandler(BlockDisplay.class, new BlockDisplayHandler());
        
        registerHandler(Axolotl.class, new AxolotlHandler());
        registerHandler(Rabbit.class, new RabbitHandler());
        registerHandler(Parrot.class, new ParrotHandler());
        registerHandler(Fox.class, new FoxHandler());
        registerHandler(Frog.class, new FrogHandler());
        registerHandler(Llama.class, new LlamaHandler());
        registerHandler(Panda.class, new PandaHandler());
        registerHandler(MushroomCow.class, new MushroomCowHandler());
        registerHandler(TropicalFish.class, new TropicalFishHandler());
        registerHandler(Boat.class, new BoatHandler());
        registerHandler(Painting.class, new PaintingHandler());
        
        registerHandler(Villager.class, new VillagerHandler());
        registerHandler(WanderingTrader.class, new WanderingTraderHandler());
        registerHandler(Cat.class, new CatHandler());
        registerHandler(Wolf.class, new WolfHandler());
        registerHandler(Horse.class, new HorseHandler());
        registerHandler(Creeper.class, new CreeperHandler());
        registerHandler(Raider.class, new RaiderHandler());
        registerHandler(Spellcaster.class, new SpellcasterHandler());
        registerHandler(AbstractArrow.class, new AbstractArrowHandler());
        registerHandler(Firework.class, new FireworkHandler());
        registerHandler(Explosive.class, new ExplosiveHandler());
        registerHandler(Fireball.class, new FireballHandler());
        registerHandler(IronGolem.class, new IronGolemHandler());
        registerHandler(Phantom.class, new PhantomHandler());
        registerHandler(Guardian.class, new GuardianHandler());
        registerHandler(Allay.class, new AllayHandler());
        registerHandler(Armadillo.class, new ArmadilloHandler());
        registerHandler(AbstractWindCharge.class, new AbstractWindChargeHandler());
        registerHandler(Camel.class, new CamelHandler());
        registerHandler(Sniffer.class, new SnifferHandler());
        registerHandler(Slime.class, new SlimeHandler());
        registerHandler(ZombieVillager.class, new ZombieVillagerHandler());

        // --- ToolAction の登録 ---
        registerDisplayScaleActions();
        registerDisplayTranslationActions();
        registerDisplayLeftRotationActions();
        registerDisplayRightRotationActions();
        registerEntityRotationActions();
    }

    public record ToolAction<T>(
            Class<T> entityType,
            String displayName,
            BiConsumer<T, Float> applyAction,
            java.util.function.Consumer<T> resetAction,
            Function<T, Float> currentValueGetter
    ) {}

    public static <T> void registerToolAction(Class<T> entityType, String actionKey, String displayName,
                                              BiConsumer<T, Float> applyAction,
                                              java.util.function.Consumer<T> resetAction,
                                              Function<T, Float> currentValueGetter) {
        TOOL_ACTIONS.put(actionKey, new ToolAction<>(entityType, displayName, applyAction, resetAction, currentValueGetter));
    }

    @SuppressWarnings("unchecked")
    public static <T> ToolAction<T> getToolAction(String actionKey) {
        return (ToolAction<T>) TOOL_ACTIONS.get(actionKey);
    }

    private static void registerDisplayScaleActions() {
        registerToolAction(Display.class, "display_scale_x", "大きさ (X)",
                (entity, val) -> {
                    Transformation t = entity.getTransformation();
                    entity.setTransformation(new Transformation(t.getTranslation(), t.getLeftRotation(),
                            new org.joml.Vector3f(val, t.getScale().y, t.getScale().z), t.getRightRotation()));
                },
                entity -> {
                    Transformation t = entity.getTransformation();
                    entity.setTransformation(new Transformation(t.getTranslation(), t.getLeftRotation(),
                            new org.joml.Vector3f(1.0f, t.getScale().y, t.getScale().z), t.getRightRotation()));
                },
                entity -> entity.getTransformation().getScale().x
        );
        registerToolAction(Display.class, "display_scale_y", "大きさ (Y)",
                (entity, val) -> {
                    Transformation t = entity.getTransformation();
                    entity.setTransformation(new Transformation(t.getTranslation(), t.getLeftRotation(),
                            new org.joml.Vector3f(t.getScale().x, val, t.getScale().z), t.getRightRotation()));
                },
                entity -> {
                    Transformation t = entity.getTransformation();
                    entity.setTransformation(new Transformation(t.getTranslation(), t.getLeftRotation(),
                            new org.joml.Vector3f(t.getScale().x, 1.0f, t.getScale().z), t.getRightRotation()));
                },
                entity -> entity.getTransformation().getScale().y
        );
        registerToolAction(Display.class, "display_scale_z", "大きさ (Z)",
                (entity, val) -> {
                    Transformation t = entity.getTransformation();
                    entity.setTransformation(new Transformation(t.getTranslation(), t.getLeftRotation(),
                            new org.joml.Vector3f(t.getScale().x, t.getScale().y, val), t.getRightRotation()));
                },
                entity -> {
                    Transformation t = entity.getTransformation();
                    entity.setTransformation(new Transformation(t.getTranslation(), t.getLeftRotation(),
                            new org.joml.Vector3f(t.getScale().x, t.getScale().y, 1.0f), t.getRightRotation()));
                },
                entity -> entity.getTransformation().getScale().z
        );
    }

    private static void registerDisplayTranslationActions() {
        registerToolAction(Display.class, "display_translation_x", "移動 (X)",
                (entity, val) -> {
                    Transformation t = entity.getTransformation();
                    entity.setTransformation(new Transformation(new org.joml.Vector3f(val, t.getTranslation().y, t.getTranslation().z), t.getLeftRotation(), t.getScale(), t.getRightRotation()));
                },
                entity -> {
                    Transformation t = entity.getTransformation();
                    entity.setTransformation(new Transformation(new org.joml.Vector3f(0.0f, t.getTranslation().y, t.getTranslation().z), t.getLeftRotation(), t.getScale(), t.getRightRotation()));
                },
                entity -> entity.getTransformation().getTranslation().x
        );
        registerToolAction(Display.class, "display_translation_y", "移動 (Y)",
                (entity, val) -> {
                    Transformation t = entity.getTransformation();
                    entity.setTransformation(new Transformation(new org.joml.Vector3f(t.getTranslation().x, val, t.getTranslation().z), t.getLeftRotation(), t.getScale(), t.getRightRotation()));
                },
                entity -> {
                    Transformation t = entity.getTransformation();
                    entity.setTransformation(new Transformation(new org.joml.Vector3f(t.getTranslation().x, 0.0f, t.getTranslation().z), t.getLeftRotation(), t.getScale(), t.getRightRotation()));
                },
                entity -> entity.getTransformation().getTranslation().y
        );
        registerToolAction(Display.class, "display_translation_z", "移動 (Z)",
                (entity, val) -> {
                    Transformation t = entity.getTransformation();
                    entity.setTransformation(new Transformation(new org.joml.Vector3f(t.getTranslation().x, t.getTranslation().y, val), t.getLeftRotation(), t.getScale(), t.getRightRotation()));
                },
                entity -> {
                    Transformation t = entity.getTransformation();
                    entity.setTransformation(new Transformation(new org.joml.Vector3f(t.getTranslation().x, t.getTranslation().y, 0.0f), t.getLeftRotation(), t.getScale(), t.getRightRotation()));
                },
                entity -> entity.getTransformation().getTranslation().z
        );
    }

    private static org.joml.Vector3f getEulerAngles(org.joml.Quaternionf q) {
        org.joml.Vector3f euler = new org.joml.Vector3f();
        q.getEulerAnglesXYZ(euler);
        return new org.joml.Vector3f((float) Math.toDegrees(euler.x), (float) Math.toDegrees(euler.y), (float) Math.toDegrees(euler.z));
    }

    private static void registerDisplayLeftRotationActions() {
        registerToolAction(Display.class, "display_left_rotation_x", "左回転 (Pitch)",
                (entity, val) -> {
                    Transformation t = entity.getTransformation();
                    org.joml.Vector3f euler = getEulerAngles(t.getLeftRotation());
                    entity.setTransformation(new Transformation(t.getTranslation(),
                            new org.joml.Quaternionf().rotationXYZ((float) Math.toRadians(val), (float) Math.toRadians(euler.y), (float) Math.toRadians(euler.z)),
                            t.getScale(), t.getRightRotation()));
                },
                entity -> {
                    Transformation t = entity.getTransformation();
                    org.joml.Vector3f euler = getEulerAngles(t.getLeftRotation());
                    entity.setTransformation(new Transformation(t.getTranslation(),
                            new org.joml.Quaternionf().rotationXYZ(0, (float) Math.toRadians(euler.y), (float) Math.toRadians(euler.z)),
                            t.getScale(), t.getRightRotation()));
                },
                entity -> getEulerAngles(entity.getTransformation().getLeftRotation()).x
        );
        registerToolAction(Display.class, "display_left_rotation_y", "左回転 (Yaw)",
                (entity, val) -> {
                    Transformation t = entity.getTransformation();
                    org.joml.Vector3f euler = getEulerAngles(t.getLeftRotation());
                    entity.setTransformation(new Transformation(t.getTranslation(),
                            new org.joml.Quaternionf().rotationXYZ((float) Math.toRadians(euler.x), (float) Math.toRadians(val), (float) Math.toRadians(euler.z)),
                            t.getScale(), t.getRightRotation()));
                },
                entity -> {
                    Transformation t = entity.getTransformation();
                    org.joml.Vector3f euler = getEulerAngles(t.getLeftRotation());
                    entity.setTransformation(new Transformation(t.getTranslation(),
                            new org.joml.Quaternionf().rotationXYZ((float) Math.toRadians(euler.x), 0, (float) Math.toRadians(euler.z)),
                            t.getScale(), t.getRightRotation()));
                },
                entity -> getEulerAngles(entity.getTransformation().getLeftRotation()).y
        );
        registerToolAction(Display.class, "display_left_rotation_z", "左回転 (Roll)",
                (entity, val) -> {
                    Transformation t = entity.getTransformation();
                    org.joml.Vector3f euler = getEulerAngles(t.getLeftRotation());
                    entity.setTransformation(new Transformation(t.getTranslation(),
                            new org.joml.Quaternionf().rotationXYZ((float) Math.toRadians(euler.x), (float) Math.toRadians(euler.y), (float) Math.toRadians(val)),
                            t.getScale(), t.getRightRotation()));
                },
                entity -> {
                    Transformation t = entity.getTransformation();
                    org.joml.Vector3f euler = getEulerAngles(t.getLeftRotation());
                    entity.setTransformation(new Transformation(t.getTranslation(),
                            new org.joml.Quaternionf().rotationXYZ((float) Math.toRadians(euler.x), (float) Math.toRadians(euler.y), 0),
                            t.getScale(), t.getRightRotation()));
                },
                entity -> getEulerAngles(entity.getTransformation().getLeftRotation()).z
        );
    }

    private static void registerDisplayRightRotationActions() {
        registerToolAction(Display.class, "display_right_rotation_x", "右回転 (Pitch)",
                (entity, val) -> {
                    Transformation t = entity.getTransformation();
                    org.joml.Vector3f euler = getEulerAngles(t.getRightRotation());
                    entity.setTransformation(new Transformation(t.getTranslation(), t.getLeftRotation(),
                            t.getScale(), new org.joml.Quaternionf().rotationXYZ((float) Math.toRadians(val), (float) Math.toRadians(euler.y), (float) Math.toRadians(euler.z))));
                },
                entity -> {
                    Transformation t = entity.getTransformation();
                    org.joml.Vector3f euler = getEulerAngles(t.getRightRotation());
                    entity.setTransformation(new Transformation(t.getTranslation(), t.getLeftRotation(),
                            t.getScale(), new org.joml.Quaternionf().rotationXYZ(0, (float) Math.toRadians(euler.y), (float) Math.toRadians(euler.z))));
                },
                entity -> getEulerAngles(entity.getTransformation().getRightRotation()).x
        );
        registerToolAction(Display.class, "display_right_rotation_y", "右回転 (Yaw)",
                (entity, val) -> {
                    Transformation t = entity.getTransformation();
                    org.joml.Vector3f euler = getEulerAngles(t.getRightRotation());
                    entity.setTransformation(new Transformation(t.getTranslation(), t.getLeftRotation(),
                            t.getScale(), new org.joml.Quaternionf().rotationXYZ((float) Math.toRadians(euler.x), (float) Math.toRadians(val), (float) Math.toRadians(euler.z))));
                },
                entity -> {
                    Transformation t = entity.getTransformation();
                    org.joml.Vector3f euler = getEulerAngles(t.getRightRotation());
                    entity.setTransformation(new Transformation(t.getTranslation(), t.getLeftRotation(),
                            t.getScale(), new org.joml.Quaternionf().rotationXYZ((float) Math.toRadians(euler.x), 0, (float) Math.toRadians(euler.z))));
                },
                entity -> getEulerAngles(entity.getTransformation().getRightRotation()).y
        );
        registerToolAction(Display.class, "display_right_rotation_z", "右回転 (Roll)",
                (entity, val) -> {
                    Transformation t = entity.getTransformation();
                    org.joml.Vector3f euler = getEulerAngles(t.getRightRotation());
                    entity.setTransformation(new Transformation(t.getTranslation(), t.getLeftRotation(),
                            t.getScale(), new org.joml.Quaternionf().rotationXYZ((float) Math.toRadians(euler.x), (float) Math.toRadians(euler.y), (float) Math.toRadians(val))));
                },
                entity -> {
                    Transformation t = entity.getTransformation();
                    org.joml.Vector3f euler = getEulerAngles(t.getRightRotation());
                    entity.setTransformation(new Transformation(t.getTranslation(), t.getLeftRotation(),
                            t.getScale(), new org.joml.Quaternionf().rotationXYZ((float) Math.toRadians(euler.x), (float) Math.toRadians(euler.y), 0)));
                },
                entity -> getEulerAngles(entity.getTransformation().getRightRotation()).z
        );
    }

    private static void registerEntityRotationActions() {
        registerToolAction(Entity.class, "entity_xrot", "向き (XRot)",
                (entity, val) -> {
                    net.minecraft.world.entity.Entity h = ((org.bukkit.craftbukkit.entity.CraftEntity) entity).getHandle();
                    h.setXRot(val);
                },
                entity -> {
                    net.minecraft.world.entity.Entity h = ((org.bukkit.craftbukkit.entity.CraftEntity) entity).getHandle();
                    h.setXRot(0f);
                },
                entity -> ((org.bukkit.craftbukkit.entity.CraftEntity) entity).getHandle().getXRot()
        );
        registerToolAction(Entity.class, "entity_yrot", "向き (YRot)",
                (entity, val) -> {
                    net.minecraft.world.entity.Entity h = ((org.bukkit.craftbukkit.entity.CraftEntity) entity).getHandle();
                    h.setYRot(val);
                    h.setYHeadRot(val);
                    h.setYBodyRot(val);
                },
                entity -> {
                    net.minecraft.world.entity.Entity h = ((org.bukkit.craftbukkit.entity.CraftEntity) entity).getHandle();
                    h.setYRot(0f);
                    h.setYHeadRot(0f);
                    h.setYBodyRot(0f);
                },
                entity -> ((org.bukkit.craftbukkit.entity.CraftEntity) entity).getHandle().getYRot()
        );
    }

    public static boolean hasHandler(Class<?> entityClass) {
        return HANDLERS.containsKey(entityClass);
    }

    public static <T> void registerHandler(Class<T> entityClass, EntityEditorHandler<T> handler) {
        HANDLERS.put(entityClass, handler);
    }

    public static <T> void unregisterHandler(Class<T> entityClass) {
        HANDLERS.remove(entityClass);
    }

    @SuppressWarnings("unchecked")
    public static <T> List<EntityEditorHandler<? super T>> getHandlers(Class<T> entityClass) {
        List<EntityEditorHandler<? super T>> result = new ArrayList<>();
        HANDLERS.forEach((targetClass, handler) -> {
            if (targetClass.isAssignableFrom(entityClass)) {
                result.add((EntityEditorHandler<? super T>) handler);
            }
        });
        return result;
    }
}
