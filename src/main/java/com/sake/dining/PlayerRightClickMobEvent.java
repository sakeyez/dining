package com.sake.dining;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 这个类处理玩家潜行右键与生物交换物品和装备的功能。
 * 它会自动被Forge的事件总线加载。
 */
@Mod.EventBusSubscriber(modid = Dining.MODID)
public class PlayerRightClickMobEvent {

    /**
     * 当玩家与实体交互时触发此事件。
     * @param event 交互事件对象
     */
    @SubscribeEvent
    public static void onPlayerRightClickMob(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        Level level = event.getLevel();
        InteractionHand hand = event.getHand();

        // 1. 条件检查：确保操作在服务器端、玩家正在潜行且使用的是主手。
        if (level.isClientSide() || hand != InteractionHand.MAIN_HAND || !player.isShiftKeyDown()) {
            return;
        }

        // 2. 目标检查：确保目标是一个生物（LivingEntity）。
        if (!(event.getTarget() instanceof LivingEntity target)) {
            return;
        }

        ItemStack playerStack = player.getItemInHand(hand).copy();
        if (playerStack.isEmpty()) {
            return;
        }

        // 3. 判断要交换的槽位
        EquipmentSlot slotToSwap;
        if (playerStack.getItem() instanceof ArmorItem armorItem) {
            // 如果玩家手持的是盔甲，则获取其对应的槽位
            slotToSwap = armorItem.getEquipmentSlot();
        } else {
            // 否则，默认为主手
            slotToSwap = EquipmentSlot.MAINHAND;
        }

        // 4. 特殊规则：检查目标是否是你的自定义NPC
        // 我们通过检查实体的类所在的包名来判断
        boolean isCustomNpc = target.getClass().getPackageName().startsWith("com.sake.npc");

        // 如果是自定义NPC，并且玩家试图交换主手物品，则直接阻止操作。
        if (isCustomNpc && slotToSwap == EquipmentSlot.MAINHAND) {
            return;
        }

        // 5. 执行交换
        ItemStack targetStack = target.getItemBySlot(slotToSwap).copy();

        target.setItemSlot(slotToSwap, playerStack);
        player.setItemInHand(hand, targetStack);

        // 6. 添加反馈
        // 给玩家一个短暂的冷却时间，防止误操作快速交换
        player.getCooldowns().addCooldown(player.getItemInHand(hand).getItem(), 10);
        // 播放一个清脆的装备声
        player.playSound(SoundEvents.ARMOR_EQUIP_GENERIC, 1.0F, 1.0F);

        // 7. 取消原始事件，防止触发其他默认交互（如打开交易界面）。
        event.setCanceled(true);
    }
}
