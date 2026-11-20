# M001.2 — Locomoção Idle/Jog

Data: 2026-11-12, America/Sao_Paulo.

**Funcional no teste local:** o modo automático seleciona o Idle UAL1 quando parado e o Jog UAL1 ao avançar no chão. O teste em cliente real confirmou as poses exclusivas, o retorno ao Idle, fallback vanilla e desativação. As transições são cortes entre poses, não crossfades; o contato dos pés ainda é experimental.

## Inspeção e preservação do M001.1

Foram lidos o relatório de retargeting e o TEST_LOG M001.1 antes das alterações, além do código, dos projetos CPM e da implementação oficial CPM disponível localmente. O Idle anterior usa uma animação vanilla `STANDING`, 2500 ms, 75 frames, no rig Classic de seis raízes e membros divididos. A aplicação usa `ICommonAPI.setPlayerModel` e off usa `resetPlayerModel`.

Os projetos e modelos de Idle do M001.1 não foram substituídos pelos modelos de locomoção. `M001_2_BASELINE.json` registra os hashes anteriores; `validate-locomotion.cjs` confirmou que continuam idênticos. Os comandos `idle`, `idle_steve` e `off` permanecem. O teste M001.1 continua disponível com `-PtoeVisualTest`; houve também verificação do comando antigo no cliente após a reconexão.

O retargeting foi reutilizado com um argumento `--jog`; a execução sem argumentos mantém a saída original byte a byte. Somente o escritor ZIP foi extraído para `zip-project.cjs`, para compartilhar o formato do projeto. Não houve reescrita das transformações ou do rig.

## Jog importado e root motion

- Fonte: `animation_pipeline/input/UAL1/UAL1_Standard.glb`, SHA-256 `69591853d817488edaa8fd9bf8fc1d821eaeaf789f8627b3cd23b41c4ed67997`.
- Único clipe novo: **Jog_Fwd_Loop**. Idle_Loop foi reaproveitado; nenhum Roll, ataque ou outro movimento foi convertido.
- Duração GLB: `0.9333333373069763` s; duração CPM inteira: **933 ms**.
- 29 instantes fonte, aproximadamente 30 Hz, com endpoint final; 28 frames de loop, interpolação `linear_loop`.
- 195 canais: posição, quaternion e escala dos 65 joints. Não foi encontrada variação relevante de escala.
- Translação `root` constante `[0,0,0]`. Pelvis local varia aproximadamente X=-0,017054..0,018845, Y=0,056568..0,056644, Z=0,700803..0,945624.
- Todas as translações da origem são descartadas no destino. Pivôs do Steve derivam da rotação do torso, sem escrita da posição/velocidade da entidade. Não há deslocamento duplicado por root motion.
- Diferença angular entre endpoints da fonte: aproximadamente 0,647°. Há uma pequena costura herdada; não foi inventado um ciclo substituto.

Metadados completos: [M001_2_SOURCE_ANALYSIS.json](M001_2_SOURCE_ANALYSIS.json).

## Mesmo Steve articulado

Cabeça 8×8×8, torso 8×12×4 e membros 4×12×4, com segmentos de seis pixels em cotovelos/joelhos. UVs Classic 64×64, seis raízes vanilla e 16 cubos incluindo camadas. O manequim UAL não foi importado como geometria do jogador.

| Ossos UAL1 | Peças do Steve |
|---|---|
| `Head` | cabeça / ID 0 |
| `spine_03`, com ancestrais | torso / ID 1 |
| `upperarm_l`, `upperarm_r` | braços / IDs 2,3 |
| `thigh_l`, `thigh_r` | coxas / IDs 4,5 |
| `lowerarm_l`, `lowerarm_r` | cotovelos / IDs 102,103 |
| `calf_l`, `calf_r` | joelhos / IDs 104,105 |

Bind pose, mudança de base, SLERP, Euler ZYX e ângulos CPM sem sinal seguem o M001.1. Cotovelos e joelhos preservam a flexão e descartam torção axial/lateral. Dedos e articulações independentes de pés/mãos continuam fora do rig experimental. O validador compara integralmente a configuração do rig Jog com a do Idle.

## Modelos e exportação CPM

Arquivos locais em `animation_pipeline/converted/m001_2/`, todos ignorados pelo Git:

| Arquivo | Uso | Tamanho nesta execução |
|---|---|---:|
| `steve-ual1-jog.cpmproject` | Jog isolado para inspeção, pose WALKING | 11.295 bytes |
| `steve-ual1-locomotion.cpmproject` | projeto automático, skin do jogador | 36.124 bytes |
| `steve-ual1-locomotion.cpmmodel` | modelo automático, skin do jogador | 13.211 bytes |
| `steve-ual1-locomotion-preview.cpmproject` | projeto de prévia Steve local | 30.940 bytes nesta exportação |
| `steve-ual1-locomotion-preview.cpmmodel` | modelo de prévia Steve | 15.514 bytes |
| `jog-data.json`, `locomotion-data.json` | dados intermediários | locais |

Os dois binários foram gerados por `Exporter.exportModel` do CPM instalado e relidos por `ModelFile.load`. Os projetos foram carregados por `ProjectIO.loadProject`, com validação de nomes, duas animações, frames, duração e loop. O tamanho relevante para o limite local do CPM é o binário exportado, não o ZIP de projeto.

SHA-256 do modelo de skin variável: `3b14c526550414ac24e1ab5112b65dd6ba7cca20cb430d8abe0e06710d522dc6`.
SHA-256 da prévia Steve: `4c58ab5bab1b33c5dc2a790bdb1b28e9234f9920d59667b612b02e6a473e31e5`.

Modelos de uso ficam também em `run/player_models/`, com prefixo `toe-`. Não são incorporados ao JAR do mod. A versão principal usa a skin do jogador; a prévia usa Steve do recurso vanilla já instalado, sem alterar a skin da conta.

## Controlador e transições

`LocomotionController` observa cada jogador que explicitamente habilitou o teste. Funciona no servidor integrado; o estado pertence à sessão/UUID, não a uma preferência persistente. Mede deslocamento horizontal real entre ticks e direção relativa ao yaw. Não escreve posição, velocidade, aceleração, atributos, colisão ou física.

| Critério | Decisão |
|---|---|
| Parado no chão, velocidade ≤0,0125 bloco/tick | candidato IDLE |
| Avançando, velocidade ≥0,025 bloco/tick, cosseno de direção ≥0,65 | candidato JOG |
| JOG já ativo, velocidade >0,0125 e direção ≥0,45 | mantém JOG |
| Novo IDLE/JOG | exige três amostras consecutivas |
| Direção lateral/traseira sem avanço suficiente | VANILLA_FALLBACK imediatamente |
| No ar, agachado, em água/bolha/lava, escalando, montado, elytra/voo Creative, sprint vanilla, morto, pose não STANDING, usando item, golpe ou dano | VANILLA_FALLBACK imediatamente |
| Descontinuidade horizontal >0,5 bloco/tick | fallback, sem tratar teleporte como passo |

Sprint vanilla não usa o Jog por engano: é um estado ainda não implementado. As tolerâncias de velocidade/direção reduzem oscilações; a classificação é testada separadamente por testes Java executáveis.

O modelo automático contém duas **poses customizadas exclusivas**: `toe_idle` e `toe_jog`, com `command=true`, `layerControlled=false`, `mustFinish=false`. O CPM usa um único slot de pose; ativar uma substitui a outra. Não há duas animações absolutas competindo sobre os mesmos ossos.

Ao alternar IDLE/JOG, envia-se `ICommonAPI.playAnimation(..., pose, 1)` apenas na mudança efetiva. Não se troca o modelo nem se reinicia a animação a cada tick. O modelo fica carregado durante a sequência Idle→Jog→Idle. O CPM mantém seu relógio enquanto a pose permanece ativa; reentrar no Jog inicia um novo ciclo, o que é intencional.

Para fallback, o modelo experimental é removido por `resetPlayerModel`, restaurando integralmente o modelo anterior/vanilla; não se tenta imitar salto/nado com keyframes ausentes. Ao voltar a um estado suportado, o modelo é reaplicado uma vez. Há espera inicial de 12 ticks e confirmação do registro de comandos CPM antes de ativar a pose. Se o registro não aparecer dentro do limite, o controlador se desativa e restaura o modelo, em vez de deixar uma pose congelada.

**Não foi encontrada API de crossfade temporal configurável nesse fluxo.** A implementação CPM inspecionada mantém a instância da animação ativa e remove a antiga ao trocar o trigger; não mistura duas poses com pesos. A alternativa implementada é corte exclusivo com histerese, preservando o loop e evitando comandos repetidos. Pode haver salto de pose no instante da transição; continuidade suave perfeita não é alegada. Entradas/saídas de fallback também incluem a latência normal do carregamento CPM.

Referências primárias: [API CPM](https://github.com/tom5454/CustomPlayerModels/wiki/API-documentation), classes oficiais `AnimationHandler`, `ModelPartAnimation`, `CustomPoseGestureButtonData` e `ServerAnimationS2C` no checkout usado no M001.1. Chamadas compiladas e executadas contra CPM 0.6.26a real. Não foram inventados métodos de blending.

## Ativação e limpeza

```text
/toe_animtest auto          # skin atual Classic
/toe_animtest auto_steve    # prévia Steve local
/toe_animtest off
```

Os comandos M001.1 continuam funcionando e desativam qualquer controlador automático antes de aplicar o Idle antigo. Off remove a sessão observadora e restaura o modelo; login, logout, respawn e parada do servidor também limpam sessões. Não há reativação automática ao reconectar ou reiniciar o cliente. O modelo não é forçado nem salvo persistentemente pela aplicação.

## Testes e evidência visual

O cliente real passou **22/22 verificações finais**, além dos testes estruturais e unitários. [Resultados estruturados](M001_2_RUNTIME_TESTS.json) e [TEST_LOG_M001_2.md](../../docs/TEST_LOG_M001_2.md).

- [Idle no chão](M001_2_visual/idle.png).
- [Jog fase A](M001_2_visual/jog-275.png) e [Jog fase B](M001_2_visual/jog-285.png).
- [Parada e retorno ao Idle](M001_2_visual/stop.png).
- [Fallback lateral](M001_2_visual/strafe-fallback.png), [no ar](M001_2_visual/air-507.png), [agachado](M001_2_visual/crouch-fallback.png).
- [Off](M001_2_visual/off.png) e [skin atual preservada](M001_2_visual/player-skin.png).

As imagens mostram o Steve cúbico e poses Jog distintas. As verificações de runtime conferem tanto o estado do controlador quanto a pose efetiva CPM, não apenas um texto de comando. Durante Jog estável, a contagem permaneceu em dois comandos (Idle inicial e entrada Jog), com uma única aplicação do modelo.

A distância em 40 ticks de avanço foi `8.374761235223515` blocos sem o controlador e `8.374761235223513` com ele. A diferença é numérica de ponto flutuante, não alteração de velocidade. Teste realizado em faixa livre e plana no save descartável `run/saves/New World`.

## Contato dos pés e limites visuais

O Jog é reproduzido em 933 ms fixos, sem resampling dinâmico por velocidade. Na marcha vanilla testada, velocidade estável ≈4,317 blocos/s e deslocamento por ciclo ≈4,028 blocos. O rig curto do Steve e o descarte do movimento vertical de pelvis não garantem plantar o pé nessa velocidade.

[Diagnóstico cinemático dos pés](M001_2_FOOT_DIAGNOSTIC.json): excursão longitudinal de aproximadamente 1,05 bloco por pé. Uma estimativa com só dois pares de amostras próximos do máximo Y do modelo aponta velocidade residual de contato próxima de 2,3 blocos/s. Essa estimativa usa o centro inferior do cubo e **não é medição visual confiável de contato no chão**; registra uma incompatibilidade provável, não uma prova de foot planting. É esperado deslizamento/contato imperfeito. Não foi alterada a física nem a duração solicitada para escondê-lo.

Capturas estáticas e logs não substituem uma revisão visual contínua de cadência e transições. O usuário deve avaliar a aceitabilidade dos cortes e do deslizamento ao jogar. IK, ajuste de stride/cadência, adaptação de pelvis e blending ficam pendentes de escopo futuro; não foram iniciados automaticamente.

## Outros limites e preservação

- Skins Classic 64×64 são o alvo. Slim/Alex, 64×32, armaduras, itens e primeira pessoa não foram homologados. Alguns itens de mundo aparecem nas capturas por coleta vanilla no save de teste.
- Água, lava, escalada, montaria, elytra e sprint têm guardas explícitas, mas não foram exercitados fisicamente nesse teste; a entrada genérica de estado não suportado foi coberta nos testes unitários.
- Servidor remoto, latência de rede e múltiplos jogadores não foram validados.
- A prévia Steve retorna à skin anterior em fallback, pois a escolha segura foi remover o modelo completo. No usuário Dev, a skin vanilla é outra personagem. O modo `auto` preserva a skin atual nos estados suportados e não a altera na conta.
- A fonte UAL1 declara CC0/Quaternius. CPM e assets Mojang conservam suas próprias licenças. Fontes e conversões continuam locais/ignoradas, sem publicação.
- Os 171 arquivos originais inventariados do Godot mantiveram SHA-256. Não houve Roll, ataques, combate, stamina, habilidades, classes ou mods novos. M001.3 não iniciado.
