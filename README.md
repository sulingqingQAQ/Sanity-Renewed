# Sanity: Renewed · Depression 附属

<p align="center">
  <img src="Sanity_Icon.png" alt="Sanity: Renewed" width="160" />
</p>

基于 **Sanity: Renewed** 改造的 **Depression 专属附属**。

这个分支不再把理智系统当作一套完全独立的通用玩法，而是让它与 Depression 的心情、精神健康、疾病和 PTSD 协同工作：两套系统各自保存数值，只通过明确的倍率和事件联动，不相互覆盖。

- **运行环境**：Minecraft 1.21.1 · NeoForge
- **当前分支**：`1.21.1`
- **Mod ID**：`sanity_renewed`
- **代码许可证**：MIT，详见 [LICENSE](LICENSE)

## 玩法结构

| 系统 | 负责内容 |
| --- | --- |
| **Depression** | 心情、精神健康、精神疾病、PTSD，以及这些系统原有的治疗和伤害 |
| **Sanity: Renewed 附属** | 独立理智值、脑子 HUD、理智恢复、心魔与相关表现 |
| **DepressionTianTian（可选）** | DTT 自己的提示与扩展玩法；不是本附属的必需依赖 |

理智使用面向玩家的 **S 值（0～100）**：`100` 表示头脑清醒，数值越低，认知状态越不稳定。

理智不会由精神健康值直接换算，也不会持续反写 Depression 的心情或精神健康。重度抑郁但保持清醒、精神健康尚可但理智濒临崩溃，都是允许出现的状态。

## 当前功能

### 独立理智与持久化

- 每名玩家分别保存理智值。
- 退出重进、死亡重生和跨维度后保留理智，不会自动回满。
- 客户端按真实变化同步理智与 HUD，不再按固定频率无脑发包。
- 保留 `/sanity` 调试命令和 `/sanity link` 联动诊断。

### 理智恢复

当前有两种基础恢复来源，数值均可配置：

| 来源 | 默认值 | 条件 |
| --- | --- | --- |
| 基础恢复 | `+0.05 S/s` | 处于安全状态且环境光照足够；受当前心情影响 |
| 营火恢复 | `+0.10 S/s` | 默认 4 格内有点燃营火且有视线；多个营火不叠加 |

心情会放慢或停止基础恢复，但**不会掐掉营火恢复**。两种来源同时满足时可以相加。

为避免与 Depression/DTT 重复奖励或重复惩罚，上游大量旧理智来源已统一收口；不要再把上游 README 中的食物、成就、宠物等整表当作本分支的当前生效清单。

### 与 Depression 的双向联动

- **心情 → 理智**：心情越差，基础理智恢复越慢；营火不受此倍率影响。
- **理智 → 心情**：低理智会减慢负心情的回稳，并放大经过 Depression 的心理伤害。
- **PTSD → 理智**：按一次发作的等级变化结算理智损失，默认四级累计值为 `2 / 4 / 6 / 8 S`；跨档只补差额，不按 PTSD 数值持续扣理智。
- **表现让位**：PTSD 发作期间，心魔心跳等表现会让位于 Depression；发作结束后恢复。

`/sanity link` 可以查看心情、理智、三项倍率、基础恢复门控和 PTSD 结算账本。该命令只读，不会改变结算状态。

### 心魔与表现层

- `S <= 40`：心魔可以显现。
- `S <= 25`：允许生成新的心魔。
- `S <= 13`：心魔可以攻击。
- 心魔伤害走 Depression 已有的非玩家伤害、心情和 PTSD 路径，不另造一套重复结算。
- 保留心魔动画、目标可见性、音效与 HUD 表现；联动诊断信息已中文化。

**新生成心魔默认关闭。** 需要服主在配置中显式打开：

```toml
[sanity.phase3]
inner_entity_spawning = true
```

关闭生成开关不会删除世界里已经存在的心魔；既有实体仍按可见性和攻击阈值工作。

## 依赖

### 必需

- Minecraft **1.21.1**
- NeoForge **21.1.x**
- [Architectury API](https://www.curseforge.com/minecraft/mc-mods/architectury-api) **13+**
- [GeckoLib](https://www.curseforge.com/minecraft/mc-mods/geckolib) **4.7+**，当前验证使用 **4.8.4**
- **Depression 0.2.3+**，需使用包含当前联动挂点的配套构建

### 可选

- **DepressionTianTian（DTT）**

### 平台范围

本附属分支目前只支持 **NeoForge 1.21.1**。仓库中保留的 Fabric 相关内容属于上游代码背景，**不是本附属当前支持的发布目标**。

## 安装

1. 安装匹配版本的 Depression、Architectury API 和 GeckoLib。
2. 将本附属的 NeoForge jar 放进 `mods`。
3. 如需 DTT，再安装与当前 Depression 构建配套的 DTT。
4. 启动游戏或服务端，检查日志中的 Depression 联动注册信息。
5. 如需心魔生成，再在配置中显式开启对应开关。

客户端与服务端应使用相互匹配的 Depression 和附属版本，不要把旧独立版 Sanity jar 与新附属混用。

## 命令

以下命令默认需要 **2 级权限**：

| 命令 | 用途 |
| --- | --- |
| `/sanity get [targets]` | 查看理智值 |
| `/sanity set <targets> <value>` | 将理智设为 `0～100` |
| `/sanity add <targets> <value>` | 增减理智，最终仍限制在合法范围 |
| `/sanity link [targets]` | 查看 Depression 联动状态与 PTSD 账本 |
| `/sanity config reload` | 重载维度配置；非法联动配置会被拒绝并保留旧覆盖 |

示例：

```text
/sanity get @s
/sanity set @s 50
/sanity add @s 10
/sanity link @s
/sanity config reload
```

不带 `targets` 的 `get`、`set`、`add` 需要由玩家本人执行；控制台或命令方块应显式指定玩家。

## 配置

- 默认配置：`config/sanity_renewed/default.toml`
- 维度覆盖：`config/sanity_renewed/dimension/`
- 维度写法参考该目录下的 `example.toml`

恢复速度、联动倍率、PTSD 结算、心魔阈值与生成冷却等均通过配置调整。维度覆盖中的未知键、错误类型、越界值或破坏阈值顺序的组合会被拒绝，避免半套配置悄悄生效。

## 从源码构建

Depression 当前不是从公共 Maven 直接拉取，需要先在其仓库发布到本地 Maven：

```bash
gradlew :common:publishToMavenLocal
```

然后在本仓库构建 NeoForge 成品：

```bash
gradlew :neoforge:remapJar -x test --no-daemon
```

成品位于 `neoforge/build/libs/`，部署时使用正式的 `-neoforge.jar`，不要使用 `-dev-shadow.jar` 或 `-sources.jar`。

## 当前进度与后续范围

已完成：

- 独立理智、HUD、持久化与基础/营火恢复；
- Depression 心情、低理智与 PTSD 联动；
- 心魔、表现层、中文化和真实客户端验证。

后续 **2B** 才会处理匹配服药、过量、睡眠恢复和重复奖励来源的最终规则；这些内容目前不是已交付功能。

## 致谢

这个附属站在原作者和上游维护者的工作之上，衷心感谢他们：

- **[croissantnova](https://github.com/croissantnova)**：原作 **Sanity: Descent Into Madness** 的作者，完成了最初的理智玩法、代码与美术设计。
- **guivnf / [Antikythera-Studios](https://github.com/Antikythera-Studios/Sanity-Renewed)**：维护 **Sanity: Renewed**，把原作延续到较新的 Minecraft 版本，为本附属提供了直接基础。
- **[toujourspareil](https://twitter.com/toujourspareil_)**：原作美术贡献。
- **[Desynq](https://github.com/Desynq)**：其 [PR #93](https://github.com/croissantnova/SanityDescentIntoMadness/pull/93) 为 Renewed 的改动提供了重要启发。
- **[Zapsplat](https://www.zapsplat.com/)**：提供上游使用的音效资源。

没有他们的原设计、代码、美术和声音，就没有现在这个 Depression 附属。这里的二次改造不代表原作者或上游团队参与维护本分支，也不表示他们为本分支的联动规则背书。

- 原作地址：[Sanity: Descent Into Madness](https://github.com/croissantnova/SanityDescentIntoMadness)
- 上游地址：[Sanity: Renewed](https://github.com/Antikythera-Studios/Sanity-Renewed)
- 本附属仓库：[sulingqingQAQ/Sanity-Renewed](https://github.com/sulingqingQAQ/Sanity-Renewed)

## 许可证与资源说明

代码沿用 **MIT License**，完整文本见 [LICENSE](LICENSE)，并保留原版权声明。

音效仍遵循 [Zapsplat Standard License](https://www.zapsplat.com/license-type/standard-license/)；其他美术与资源沿用其各自的上游许可和权利说明，不因本次改造而重新授权。
