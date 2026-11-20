# M002.A2 — Registro de testes

13/11/2026. Windows, Minecraft 1.20.1, Forge 47.4.28, Microsoft Java 17.0.20.1 x64, Gradle 8.8, Polymesh 1.0.0 e CPM 0.6.26a. Blender 5.2.3 LTS já instalado foi usado para bake de duas animações. Nenhuma dependência ou ferramenta nova foi instalada.

## Isolamento

Backup de fontes, scripts, fixtures e documentação em `.downloads/m002_a2/backup`; hashes em `animation_pipeline/reports/m002_a2/BASELINE.json`. Mundo copiado para `run-a2/saves/TOE_A2`, ignorado pelo Git. O mundo normal `run/` foi preservado. Fixtures antigas A1/Roll continuam no mundo de QA `run-r4/`; suas saídas A2 são separadas dos relatórios históricos.

Capturas antes/depois foram executadas em JVMs separadas, mesmo roteiro, câmera e luz. Nenhum comando é necessário para ativar o R3 ou a locomoção. As fixtures posicionam o jogador, preparam cenário e simulam controles vanilla; modificações de cenário/alimentação/remapeamento são exclusivas dos testes. Não existe essa lógica no mod distribuído.

Walk é exercitado sobre soul sand. Variação de velocidade substitui o piso por stone durante o roteiro; o breve ajuste de altura pode causar fallback/parada. A sequência extra usa condições vanilla de Survival/food para encerrar sprint sem soltar avanço; não implementa stamina nem balanceamento novo. A tecla de sprint é temporariamente remapeada para R e restaurada. Pulo, agachamento e lajes testam a entrega aos fallbacks.

## Suítes e evidências

| Verificação executada | Resultado | Evidência em `animation_pipeline/reports/m002_a2/` |
|---|---:|---|
| Seletor puro: yaw, direção, diagonais, histerese, parada e dados inválidos | 43/43 | `.logs/m002-a2-unit.log` |
| Caminho posterior, 18 roteiros × três vistas + reconexão | 96/96 | `after/RUNTIME.json` |
| Caminho anterior, mesmos roteiros/vistas | 57/57 | `before/RUNTIME.json` |
| Extras posteriores: retorno Sprint → Jog e remapeamento | 18/18 | `after/EXTRA.json` |
| Extras anteriores, mesmos controles | 12/12 | `before/EXTRA.json` |
| Auditoria de seleção, fallback, prioridades, fase, matrizes e empunhadura | 54/54 | `VISUAL_ANALYSIS.json` |
| Regressão A1: combos, pesado, dano, cancelamento e sincronização | 145/145 | `regression_a1/RUNTIME.json` |
| Regressão Roll físico: direções, colisões, bordas e ciclo de vida | 167/167 | `ROLL_REGRESSION.json` |
| I-frames nos limites exatos do servidor e dano real | 24/24 | `IFRAMES.json` |
| Nova JVM: AUTO, R3 padrão e ausência de ação presa | 3/3 | `after/RESTART.json` |
| Comparador local no Edge: 1×/0,25×, seleção, avanço e imagens | 6/6 | `COMPARATOR_TEST.json` |
| Preservação de assets e código autoritativo | PASS | `PRESERVATION.json` |

O comparador contém 4.320 PNG a 10 Hz e 120 sequências antes/depois. A seleção compacta contém 12 pares. Os testes de pose excluem frames em que R3 não desenhou: o controller antigo fica armazenado durante fallback e não representa a pose vanilla.

O pacote `REVIEW_COMPACT.zip` contém 24 PNG, índice HTML e README, com **1.642.916 bytes**. É uma seleção estática compartilhável; não substitui a reprodução 1× do comparador completo. Quantidades de checks de sincronização/limites na fixture A1 podem variar conforme amostragem cliente; por isso os i-frames também recebem verificação dedicada no tick do servidor.

A regressão A1 incluiu 23 verificações de relógio visual versus impacto autoritativo. A fixture de proteção observou idades 2, 3, 9 e 10 no servidor: janela original `[3,10)` ticks preservada. Melee e projétil hostil são bloqueados somente nessa janela; lava, queda e vazio não recebem proteção adicional. No teste de dano real, idade 3 manteve 20 HP e idade 10 reduziu para 18 HP. Nenhuma regra de combate foi alterada.

`validate-a2.cjs` confirma os 171 arquivos Godot previamente inventariados, oito assets aprovados e os fontes autoritativos de combate intactos. GLB ampliado preserva nodes, meshes, skins, UVs, pesos, materiais, prefixo binário e sete animações existentes. Novos Walk/Sprint possuem 36 canais cada e durações exatas de 1,333333/0,666667 s. Costuras máximas de orientação nos endpoints: Walk ~0,0000034°, Sprint ~0,64669°.

## Método de desempenho

Antes/depois no mesmo mundo, resolução/configuração verificadas por JSON. Aquecimento de 320 ticks, incluindo primeiro uso dos novos ciclos; três repetições de quatro cargas, com 120 ticks por bloco e descarte dos primeiros 20. Sem screenshots ou probes de ossos durante medição.

O microbenchmark mede pipeline CPU do R3 (sampler/compositor/chamadas de corpo e item). Nas cargas Walk/Sprint, a referência usa o Jog antigo para manter um corpo R3 comparável; **não mede o custo GPU do antigo fallback vanilla de Sprint**. São poses controladas, sem percurso físico durante a medição. Os roteiros de movimento real são testados separadamente nas capturas.

A referência executa o caminho visual A1.2 preservado na compilação atual, selecionado por `toe.a2.previous`, em uma JVM separada. Os recursos novos continuam presentes nessa compilação. Portanto essa comparação não isola o custo de memória/boot de dois JARs históricos distintos nem o carregamento adicional do modelo derivado. O seletor horizontal observador também continua carregado no baseline, embora não controle sua renderização. Não foi medida a sobrecarga total de cache de modelos.

Também são registrados os primeiros 120 frames elegíveis do R3 e os primeiros 30 de cada clipe. Essa amostra inicial é separada dos blocos aquecidos. Não isola todo o boot do Minecraft, compilação de shaders ou tempo GPU. FPS e 1% low vêm de intervalos de quadro da aplicação, sujeitos a VSync e a pausas fora da seção CPU medida. Nenhuma métrica indisponível deve ser estimada.

Ambiente confirmado: 854×480, render/simulation distance 5, gráficos fancy, FOV 50, VSync ligado, limite 60 FPS. Resultados aquecidos:

| Carga R3 | CPU médio antes → depois | Quadro médio antes → depois | FPS médio antes → depois | 1% low antes → depois |
|---|---:|---:|---:|---:|
| Idle | 0,676 → 0,637 ms | 16,738 → 16,759 ms | 59,74 → 59,67 | 48,07 → 47,59 |
| Walk (antes: Jog) | 0,624 → 0,672 ms | 16,738 → 16,750 ms | 59,74 → 59,70 | 49,42 → 47,05 |
| Sprint (antes: Jog) | 0,643 → 0,621 ms | 16,733 → 16,734 ms | 59,76 → 59,76 | 51,28 → 48,52 |
| Transições | 0,661 → 0,639 ms | 16,726 → 16,747 ms | 59,79 → 59,71 | 49,88 → 46,76 |

Walk aumentou 0,048 ms (+7,73%) nesta comparação; os outros custos médios ficaram abaixo da referência. O maior acréscimo equivale a cerca de 0,29% do orçamento de um quadro a 60 FPS, mas não garante o mesmo comportamento em outra máquina. Não foi identificado aumento médio significativo do pipeline; o 1% low ficou ligeiramente menor nos quatro cenários, sem se atribuir a causa automaticamente ao renderer. A precisão limitada pelo VSync impede inferir FPS sem limite. GPU indisponível.

Amostra inicial de 120 frames: pipeline médio 1,733 → 2,090 ms, pico 9,263 → 11,785 ms; maior intervalo de quadro 108,68 → 81,95 ms. Há custo inicial e pausas em ambas as versões, separados do regime aquecido. No primeiro uso específico dos novos clipes (30 frames), Walk apresentou média/pico CPU de 0,631/1,129 ms e maior intervalo de 19,20 ms; Sprint 0,598/1,710 ms e intervalo de 18,66 ms. Não houve pausa longa nessa amostra específica; isso não certifica todo boot ou outra combinação de mods.

Dados e repetições: `before/BENCHMARK.json`, `after/BENCHMARK.json`, `PERFORMANCE.json` e `PERFORMANCE_SUMMARY.json`.

## Reprodução

Na pasta `minecraft`, usando as dependências/modelos locais já preparados:

```powershell
.\scripts\test-horizontal.ps1
.\scripts\test-a2.ps1 -Suite Visual
.\scripts\test-a2.ps1 -Suite Previous
.\scripts\test-a2.ps1 -Suite Extra
.\scripts\test-a2.ps1 -Suite PreviousExtra
.\scripts\test-a2.ps1 -Suite Benchmark
.\scripts\test-a2.ps1 -Suite PreviousBenchmark
.\scripts\test-a2.ps1 -Suite Iframes
.\scripts\test-a2.ps1 -Suite Restart
.\scripts\gradle.ps1 runClient -PtoeA1Test -PtoeA2Regression --offline
.\scripts\gradle.ps1 runClient -PtoeRollTest -PtoeA1Regression -PtoeA2Regression --offline
node animation_pipeline/scripts/validate-a2.cjs
node animation_pipeline/scripts/audit-a2-runtime.cjs
node animation_pipeline/scripts/build-a2-comparator.cjs
node animation_pipeline/scripts/analyze-a2-performance.cjs
.\scripts\gradle.ps1 build -PtoeR31 --offline
.\scripts\gradle.ps1 build --offline
powershell -ExecutionPolicy Bypass -File scripts/audit-a2-artifact.ps1
```

Não execute dois clientes/benchmarks simultaneamente. As fixtures Visual/Previous/Extra substituem somente suas saídas A2; não alteram evidências históricas.

## Builds e artefato final

Build experimental R3.1: **PASS, 17 s**. Build normal final: **PASS, 13 s**. `runClient` encerrou com sucesso nos perfis de captura, extras, benchmark, regressões, i-frames e reinício. A compilação experimental preserva R3.1 sem promovê-lo a padrão. Os avisos de APIs depreciadas permanecem sem falha de compilação.

JAR normal: `build/libs/towerofentropy-0.0.1.jar`, **723.314 bytes**, 91 entradas. SHA-256: `B1E451CB855DFBACD8F3B8B27A86F7DC674849A555742CB3D52CC138C516D129`. `ARTIFACT.json` confirma os dois modelos (R3 aprovado e cópia com Walk/Sprint), seus hashes corretos, configurações/metadados e ausência das fixtures QA ou classes experimentais R3.1.

Resultado consolidado: [RESULTS.json](../animation_pipeline/reports/m002_a2/RESULTS.json). Scripts `validate-a2.cjs`, `audit-a2-runtime.cjs`, `audit-a2-artifact.ps1` e `summarize-a2.cjs` reproduzem as verificações sobre as evidências disponíveis.

## Limitações de homologação

Os novos Walk/Sprint foram avaliados no R3 local com Steve Classic. Clipes direcionais próprios, paridade CPM/R3.1, renderização remota, servidor dedicado com latência e todo tipo de terreno permanecem pendentes. O build experimental comprova compilação/preservação de R3.1, não aprovação visual dos novos estados naquele renderer.

Contato não é foot locking: Walk pode penetrar a camada externa, Jog preserva folga visual existente e Sprint tem fases aéreas. A [avaliação visual](M002_A2_VISUAL_QA.md) separa essas limitações dos testes técnicos. Não foi concedida aprovação estética automática nem iniciada M002.A3.
