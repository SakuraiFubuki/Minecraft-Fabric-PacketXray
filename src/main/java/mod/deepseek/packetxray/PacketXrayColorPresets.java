package mod.deepseek.packetxray;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * 改色用的预设色选择菜单:一排常用颜色,点一下就把选中矿石改成那个色。
 * <p>
 * 想自己调色的话不用进这里 —— 主菜单那行仍保留 RRGGBB 输入框,两边并存。
 * <p>
 * 颜色存 0xAARRGGBB,透明度沿用矿石原来的(透视要半透明,不在这里改)。
 */
public final class PacketXrayColorPresets extends Screen {

    private static final int CANVAS_W = 320;
    /**
     * 画布高要装得下:标题(8)+ 说明(30)+ 3 行网格(48→114)+ 状态行(116→127)+ 返回按钮(y104 起算得留到 150+)。
     * 原来写 150 时网格就到了 y=114、状态行 y=116,返回按钮 y=126(h18)→144,
     * 算下来在 320x240 上状态行会和按钮相交,所以留到 170。
     */
    private static final int CANVAS_H = 170;
    private static final int CTRL_W = 260;
    private static final double MIN_SCALE = 0.6D;
    private static final double MAX_SCALE = 2.5D;

    /** 预设色(不含 alpha):{名称, RGB}。 */
    private static final Object[][] PRESETS = {
            {"亮绿", 0x00FF00}, {"翠绿", 0x2ECC71}, {"青色", 0x00FFFF},
            {"天蓝", 0x3FA9F5}, {"宝蓝", 0x2A4BE0}, {"紫色", 0xB04BE0},
            {"品红", 0xFF00FF}, {"红色", 0xFF3030}, {"橙色", 0xFF8C00},
            {"琥珀", 0xFFC300}, {"黄色", 0xFFFF00}, {"白色", 0xFFFFFF},
    };

    private static final int COLS = 4;
    private static final int CELL_W = 62;
    private static final int CELL_H = 22;

    private final Screen returnTo;
    private final String oreId;

    private double k = 1.0D;
    private int left;
    private int top;
    private StringWidget statusLine;

    public PacketXrayColorPresets(Screen returnTo, String oreId) {
        super(Component.literal("选择预设颜色"));
        this.returnTo = returnTo;
        this.oreId = oreId;
    }

    private int sx(int canvasX) {
        return left + (int) Math.round(canvasX * k);
    }

    private int sy(int canvasY) {
        return top + (int) Math.round(canvasY * k);
    }

    private int sl(int canvasLen) {
        return Math.max(1, (int) Math.round(canvasLen * k));
    }

    @Override
    protected void init() {
        k = Math.max(MIN_SCALE, Math.min(MAX_SCALE,
                Math.min(this.width / (double) CANVAS_W, this.height / (double) CANVAS_H)));
        left = (this.width - sl(CANVAS_W)) / 2;
        top = (this.height - sl(CANVAS_H)) / 2;

        PacketXrayConfig cfg = PacketXray.config();
        String name = oreId == null ? "(未选中)" : PacketXrayConfig.displayName(oreId);
        addRenderableWidget(new StringWidget(sx(30), sy(30), sl(CTRL_W), sl(11),
                Component.literal("给「" + name + "」选一个颜色"), this.font)
                .setMaxWidth(sl(CTRL_W)));

        // 预设色按钮摆成网格;每个按钮左边那一小块颜色在 extractRenderState 里画
        int gridTop = 48;
        for (int i = 0; i < PRESETS.length; i++) {
            final int rgb = (Integer) PRESETS[i][1];
            final String label = (String) PRESETS[i][0];
            int col = i % COLS;
            int row = i / COLS;
            int x = 30 + col * CELL_W;
            int y = gridTop + row * CELL_H;
            addRenderableWidget(Button.builder(Component.literal("   " + label), b -> apply(rgb))
                    .bounds(sx(x), sy(y), sl(CELL_W - 4), sl(CELL_H - 4)).build());
        }

        int gridBottom = gridTop + ((PRESETS.length + COLS - 1) / COLS) * CELL_H;

        statusLine = addRenderableWidget(new StringWidget(sx(30), sy(gridBottom + 2), sl(CTRL_W), sl(11),
                Component.empty(), this.font).setMaxWidth(sl(CTRL_W)));
        PacketXrayConfig c = PacketXray.config();
        setStatus(oreId == null
                ? "先在主菜单里选中一块矿石"
                : "当前:" + PacketXrayConfig.toHex(c.colorOfId(oreId))
                        + "  (想自己调就在主菜单填 RRGGBB)");

        addRenderableWidget(Button.builder(Component.literal("返回"), b -> this.onClose())
                .bounds(sx(CANVAS_W / 2 - 37), sy(CANVAS_H - 24), sl(74), sl(18)).build());
    }

    private void setStatus(String s) {
        if (statusLine != null) statusLine.setMessage(Component.literal(s));
    }

    /** 套用预设色:保留原颜色的 alpha。 */
    private void apply(int rgb) {
        if (oreId == null) {
            setStatus("没有选中的矿石");
            return;
        }
        PacketXrayConfig cfg = PacketXray.config();
        int alpha = cfg.colorOfId(oreId) & 0xFF000000;
        cfg.setOreColor(oreId, alpha | (rgb & 0xFFFFFF));
        PacketXray.saveConfig();
        PacketXray.queueScreen(returnTo);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        g.centeredText(this.font, this.title, this.width / 2, sy(8), 0xFFFFFFFF);
        super.extractRenderState(g, mouseX, mouseY, partialTick);

        // 每个预设按钮左边画一块该颜色的实心方块。
        // 不画描边:之前加了黑色 outline,渲染出来是一圈突兀的黑框,去掉更干净。
        int gridTop = 48;
        for (int i = 0; i < PRESETS.length; i++) {
            int rgb = (Integer) PRESETS[i][1];
            int col = i % COLS;
            int row = i / COLS;
            int x = 30 + col * CELL_W;
            int y = gridTop + row * CELL_H;
            int bx = sx(x + 3);
            int by = sy(y + 4);
            int bw = sl(10);
            int bh = sl(CELL_H - 12);
            g.fill(bx, by, bx + bw, by + bh, 0xFF000000 | rgb);
        }
    }

    @Override
    public void onClose() {
        PacketXray.queueScreen(returnTo);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        this.extractTransparentBackground(g);
    }
}
