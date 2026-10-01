package mod.deepseek.packetxray;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

/**
 * 客户端入口:注册 /packetxray 命令,断线时停掉。
 * 用法:/packetxray [半径] [每批包数] [休息时间ms],再敲一次关闭。
 */
public class PacketXrayMod implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        PacketXray.init();
        // 只注册 /packetxray 一个主级命令,help / config / 半径参数全部挂在它下面
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(build("packetxray")));

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> PacketXray.stop());
    }

    /** 命令树:/packetxray 开关,/packetxray help 用法,/packetxray config 菜单,/packetxray <半径> [每批] [休息ms]。 */
    private static LiteralArgumentBuilder<FabricClientCommandSource> build(String name) {
        return ClientCommands.literal(name)
                .executes(ctx -> {
                    PacketXray.LOGGER.info("/{} 回调:开关", name);
                    PacketXray.toggle();
                    return 1;
                })
                .then(ClientCommands.literal("help")
                        .executes(ctx -> {
                            PacketXray.LOGGER.info("/{} help 回调", name);
                            PacketXray.help();
                            return 1;
                        }))
                .then(ClientCommands.literal("config")
                        .executes(ctx -> {
                            PacketXray.LOGGER.info("/{} config 回调", name);
                            PacketXray.openConfig();
                            return 1;
                        }))
                .then(ClientCommands.argument("radius", IntegerArgumentType.integer(1, 64))
                        .executes(ctx -> run(ctx, PacketXray.getBatchSize(), PacketXray.getBatchSleepMs()))
                        .then(ClientCommands.argument("batch", IntegerArgumentType.integer(1))
                                .executes(ctx -> run(ctx, IntegerArgumentType.getInteger(ctx, "batch"), PacketXray.getBatchSleepMs()))
                                .then(ClientCommands.argument("sleepMs", LongArgumentType.longArg(1))
                                        .executes(ctx -> run(ctx, IntegerArgumentType.getInteger(ctx, "batch"),
                                                LongArgumentType.getLong(ctx, "sleepMs"))))));
    }

    private static int run(CommandContext<FabricClientCommandSource> ctx, int batchSize, long batchSleepMs) {
        PacketXray.configure(IntegerArgumentType.getInteger(ctx, "radius"), batchSize, batchSleepMs);
        PacketXray.toggle();
        return 1;
    }
}
