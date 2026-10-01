package mod.deepseek.packetxray;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 配置:/packetxray config 菜单改的就是这份东西,落地在 config/packetxray.json。
 * <p>
 * 字段全部是可空包装类型:JSON 里缺哪个键就保留默认值,不会被反序列化成 0/null。
 * 矿石用「方块 id → ARGB 颜色」的有序表存,顺序即菜单里的显示顺序。
 */
public final class PacketXrayConfig {

    /** 内置的两块矿,首次运行时作为默认名单。 */
    private static final Map<String, Integer> DEFAULT_ORES = new LinkedHashMap<>();

    static {
        DEFAULT_ORES.put("minecraft:deepslate_diamond_ore", 0x9900FF00);
        DEFAULT_ORES.put("minecraft:ancient_debris", 0x9900FF00);
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("packetxray.json");

    // ===== 发包参数 =====
    public Integer radius;
    public Integer batchSize;
    public Long batchSleepMs;

    // ===== 渲染开关 =====
    /** 画填充方块。 */
    public Boolean drawBoxes;
    /** 画准星射线。 */
    public Boolean drawTracer;
    /** 最多画几条射线:只取离相机最近的这么多块矿,每块一条。 */
    public Integer tracerCount;
    /** 射线宽度(像素)。颜色不在这里配 —— 每条射线用目标矿自己的高亮色。 */
    public Float tracerWidth;

    // ===== 矿石名单:方块 id → ARGB =====
    public Map<String, Integer> ores;

    /** 运行期用:方块 → 颜色。配置改动后就地重建,渲染和收包判定都读它。 */
    private transient Map<Block, Integer> resolved = new LinkedHashMap<>();

    public static PacketXrayConfig load() {
        PacketXrayConfig cfg = new PacketXrayConfig();
        if (Files.isRegularFile(FILE)) {
            try {
                String text = Files.readString(FILE, StandardCharsets.UTF_8);
                JsonElement root = JsonParser.parseString(text);
                if (root != null && root.isJsonObject()) {
                    cfg = GSON.fromJson(root, PacketXrayConfig.class);
                }
            } catch (Exception e) {
                PacketXray.LOGGER.warn("读取 {} 失败,用默认配置:{}", FILE, e.toString());
            }
        }
        if (cfg == null) cfg = new PacketXrayConfig();
        cfg.applyDefaults();
        cfg.rebuild();
        return cfg;
    }

    /** 缺省值补齐。 */
    private void applyDefaults() {
        if (radius == null || radius < 1) radius = 6;
        if (batchSize == null || batchSize < 1) batchSize = 40;
        if (batchSleepMs == null || batchSleepMs < 1) batchSleepMs = 100L;
        if (drawBoxes == null) drawBoxes = true;
        if (drawTracer == null) drawTracer = true;
        if (tracerCount == null || tracerCount < 1) tracerCount = 1;
        if (tracerWidth == null || tracerWidth <= 0) tracerWidth = 2.0F;
        if (ores == null || ores.isEmpty()) ores = new LinkedHashMap<>(DEFAULT_ORES);
    }

    public void save() {
        applyDefaults();
        rebuild();
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(this) + System.lineSeparator(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            PacketXray.LOGGER.warn("写入 {} 失败:{}", FILE, e.toString());
        }
    }

    /** 把 id 表解析成方块表。认不出的 id 跳过(不删,留着等以后版本能认)。 */
    public void rebuild() {
        Map<Block, Integer> map = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> e : ores.entrySet()) {
            Identifier id = Identifier.tryParse(e.getKey());
            if (id == null) continue;
            Block block = BuiltInRegistries.BLOCK.getOptional(id).orElse(null);
            if (block == null) continue;
            map.put(block, e.getValue());
        }
        resolved = map;
    }

    /** 当前生效的矿石名单(方块 → 颜色),渲染与收包判定用。 */
    public Map<Block, Integer> ores() {
        return resolved;
    }

    public boolean isTarget(Block block) {
        return resolved.containsKey(block);
    }

    public int colorOf(Block block) {
        Integer c = resolved.get(block);
        return c == null ? 0x9900FF00 : c;
    }

    /** 按显示名排序的名单快照,给菜单列表用。 */
    public List<String> oreIds() {
        return new ArrayList<>(ores.keySet());
    }

    /** 加一块矿(已存在则只更新颜色)。返回 false 表示这个 id 认不出。 */
    public boolean addOre(String rawId, int color) {
        Identifier id = Identifier.tryParse(rawId);
        if (id == null) return false;
        if (BuiltInRegistries.BLOCK.getOptional(id).isEmpty()) return false;
        ores.put(id.toString(), color);
        rebuild();
        return true;
    }

    public boolean removeOre(String id) {
        boolean removed = ores.remove(id) != null;
        if (removed) rebuild();
        return removed;
    }

    public void setOreColor(String id, int color) {
        if (ores.containsKey(id)) {
            ores.put(id, color);
            rebuild();
        }
    }

    public int colorOfId(String id) {
        Integer c = ores.get(id);
        return c == null ? 0x9900FF00 : c;
    }

    /** 恢复成内置两块矿。 */
    public void resetOres() {
        ores = new LinkedHashMap<>(DEFAULT_ORES);
        rebuild();
    }

    /** 0xAARRGGBB → "RRGGBB" 便于手输。 */
    public static String toHex(int argb) {
        return String.format("%06X", argb & 0xFFFFFF);
    }

    /** "RRGGBB" / "#RRGGBB" / "AARRGGBB" → ARGB;解析不了返回 -1。 */
    public static int parseColor(String text, int alphaFrom) {
        if (text == null) return -1;
        String s = text.trim();
        if (s.startsWith("#")) s = s.substring(1);
        if (s.isEmpty() || s.length() > 8) return -1;
        try {
            long v = Long.parseLong(s, 16);
            if (s.length() <= 6) {
                // 只给了 RGB,alpha 沿用原来的
                return (alphaFrom & 0xFF000000) | (int) (v & 0xFFFFFF);
            }
            return (int) v;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** 菜单里显示用的名字。 */
    public static String displayName(String id) {
        Identifier ident = Identifier.tryParse(id);
        if (ident == null) return id;
        Block block = BuiltInRegistries.BLOCK.getOptional(ident).orElse(null);
        if (block == null) return id;
        return block.getName().getString();
    }

    public Path file() {
        return FILE;
    }
}
