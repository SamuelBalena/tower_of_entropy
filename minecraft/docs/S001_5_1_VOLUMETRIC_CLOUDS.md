# S001.5.1 — Nuvens volumétricas

## Auditoria

Na S001.5, `gbuffers_skybasic` chamava `lib/skydome.glsl`, que desenhava o gradiente, as estrelas, o sol e a lua antes de misturar a cor de `lib/cloudscape.glsl`. Cada raio do céu cruzava uma altura aparente. Nesse ponto, ruído 2D gerava a silhueta e outro deslocamento simulava iluminação. O Alto acrescentava uma segunda máscara, amostrada no mesmo ponto. A câmera mudava a posição no plano, mas os contornos claros e escuros continuavam presos à mesma superfície. Isso explica o aspecto de imagem chapada.

## Nuvens da S001.5.1

O mesmo passe de céu agora integra várias amostras ao longo do trecho que cruza um volume entre Y=174 e Y=224. O campo procedural varia com X, Y e Z. Os lobos têm base larga, laterais e topo afunilados. O deslocamento horizontal muda com a altitude, criando paralaxe entre as partes da nuvem. As amostras próximas atenuam as distantes e os astros através de transmitância exponencial. [Referência conceitual sobre integração volumétrica e iluminação de nuvens](https://www.guerrilla-games.com/read/nubis-cubed)

A iluminação combina espessura, altitude e orientação da luz com as cores já usadas no céu. O sol introduz bordas quentes ao amanhecer e no pôr do sol. A noite usa uma faixa fria e mais escura. A amostragem é determinística no quadro atual. A composição termina no próprio passe de céu. Raios rasantes usam intervalos menores e as nuvens distantes desaparecem gradualmente até 830 blocos, reduzindo as faixas de amostragem perceptíveis no primeiro teste.

| Preset | Passos por raio | Sombreamento |
| --- | ---: | --- |
| Leve | 8 a 20 | Aproxima a espessura pelo nível de densidade local e pela altura |
| Equilibrado | 14 a 28 | Mede densidade em um ponto voltado à luz |
| Alto | 22 a 38 | Mede densidade em dois pontos ao longo da direção da luz |

Os nomes e as opções dos presets existentes foram reaproveitados. Foram alterados `lib/cloudscape.glsl`, a chamada em `lib/skydome.glsl` e o ZIP Fantasy Foundation. O sol, a lua, as estrelas e os gradientes continuam sendo compostos pela S001.5.

## Capturas no Minecraft

As capturas reutilizam o roteiro `-PtoeS0015Qa` com o mesmo mundo, câmera, clima e horário. As referências desligadas e S001.5 são os quadros originais desse roteiro. O vento se move no tempo de execução do cliente, o que pode deslocar levemente as formações entre sessões.

| Cena | Desligado | Leve | Equilibrado | Alto |
| --- | --- | --- | --- | --- |
| Amanhecer em planície | [Desligado](assets/s001_5/sunrise-off.png) | [Antes](assets/s001_5/sunrise-leve.png) · [Depois](assets/s001_5_1/sunrise-leve.png) | [Antes](assets/s001_5/sunrise-equilibrado.png) · [Depois](assets/s001_5_1/sunrise-equilibrado.png) | [Antes](assets/s001_5/sunrise-alto.png) · [Depois](assets/s001_5_1/sunrise-alto.png) |
| Meio-dia em campo | [Desligado](assets/s001_5/noon-off.png) | [Antes](assets/s001_5/noon-leve.png) · [Depois](assets/s001_5_1/noon-leve.png) | [Antes](assets/s001_5/noon-equilibrado.png) · [Depois](assets/s001_5_1/noon-equilibrado.png) | [Antes](assets/s001_5/noon-alto.png) · [Depois](assets/s001_5_1/noon-alto.png) |
| Vila ao pôr do sol | [Desligado](assets/s001_5/sunset-off.png) | [Antes](assets/s001_5/sunset-leve.png) · [Depois](assets/s001_5_1/sunset-leve.png) | [Antes](assets/s001_5/sunset-equilibrado.png) · [Depois](assets/s001_5_1/sunset-equilibrado.png) | [Antes](assets/s001_5/sunset-alto.png) · [Depois](assets/s001_5_1/sunset-alto.png) |
| Lua e nuvens | [Desligado](assets/s001_5/night-off.png) | [Antes](assets/s001_5/night-leve.png) · [Depois](assets/s001_5_1/night-leve.png) | [Antes](assets/s001_5/night-equilibrado.png) · [Depois](assets/s001_5_1/night-equilibrado.png) | [Antes](assets/s001_5/night-alto.png) · [Depois](assets/s001_5_1/night-alto.png) |
| Horizonte amplo | [Desligado](assets/s001_5/water-off.png) | [Antes](assets/s001_5/water-leve.png) · [Depois](assets/s001_5_1/water-leve.png) | [Antes](assets/s001_5/water-equilibrado.png) · [Depois](assets/s001_5_1/water-equilibrado.png) | [Antes](assets/s001_5/water-alto.png) · [Depois](assets/s001_5_1/water-alto.png) |
| Crepúsculo | [Desligado](assets/s001_5/dusk-off.png) | [Antes](assets/s001_5/dusk-leve.png) · [Depois](assets/s001_5_1/dusk-leve.png) | [Antes](assets/s001_5/dusk-equilibrado.png) · [Depois](assets/s001_5_1/dusk-equilibrado.png) | [Antes](assets/s001_5/dusk-alto.png) · [Depois](assets/s001_5_1/dusk-alto.png) |

No pôr do sol, as bordas alaranjadas passam sobre bases sombreadas, e o disco solar continua legível junto à borda da nuvem. No meio-dia, formações mais próximas ocupam uma área maior e as distantes perdem contraste. A lua cheia aparece através de uma formação fina. Faixas discretas ainda aparecem em algumas formações claras do Leve e do Equilibrado. O Alto suaviza essas faixas com maior densidade de amostras e acrescenta contraste nas bases.

## Desempenho

Minecraft 1.20.1, Forge 47.4.28, Oculus 1.8.0, Embeddium 0.3.31 e GTX 1050 Ti, a 854 × 480 e 8 chunks. VSync desligado, com 48 ticks de aquecimento e 60 ticks medidos por cena. Cada célula contém **FPS médio / 1% low em FPS / p95 em ms**, S001.5 → S001.5.1. O 1% low é o inverso da média dos tempos do 1% de quadros mais lentos. O comparativo reaproveita as medições da S001.5 feitas nessa máquina com o mesmo roteiro.

| Cena | Leve | Equilibrado | Alto |
| --- | ---: | ---: | ---: |
| Amanhecer | 364 / 132 / 4,85 → 280 / 84 / 6,12 | 275 / 84 / 6,62 → 345 / 129 / 5,06 | 280 / 96 / 6,30 → 251 / 92 / 7,39 |
| Manhã | 500 / 152 / 3,57 → 419 / 159 / 4,08 | 582 / 211 / 3,06 → 363 / 177 / 4,53 | 477 / 185 / 3,45 → 211 / 116 / 7,14 |
| Meio-dia | 588 / 171 / 3,38 → 524 / 196 / 2,93 | 626 / 230 / 2,76 → 382 / 200 / 4,01 | 648 / 267 / 2,64 → 230 / 136 / 6,52 |
| Tarde | 713 / 242 / 2,41 → 548 / 261 / 2,70 | 775 / 322 / 1,94 → 382 / 195 / 3,95 | 736 / 279 / 2,11 → 229 / 137 / 6,39 |
| Pôr do sol | 701 / 233 / 2,32 → 603 / 278 / 2,41 | 781 / 339 / 1,98 → 450 / 213 / 3,02 | 695 / 266 / 2,56 → 280 / 145 / 5,58 |
| Crepúsculo | 526 / 213 / 3,16 → 538 / 224 / 3,02 | 479 / 184 / 3,30 → 386 / 189 / 3,83 | 340 / 94 / 5,23 → 215 / 131 / 6,76 |
| Noite | 666 / 237 / 2,60 → 494 / 235 / 2,86 | 786 / 329 / 1,97 → 308 / 172 / 4,50 | 669 / 294 / 2,39 → 180 / 106 / 7,27 |
| Floresta | 712 / 255 / 2,38 → 588 / 240 / 2,40 | 687 / 256 / 2,41 → 490 / 220 / 3,28 | 665 / 239 / 2,25 → 366 / 206 / 3,92 |
| Horizonte com água | 771 / 335 / 1,92 → 681 / 262 / 2,07 | 728 / 313 / 2,24 → 587 / 290 / 2,31 | 677 / 288 / 2,48 → 408 / 215 / 3,50 |
| Armadura | 211 / 106 / 6,74 → 253 / 144 / 5,56 | 244 / 163 / 5,29 → 252 / 162 / 5,03 | 226 / 104 / 7,08 → 239 / 152 / 5,61 |
| Túnel | 254 / 77 / 5,88 → 293 / 147 / 4,94 | 284 / 162 / 4,78 → 311 / 184 / 4,22 | 287 / 172 / 4,51 → 282 / 179 / 4,71 |
| Saída do túnel | 238 / 155 / 5,39 → 272 / 142 / 5,29 | 221 / 117 / 6,71 → 249 / 138 / 5,68 | 271 / 171 / 4,81 → 241 / 121 / 5,64 |
| Combate | 208 / 125 / 6,43 → 243 / 146 / 5,70 | 220 / 124 / 5,83 → 236 / 114 / 5,29 | 234 / 114 / 5,77 → 207 / 92 / 6,84 |
| Submerso | 599 / 222 / 3,06 → 640 / 149 / 2,27 | 692 / 294 / 2,28 → 564 / 251 / 2,87 | 689 / 323 / 2,06 → 423 / 249 / 3,43 |

O custo aumentou principalmente quando o céu ocupa grande parte do quadro. No pôr do sol, o Equilibrado passou de 781 para 450 FPS e de 1,98 para 3,02 ms no p95. O Alto passou de 695 para 280 FPS e de 2,56 para 5,58 ms. Na noite do Alto, o p95 passou de 2,39 para 7,27 ms. O Leve teve o menor aumento de p95 no pôr do sol, de 2,32 para 2,41 ms. As diferenças em cenas dominadas pelo primeiro plano incluem a variabilidade de uma rodada por configuração. Trata-se de tempo do quadro inteiro.

Registros da S001.5 em `.logs/s0015-{v3-leve,v3-equilibrado,alto}.log`. Novos registros em `.logs/s00151-{leve,equilibrado-final,alto}.log`. Os testes integrados ficam em `.logs/s00151-{parkour,qa-slim,main,native}.log`.

## Compatibilidade e limites

O roteiro Slim confirmou armadura, combate, rolagem ativa, composição da transparência a 12% no túnel, restauração a 100% e natação nas oito direções. O roteiro de parkour passou com salto, deslizamento, escalada e clipes com armadura, com zero erros do renderer. Na floresta do Alto, o primeiro plano coincidiu em 19.950 de 19.950 amostras RGB com a S001.5.

O comando `.\gradlew runClient` sincronizou o ZIP final byte a byte e abriu a vila com o preset Alto previamente salvo. [Vila na instância principal](assets/s001_5_1/main-alto.png)

O roteiro `runClient -PtoeNoOculus` também renderizou o amanhecer e terminou com sucesso. [Amanhecer sem Oculus](assets/s001_5_1/native-sunrise.png)

O volume fica no passe de fundo. Colisões com nuvens, interseções volumétricas com montanhas e sombras projetadas pelas nuvens no terreno ficam fora da cobertura desta etapa. A próxima etapa da água pode reutilizar a composição direcional de céu e nuvens nos reflexos, com um orçamento de amostras próprio.
