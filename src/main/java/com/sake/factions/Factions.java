// 文件路径: src/main/java/com/sake/factions/Factions.java
package com.sake.factions;

import com.mojang.logging.LogUtils;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

// 【核心修复】新增这个注解！
// 它告诉 Forge 要扫描这个文件里的静态事件处理方法，并将它们注册到 FORGE 事件总线上。
@Mod.EventBusSubscriber(modid = Factions.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)

@Mod(Factions.MODID) // 这个注解保持不变，它标识了模块的入口点
public class Factions {
    public static final String MODID = "factions";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Factions() {
        // 在构造函数里，我们不再需要手动注册任何东西。
        // FactionHandler 有自己的 @Mod.EventBusSubscriber 注解，会自动注册。
        // 这个 Factions 类现在也因为上面的新注解而自动注册了。
        LOGGER.info("[Factions] 模块加载成功！");
    }

    // 这两个静态方法现在可以被正确地找到了！
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        FactionHandler.load(event.getServer().overworld());
        Factions.LOGGER.info("[Factions] 派系数据已加载。");
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        FactionHandler.setDirty();
    }
}