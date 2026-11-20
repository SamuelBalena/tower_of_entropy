# Socos frontais, salto e parkour no Polymesh R3

19 de novembro de 2026

## Mecânicas

- A mineração contínua com mão vazia acompanha a quebra vanilla e alterna os clipes no ritmo do swing, incluindo Pressa e Fadiga de Mineração. A fase é calculada por tick e a sequência termina com a quebra, a troca de item ou a liberação do ataque. Ataques a entidades e no vazio seguem o observador de swings existente. Regras de quebra, alcance e dano não foram alteradas.
- O salto normal a pé usa impulso vertical base de 0,618 bloco/tick em terreno comum. A integração usa o evento de salto do Forge no cliente e no servidor e conserva o fator do bloco e o bônus de Salto vanilla. Gravidade, atrito, colisões e controle horizontal permanecem a cargo do jogo. Água, escadas, voo e montarias ficam fora do ajuste.
- Wall Jump usa a mesma tecla do salto. Contra a parede, o servidor aplica impulso vertical. Para longe, acrescenta 0,36 bloco/tick ao longo da normal de saída. A direção é calculada a partir dos eixos de movimento e da orientação do jogador, com limiar de produto escalar 0,40. Entrada neutra ou tangencial usa o salto vertical. Um salto consome oito ticks de vigor e só pode ser repetido após tocar o chão.
- Ledge Grab procura o topo de um bloco de colisão integral ao alcance das mãos. O servidor verifica a caixa do jogador em pé ao longo do percurso vertical e depois horizontal. A captura limita a acomodação inicial a 0,22 bloco na vertical. Pulo inicia a subida em 18 ticks, com checagens de colisão durante toda a trajetória. Agachar cancela, assim como perder apoio, esgotar vigor durante a suspensão ou bloquear o trajeto. A aderência e a suspensão usam a mesma reserva.
- O cliente envia apenas os eixos de intenção e recebe a fase visual autorizada. A correção de posição para suspensão e subida usa atualizações incrementais do servidor. O canal de ações agora usa a versão 10, então cliente e servidor precisam da mesma versão do mod.

## Auditoria e clipes

| Biblioteca | Candidatos examinados | Decisão |
| --- | --- | --- |
| UAL1 | `Jump_Start`, `Jump_Loop`, `Jump_Land`, `Punch_Jab`, `Punch_Cross` | Ciclo de salto normal reaproveitado. Os dois socos anteriores permanecem empacotados para comparação. |
| UAL2 | `ClimbUp_1m` (0,667 s), variantes `NinjaJump_*` | A variante RM da subida avança na raiz e a sequência não oferece suspensão separada com apoio de duas mãos. |
| Free100Animations | `MM_WallJump` (0,467 s), `AS_LeaningOnWall` (10,400 s), `MM_Fall_Loop` (3,000 s) e variantes de queda | As curvas do salto mostram excursões assimétricas das mãos. A pose encostada na parede tem pouca excursão das mãos e da pelve. A licença desse material continua pendente e nenhum desses assets foi incorporado. |

A busca local incluiu Wall, Ledge, Hang, Climb, Mantle, Pull e Vault. A auditoria anterior contém as [curvas amostradas no Unreal](../animation_pipeline/reports/expansion/FREE100_METADADOS.txt).

Os novos clipes foram autorados para o Steve no pipeline Node → Blender → GLB. Os novos socos limitam a rotação do tronco a 12° de guinada e alternam braços, com extensão frontal do conjunto ombro–cotovelo. O teste geométrico do keyframe de impacto estima mais de nove unidades à frente e menos de uma unidade lateral para o braço na convenção de modelagem (16 unidades por bloco), medindo a partir do ombro e antes da contribuição da rotação do tronco.

| Clipe | Duração | Uso |
| --- | ---: | --- |
| `UAL_punch_jab_front`, `UAL_punch_cross_front` | 0,833 s cada | Socos frontais |
| `UAL_wall_slide` | 0,800 s | Apoio das mãos e pernas flexionadas |
| `UAL_wall_jump` | 0,633 s | Preparação e extensão no impulso |
| `UAL_ledge_hang` | 0,600 s | Suspensão com as duas mãos elevadas |
| `UAL_ledge_climb` | 0,900 s | Elevação, apoio e final em pé |

As duas variantes do GLB retêm 12 juntas e 32 clipes originais e acrescentam seis clipes, totalizando 38. O empacotador compara a geometria, hierarquia, skinning, UVs, clipes e buffers originais antes de aceitar o derivado. O projeto Blender de revisão fica em `animation_pipeline/converted/parkour/steve-a3-motion-review.blend`.

## Arquivos

- Movimento e colisões em `WallSlide.java`, `ParkourJump.java`, `ParkourPose.java` e `Ledge.java`.
- Entrada e sincronização em `ActionNetwork.java` e `ClientWallVisual.java`.
- Seleção do Polymesh R3 em `ClientSwingPose.java`, `VisualContext.java` e `PlayerRendererR3.java`.
- Bake e pacote em `prepare-parkour.cjs`, `blender-a3.py`, `package-expansion.cjs` e `build.gradle`.
- Cobertura e isolamento em `ExpansionRuntimeTest.java`, `build.gradle` e `.gitignore`.

## Testes e medições

| Cenário | Resultado no Minecraft Forge, Classic |
| --- | --- |
| Mineração contínua | Oito amostras a cada seis ticks alternaram Jab e Cross em obsidiana. O soco parou após soltar o ataque. |
| Salto normal | 2,5019 blocos entre a posição de partida e o ápice. |
| Wall Jump contra a parede | 2,4369 blocos medidos desde o tick da solicitação. |
| Wall Jump para longe | 2,4369 blocos na vertical e 3,6487 blocos de afastamento durante a janela observada. |
| Ledge Grab | Captura após salto normal e após Wall Jump, subida e aterrissagem sobre o bloco. O cenário com teto obstruindo a subida não capturou a borda. |
| Vigor | 35 ticks de aderência, soltura por esgotamento, recuperação no chão e bloqueio de uma segunda solicitação de Wall Jump sem aterrissar. |
| Locomoção | Caminhada, Sprint 8/8 e inversão W → S sem perder Sprint. |

A integração Classic passou em 16/16, com zero erros de renderização. Os testes existentes de movimento horizontal (43), combate (18), Roll (104), máquina de locomoção e janelas de ação também passaram. O log Classic está em `.logs/parkour-runtime.log`. As suítes usaram cópias isoladas do mundo em `run-parkour-qa`. O perfil Slim grava em `animation_pipeline/converted/parkour-qa/runtime`.

A regressão Slim passou em 13/13, com zero erros de renderização. Registrei o uso de espada, Roll, soltura de flecha, ciclo de salto e reconexão automática do R3. O build Forge final passou. Os 17 recursos anteriores no JAR conferem byte a byte com o pacote de referência. As capturas históricas permaneceram intactas. Nenhum commit foi criado.

SHA-256 do JAR final B4B6FF6B03D38B20277E3E0FF4047124EB0402879837DCA146D5EA47441B7E61.

## Limitações e revisão visual

A subida exige topo de bloco completo, espaço para ficar de pé e um percurso vertical/horizontal livre. Cercas, lajes, escadas de bloco e tetos baixos não servem como bordas. A altura nominal do salto não garante subir superfícies dessa altura. A fase visual continua no renderizado R3 de terceira pessoa do jogador local, nas mesmas condições Classic/Slim da expansão. Servidor dedicado e latência de rede não fazem parte do teste integrado.

Em F5, segure o ataque com a mão vazia mirando um bloco resistente e alterne para o vazio e para um alvo. Confira o retorno do braço e a extensão frontal. Em seguida, compare os saltos contra a parede e para longe dela, usando a diagonal quando houver espaço. Confira contato das duas mãos, deformação dos cotovelos, transição visual da subida e posição final dos pés. A aprovação estética segue para revisão no Minecraft pelo usuário.
