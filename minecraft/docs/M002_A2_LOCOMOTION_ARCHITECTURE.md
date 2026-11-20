# M002.A2 — Locomoção horizontal no Polymesh R3

13/11/2026. O renderer padrão continua sendo Polymesh R3, com a geometria aprovada. Idle, Walk, Jog e Sprint são selecionados automaticamente. Marcha para trás e strafe são estados reconhecidos, mas usam fallback por falta de clipes adequados. Não foram criadas mecânicas ou velocidades próprias.

## Responsabilidades

`HorizontalMotion` é um seletor visual puro. `HorizontalLocomotion` observa o deslocamento efetivo do jogador a cada tick cliente, a orientação e `isSprinting()` vanilla. Não consulta códigos físicos de W/CTRL e não escreve posição, velocidade, inputs ou atributos. Portanto remapeamentos, sprint vanilla, colisões e movimento do servidor continuam responsáveis pelo deslocamento real.

A máquina CPM/servidor anterior não foi substituída: continua responsável por registro das ações e fallbacks técnicos. Os sete estados novos pertencem ao controlador visual local Polymesh, sem novos pacotes ou autoridade cliente sobre combate. Renderização remota continua fora da homologação atual.

| Estado visual | Seleção | Saída |
|---|---|---|
| IDLE | Sem deslocamento real | Idle aprovado |
| WALK | Velocidade baixa | UAL1 Walk |
| JOG | Velocidade normal sem sprint | Jog aprovado |
| SPRINT | Sprint vanilla e deslocamento suficiente | UAL1 Sprint |
| MOVE_BACKWARD | Componente reversa real relativa ao yaw | Fallback anterior |
| STRAFE_LEFT/RIGHT | Componente lateral dominante real | Fallback anterior |

Velocidade é medida em blocos/tick, suavizada com fator 0,55. Walk entra abaixo de 0,115 e só sai acima de 0,155; Sprint exige o estado vanilla e velocidade suavizada >0,14. Idle usa limiares 0,018/0,009, além da parada real abaixo de 0,003. Deltas não finitos ou acima de um bloco/tick reinicializam o seletor visual, sem correção física da entidade.

Direção usa os produtos escalares do deslocamento com os vetores frontal/lateral do yaw. Marcha reversa entra com cosseno <−0,48 e mantém até −0,30; lateral entra com magnitude >0,87 e mantém até 0,77. Isso permite diagonais frontais e evita troca excessiva junto às fronteiras. Segurar W contra uma parede não força Jog se o deslocamento efetivo cessar.

Walk não tem tecla nova: aparece por velocidade real reduzida, por exemplo sobre soul sand. Agachar continua usando fallback de agachamento; não foi usado como atalho de Walk. Itens, menus, primeira pessoa, armaduras, capa, offhand, escalada, natação, pulo e queda mantêm as regras de fallback existentes.

## Relógio e blending

O compositor local TRS de A1.2 continua em uso. `transitions.locomotionSeconds` no `towerofentropy-visual-client.toml` controla as misturas entre ciclos, padrão **0,14 s**, intervalo permitido 0,06–0,25 s. Retornos de ataques e Roll para ciclos usam o parâmetro anterior de retorno, 0,14 s. Entrada pesada 0,12 s e entrada Roll 0,06 s permanecem.

Uma fase normalizada compartilhada avança durante a movimentação e as ações. Trocar Walk/Jog/Sprint conserva essa fase em vez de reiniciar no frame zero. Walk recebe offset de 0,19 ciclo e Sprint de 0,99, medidos pelos sinais de altura dos pés na fonte. A interpolação da pose anterior trata as diferenças residuais.

A cadência visual acompanha a razão entre velocidade observada e referência por ciclo: Walk 0,105, Jog 0,216 e Sprint 0,281 bloco/tick. A razão é limitada a 0,45–1,5 e suavizada com constante de 100 ms. Essas referências só afetam o relógio do sampler; não são velocidades impostas à entidade. Não há garantia de foot locking: comparar passos/distância nas evidências continua necessário. Root motion fonte não é somado ao movimento vanilla.

Idle → Walk, Walk → Jog, Jog → Sprint, Sprint → Jog e Jog → Idle compartilham essa mistura. Mudanças para trás/lados entregam o corpo ao fallback anterior; não existe crossfade de esqueleto entre dois renderizadores diferentes. A suavidade estética dessas fronteiras permanece pendente até existir um clipe direcional adequado. Não se anunciou suporte completo apenas porque os estados têm nomes.

## Combate e estados especiais

Prioridade continua: **Roll autorizado > ataques leves/pesado > locomoção**. A detecção horizontal não envia cancelamentos. Relógio do ataque, impactos, combos, dano e i-frames permanecem nos controladores anteriores. Ao encerrar uma ação, a seleção lê velocidade/direção atuais; não força Jog por uma tecla isolada. A espada usa o mesmo antebraço composto e anchor de A1.2.

Durante Roll, o mesmo deslocamento físico continua sendo observado; ele não é reaplicado pelo visual. Ao terminar, há mistura para o ciclo selecionado. Situações incompatíveis com o renderer mantêm a entrega para vanilla/CPM. Não foram implementadas animações novas de pulo, queda, natação ou agachamento, nem paridade dos novos ciclos no CPM.

## Assets e diagnóstico

O runtime usa `steve_a2.glb`, uma cópia ampliada do GLB R3 com dois clipes adicionais, e `a2-poses.json`. O arquivo aprovado `steve_r3.glb` é preservado. O parâmetro QA `-PtoeA2Previous` seleciona o caminho visual A1.2 para comparação; não é necessário no gameplay normal. Comandos existentes de renderer/combate continuam disponíveis.

Veja [assets](M002_A2_ANIMATION_ASSETS.md), [QA visual](M002_A2_VISUAL_QA.md) e [testes](TEST_LOG_M002_A2.md). Nenhuma etapa M002.A3 foi iniciada.
