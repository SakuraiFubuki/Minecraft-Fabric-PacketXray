<h1 align="center">🔮 Packet Xray</h1>

<p align="center">
  <b>Minecraft Fabric 客户端模组 · 用发包让反矿透服务器亲口交出真矿</b><br>
  针对 Paper 系服务端(Paper / Purpur / Folia / Canvas / Leaf)的 Anti-Xray
</p>

<p align="center">
  <img alt="Minecraft 26.2" src="https://img.shields.io/badge/Minecraft-26.2-62b47a?style=flat-square">
  <img alt="Fabric" src="https://img.shields.io/badge/Loader-Fabric-dbb35c?style=flat-square">
  <img alt="Java 25" src="https://img.shields.io/badge/Java-25-f89820?style=flat-square">
  <img alt="Client only" src="https://img.shields.io/badge/Side-Client%20only-8a63d2?style=flat-square">
  <img alt="License MIT" src="https://img.shields.io/badge/License-MIT-blue?style=flat-square">
</p>

---

## 📖 这是什么

**Packet Xray** 是一个 Minecraft **Fabric 客户端模组**,专门对付 Paper 系服务端自带的 **Anti-Xray 反矿透**。

普通的 X-Ray / 矿物透视 / ESP 在开了反矿透的服务器上只能看到满地假矿 —— 服务器发给你的区块本来就是掺过假的。本模组不猜、不扫假矿,它用一套精确的**发包策略**让服务器自己把**真实方块**回传给你,再把真方块画成半透明的**透视高亮方块**:

- 💎 深层钻石矿 `minecraft:deepslate_diamond_ore`
- 🔥 远古残骸 `minecraft:ancient_debris`
- ➕ **任意方块** —— 想透视什么就在配置菜单里加什么

默认两块矿是亮绿色高亮,颜色可逐个自定义。还可以从准星拉出射线指向最近的若干块矿,视野外的目标也知道往哪走。

**一句话:反矿透服务器上也能用的钻石透视 / 下界合金透视 / 任意方块透视。**

## ✨ 特性

- ⚡ **真矿,不是假矿** —— 只显示服务器亲口确认过的方块,假矿一个不画
- 🎯 **一包揭一片** —— 按数学点阵发包,包量只有逐格发的 **1/16**,站着不动时**零发包**
- 🧠 **自动适配服务器配置** —— 自动识别 Anti-Xray 的 `update-radius`,不用改任何设置
- 🧱 **累积地图** —— 走过的地方持续显示,直到被挖掉或区块重载
- 🖥️ **纯客户端** —— 服务器不需要装任何东西,不改游戏文件,一个 jar 丢进 `mods` 就行
- ⚙️ **图形配置菜单** —— 半径、发包频率、方块名单、颜色、渲染开关都在游戏里点着改
- 🎨 **逐方块配色** —— 每种方块一个颜色,12 个预设色 + 手动填色码
- ✂️ **极简** —— 一个命令、零线程、唯一一个 mixin

## 📥 安装

前置:[Fabric Loader](https://fabricmc.net/) ≥ 0.19.2 与 [Fabric API](https://modrinth.com/mod/fabric-api)(Minecraft **26.2**)。

1. 把 `packetxray-*.jar` 放进 `.minecraft/mods/`
2. 启动游戏(需要 **Java 25**)
3. 进服务器,打字 `/packetxray`,开挖 ⛏️

> ⚠️ **仅支持 Minecraft 26.2**。26.1.x 不支持 —— 26.2 删除了 `Minecraft.screen` 字段与 `setScreen(Screen)` 方法,界面 API 已经互不兼容,做双版本兼容不划算。

## 🎮 用法

所有功能都挂在 `/packetxray` 主命令下,没有别的顶层命令:

```
/packetxray                              开 / 关
/packetxray help                         查看用法
/packetxray config                       打开图形配置菜单
/packetxray <半径> [每批包数] [休息ms]     带参数开启
```

默认:半径 **6**、每批 **40** 包、休息 **100ms**(≈ 400 包/秒)。再敲一次 `/packetxray` 关闭,断线自动关闭。

聊天栏只在真有**新**发现时提示一次:`新发现 N 个矿,共 M 个`。高亮会一直显示,直到被挖掉、所在区块被服务器重发/卸载、换维度或关闭模组。

## ⚙️ 配置菜单

`/packetxray config` 打开。所有改动**关闭界面即存盘**到 `config/packetxray.json`,下次启动自动读回。

**发包参数** —— 手打数值,点「应用」生效。任一项非法则整批不生效并红字提示,不会出现"改对一半":

| 项目 | 范围 | 说明 |
|---|---|---|
| 半径 | 1~64 | 候选范围上限,实际受服务器交互距离约束(见下) |
| 每批包数 | 1~200 | 一个 tick 最多发几个包 |
| 休息 | 1~1000 ms | 每批之间的间隔 |

**矿石名单** —— 管理要透视哪些方块:

| 按钮 | 作用 |
|---|---|
| `预设色…` | 从 12 个常用色里挑一个套给选中的方块 |
| `RRGGBB` 输入框 + `改色` | 手动填色码(可带 `#`,也支持 `AARRGGBB`) |
| `删除` | 从名单里移除选中的方块 |
| `恢复默认` | 恢复成深层钻石矿 + 远古残骸 |
| `<` `>` | 切换当前选中的方块 |
| `添加方块…` | 打开**方块选择菜单** |

**方块选择菜单** —— 搜索框 + 分页列表,共 1167 个方块:

- 搜索同时匹配**译名**和**方块 id**,不分大小写;`iron_ore` 这种不带命名空间的写法也认
- 方块名自动跟随**客户端语言**(中文客户端显示「铁矿石」)
- 默认按**方块 id** 升序排列,`minecraft` 在前、模组方块按命名空间跟在后面
- 行首小方块:**红色 = 未加入名单,绿色 = 已加入**,点一下即添加

**渲染开关** —— 点一下立即生效:

| 项目 | 说明 |
|---|---|
| 填充方块 | 是否画透视高亮方块 |
| 准星射线 | 是否从准星画射线指向最近的矿 |
| 射线数 | 最多画几条射线(1~64),每条指向一块矿 |

> 射线颜色跟着**目标方块自己的高亮色**,所以多种方块混在一起也分得清。

## ❓ 常见问题

**为什么只能看到身边 6 格?**
这是**服务器的硬上限**,不是模组的限制。服务端处理挖掘包之前先检查交互距离(眼睛到方块 4.5 + 1 = 5.5 格),超出直接丢弃,反矿透的揭示逻辑根本跑不到。所以任何模组都揭不到 5.5 格以外的方块,半径参数超过这个值会被自动封顶。想看更远就边走边看,揭示过的方块会**累积**显示。

**会被反作弊(GrimAC、Vulcan、Matrix)封吗?**
发的是原版客户端本来就会发的"取消挖掘"包(`ABORT_DESTROY_BLOCK`),不会真挖、不会改移动、不改任何原版行为。但 400 包/秒的频率是可配置的,保守起见可以把每批包数调小、休息时间调大。**请自行评估你所在服务器的规则。**

**在没开反矿透的服务器上有用吗?**
没意义 —— 那种服务器区块里就是真矿,用普通 X-Ray 就行。本模组的价值在于**反矿透开着**的服务器。

**加了自定义方块,远处也能看到吗?**
模组对**任何**名单里的方块都走同一套发包揭示流程,但受同样的 5.5 格限制。远处服务端没广播过的方块不会显示。

**配置存在哪?**
`config/packetxray.json`(Fabric 标准配置目录)。删掉它就恢复默认。

## 🔬 原理

针对 Paper 反矿透的真实实现 `ChunkPacketBlockControllerAntiXray` 设计:

1. **一包揭一片**。服务器收到任意挖掘动作包(哪怕是 `ABORT_DESTROY_BLOCK`)都会调 `updateNearbyBlocks`,把目标格周围 `update-radius` 范围内、真实方块属于伪装名单的格子广播出来。`update-radius=2`(默认)是曼哈顿距离 1~2 的 24 格,`=1` 只有 6 个面邻居。
2. **按点阵发包**。radius=2 按 `x+3y+8z ≡ 0 (mod 16)` 的点阵发(穷举验证过是能盖满全空间的最稀点阵,每 16 格 1 包),radius=1 按 `x+2y+3z ≡ 0 (mod 7)` 的完美点阵发;可达区边缘盖不到的再贪心补几个。
3. **只信服务器回的包**。反矿透只伪装整块的区块包,单格 / 分段更新包一律是真方块。所以"已揭示"只在收到 `ClientboundBlockUpdatePacket` / `ClientboundSectionBlocksUpdatePacket` 时登记,目标方块也在收包时当场判定。发了包但服务器没回的格子(超距、真方块不在伪装名单、被 Folia 区域线程丢弃……)不会被当真。
4. **不重复发包**。已揭示的格子不再发;每格最多被覆盖 2 次,还没回就放弃(直到区块重载)。
5. **自动识别 update-radius**。按收到的更新相对目标格的距离统计,只见距离 1、从不见距离 2 就切到 radius=1 的球和点阵。
6. **全在主线程**。整个流程挂在客户端 tick 上的状态机:规划 → 分批发包 → 等回包 → 结算 → 歇息,没有线程和 sleep。

## 🧩 版本兼容

- 只支持 **Minecraft 26.2**,`fabric.mod.json` 里声明 `"minecraft": "~26.2"`,26.1.x 会被加载器直接拒绝而不是进去后诡异报错。
- 唯一逐版本易变的渲染部分不写 mixin,走 Fabric API 的 `LevelRenderEvents.COLLECT_SUBMITS` + `submitCustomGeometry`,管线继承原版 `DEBUG_FILLED_SNIPPET` / `LINES_SNIPPET`。
- 换界面统一走 `setScreenAndShow` —— 26.2 删掉了 `setScreen(Screen)`,且界面必须由它内部的 `renderFrame` 完成初始化,否则 Fabric 的 screen-api 会抛 `has not been correctly initialised`。

## 🛠️ 构建

Gradle + fabric-loom,Java 25:

```
./gradlew build
```

产物在 `build/libs/`。想对着别的版本编译,改 `gradle.properties` 里的 `minecraft_version` 和 `fabric_api_version` 即可 —— 但注意上面说的 26.1.x 界面 API 不兼容,源码需要改。

## 📂 源码结构

| 文件 | 作用 |
|------|------|
| `PacketXray.java` | 状态机、发包规划、收包登记、渲染(挂 Fabric `LevelRenderEvents`) |
| `PacketXrayMod.java` | 入口,注册 `/packetxray` 命令,断线时停掉 |
| `PacketXrayConfig.java` | 配置模型与 JSON 读写、方块名单与颜色解析 |
| `PacketXrayConfigScreen.java` | 主配置菜单 |
| `PacketXrayBlockPicker.java` | 方块选择菜单(搜索 + 分页) |
| `PacketXrayColorPresets.java` | 预设色选择菜单 |
| `ScreenAccess.java` | 界面切换的版本适配层 |
| `PacketXrayRenderTypes.java` | 透视用的 RenderType(关掉深度测试的填充四边形与射线) |
| `mixin/ClientPacketListenerMixin.java` | 截获服务器的方块更新包(唯一的 mixin) |

## 🔗 链接

- **本项目基于此项目重构**:https://github.com/eternity4719/TrueSight
- **项目主页 / 源码**:https://github.com/SakuraiFubuki/Minecraft-Fabric-PacketXray
- **问题反馈**:https://github.com/SakuraiFubuki/Minecraft-Fabric-PacketXray/issues

## ⚖️ 免责声明

所有代码由 DeepSeek 编写<br>
DeepSeek 模型:DeepSeek-V4.1-Flash-0910-Max<br>
本项目用于学习 Minecraft 网络协议与服务端反矿透机制。**在他人服务器上使用前请遵守该服务器规则**，后果自负。<br>
原仓库没有 LICENSE，但原仓库在 fabric.mod.json 中填写 LICENSE 为 MIT，故使用 MIT 协议开源  

---

<p align="center">
  <sub>Keywords: Minecraft X-Ray · 矿透 · 反矿透破解 · Anti-Xray bypass · Paper Anti-Xray · Folia · Canvas · Fabric mod · 钻石透视 · 远古残骸 · 下界合金 · Ancient Debris ESP · Diamond ESP · Ore ESP · 矿物透视 · 26.2 · 客户端 mod · Packet Xray</sub>
</p>
