# S001.6 — Água e reflexos

## Auditoria da S001.5.2

`gbuffers_water` identificava `minecraft:water` por `block.11001` e usava `lib/world.fsh`. O passe combinava o lightmap com uma cor fixa, perturbava a normal com um par seno/cosseno, misturava o antigo gradiente atmosférico pelo Fresnel e calculava o alpha sem profundidade. A reflexão via `horizonColor` ou `skyGradient` não representava os astros e as nuvens da S001.5.2. Toda a saída ia para `colortex0`.

No bytecode de `IrisRenderingPipeline.beginTranslucents` do Oculus 1.8.0, `copyPreTranslucentDepth` roda antes dos programas `deferred`. Isso disponibiliza `depthtex1` e permite copiar a cor já renderizada para outro alvo. `IrisSamplers.addRenderTargetSamplers` começa em 4 para a rota de mundo. O primeiro teste com os alvos 1 e 2 mostrou o atlas de blocos na água. A versão final grava a cópia em `colortex4` e o cache de céu em `colortex5`. A água escreve em `colortex0`, e a composição R3 continua no ponto existente do Oculus. [Estágio deferred](https://shaders.properties/current/reference/programs/deferred/) · [Buffers de profundidade](https://shaders.properties/current/reference/buffers/depthtex/)

## Superfície e luz

`lib/water_optics.glsl` deriva as inclinações da superfície de ondas em direções, frequências, amplitudes e velocidades distintas. As ondas são ancoradas no espaço do mundo. As normais ficam mais suaves a distância para conter cintilação. A geometria dos blocos e a física da água continuam vanilla.

O passe `deferred1` gera um mapa hemisférico de 256 × 96 uma vez por quadro. Equilibrado e Alto usam o mesmo gradiente, astros e volume de nuvens do céu acima da água. O Leve usa a mesma paleta celeste com a composição de nuvens desligada só no cache. A água consulta o mapa pela normal ondulada e dá mais peso à reflexão em ângulos rasantes usando Fresnel com refletância de 0,0204 na normal. Os destaques solar e lunar usam lóbulos direcionais, modulação de chuva e luz do céu. A fase lunar modula a intensidade.

## Profundidade, refração e visão subaquática

A projeção inversa reconstrói o fundo a partir de `depthtex1` e calcula a distância dentro da água. A transmitância exponencial absorve vermelho mais rapidamente que verde e azul. A areia e a prismarina aparecem nas margens. Água profunda recebe mais cor de espalhamento e deixa menos fundo visível. O canal de luz de bloco acrescenta uma contribuição quente em cavernas e perto de tochas.

Equilibrado e Alto podem deslocar uma única amostra da cópia opaca em função da normal ondulada. O deslocamento só entra quando a coordenada continua na tela, a geometria está atrás da superfície e a profundidade se mantém próxima à do fundo original. Quando a câmera está submersa, `final.fsh` aplica absorção em função da distância reconstruída do pixel. Em terra, a gradação usa a mesma leitura de cor da S001.5.2.

## SSR

O raio refletido avança no espaço de vista sobre `depthtex1`. Quando cruza a profundidade da cena, até quatro refinamentos localizam a amostra de `colortex4`. O shader rejeita superfícies abaixo da água e cruzamentos imprecisos. A borda da tela e a distância reduzem a confiança. Quando falta um cruzamento confiável, o reflexo usa o cache celeste. A contribuição é limitada pela confiança e pelo Fresnel.

| Preset | Ondas | Céu | Refração | Orçamento de SSR |
| --- | ---: | --- | --- | --- |
| Leve | 2 | Cores e astros | Amostra na posição original | Caminho omitido na compilação |
| Equilibrado | 4 | Cores, astros e nuvens | Deslocamento com teste de profundidade | 12 passos + até 4 refinamentos |
| Alto | 5 | Cores, astros e nuvens | Deslocamento com teste de profundidade | 20 passos + até 4 refinamentos |

`Reflexos do cenário (SSR)` no menu Água controla o caminho de geometria. A água continua usando o céu quando o SSR está desativado.

## Arquivos

- `lib/world.fsh`, novo `lib/water_optics.glsl`, `lib/skydome.glsl`, `lib/options.glsl` e `final.fsh`.
- Novos pares GLSL `deferred` e `deferred1`, `shaders.properties` e traduções do menu Água.
- ZIP Fantasy Foundation, sincronizado no `runClient` pelo mecanismo existente.
- Roteiro opt-in em `validation/s001/forge.init.gradle` e `ShaderBenchmark.java` para o lago raso, rio, lago fundo, pôr do sol, lua, chuva, natação, visão subaquática, vila, caverna, rotação e bordas.

## Capturas reais

As capturas foram feitas no cliente Forge com o roteiro `-PtoeS0016Qa`. Cada configuração usa uma cópia do mesmo mundo, cenário, hora e pose de câmera. As cenas animadas reaproveitam as mesmas entradas de movimento. Ondas, nuvens e partículas usam o tempo do cliente e podem variar entre quadros. A coluna S001.5.2 usa o Equilibrado anterior.

| Cena | Desligado | S001.5.2 | Leve | Equilibrado | Alto |
| --- | --- | --- | --- | --- | --- |
| Lago raso | [Captura](assets/s001_6/lake_shallow-off.png) | [Captura](assets/s001_6/lake_shallow-s00152.png) | [Captura](assets/s001_6/lake_shallow-leve.png) | [Captura](assets/s001_6/lake_shallow-equilibrado.png) | [Captura](assets/s001_6/lake_shallow-alto.png) |
| Rio com árvores | [Captura](assets/s001_6/river_trees-off.png) | [Captura](assets/s001_6/river_trees-s00152.png) | [Captura](assets/s001_6/river_trees-leve.png) | [Captura](assets/s001_6/river_trees-equilibrado.png) | [Captura](assets/s001_6/river_trees-alto.png) |
| Lago profundo | [Captura](assets/s001_6/lake_deep-off.png) | [Captura](assets/s001_6/lake_deep-s00152.png) | [Captura](assets/s001_6/lake_deep-leve.png) | [Captura](assets/s001_6/lake_deep-equilibrado.png) | [Captura](assets/s001_6/lake_deep-alto.png) |
| Pôr do sol | [Captura](assets/s001_6/water_sunset-off.png) | [Captura](assets/s001_6/water_sunset-s00152.png) | [Captura](assets/s001_6/water_sunset-leve.png) | [Captura](assets/s001_6/water_sunset-equilibrado.png) | [Captura](assets/s001_6/water_sunset-alto.png) |
| Lua | [Captura](assets/s001_6/water_moon-off.png) | [Captura](assets/s001_6/water_moon-s00152.png) | [Captura](assets/s001_6/water_moon-leve.png) | [Captura](assets/s001_6/water_moon-equilibrado.png) | [Captura](assets/s001_6/water_moon-alto.png) |
| Chuva | [Captura](assets/s001_6/water_rain-off.png) | [Captura](assets/s001_6/water_rain-s00152.png) | [Captura](assets/s001_6/water_rain-leve.png) | [Captura](assets/s001_6/water_rain-equilibrado.png) | [Captura](assets/s001_6/water_rain-alto.png) |
| Natação | [Captura](assets/s001_6/water_swim-off.png) | [Captura](assets/s001_6/water_swim-s00152.png) | [Captura](assets/s001_6/water_swim-leve.png) | [Captura](assets/s001_6/water_swim-equilibrado.png) | [Captura](assets/s001_6/water_swim-alto.png) |
| Submerso | [Captura](assets/s001_6/water_submerged-off.png) | [Captura](assets/s001_6/water_submerged-s00152.png) | [Captura](assets/s001_6/water_submerged-leve.png) | [Captura](assets/s001_6/water_submerged-equilibrado.png) | [Captura](assets/s001_6/water_submerged-alto.png) |
| Vila | [Captura](assets/s001_6/waterside_village-off.png) | [Captura](assets/s001_6/waterside_village-s00152.png) | [Captura](assets/s001_6/waterside_village-leve.png) | [Captura](assets/s001_6/waterside_village-equilibrado.png) | [Captura](assets/s001_6/waterside_village-alto.png) |
| Caverna | [Captura](assets/s001_6/water_cave-off.png) | [Captura](assets/s001_6/water_cave-s00152.png) | [Captura](assets/s001_6/water_cave-leve.png) | [Captura](assets/s001_6/water_cave-equilibrado.png) | [Captura](assets/s001_6/water_cave-alto.png) |
| Rotação | [Captura](assets/s001_6/water_turn-off.png) | [Captura](assets/s001_6/water_turn-s00152.png) | [Captura](assets/s001_6/water_turn-leve.png) | [Captura](assets/s001_6/water_turn-equilibrado.png) | [Captura](assets/s001_6/water_turn-alto.png) |
| Borda | [Captura](assets/s001_6/water_edge-off.png) | [Captura](assets/s001_6/water_edge-s00152.png) | [Captura](assets/s001_6/water_edge-leve.png) | [Captura](assets/s001_6/water_edge-equilibrado.png) | [Captura](assets/s001_6/water_edge-alto.png) |

No rio, os troncos aparecem sobre a superfície ondulada com SSR. Desativar o SSR mantém a água e o céu refletido. A fachada também aparece no Alto, com atenuação nas bordas e ondulação no reflexo. O lago raso mostra areia e prismarina. O lago profundo escurece progressivamente. A cena do pôr do sol tem uma faixa dourada e a da lua uma faixa azul. Na caverna, as tochas iluminam a margem e a água recebe contribuição quente. [Rio com SSR](assets/s001_6/river_trees-alto.png) · [Rio sem SSR](assets/s001_6/river_trees-alto-sem-ssr.png)

## Desempenho

GTX 1050 Ti, Forge 47.4.28, Oculus 1.8.0 e Embeddium 0.3.31. Distância de renderização de 8 chunks, VSync desligado, 48 ticks de aquecimento e 60 ticks de medição. As dimensões são as do framebuffer verificadas em execução. Cada célula mostra **FPS médio / 1% low em FPS / p95 em ms / p99 em ms**. O 1% low é o inverso da média dos tempos do 1% de quadros mais lentos. As comparações medem o quadro inteiro, com uma rodada por cenário e configuração. As duas resoluções maiores reutilizam seis cenas representativas do roteiro completo.

### 854 × 480

| Cena | Desligado | S001.5.2 Eq. | Leve | Equilibrado | Alto | Alto sem SSR |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| Lago raso | 413 / 93 / 4,77 / 9,22 | 221 / 88 / 7,21 / 9,64 | 245 / 101 / 7,12 / 9,41 | 199 / 70 / 8,93 / 12,37 | 223 / 83 / 7,80 / 10,70 | 171 / 55 / 9,91 / 13,73 |
| Rio | 991 / 289 / 1,74 / 2,90 | 450 / 169 / 3,65 / 4,70 | 421 / 157 / 3,98 / 4,86 | 348 / 144 / 4,74 / 5,75 | 347 / 169 / 4,43 / 5,15 | 392 / 163 / 4,15 / 5,19 |
| Lago fundo | 1264 / 416 / 1,04 / 2,19 | 633 / 270 / 2,28 / 3,17 | 613 / 259 / 2,25 / 3,42 | 485 / 207 / 3,30 / 4,09 | 381 / 197 / 3,98 / 4,48 | 418 / 185 / 3,87 / 4,90 |
| Pôr do sol | 1242 / 411 / 1,05 / 2,12 | 518 / 209 / 2,81 / 3,56 | 582 / 273 / 2,42 / 3,41 | 395 / 175 / 3,85 / 4,65 | 264 / 153 / 5,08 / 5,53 | 283 / 127 / 5,12 / 6,25 |
| Lua | 1293 / 461 / 0,96 / 1,99 | 688 / 206 / 1,93 / 2,80 | 666 / 312 / 2,04 / 2,94 | 551 / 274 / 2,64 / 3,41 | 445 / 226 / 3,47 / 3,89 | 455 / 201 / 3,38 / 4,13 |
| Chuva | 788 / 285 / 1,49 / 3,39 | 446 / 146 / 4,07 / 5,79 | 517 / 200 / 2,49 / 4,38 | 445 / 194 / 3,30 / 4,85 | 343 / 188 / 4,89 / 5,20 | 373 / 162 / 4,83 / 5,57 |
| Armadura | 578 / 164 / 2,57 / 3,23 | 244 / 152 / 5,20 / 5,77 | 233 / 147 / 5,54 / 6,43 | 225 / 146 / 5,56 / 6,23 | 235 / 149 / 5,45 / 6,04 | 236 / 132 / 5,47 / 6,18 |
| Natação | 660 / 305 / 1,95 / 3,01 | 254 / 100 / 5,28 / 6,13 | 242 / 134 / 5,64 / 6,19 | 236 / 140 / 5,58 / 6,32 | 228 / 85 / 5,70 / 6,82 | 254 / 142 / 5,24 / 5,99 |
| Submerso | 1424 / 500 / 0,88 / 1,90 | 495 / 260 / 2,73 / 3,62 | 582 / 235 / 2,53 / 3,36 | 428 / 88 / 3,06 / 3,67 | 304 / 193 / 4,30 / 4,99 | 303 / 206 / 4,45 / 4,67 |
| Vila | 1425 / 483 / 0,88 / 1,85 | 660 / 314 / 1,98 / 3,00 | 626 / 229 / 2,05 / 2,96 | 500 / 297 / 2,67 / 3,24 | 360 / 213 / 3,90 / 4,47 | 405 / 213 / 3,56 / 3,98 |
| Caverna | 1355 / 467 / 0,92 / 1,97 | 651 / 256 / 2,11 / 3,19 | 617 / 288 / 2,27 / 3,06 | 505 / 286 / 2,67 / 3,27 | 366 / 214 / 3,70 / 4,53 | 422 / 251 / 3,25 / 3,64 |
| Rotação | 1256 / 340 / 1,27 / 2,51 | 664 / 295 / 1,85 / 3,22 | 643 / 318 / 2,10 / 2,96 | 504 / 291 / 2,70 / 3,20 | 361 / 211 / 4,11 / 4,66 | 408 / 167 / 3,53 / 4,29 |
| Borda | 1291 / 325 / 1,28 / 2,54 | 674 / 308 / 1,82 / 3,16 | 643 / 280 / 2,14 / 2,98 | 506 / 288 / 2,69 / 3,15 | 364 / 214 / 4,19 / 4,61 | 413 / 260 / 3,37 / 3,64 |
| Túnel | 731 / 287 / 2,20 / 3,01 | 325 / 191 / 4,31 / 4,67 | 331 / 183 / 4,21 / 4,50 | 313 / 218 / 4,16 / 4,38 | 316 / 198 / 4,33 / 4,59 | 311 / 203 / 4,03 / 4,37 |
| Saída do túnel | 756 / 356 / 1,63 / 2,63 | 265 / 168 / 5,05 / 5,50 | 265 / 166 / 4,97 / 5,57 | 258 / 175 / 4,88 / 5,23 | 263 / 183 / 4,93 / 5,33 | 266 / 194 / 4,56 / 4,92 |
| Combate | 566 / 213 / 2,70 / 3,68 | 245 / 164 / 5,41 / 5,89 | 244 / 123 / 5,46 / 7,66 | 239 / 163 / 5,26 / 5,78 | 227 / 144 / 5,77 / 6,31 | 227 / 159 / 5,44 / 5,91 |

No rio a 854 × 480, o p95 foi 3,65 ms na S001.5.2, 3,98 ms no Leve, 4,74 ms no Equilibrado e 4,43 ms no Alto. O Alto sem SSR mediu 4,15 ms. A maior diferença do Alto nessa resolução está no pôr do sol, com p95 de 5,08 ms, ante 2,81 ms da S001.5.2 no Equilibrado.

### 1280 × 720

| Cena | S001.5.2 Eq. | Equilibrado | Alto | Alto sem SSR |
| --- | ---: | ---: | ---: | ---: |
| Lago raso | 201 / 55 / 10,33 / 17,04 | 220 / 73 / 7,93 / 11,48 | 194 / 76 / 8,86 / 11,81 | 162 / 60 / 11,85 / 15,95 |
| Rio | 362 / 180 / 4,49 / 5,24 | 301 / 158 / 4,99 / 5,63 | 233 / 137 / 6,02 / 6,82 | 269 / 125 / 6,10 / 7,51 |
| Pôr do sol | 286 / 169 / 4,78 / 5,49 | 228 / 136 / 5,89 / 6,56 | 146 / 102 / 8,66 / 9,32 | 165 / 109 / 8,04 / 8,89 |
| Lua | 504 / 246 / 2,76 / 3,86 | 360 / 168 / 4,26 / 5,41 | 322 / 159 / 4,28 / 4,78 | 334 / 134 / 4,68 / 5,54 |
| Vila | 422 / 218 / 3,23 / 4,44 | 305 / 177 / 4,89 / 5,34 | 229 / 138 / 5,76 / 6,33 | 274 / 137 / 5,31 / 5,67 |
| Rotação | 404 / 198 / 3,96 / 4,56 | 304 / 180 / 4,92 / 5,35 | 227 / 145 / 5,92 / 6,35 | 266 / 162 / 5,40 / 5,90 |

No Alto, desligar SSR aumentou a média de 233 para 269 FPS no rio, com p95 de 6,02 para 6,10 ms. Na vila, a média passou de 229 para 274 FPS e o p95 de 5,76 para 5,31 ms. [Rio 720p com SSR](assets/s001_6/river-alto-720.png) · [Rio 720p sem SSR](assets/s001_6/river-alto-720-sem-ssr.png)

### 1604 × 881

O roteiro solicitou 1920 × 1080. O desktop disponível reportou 1600 × 900, e o cliente criou um framebuffer de 1604 × 881. Esta tabela usa a dimensão efetiva.

| Cena | S001.5.2 Eq. | Equilibrado | Alto |
| --- | ---: | ---: | ---: |
| Lago raso | 201 / 76 / 8,25 / 11,86 | 173 / 64 / 10,15 / 13,65 | 151 / 63 / 10,83 / 15,58 |
| Rio | 307 / 167 / 5,00 / 5,53 | 221 / 123 / 6,44 / 7,58 | 174 / 108 / 7,95 / 8,57 |
| Pôr do sol | 198 / 129 / 6,46 / 6,86 | 159 / 114 / 8,24 / 8,49 | 101 / 76 / 11,81 / 12,64 |
| Lua | 395 / 239 / 3,49 / 4,10 | 295 / 168 / 5,12 / 5,54 | 258 / 137 / 5,38 / 5,91 |
| Vila | 310 / 201 / 4,23 / 4,77 | 224 / 150 / 6,23 / 6,58 | 170 / 118 / 7,46 / 8,00 |
| Rotação | 303 / 176 / 4,57 / 5,18 | 223 / 141 / 6,05 / 6,43 | 170 / 123 / 7,40 / 8,07 |

Em 1604 × 881, o Equilibrado novo mediu 221 FPS médios e p95 de 6,44 ms no rio, ante 307 FPS e 5,00 ms na S001.5.2. O Alto atingiu 101 FPS médios, 76 FPS no 1% low e p95 de 11,81 ms no pôr do sol. O custo de cenas rasantes aumenta com a área de água refletiva. [Pôr do sol no Alto a 1604 × 881](assets/s001_6/sunset-alto-1604.png)

### GPU e VRAM

A utilização e a VRAM são médias das amostras NVML registradas a cada segundo, alinhadas ao fim da janela de medição. São valores globais do dispositivo, incluindo outros processos do desktop.

| Resolução e perfil | Rio GPU / VRAM | Pôr do sol GPU / VRAM |
| --- | ---: | ---: |
| 720p, S001.5.2 Eq. | 87% / 1248 MiB | 99% / 1266 MiB |
| 720p, Equilibrado | 95% / 1262 MiB | 100% / 1277 MiB |
| 720p, Alto | 100% / 1317 MiB | 100% / 1342 MiB |
| 720p, Alto sem SSR | 98% / 1324 MiB | 100% / 1345 MiB |
| 1604 × 881, S001.5.2 Eq. | 100% / 1290 MiB | 100% / 1306 MiB |
| 1604 × 881, Equilibrado | 93% / 1320 MiB | 100% / 1337 MiB |
| 1604 × 881, Alto | 100% / 1358 MiB | 100% / 1378 MiB |

## Regressões e limites

O roteiro de água registrou natação efetiva com armadura em terceira pessoa, visão subaquática em primeira pessoa e rotação de câmera durante a medição. O túnel e a recuperação de opacidade usam o mesmo compositor C002.2. [Armadura](assets/s001_6/qa-armor.png) · [Túnel](assets/s001_6/qa-camera_tunnel.png) · [Saída do túnel](assets/s001_6/qa-camera_restore.png) · [Combate](assets/s001_6/qa-combat.png) O roteiro sem Oculus renderizou o lago raso. [Lago sem Oculus](assets/s001_6/lake-native.png)

A regressão existente `ExpansionRuntimeTest` marcou 30 verificações como aprovadas, incluindo combate, mineração, sprint em oito direções, saltos, parede, beiral e animações com armadura. O diagnóstico W002.3 permaneceu ativo durante a execução.

A regressão de câmera e armaduras executou 95 verificações com Oculus e o ZIP final. Registrou `0,12` no túnel e com armadura, `1,0` após a saída, roll ativo, natação em oito direções, troca entre Classic e Slim e restauração das armaduras após troca de skin.

O comando `.\gradlew runClient` sincronizou o ZIP e abriu o mundo de QA com Oculus e Embeddium. O ZIP instalado tem o mesmo SHA-256 da entrega. O comando `.\gradlew runClient -PtoeNoOculus` chegou ao menu Forge, e o roteiro separado sem Oculus entrou no mundo e renderizou o lago. Os dois comandos terminaram com `BUILD SUCCESSFUL`. [Água pelo comando padrão](assets/s001_6/main-water.png) · [Menu sem Oculus](assets/s001_6/main-native-title.png)

O SSR depende da geometria visível no quadro e pode perder partes dos reflexos em bordas, por oclusão ou por falta de profundidade válida. A cópia inclui as passagens que Oculus já compôs antes de `beginTranslucents`. O reflexo e a refração representam a cena até esse ponto. Vidro e outros translúcidos desenhados depois da cópia ficam sujeitos à ordenação do cliente. A composição tardia da transparência contextual fica fora do SSR. O cache aproxima o céu a partir da posição da câmera. Ele preserva a direção e a paleta das nuvens, com resolução angular menor. Caustics, reflexos fora de tela e sombras das nuvens na água ficam fora desta implementação.
