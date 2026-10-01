package mod.deepseek.packetxray;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * 添加矿石用的选择菜单:上面搜索框,下面一页方块,点一下就加进透视名单。
 * <p>
 * 方块名走 {@code Block.getName()}(可翻译组件),所以跟着客户端语言走;方块 id 作为次要信息显示。
 * 搜索匹配"译名"或"id"任意一边,不分大小写,也接受不带命名空间的写法(如 {@code iron_ore})。
 * <p>
 * 一次列出全部方块太多,所以自己分页,不依赖原版的列表控件 ——
 * 26.1 起列表控件的接口也改过,自己画更省事也更可控。
 */
public final class PacketXrayBlockPicker extends Screen {

    private static final int CANVAS_W = 320;
    private static final int CANVAS_H = 250;
    private static final int CTRL_W = 260;
    private static final int ROW_H = 15;
    private static final int TOP = 20;
    private static final double MIN_SCALE = 0.6D;
    private static final double MAX_SCALE = 2.5D;
    private static final int PER_PAGE = 9;

    /** 条目前面的小方块:未加入名单为红,已加入为绿。 */
    private static final int COLOR_NOT_ADDED = 0xFFFF4040;
    private static final int COLOR_ADDED = 0xFF40E040;

    private final Screen returnTo;

    private double k = 1.0D;
    private int left;
    private int top;

    private EditBox searchBox;
    private StringWidget statusLine;
    private Button prevBtn;
    private Button nextBtn;

    /** 候选方块(全量,只在第一次用时建一次)。 */
    private static List<Block> allBlocks;
    /** 按搜索词过滤后的结果。 */
    private List<Block> filtered = List.of();
    private int page;

    public PacketXrayBlockPicker(Screen returnTo) {
        super(Component.literal("选择要透视的方块"));
        this.returnTo = returnTo;
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

    /**
     * 全量方块表,首次使用时建好,按<b>方块 id</b> 升序。
     * <p>
     * 注意:原版注册表的迭代顺序<b>不是</b> id 顺序(实测出现 {@code copper_torch} 排在
     * {@code copper_bars} 前面),所以必须显式按 id 排一次,不能指望注册表本身有序。
     * id 是"命名空间:路径",所以 minecraft 会排在模组前面,模组之间按命名空间排。
     * <p>
     * 顺带剔除 {@code minecraft:air} —— 它不是方块,放进透视名单没有意义。
     */
    private static List<Block> allBlocks() {
        if (allBlocks == null) {
            List<Block> list = new ArrayList<>();
            for (Block b : BuiltInRegistries.BLOCK) {
                Identifier id = BuiltInRegistries.BLOCK.getKey(b);
                if (id == null) continue;
                if (id.getNamespace().equals("minecraft") && id.getPath().equals("air")) continue;
                list.add(b);
            }
            list.sort(Comparator.comparing(b -> {
                Identifier id = BuiltInRegistries.BLOCK.getKey(b);
                return id == null ? "" : id.toString();
            }));
            allBlocks = List.copyOf(list);
        }
        return allBlocks;
    }

    private int pageCount() {
        return Math.max(1, (filtered.size() + PER_PAGE - 1) / PER_PAGE);
    }

    @Override
    protected void init() {
        k = Math.max(MIN_SCALE, Math.min(MAX_SCALE,
                Math.min(this.width / (double) CANVAS_W, this.height / (double) CANVAS_H)));
        left = (this.width - sl(CANVAS_W)) / 2;
        top = (this.height - sl(CANVAS_H)) / 2;

        int y = TOP;

        searchBox = addRenderableWidget(new EditBox(this.font, sx(30), sy(y),
                sl(CTRL_W), sl(ROW_H - 2), Component.literal("搜索")));
        searchBox.setHint(Component.literal("搜索方块名或 id,如 铁 / iron_ore"));
        searchBox.setMaxLength(64);
        searchBox.setResponder(s -> {
            page = 0;
            refilter();
        });
        y += ROW_H + 2;

        // 列表区域高度预留出来,条目在 extractRenderState 里按行画
        y += PER_PAGE * ROW_H;

        prevBtn = addRenderableWidget(Button.builder(Component.literal("< 上一页"), b -> {
            if (page > 0) page--;
            syncButtons();
        }).bounds(sx(30), sy(y + 2), sl(74), sl(16)).build());
        nextBtn = addRenderableWidget(Button.builder(Component.literal("下一页 >"), b -> {
            if (page < pageCount() - 1) page++;
            syncButtons();
        }).bounds(sx(30 + CTRL_W - 74), sy(y + 2), sl(74), sl(16)).build());
        y += 20;

        statusLine = addRenderableWidget(new StringWidget(sx(30), sy(y), sl(CTRL_W), sl(11),
                Component.empty(), this.font).setMaxWidth(sl(CTRL_W)));

        addRenderableWidget(Button.builder(Component.literal("取消"), b -> this.onClose())
                .bounds(sx(CANVAS_W / 2 - 77), sy(CANVAS_H - 22), sl(74), sl(18)).build());

        refilter();
        setInitialFocus(searchBox);
    }

    private void refilter() {
        String q = searchBox == null ? "" : searchBox.getValue().trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) {
            filtered = allBlocks();
        } else {
            String bare = q.startsWith("minecraft:") ? q.substring("minecraft:".length()) : q;
            List<Block> hit = new ArrayList<>();
            for (Block b : allBlocks()) {
                Identifier id = BuiltInRegistries.BLOCK.getKey(b);
                String path = id == null ? "" : id.getPath();
                String full = id == null ? "" : id.toString();
                String name = b.getName().getString();
                if (name.toLowerCase(Locale.ROOT).contains(q)
                        || full.contains(q)
                        || path.contains(bare)) {
                    hit.add(b);
                }
            }
            filtered = List.copyOf(hit);
        }
        if (page >= pageCount()) page = pageCount() - 1;
        syncButtons();
    }

    private void syncButtons() {
        if (prevBtn != null) prevBtn.active = page > 0;
        if (nextBtn != null) nextBtn.active = page < pageCount() - 1;
        if (statusLine != null) {
            statusLine.setMessage(Component.literal("共 " + filtered.size() + " 个方块   第 "
                    + (page + 1) + "/" + pageCount() + " 页   点条目即添加"));
        }
    }

    /** 列表区域(画布里)的矩形。 */
    private int listTopCanvas() {
        return TOP + ROW_H + 2;
    }

    private void clickRow(int indexOnPage) {
        int idx = page * PER_PAGE + indexOnPage;
        if (idx < 0 || idx >= filtered.size()) return;
        Block block = filtered.get(idx);
        Identifier id = BuiltInRegistries.BLOCK.getKey(block);
        if (id == null) return;
        PacketXrayConfig cfg = PacketXray.config();
        int color = cfg.oreIds().isEmpty() ? 0x9900FF00 : cfg.colorOfId(cfg.oreIds().get(0));
        if (cfg.addOre(id.toString(), color)) {
            PacketXray.saveConfig();
            // 回配置菜单:走排队换屏,别在当前这轮点击处理里直接拆界面
            PacketXray.queueScreen(returnTo);
        }
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubled) {
        // 先给控件处理(搜索框、按钮),没吃掉再当成点列表条目
        if (super.mouseClicked(event, doubled)) return true;
        double mx = event.x();
        double my = event.y();
        for (int i = 0; i < PER_PAGE; i++) {
            int idx = page * PER_PAGE + i;
            if (idx >= filtered.size()) break;
            int ry = sy(listTopCanvas() + i * ROW_H);
            int rh = sl(ROW_H);
            if (mx >= sx(30) && mx <= sx(30 + CTRL_W) && my >= ry && my < ry + rh) {
                clickRow(i);
                return true;
            }
        }
        return false;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        g.centeredText(this.font, this.title, this.width / 2, sy(6), 0xFFFFFFFF);
        super.extractRenderState(g, mouseX, mouseY, partialTick);

        // 列表条目自己画:悬停高亮 + 方块名 + id + 当前颜色块
        PacketXrayConfig cfg = PacketXray.config();
        for (int i = 0; i < PER_PAGE; i++) {
            int idx = page * PER_PAGE + i;
            if (idx >= filtered.size()) break;
            Block block = filtered.get(idx);
            Identifier id = BuiltInRegistries.BLOCK.getKey(block);
            if (id == null) continue;

            int ry = sy(listTopCanvas() + i * ROW_H);
            int rh = sl(ROW_H);
            int rx = sx(30);
            int rw = sl(CTRL_W);
            boolean hovered = mouseX >= rx && mouseX <= rx + rw && mouseY >= ry && mouseY < ry + rh;

            g.fill(rx, ry, rx + rw, ry + rh, hovered ? 0x60FFFFFF : 0x30000000);

            boolean already = cfg.oreIds().contains(id.toString());
            // 小方块只表状态:没加进名单的红、已加的绿(不显示透视高亮色,免得和状态混淆)
            g.fill(rx + 2, ry + 2, rx + 12, ry + rh - 2, already ? COLOR_ADDED : COLOR_NOT_ADDED);
            // 方块名(跟随客户端语言)
            g.text(this.font, block.getName().getString(), rx + 16, ry + (rh - 8) / 2,
                    already ? 0xFF9AE59A : 0xFFFFFFFF);
            // id 放右边,次要信息
            String idText = id.toString();
            int idW = this.font.width(idText);
            g.text(this.font, idText, rx + rw - idW - 4, ry + (rh - 8) / 2, 0xFF808080);
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
