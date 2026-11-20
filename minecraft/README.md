# Tower of Entropy — Minecraft

**M002.A3.3:** avanços A/B/C/pesado de 0,40/0,50/0,60/0,70 bloco, pesado com recuperação final de 250 ms e duração efetiva de 1,75 s. Pulo não cancela ataques; exige soltar e pressionar novamente. Roll continua cancelando. [Ajustes](docs/M002_A3_3_COMBAT_REFINEMENTS.md) e [testes/limites](docs/TEST_LOG_M002_A3_3.md). Rede: protocolo 8; cliente e servidor devem usar a mesma versão.

**M002.A3.2 (histórico):** Roll permite Sprint, devolve controle após 14 passos físicos e usa saída visual de 80 ms. Ataques de espada comprometem WASD e avançam nominalmente 0,18/0,22/0,28/0,32 bloco, com validação do servidor. [Movimento de combate](docs/M002_A3_2_COMBAT_MOVEMENT.md), [Roll](docs/M002_A3_2_ROLL_RECOVERY.md) e [testes/limites](docs/TEST_LOG_M002_A3_2.md). Protocolo histórico 7 (atual: 8). A arquitetura global Classic/Slim continua vigente.

**M002.A3.1:** as animações suportadas funcionam automaticamente em qualquer mundo Singleplayer; skins Classic e Slim são selecionadas pelo renderizador. Não é preciso circuito, script ou comando. [Instalação do JAR](docs/M002_A3_1_GLOBAL_DEFAULT_ANIMATIONS.md), [skins](docs/M002_A3_1_SKIN_COMPATIBILITY.md) e [validação/limites](docs/TEST_LOG_M002_A3_1.md). Os circuitos e relatórios abaixo são ferramentas e histórico de desenvolvimento.

Projeto Java independente, criado em `minecraft/`. **Arquitetura atual: Polymesh R3 como renderizador padrão aprovado**, CPM como compatibilidade/fallback e combate próprio no servidor. M002.A3 reúne locomoção, crouch, salto/queda, água, dano/morte, arco, consumo e contextos adequados em uma versão jogável. AUTO e as animações não exigem comandos. R3.1 permanece experimental.

M002.A3: [cobertura e circuito](docs/M002_A3_FULL_ANIMATION_COVERAGE.md), [fontes/licenças/limites](docs/M002_A3_ASSET_COMPATIBILITY.md), [testes](docs/TEST_LOG_M002_A3.md), [visualizador 1×/câmera lenta](animation_pipeline/reports/m002_a3/VIEWER.html) e [seleção compacta](animation_pipeline/reports/m002_a3/REVIEW_COMPACT.zip). **Não é 100% de cobertura:** picareta customizada está bloqueada; contextos de arco/interações e ações excluídas usam vanilla. Aprovação estética pendente.

Circuito opcional de desenvolvimento: `powershell -ExecutionPolicy Bypass -File scripts/play-a3.ps1`, mundo da pasta `TOE_A3` (o título herdado pode ser New World), F5. Esse circuito não é necessário para ativar animações; o perfil não inclui fixtures de teste.

M002.A2: [arquitetura](docs/M002_A2_LOCOMOTION_ARCHITECTURE.md), [assets](docs/M002_A2_ANIMATION_ASSETS.md), [QA visual](docs/M002_A2_VISUAL_QA.md), [testes/desempenho](docs/TEST_LOG_M002_A2.md), [comparador 1×/câmera lenta](animation_pipeline/reports/m002_a2/COMPARATOR.html) e [seleção compacta](animation_pipeline/reports/m002_a2/review/INDEX.html). Para trás e strafe são reconhecidos e usam fallback, pendentes de clipes adequados. Aprovação estética das novas animações pendente do usuário.

M002.A1.2: [transições e configuração](docs/M002_A1_2_ANIMATION_TRANSITIONS.md), [QA visual](docs/M002_A1_2_VISUAL_QA.md), [testes e desempenho](docs/TEST_LOG_M002_A1_2.md) e [comparador antes/depois 1× e câmera lenta](animation_pipeline/reports/m002_a1_2/COMPARATOR.html). Entrada pesada 120 ms, retorno 140 ms, cancelamento visual para Roll 60 ms; apoio visual limitado aos quadros apoiados de B/C. Aprovação estética pendente do usuário.

- Minecraft Java Edition 1.20.1
- Forge 47.4.28 Recommended
- JDK 17 x64; ambiente verificado com Microsoft OpenJDK 17.0.20.1
- Wrapper oficial MDK: Gradle 8.8 / ForgeGradle 6
- Mod ID `towerofentropy`; versao inicial `0.0.1`
- CPM Forge `1.20v0.6.26a-forge`, dependência local remapeada pelo ForgeGradle e obrigatória; não incorporada ao JAR do mod
- Polymesh Forge `1.0.0`, dependência de cliente obrigatória, separada do JAR; o build preserva a geometria R3 aprovada e acrescenta somente os clipes A1

Abra esta pasta `minecraft` no VS Code. No terminal PowerShell:

```powershell
.\scripts\gradle.ps1 build --console=plain
.\scripts\gradle.ps1 runClient --console=plain
```

O script verifica o JDK, define JAVA_HOME no processo e usa `.gradle-user-home/` local. Nao requer Gradle global. Se a politica local bloquear scripts, use `powershell -ExecutionPolicy Bypass -File .\scripts\gradle.ps1 build` apenas para essa execucao.

Alternativa com wrapper diretamente:

```powershell
$env:JAVA_HOME = Split-Path (Split-Path (Get-Command javac.exe).Source -Parent) -Parent
$env:GRADLE_USER_HOME = Join-Path $PWD '.gradle-user-home'
.\gradlew.bat build
.\gradlew.bat runClient
```

Artefato: `build/libs/towerofentropy-0.0.1.jar` apos build bem-sucedido.

No cliente normal, use skin Classic e F5: R3 inicia sem comando de laboratório. AUTO é padrão: equipar uma espada na mão principal habilita ATTACK para A/B/C, USE contextual para pesado e V para Roll. Não exige `/toe_combat on`. `/toe_combat auto|on|off` são diagnósticos. Os remapeamentos são respeitados e itens sem conjunto permanecem vanilla. Roll e balanceamento do A1 foram preservados.

M002.A1.1: [ativação automática](docs/M002_A1_1_AUTOMATIC_COMBAT_ANIMATIONS.md), [QA visual](docs/M002_A1_1_VISUAL_QA.md), [testes](docs/TEST_LOG_M002_A1_1.md) e [visualizador local 1×/câmera lenta](animation_pipeline/reports/m002_a1_1/VIEWER.html). Há achados visuais pendentes de aprovação estética; nenhum clipe/modelo foi alterado. Documentação M002.A1: [entradas e combos](docs/M002_A1_ANIMATION_INPUT_AND_COMBOS.md), [pesado](docs/M002_A1_HEAVY_ATTACKS.md), [i-frames](docs/M002_A1_IFRAMES.md), [testes](docs/TEST_LOG_M002_A1.md) e [GDD Minecraft](docs/GDD_MINECRAFT.md). A estética dos novos movimentos aguarda aprovação. Relatórios anteriores: [promoção R3](docs/M002_R4_POLYMESH_PROMOTION.md), [Roll M002.1](docs/M002_1_ROLL_ARCHITECTURE.md) e [testes](docs/TEST_LOG_M002_1.md). Os relatos abaixo são históricos; suas descrições anteriores de “CPM padrão” ou “Roll apenas visual” foram substituídas por esta decisão. M002.2 não iniciado.

Estrutura: `src/main/java` (mod, animação e combate experimental), `src/main/resources` (metadados e assets/data), `art/` (futuros modelos, texturas e animacoes), `dependencies/cpm/` (JAR local ignorado), `scripts/`, `docs/`.

A proposta futura e um jogo completamente 3D em terceira pessoa, preservando Steve tradicional, com articulacoes via CPM e sistemas proprios de RPG/combate. Nada disso foi implementado no M000. O mod independe do launcher; nenhuma autenticacao ou licenciamento foi alterado.

Consulte [setup](docs/SETUP_M000.md), [testes](docs/TEST_LOG.md) e [proximo marco](docs/NEXT_M001.md).

Documentação atual: [índice](docs/README.md), [retargeting M001.1](animation_pipeline/reports/M001_1_RETARGET_REPORT.md) e [testes M001.1](docs/TEST_LOG_M001_1.md). Os documentos M000 são registros históricos.

Para regenerar e exportar o Idle localmente:

```powershell
node animation_pipeline/scripts/retarget-idle.cjs
node animation_pipeline/scripts/validate-idle.cjs
.\scripts\gradle.ps1 runClient -PtoePrepareModel --console=plain
```

Em um mundo local com cheats, `/toe_animtest idle_steve` ativa a prévia Steve; `/toe_animtest idle` utiliza a skin atual Classic; `/toe_animtest off` restaura o modelo anterior. Use F5 para terceira pessoa. Os modelos ficam em `animation_pipeline/converted/m001_1/` e `run/player_models/`, ignorados pelo Git. Não são incluídos no JAR.

Atualização M001.2: [locomoção e limitações](animation_pipeline/reports/M001_2_LOCOMOTION_REPORT.md), [testes e execução](docs/TEST_LOG_M001_2.md). Para gerar os modelos novos:

```powershell
node animation_pipeline/scripts/retarget-locomotion.cjs
node animation_pipeline/scripts/validate-locomotion.cjs
.\scripts\test-locomotion.ps1
.\scripts\gradle.ps1 runClient -PtoePrepareLocomotion --console=plain
```

`/toe_animtest auto` usa a skin atual Classic; `/toe_animtest auto_steve` usa a prévia Steve. Fique parado/avance para alternar Idle/Jog; estados não suportados usam fallback vanilla. `/toe_animtest off` desativa. Transições são cortes com histerese; foot planting e crossfade não foram implementados. Modelos M001.2 ficam em `animation_pipeline/converted/m001_2/`.
Para restaurar o JAR CPM ignorado pelo Git: `.\scripts\download-cpm.ps1` (requer rede; valida SHA-512).

M001.3: [relatório técnico e evidências](animation_pipeline/reports/M001_3_COMBAT_ANIMATIONS_REPORT.md), [testes e limitações](docs/TEST_LOG_M001_3.md).

```powershell
node animation_pipeline/scripts/retarget-actions.cjs
node animation_pipeline/scripts/validate-actions.cjs
.\scripts\test-actions.ps1
.\scripts\gradle.ps1 runClient -PtoePrepareActions --console=plain
```

Após a exportação, entre no mundo local com cheats e use `/toe_animtest auto_steve`. V executa Roll; J executa Sword Attack visual com combate desligado. As teclas são remapeáveis em Tower of Entropy. Também existem `/toe_animtest roll` e `/toe_animtest sword`; `/toe_animtest off` restaura o modelo anterior. Uma ação em andamento rejeita novos pedidos, sem fila. Os comandos anteriores continuam disponíveis. Projetos/modelos novos ficam em `animation_pipeline/converted/m001_3/`, fora do Git e do JAR. O Roll permanece sem deslocamento de esquiva.


M002.0: [arquitetura do ataque](docs/M002_0_COMBAT_ARCHITECTURE.md), [testes e pendências](docs/TEST_LOG_M002_0.md), [propostas futuras](docs/NEXT_M002_1.md). Com modelos M001.3 preparados, use `/toe_combat on`, equipe uma espada de ferro na mão principal e pressione J. Alcance inicial de 3 blocos, arco de 90°, um monstro vanilla por golpe, dano bruto 4 HP e impacto após 4 ticks. `/toe_combat off` restaura J visual; V continua Roll visual. O modo começa desligado em cada sessão. Não há combos, stamina ou física de Roll. M002.1 não iniciado.

M002.R1: [relatório de deformação do Steve](docs/M002_R1_STEVE_DEFORMATION_REPORT.md) e [comparação visual](experiments/m002_r1/COMPARE.html). O laboratório mantém os modelos/combate aprovados e usa somente `run-r1/`. Teste interativo: `.\scripts\start-r1.ps1 -Mode fixed -Interactive`; `/toe_r1 fixed jog`, `/toe_r1 fixed sword`, `/toe_r1 off`. Full Motion e a malha Blender ponderada permanecem alternativas experimentais, sem adoção no mod normal. A correção elástica definitiva não foi considerada resolvida.

M002.R2: [renderização ponderada no Minecraft](docs/M002_R2_SKINNED_RENDERER_REPORT.md) e [comparador antes/depois](experiments/m002_r2/COMPARE.html). Teste `.\scripts\start-r2.ps1 -Interactive`; `/toe_renderlab skinned` ativa e `/toe_renderlab off` retorna ao CPM. Usa Polymesh somente no perfil `-PtoeR2` e no diretório `run-r2/`. A malha R1 e o combate foram preservados; cotovelos/joelhos deformam com pesos, mas tronco, armadura, primeira pessoa e remoto têm limitações documentadas. Não substitui o renderer padrão nem inicia M002.2.

M002.R3: [refinamento do tronco](docs/M002_R3_CHARACTER_REFINEMENT.md), [testes](docs/TEST_LOG_M002_R3.md) e [comparação CPM/R2/R3](experiments/m002_r3/COMPARE.html). Execute `.\scripts\start-r3.ps1 -Interactive` na pasta `minecraft`. Acrescenta pelve/cintura ponderadas ao modelo existente e fallback explícito de equipamento, somente em `-PtoeR3`/`run-r3/`. CPM continua padrão; a aprovação estética depende da revisão das capturas pelo usuário. Nenhuma nova mecânica ou M002.2.

M002.R3.1: [refinamento do Roll](docs/M002_R3_1_ROLL_REFINEMENT.md), [testes](docs/TEST_LOG_M002_R3_1.md) e [comparador CPM/R2/R3/R3.1](experiments/m002_r3_1/COMPARE.html). Execute `.\scripts\start-r31.ps1 -Interactive`. Variante isolada `-PtoeR31`/`run-r31/`, com pesos regularizados e morph targets corretivos, preservando os clipes e o R3. Há recuperação de espessura nas articulações; contato coxa/abdômen ainda é uma limitação. CPM segue padrão e não houve nova mecânica.

M002.R3.QA: [benchmark e avaliação visual](docs/M002_R3_QA_REPORT.md), [reprodução a 1×](docs/M002_R3_QA_EVIDENCE/PLAYBACK.html) e [pacote compartilhável](docs/M002_R3_QA_EVIDENCE.zip). Recomenda manter R3 como referência; R3.1 permanece opcional para avaliação estética. Ensaio reproduzível: `.\scripts\run-qa.ps1`, exclusivamente em `run-qa/`, com aquecimento e três repetições por animação. Nenhum asset ou combate foi alterado.
