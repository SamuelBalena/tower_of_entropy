# S001.4 — Iluminação e sombras do Overworld

## Auditoria

Na S001.3, `world.fsh` começava por `albedo × lightmap` e aplicava a sombra como parte de uma modulação dessa cor. O caminho `TERRAIN` já comparava profundidade em `shadowtex0`, mas deixava pelo menos 78% da contribuição no fator de sombra e diluía a diferença pelo horário, pela chuva e pela exposição à luz do céu. O resultado de `gbuffers_entities` e `gbuffers_block` vinha apenas da textura, da cor interpolada dos vértices e do lightmap.

O Oculus rasteriza profundidade da geometria no passe `shadow`. `shadow.vsh` usa a transformação fornecida para esse passe e `shadow.fsh` descarta texels com alpha abaixo de 0,45, inclusive os recortes das folhas. O caminho receptor transforma a posição de vista por `gbufferModelViewInverse`, `shadowModelView` e `shadowProjection` e compara sua profundidade com a do mapa. `sunPosition` permanece no espaço de vista para o produto com a normal. O componente vertical do vetor solar em espaço de mundo determina iluminação diurna, crepúsculo e noite. [Documentação do pipeline de sombras do Iris](https://shaders.properties/current/guides/your-first-shaderpack/4_shadows/)

## Modelo novo

- Terreno, entidades e blocos com renderização especial compartilham o modelo em `lib/lighting.glsl`. As coordenadas do lightmap fornecem níveis independentes de luz do céu e de bloco. A textura do lightmap continua nos passes que não usam esse modelo.
- A luz ambiente combina um piso para leitura de superfícies com céu frio orientado pela normal. O sol soma luz proporcional ao ângulo da superfície e à altura solar. A visibilidade do shadow map atua só nessa parcela. Lanternas, tochas e outras fontes de luz de bloco acrescentam uma parcela quente local, inclusive sob sombra.
- Sol baixo aumenta o contraste entre luz quente e ambiente violeta. A noite mantém luz ambiente azul e lanternas quentes. A chuva reduz a luz solar e aproxima o ambiente de céu encoberto. Uma névoa suave por distância e luz do céu acrescenta profundidade no terreno exposto.
- O filtro compara profundidades separadamente com amostragem `nearest`. A posição da superfície receptora é deslocada entre 0,012 e 0,045 bloco conforme a incidência da luz. Cada comparação usa a profundidade do plano receptor no centro do texel consultado. Isso reduz as listras de auto-sombra nas superfícies inclinadas em relação ao mapa, especialmente no Leve ao entardecer. Um viés residual reduz erros de quantização, e o efeito faz uma transição no limite do mapa.
- A projeção de entidades permanece a cargo do Oculus. Em terceira pessoa, o R3 pode participar tanto da gravação de profundidade do sol quanto da renderização colorida. São duas passagens com saídas diferentes. A C002.2 mantém uma composição de cor do jogador local por quadro quando a câmera recua. A pele e a armadura usam a iluminação dos programas de entidade.

| Preset | Mapa | Alcance nominal | Filtro de profundidade |
| --- | ---: | ---: | --- |
| Leve | 512² | 44 blocos | 1 comparação, borda rígida |
| Equilibrado | 1024² | 68 blocos | 5 comparações em cruz |
| Alto | 2048² | 100 blocos | 9 comparações em grade, cobertura mais extensa |

O alcance deriva da resolução do mapa. As três opções gravadas pelos presets anteriores continuam válidas. O alcance efetivo também depende da distância carregada pelo cliente. O passe final da S001.3 continua fazendo gradação com um texel do quadro atual.

## Arquivos

Na pasta editável do pack, foram alterados os pares de programas `gbuffers_terrain`, `gbuffers_entities` e `gbuffers_block`, os arquivos `lib/world.fsh`, `lib/world.vsh` e `lib/options.glsl`. Os arquivos novos são `lib/lighting.glsl` e `lib/shadow.glsl`. O ZIP continua com o nome que o `runClient` sincroniza. O roteiro opt-in foi acrescentado a `validation/s001/forge.init.gradle` e `validation/s001/java/com/towerofentropy/client/qa/ShaderBenchmark.java`.

## Comparações no Minecraft

O roteiro `-PtoeS0014Qa` prepara os mesmos blocos e reposiciona a câmera em quatro cópias do mesmo mundo. A vila, a floresta e a planície começam no tick 3500. As outras tomadas da vila usam 1200, 11200, 17000 e, sob chuva, 6000. A tomada de armadura também usa 6000. O log registra a hora e a intensidade da chuva no cliente. O quadro com shader desligado mantém Forge, Oculus, Embeddium e R3 carregados para comparar apenas o efeito de ativar o pack.

| Cena | Desligado | Leve | Equilibrado | Alto |
| --- | --- | --- | --- | --- |
| Vila | [Captura](assets/s001_4/village-off.png) | [Captura](assets/s001_4/village-leve.png) | [Captura](assets/s001_4/village-equilibrado.png) | [Captura](assets/s001_4/village-alto.png) |
| Floresta densa | [Captura](assets/s001_4/forest-off.png) | [Captura](assets/s001_4/forest-leve.png) | [Captura](assets/s001_4/forest-equilibrado.png) | [Captura](assets/s001_4/forest-alto.png) |
| Planície | [Captura](assets/s001_4/plains-off.png) | [Captura](assets/s001_4/plains-leve.png) | [Captura](assets/s001_4/plains-equilibrado.png) | [Captura](assets/s001_4/plains-alto.png) |
| Manhã | [Captura](assets/s001_4/dawn-off.png) | [Captura](assets/s001_4/dawn-leve.png) | [Captura](assets/s001_4/dawn-equilibrado.png) | [Captura](assets/s001_4/dawn-alto.png) |
| Entardecer | [Captura](assets/s001_4/sunset-off.png) | [Captura](assets/s001_4/sunset-leve.png) | [Captura](assets/s001_4/sunset-equilibrado.png) | [Captura](assets/s001_4/sunset-alto.png) |
| Noite | [Captura](assets/s001_4/night-off.png) | [Captura](assets/s001_4/night-leve.png) | [Captura](assets/s001_4/night-equilibrado.png) | [Captura](assets/s001_4/night-alto.png) |
| Chuva | [Captura](assets/s001_4/rain-off.png) | [Captura](assets/s001_4/rain-leve.png) | [Captura](assets/s001_4/rain-equilibrado.png) | [Captura](assets/s001_4/rain-alto.png) |
| Armadura | [Captura](assets/s001_4/armor-off.png) | [Captura](assets/s001_4/armor-leve.png) | [Captura](assets/s001_4/armor-equilibrado.png) | [Captura](assets/s001_4/armor-alto.png) |

As casas sombreiam a rua e as fachadas conforme a posição do sol. A faixa iluminada passa da direita para a esquerda entre manhã e entardecer. Na floresta, as copas projetam recortes no chão. Na chuva, o contraste solar diminui e as lanternas continuam iluminando o entorno. O destaque mais visível entre presets está nas bordas de sombra. Leve mantém contornos em degraus. Equilibrado e Alto filtram esses degraus com cobertura mais fina.

As listras da primeira versão do filtro de sombras do Leve desapareceram da fachada e da rua na tomada de entardecer após a correção do plano receptor. [Leve antes da correção do viés](assets/s001_4/sunset-leve-before-bias.png) · [Leve final](assets/s001_4/sunset-leve.png)

Uma cópia diagnóstica mostrou `shadowtex0` no canto superior direito e desativou apenas sua contribuição à visibilidade solar. Na rua, o pixel (419, 442) passou de RGB (198, 159, 75) no Equilibrado final para (250, 198, 89) no diagnóstico. O pixel já iluminado (410, 262) ficou em (231, 180, 83) nas duas capturas. O diagnóstico foi executado em uma instância isolada. [Equilibrado](assets/s001_4/village-equilibrado.png) · [Diagnóstico do mapa de profundidade](assets/s001_4/shadow-depth-probe.png)

## Desempenho

Medições em Minecraft 1.20.1 com Forge 47.4.28, Oculus 1.8.0, Embeddium 0.3.31 e GTX 1050 Ti, a 854 × 480, distância de renderização de 8 chunks e VSync desligado. Cada cena tem 48 ticks de aquecimento e 60 medidos. Uma célula mostra **FPS médio / 1% low em FPS / p95 em ms**. O 1% low usa o inverso da média dos tempos dos 1% de quadros mais lentos.

| Cenário | Desligado | Leve | Equilibrado | Alto |
| --- | ---: | ---: | ---: | ---: |
| Vila | 343 / 88 / 5,68 | 186 / 77 / 9,61 | 198 / 52 / 9,42 | 133 / 57 / 12,77 |
| Floresta | 994 / 220 / 1,65 | 366 / 164 / 4,24 | 380 / 156 / 4,27 | 350 / 132 / 4,84 |
| Planície | 1386 / 330 / 0,96 | 573 / 251 / 2,87 | 556 / 215 / 2,90 | 564 / 213 / 2,93 |
| Manhã | 662 / 236 / 2,27 | 345 / 206 / 4,11 | 353 / 151 / 4,19 | 329 / 180 / 4,35 |
| Entardecer | 763 / 271 / 1,74 | 329 / 158 / 4,27 | 305 / 167 / 4,66 | 329 / 158 / 4,27 |
| Noite | 765 / 281 / 1,79 | 318 / 150 / 4,78 | 339 / 184 / 4,09 | 361 / 216 / 3,93 |
| Chuva | 512 / 177 / 2,64 | 246 / 103 / 6,38 | 284 / 143 / 5,71 | 295 / 147 / 5,62 |
| Armadura | 634 / 256 / 2,60 | 268 / 181 / 4,82 | 264 / 174 / 4,78 | 247 / 130 / 5,57 |
| Túnel | 695 / 252 / 2,55 | 284 / 169 / 4,68 | 282 / 181 / 4,57 | 279 / 181 / 4,83 |
| Saída do túnel | 675 / 345 / 2,13 | 236 / 129 / 5,53 | 260 / 127 / 5,21 | 233 / 100 / 6,31 |
| Combate | 549 / 259 / 2,77 | 219 / 112 / 5,95 | 230 / 113 / 6,02 | 209 / 105 / 7,06 |
| Submerso | 1449 / 457 / 0,98 | 647 / 296 / 2,18 | 576 / 228 / 2,76 | 620 / 306 / 2,42 |
| Água | 1249 / 462 / 1,13 | 552 / 275 / 2,76 | 521 / 269 / 2,79 | 503 / 186 / 2,99 |

O shader aumenta o custo de renderização em relação ao caminho desligado. Na vila, o Equilibrado mediu 198 FPS, 1% low de 52 FPS e p95 de 9,42 ms. Desligado, foram 343 FPS, 88 FPS e 5,68 ms. O Alto registrou o maior p95 da matriz na vila, 12,77 ms. O Leve usa menos comparações e menor alcance. As medições curtas de uma rodada por perfil tiveram variação, e a ordem de FPS entre presets mudou conforme a cena. Os valores absolutos descrevem esta máquina, esta resolução e o mundo controlado.

Os registros finais usam `.logs/s0014-off-final.log` e `.logs/s0014-rp-{leve,equilibrado,alto}.log`. Cada perfil completou 13 cenários. `TOE_S0014_SHADOW` confirmou os alcances 44, 68 e 100 no carregamento efetivo do Oculus.

## Regressões e limites

O roteiro de parkour com o pack ativo passou pela corrida em oito direções, salto, deslizamento de parede e escalada de beirada. Registrou os clipes esperados com armadura e zero erros do renderer. O roteiro Slim confirmou composição Oculus, minério visível no túnel a 12%, restauração a 100%, armaduras, arco, combate e natação omnidirecional com armadura. A matriz também incluiu o caminho Classic.

A primeira execução Slim marcou `active_roll` como falha e o servidor não registrou o pedido nessa etapa. A rolagem contextual em combate e a pose R3 com armadura passaram na mesma execução. Repeti o roteiro com a S001.3 e com o ZIP final, a partir da mesma cópia de origem. Ambos registraram a segunda rolagem e passaram em `active_roll`. [Rolagem Slim com o ZIP final](assets/s001_4/active-roll-slim.png)

O comando padrão `.\gradlew runClient` sincronizou o ZIP final em `run/shaderpacks`, com SHA-256 igual ao original. O Oculus reconheceu o Alto já salvo, com zero opções extras, e a vila abriu com as sombras ativas. `.\gradlew runClient -PtoeNoOculus` também abriu a vila. [Vila no cliente principal](assets/s001_4/main-alto.png) · [Diagnóstico F3 no Alto](assets/s001_4/main-alto-debug.png) · [Vila sem Oculus](assets/s001_4/main-no-oculus.png)

Os registros da instância principal e das regressões estão em `.logs/s0014-main-final.log`, `.logs/s0014-no-oculus.log`, `.logs/s0014-parkour.log`, `.logs/s0014-qa-slim.log` e `.logs/s0014-roll-{baseline,repeat}.log`. A aprovação visual definitiva permanece com o usuário.

O shadow map representa oclusão solar por profundidade. Sombras coloridas através de vidro e emissão por material permanecem fora desta etapa. O próximo refinamento de água pode alinhar a iluminação e reflexão ao novo sol. O céu pode ganhar transições atmosféricas próprias e nuvens que acompanhem a iluminação do terreno.
