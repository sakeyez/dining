package com.sake.dining;

import com.mojang.logging.LogUtils;
import net.minecraft.commands.Commands;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import org.slf4j.Logger;

// 创造栏相关
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;

@Mod(Dining.MODID)
public class Dining {
    public static final String MODID = "dining";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Dining() {
        // 注册物品
        DiningItems.register();
        DiningCreativeTab.register();

        // **初始化时加载配置**
        FeedingConfig.load();
        LOGGER.info("[Dining] 配置文件已加载完成。");

        // 下面这句只在需要把物品加到其他原版栏位时用
        // FMLJavaModLoadingContext.get().getModEventBus().addListener(this::addCreative);

        // Forge 事件总线注册
        MinecraftForge.EVENT_BUS.register(this);
    }

    // 如果你希望继续把物品放到原版食物栏，可以保留这个函数并上面的addListener；如果不用就删掉
    /*
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTab().equals(CreativeModeTabs.FOOD_AND_DRINKS)) {
            event.accept(DiningItems.ZOMBIE_TOKEN.get());
            event.accept(DiningItems.SKELETON_TOKEN.get());
            event.accept(DiningItems.CREEPER_TOKEN.get());
            event.accept(DiningItems.ENDERMAN_TOKEN.get());
            event.accept(DiningItems.SLIME_TOKEN.get());
            event.accept(DiningItems.BLAZE_TOKEN.get());
        }
    }
    */

    // 给怪物添加吃东西的 Goal
    @SubscribeEvent
    public void onEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof Mob mob)) return;
        if (EatCakeBlockGoal.isSupportedEater(mob)) {
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
                            // **重新加载配置**
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
