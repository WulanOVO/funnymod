package yibo.funnymod.item;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.level.block.DispenserBlock;
import yibo.funnymod.Funnymod;

public class ModItems {
    public static final ResourceKey<Item> MIXED_SNOWBALL_KEY =
            ResourceKey.create(Registries.ITEM, Funnymod.id("mixed_snowball"));

    public static final Item MIXED_SNOWBALL = Registry.register(
            BuiltInRegistries.ITEM,
            MIXED_SNOWBALL_KEY,
            new MixedSnowballItem(new Item.Properties()
                    .setId(MIXED_SNOWBALL_KEY)
                    .stacksTo(16))
    );

    public static final ResourceKey<Item> SADDLE_ELYTRA_KEY =
            ResourceKey.create(Registries.ITEM, Funnymod.id("saddle_elytra"));

    /** 鞍鞘：马鞍 + 鞘翅合成。放在鞍槽后既是鞍（可骑控），又赋予坐骑滑翔能力。 */
    public static final Item SADDLE_ELYTRA = Registry.register(
            BuiltInRegistries.ITEM,
            SADDLE_ELYTRA_KEY,
            new Item(new Item.Properties()
                    .setId(SADDLE_ELYTRA_KEY)
                    .stacksTo(1)
                    .durability(432)
                    .rarity(Rarity.EPIC)
                    // GLIDER 组件：LivingEntity.canGlide() 遍历装备槽检测，鞍槽中的鞍鞘让坐骑获得滑翔资格，
                    // 滑翔物理（travelFallFlying）与耐久消耗（updateFallFlying）均由原版鞘翅机制自动接管
                    .component(DataComponents.GLIDER, Unit.INSTANCE)
                    .component(DataComponents.EQUIPPABLE, buildSaddleElytraEquippable())
                    .repairable(Items.PHANTOM_MEMBRANE))
    );

    /**
     * 构建鞍鞘的 EQUIPPABLE 组件：
     * 槽位为鞍槽，可装备实体与原版鞍一致（EntityTypeTags#CAN_EQUIP_SADDLE，即一切可上鞍的动物），
     * 对坐骑右键可直接装备、可用剪刀卸下，装备后在坐骑身上渲染马鞍外观。
     */
    private static Equippable buildSaddleElytraEquippable() {
        HolderGetter<EntityType<?>> entityGetter =
                BuiltInRegistries.acquireBootstrapRegistrationLookup(BuiltInRegistries.ENTITY_TYPE);
        return Equippable.builder(EquipmentSlot.SADDLE)
                .setEquipSound(SoundEvents.ARMOR_EQUIP_ELYTRA)
                .setAsset(EquipmentAssets.SADDLE)
                .setAllowedEntities(entityGetter.getOrThrow(EntityTypeTags.CAN_EQUIP_SADDLE))
                .setEquipOnInteract(true)
                .setCanBeSheared(true)
                .setShearingSound(SoundEvents.SADDLE_UNEQUIP)
                .setDamageOnHurt(false)
                .build();
    }

    public static void initialize() {
        // 注册发射器行为，让混合雪球能被发射器射出
        DispenserBlock.registerProjectileBehavior(MIXED_SNOWBALL);
    }
}
