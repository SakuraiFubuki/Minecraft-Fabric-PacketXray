package mod.deepseek.packetxray;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/**
 * 切换界面。
 * <p>
 * 本 mod 只支持 26.2,所以直接用 {@code Minecraft.setScreenAndShow}。
 * <p>
 * 为什么不用 {@code setScreen}:26.2 已经把 {@code Minecraft.setScreen(Screen)} 删掉了
 * (26.1.2 还有),只留 {@code setScreenAndShow}。而 {@code setScreenAndShow} 除换屏外
 * 还会走一次 {@code renderFrame} 完成 {@code Screen.init()} —— 少了这一步,
 * Fabric 的 screen-api 会在渲染时抛
 * {@code IllegalStateException: The current screen ... has not been correctly initialised}。
 * 所以换屏统一走这里,别再改成直接操作字段。
 */
final class ScreenAccess {

    private ScreenAccess() {
    }

    /**
     * 切换界面。传 null 表示关掉当前界面回到游戏。
     * <p>
     * 队列里的元素可能是 null(表示"关掉界面"),不能当成"没有待办"跳过。
     */
    static void set(Screen screen) {
        Minecraft mc = Minecraft.getInstance();
        mc.setScreenAndShow(screen);
        PacketXray.LOGGER.info("已换屏 -> {}", screen == null ? "(关闭界面)" : screen.getClass().getSimpleName());
    }
}
