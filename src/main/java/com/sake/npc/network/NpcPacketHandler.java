package com.sake.npc.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * 管理我们所有自定义数据包的类。
 */
public class NpcPacketHandler {
    // 协议版本，如果以后修改了数据包结构，需要更改此版本
    private static final String PROTOCOL_VERSION = "1";

    // 创建一个网络频道，用于在客户端和服务器之间传输我们的数据包
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("npc", "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    /**
     * 注册所有的数据包。这个方法需要在模组初始化时被调用。
     */
    public static void register() {
        int id = 0;
        // 注册我们的第一个数据包
        CHANNEL.registerMessage(
                id++, // 每个数据包都需要一个唯一的ID
                ClientboundDisplayItemActivationPacket.class,
                ClientboundDisplayItemActivationPacket::encode,
                ClientboundDisplayItemActivationPacket::decode,
                ClientboundDisplayItemActivationPacket::handle
        );
    }
}