# M002.A1.2 — Registro de testes

13/11/2026, Windows, Minecraft 1.20.1/Forge 47.4.28, Microsoft OpenJDK 17.0.20.1 x64, Gradle 8.8, Polymesh 1.0.0 e CPM 0.6.26a. Nenhuma dependência ou ferramenta nova instalada.

## Isolamento e método

Backup prévio de fontes e scripts em `.downloads/m002_a1_2/backup`. Mundo copiado para `run-a1-2/saves/TOE_A1_2`, ignorado pelo Git; mundo normal `run/` preservado. Regressões A1/Roll usam o mundo de testes anterior em `run-r4/`, com saídas redirecionadas para a pasta A1.2. A câmera orbital, fixtures, referência numérica e medidas de benchmark só entram com `-PtoeA12Test`. O JAR normal não deve contê-las.

A primeira captura de diagnóstico encontrou noite no mundo copiado; foi descartada como evidência comparativa. Antes/depois publicados usam meio-dia e clima fixos. Não foram editados relatórios históricos A1.1.

## Resultados executados

| Suíte | Resultado | Evidência em `animation_pipeline/reports/m002_a1_2/` |
|---|---:|---|
| Caminho anterior, 30 sequências e controles AUTO | 178/178 | `before/RUNTIME.json` |
| Caminho posterior, 30 sequências e controles AUTO | 178/178 | `after/RUNTIME.json` |
| Regressão A1: combos, pesado, cancelamento, dano e sincronização | 140/140 | `regression_a1/RUNTIME.json` |
| Regressão Roll físico, oito direções, obstáculos, bordas e ciclo de vida | 167/167 | `ROLL_REGRESSION.json` |
| Limites exatos de i-frames no servidor e dano real | 24/24 | `IFRAMES.json` |
| Auditoria comparativa de poses/apoio/empunhadura | 45/45 | `COMPARISON.json` |
| Comparador no Edge existente, imagens, controles 1×/0,25× e ausência de erros JS | 6/6 | `COMPARATOR_TEST.json` |
| Equivalência numérica da otimização final | 1.183 quadros, PASS | `EQUIVALENCE.json` |
| Nova JVM: AUTO, R3 e ativação por espada sem comandos | 3/3 | `after/RESTART.json` |

As 178 verificações incluem entrada natural sem comandos, AUTO/ON/OFF, troca de equipamento, item não suportado, prioridade contextual, offhand/armadura, mineração, manutenção de USE, remapeamento, morte, reconexão e recuperação das ações. Não houve erro do renderer. Os cancelamentos visuais foram amostrados em A/B/C/pesado e três ângulos.

A regressão A1 amostrou 23 verificações de sincronização visual/impacto. Sua quantidade de checks pode variar por amostragem cliente e não cobriu o tick exato 10 da proteção. Por isso foi acrescentada fixture dedicada no tick do servidor, cobrindo idades 2, 3, 9 e 10. Melee/projétil hostil são bloqueados somente na janela original; lava, queda e vazio não ganham proteção. Dano real: idade 3 mantém 20 HP; idade 10 reduz para 18 HP. O i-frame original permanece `[3,10)` ticks.

## Desempenho

Mesmo cenário e equipamento, 854×480, render/simulation distance 5, gráficos fancy, FOV 50, VSync e limite 60 FPS. Aquecimento de 320 ticks e descarte dos primeiros 20 de cada bloco. Três repetições por cenário de 120 ticks. Sem captura PNG nem probe de ossos durante as medições.

Pipeline CPU mede sampler + compositor + chamadas existentes de corpo/item; não é tempo de GPU nem do quadro inteiro. 1% low = 1.000 / média do 1% mais lento dos intervalos de quadro. GPU não disponível.

| Cenário | CPU pipeline médio antes → depois | FPS médio antes → depois | Quadro médio antes → depois | 1% low antes → depois |
|---|---:|---:|---:|---:|
| Idle | 0,613 → 0,573 ms | 59,69 → 59,62 | 16,754 → 16,772 ms | 44,48 → 43,01 |
| Apoio B | 0,604 → 0,575 ms | 59,73 → 59,68 | 16,742 → 16,756 ms | 44,40 → 49,11 |
| Transições repetidas | 0,612 → 0,561 ms | 59,75 → 59,67 | 16,736 → 16,759 ms | 46,38 → 49,97 |

O caminho inicial sem otimização aumentava o pipeline até 0,088 ms: havia decomposição/recomposição até sem blend. Foi substituído por cópia direta das matrizes do sampler em estado estável, com snapshot local somente quando necessário. A empunhadura também reutiliza a pose calculada, evitando o segundo compute antigo. A equivalência foi comprovada antes da medição final.

O custo médio de CPU final não aumentou nesta máquina. Foram executadas duas medições completas posteriores, cada uma com três repetições por cenário. A tabela usa a confirmação, `PERFORMANCE.json`; a primeira permanece integral em `PERFORMANCE_FINAL_RUN1.json` e `after/BENCHMARK_FINAL_RUN1.json`. Nela, CPU ficou em 0,497/0,531/0,513 ms e 1% low em 49,39/29,28/45,33 FPS para Idle/B/transições.

Na primeira execução houve uma pausa de quadro de 143,87 ms no bloco B, que reduziu seu 1% low; pipeline CPU máximo naquele bloco foi 1,65 ms. A pausa longa não se repetiu na confirmação. Sua origem fora da seção medida não foi identificada e não deve ser atribuída automaticamente à GPU, GC ou ao compositor. Não foi removida das estatísticas da primeira execução. Os dois resultados apoiam ausência de aumento médio do pipeline, não garantia de ausência de stutter. FPS médio com VSync não permite concluir desempenho sem limite de quadros. Consulte dados brutos, p95 e repetições em `PERFORMANCE.json`, `FRAME_STALLS.json` e `before/after/BENCHMARK.json`.

## Reproduzir

No terminal da pasta `minecraft`:

```powershell
.\scripts\test-a12.ps1 -Suite Visual
.\scripts\test-a12.ps1 -Suite Baseline
.\scripts\test-a12.ps1 -Suite Benchmark
.\scripts\test-a12.ps1 -Suite BaselineBenchmark
.\scripts\test-a12.ps1 -Suite Iframes
.\scripts\test-a12.ps1 -Suite Equivalence
.\scripts\test-a12.ps1 -Suite Restart
node animation_pipeline/scripts/build-a12-comparator.cjs
node animation_pipeline/scripts/audit-a12-comparison.cjs
node animation_pipeline/scripts/analyze-a12-performance.cjs
node animation_pipeline/scripts/audit-a12-stalls.cjs
node animation_pipeline/scripts/verify-a12.cjs
node animation_pipeline/scripts/summarize-a12.cjs
.\scripts\gradle.ps1 runClient -PtoeA1Test -PtoeA12Regression --offline
.\scripts\gradle.ps1 runClient -PtoeRollTest -PtoeA1Regression -PtoeA12Regression --offline
.\scripts\gradle.ps1 build -PtoeR31 --offline
.\scripts\gradle.ps1 build --offline
```

As fixtures precisam das dependências locais e do mundo isolado existente. Reexecutar Visual/Baseline substitui somente as evidências A1.2 correspondentes. Não execute dois clientes/benchmarks simultaneamente.

## Preservação e limites

Build experimental R3.1: **PASS, 18 s**. Build normal final: **PASS, 13 s**. O experimental apresentou seis avisos conhecidos de APIs depreciadas, sem falha. O build comprova compilação/preservação de R3.1, não homologação estética do seu renderer. `runClient` foi executado nos perfis de captura, benchmark, i-frames, equivalência, reinício e regressões; todos encerraram normalmente.

JAR normal final: `build/libs/towerofentropy-0.0.1.jar`, **419.521 bytes**, 84 entradas. SHA-256: `C23774C387E5EFAF3137AF4148B2A8FF2DB2E4BCB2C4E90393A5C47C9F5F3E4A`. `ARTIFACT.json` confirma inclusão do compositor/configuração/metadados de contato, GLB aprovado idêntico e ausência de fixtures QA ou renderer R3.1. Auditoria reproduzível: `powershell -ExecutionPolicy Bypass -File scripts/audit-a12-artifact.ps1`. O script foi ajustado para conferir o GLB no diretório de runtime convertido, que é sua localização real; nenhuma alteração do asset foi necessária.

`PRESERVATION.json`: 171 arquivos Godot intactos; oito assets aprovados byte a byte; 25 entradas históricas de modelos/experimentos preservadas. Corpos dos métodos de física/predição Roll e tick de impacto permanecem iguais ao backup; regras de dano, seleção de alvo e i-frames também. O cancelamento mudou apenas a entrega visual intermediária.

Os resultados são do cliente integrado local. Multiplayer remoto, servidor dedicado com latência, terrenos irregulares e paridade estética CPM/R3.1 continuam pendentes. Seleção de alvos hostis vanilla permanece a anterior; não foram adicionados PvP ou novos alvos. A aprovação estética do usuário não foi concedida por este relatório.
