# S001.6.1 — Lava e materiais fundidos

## Auditoria

No Forge 1.20.1, `ItemBlockRenderTypes.getRenderLayer(FluidState)` retorna `solid` como padrão. O mapa registra água e água corrente como translúcidas. No Embeddium, `DefaultMaterials.forFluidState` consulta essa seleção. No Oculus 1.8.0, `IrisTerrainPass.GBUFFER_SOLID` seleciona `gbuffers_terrain`. A água usa `gbuffers_water`, onde o ID 11001 ativa a óptica da S001.6. Antes da mudança, a lava passava pela amostragem de atlas, iluminação de terreno e neblina. A malha, as alturas, os sprites animados e o lightmap eram fornecidos pelo jogo.

O hook de meshing do Oculus passa o estado devolvido por `FluidState.createLegacyBlock()` ao escritor de materiais. O mapa novo usa 11002 para o estado de bloco `minecraft:lava:level=0` e 11003 para `level=1…15`. O vertex shader recebe a classificação via `mc_Entity`. O fragment shader usa a posição em coordenadas do mundo e as derivadas da malha para distinguir faces verticais e superfícies, inclusive quando a normal do vértice do fluido aponta para cima. O atlas vanilla participa como modulação sutil. A lava permanece opaca, escreve no alvo já usado pelo terreno e aparece na cópia opaca que a água usa na S001.6. A integração vale no Overworld e no Nether.

## Material

Dois campos de ruído coerente deformam lentamente as coordenadas antes da amostragem térmica. A projeção vertical usa a altura do mundo e desloca o padrão para baixo. A projeção horizontal usa XZ e simula circulação local. Uma faixa estreita do campo térmico abre fissuras laranja entre regiões de crosta escura. O núcleo cresce de vermelho para laranja e amarelo, com destaques claros restritos aos picos. As fissuras perdem contraste com a distância para reduzir cintilação.

A emissão visual é independente do sol e passa pela neblina, exposição e gradação existentes. Os arredores continuam recebendo o block light vanilla com a tonalização quente da S001.4. A borda combina a silhueta da malha vanilla com as porções mais frias do campo térmico. A transição de temperatura usa o próprio material, sem amostragem da geometria vizinha.

| Preset | Amostras de ruído no caminho da lava | Comportamento |
| --- | ---: | --- |
| Leve | 3 | Circulação, crosta, fissuras e emissão |
| Equilibrado | 4 | Grão intermediário e contorno de fissuras mais irregular |
| Alto | 5 | Microdetalhe nas regiões próximas e transição térmica mais variada |

O menu **Lava** oferece `LAVA_DETAIL`, independente de `WATER_DETAIL`. Clientes que já guardavam ajustes de um preset recebem detalhe Equilibrado até selecionar um preset novamente ou alterar a opção Lava. A solução usa o passe opaco existente, sem histórico temporal ou amostragem de cor deslocada. A física dos fluidos e a formação de obsidiana seguem o jogo. Distorção do ar, iluminação dinâmica colorida, iluminação global e detecção de contato com rochas ficam fora desta versão. O shader usa a gravidade nas faces verticais. A direção de fluxo horizontal do fluido fica sob responsabilidade da malha e do atlas vanilla. Quinas e mudanças de altura podem expor a transição entre as projeções, especialmente na borda da queda. Uma distorção de calor localizada exigiria delimitar o volume quente e a oclusão da cena. O relógio de animação do Oculus retorna a zero ao acumular 3600 segundos, quando a fase desta animação também volta ao início.

## Arquivos

- `block.properties`, `gbuffers_terrain.fsh`, `gbuffers_terrain.vsh`, `lib/world.fsh`, `lib/world.vsh` e novo `lib/lava.glsl`.
- `lib/options.glsl`, `shaders.properties` e traduções do menu Lava.
- ZIP instalável Fantasy Foundation. A tarefa existente de preparação do `runClient` sincroniza o pacote.
- `validation/s001/forge.init.gradle` e `ShaderBenchmark.java` para cenas comparáveis. O roteiro cria a geometria de laboratório em uma cópia de mundo. A medição do Nether usa um mar natural da mesma seed. A câmera fica sobre uma plataforma de um bloco em X −8, Y 37, Z −24, com yaw de 270°.

## Capturas reais

Na caverna, a lava permanece emissiva sob o teto fechado. No par lava e água, a margem de pedra separa a lava opaca do fundo visível da água. As quedas mostram o padrão alongado ao longo da altura. No mar do Nether, os campos de crosta continuam visíveis entre a margem e a névoa.

Capturas feitas no cliente Forge a 1280 × 720. Cada coluna usa a mesma seed, posição e configuração de cena. O movimento do material, as partículas e os mobs podem variar entre quadros.

| Cena | S001.6 Eq. | Leve | Equilibrado | Alto |
| --- | --- | --- | --- | --- |
| Lago no Overworld | [Captura](assets/s001_6_1/lava_pool-old.png) | [Captura](assets/s001_6_1/lava_pool-leve.png) | [Captura](assets/s001_6_1/lava_pool-eq.png) | [Captura](assets/s001_6_1/lava_pool-alto.png) |
| Mar natural no Nether | [Captura](assets/s001_6_1/lava_nether-old.png) | [Captura](assets/s001_6_1/lava_nether-leve.png) | [Captura](assets/s001_6_1/lava_nether-eq.png) | [Captura](assets/s001_6_1/lava_nether-alto.png) |
| Rio corrente | [Captura](assets/s001_6_1/lava_river-old.png) | [Captura](assets/s001_6_1/lava_river-leve.png) | [Captura](assets/s001_6_1/lava_river-eq.png) | [Captura](assets/s001_6_1/lava_river-alto.png) |
| Queda vertical | [Captura](assets/s001_6_1/lava_fall-old.png) | [Captura](assets/s001_6_1/lava_fall-leve.png) | [Captura](assets/s001_6_1/lava_fall-eq.png) | [Captura](assets/s001_6_1/lava_fall-alto.png) |
| Caverna | [Captura](assets/s001_6_1/lava_cave-old.png) | [Captura](assets/s001_6_1/lava_cave-leve.png) | [Captura](assets/s001_6_1/lava_cave-eq.png) | [Captura](assets/s001_6_1/lava_cave-alto.png) |
| Lava próxima de água | [Captura](assets/s001_6_1/lava_water-old.png) | [Captura](assets/s001_6_1/lava_water-leve.png) | [Captura](assets/s001_6_1/lava_water-eq.png) | [Captura](assets/s001_6_1/lava_water-alto.png) |
| Lava junto à obsidiana | [Captura](assets/s001_6_1/lava_obsidian-old.png) | [Captura](assets/s001_6_1/lava_obsidian-leve.png) | [Captura](assets/s001_6_1/lava_obsidian-eq.png) | [Captura](assets/s001_6_1/lava_obsidian-alto.png) |
| Dia | [Captura](assets/s001_6_1/lava_day-old.png) | [Captura](assets/s001_6_1/lava_day-leve.png) | [Captura](assets/s001_6_1/lava_day-eq.png) | [Captura](assets/s001_6_1/lava_day-alto.png) |
| Noite | [Captura](assets/s001_6_1/lava_night-old.png) | [Captura](assets/s001_6_1/lava_night-leve.png) | [Captura](assets/s001_6_1/lava_night-eq.png) | [Captura](assets/s001_6_1/lava_night-alto.png) |
| Terceira pessoa com armadura | [Captura](assets/s001_6_1/lava_player-old.png) | [Captura](assets/s001_6_1/lava_player-leve.png) | [Captura](assets/s001_6_1/lava_player-eq.png) | [Captura](assets/s001_6_1/lava_player-alto.png) |
| Rotação rápida | [Captura](assets/s001_6_1/lava_turn-old.png) | [Captura](assets/s001_6_1/lava_turn-leve.png) | [Captura](assets/s001_6_1/lava_turn-eq.png) | [Captura](assets/s001_6_1/lava_turn-alto.png) |
| Próximo à superfície | [Captura](assets/s001_6_1/lava_close-old.png) | [Captura](assets/s001_6_1/lava_close-leve.png) | [Captura](assets/s001_6_1/lava_close-eq.png) | [Captura](assets/s001_6_1/lava_close-alto.png) |
| Cena com 33 mobs | [Captura](assets/s001_6_1/lava_mobs-old.png) | [Captura](assets/s001_6_1/lava_mobs-leve.png) | [Captura](assets/s001_6_1/lava_mobs-eq.png) | [Captura](assets/s001_6_1/lava_mobs-alto.png) |

## Desempenho

O cliente recebeu um pedido de 1920 × 1080 e criou um framebuffer de 1604 × 881. O quadro maior usa essa dimensão efetiva. Foi feita uma rodada por configuração. O 1% low é o inverso da média do 1% de frames mais lentos. Carregamento de chunks e simulação do servidor integrado também afetam as amostras.

Cada célula traz FPS médio, 1% low, p95 e p99 em ms, nessa ordem. GPU GTX 1050 Ti, Java 17, VSync desligado e distância de renderização de 8 chunks. As amostras começam após 48 ticks de aquecimento e cobrem os 60 ticks seguintes. A configuração anterior é a S001.6 no Equilibrado. Ao alternar de preset, os ajustes existentes de sombra e água variam junto com o detalhe da lava. A comparação direta do material usa Equilibrado contra Equilibrado.

### 1280 × 720

| Cena | S001.6 Eq. | Leve | Equilibrado | Alto |
| --- | --- | --- | --- | --- |
| Lago no Overworld | 356 / 125 / 4,84 / 7,45 | 320 / 101 / 5,46 / 8,54 | 278 / 98 / 6,90 / 9,42 | 282 / 84 / 6,98 / 10,83 |
| Mar natural no Nether | 431 / 119 / 3,86 / 5,18 | 383 / 88 / 4,12 / 5,65 | 395 / 103 / 3,92 / 5,39 | 404 / 138 / 4,16 / 6,08 |
| Rio corrente | 510 / 209 / 3,28 / 4,03 | 519 / 194 / 3,30 / 4,33 | 527 / 208 / 2,69 / 3,97 | 409 / 174 / 4,09 / 5,32 |
| Queda vertical | 241 / 136 / 5,27 / 5,58 | 310 / 165 / 4,25 / 4,63 | 234 / 155 / 5,57 / 6,19 | 162 / 117 / 7,93 / 8,39 |
| Caverna | 523 / 234 / 3,06 / 3,76 | 563 / 279 / 2,76 / 3,13 | 494 / 295 / 2,79 / 3,27 | 399 / 190 / 3,62 / 4,44 |
| Lava próxima de água | 488 / 269 / 2,95 / 3,44 | 564 / 308 / 2,65 / 2,94 | 466 / 294 / 3,03 / 3,24 | 379 / 229 / 3,65 / 4,17 |
| Lava junto à obsidiana | 535 / 286 / 2,69 / 3,24 | 588 / 273 / 2,50 / 2,97 | 510 / 294 / 2,76 / 3,22 | 408 / 194 / 3,48 / 4,21 |
| Dia | 511 / 306 / 2,78 / 3,12 | 554 / 327 / 2,64 / 2,91 | 478 / 305 / 2,99 / 3,17 | 388 / 237 / 3,56 / 3,84 |
| Noite | 489 / 283 / 2,90 / 3,21 | 541 / 330 / 2,64 / 2,89 | 444 / 285 / 3,13 / 3,29 | 364 / 233 / 3,74 / 4,05 |
| Terceira pessoa com armadura | 334 / 157 / 4,03 / 5,44 | 324 / 175 / 4,09 / 5,07 | 344 / 211 / 3,65 / 4,00 | 279 / 82 / 5,48 / 6,87 |
| Rotação rápida | 522 / 275 / 2,76 / 3,14 | 563 / 164 / 2,48 / 2,85 | 488 / 261 / 2,94 / 3,21 | 397 / 237 / 3,53 / 3,88 |
| Próximo à superfície | 451 / 155 / 3,06 / 3,33 | 504 / 274 / 2,83 / 3,35 | 426 / 287 / 3,21 / 3,38 | 336 / 215 / 4,01 / 4,26 |
| Cena com 33 mobs | 428 / 235 / 3,35 / 3,86 | 414 / 207 / 3,53 / 4,42 | 369 / 199 / 4,03 / 4,71 | 333 / 177 / 4,10 / 4,59 |

### 1604 × 881

| Cena | S001.6 Eq. | Leve | Equilibrado | Alto |
| --- | --- | --- | --- | --- |
| Lago no Overworld | 372 / 74 / 4,24 / 5,49 | 403 / 192 / 3,81 / 4,71 | 334 / 139 / 4,90 / 6,38 | 307 / 141 / 4,67 / 5,65 |
| Mar natural no Nether | 228 / 69 / 7,63 / 9,01 | 274 / 102 / 6,16 / 7,61 | 252 / 85 / 7,19 / 8,77 | 277 / 105 / 5,61 / 7,33 |
| Queda vertical | 167 / 111 / 7,94 / 8,29 | 217 / 144 / 6,18 / 6,56 | 162 / 116 / 7,70 / 8,30 | 114 / 59 / 10,54 / 11,25 |
| Caverna | 434 / 223 / 3,36 / 4,11 | 428 / 185 / 3,34 / 4,10 | 383 / 241 / 3,59 / 3,83 | 320 / 189 / 4,39 / 5,17 |

No mar do Nether, a 720p, o Equilibrado novo mediu 395 FPS e p95 de 3,92 ms, frente a 431 FPS e 3,86 ms na S001.6. Em 1604 × 881, os números foram 252 FPS e 7,19 ms, frente a 228 FPS e 7,63 ms. A direção da diferença mudou entre resoluções nas rodadas feitas. Na queda vertical a 1604 × 881, o Alto foi o cenário mais lento da matriz, com 114 FPS e p95 de 10,54 ms. [Mar na S001.6 a 1604 × 881](assets/s001_6_1/nether-s0016-1604.png) · [Mar no Alto a 1604 × 881](assets/s001_6_1/nether-alto-1604.png)

## Regressões

O build Forge `.\gradlew --offline build` terminou com sucesso. O comando `.\gradlew runClient -PtoeNoOculus` abriu o menu e terminou com sucesso. O ensaio separado sem Oculus registrou `shader=no-loader` e renderizou quatro cenas, incluindo o mar natural. O ZIP contém os mesmos 37 arquivos e conteúdos da pasta editável. [Mar sem Oculus](assets/s001_6_1/nether-sem-oculus.png)

O comando `.\gradlew runClient` sincronizou o ZIP. O hash SHA-256 de origem e o instalado coincidiram. No Nether, duas capturas com a câmera parada registraram posições diferentes das regiões quentes e da crosta. [Primeiro quadro](assets/s001_6_1/nether-runclient.png) · [Segundo quadro](assets/s001_6_1/nether-runclient-second.png)

A regressão de água completou seis cenas com o pacote final, incluindo o rio com SSR, o lago raso, o pôr do sol, a lua, a vila e a rotação de câmera. Os programas de óptica da água, céu, nuvens, iluminação e composição final coincidem byte a byte com os do ZIP anterior. [Rio no pacote final](assets/s001_6_1/water-regression.png)

A suíte existente de parkour e combate aprovou 30 verificações, incluindo sprint em oito direções, saltos, parede, beiral, mineração e animações com armadura. O diagnóstico W002.3 esteve ativo durante o ensaio.

A suíte existente C002.2/A001 aprovou 95 verificações com Oculus. A primeira execução pulou o registro de acabamento e brilho, e a repetição confirmou esse caso junto aos demais. O personagem chegou a 12% de opacidade em túnel e com armadura, voltou a 100% em espaço aberto e manteve a composição durante combate. A suíte também confirmou roll, oito direções de natação, UAL_swim, UAL_swim_idle e troca Classic/Slim com restauração de armaduras.

A aprovação artística depende do teste do usuário no Minecraft.
