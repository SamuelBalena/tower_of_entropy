# M002.A1 — Registro técnico de testes

13/11/2026. Windows, Microsoft JDK 17.0.20.1 x64, Forge 47.4.28, Minecraft 1.20.1, Gradle 8.8, Polymesh 1.0.0, CPM 0.6.26a, Blender 5.2.3 LTS. Nenhuma ferramenta ou dependência nova instalada.

## Isolamento e evidências

Testes usam o mundo de desenvolvimento `run-r4/TOE_R4`; não modificam o mundo normal `run/` nem Godot. Fixtures `validation/a1` só entram com `-PtoeA1Test` e não acompanham o build normal. Backup pré-A1: `.downloads/m002_a1/backup`; inventário de hashes em `animation_pipeline/reports/m002_a1/BASELINE.json`.

Evidências em `animation_pipeline/reports/m002_a1/`: `RUNTIME.json` contém checks e telemetria servidor; `TIMELINE_AUDIT.json` verifica sequência/fila/tempos; `PRESERVATION.json` verifica assets; `KEYFRAME_ANALYSIS.json` registra amostras fonte; `screenshots/` contém PNGs reais. Logs brutos em `.logs/m002-a1-*.log`.

## Executado

- Cliente real, renderizador normal R3, servidor integrado: **145/145 verificações** na bateria principal do candidato final.
- Combo A/B/C e limite de entradas, pausa reiniciando A, pesado isolado, transições após recuperação leve→pesado e pesado→leve.
- Roll interrompendo leve/pesado antes do impacto: zero impactos restantes, movimento final 2,5 blocos e recuperação limpa.
- Dano real contra Vindicator vanilla sem armadura: saúde 100→96 no leve, 100→94 no pesado e 100→88 no combo completo A/B/C. Tentativa única por golpe; dano continua aplicado por servidor/vanilla.
- Eventos Forge com ATTACK, USE e ATTACK remapeado: consumo, swing suprimido e travamento de duplicata mantida. Teste usa o evento real com KeyMapping remapeada; não simula hardware físico de mouse/teclado via sistema operacional.
- Política de i-frames nos ticks 2, 3, 9, 10: melee e flecha com proprietário hostil, lava, queda e vazio. São eventos reais Forge; a fixture não equivale a uma sessão manual contra um esqueleto.
- Eventos de mineração/blocos preservados, escudo/offhand preservado, ferramenta sem conjunto próprio, menus/chat sem envio, morte/respawn e reconexão sem estado preso.
- Regras anteriores: **104 checks Roll**, **18 checks geometria/linha do tempo de ataque**, testes de arbitragem visual e histerese de locomoção passaram.
- Preservação: 171 arquivos Godot, 25 entradas históricas de modelos/experimentos e geometria/UVs/rig/pesos/quatro animações originais R3 conferidos por hash/comparação de buffers.

## Inspeção visual e correções durante o trabalho

O primeiro recorte pesado 0–24 passou em lógica, mas foi rejeitado visualmente por acabar dentro de um giro. A implementação final usa 60–130 e preserva a recuperação original. Os resultados 81/81 do recorte anterior estão identificados como históricos e não comprovam aprovação final.

Uma execução do perfil de captura falhou por variável local duplicada na fixture Java; o teste foi corrigido e reexecutado. A sessão de capturas após a morte também foi separada para reequipar a espada antes de inspecionar a empunhadura. Esses problemas eram de automação de QA, não foram ocultados como resultados aprovados.

**Aprovação estética pendente:** novo pesado, postura inicial inclinada, transições entre clipes e leitura do combo. R3 aprovado anteriormente continua com a mesma geometria. Fallbacks CPM/vanilla não recebem os novos gestos B/C/pesado; R3 remoto, servidor dedicado com múltiplos clientes e condições reais de latência não foram homologados neste teste local.

Os testes de prioridade verificam que o mod não consome eventos destinados a blocos/escudo. Não se afirma ter testado manualmente cada porta, baú ou mod de interação existente.

## Reprodução

```powershell
.\scripts\test-a1.ps1
.\scripts\test-a1.ps1 -Visual
.\scripts\test-a1.ps1 -Restart
.\scripts\test-a1.ps1 -Cold
node animation_pipeline/scripts/audit-a1-runtime.cjs
node animation_pipeline/scripts/verify-a1.cjs
.\scripts\gradle.ps1 runClient -PtoeRollTest -PtoeA1Regression --offline
.\scripts\gradle.ps1 build -PtoeR31 --offline
.\scripts\gradle.ps1 clean build --offline
```

O perfil de regressão Roll preserva as expectativas antigas, exceto a decisão autorizada de permitir Roll cancelar ataque. Salva resultados A1 separados e não sobrescreve relatórios históricos M002.1. Nenhum próximo marco foi iniciado.

## Regressões e auditorias finais

- Roll físico: **168/168 checks** em ROLL_REGRESSION.json, incluindo oito direções, WASD real da KeyMapping V, diagonais, parede, borda/gravidade, armadura, capa, offhand, primeira pessoa, entradas inválidas, morte e reconexão.
- Auditoria da telemetria: **10/10 checks** em TIMELINE_AUDIT.json, sequência A/B/C, reinício por pausa, transições para/de pesado, fila máxima de duas entradas e impactos individuais nos ticks 4/5/14/12.
- Reinício em nova JVM: **1/1**, R3 padrão sem comandos e combate desligado.
- Captura técnica: **1/1**, modelo renderizando sem erros com empunhadura, além de PNGs dos leves e nove fases do pesado.

O primeiro teste frio solicitava ataque no mesmo instante do teleport da fixture. Foi interrompido corretamente por ground=false, antes do impacto. O relatório inicial e COLD_TELEPORT_DIAGNOSIS.json preservam o diagnóstico. O teste definitivo estabiliza o chão antes de ligar combate e solicitar o primeiro ataque no mesmo task.

Início a frio estabilizado: **3/3 checks** em `COLD.json`, incluindo primeiro ataque sem aguardar registro CPM, impacto único e sincronização visual. O wrapper `test-a1.ps1` verifica o conteúdo do relatório e retorna erro se houver checks reprovados; o sucesso de `runClient` sozinho não é considerado aprovação da suíte.

## Builds e entrega

Build experimental `-PtoeR31`: **PASS, 18 s**. Build normal `clean build`: **PASS, 20 s**. Permanecem avisos de APIs depreciadas do Forge/ResourceLocation, sem erro de compilação. O launcher `start-r31.ps1` seleciona o checkpoint A1 quando presente, preservando os validadores históricos e evitando que a adição autorizada de clipes seja tratada como alteração indevida da geometria.

Artefato normal: `build/libs/towerofentropy-0.0.1.jar`, **404.582 bytes**, 78 entradas. SHA-256: `C7676C618B215CA5270BA6B1D32E293CF3516EB63B4A048D8458BE0BE0BFB05D`. A auditoria confirmou ausência das fixtures A1/Roll/R4 e das classes/assets R3.1, e identidade do GLB empacotado com a cópia de runtime verificada. Não foi publicado nem instalado em launcher de terceiros.

Resumo estruturado: [RESULTS.json](../animation_pipeline/reports/m002_a1/RESULTS.json); [auditoria do JAR](../animation_pipeline/reports/m002_a1/ARTIFACT.json). A entrega é tecnicamente testável; aprovação estética continua pendente.
