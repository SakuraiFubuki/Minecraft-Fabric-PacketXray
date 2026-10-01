package mod.deepseek.packetxray;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * /packetxray config 打开的配置菜单。
 * <p>
 * 26.1 起 GUI 改了写法:原版不再有 render(),一律走 extractRenderState(GuiGraphicsExtractor,...)
 * (见 {@link net.minecraft.client.gui.components.Renderable}),控件走 extractWidgetRenderState。
 * 所以这个界面不重写 render,只往 init() 里塞控件,文字用 StringWidget 摆。
 * <p>
 * <b>自适应分辨率</b>:所有坐标都写在 320x240 的虚拟画布上,init() 时按屏幕实际客户区算一个等比缩放
 * k = min(width/320, height/240) 并夹在 [MIN_SCALE, MAX_SCALE],再整体居中铺开。
 * 所以 GUI 缩放开到 4(只剩约 320x240)时是 1:1,大屏上控件等比变大而不是缩在中间一小撮。
 * 注意文字仍用原版字号,不跟着 k 放大 —— 免得小屏上文字糊成一团;布局位置是跟着 k 走的。
 * <p>
 * 数值参数一律是输入框(手打),不装滑条:「应用」时逐个校验,任何一个超出范围/不是数字就整批不生效并红字提示。
 * <p>
 * 矿石名走 {@code Block.getName()}(可翻译组件),自动跟随客户端语言;方块 id 作为次要信息显示。
 */
public final class PacketXrayConfigScreen extends Screen {

    // ===== 虚拟画布(基准布局,320x240 是原版默认 GUI 分辨率) =====
    private static final int CANVAS_W = 320;
    private static final int CANVAS_H = 240;
    private static final int CTRL_W = 260;
    private static final int LABEL_W = 40;
    private static final int ROW_H = 17;
    private static final int BTN_H = 15;
    private static final int TOP = 18;
    private static final int STATUS_H = 12;

    private static final double MIN_SCALE = 0.6D;
    private static final double MAX_SCALE = 2.5D;

    private final PacketXrayConfig cfg;

    // ===== 缩放后的实际布局参数,由 init() 算出来 =====
    private double k = 1.0D;
    private int left;
    private int top;

    private EditBox radiusInput;
    private EditBox batchInput;
    private EditBox sleepInput;
    private EditBox tracerCountInput;
    private EditBox colorInput;
    private Button prevBtn;
    private Button nextBtn;
    private StringWidget statusLine;

    /** 当前选中的矿石在名单里的下标。 */
    private int selected;

    public PacketXrayConfigScreen() {
        super(Component.literal("Packet Xray 配置"));
        this.cfg = PacketXray.config();
    }

    private List<String> ids() {
        return cfg.oreIds();
    }

    private String selectedId() {
        List<String> list = ids();
        if (list.isEmpty()) return null;
        if (selected < 0) selected = 0;
        if (selected >= list.size()) selected = list.size() - 1;
        return list.get(selected);
    }

    /** 画布坐标 → 屏幕横坐标。 */
    private int sx(int canvasX) {
        return left + (int) Math.round(canvasX * k);
    }

    /** 画布坐标 → 屏幕纵坐标。 */
    private int sy(int canvasY) {
        return top + (int) Math.round(canvasY * k);
    }

    /** 画布长度 → 屏幕长度,至少 1 像素。 */
    private int sl(int canvasLen) {
        return Math.max(1, (int) Math.round(canvasLen * k));
    }

    @Override
    protected void init() {
        k = Math.max(MIN_SCALE, Math.min(MAX_SCALE,
                Math.min(this.width / (double) CANVAS_W, this.height / (double) CANVAS_H)));
        left = (this.width - sl(CANVAS_W)) / 2;
        top = (this.height - sl(CANVAS_H)) / 2;

        int y = TOP;

        // ===== 发包参数(输入框,手打) =====
        radiusInput = addRow(y, "半径", cfg.radius, "1~64");
        y += ROW_H;
        batchInput = addRow(y, "每批", cfg.batchSize, "1~200");
        y += ROW_H;
        sleepInput = addRow(y, "休息", cfg.batchSleepMs.intValue(), "1~1000 ms");
        y += ROW_H;

        // ===== 矿石名单 =====
        StringWidget oreLabel = addRenderableWidget(new StringWidget(sx(30), sy(y), sl(CTRL_W), sl(11),
                Component.literal("矿石名单"), this.font).setMaxWidth(sl(CTRL_W)));
        y += ROW_H;

        // 改色:预设色 + 手动输入色码(两个并存)
        addRenderableWidget(Button.builder(Component.literal("预设色…"), b -> openPresets())
                .bounds(sx(30), sy(y), sl(52), sl(BTN_H)).build());
        colorInput = addRenderableWidget(new EditBox(this.font, sx(86), sy(y),
                sl(52), sl(BTN_H), Component.literal("RRGGBB")));
        colorInput.setHint(Component.literal("RRGGBB"));
        colorInput.setMaxLength(8);
        addRenderableWidget(Button.builder(Component.literal("改色"), b -> applyColor())
                .bounds(sx(142), sy(y), sl(44), sl(BTN_H)).build());
        addRenderableWidget(Button.builder(Component.literal("删除"), b -> removeOre())
                .bounds(sx(190), sy(y), sl(46), sl(BTN_H)).build());
        addRenderableWidget(Button.builder(Component.literal("恢复默认"), b -> {
            cfg.resetOres();
            selected = 0;
            syncSelection();
        }).bounds(sx(240), sy(y), sl(50), sl(BTN_H)).build());
        y += ROW_H;

        prevBtn = addRenderableWidget(Button.builder(Component.literal("<"), b -> step(-1))
                .bounds(sx(30), sy(y), sl(18), sl(BTN_H)).build());
        nextBtn = addRenderableWidget(Button.builder(Component.literal(">"), b -> step(1))
                .bounds(sx(50), sy(y), sl(18), sl(BTN_H)).build());
        // 「添加方块…」整行铺满右侧(两个翻页小按钮只到画布 x=68)
        addRenderableWidget(Button.builder(Component.literal("添加方块…"), b -> openPicker())
                .bounds(sx(72), sy(y), sl(218), sl(BTN_H)).build());
        y += ROW_H;

        // ===== 渲染开关(立即生效,不用点应用) =====
        addRenderableWidget(Button.builder(toggleLabel("填充方块", cfg.drawBoxes), b -> {
            cfg.drawBoxes = !cfg.drawBoxes;
            b.setMessage(toggleLabel("填充方块", cfg.drawBoxes));
        }).bounds(sx(30), sy(y), sl(CTRL_W), sl(BTN_H)).build());
        y += ROW_H;

        addRenderableWidget(Button.builder(toggleLabel("准星射线", cfg.drawTracer), b -> {
            cfg.drawTracer = !cfg.drawTracer;
            b.setMessage(toggleLabel("准星射线", cfg.drawTracer));
        }).bounds(sx(30), sy(y), sl(CTRL_W), sl(BTN_H)).build());
        y += ROW_H;

        tracerCountInput = addRow(y, "射线数", cfg.tracerCount, "1~64");
        y += ROW_H;

        // 状态行:显示当前选中项 / 校验错误,固定在画布底部。
        // 注意 StringWidget 的 (x,y,w,h,msg,font) 构造器会按文字宽度自动撑宽,必须再 setMaxWidth 压回控件宽度,
        // 否则长矿石名会把控件撑到屏幕外。
        statusLine = addRenderableWidget(new StringWidget(sx(30), sy(CANVAS_H - 34),
                sl(CTRL_W), sl(STATUS_H - 1), Component.empty(), this.font)
                .setMaxWidth(sl(CTRL_W)));

        // 底部按钮同样落在画布里,跟着 k 走
        int bw = 74;
        addRenderableWidget(Button.builder(Component.literal("应用"), b -> commit())
                .bounds(sx(CANVAS_W / 2 - bw - 3), sy(CANVAS_H - 20), sl(bw), sl(BTN_H)).build());
        addRenderableWidget(Button.builder(Component.literal("完成"), b -> {
            if (commit()) this.onClose();
        }).bounds(sx(CANVAS_W / 2 + 3), sy(CANVAS_H - 20), sl(bw), sl(BTN_H)).build());

        syncSelection();
    }

    private static Component toggleLabel(String name, boolean on) {
        return Component.literal(name + ":" + (on ? "开" : "关"));
    }

    /** 一行「标签 + 数值输入框」,返回这个输入框。 */
    private EditBox addRow(int y, String label, int initial, String hint) {
        addRenderableWidget(new StringWidget(sx(30), sy(y + 4), sl(LABEL_W), sl(11),
                Component.literal(label), this.font).setMaxWidth(sl(LABEL_W)));
        EditBox box = addRenderableWidget(new EditBox(this.font, sx(30 + LABEL_W), sy(y),
                sl(CTRL_W - LABEL_W), sl(BTN_H), Component.literal(label)));
        box.setValue(String.valueOf(initial));
        box.setHint(Component.literal(hint));
        box.setMaxLength(6);
        return box;
    }

    private void step(int delta) {
        List<String> list = ids();
        if (list.isEmpty()) return;
        selected = Math.floorMod(selected + delta, list.size());
        syncSelection();
    }

    /** 把当前选中项回填到改色输入框和状态行。 */
    private void syncSelection() {
        String id = selectedId();
        boolean has = id != null;
        boolean many = ids().size() > 1;
        if (prevBtn != null) prevBtn.active = has && many;
        if (nextBtn != null) nextBtn.active = has && many;
        if (colorInput != null) colorInput.setValue(has ? PacketXrayConfig.toHex(cfg.colorOfId(id)) : "");

        if (!has) {
            setStatus("名单为空 —— 上面填方块 id 后点「添加」", null);
            return;
        }
        // 方块名是可翻译组件,跟随客户端语言;id 放后面做次要信息
        setStatus("[" + (selected + 1) + "/" + ids().size() + "] "
                + PacketXrayConfig.displayName(id) + "  (" + id + ")", null);
    }

    private void setStatus(String text, ChatFormatting color) {
        if (statusLine == null) return;
        Component c = Component.literal(text);
        statusLine.setMessage(color == null ? c : c.copy().withStyle(color));
    }

    /**
     * 校验并应用所有数值输入框。任何一个不合法就整批不生效,红字报错并返回 false
     * —— 免得「半径改对了、每批打错了」时一半生效一半没生效。
     */
    private boolean commit() {
        Integer radius;
        Integer batch;
        Integer sleep;
        Integer tracers;
        try {
            radius = parse(radiusInput, "半径", 1, 64);
            batch = parse(batchInput, "每批包数", 1, 200);
            sleep = parse(sleepInput, "休息", 1, 1000);
            tracers = parse(tracerCountInput, "射线数", 1, 64);
        } catch (IllegalArgumentException e) {
            setStatus(e.getMessage(), ChatFormatting.RED);
            return false;
        }
        cfg.radius = radius;
        cfg.batchSize = batch;
        cfg.batchSleepMs = sleep.longValue();
        cfg.tracerCount = tracers;
        PacketXray.applyConfig();
        setStatus("已应用:半径 " + radius + " 每批 " + batch + " 包 休息 " + sleep + " ms 射线 " + tracers + " 条", ChatFormatting.GREEN);
        return true;
    }

    private static int parse(EditBox box, String label, int min, int max) {
        String raw = box.getValue().trim();
        if (raw.isEmpty()) throw new IllegalArgumentException(label + "不能为空");
        int v;
        try {
            v = Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(label + "要填整数,现在是「" + raw + "」");
        }
        if (v < min || v > max) throw new IllegalArgumentException(label + "要在 " + min + "~" + max + " 之间");
        return v;
    }

    /** 打开方块选择菜单(「添加方块…」按钮)。回来后自动选中新加的那块。 */
    private void openPicker() {
        PacketXray.openBlockPicker(this);
    }

    /** 打开预设色菜单(「预设色…」按钮)。 */
    private void openPresets() {
        PacketXray.openColorPresets(this, selectedId());
    }

    /** 从选择菜单回来时调用:定位到刚加的方块上。 */
    public void selectOre(String id) {
        int idx = ids().indexOf(id);
        if (idx >= 0) selected = idx;
        syncSelection();
    }

    private void removeOre() {
        String id = selectedId();
        if (id == null) return;
        cfg.removeOre(id);
        syncSelection();
    }

    private void applyColor() {
        String id = selectedId();
        if (id == null) return;
        int parsed = PacketXrayConfig.parseColor(colorInput.getValue(), cfg.colorOfId(id));
        if (parsed < 0) {
            setStatus("颜色要填 RRGGBB(如 00FF00)", ChatFormatting.RED);
            return;
        }
        cfg.setOreColor(id, parsed);
        syncSelection();
    }

    @Override
    public void onClose() {
        // 关界面即存盘:菜单里的改动全部落地
        PacketXray.saveConfig();
        // 传 null 关掉界面回到游戏(拿不到"上一个界面":26.2 既无公开字段也无 getter)
        ScreenAccess.set(null);
    }

    @Override
    public boolean isPauseScreen() {
        // 单机里不暂停,方便对着游戏调参数
        return false;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        g.centeredText(this.font, this.title, this.width / 2, sy(6), 0xFFFFFFFF);
        super.extractRenderState(g, mouseX, mouseY, partialTick);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        // 菜单要在游戏里打开,不暂停也不换背景,只压一层半透明底免得好坏看不清
        this.extractTransparentBackground(g);
    }
}
