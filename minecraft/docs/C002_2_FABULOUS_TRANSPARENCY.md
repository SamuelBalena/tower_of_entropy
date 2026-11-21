# C002.2 — Transparência contextual em Fabulous

## Causa

A C002.1 media a retração da câmera em todos os modos, mas o despachante pulava a captura isolada quando `Minecraft.useShaderTransparency()` era verdadeiro. Simplesmente liberar a captura nesse caso permitiria que os estados de saída dos materiais enviassem partes do equipamento para o alvo de itens translúcidos do cenário. O modo Fabulous também executa seu pós-processamento após o evento de partículas, com cor e profundidade separadas para blocos translúcidos, itens, partículas, clima e nuvens.

O primeiro ensaio de integração encontrou outro limite verificável. O `EffectInstance` passou a usar 13 samplers de fragmento, mas o cache de vínculos do Blaze3D em 1.20.1 tinha 12 posições. A exceção ocorria ao limpar as unidades do shader. O cache foi ampliado para 16 posições, o mínimo de unidades de fragmento de OpenGL 3.2. As políticas de vínculo, limpeza e restauração das unidades seguem o código existente.

## Alterações

O GltfModelRenderer do Polymesh escolhe o RenderType com base no modo alpha do material e envia a malha deformada pelo CpuSkinningPipeline ao MultiBufferSource. O corpo R3 e seus acessórios continuam usando esses materiais e buffers de vértices. Somente o jogador local, no passe do mundo e com opacidade abaixo de 100%, usa o alvo privado reutilizável. Durante a captura e o flush, os estados de saída do Fabulous mantêm os draws no alvo privado. O bloco `finally` restaura a rota normal e o framebuffer principal. Fast e Fancy seguem com a composição direta da C002.1.

No Fabulous, o passe principal da cadeia vanilla recebe a textura de cor premultiplicada e a profundidade do jogador como uma camada adicional. A cor e a cobertura recebem o mesmo multiplicador de opacidade. O passe ordena a camada do jogador junto das camadas vanilla, descarta seus fragmentos atrás da cena sólida e mantém o blit final do Minecraft. O passe lê as texturas quando o fade está ativo. Em espaço livre a contribuição é zero e o desenho do jogador continua no alvo usual. A limpeza da cadeia descarta a referência ao uniform e o logout libera as texturas privadas.

Espadas, arco, item translúcido e o caminho de armadura existente usam a captura do próprio jogador. O capacete continua na rota pré-existente de armaduras vanilla/CPM e recebe a mesma composição visual. Skins com recortes alpha passam pelos materiais já usados pelo R3. Um jogador remoto conserva seu render normal, inclusive no Fabulous. Colisão volumétrica, ombro, configuração de alpha e zoom de COMBAT continuam nos controladores existentes.

Arquivos de runtime alterados em `client/ContextualPlayerLayer.java`, `coremods/player_fade.js` e `assets/towerofentropy/shaders/program/fabulous_player.{json,fsh}`. A fixture gráfica foi ampliada em `validation/w002/java/com/towerofentropy/client/camera/C0021RuntimeTest.java`, com seleção explícita de perfil em `build.gradle`.

## Validação no Minecraft

Os perfis confirmam explicitamente o modo gráfico e, no Fabulous, `useShaderTransparency=true`. A fixture verifica espaço livre, corredor, 12% no túnel, minério sob a mira, retorno a 100%, zoom manual, dois ombros, skin com recortes alpha, equipamento, COMBAT e regressão aquática. O ataque e o Roll foram emitidos durante a cena de COMBAT. O jogador remoto simulado foi fotografado separadamente e permanece opaco.

| Modo | Classic | Slim |
| --- | ---: | ---: |
| Fast | 29/29 | 29/29 |
| Fancy | 29/29 | 29/29 |
| Fabulous | 29/29 | 29/29 |

Nas capturas de 854 × 480, a contagem simples de pixels ciano na região central de 121 × 125 foi zero com o corpo opaco nos seis perfis. Com fade, houve 2.538 e 2.459 pixels em Fast, 2.684 e 2.639 em Fancy, 2.728 e 2.746 em Fabulous, respectivamente Classic e Slim. A captura Fabulous Classic usada nessa contagem vem da primeira rodada funcional. A referência C002.1 em Fabulous ficou em zero na mesma região, inclusive na cena com alpha calculado de 12%.

Os logs e as capturas estão nos perfis isolados `.logs/c0022-*`. Na primeira rodada funcional Fabulous Classic, o primeiro percurso da piscina começou antes de estabilizar e a asserção de razão diagonal falhou. A fixture passou a deixar o jogador na piscina por 28 ticks antes de medir o primeiro percurso. O limite da asserção foi preservado. No reteste a razão foi 1,021 e as 29 checagens passaram. O log e as capturas finais estão em `.logs/c0022-fab-classic-replay`.

As suítes locais passaram em movimento horizontal (43), locomoção, arbitragem de ações, combate (60) e Roll (104).

A regressão C002 passou em 26/26. W002 Slim passou em 59/59. A primeira rodada W002 Classic passou em 58/59, com uma falha isolada no limite lateral do ombro esquerdo. O reteste em perfil novo passou em 59/59 com a mesma asserção. Log em `.logs/c0022-combat-classic-replay.log`.

A regressão Slim de gameplay passou em 50/50 na W001, 36/36 na W001.1/C001.3 e 29/29 em expansão e parkour. Logs em `.logs/c0022-camera.log`, `.logs/c0022-combat-slim.log`, `.logs/c0022-combat-classic.log` e `.logs/c0022-regression.log`.

## Desempenho

A referência foi executada a partir de uma cópia de código e recursos anterior à C002.2. As duas rodadas Classic usaram a mesma fixture, Fabulous ativo, Forge com servidor integrado, 854 × 480 e VSync a 60 FPS. A amostra comparada da C002.2 é a primeira rodada já funcional. O aquecimento extra da piscina acrescentado depois ocorre após as cenas dessa tabela.

| Cena | Quadro médio C002.1 | Quadro médio C002.2 | CPU da captura isolada C002.2 |
| --- | ---: | ---: | ---: |
| Espaço aberto | 16,993 ms | 16,654 ms | — |
| Corredor | 16,687 ms | 16,700 ms | 0,717 ms |
| Túnel com minério | 16,798 ms | 16,810 ms | 0,689 ms |
| COMBAT | 16,747 ms | 16,770 ms | 0,736 ms |
| Deslocamento rápido | 16,752 ms | 16,831 ms | 0,649 ms |

A referência marcou zero composições em todas as cenas Fabulous, inclusive nas sete verificações que esperavam fade. A C002.2 fez zero capturas isoladas em espaço aberto e uma composição por desenho R3 nas cenas de fade estável. A CPU da captura inclui o desenho e o flush do jogador. O shader adicional usa o passe de tela cheia já existente do Fabulous. As médias de quadro ficaram próximas do limite de VSync. Esses ensaios não medem tempo isolado de GPU nem capacidade acima de 60 FPS. Logs em `.logs/c0022-baseline-fab.log` e `.logs/c0022-fab-classic-final.log`.

## Limites e artefato

Os ensaios usam Forge 1.20.1, Polymesh R3 e servidor integrado. A ordenação herda a granularidade dos alvos de Fabulous, cada um com uma profundidade por pixel. Shaders de terceiros, substituições da cadeia por resource pack, outras GPUs e sessões com dois clientes reais ficam em aberto.

Build Forge 47.4.28 e Gradle offline com JDK 17. A auditoria do JAR conferiu 16/16 arquivos de modelos e poses e zero classes de QA. O programa Fabulous e o coremod estão no pacote.

JAR em `build/libs/towerofentropy-0.0.1.jar`. SHA-256 `045617B062B5B93C37CF99457ECFD9893BE8DA8C1BDB986260C76C55ED77BFD9`.
