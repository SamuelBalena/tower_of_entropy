# Testes M001.2 — Idle/Jog e fallback

Data: 2026-11-12, America/Sao_Paulo. Minecraft 1.20.1, Forge 47.4.28, CPM 0.6.26a, Microsoft JDK 17.0.20.1 x64, Gradle 8.8, ForgeGradle 6.0.54, Node 24.16.0. Nenhuma ferramenta ou mod adicional foi instalado.

## Resultados executados

| Teste | Resultado |
|---|---|
| Jog original GLB | Jog_Fwd_Loop, 0,933333337 s, 29 instantes fonte |
| Retarget | mesmo rig e UVs do Idle; 28 frames; 933 ms; loop |
| M001.1 | hashes dos projetos/modelos de referência preservados; validador Idle passa |
| Projeto CPM | carregado pelo CPM instalado; duas poses customizadas com nomes e durações conferidos |
| Binários CPM | exportação oficial e releitura ModelFile.load passam; 13.211 / 15.514 bytes |
| Máquina de estados | testes Java de estabilidade, histerese, direção, fallback, entradas finitas e nova sessão passam |
| Cliente real — Idle | estado IDLE e pose CPM toe_idle confirmados |
| Cliente real — avanço | estado JOG e pose CPM toe_jog em duas fases, com capturas |
| Parada | volta a IDLE/toe_idle |
| Alternância repetida | Jog→Idle→Jog→Idle confirmados |
| Direção lateral/traseira | VANILLA_FALLBACK, sem modelo automático ativo |
| Pulo/queda | fallback em duas amostras no ar; velocidade Y negativa e positiva observadas |
| Agachamento | fallback sem modelo automático ativo |
| Aterrissagem/parada | volta ao Idle após estabilização |
| Skin atual Classic | auto usa o modelo com skin variável; modelo/pose carregados |
| Off | sessão null, sem comandos de locomoção no modelo ativo |
| Reconexão com auto previamente ligado | volta sem controlador e sem modelo automático |
| Reinício do cliente | check separado confirma controlador/pose automática ausentes |
| Comando M001.1 | idle_steve carrega após reconexão; off restaura depois |
| Preservação Godot | 171 arquivos relidos, hashes SHA-256 idênticos |
| Física na faixa plana | avanço por 40 ticks dá a mesma distância com e sem controlador |

Execução final do cliente: **22/22 checks passaram**. Reinício: **1/1 passou**. Os testes Java e Node são verificações reais executadas separadamente; a tarefa Gradle `test` continua NO-SOURCE e não representa essa suíte.

Na comparação de movimento:

- Vanilla sem controlador: `8.374761235223515` blocos.
- Automático: `8.374761235223513` blocos.
- Mesma duração de entrada, direção, piso e velocidade vanilla; diferença somente de ponto flutuante.

No trecho estável de Jog, `poseCommands=2` e `modelAssignments=1`: Idle inicial e entrada Jog, sem novos comandos nos ticks seguintes. Depois de parar, `poseCommands=3`, mantendo a mesma aplicação do modelo. O controle só interfere no CPM; não implementa movimento próprio.

## Execução reproduzível

Na pasta `minecraft`:

```powershell
node animation_pipeline/scripts/retarget-locomotion.cjs
node animation_pipeline/scripts/validate-idle.cjs
node animation_pipeline/scripts/validate-locomotion.cjs
.\scripts\test-locomotion.ps1
.\scripts\gradle.ps1 build --console=plain
.\scripts\gradle.ps1 runClient -PtoePrepareLocomotion --console=plain
```

A preparação lê o projeto novo e usa o editor CPM instalado para exportar os modelos. Espere `TOE_M001_2 EXPORT_VALID` para as variantes com skin atual e Steve. As conversões e modelos são locais/ignorados, não embutidos no JAR.

Em mundo local com cheats/operador, use:

```text
/toe_animtest auto
/toe_animtest auto_steve
/toe_animtest off
```

Use apenas uma ativação por vez. `auto` usa a skin atual Classic, `auto_steve` usa a prévia Steve. Ambos selecionam poses pelos estados reais. `/toe_animtest idle` e `/toe_animtest idle_steve` preservam o teste M001.1 e retiram o controlador automático antes de aplicar o modelo antigo.

## Teste automatizado no save descartável

```powershell
.\scripts\gradle.ps1 runClient -PtoePrepareLocomotion -PtoeLocomotionTest --console=plain
```

**Somente para o save `run/saves/New World` criado nos marcos anteriores.** O teste prepara uma faixa de pedra/ar nesse mundo, simula entradas vanilla de movimento, muda temporariamente câmera/FOV/pausa por perda de foco e fixa a orientação na direção da faixa. Não use um save pessoal renomeado como New World. Se o save não existir, a rotina aborta sem criar outro.

Essa modificação de terreno e os controles de câmera/teclas pertencem exclusivamente ao diagnóstico opt-in, não ao controlador. O jogador não é teleportado nem recebe velocidade ou atributos modificados. Ao terminar, solta as teclas, restaura as opções e a orientação, desativa a experiência e preserva as evidências. A rotina também sai/reentra no mundo com auto antes ligado e testa os comandos M001.1 na nova sessão.

Para a verificação separada após reiniciar o processo:

```powershell
.\scripts\gradle.ps1 runClient -PtoeLocomotionRestartCheck --console=plain
```

Esse modo apenas abre o save de teste e verifica ausência de controlador/poses automáticas; não habilita auto nem altera o terreno. Os diagnósticos não executam num runClient normal. Mantenha `-PtoeVisualTest` para repetir a rotina original do M001.1; ela não foi removida.

## Evidências e problemas resolvidos

- `.logs/m001_2-build.log`: primeira compilação, BUILD SUCCESSFUL in 19s.
- `.logs/m001_2-final-build.log`: compilação final, resultado registrado ao final deste documento.
- `.logs/m001_2-client-final.log`: suíte final, BUILD SUCCESSFUL in 3m 20s após encerramento normal.
- `.logs/m001_2-restart.log`: cliente reiniciado e check de ausência de pose, encerrado normalmente.
- [Checks do cliente em JSON](../animation_pipeline/reports/M001_2_RUNTIME_TESTS.json), [check de reinício](../animation_pipeline/reports/M001_2_RESTART_TEST.json), [validação estrutural](../animation_pipeline/reports/M001_2_VALIDATION.json).
- [Relatório e capturas](../animation_pipeline/reports/M001_2_LOCOMOTION_REPORT.md).

Ocorrências:

1. A exportação inicial das poses customizadas falhou porque `layerControlled=true` exigia reserva de bits das camadas da skin. Corrigida para `layerControlled=false`, usando o canal de comandos/rede CPM. Não foram alteradas preferências de roupa/skin para contornar a falha.
2. A primeira suíte física passou em 20/22 checks. O yaw salvo apontava fora da faixa e o jogador colidiu com terreno, invalidando a comparação de distância e uma repetição Jog. A fixture passou a manter direção alinhada com a faixa; a nova execução passou em 22/22, com distâncias iguais. Não foi alterada a lógica do controlador para mascarar a colisão.
3. Avisos de Realms na sessão de desenvolvimento e APIs Gradle depreciadas permanecem; não impediram o teste nem motivaram alterações de autenticação/stack.

## Qualidade visual e testes pendentes

Foi possível confirmar visualmente o Steve cúbico e fases distintas do Jog, além do retorno ao Idle. **Isso não equivale a homologar a suavidade:** as trocas são cortes exclusivos, sem crossfade configurável demonstrado pela API. Há risco de salto de pose nas transições e latência ao reaplicar o modelo após fallback.

O ciclo fixo de 933 ms não se ajusta à velocidade. A estimativa cinemática dos pés aponta discrepância de contato; não há IK ou foot planting. O usuário ainda deve avaliar, em movimento contínuo, se a cadência, o deslizamento dos pés e os cortes são aceitáveis. Não é necessário programar ou modelar para fazer essa validação: basta ativar auto, avançar/parar e observar em F5.

Não foram realizados testes físicos de nado, lava, escalada, montaria, elytra ou sprint; existem guardas de fallback, mas isso não é prova visual de todos esses estados. Também permanecem sem homologação skins Slim/64×32, armaduras, itens, primeira pessoa, multiplayer remoto e condições de latência. A reconexão com auto ligado e o reinício após encerramento normal foram testados; encerramento abrupto/crash não foi simulado.

Nenhum Roll, ataque, combate, stamina, habilidade ou classe foi integrado. M001.3 não iniciado.

## Encerramento final

- Build final: `BUILD SUCCESSFUL in 12s`, saída 0.
- Reinício/runClient: `BUILD SUCCESSFUL in 4m 4s`, saída 0 após fechamento normal; check client_restart_disabled passou.
- Ignore verificado em repositório temporário isolado sob `.logs`: projetos/modelos e dados intermediários ignorados; scripts e documentação não ignorados.
- Os diagnósticos e mundos são locais, sem commit, publicação ou alteração do projeto Godot.