# S001.5 — Céu, nuvens, sol e lua

## Arquitetura

Na S001.4, `gbuffers_skybasic` desenhava um gradiente simples e um halo de crepúsculo usando funções compartilhadas com a água. O horário vinha de uma senoide de `worldTime`, e as estrelas conservavam a geometria e a cor vanilla. Sem programas próprios para os passes de céu texturizado e nuvens, o Oculus ainda desenhava os discos quadrados do jogo e as nuvens em blocos. A iluminação dos blocos já usava o vetor solar real na S001.4. [Passes gbuffers do Iris](https://shaders.properties/current/reference/programs/gbuffers/)

A S001.5 reaproveita a geometria de fundo que o Oculus encaminha a `gbuffers_skybasic`. O fragmento transforma os vetores solares e lunares do espaço da câmera para o espaço do mundo e compõe gradiente, estrelas, discos e nuvens nessa ordem, atrás da geometria. As diretivas do pack desativam sol, lua e nuvens vanilla. O fragmento descarta os estágios de estrelas e de crepúsculo vanilla, inclusive no Oculus 1.8.0. A diretiva de estrelas da documentação atual do Iris não é reconhecida nessa versão do Oculus. O resolve final da S001.3 continua lendo um texel do quadro atual. [Diretivas de céu](https://shaders.properties/current/reference/shadersproperties/features/) · [Uniformes celestes](https://shaders.properties/current/reference/uniforms/world/)

## Céu e astros

- Horizonte, cúpula e zênite têm cores independentes. A altura solar controla o percurso entre dia, crepúsculo e noite. A componente leste–oeste inclina a paleta da manhã para rosa e violeta e a do fim da tarde para ouro e âmbar. A intensidade decai gradualmente durante o crepúsculo após o disco se pôr.
- O sol circular recebe um halo angular estreito e outro de baixa intensidade pela atmosfera. A cor muda durante a aproximação ao horizonte. O antialiasing se limita à borda analítica do disco.
- A lua prateada tem terminador por fase, duas regiões de albedo escuro e um halo frio. As estrelas são fixas em coordenadas angulares, com variação de tamanho e uma faixa difusa discreta. Nuvens ocluem parcialmente os astros. A chuva reduz a visibilidade dos discos e das estrelas.
- A cobertura das nuvens usa ruído procedural ancorado às coordenadas do mundo e deslocamento pelo vento. Espessura, face iluminada e cor do horário simulam relevo numa camada estilizada. O Alto adiciona uma segunda camada tênue. A camada principal fica atrás do terreno.

| Preset | Nuvens | Estrelas |
| --- | --- | --- |
| Leve | Cobertura e espessura a partir de três escalas de ruído | Uma distribuição |
| Equilibrado | Detalhe adicional e relevo orientado ao sol | Duas distribuições |
| Alto | Relevo do Equilibrado e camada alta translúcida | Duas distribuições |

A qualidade acompanha a resolução já gravada pelos três presets, preservando as opções salvas.

## Arquivos

- `gbuffers_skybasic.fsh` e `shaders.properties` na pasta editável.
- Novos módulos `lib/skydome.glsl`, `lib/cloudscape.glsl` e `lib/celestial.glsl`.
- ZIP Fantasy Foundation atualizado para sincronização pelo `runClient`.
- Roteiro opt-in `-PtoeS0015Qa` em `validation/s001/forge.init.gradle` e `validation/s001/java/com/towerofentropy/client/qa/ShaderBenchmark.java`.

## Capturas no Minecraft

O roteiro usa quatro cópias do mesmo mundo, com direção, posição, hora e clima fixos para cada cena. As sete capturas do ciclo usam 23600, 2300, 6000, 8800, 12400, 13000 e 14800 ticks do relógio do mundo. A fase registrada na tomada noturna é lua cheia. O vento usa o tempo do cliente, então a posição das nuvens pode variar ligeiramente entre inicializações.

| Cena | Desligado | Leve | Equilibrado | Alto |
| --- | --- | --- | --- | --- |
| Amanhecer na planície | [Captura](assets/s001_5/sunrise-off.png) | [Captura](assets/s001_5/sunrise-leve.png) | [Captura](assets/s001_5/sunrise-equilibrado.png) | [Captura](assets/s001_5/sunrise-alto.png) |
| Manhã | [Captura](assets/s001_5/morning-off.png) | [Captura](assets/s001_5/morning-leve.png) | [Captura](assets/s001_5/morning-equilibrado.png) | [Captura](assets/s001_5/morning-alto.png) |
| Meio-dia em campo | [Captura](assets/s001_5/noon-off.png) | [Captura](assets/s001_5/noon-leve.png) | [Captura](assets/s001_5/noon-equilibrado.png) | [Captura](assets/s001_5/noon-alto.png) |
| Tarde | [Captura](assets/s001_5/afternoon-off.png) | [Captura](assets/s001_5/afternoon-leve.png) | [Captura](assets/s001_5/afternoon-equilibrado.png) | [Captura](assets/s001_5/afternoon-alto.png) |
| Vila ao pôr do sol | [Captura](assets/s001_5/sunset-off.png) | [Captura](assets/s001_5/sunset-leve.png) | [Captura](assets/s001_5/sunset-equilibrado.png) | [Captura](assets/s001_5/sunset-alto.png) |
| Crepúsculo | [Captura](assets/s001_5/dusk-off.png) | [Captura](assets/s001_5/dusk-leve.png) | [Captura](assets/s001_5/dusk-equilibrado.png) | [Captura](assets/s001_5/dusk-alto.png) |
| Noite com lua | [Captura](assets/s001_5/night-off.png) | [Captura](assets/s001_5/night-leve.png) | [Captura](assets/s001_5/night-equilibrado.png) | [Captura](assets/s001_5/night-alto.png) |
| Floresta | [Captura](assets/s001_5/forest-off.png) | [Captura](assets/s001_5/forest-leve.png) | [Captura](assets/s001_5/forest-equilibrado.png) | [Captura](assets/s001_5/forest-alto.png) |
| Água ao fundo | [Captura](assets/s001_5/water-off.png) | [Captura](assets/s001_5/water-leve.png) | [Captura](assets/s001_5/water-equilibrado.png) | [Captura](assets/s001_5/water-alto.png) |

No Equilibrado, o disco solar e a faixa rosa aparecem na planície ao amanhecer. O fim de tarde enquadra o disco dourado entre telhados, com uma faixa que permanece quente no crepúsculo após o disco se pôr. Na noite, a lua circular aparece cercada por estrelas esparsas e nuvens frias. Nas tomadas com a mesma geometria, a S001.4 conserva os discos quadrados e o desenho em blocos das nuvens. [Amanhecer S001.4](assets/s001_5/sunrise-s0014.png) · [Entardecer S001.4](assets/s001_5/sunset-s0014.png) · [Noite S001.4](assets/s001_5/night-s0014.png)

## Desempenho

Minecraft 1.20.1, Forge 47.4.28, Oculus 1.8.0, Embeddium 0.3.31 e GTX 1050 Ti, a 854 × 480 e 8 chunks. VSync desligado, com 48 ticks de aquecimento e 60 ticks medidos por cena. As células mostram **FPS médio / 1% low em FPS / p95 em ms**. O 1% low é o inverso da média dos tempos do 1% de quadros mais lentos. A referência desligada usa o mesmo jogo modificado. A referência S001.4 usa o Equilibrado no mesmo roteiro para estimar o custo do novo céu.

| Cenário | Desligado | S001.4 Equilibrado | Leve | Equilibrado | Alto |
| --- | ---: | ---: | ---: | ---: | ---: |
| Amanhecer | 1026 / 210 / 1,60 | 301 / 91 / 5,73 | 364 / 132 / 4,85 | 275 / 84 / 6,62 | 280 / 96 / 6,30 |
| Manhã | 1425 / 343 / 1,03 | 455 / 177 / 3,44 | 500 / 152 / 3,57 | 582 / 211 / 3,06 | 477 / 185 / 3,45 |
| Meio-dia | 1594 / 389 / 0,81 | 637 / 253 / 2,48 | 588 / 171 / 3,38 | 626 / 230 / 2,76 | 648 / 267 / 2,64 |
| Tarde | 1560 / 367 / 0,85 | 715 / 287 / 2,23 | 713 / 242 / 2,41 | 775 / 322 / 1,94 | 736 / 279 / 2,11 |
| Pôr do sol | 1533 / 380 / 0,84 | 706 / 273 / 2,06 | 701 / 233 / 2,32 | 781 / 339 / 1,98 | 695 / 266 / 2,56 |
| Crepúsculo | 1525 / 378 / 0,86 | 468 / 220 / 3,35 | 526 / 213 / 3,16 | 479 / 184 / 3,30 | 340 / 94 / 5,23 |
| Noite | 1583 / 430 / 0,79 | 766 / 319 / 1,69 | 666 / 237 / 2,60 | 786 / 329 / 1,97 | 669 / 294 / 2,39 |
| Floresta | 1271 / 315 / 1,33 | 676 / 269 / 2,14 | 712 / 255 / 2,38 | 687 / 256 / 2,41 | 665 / 239 / 2,25 |
| Água ao fundo | 1528 / 489 / 0,89 | 651 / 248 / 2,37 | 771 / 335 / 1,92 | 728 / 313 / 2,24 | 677 / 288 / 2,48 |
| Armadura | 590 / 227 / 2,79 | 223 / 127 / 6,20 | 211 / 106 / 6,74 | 244 / 163 / 5,29 | 226 / 104 / 7,08 |
| Túnel | 732 / 278 / 2,25 | 266 / 97 / 5,24 | 254 / 77 / 5,88 | 284 / 162 / 4,78 | 287 / 172 / 4,51 |
| Saída do túnel | 686 / 236 / 2,56 | 260 / 154 / 5,25 | 238 / 155 / 5,39 | 221 / 117 / 6,71 | 271 / 171 / 4,81 |
| Combate | 604 / 277 / 2,43 | 223 / 119 / 6,25 | 208 / 125 / 6,43 | 220 / 124 / 5,83 | 234 / 114 / 5,77 |
| Submerso | 1473 / 475 / 0,94 | 651 / 141 / 2,09 | 599 / 222 / 3,06 | 692 / 294 / 2,28 | 689 / 323 / 2,06 |

A comparação Equilibrado → Equilibrado teve variação por cena. O p95 no amanhecer passou de 5,73 para 6,62 ms e na noite de 1,69 para 1,97 ms. No pôr do sol ficou em 1,98 ms ante 2,06 ms. O Alto registrou p95 de 5,23 ms no crepúsculo ante 3,30 ms no Equilibrado. Os valores incluem todo o quadro e vêm de uma rodada por configuração. O custo específico do passe de céu em tempo de GPU fica fora dessa medição.

Os 14 cenários por configuração estão nos registros `.logs/s0015-{final-off,s0014,v3-leve,v3-equilibrado,alto}.log`.

## Compatibilidade e limites

A matriz também inclui floresta com sombras, água, armadura, combate e túnel. Na floresta, as 19.950 amostras de cor do primeiro plano coincidiram entre a S001.4 e o Equilibrado final da S001.5. No Equilibrado final, a captura do jogador com armadura registrou uma composição Oculus por quadro a 12% no túnel e retorno a 100% em espaço aberto. No roteiro Slim, a transparência diante do minério, a composição Oculus, a rolagem ativa, a armadura, o combate e as oito direções de natação passaram. A suíte de parkour registrou salto, escalada de beirada e clipes com armadura, sem erros do renderer.

O comando `.\gradlew runClient` sincronizou o ZIP final byte a byte e carregou a vila com a configuração Alto salva. A execução `.\gradlew runClient -PtoeNoOculus` iniciou e entrou na cópia de teste. [Vila na instância principal, Alto](assets/s001_5/main-alto.png)

O roteiro isolado sem carregador também renderizou o amanhecer e terminou com sucesso. [Amanhecer sem Oculus](assets/s001_5/native-sunrise.png)

As nuvens simulam volume no céu. Sua cobertura ainda é uma camada remota e o terreno continua usando as sombras solares da S001.4. A altura aparente da camada é limitada para manter a composição legível também quando o jogador sobe. A próxima etapa da água pode reaproveitar a direção celeste e a paleta do horizonte nos reflexos. Nesta entrega a água mantém o caminho anterior.
