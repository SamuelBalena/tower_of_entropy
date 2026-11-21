# S001.7 — Materiais e LabPBR

## Auditoria do pipeline

A S001.6.2 somava ambiente, sol com visibilidade do shadow map e luz de bloco do lightmap. O produto pelo albedo em `lib/world.fsh` usava uma normal geométrica. O termo solar era difuso em toda a geometria. O passe de terreno já recebia `mc_Entity` para separar lava fonte e corrente. Consultei o carregador de formatos, `PBRType` e `NormalHelper` no JAR do Oculus 1.8.0. O carregador lê `assets/minecraft/optifine/texture.properties`. O Oculus fornece atlas auxiliares com o mesmo UV do albedo. O normal neutro do carregador é RGBA (127, 127, 255, 255), e o especular ausente é RGBA (0, 0, 0, 0). [Atlases](https://shaders.properties/current/reference/buffers/atlases/) · [Formatos PBR](https://shaders.properties/current/how-to/pbr_standards/)

A S001.7 separa os termos da luz de mundo em `lib/lighting.glsl` e introduz propriedades de superfície no programa de terreno. A função legada segue sendo usada por entidades, blocos renderizados como entidades e áreas fora das famílias sem dados de atlas. A água continua na passagem translúcida. A lava é resolvida antes dos materiais sólidos no mesmo passe. Vidro e gelo translúcido continuam no fluxo original. Gelo compacto e gelo azul podem receber o fallback de sólidos. Os render targets e a ordem dos passes seguem a S001.6.2.

## Material e iluminação

| Família | Resposta sem pack |
| --- | --- |
| Pedra, tufo, andesito, granito, diorito, basalto, ardósia e pedra-negra | Rugosidade elevada, reflexo discreto |
| Tijolos, prismarine e alvenaria do Nether | Reflexo amplo e fraco |
| Tábuas, toras, madeira descascada, cercas, portas, escadas e barris | F0 baixa e resposta mais suave que a pedra |
| Ferro, ouro e cobre | F0 por família, diferenças de rugosidade e contribuição aproximada do céu |
| Minérios vanilla | Matriz fosca no fallback. Mapas por texel podem separar cristais e rocha |
| Terra, areia, cascalho, neve, lã e terracota | Rugosidade alta e F0 baixa |
| Obsidiana e gelo sólido | Rugosidade intermediária, reflexo dielétrico contido |

Os IDs vêm de `block.properties`. O albedo segue amostrado no atlas original. O especular tem ângulo de visão, direção solar, Fresnel-Schlick e modulação pelo mesmo shadow map da iluminação difusa. O Leve usa Blinn com expoente dependente da rugosidade. Equilibrado e Alto usam distribuição GGX. O Alto calcula visibilidade de Smith correlacionada. O pico é limitado e parte da resposta difusa é reduzida à medida que cresce a parcela especular. A soma aproximada do céu para metais usa o canal de luz do céu. Reflete cor ambiente, sem reproduzir a geometria de cenas. A luz de tochas continua sendo a contribuição difusa da S001.4.

## LabPBR 1.3

O shader lê o vermelho de `_s` como smoothness perceptual e converte para rugosidade linear com o quadrado do complemento. O verde até 229 é F0 linear. Os metais 230–237 recebem F0 calculada a partir de n e k. Códigos restantes do intervalo metálico usam aproximação com albedo. O alpha de 0 a 254 descreve emissão de superfície, com 255 reservado. O fallback de Oculus impede distinguir um mapa RGBA totalmente zerado de uma entrada ausente. Porosidade, SSS e altura ficam fora desta etapa. [Padrão LabPBR 1.3](https://shaderlabs.org/wiki/LabPBR_Material_Standard)

No Equilibrado e Alto, o shader lê RG para X e Y, reconstitui Z, ortogonaliza a tangente do Oculus em relação à normal e usa a bitangente `cross(tangent, normal) * sign(w)` seguindo a orientação V do atlas. B da normal controla a oclusão do ambiente. O bias do shadow map continua tomando a direção geométrica da face. Superfícies sem mapas mantêm normais geométricas e famílias predefinidas. O carregamento dos mapas exige o formato 1.3 declarado e a opção LabPBR habilitada. [Atributo de tangente do Iris](https://shaders.properties/current/reference/attributes/at_tangent/)

## Pack de demonstração

O ZIP opcional `resourcepacks/Tower of Entropy - LabPBR Demo.zip` contém dez pares originais de mapas RGBA 16 × 16. Há altura e AO nas juntas e nos veios, roughness e F0 nos mapas especulares, códigos de ferro, ouro e cobre e máscaras de cristais para diamante e esmeralda. Os dois minérios da demonstração usam albedos originais alinhados às máscaras. Os outros oito exemplos preservam seus albedos do Minecraft. As texturas de terceiros e os arquivos vanilla extraídos ficam fora do pacote.

O `runClient` disponibiliza o ZIP em `run/resourcepacks`. Para ativá-lo, abra **Opções → Pacotes de recursos**, mova **Tower of Entropy - LabPBR Demo** para os selecionados e confirme. O Gradle copia o arquivo e o Minecraft conserva a seleção em suas próprias preferências. O menu **Materiais** do shader oferece qualidade, intensidade especular, intensidade de normal e a opção LabPBR. Qualidade **Desligada** roda a resposta anterior no mesmo shader.

## Entidades, equipamentos e limites

A renderização de pele e armadura R3 usa atlas e materiais próprios. A001 resolve modelos, tint, trims e glint separadamente, e `ContextualPlayerLayer` resolve captura e composição com alpha posteriormente. As propriedades LabPBR de terreno ficam restritas à sua passagem. A aplicação de mapas a armaduras exigiria um caminho compatível com a composição e com os materiais do R3. Essa é a próxima extensão recomendada. Os reflexos de metais representam o céu de forma aproximada. A emissão altera apenas a cor da superfície e não ilumina blocos vizinhos. O pack mantém os minérios sem emissão e esta rodada não incluiu um alvo emissivo dedicado.

## Arquivos

- GLSL em `gbuffers_terrain.*`, `lib/world.*`, `lib/lighting.glsl`, `lib/materials.glsl` e `lib/options.glsl`.
- IDs em `block.properties`, presets em `shaders.properties` e textos do menu em `lang/`.
- ZIP e pasta editável do shader, pack LabPBR original, tarefa de cópia do `runClient` e galeria em `validation/s001`.

## Capturas no Minecraft

Galeria com 15 cenas e a mesma posição, horário e geometria em cada coluna a 854 × 480. Leve e Alto também usam seus ajustes de sombras, água e lava. Em terceira pessoa, o roteiro mantém a armadura e 36 husks. Os mapas da demonstração mudam as juntas em alvenaria e a resposta do ferro e do ouro. Na caverna, os cristais acompanham os albedos originais do pack de demonstração. Em placas metálicas, o especular pode tornar o padrão do shadow map mais visível em certos ângulos.

| Cena | Desligado | S001.6.2 | Leve | Equilibrado | Alto | LabPBR Eq. |
| --- | --- | --- | --- | --- | --- | --- |
| Pedra | [Captura](assets/s001_7/stone-vanilla.png) | [Captura](assets/s001_7/stone-old.png) | [Captura](assets/s001_7/stone-leve.png) | [Captura](assets/s001_7/stone-eq.png) | [Captura](assets/s001_7/stone-alto.png) | [Captura](assets/s001_7/stone-pbr.png) |
| Tijolos | [Captura](assets/s001_7/brick-vanilla.png) | [Captura](assets/s001_7/brick-old.png) | [Captura](assets/s001_7/brick-leve.png) | [Captura](assets/s001_7/brick-eq.png) | [Captura](assets/s001_7/brick-alto.png) | [Captura](assets/s001_7/brick-pbr.png) |
| Madeira | [Captura](assets/s001_7/wood-vanilla.png) | [Captura](assets/s001_7/wood-old.png) | [Captura](assets/s001_7/wood-leve.png) | [Captura](assets/s001_7/wood-eq.png) | [Captura](assets/s001_7/wood-alto.png) | [Captura](assets/s001_7/wood-pbr.png) |
| Ferro e ouro | [Captura](assets/s001_7/metals-vanilla.png) | [Captura](assets/s001_7/metals-old.png) | [Captura](assets/s001_7/metals-leve.png) | [Captura](assets/s001_7/metals-eq.png) | [Captura](assets/s001_7/metals-alto.png) | [Captura](assets/s001_7/metals-pbr.png) |
| Cobre | [Captura](assets/s001_7/copper-vanilla.png) | [Captura](assets/s001_7/copper-old.png) | [Captura](assets/s001_7/copper-leve.png) | [Captura](assets/s001_7/copper-eq.png) | [Captura](assets/s001_7/copper-alto.png) | [Captura](assets/s001_7/copper-pbr.png) |
| Minérios | [Captura](assets/s001_7/ores-vanilla.png) | [Captura](assets/s001_7/ores-old.png) | [Captura](assets/s001_7/ores-leve.png) | [Captura](assets/s001_7/ores-eq.png) | [Captura](assets/s001_7/ores-alto.png) | [Captura](assets/s001_7/ores-pbr.png) |
| Gelo e neve | [Captura](assets/s001_7/ice-vanilla.png) | [Captura](assets/s001_7/ice-old.png) | [Captura](assets/s001_7/ice-leve.png) | [Captura](assets/s001_7/ice-eq.png) | [Captura](assets/s001_7/ice-alto.png) | [Captura](assets/s001_7/ice-pbr.png) |
| Chuva | [Captura](assets/s001_7/rain-vanilla.png) | [Captura](assets/s001_7/rain-old.png) | [Captura](assets/s001_7/rain-leve.png) | [Captura](assets/s001_7/rain-eq.png) | [Captura](assets/s001_7/rain-alto.png) | [Captura](assets/s001_7/rain-pbr.png) |
| Sol lateral | [Captura](assets/s001_7/rake-vanilla.png) | [Captura](assets/s001_7/rake-old.png) | [Captura](assets/s001_7/rake-leve.png) | [Captura](assets/s001_7/rake-eq.png) | [Captura](assets/s001_7/rake-alto.png) | [Captura](assets/s001_7/rake-pbr.png) |
| Entardecer | [Captura](assets/s001_7/dusk-vanilla.png) | [Captura](assets/s001_7/dusk-old.png) | [Captura](assets/s001_7/dusk-leve.png) | [Captura](assets/s001_7/dusk-eq.png) | [Captura](assets/s001_7/dusk-alto.png) | [Captura](assets/s001_7/dusk-pbr.png) |
| Tochas | [Captura](assets/s001_7/torch-vanilla.png) | [Captura](assets/s001_7/torch-old.png) | [Captura](assets/s001_7/torch-leve.png) | [Captura](assets/s001_7/torch-eq.png) | [Captura](assets/s001_7/torch-alto.png) | [Captura](assets/s001_7/torch-pbr.png) |
| Armadura e multidão | [Captura](assets/s001_7/armor-vanilla.png) | [Captura](assets/s001_7/armor-old.png) | [Captura](assets/s001_7/armor-leve.png) | [Captura](assets/s001_7/armor-eq.png) | [Captura](assets/s001_7/armor-alto.png) | [Captura](assets/s001_7/armor-pbr.png) |
| Mineração | [Captura](assets/s001_7/mining-vanilla.png) | [Captura](assets/s001_7/mining-old.png) | [Captura](assets/s001_7/mining-leve.png) | [Captura](assets/s001_7/mining-eq.png) | [Captura](assets/s001_7/mining-alto.png) | [Captura](assets/s001_7/mining-pbr.png) |
| Água | [Captura](assets/s001_7/water-vanilla.png) | [Captura](assets/s001_7/water-old.png) | [Captura](assets/s001_7/water-leve.png) | [Captura](assets/s001_7/water-eq.png) | [Captura](assets/s001_7/water-alto.png) | [Captura](assets/s001_7/water-pbr.png) |
| Lava | [Captura](assets/s001_7/lava-vanilla.png) | [Captura](assets/s001_7/lava-old.png) | [Captura](assets/s001_7/lava-leve.png) | [Captura](assets/s001_7/lava-eq.png) | [Captura](assets/s001_7/lava-alto.png) | [Captura](assets/s001_7/lava-pbr.png) |

## Desempenho

Cada célula registra FPS médio, 1% low, p95 e p99 em ms. GTX 1050 Ti, renderização a 8 chunks e VSync desligado. Foram 48 ticks de aquecimento e 60 de amostragem por cena, uma rodada por configuração. O servidor integrado pode influenciar o resultado. O pedido de 1920 × 1080 resultou em 1604 × 881 efetivos. A qualidade de materiais desligada usa as demais opções do Equilibrado.

### 854 × 480

| Cena | S001.6.2 | Leve | Equilibrado | Alto | LabPBR Eq. | Materiais desligados |
| --- | --- | --- | --- | --- | --- | --- |
| Madeira | 427 / 206 / 3,49 / 4,40 | 569 / 297 / 2,64 / 3,03 | 424 / 257 / 3,18 / 3,67 | 281 / 175 / 4,73 / 5,05 | 409 / 221 / 3,50 / 4,05 | 297 / 104 / 5,09 / 6,35 |
| Ferro e ouro | 428 / 185 / 3,30 / 3,73 | 578 / 329 / 2,61 / 2,86 | 424 / 265 / 2,94 / 3,50 | 279 / 186 / 4,58 / 5,02 | 411 / 219 / 3,25 / 3,55 | 375 / 148 / 4,40 / 6,06 |
| Sol lateral | 427 / 264 / 3,23 / 3,48 | 614 / 369 / 1,82 / 2,65 | 413 / 276 / 3,21 / 3,47 | 263 / 169 / 4,79 / 5,24 | 409 / 236 / 3,19 / 3,79 | 427 / 205 / 3,50 / 4,16 |
| Armadura e multidão | 203 / 131 / 6,11 / 7,22 | 208 / 89 / 6,00 / 6,56 | 205 / 105 / 5,95 / 6,63 | 217 / 154 / 5,76 / 6,21 | 189 / 38 / 6,91 / 8,62 | 198 / 89 / 6,58 / 7,87 |
| Água | 401 / 166 / 3,30 / 3,55 | 593 / 338 / 1,99 / 2,80 | 383 / 250 / 3,50 / 3,69 | 257 / 172 / 4,85 / 5,04 | 385 / 254 / 3,52 / 3,81 | 385 / 85 / 3,56 / 4,60 |

### 1280 × 720

| Cena | S001.6.2 | Leve | Equilibrado | Alto | LabPBR Eq. | Materiais desligados |
| --- | --- | --- | --- | --- | --- | --- |
| Madeira | 215 / 89 / 6,79 / 8,17 | 270 / 79 / 5,29 / 7,94 | 209 / 95 / 7,39 / 8,52 | 140 / 61 / 9,41 / 12,42 | 206 / 79 / 6,80 / 8,33 | 216 / 82 / 6,29 / 7,48 |
| Ferro e ouro | 214 / 74 / 7,27 / 9,06 | 271 / 119 / 5,51 / 7,21 | 205 / 85 / 7,17 / 9,56 | 138 / 77 / 9,24 / 10,99 | 205 / 131 / 6,61 / 7,06 | 216 / 129 / 6,28 / 7,15 |
| Sol lateral | 216 / 139 / 6,06 / 6,72 | 282 / 160 / 4,71 / 5,57 | 208 / 154 / 5,84 / 6,18 | 134 / 108 / 8,71 / 9,10 | 205 / 122 / 6,45 / 6,86 | 216 / 140 / 6,00 / 6,40 |
| Armadura e multidão | 185 / 77 / 7,37 / 9,89 | 179 / 100 / 7,75 / 9,53 | 173 / 54 / 7,70 / 9,51 | 135 / 98 / 9,29 / 10,04 | 189 / 116 / 7,03 / 8,10 | 191 / 129 / 6,61 / 7,32 |
| Água | 200 / 142 / 6,06 / 6,72 | 279 / 46 / 4,30 / 4,77 | 194 / 136 / 6,01 / 6,40 | 131 / 51 / 8,58 / 9,42 | 190 / 137 / 6,74 / 7,27 | 197 / 86 / 6,29 / 6,81 |

### 1604 × 881

| Cena | S001.6.2 | Leve | Equilibrado | Alto | LabPBR Eq. | Materiais desligados |
| --- | --- | --- | --- | --- | --- | --- |
| Madeira | 149 / 66 / 9,12 / 11,92 | 205 / 117 / 6,54 / 7,64 | 144 / 65 / 9,33 / 12,48 | 97 / 43 / 13,23 / 17,74 | 141 / 48 / 9,57 / 12,89 | 149 / 72 / 8,74 / 9,89 |
| Ferro e ouro | 148 / 97 / 8,89 / 10,01 | 202 / 98 / 6,37 / 7,64 | 143 / 94 / 9,32 / 10,21 | 95 / 65 / 12,52 / 13,62 | 141 / 72 / 8,85 / 10,93 | 148 / 80 / 8,76 / 10,15 |
| Sol lateral | 148 / 114 / 8,12 / 8,61 | 203 / 160 / 5,84 / 6,08 | 143 / 104 / 8,49 / 9,04 | 92 / 78 / 12,32 / 12,74 | 140 / 108 / 8,47 / 9,05 | 148 / 106 / 8,40 / 8,79 |
| Armadura e multidão | 142 / 100 / 9,02 / 9,69 | 188 / 122 / 6,53 / 7,29 | 137 / 102 / 8,99 / 9,62 | 94 / 76 / 12,36 / 13,06 | 134 / 53 / 8,97 / 11,93 | 139 / 46 / 9,05 / 11,53 |
| Água | 135 / 56 / 8,53 / 8,90 | 200 / 151 / 5,98 / 6,41 | 133 / 65 / 8,88 / 9,24 | 91 / 74 / 12,29 / 12,87 | 130 / 54 / 8,95 / 9,37 | 137 / 107 / 8,87 / 9,25 |

## Regressões

O build Forge terminou com sucesso. As cópias instaláveis foram verificadas byte a byte contra as pastas editáveis, com 38 entradas no shader e 24 no pack. Passaram os 19 ensaios da matriz em três resoluções. A imagem final exercitou água e lava ao lado dos sólidos, nuvens e os três perfis, com e sem o pack.

A regressão de parkour e combate aprovou 30 verificações. A regressão de câmera e armaduras aprovou 95 na repetição, incluindo Classic e Slim, acabamento, glint, roll, natação em oito direções e transição de 12% para 100% de opacidade. O roteiro registrou a composição pelo Oculus com a armadura.

O comando `.\gradlew runClient` sincronizou os dois ZIPs, entrou na galeria com Oculus e encerrou com sucesso. A variante `.\gradlew runClient -PtoeNoOculus` chegou ao menu Forge e também encerrou com sucesso. [Galeria no cliente padrão](assets/s001_7/main-gallery.png) · [Menu Forge sem Oculus](assets/s001_7/forge-native.png)

A aprovação visual definitiva fica com o usuário.
