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
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.entity.*;
import org.bukkit.entity.minecart.ExplosiveMinecart;
import org.bukkit.material.Colorable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class EntityEditorRegistry {
    private static final Map<Class<?>, EntityEditorHandler<?>> HANDLERS = new LinkedHashMap<>();
    private static final Map<String, ToolAction<?, ?>> TOOL_ACTIONS = new HashMap<>();

    private static final Function<String, Integer> INTEGER_DECODER = Integer::parseInt;
    private static final Function<Integer, String> INTEGER_ENCODER = String::valueOf;
    private static final Function<Integer, String> INTEGER_FORMATTER = String::valueOf;

    private static final Function<String, Float> FLOAT_DECODER = Float::parseFloat;
    private static final Function<Float, String> FLOAT_ENCODER = String::valueOf;
    private static final Function<Float, String> FLOAT_FORMATTER = value -> String.format("%.2f", value);
    private static final Function<Float, String> DEGREE_FORMATTER = value -> String.format("%.1f°", value);

    private static final Function<String, Double> DOUBLE_DECODER = Double::parseDouble;
    private static final Function<Double, String> DOUBLE_ENCODER = String::valueOf;
    private static final Function<Double, String> DOUBLE_FORMATTER = value -> String.format("%.2f", value);

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

        ToolAction.nothing();
    }

    public record ToolAction<T,V>(Class<T> entityType, String key, String displayName,
            Function<String, V> decoder, Function<V, String> encoder,
            Function<V, String> formatter,
            BiConsumer<T, V> applyAction, BiConsumer<T, V> incrementAction, BiConsumer<T, V> decrementAction,
            Consumer<T> resetAction,
            Function<T, V> currentValueGetter
    ) {
        public static final ToolAction<Display, Float> DISPLAY_SCALE_X = registerToolAction(Display.class, "display_scale_x", "大きさ (X)",
                FLOAT_DECODER,
                FLOAT_ENCODER,
                FLOAT_FORMATTER,
                (entity, value) -> {
                    Transformation current = entity.getTransformation();
                    entity.setTransformation(new Transformation(
                            current.getTranslation(),
                            current.getLeftRotation(),
                            new Vector3f(value, current.getScale().y, current.getScale().z),
                            current.getRightRotation())
                    );
                },
                (entity, incrementValue) -> {
                    Transformation current = entity.getTransformation();
                    entity.setTransformation(new Transformation(
                            current.getTranslation(),
                            current.getLeftRotation(),
                            current.getScale().add(incrementValue, 0.0f, 0.0f),
                            current.getRightRotation())
                    );
                },
                (entity, decrementValue) -> {
                    Transformation current = entity.getTransformation();
                    entity.setTransformation(new Transformation(
                            current.getTranslation(),
                            current.getLeftRotation(),
                            current.getScale().add(-decrementValue, 0.0f, 0.0f),
                            current.getRightRotation())
                    );
                },
                entity -> {
                    Transformation current = entity.getTransformation();
                    entity.setTransformation(new Transformation(
                            current.getTranslation(),
                            current.getLeftRotation(),
                            new Vector3f(1.0f, current.getScale().y, current.getScale().z),
                            current.getRightRotation())
                    );
                },
                entity -> entity.getTransformation().getScale().x
        );

        public static final ToolAction<Display, Float> DISPLAY_SCALE_Y = registerToolAction(Display.class, "display_scale_y", "大きさ (Y)",
                FLOAT_DECODER,
                FLOAT_ENCODER,
                FLOAT_FORMATTER,
                (entity, value) -> {
                    Transformation current = entity.getTransformation();
                    entity.setTransformation(new Transformation(
                            current.getTranslation(),
                            current.getLeftRotation(),
                            new Vector3f(current.getScale().x, value, current.getScale().z),
                            current.getRightRotation())
                    );
                },
                (entity, incrementValue) -> {
                    Transformation current = entity.getTransformation();
                    entity.setTransformation(new Transformation(
                            current.getTranslation(),
                            current.getLeftRotation(),
                            current.getScale().add(0.0f, incrementValue, 0.0f),
                            current.getRightRotation())
                    );
                },
                (entity, decrementValue) -> {
                    Transformation current = entity.getTransformation();
                    entity.setTransformation(new Transformation(
                            current.getTranslation(),
                            current.getLeftRotation(),
                            current.getScale().add(0.0f, -decrementValue, 0.0f),
                            current.getRightRotation())
                    );
                },
                entity -> {
                    Transformation current = entity.getTransformation();
                    entity.setTransformation(new Transformation(
                            current.getTranslation(),
                            current.getLeftRotation(),
                            new Vector3f(current.getScale().x, 1.0f, current.getScale().z),
                            current.getRightRotation())
                    );
                },
                entity -> entity.getTransformation().getScale().y
        );

        public static final ToolAction<Display, Float> DISPLAY_SCALE_Z = registerToolAction(Display.class, "display_scale_z", "大きさ (Z)",
                FLOAT_DECODER,
                FLOAT_ENCODER,
                FLOAT_FORMATTER,
                (entity, value) -> {
                    Transformation current = entity.getTransformation();
                    entity.setTransformation(new Transformation(
                            current.getTranslation(),
                            current.getLeftRotation(),
                            new Vector3f(current.getScale().x, current.getScale().y, value),
                            current.getRightRotation())
                    );
                },
                (entity, incrementValue) -> {
                    Transformation current = entity.getTransformation();
                    entity.setTransformation(new Transformation(
                            current.getTranslation(),
                            current.getLeftRotation(),
                            current.getScale().add(0.0f, 0.0f, incrementValue),
                            current.getRightRotation())
                    );
                },
                (entity, decrementValue) -> {
                    Transformation current = entity.getTransformation();
                    entity.setTransformation(new Transformation(
                            current.getTranslation(),
                            current.getLeftRotation(),
                            current.getScale().add(0.0f, 0.0f, -decrementValue),
                            current.getRightRotation())
                    );
                },
                entity -> {
                    Transformation current = entity.getTransformation();
                    entity.setTransformation(new Transformation(
                            current.getTranslation(),
                            current.getLeftRotation(),
                            new Vector3f(current.getScale().x, current.getScale().y, 1.0f),
                            current.getRightRotation())
                    );
                },
                entity -> entity.getTransformation().getScale().z
        );

        public static final ToolAction<Display, Float> DISPLAY_TRANSLATION_X = registerToolAction(Display.class, "display_translation_x", "移動 (X)",
                FLOAT_DECODER,
                FLOAT_ENCODER,
                FLOAT_FORMATTER,
                (entity, value) -> {
                    Transformation current = entity.getTransformation();
                    entity.setTransformation(new Transformation(
                            new Vector3f(value, current.getTranslation().y, current.getTranslation().z),
                            current.getLeftRotation(),
                            current.getScale(),
                            current.getRightRotation())
                    );
                },
                (entity, incrementValue) -> {
                    Transformation current = entity.getTransformation();
                    entity.setTransformation(new Transformation(
                            current.getTranslation().add(incrementValue, 0.0f, 0.0f),
                            current.getLeftRotation(),
                            current.getScale(),
                            current.getRightRotation())
                    );
                },
                (entity, decrementValue) -> {
                    Transformation current = entity.getTransformation();
                    entity.setTransformation(new Transformation(
                            current.getTranslation().add(-decrementValue, 0.0f, 0.0f),
                            current.getLeftRotation(),
                            current.getScale(),
                            current.getRightRotation())
                    );
                },
                entity -> {
                    Transformation current = entity.getTransformation();
                    entity.setTransformation(new Transformation(
                            new Vector3f(0.0f, current.getTranslation().y, current.getTranslation().z),
                            current.getLeftRotation(),
                            current.getScale(),
                            current.getRightRotation())
                    );
                },
                entity -> entity.getTransformation().getTranslation().x
        );

        public static final ToolAction<Display, Float> DISPLAY_TRANSLATION_Y = registerToolAction(Display.class, "display_translation_y", "移動 (Y)",
                FLOAT_DECODER,
                FLOAT_ENCODER,
                FLOAT_FORMATTER,
                (entity, value) -> {
                    Transformation current = entity.getTransformation();
                    entity.setTransformation(new Transformation(
                            new Vector3f(current.getTranslation().x, value, current.getTranslation().z),
                            current.getLeftRotation(),
                            current.getScale(),
                            current.getRightRotation())
                    );
                },
                (entity, incrementValue) -> {
                    Transformation current = entity.getTransformation();
                    entity.setTransformation(new Transformation(
                            current.getTranslation().add(0.0f, incrementValue, 0.0f),
                            current.getLeftRotation(),
                            current.getScale(),
                            current.getRightRotation())
                    );
                },
                (entity, decrementValue) -> {
                    Transformation current = entity.getTransformation();
                    entity.setTransformation(new Transformation(
                            current.getTranslation().add(0.0f, -decrementValue, 0.0f),
                            current.getLeftRotation(),
                            current.getScale(),
                            current.getRightRotation())
                    );
                },
                entity -> {
                    Transformation current = entity.getTransformation();
                    entity.setTransformation(new Transformation(
                            new Vector3f(current.getTranslation().x, 0.0f, current.getTranslation().z),
                            current.getLeftRotation(),
                            current.getScale(),
                            current.getRightRotation())
                    );
                },
                entity -> entity.getTransformation().getTranslation().y
        );

        public static final ToolAction<Display, Float> DISPLAY_TRANSLATION_Z = registerToolAction(Display.class, "display_translation_z", "移動 (Z)",
                FLOAT_DECODER,
                FLOAT_ENCODER,
                FLOAT_FORMATTER,
                (entity, value) -> {
                    Transformation current = entity.getTransformation();
                    entity.setTransformation(new Transformation(
                            new Vector3f(current.getTranslation().x, current.getTranslation().y, value),
                            current.getLeftRotation(),
                            current.getScale(),
                            current.getRightRotation())
                    );
                },
                (entity, incrementValue) -> {
                    Transformation current = entity.getTransformation();
                    entity.setTransformation(new Transformation(
                            current.getTranslation().add(0.0f, 0.0f, incrementValue),
                            current.getLeftRotation(),
                            current.getScale(),
                            current.getRightRotation())
                    );
                },
                (entity, decrementValue) -> {
                    Transformation current = entity.getTransformation();
                    entity.setTransformation(new Transformation(
                            current.getTranslation().add(0.0f, 0.0f, -decrementValue),
                            current.getLeftRotation(),
                            current.getScale(),
                            current.getRightRotation())
                    );
                },
                entity -> {
                    Transformation current = entity.getTransformation();
                    entity.setTransformation(new Transformation(
                            new Vector3f(current.getTranslation().x, current.getTranslation().y, 0.0f),
                            current.getLeftRotation(),
                            current.getScale(),
                            current.getRightRotation())
                    );
                },
                entity -> entity.getTransformation().getTranslation().z
        );

        public static final ToolAction<Display, Float> DISPLAY_LEFT_ROTATION_X = registerToolAction(Display.class, "display_left_rotation_x", "左回転 (上下 / Pitch)",
                FLOAT_DECODER,
                FLOAT_ENCODER,
                DEGREE_FORMATTER,
                (entity, value) -> {
                    Transformation current = entity.getTransformation();
                    Vector3f euler = new Vector3f();
                    current.getLeftRotation().getEulerAnglesXYZ(euler);
                    euler.x = (float) Math.toRadians(value);
                    entity.setTransformation(new Transformation(
                            current.getTranslation(),
                            new Quaternionf().rotationXYZ(euler.x, euler.y, euler.z),
                            current.getScale(),
                            current.getRightRotation())
                    );
                },
                (entity, incrementValue) -> {
                    Transformation current = entity.getTransformation();
                    entity.setTransformation(new Transformation(
                            current.getTranslation(),
                            new Quaternionf(current.getLeftRotation()).rotateX((float) Math.toRadians(incrementValue)),
                            current.getScale(),
                            current.getRightRotation())
                    );
                },
                (entity, decrementValue) -> {
                    Transformation current = entity.getTransformation();
                    entity.setTransformation(new Transformation(
                            current.getTranslation(),
                            new Quaternionf(current.getLeftRotation()).rotateX((float) Math.toRadians(-decrementValue)),
                            current.getScale(),
                            current.getRightRotation())
                    );
                },
                entity -> {
                    Transformation current = entity.getTransformation();
                    Vector3f euler = new Vector3f();
                    current.getLeftRotation().getEulerAnglesXYZ(euler);
                    euler.x = 0;
                    entity.setTransformation(new Transformation(
                            current.getTranslation(),
                            new Quaternionf().rotationXYZ(euler.x, euler.y, euler.z),
                            current.getScale(),
                            current.getRightRotation())
                    );
                },
                entity -> {
                    Vector3f euler = new Vector3f();
                    entity.getTransformation().getLeftRotation().getEulerAnglesXYZ(euler);
                    return (float) Math.toDegrees(euler.x);
                }
        );

        public static final ToolAction<Display, Float> DISPLAY_LEFT_ROTATION_Y = registerToolAction(Display.class, "display_left_rotation_y", "左回転 (左右 / Yaw)",
                FLOAT_DECODER,
                FLOAT_ENCODER,
                DEGREE_FORMATTER,
                (entity, value) -> {
                    Transformation current = entity.getTransformation();
                    Vector3f euler = new Vector3f();
                    current.getLeftRotation().getEulerAnglesXYZ(euler);
                    euler.y = (float) Math.toRadians(value);
                    entity.setTransformation(new Transformation(
                            current.getTranslation(),
                            new Quaternionf().rotationXYZ(euler.x, euler.y, euler.z),
                            current.getScale(),
                            current.getRightRotation())
                    );
                },
                (entity, incrementValue) -> {
                    Transformation current = entity.getTransformation();
                    entity.setTransformation(new Transformation(
                            current.getTranslation(),
                            new Quaternionf(current.getLeftRotation()).rotateY((float) Math.toRadians(incrementValue)),
                            current.getScale(),
                            current.getRightRotation())
                    );
                },
                (entity, decrementValue) -> {
                    Transformation current = entity.getTransformation();
                    entity.setTransformation(new Transformation(
                            current.getTranslation(),
                            new Quaternionf(current.getLeftRotation()).rotateY((float) Math.toRadians(-decrementValue)),
                            current.getScale(),
                            current.getRightRotation())
                    );
                },
                entity -> {
                    Transformation current = entity.getTransformation();
                    Vector3f euler = new Vector3f();
                    current.getLeftRotation().getEulerAnglesXYZ(euler);
                    euler.y = 0;
                    entity.setTransformation(new Transformation(
                            current.getTranslation(),
                            new Quaternionf().rotationXYZ(euler.x, euler.y, euler.z),
                            current.getScale(),
                            current.getRightRotation())
                    );
                },
                entity -> {
                    Vector3f euler = new Vector3f();
                    entity.getTransformation().getLeftRotation().getEulerAnglesXYZ(euler);
                    return (float) Math.toDegrees(euler.y);
                }
        );

        public static final ToolAction<Display, Float> DISPLAY_LEFT_ROTATION_Z = registerToolAction(Display.class, "display_left_rotation_z", "左回転 (傾き / Roll)",
                FLOAT_DECODER,
                FLOAT_ENCODER,
                DEGREE_FORMATTER,
                (entity, value) -> {
                    Transformation current = entity.getTransformation();
                    Vector3f euler = new Vector3f();
                    current.getLeftRotation().getEulerAnglesXYZ(euler);
                    euler.z = (float) Math.toRadians(value);
                    entity.setTransformation(new Transformation(
                            current.getTranslation(),
                            new Quaternionf().rotationXYZ(euler.x, euler.y, euler.z),
                            current.getScale(),
                            current.getRightRotation())
                    );
                },
                (entity, incrementValue) -> {
                    Transformation current = entity.getTransformation();
                    entity.setTransformation(new Transformation(
                            current.getTranslation(),
                            new Quaternionf(current.getLeftRotation()).rotateZ((float) Math.toRadians(incrementValue)),
                            current.getScale(),
                            current.getRightRotation())
                    );
                },
                (entity, decrementValue) -> {
                    Transformation current = entity.getTransformation();
                    entity.setTransformation(new Transformation(
                            current.getTranslation(),
                            new Quaternionf(current.getLeftRotation()).rotateZ((float) Math.toRadians(-decrementValue)),
                            current.getScale(),
                            current.getRightRotation())
                    );
                },
                entity -> {
                    Transformation current = entity.getTransformation();
                    Vector3f euler = new Vector3f();
                    current.getLeftRotation().getEulerAnglesXYZ(euler);
                    euler.z = 0;
                    entity.setTransformation(new Transformation(
                            current.getTranslation(),
                            new Quaternionf().rotationXYZ(euler.x, euler.y, euler.z),
                            current.getScale(),
                            current.getRightRotation())
                    );
                },
                entity -> {
                    Vector3f euler = new Vector3f();
                    entity.getTransformation().getLeftRotation().getEulerAnglesXYZ(euler);
                    return (float) Math.toDegrees(euler.z);
                }
        );

        public static final ToolAction<Display, Float> DISPLAY_RIGHT_ROTATION_X = registerToolAction(Display.class, "display_right_rotation_x", "右回転 (上下 / Pitch)",
                FLOAT_DECODER,
                FLOAT_ENCODER,
                DEGREE_FORMATTER,
                (entity, value) -> {
                    Transformation current = entity.getTransformation();
                    Vector3f euler = new Vector3f();
                    current.getRightRotation().getEulerAnglesXYZ(euler);
                    euler.x = (float) Math.toRadians(value);
                    entity.setTransformation(new Transformation(
                            current.getTranslation(),
                            current.getLeftRotation(),
                            current.getScale(),
                            new Quaternionf().rotationXYZ(euler.x, euler.y, euler.z))
                    );
                },
                (entity, incrementValue) -> {
                    Transformation current = entity.getTransformation();
                    entity.setTransformation(new Transformation(
                            current.getTranslation(),
                            current.getLeftRotation(),
                            current.getScale(),
                            new Quaternionf(current.getRightRotation()).rotateX((float) Math.toRadians(incrementValue)))
                    );
                },
                (entity, decrementValue) -> {
                    Transformation current = entity.getTransformation();
                    entity.setTransformation(new Transformation(
                            current.getTranslation(),
                            current.getLeftRotation(),
                            current.getScale(),
                            new Quaternionf(current.getRightRotation()).rotateX((float) Math.toRadians(-decrementValue)))
                    );
                },
                entity -> {
                    Transformation current = entity.getTransformation();
                    Vector3f euler = new Vector3f();
                    current.getRightRotation().getEulerAnglesXYZ(euler);
                    euler.x = 0;
                    entity.setTransformation(new Transformation(
                            current.getTranslation(),
                            current.getLeftRotation(),
                            current.getScale(),
                            new Quaternionf().rotationXYZ(euler.x, euler.y, euler.z))
                    );
                },
                entity -> {
                    Vector3f euler = new Vector3f();
                    entity.getTransformation().getRightRotation().getEulerAnglesXYZ(euler);
                    return (float) Math.toDegrees(euler.x);
                }
        );

        public static final ToolAction<Display, Float> DISPLAY_RIGHT_ROTATION_Y = registerToolAction(Display.class, "display_right_rotation_y", "右回転 (左右 / Yaw)",
                FLOAT_DECODER,
                FLOAT_ENCODER,
                DEGREE_FORMATTER,
                (entity, value) -> {
                    Transformation current = entity.getTransformation();
                    Vector3f euler = new Vector3f();
                    current.getRightRotation().getEulerAnglesXYZ(euler);
                    euler.y = (float) Math.toRadians(value);
                    entity.setTransformation(new Transformation(
                            current.getTranslation(),
                            current.getLeftRotation(),
                            current.getScale(),
                            new Quaternionf().rotationXYZ(euler.x, euler.y, euler.z))
                    );
                },
                (entity, incrementValue) -> {
                    Transformation current = entity.getTransformation();
                    entity.setTransformation(new Transformation(
                            current.getTranslation(),
                            current.getLeftRotation(),
                            current.getScale(),
                            new Quaternionf(current.getRightRotation()).rotateY((float) Math.toRadians(incrementValue)))
                    );
                },
                (entity, decrementValue) -> {
                    Transformation current = entity.getTransformation();
                    entity.setTransformation(new Transformation(
                            current.getTranslation(),
                            current.getLeftRotation(),
                            current.getScale(),
                            new Quaternionf(current.getRightRotation()).rotateY((float) Math.toRadians(-decrementValue)))
                    );
                },
                entity -> {
                    Transformation current = entity.getTransformation();
                    Vector3f euler = new Vector3f();
                    current.getRightRotation().getEulerAnglesXYZ(euler);
                    euler.y = 0;
                    entity.setTransformation(new Transformation(
                            current.getTranslation(),
                            current.getLeftRotation(),
                            current.getScale(),
                            new Quaternionf().rotationXYZ(euler.x, euler.y, euler.z))
                    );
                },
                entity -> {
                    Vector3f euler = new Vector3f();
                    entity.getTransformation().getRightRotation().getEulerAnglesXYZ(euler);
                    return (float) Math.toDegrees(euler.y);
                }
        );

        public static final ToolAction<Display, Float> DISPLAY_RIGHT_ROTATION_Z = registerToolAction(Display.class, "display_right_rotation_z", "右回転 (傾き / Roll)",
                FLOAT_DECODER,
                FLOAT_ENCODER,
                DEGREE_FORMATTER,
                (entity, value) -> {
                    Transformation current = entity.getTransformation();
                    Vector3f euler = new Vector3f();
                    current.getRightRotation().getEulerAnglesXYZ(euler);
                    euler.z = (float) Math.toRadians(value);
                    entity.setTransformation(new Transformation(
                            current.getTranslation(),
                            current.getLeftRotation(),
                            current.getScale(),
                            new Quaternionf().rotationXYZ(euler.x, euler.y, euler.z))
                    );
                },
                (entity, incrementValue) -> {
                    Transformation current = entity.getTransformation();
                    entity.setTransformation(new Transformation(
                            current.getTranslation(),
                            current.getLeftRotation(),
                            current.getScale(),
                            new Quaternionf(current.getRightRotation()).rotateZ((float) Math.toRadians(incrementValue)))
                    );
                },
                (entity, decrementValue) -> {
                    Transformation current = entity.getTransformation();
                    entity.setTransformation(new Transformation(
                            current.getTranslation(),
                            current.getLeftRotation(),
                            current.getScale(),
                            new Quaternionf(current.getRightRotation()).rotateZ((float) Math.toRadians(-decrementValue)))
                    );
                },
                entity -> {
                    Transformation current = entity.getTransformation();
                    Vector3f euler = new Vector3f();
                    current.getRightRotation().getEulerAnglesXYZ(euler);
                    euler.z = 0;
                    entity.setTransformation(new Transformation(
                            current.getTranslation(),
                            current.getLeftRotation(),
                            current.getScale(),
                            new Quaternionf().rotationXYZ(euler.x, euler.y, euler.z))
                    );
                },
                entity -> {
                    Vector3f euler = new Vector3f();
                    entity.getTransformation().getRightRotation().getEulerAnglesXYZ(euler);
                    return (float) Math.toDegrees(euler.z);
                }
        );

        public static final ToolAction<Entity, Double> ENTITY_POSITION_X = registerToolAction(Entity.class, "entity_position_x", "位置 (X)",
                DOUBLE_DECODER,
                DOUBLE_ENCODER,
                DOUBLE_FORMATTER,
                (entity, value) -> ((CraftEntity) entity).getHandle().teleportTo(value, entity.getY(), entity.getZ()),
                (entity, incrementValue) -> ((CraftEntity) entity).getHandle().teleportRelative(incrementValue, 0.0, 0.0),
                (entity, decrementValue) -> ((CraftEntity) entity).getHandle().teleportRelative(-decrementValue, 0.0, 0.0),
                entity -> { /* nothing to do. position is can't reset. */ },
                entity -> entity.getX()
        );

        public static final ToolAction<Entity, Double> ENTITY_POSITION_Y = registerToolAction(Entity.class, "entity_position_y", "位置 (Y)",
                DOUBLE_DECODER,
                DOUBLE_ENCODER,
                DOUBLE_FORMATTER,
                (entity, value) -> ((CraftEntity) entity).getHandle().teleportTo(entity.getX(), value, entity.getZ()),
                (entity, incrementValue) -> ((CraftEntity) entity).getHandle().teleportRelative(0.0, incrementValue, 0.0),
                (entity, decrementValue) -> ((CraftEntity) entity).getHandle().teleportRelative(0.0, -decrementValue, 0.0),
                entity -> { /* nothing to do. position is can't reset. */ },
                entity -> entity.getY()
        );

        public static final ToolAction<Entity, Double> ENTITY_POSITION_Z = registerToolAction(Entity.class, "entity_position_z", "位置 (Z)",
                DOUBLE_DECODER,
                DOUBLE_ENCODER,
                DOUBLE_FORMATTER,
                (entity, value) -> ((CraftEntity) entity).getHandle().teleportTo(entity.getX(), entity.getY(), value),
                (entity, incrementValue) -> ((CraftEntity) entity).getHandle().teleportRelative(0.0, 0.0, incrementValue),
                (entity, decrementValue) -> ((CraftEntity) entity).getHandle().teleportRelative(0.0, 0.0, -decrementValue),
                entity -> { /* nothing to do. position is can't reset. */ },
                entity -> entity.getZ()
        );

        public static final ToolAction<Entity, Float> ENTITY_ROTATION_PITCH = registerToolAction(Entity.class, "entity_rotation_pitch", "向き (XRot / Pitch)",
                FLOAT_DECODER,
                FLOAT_ENCODER,
                FLOAT_FORMATTER,
                (entity, value) -> entity.setRotation(entity.getYaw(), value),
                (entity, incrementValue) -> entity.setRotation(entity.getYaw(), entity.getPitch() + incrementValue),
                (entity, decrementValue) -> entity.setRotation(entity.getYaw(), entity.getPitch() - decrementValue),
                entity -> entity.setRotation(entity.getYaw(), 0f),
                entity -> entity.getPitch()
        );

        public static final ToolAction<Entity, Float> ENTITY_ROTATION_YAW = registerToolAction(Entity.class, "entity_rotation_yaw", "向き (YRot / Yaw)",
                FLOAT_DECODER,
                FLOAT_ENCODER,
                FLOAT_FORMATTER,
                (entity, value) -> entity.setRotation(value, entity.getPitch()),
                (entity, incrementValue) -> entity.setRotation(entity.getYaw() + incrementValue, entity.getPitch()),
                (entity, decrementValue) -> entity.setRotation(entity.getYaw() - decrementValue, entity.getPitch()),
                entity -> entity.setRotation(0f, entity.getPitch()),
                entity -> entity.getYaw()
        );

        public static final ToolAction<Slime, Integer> SLIME_SIZE = registerToolAction(Slime.class, "slime_size", "大きさ",
                INTEGER_DECODER,
                INTEGER_ENCODER,
                INTEGER_FORMATTER,
                (entity, value) -> entity.setSize(value),
                (entity, incrementValue) -> entity.setSize(Math.min(127, entity.getSize() + incrementValue)),
                (entity, decrementValue) -> entity.setSize(Math.max(1, entity.getSize() - decrementValue)),
                entity -> entity.setSize(1),
                Slime::getSize
        );

        public static final ToolAction<Shulker, Float> SHULKER_PEEK = registerToolAction(Shulker.class, "shulker_peek", "Peek",
                FLOAT_DECODER,
                FLOAT_ENCODER,
                FLOAT_FORMATTER,
                (entity, value) -> entity.setPeek(value),
                (entity, incrementValue) -> entity.setPeek(Math.min(1.0f, entity.getPeek() + incrementValue)),
                (entity, decrementValue) -> entity.setPeek(Math.max(0.0f, entity.getPeek() - decrementValue)),
                entity -> entity.setPeek(0.0f),
                Shulker::getPeek
        );

        public static void nothing() {}
    }

    public static <T,V> ToolAction<T, V> registerToolAction(Class<T> entityType, String actionKey, String displayName,
                                              Function<String, V> decoder,
                                              Function<V, String> encoder,
                                              Function<V, String> formatter,
                                              BiConsumer<T, V> applyAction,
                                              BiConsumer<T, V> incrementAction,
                                              BiConsumer<T, V> decrementAction,
                                              Consumer<T> resetAction,
                                              Function<T, V> currentValueGetter) {
        ToolAction<T, V> toolAction = new ToolAction<>(entityType, actionKey, displayName, decoder, encoder, formatter, applyAction, incrementAction, decrementAction, resetAction, currentValueGetter);
        TOOL_ACTIONS.put(actionKey, toolAction);
        return toolAction;
    }

    @SuppressWarnings("unchecked")
    public static <T,V> ToolAction<T,V> getToolAction(String actionKey) {
        return (ToolAction<T,V>) TOOL_ACTIONS.get(actionKey);
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
