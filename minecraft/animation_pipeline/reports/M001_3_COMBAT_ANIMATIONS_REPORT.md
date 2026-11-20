# M001.3 — Roll e espada: integração exclusivamente visual

Data: 2026-11-12, America/Sao_Paulo.

**Funcional nos testes locais realizados:** Steve executa Roll e Sword_Regular_A, uma vez por acionamento, e volta ao Idle/Jog. V/J, comandos, bloqueio em menus, arbitragem, off, fallback e limpeza entre sessões foram exercitados no Minecraft real. Não foi implementado combate funcional, dano, hitbox, stamina, invulnerabilidade ou deslocamento de esquiva.

## Inspeção e preservação

Relatórios M001.1/M001.2, controlador, projetos e binários existentes foram inspecionados antes das alterações. A fonte do CPM e o JAR 0.6.26a foram usados para verificar gestos não repetidos, prioridade e reset por comando. A documentação Forge 1.20.1 foi consultada para registrar KeyMapping no cliente.

Os **nove projetos/modelos aprovados** em `converted/m001_1/` e `converted/m001_2/` continuam byte a byte idênticos aos hashes registrados em [M001_3_BASELINE.json](M001_3_BASELINE.json). Não foram sobrescritos pelos modelos novos. Os 171 arquivos originais inventariados do Godot foram relidos e mantêm SHA-256.

O pipeline existente recebeu somente as opções `--roll` e `--sword`; as transformações anteriores e seus validadores foram preservados. O novo `retarget-actions.cjs` lê o Idle/Jog já convertido, acrescenta as ações e grava somente `converted/m001_3/`.

## Fontes e duração

| Ação | GLB de trabalho | Clipe real | Tempos fonte / quadros CPM | Duração CPM |
|---|---|---|---:|---:|
| Roll | `input/UAL1/UAL1_Standard.glb` | `Roll` | 45 / 45 | 1467 ms |
| Espada | `input/UAL2/UAL2_Standard.glb` | `Sword_Regular_A` | 14 / 14 | 433 ms |

- UAL1 SHA-256: `69591853d817488edaa8fd9bf8fc1d821eaeaf789f8627b3cd23b41c4ed67997`.
- UAL2 SHA-256: `8cee20ab1bc55130092447e810e26df22dd2803eccc54f52137a7d54d7ab88a8`.
- Durações GLB: 1,466666698 s e 0,433333337 s, respectivamente. Tempos de origem próximos de 30 Hz. As durações não foram aceleradas.
- Ambos têm 195 canais dos 65 joints: posição, quaternion e escala. Amostragem inclui o endpoint, ao contrário dos loops Idle/Jog. Interpolação CPM `linear_single`, `loop=false`.
- O source de `root.translation` é zero nos dois clipes Standard. Translações locais de pelvis variam e foram inspecionadas, mas não são copiadas para a entidade Minecraft.
- Nenhum clipe de recuperação foi inventado. `Sword_Regular_A_Rec` não foi convertido: ao final de A, há corte para a locomoção, não uma recuperação nova.

Detalhes: [Roll fonte](M001_3_ROLL_SOURCE.json), [espada fonte](M001_3_SWORD_SOURCE.json) e [validação estrutural](M001_3_VALIDATION.json).

## Mesmo rig Classic e mapeamento

Cabeça 8×8×8, tronco 8×12×4 e membros 4×12×4 com cotovelos/joelhos de seis pixels, UVs Classic 64×64 e as mesmas 16 peças visíveis/camadas. Não foi importada a malha humana UAL. O config UAL1 e UAL2 é comparado para confirmar que os dois retargets usam o mesmo Steve.

| Origem UAL1/UAL2 | Destino |
|---|---|
| `Head` | cabeça, ID 0 |
| `spine_03` e ancestrais | torso, ID 1 |
| `upperarm_l/r` | braços, IDs 2/3 |
| `thigh_l/r` | coxas, IDs 4/5 |
| `lowerarm_l/r` | cotovelos, IDs 102/103 |
| `calf_l/r` | joelhos, IDs 104/105 |
| `hand_l/r` | orientação dos pontos de item/pulso, IDs 302/303 |

Bind pose e hierarquia são calculados separadamente para cada GLB. Quaternions globais, mudança de base e Euler ZYX seguem o pipeline anterior. A flexão dos cotovelos/joelhos é preservada; torção lateral/axial desses segmentos é descartada para o rig rígido. Dedos e uma mão geométrica separada não foram adicionados.

O único acréscimo ao rig novo são **dois pontos invisíveis de tamanho zero**, sob os antebraços, com `itemRenderer=left_hand/right_hand`. As peças visíveis e UVs aprovadas não mudam. Esses pontos usam o mecanismo real de item do CPM e orientações derivadas de `hand_l/r` para posicionar a espada vanilla, sem criar uma arma personalizada.

O ponto do item compensa a translação de empunhadura já aplicada pelo renderizador vanilla. A orientação do pulso é relativa ao antebraço efetivamente retargetado; o centro da empunhadura permanece junto à extremidade do cubo da mão. Idle/Jog têm pontos neutros para evitar transformações residuais depois da ação. A mão visível continua parte rígida do antebraço; não há dedos articulados.

## Roll e contato visual com o chão

O giro é derivado dos keyframes originais. O arco desenrolado da cabeça é aproximadamente 351°, com diferenças de pose entre começo e fim da fonte; não foi substituído por um giro genérico.

Para o Roll, os cantos dos cubos retargetados são usados para aproximar a menor altura visível em cada quadro e alinhar essa geometria ao plano local das solas. O ajuste move somente as posições **de renderização** das seis raízes: pode elevar ou baixar a figura para reduzir afundamento/flutuação. Não escreve posição, velocidade, escala ou bounding box da entidade, não acompanha o relevo e não é IK.

Isso permite o rolamento visual parado, ou enquanto o jogador se desloca pela física vanilla. O corpo físico continua no tamanho/pose normal do Minecraft; não é possível passar por espaços menores nem evitar golpes por causa da animação.

## Reprodução única e arbitragem

O modelo novo contém:

| Nome CPM | Tipo | Prioridade | Loop |
|---|---|---:|---|
| `toe_idle` | pose customizada exclusiva | 0 | sim, 2500 ms |
| `toe_jog` | pose customizada exclusiva | 0 | sim, 933 ms |
| `toe_roll` | gesto | 10 | não, 1467 ms |
| `toe_sword` | gesto | 10 | não, 433 ms |

Todos usam comandos do CPM, sem reservar bits das camadas de skin (`layerControlled=false`). As ações têm `mustFinish=false` para permitir o cancelamento seguro por off/fallback. O gestor CPM aplica a prioridade superior do gesto; a locomoção continua atualizando seu estado/relógio por baixo dele, mas a ação fornece os valores finais das peças animadas. Assim o fim da ação revela Idle ou Jog conforme a movimentação atual.

`VisualActionWindow` guarda no máximo uma solicitação pendente ou uma ação ativa. **Primeiro acionamento vence, sem fila.** Repetições e ações concorrentes são recusadas enquanto ocupado; Roll não interrompe Sword e Sword não interrompe Roll. Se V e J chegarem no mesmo tick, sem ação ativa, o consumidor escolhe V. Não há cooldown de combate, custo ou stamina: apenas exclusão visual e uma margem de transporte.

O gesto não é reenviado a cada tick. O CPM encerra sua reprodução não repetida pela duração em milissegundos. O servidor mantém uma janela limitada de `ceil(duração/50)+2` ticks antes de limpar o parâmetro por `ICommonAPI.playAnimation(..., 0)` e aceitar nova ação. Isso dá 32 ticks para Roll e 11 para espada no servidor a 20 TPS; **a animação visual continua com 1467/433 ms**, não com a duração dessa margem. Não foi inventado um callback público de término.

Pedidos aguardam o modelo e seu registro de ações CPM; uma solicitação que não ficar pronta expira, sem fila infinita. Em estados incompatíveis, o controlador cancela a ação, limpa o pedido e restaura o modelo vanilla, reaproveitando os guardas M001.2. Off remove a sessão antes de resetar o gesto/modelo. Login/logout/respawn/parada do servidor não mantêm ações entre sessões.

Estados efetivos: IDLE, JOG, ROLL, SWORD_ATTACK e VANILLA_FALLBACK. A máquina de locomoção continua calculando só Idle/Jog/fallback; a janela da ação tem prioridade no status efetivo, sem reescrever a seleção validada.

APIs reais: `ICommonAPI.setPlayerModel`, `playAnimation`, `getAnimationPlaying` e `resetPlayerModel`, compiladas e executadas contra o JAR instalado. Não foi implementado blending temporal. Permanecem os cortes de pose documentados no M001.2.

## Teclas e comandos

V = Roll; J = Sword Attack. Categoria **Tower of Entropy**, com textos en_us/pt_br. Registro via `RegisterKeyMappingsEvent` no mod bus do cliente, `KeyConflictContext.IN_GAME` e consumo de `KeyMapping.consumeClick` no ClientTick END. Há proteção contra repetição por tecla mantida e exigência de liberação depois dos menus; eventos enfileirados em chat/inventário/controles são descartados.

Um pacote próprio C2S transporta apenas a ação solicitada para o próprio jogador. Não envia ataque vanilla, não chama swing/attack/hurt e não substitui Space ou o botão de ataque.

```text
/toe_animtest auto_steve
/toe_animtest roll
/toe_animtest sword
/toe_animtest off
```

`idle`, `idle_steve`, `auto`, `auto_steve` e `off` continuam disponíveis. Auto usa o modelo novo se preparado; caso contrário, continua podendo usar os arquivos M001.2. Roll/Sword precisam dos modelos M001.3. Se acionados sem sessão automática, habilitam a sessão com a skin atual; para a prévia Steve, habilite `auto_steve` primeiro. O modo normal não persiste a skin ou o modelo entre sessões.

## Arquivos gerados

Em `animation_pipeline/converted/m001_3/`, ignorado pelo Git:

| Arquivo | Tamanho desta exportação |
|---|---:|
| `steve-ual1-roll.cpmproject` | 23.691 bytes |
| `steve-ual2-sword.cpmproject` | 8.566 bytes |
| `steve-ual-actions.cpmproject` | 66.324 bytes |
| `steve-ual-actions.cpmmodel` | 21.747 bytes |
| `steve-ual-actions-preview.cpmproject` | 53.809 bytes nesta execução |
| `steve-ual-actions-preview.cpmmodel` | 24.050 bytes |
| `roll-data.json`, `sword-data.json`, `actions-data.json` | dados intermediários locais |

Os binários cabem no limite local de 30 KiB e não usam upload/overflow remoto. Tamanho de ZIP do projeto não equivale ao limite do binário de jogo.

SHA-256 do modelo principal: `9dab98175759a63cb83b5104afd13029a3e49b25e946c0e16c138265d26ce62c`.
SHA-256 da prévia Steve: `5153045d5634fc73c501167658cf1af4989546c5125a52ed47ee67dfac8ce701`.

O editor CPM instalado carregou os projetos, conferiu quatro clipes e exportou por `Exporter.exportModel`; `ModelFile.load` releu os arquivos. Cópias de uso ficam em `run/player_models/` com prefixo `toe-`. Nenhum modelo, GLB ou textura vanilla é embutido no JAR Tower of Entropy.

## Testes e evidências

Ver [TEST_LOG_M001_3.md](../../docs/TEST_LOG_M001_3.md), [checks de ações](M001_3_RUNTIME_TESTS.json), [regressão M001.2](M001_3_LOCOMOTION_REGRESSION.json), [reinício](M001_3_RESTART_TEST.json) e [validador estrutural](M001_3_VALIDATION.json).

- Ações: 37/37 checks no cliente, incluindo teclas remapeadas, menus, concorrência, retorno a Idle/Jog, arma equipada/vazia, off durante ação, salto e reconexão durante Roll.
- Regressão original M001.2: 22/22 checks no processo reiniciado, incluindo os comandos Idle antigos.
- Reinício: 1/1 check, sem sessão ou parâmetros de ação presos.
- Validadores Node e testes Java de locomoção/arbitragem passam. Não são apresentados como testes JUnit do Gradle.
- Estacionário: deslocamento real zero em Roll e Sword.
- Avanço de 40 ticks: referência, Roll e espada mediram `8.374761235223474` blocos na suíte aprovada. Na regressão isolada, vanilla e auto também coincidiram exatamente.

Capturas reais do cliente:

- [Roll fase inicial](M001_3_visual/roll-236.png), [fase intermediária](M001_3_visual/roll-246.png), [retorno](M001_3_visual/roll-256.png).
- [Espada de ferro fase A](M001_3_visual/sword-354.png), [fase B](M001_3_visual/sword-358.png), [ataque sem espada](M001_3_visual/sword-empty.png).
- [Jog→Roll](M001_3_visual/moving-roll.png), [Jog→Sword](M001_3_visual/moving-sword.png).
- [Controles V/J](M001_3_visual/controls.png), [off](M001_3_visual/off.png), [fallback por salto](M001_3_visual/jump-fallback.png).

As vistas diurnas confirmam o Steve cúbico, fases distintas do rolamento e a espada de ferro junto à mão nas fases observadas. Isso não prova empunhadura perfeita durante todos os quadros ou ângulos. O ajuste do pulso e a suavidade do corte de retorno precisam de revisão visual contínua humana.

## Limitações e pendências

- Sem blending temporal: ação→locomoção pode ter corte perceptível. Sword A não inclui o clipe de recuperação da biblioteca.
- Mãos geométricas rígidas, sem dedos; a orientação do pulso move o ponto da espada. Mão esquerda dominante, outras armas/itens, armaduras e primeira pessoa não foram homologadas.
- Classic 64×64 é o alvo; Slim/Alex e 64×32 continuam fora da validação.
- Ajuste de altura do Roll pressupõe plano local das solas, não terreno real. Escadas, rampas, espaços baixos e contatos finos precisam de avaliação; o collider físico não encolhe.
- O deslizamento dos pés e os limites do Jog permanecem como no M001.2.
- O término usa janela limitada de ticks mais gesto nativo; latência alta, TPS/FPS extremos e multiplayer remoto não foram testados.
- Teclas foram exercitadas pelo KeyMapping real com injeção de cliques e pacote C2S real; o teste de teclado físico e remapeamento pelo usuário ainda é recomendado.
- As fontes UAL1/UAL2 declaram CC0/Quaternius. CPM e assets Mojang têm licenças próprias. Fontes/conversões permanecem locais/ignoradas, sem publicação.

Não há nova arma, inimigo, dano, hitbox, stamina, invulnerabilidade ou física de Roll. O ataque normal do Minecraft continua sendo vanilla. M002 não iniciado.
