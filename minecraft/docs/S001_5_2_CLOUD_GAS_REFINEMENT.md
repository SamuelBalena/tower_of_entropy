# S001.5.2 — Nuvens mais vaporosas

## Auditoria

Na S001.5.1, `lib/cloudscape.glsl` integrava o volume entre Y=174 e Y=224 no passe de céu. A posição no mundo e o deslocamento com a altura criavam paralaxe. Dois ruídos, uma envoltória vertical simétrica e transições curtas nas extremidades definiam a densidade. A espessura de 50 blocos e a extinção diurna de 0,083 acumulavam opacidade rapidamente. O sombreamento direcional aprofundava a base. Juntos, esses parâmetros produziam miolos uniformes e bordas com silhuetas compactas.

## Refinamento

A faixa do volume agora vai de Y=181 a Y=222. O ruído de menor escala muda com a altura, erode as bordas e desloca localmente o perfil vertical. O miolo e a franja usam curvas de densidade distintas. A franja é rarefeita e desaparece gradualmente nas extremidades. A menor extinção preserva parte da luz de fundo. A base recebeu uma cor ambiente mais clara e o auto-sombreamento perdeu intensidade. O auto-sombreamento detalhado entra gradualmente nas regiões mais densas, poupando as amostras de luz na franja. A iluminação preserva tons quentes de crepúsculo e tons frios à noite.

| Preset | Passos por raio | Sombreamento |
| --- | ---: | --- |
| Leve | 8 a 20 | Aproximação com densidade e altura locais |
| Equilibrado | 14 a 28 | Uma amostra na direção da luz |
| Alto | 22 a 38 | Duas amostras na direção da luz |

O percurso volumétrico, a paralaxe e a transmitância continuam sendo calculados no quadro atual. Os arquivos de entrega são `shaders/lib/cloudscape.glsl`, o ZIP Fantasy Foundation, este relatório e as capturas em `docs/assets/s001_5_2`.

## Capturas no Minecraft

O roteiro `-PtoeS0015Qa` usa cópias da mesma área de teste, com posição da câmera, horário e clima fixos. As referências desligadas e da S001.5.1 foram reaproveitadas dos testes anteriores desse roteiro. A deriva das nuvens usa o tempo do cliente e pode alterar ligeiramente a posição de cada formação.

| Cena | Shader desligado | Leve | Equilibrado | Alto |
| --- | --- | --- | --- | --- |
| Amanhecer | [Desligado](assets/s001_5/sunrise-off.png) | [Antes](assets/s001_5_1/sunrise-leve.png) · [Depois](assets/s001_5_2/sunrise-leve.png) | [Antes](assets/s001_5_1/sunrise-equilibrado.png) · [Depois](assets/s001_5_2/sunrise-equilibrado.png) | [Antes](assets/s001_5_1/sunrise-alto.png) · [Depois](assets/s001_5_2/sunrise-alto.png) |
| Meio-dia | [Desligado](assets/s001_5/noon-off.png) | [Antes](assets/s001_5_1/noon-leve.png) · [Depois](assets/s001_5_2/noon-leve.png) | [Antes](assets/s001_5_1/noon-equilibrado.png) · [Depois](assets/s001_5_2/noon-equilibrado.png) | [Antes](assets/s001_5_1/noon-alto.png) · [Depois](assets/s001_5_2/noon-alto.png) |
| Pôr do sol | [Desligado](assets/s001_5/sunset-off.png) | [Antes](assets/s001_5_1/sunset-leve.png) · [Depois](assets/s001_5_2/sunset-leve.png) | [Antes](assets/s001_5_1/sunset-equilibrado.png) · [Depois](assets/s001_5_2/sunset-equilibrado.png) | [Antes](assets/s001_5_1/sunset-alto.png) · [Depois](assets/s001_5_2/sunset-alto.png) |
| Lua e nuvens | [Desligado](assets/s001_5/night-off.png) | [Antes](assets/s001_5_1/night-leve.png) · [Depois](assets/s001_5_2/night-leve.png) | [Antes](assets/s001_5_1/night-equilibrado.png) · [Depois](assets/s001_5_2/night-equilibrado.png) | [Antes](assets/s001_5_1/night-alto.png) · [Depois](assets/s001_5_2/night-alto.png) |
| Horizonte amplo | [Desligado](assets/s001_5/afternoon-off.png) | [Antes](assets/s001_5_2/afternoon-leve-before.png) · [Depois](assets/s001_5_2/afternoon-leve.png) | [Antes](assets/s001_5_2/afternoon-equilibrado-before.png) · [Depois](assets/s001_5_2/afternoon-equilibrado.png) | [Antes](assets/s001_5_2/afternoon-alto-before.png) · [Depois](assets/s001_5_2/afternoon-alto.png) |
| Horizonte com água | [Desligado](assets/s001_5/water-off.png) | [Antes](assets/s001_5_1/water-leve.png) · [Depois](assets/s001_5_2/water-leve.png) | [Antes](assets/s001_5_1/water-equilibrado.png) · [Depois](assets/s001_5_2/water-equilibrado.png) | [Antes](assets/s001_5_1/water-alto.png) · [Depois](assets/s001_5_2/water-alto.png) |

As capturas mostram franjas mais largas e transparentes e bases menos escuras. As nuvens distantes formam faixas finas no horizonte. No Alto, o pixel central da lua em (427, 244) passou de RGB (138, 151, 179) na S001.5.1 para (200, 210, 225) na S001.5.2. A lua continua na mesma posição. A diferença é compatível com a menor extinção no raio de visão, incluindo a deriva natural das formações entre sessões.

## Desempenho

Minecraft 1.20.1, Forge 47.4.28, Oculus 1.8.0, Embeddium 0.3.31 e GTX 1050 Ti, a 854 × 480 e 8 chunks. VSync desligado, 48 ticks de aquecimento e 60 de medição por cenário. Cada célula usa **FPS médio / 1% low em FPS / p95 em ms**, S001.5.1 → S001.5.2. O 1% low é o inverso da média dos tempos do 1% de quadros mais lentos. Foi medida uma rodada por combinação, com tempo de quadro total.

| Cena | Leve | Equilibrado | Alto |
| --- | ---: | ---: | ---: |
| Amanhecer | 280 / 84 / 6,12 → 384 / 143 / 4,42 | 345 / 129 / 5,06 → 360 / 127 / 4,68 | 251 / 92 / 7,39 → 258 / 143 / 6,18 |
| Manhã | 419 / 159 / 4,08 → 507 / 188 / 3,00 | 363 / 177 / 4,53 → 333 / 176 / 4,63 | 211 / 116 / 7,14 → 202 / 135 / 6,67 |
| Meio-dia | 524 / 196 / 2,93 → 519 / 255 / 2,78 | 382 / 200 / 4,01 → 345 / 185 / 4,39 | 230 / 136 / 6,52 → 220 / 133 / 5,96 |
| Tarde | 548 / 261 / 2,70 → 552 / 256 / 2,19 | 382 / 195 / 3,95 → 355 / 219 / 4,19 | 229 / 137 / 6,39 → 220 / 149 / 6,26 |
| Pôr do sol | 603 / 278 / 2,41 → 599 / 289 / 2,11 | 450 / 213 / 3,02 → 405 / 235 / 3,77 | 280 / 145 / 5,58 → 235 / 159 / 5,63 |
| Crepúsculo | 538 / 224 / 3,02 → 546 / 248 / 2,84 | 386 / 189 / 3,83 → 377 / 222 / 3,99 | 215 / 131 / 6,76 → 207 / 121 / 6,36 |
| Noite | 494 / 235 / 2,86 → 465 / 229 / 2,84 | 308 / 172 / 4,50 → 270 / 183 / 4,95 | 180 / 106 / 7,27 → 163 / 89 / 7,87 |
| Floresta | 588 / 240 / 2,40 → 586 / 291 / 2,25 | 490 / 220 / 3,28 → 519 / 248 / 2,67 | 366 / 206 / 3,92 → 365 / 202 / 4,00 |
| Horizonte com água | 681 / 262 / 2,07 → 714 / 332 / 1,86 | 587 / 290 / 2,31 → 562 / 258 / 2,85 | 408 / 215 / 3,50 → 399 / 163 / 3,69 |
| Armadura | 253 / 144 / 5,56 → 254 / 144 / 5,17 | 252 / 162 / 5,03 → 230 / 76 / 6,58 | 239 / 152 / 5,61 → 215 / 30 / 6,98 |
| Túnel | 293 / 147 / 4,94 → 308 / 109 / 4,36 | 311 / 184 / 4,22 → 316 / 186 / 4,18 | 282 / 179 / 4,71 → 308 / 174 / 4,49 |
| Saída do túnel | 272 / 142 / 5,29 → 274 / 173 / 4,79 | 249 / 138 / 5,68 → 255 / 128 / 5,14 | 241 / 121 / 5,64 → 261 / 168 / 5,13 |
| Combate | 243 / 146 / 5,70 → 229 / 85 / 5,74 | 236 / 114 / 5,29 → 241 / 155 / 5,31 | 207 / 92 / 6,84 → 221 / 110 / 6,26 |
| Submerso | 640 / 149 / 2,27 → 709 / 309 / 1,69 | 564 / 251 / 2,87 → 590 / 312 / 2,14 | 423 / 249 / 3,43 → 412 / 228 / 3,56 |

O custo variou por cena e preset. No pôr do sol, o p95 passou de 2,41 para 2,11 ms no Leve, de 3,02 para 3,77 ms no Equilibrado e de 5,58 para 5,63 ms no Alto. Na noite, o p95 subiu de 4,50 para 4,95 ms no Equilibrado e de 7,27 para 7,87 ms no Alto. As franjas translúcidas deixam mais raios percorrerem o volume inteiro, mesmo com a omissão de amostras de sombra nas regiões rarefeitas. A comparação mede o quadro inteiro e tem uma rodada por configuração.

Os registros anteriores estão em `.logs/s00151-{leve,equilibrado-final,alto}.log`. Os registros novos estão em `.logs/s00152-{leve,equilibrado-final,alto,native,parkour,qa-slim,main}.log`.

## Compatibilidade e limites

O roteiro isolado `runClient -PtoeNoOculus` renderizou o amanhecer e terminou com sucesso. [Amanhecer sem Oculus](assets/s001_5_2/native-sunrise.png)

`.\gradlew runClient` sincronizou o ZIP final byte a byte e abriu a vila de teste. Os roteiros integrados passaram em armadura A001, combate, rolagem, parkour e natação em oito direções. A câmera registrou 12% no túnel e voltou a 100% na saída, com composição Oculus e Polymesh R3. [Vila no runClient](assets/s001_5_2/main.png)

O volume continua no passe de fundo. A água pode reutilizar sua composição direcional em reflexos com um orçamento de amostras próprio. As sombras projetadas no terreno continuam a cargo do passe solar da S001.4.

## Publicação

Branch `main`, no remoto `origin`. Commit da implementação `c386cbc20dd7a7da42806f8092ef93a7eb283fa8`.
