# Testes M001.1

Data: 2026-11-12, America/Sao_Paulo.

## Resultado

**Build e execução no cliente passaram. Steve executando o Idle foi confirmado por capturas do jogo e mudança das rotações CPM em runtime.** O escopo é um teste experimental local em terceira pessoa, não validação de todas as skins, poses, servidores ou itens.

| Verificação executada | Resultado |
|---|---|
| CPM do M000 | 0.6.26a, Forge 1.20/1.20.1; SHA-512 conferido |
| CPM carregado antes da conversão | inicialização e API confirmadas em log |
| GLB fonte | hash conhecido preservado; Idle_Loop encontrado |
| Duração / amostragem | 2,5 s; 76 tempos fonte; 75 frames de loop a 30 Hz |
| Estrutura | seis raízes vanilla, 16 cubos de membros/camadas, dez peças animadas, IDs únicos |
| UV / valores | UVs em 64×64; números finitos; escala unitária; ângulos em [0,360) |
| Arquivos Godot | 171 hashes SHA-256 conferidos, sem alteração |
| Projeto CPM | carregado pelo ProjectIO instalado; uma animação, 2500 ms, loop, 75 frames |
| Binários CPM | exportados pelo CPM e relidos por ModelFile.load |
| Runtime | modelo LOADED; error=null; blocked=null; trigger STANDING presente |
| Movimento da animação | rotações do braço variam entre amostras; capturas em fases distintas |
| Posição real do jogador | drift=0.0 em três amostras do Idle e após off |
| Inventário | CreativeModeInventoryScreen abriu, verificação true |
| Desativação | off confirmado em chat/log e captura da skin vanilla anterior |
| Movimento básico após off | entrada vanilla de avanço por 20 ticks; distância 4,0554955314 blocos |
| Build final | BUILD SUCCESSFUL in 13s; saída 0 |
| runClient final | BUILD SUCCESSFUL in 2m 23s; saída 0 após fechamento normal |

Não há testes unitários Java: a tarefa test indica NO-SOURCE. A validação efetiva combina o script estrutural e o cliente real. Um build isolado não comprova o movimento visual.

## Reprodução mínima

Na pasta `minecraft`:

```powershell
node animation_pipeline/scripts/retarget-idle.cjs
node animation_pipeline/scripts/validate-idle.cjs
.\scripts\gradle.ps1 build --console=plain
.\scripts\gradle.ps1 runClient -PtoePrepareModel --console=plain
```

Aguarde no menu inicial o log `TOE_M001_1 EXPORT_VALID` e `STEVE_PREVIEW_VALID`. O parâmetro de preparação habilita o exportador por esta execução; ele usa o `.cpmproject` local e grava os dois modelos em `run/player_models/`. Sem esse parâmetro o exportador não roda; os modelos já gerados continuam utilizáveis.

Abra um mundo local com cheats/operador e teste:

```text
/toe_animtest idle_steve
```

Use F5 para terceira pessoa, fique parado e observe por mais de 2,5 segundos. `idle_steve` utiliza a textura Steve da instalação local. `/toe_animtest idle` usa a skin atual do jogador, com UV Classic. Para desativar:

```text
/toe_animtest off
```

O teste é opt-in, com permissão nível 2. A aplicação usa `ICommonAPI.setPlayerModel(Player.class, player, ModelFile, false)`, sem persistência de modelo entre sessões. A desativação usa `resetPlayerModel`. Não se alteram posição ou velocidade da entidade nos comandos.

Para repetir a verificação automatizada no mundo **New World criado especificamente por este marco**:

```powershell
.\scripts\gradle.ps1 runClient -PtoePrepareModel -PtoeVisualTest --console=plain
```

Esse parâmetro adicional abre `run/saves/New World`, muda temporariamente câmera/FOV e pausa por perda de foco, ativa a prévia, captura três fases, abre o inventário, desativa, captura o estado anterior e pressiona a entrada vanilla de avanço por 20 ticks. Depois restaura os parâmetros e solta a tecla. Não execute esse teste contra um save pessoal renomeado como New World. Sem `-PtoeVisualTest` ele permanece inativo; se o save não existir, aborta sem criar outro.

## Evidências

- Logs locais, ignorados: `.logs/m001_1-final-build.log` e `.logs/m001_1-final-client.log`.
- Diagnósticos anteriores: `.logs/m001_1-cpm-diagnostic.log`, `.logs/m001_1-cpm-loaded.log`, `.logs/m001_1-export-client.log`, `.logs/m001_1-visual-test.log`, `.logs/m001_1-visual-steve.log`.
- Capturas originais: `run/screenshots/toe-m001_1-idle-{0,13,26}.png` e `toe-m001_1-off.png`.
- Cópias de evidência e análise: [relatório de retargeting](../animation_pipeline/reports/M001_1_RETARGET_REPORT.md).
- `M001_1_SOURCE_ANALYSIS.json` e `M001_1_VALIDATION.json` em `animation_pipeline/reports/`.

No teste final: posição inicial `(-56.5,68.0,12.5)`; drift zero durante Idle e até a verificação de off. A movimentação explícita ocorre somente depois dessas medições. Braço esquerdo em graus: fase 0 `(13,968;19,655;-17,784)`, fase 13 `(8,686;21,353;-16,615)`, fase 26 `(13,953;19,236;-18,050)`.

## Ocorrências e limites

1. Falha inicial Mixin SRG/userdev: corrigida com remapeamento de refmap. CPM carregou antes da conversão.
2. Ângulos negativos na primeira exportação: truncados pelo escritor sem sinal; corrigidos por normalização e conferidos em runtime.
3. Dev recebe skin vanilla diferente de Steve: criada prévia local Steve separada, mantendo o modelo principal compatível com a skin atual.
4. A primeira sondagem de inventário esperava InventoryScreen; no Creative o Minecraft troca para CreativeModeInventoryScreen. A verificação final confirmou o container correto.
5. Captura inicial por região de desktop não isolou a janela; foi substituída por captura direta da janela e, na validação final, pelo Screenshot.grab do Minecraft. Somente capturas do próprio jogo foram usadas como evidência.
6. Avisos informativos: Realms sem autorização na sessão dev, sampler de shader, configurações iniciais Forge e APIs depreciadas para Gradle 9. Não impediram os testes; não houve mudança de autenticação ou atualização de stack.

Pernas/pés estão parcialmente encobertos por vegetação nas capturas. Não foram testadas todas as vistas, skins Slim/64×32, camadas por configuração, armaduras, multiplayer, primeira pessoa ou transições com o modelo ativo. Não há conversões de Run/Roll/ataques nem sistemas de gameplay. M001.2 não iniciado.
