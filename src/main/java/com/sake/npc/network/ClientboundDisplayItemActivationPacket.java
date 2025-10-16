package com.sake.npc.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 一个从服务器发送到客户端的数据包，
 * 用于指令客户端播放一个物品在屏幕上激活的动画（类似不死图腾）。
 */
public class ClientboundDisplayItemActivationPacket {

    private final ItemStack itemStack;

    // 构造函数：创建数据包时传入要显示的物品
    public ClientboundDisplayItemActivationPacket(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    // 解码器：从网络数据流中读取物品信息，重新构建数据包
    public static ClientboundDisplayItemActivationPacket decode(FriendlyByteBuf buf) {
        return new ClientboundDisplayItemActivationPacket(buf.readItem());
    }

    // 编码器：将数据包中的物品信息写入到网络数据流中
    public void encode(FriendlyByteBuf buf) {
        buf.writeItem(this.itemStack);
    }

    // 处理器：当客户端收到这个数据包后，执行这里的逻辑
    public static void handle(ClientboundDisplayItemActivationPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // DistExecutor确保这段代码只在客户端运行，防止在服务端执行导致崩溃
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                // 【核心代码】调用Minecraft游戏渲染器，显示物品激活的动画
                Minecraft.getInstance().gameRenderer.displayItemActivation(msg.itemStack);
            });
        });
        ctx.get().setPacketHandled(true);
    }
}