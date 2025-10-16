package com.sake.dining;

import com.mojang.logging.LogUtils;
import net.minecraft.commands.Commands;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import org.slf4j.Logger;

@Mod(Dining.MODID)
public class Dining {
    public static final String MODID = "dining";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Dining() {
        DiningItems.register();
        DiningCreativeTab.register();

        FeedingConfig.load();
        LOGGER.info("[Dining] 配置文件已加载完成。");

        MinecraftForge.EVENT_BUS.register(this);
    }

    // 给怪物添加吃东西的 Goal
    @SubscribeEvent
    public void onEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof Mob mob)) return;

        // 【核心修正】使用 FeedingConfig.getFoodBlocks 来判断是否是支持的生物
        // 如果能从配置中找到该生物的食物列表（即使是空的），就说明它是被支持的。
        // 我们检查列表是否为非空，来决定是否添加AI。
        if (!FeedingConfig.getFoodBlocks(mob).isEmpty()) {
            mob.goalSelector.addGoal(1, new EatCakeBlockGoal(mob));
        }
    }

    // 注册命令
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("reload_dining")
                        .requires(cs -> cs.hasPermission(2))
                        .executes(ctx -> {
                            FeedingConfig.load();
                            ctx.getSource().sendSuccess(
                                    () -> net.minecraft.network.chat.Component.literal("Dining配置已重载!"),
                                    false
                            );
                            LOGGER.info("[Dining] 配置文件已通过命令重载。");
                            return 1;
                        })
        );
    }
}