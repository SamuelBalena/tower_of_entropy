# M002.A3 — Testes e entrega

13/11/2026. Windows, Minecraft 1.20.1, Forge 47.4.28, Microsoft JDK 17.0.20.1 x64, Gradle 8.8, Polymesh 1.0.0 e CPM 0.6.26a. Conversão pelo Blender 5.2.3 LTS já instalado. Nenhuma ferramenta, dependência ou mod de gameplay novo foi instalado.

## Isolamento e preservação

Backup anterior ao lote em `.downloads/m002_a3/backup`, baseline em `animation_pipeline/reports/m002_a3/BASELINE.json`. Perfil/mundo isolado `run-a3/saves/TOE_A3`; mundo normal `run/` e Godot preservados. Fontes Archer copiadas somente após conferir a licença; intermediários ficam ignorados pelo Git.

`validate-a3.cjs` confirmou os 171 arquivos Godot previamente inventariados, assets aprovados, dados da malha/hierarquia/UVs/pesos e os nove clipes A2 intactos. Código autoritativo de combate, física/predição do Roll, regras de dano e i-frames não foi alterado. As fixtures antigas só receberam caminhos de saída A3, preservando seus relatórios anteriores.

## Resultados executados

| Suíte | Resultado | Evidência em `animation_pipeline/reports/m002_a3/` |
|---|---:|---|
| 28 roteiros reais por categoria, incluindo reconexão | 43/43 | `RUNTIME.json` |
| Rodada lateral: arco, itens, direções, água e knockback | 16/16 | `EXTRA.json` |
| Arco final frente/lateral, sneak reverso e água | 11/11 | `FINAL_CHECKS.json` |
| Auditoria de clipes realmente desenhados e matrizes | 32/32 | `VISUAL_ANALYSIS.json` |
| Regressão A1: combos, pesado, dano e cancelamentos | 145/145 | `regression_a1/RUNTIME.json` |
| Regressão Roll físico: direções, paredes, bordas e recuperação | 167/167 | `ROLL_REGRESSION.json` |
| I-frames exatos no tick servidor e dano real | 24/24 | `IFRAMES.json` |
| Preparação/salvamento do circuito | 1/1 | `CIRCUIT.json` |
| Reinício em outra JVM: AUTO/R3 e ações limpas | 2/2 | `RESTART.json` |
| Direção matemática do adaptador do arco | 125 casos | `BOW_AIM.json` |
| Visualizador no Edge: imagens, seleção, fallback, 1×/0,25× | 6/6 | `VIEWER_TEST.json` |

O runtime verificou flecha vanilla real e consumo de uma flecha por disparo; troca de arco para espada não disparou nem iniciou pesado indevidamente. Comer consumiu uma maçã e elevou fome de 8 para 12; beber produziu garrafa vazia. A picareta vanilla quebrou o bloco, e o baú abriu a interface. Dano real foi conferido no instante de aplicação; morte e respawn completaram sem estado preso.

Start/Loop/Land foram observados no salto; queda, água, crouch e direções foram acionados pelo estado físico real. `Hit_Knockback` foi observado após impulso vanilla na rodada lateral. Não foi inventada detecção de headshot para usar `Hit_Head`.

A auditoria das poses ativas examinou 829 amostras, com matrizes finitas, erro máximo de escala de aproximadamente 0,00000184 e de comprimento de osso de 0,000000552. Isso verifica ausência de estiramento introduzido pelo compositor, não ausência de toda interseção estética.

I-frames conservaram as idades servidor 2/3/9/10 e a janela `[3,10)` ticks. Melee/projétil hostil foram bloqueados somente na janela; lava, queda e vazio não ganharam proteção. O controle visual não altera vida, invulnerabilidade, knockback, duração de ataques, colisões ou velocidades.

## Problemas encontrados e tratados

- O FBX Archer tinha compensação de root diferente do repouso exportado, produzindo cabeça girada em 90°. A normalização e a adaptação geométrica dos eixos de cotovelo/joelho foram corrigidas e recapturadas.
- Misturar o torso Archer com outra pose de pernas deixou a mira inadequada. Essa combinação não foi promovida: arco mirando em movimento, agachado, ar/água ou ângulo extremo usa fallback vanilla. A pose completa foi mantida para os contextos aceitos.
- A montagem do sprite vanilla do arco precisou considerar sua flecha diagonal NW. O transform final foi verificado contra 125 combinações de yaw/pitch/offset e capturado lateralmente; o teste é do espaço de desenho, não uma medição de física do projétil.
- Consume e tocha utilizam a mão de suporte da fonte. Os slots, consumo e lógica de uso permanecem vanilla. O encaixe fino da mão de tração do arco ainda depende da avaliação estética.
- Duas assertivas iniciais verificavam vida muito depois de dano de 2 HP; regeneração vanilla já a havia restaurado. A fixture foi corrigida para verificar antes/depois no instante do dano e a bateria foi repetida: 43/43. A regeneração não foi alterada.
- O primeiro teste de baú não apontava a câmera cliente para o bloco. Foi corrigido o posicionamento da fixture e a abertura real foi confirmada. Não houve atraso de UI adicionado pelo mod.
- Picareta customizada: UASSET localizado, porém sem licença local identificável e sem exportador Unreal compatível disponível. Ficou bloqueada, preservando mineração vanilla funcional.

Tentativas anteriores foram guardadas na área privada `.downloads/m002_a3/first-pass`; as evidências publicadas refletem as rodadas corrigidas. Capturas do adaptador final do arco e do sneak reverso são selecionadas de `FINAL_CHECKS.json` pelo visualizador.

## Evidência visual

36 sequências e **1.296 PNG únicos**, a 10 Hz, incluindo vistas frontal/lateral e situações de fallback. [VIEWER.html](../animation_pipeline/reports/m002_a3/VIEWER.html) reproduz 1× e câmera lenta, sem inventar quadros. A amostragem tem resolução de 100 ms e pode mostrar o frame anterior ao estado atualizado no tick.

[REVIEW_COMPACT.zip](../animation_pipeline/reports/m002_a3/REVIEW_COMPACT.zip): **15 PNG**, HTML e README, **1.209.606 bytes**. É uma seleção estática pequena; a reprodução completa depende da pasta screenshots. Aprovação estética pendente.

## Builds e artefato

- Experimental R3.1: **PASS, 20 s**.
- Normal final: **PASS, 15 s**.
- `runClient`: concluído nas rodadas de categoria, lateral, verificações finais, regressões, i-frames, circuito e reinício.
- JAR normal: `build/libs/towerofentropy-0.0.1.jar`, **1.703.487 bytes**, 98 entradas.
- SHA-256: `A8F17FAF359B1BD8A1A95E5CCA366B8E2D6D3849D2A06E085322511C1751E382`.

`ARTIFACT.json` confirma os hashes dos modelos, os recursos do lote e notices de terceiros. O JAR normal não contém fixtures QA, classes experimentais R3.1, FBX ou rig Archer original. R3.1 foi compilado e preservado, sem homologação visual dos estados novos nesse renderer.

Não foi realizado benchmark formal de desempenho A3. Os números A2 não são apresentados como medição deste lote. GPU, multiplayer remoto e servidor dedicado com latência não foram homologados.

## Reprodução e circuito

```powershell
.\scripts\play-a3.ps1
.\scripts\test-a3.ps1 -Suite Runtime
.\scripts\test-a3.ps1 -Suite Extra
.\scripts\test-a3.ps1 -Suite Final
.\scripts\test-a3.ps1 -Suite Iframes
.\scripts\test-a3.ps1 -Suite Circuit
.\scripts\test-a3.ps1 -Suite Restart
.\scripts\gradle.ps1 runClient -PtoeA1Test -PtoeA3Regression --offline
.\scripts\gradle.ps1 runClient -PtoeRollTest -PtoeA1Regression -PtoeA3Regression --offline
node animation_pipeline/scripts/validate-a3.cjs
node animation_pipeline/scripts/audit-a3-runtime.cjs
node animation_pipeline/scripts/test-a3-bow-aim.cjs
node animation_pipeline/scripts/build-a3-viewer.cjs
powershell -ExecutionPolicy Bypass -File scripts/audit-a3-artifact.ps1
node animation_pipeline/scripts/summarize-a3.cjs
```

O perfil de jogo não inclui fixtures. Abra o mundo da pasta **TOE_A3** em Singleplayer; o nome principal herdado pode aparecer como “New World”, com TOE_A3 na linha da pasta. As animações são automáticas para Steve Classic, sem comandos de ativação. F5, WASD, sprint, sneak, ATTACK e USE são controles vanilla. Os roteiros de QA usam Steve Classic para comparação; skins/equipamentos não suportados mantêm fallback.

O circuito contém piscina, teto para crouch, plataforma/queda, alvos, árvore/pedra, estoque de itens e cercado com zombie vanilla. O mundo de teste usa keepInventory, dia fixo e respawn no spawn; essas regras não foram aplicadas ao mundo normal. Reexecutar Circuit reinicializa o cenário de QA, não o projeto Godot.

Escada e pesca tiveram verificação runtime de fallback; crawling, Elytra e montarias/remo permanecem excluídos por gates explícitos, sem uma bateria dinâmica completa dessas combinações. Oxigênio, encantamentos e resistência de blocos não foram reimplementados; sua lógica vanilla ficou intacta. Não foi alegado teste exaustivo de cada encantamento, terreno ou mod externo.

Resultado consolidado: [RESULTS.json](../animation_pipeline/reports/m002_a3/RESULTS.json). Nenhum marco posterior foi iniciado.
