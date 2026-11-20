# Pipeline de animações — M001.0 / M001.1 / M001.2 / M001.3

O M001.0 registrou somente inventário e cópias de trabalho. No M001.1 foi convertido exclusivamente o UAL1 `Idle_Loop` para um Steve articulado experimental, com projeto e modelo validados pelo CPM no Minecraft.

No M001.2, `Jog_Fwd_Loop` foi convertido pelo mesmo retargeting. `retarget-locomotion.cjs` produz o projeto com poses exclusivas Idle/Jog e `validate-locomotion.cjs` verifica também a preservação do M001.1. Veja [relatório de locomoção](reports/M001_2_LOCOMOTION_REPORT.md) e [testes M001.2](../docs/TEST_LOG_M001_2.md). Nesse marco foram convertidos somente Idle/Jog.

- `input/`: dois GLB originais, licenças e README dos pacotes UAL1/UAL2; conteúdo local ignorado pelo Git.
- `converted/`: projetos/modelos e dados intermediários M001.1; conteúdo ignorado pelo Git.
- `scripts/`: inspeção somente leitura, retargeting do Idle e validação; Node.js sem dependências externas.
- `reports/`: inventário e metadados auditáveis, destinados ao versionamento.

Na pasta `minecraft/animation_pipeline`, para repetir a inspeção:

```powershell
node scripts/inspect-assets.cjs "C:\Users\balen\Desktop\tower-of-entroy" reports/asset-metadata.json
```

O script lê GLB e FBX diretamente. Não inicia Godot, Blender, Forge ou CPM. Não grava no diretório Godot. O JSON é um inventário de metadados, não uma conversão de animações, e não contém curvas completas, malhas ou texturas.

Consulte [M001_ASSET_INVENTORY.md](reports/M001_ASSET_INVENTORY.md) para seleção, durações, ossos, licenças e limitações. Fontes de terceiros não devem ser adicionadas automaticamente ao Git, ao JAR ou a distribuições públicas. Nada deste diretório é incluído pelo source set padrão do mod.

Para reproduzir o marco M001.1, veja [M001_1_RETARGET_REPORT.md](reports/M001_1_RETARGET_REPORT.md) e [TEST_LOG_M001_1.md](../docs/TEST_LOG_M001_1.md). `retarget-idle.cjs` gera o projeto local; `validate-idle.cjs` verifica rig, UVs, frames e preservação das fontes. O exportador binário roda no cliente com `-PtoePrepareModel` e usa o próprio CPM. Naquele marco, somente Idle foi convertido.

M001.3: `retarget-actions.cjs` converte Roll UAL1 e Sword_Regular_A UAL2 para o mesmo rig. `validate-actions.cjs` verifica formatos e preservação das fontes/modelos anteriores. A exportação binária nativa usa `-PtoePrepareActions`. Consulte o [relatório M001.3](reports/M001_3_COMBAT_ANIMATIONS_REPORT.md) e os [testes M001.3](../docs/TEST_LOG_M001_3.md). Fontes e conversões permanecem locais; scripts e relatórios são destinados ao versionamento.

M002.0 reutiliza os arquivos aprovados do M001.3, sem novas conversões ou exportações. A lógica funcional de espada fica no código Java; consulte a [arquitetura](../docs/M002_0_COMBAT_ARCHITECTURE.md). M002.1 não iniciado.
