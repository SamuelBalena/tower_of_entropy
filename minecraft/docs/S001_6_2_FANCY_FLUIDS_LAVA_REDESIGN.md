# S001.6.2 — Lava inspirada em Rekindled

## Referência e licenças

Na captura oficial Rekindled, o campo é predominantemente laranja e contínuo. O dourado aparece em detalhes pequenos, distribuídos pela superfície. O padrão permanece visível longe da câmera. A captura permite observar escala e distribuição de cor. A animação, as faces verticais e a aparência de perto exigem observação em jogo. O pacote Rekindled do JAR configura escala 4 para lava e água e inclui sprites distintos para fonte e fluxo, ambos com 64 quadros, interpolação e `frametime` 4. Os sprites têm quadros de 16 × 16 para fonte e 32 × 32 para fluxo. [Projeto Fancy Fluids](https://www.curseforge.com/minecraft/mc-mods/fancy-fluids) · [Captura oficial Rekindled](https://media.forgecdn.net/attachments/description/null/description_e21b2303-9ae4-4386-ab7e-7bcb961fb0f9.png)

A ficha do CurseForge declara LGPLv3. O `META-INF/mods.toml` do JAR 0.2 declara Apache 2.0, e o `pack.mcmeta` de Rekindled credita Tec sem declarar licença individual. O projeto também credita Tec pelas texturas originais e pelo port inicial. Essa divergência deixa a licença individual dos sprites sem confirmação. A S001.6.2 usa GLSL próprio e o atlas que o próprio Minecraft fornece em tempo de execução. O mod e os assets de terceiros ficam fora do ZIP distribuído. [Ficha do arquivo 0.2 para Forge 1.20.1](https://www.curseforge.com/minecraft/mc-mods/fancy-fluids/files/8283107) · [Créditos do projeto](https://www.curseforge.com/minecraft/mc-mods/fancy-fluids)

## Auditoria da S001.6.1

`lavaNoise()` produz valores interpolados entre vértices da grade. A escala principal de 0,79 célula por bloco gerava regiões coerentes de vários blocos após a deformação. Valores abaixo de 0,43 mantinham a crosta quase toda escura. `seam` iluminava só uma faixa perto de 0,44 e `heat` controlava a gradação e o laranja do magma. A mistura final separava grandes áreas de crosta do líquido luminoso. A gradação de contraste empurrava os tons baixos ainda mais perto do preto.

O caminho existente usa `block.properties`, os programas `gbuffers_terrain` e `lib/world` para reconhecer fonte e fluxo via `mc_Entity`. A lava sai com alpha 1 e neblina do terreno. O ramo óptico da água roda em outro programa. A mudança de material está em `lib/lava.glsl`. O roteiro `validation/s001/…/ShaderBenchmark.java` ganhou borda de pedra e terra na poça e ângulo fixo no mar. O ZIP e a pasta editável estão sincronizados pelo mecanismo existente do `runClient`.

## Material

A superfície é ancorada em XZ, com rotação da grade. Há 5,8 células de base por bloco e o dobro dessa frequência para os pontos quentes. A distância suaviza as frequências finas. Toda a superfície usa a mesma faixa térmica. O material interpola de vermelho-alaranjado para laranja, com focos pequenos de dourado. A cor vem da emissão visual do material, com uma modulação discreta do atlas animado do jogo. Iluminação dos blocos vizinhos, física, composição de água, nuvens, céu e menu Lava usam seus caminhos existentes.

As fontes têm circulação suave. Os estados correntes usam deslocamento maior. Nas faces verticais, a projeção inclui a altura real da malha e a advecção descendente usa 0,2 célula por segundo. O estado do fluido indica fonte ou corrente. A malha não expõe ao shader um vetor horizontal confiável, então a circulação horizontal é local. O campo repete a cada 720 células, e os harmônicos animados e a advecção foram escolhidos para coincidir com a volta a zero do relógio do Oculus em 3600 segundos. A junção entre projeções pode ficar visível nas quinas de algumas quedas.

| Preset | Campos de ruído | Diferença |
| --- | ---: | --- |
| Leve | 2 | Corpo laranja e pontos quentes |
| Equilibrado | 3 | Segundo tamanho de detalhe nos pontos quentes |
| Alto | 4 | Microvariação na temperatura perto da câmera |

A implementação procedural reaproveitou o caminho comprovado no terreno opaco. A alternativa de uma textura ampliada teria exigido um asset original e um sampler de terreno. Este material usa o atlas já ligado e uma amostragem procedural a menos em cada preset, comparado à S001.6.1.

## Comparações no Minecraft

No recorte de lava da poça, a S001.6.1 teve 9.269 de 22.080 amostras com luma RGB ponderada abaixo de 31 em 255. O Equilibrado novo teve zero no mesmo recorte. A medição usa uma grade de quatro pixels em um único quadro por versão. Ela quantifica a remoção das áreas quase pretas naquele enquadramento.

Poça com borda de pedra e terra e câmera a meio bloco da margem. A S001.6.1 e os três presets foram capturados em 1280 × 720, no mesmo mundo e com as mesmas poses do roteiro. Os quadros correspondem a instantes diferentes do material e das partículas.

| Cena | S001.6.1 Eq. | Leve | Equilibrado | Alto |
| --- | --- | --- | --- | --- |
| Poça de pedra e terra | [Captura](assets/s001_6_2/lava_pool-old.png) | [Captura](assets/s001_6_2/lava_pool-leve.png) | [Captura](assets/s001_6_2/lava_pool-eq.png) | [Captura](assets/s001_6_2/lava_pool-alto.png) |
| Mar natural do Nether | [Captura](assets/s001_6_2/lava_nether-old.png) | [Captura](assets/s001_6_2/lava_nether-leve.png) | [Captura](assets/s001_6_2/lava_nether-eq.png) | [Captura](assets/s001_6_2/lava_nether-alto.png) |
| Rio corrente | [Captura](assets/s001_6_2/lava_river-old.png) | [Captura](assets/s001_6_2/lava_river-leve.png) | [Captura](assets/s001_6_2/lava_river-eq.png) | [Captura](assets/s001_6_2/lava_river-alto.png) |
| Queda | [Captura](assets/s001_6_2/lava_fall-old.png) | [Captura](assets/s001_6_2/lava_fall-leve.png) | [Captura](assets/s001_6_2/lava_fall-eq.png) | [Captura](assets/s001_6_2/lava_fall-alto.png) |
| Caverna escura | [Captura](assets/s001_6_2/lava_cave-old.png) | [Captura](assets/s001_6_2/lava_cave-leve.png) | [Captura](assets/s001_6_2/lava_cave-eq.png) | [Captura](assets/s001_6_2/lava_cave-alto.png) |
| Vista próxima | [Captura](assets/s001_6_2/lava_close-old.png) | [Captura](assets/s001_6_2/lava_close-leve.png) | [Captura](assets/s001_6_2/lava_close-eq.png) | [Captura](assets/s001_6_2/lava_close-alto.png) |
| Rotação rápida | [Captura](assets/s001_6_2/lava_turn-old.png) | [Captura](assets/s001_6_2/lava_turn-leve.png) | [Captura](assets/s001_6_2/lava_turn-eq.png) | [Captura](assets/s001_6_2/lava_turn-alto.png) |
| Terceira pessoa | [Captura](assets/s001_6_2/lava_player-old.png) | [Captura](assets/s001_6_2/lava_player-leve.png) | [Captura](assets/s001_6_2/lava_player-eq.png) | [Captura](assets/s001_6_2/lava_player-alto.png) |
| Lava próxima de água | [Captura](assets/s001_6_2/lava_water-old.png) | [Captura](assets/s001_6_2/lava_water-leve.png) | [Captura](assets/s001_6_2/lava_water-eq.png) | [Captura](assets/s001_6_2/lava_water-alto.png) |

O controle vanilla usa o shader desligado. A comparação original usa Fancy Fluids 0.2 só numa instância Forge separada, com o mesmo mundo e pack `prettier_water_rekindled` confirmado no log de recursos. O shader ficou desligado para esse controle. Na poça, os dois materiais distribuem os pontos dourados pelo laranja contínuo. A referência em jogo exibe texels maiores e mais amarelo na queda. A implementação própria mostra um padrão mais fino e suave, com uma paleta mais laranja nas faces verticais.

| Controle | Poça | Nether | Queda |
| --- | --- | --- | --- |
| Vanilla | [Captura](assets/s001_6_2/lava_pool-vanilla.png) | [Captura](assets/s001_6_2/lava_nether-vanilla.png) | [Captura](assets/s001_6_2/lava_fall-vanilla.png) |
| Fancy Fluids Rekindled | [Captura](assets/s001_6_2/lava_pool-rekindled.png) | [Captura](assets/s001_6_2/lava_nether-rekindled.png) | [Captura](assets/s001_6_2/lava_fall-rekindled.png) |

## Desempenho

Na poça a 720p, o Equilibrado passou de 311 para 351 FPS e de 5,12 para 4,61 ms no p95. No mar, as médias ficaram próximas, de 280 para 278 FPS. O p95 caiu de 6,24 para 5,57 ms e o p99 de 7,78 para 6,75 ms. As rodadas são medições locais curtas e incluem o custo do servidor integrado e do carregamento.

Cada célula traz FPS médio, 1% low, p95 e p99 em ms. GTX 1050 Ti, Java 17, distância de renderização de 8 chunks e VSync desligado. O mar foi medido isoladamente com orientação de câmera fixa. Cada perfil teve uma rodada a 1280 × 720, com 48 ticks de aquecimento e 60 ticks de amostragem por cena. O 1% low é o inverso da média do 1% de frames mais lentos. Leve e Alto mantêm as diferenças existentes de sombra e água. Equilibrado contra Equilibrado é a comparação direta do material.

| Cena | S001.6.1 Eq. | Leve | Equilibrado | Alto |
| --- | --- | --- | --- | --- |
| Poça de pedra e terra | 311 / 140 / 5,12 / 6,50 | 356 / 108 / 5,62 / 7,38 | 351 / 145 / 4,61 / 6,40 | 296 / 86 / 6,83 / 10,37 |
| Mar natural do Nether | 280 / 85 / 6,24 / 7,78 | 278 / 107 / 5,84 / 7,20 | 278 / 110 / 5,57 / 6,75 | 270 / 115 / 6,37 / 7,97 |
| Rio corrente | 517 / 220 / 3,14 / 3,68 | 598 / 235 / 2,58 / 3,91 | 541 / 241 / 2,84 / 3,51 | 424 / 161 / 3,82 / 5,25 |
| Queda | 238 / 159 / 5,54 / 6,16 | 314 / 177 / 4,35 / 5,14 | 246 / 159 / 5,33 / 6,03 | 162 / 118 / 7,76 / 8,33 |
| Caverna escura | 492 / 236 / 3,02 / 3,97 | 576 / 285 / 2,63 / 3,00 | 510 / 192 / 2,84 / 3,25 | 408 / 242 / 3,49 / 3,91 |
| Vista próxima | 425 / 280 / 3,22 / 3,41 | 531 / 327 / 2,71 / 2,91 | 458 / 191 / 3,46 / 4,29 | 321 / 128 / 5,72 / 7,11 |
| Rotação rápida | 489 / 281 / 2,96 / 3,25 | 584 / 327 / 2,53 / 2,90 | 496 / 87 / 2,83 / 4,01 | 403 / 247 / 3,43 / 3,84 |
| Terceira pessoa | 294 / 86 / 4,89 / 6,92 | 322 / 102 / 4,44 / 5,84 | 297 / 40 / 4,36 / 5,19 | 298 / 105 / 4,41 / 4,97 |
| Lava próxima de água | 464 / 236 / 3,16 / 3,99 | 574 / 311 / 2,58 / 2,95 | 485 / 246 / 2,94 / 3,50 | 383 / 228 / 3,64 / 4,14 |

## Validação e limites

O build Forge `.\gradlew --offline build` passou. O comando `.\gradlew runClient` entrou no Nether com Oculus e Embeddium e terminou com sucesso. O ZIP contém os mesmos 37 arquivos da pasta editável, e a cópia instalada coincide byte a byte com o ZIP de entrega. Somente `lib/lava.glsl` difere entre o ZIP anterior e o atual. As capturas abaixo, separadas por cerca de nove segundos, mostram a animação com a câmera parada. [Primeiro quadro](assets/s001_6_2/lava-runclient-a.png) · [Segundo quadro](assets/s001_6_2/lava-runclient-b.png)

O comando `.\gradlew runClient -PtoeNoOculus` chegou ao menu Forge e encerrou com sucesso. A matriz com o shader ativo inclui Polymesh com armadura em terceira pessoa e água ao lado da lava. A verificação de água, céu, nuvens e iluminação também comparou os arquivos internos dos ZIPs byte a byte. A suíte completa de câmera, parkour, combate, roll e natação está registrada na S001.6.1. [Cliente sem Oculus](assets/s001_6_2/forge-sem-oculus.png)

A aprovação visual final fica com o usuário.
