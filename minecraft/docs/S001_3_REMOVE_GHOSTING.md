# S001.3 — Nitidez sem rastros

## Causa

O passe `shaders/final.fsh` do pack original trabalhava com `colortex0`, o quadro atual. A inspeção não encontrou histórico de quadros, TAA, reprojeção, acumulação, jitter ou motion blur. O relógio de quadro na água anima as normais de sua ondulação e não alimenta a resolução da imagem.

| Efeito anterior | Presets | O que podia aparecer no quadro |
| --- | --- | --- |
| Antialiasing espacial | Leve, Equilibrado e Alto | O passe lia quatro vizinhos a 1 pixel e misturava o par escolhido com peso de até 32% em bordas. Ela suavizava e alargava o contorno. |
| Bloom por quatro amostras diagonais | Equilibrado e Alto | A cor de pontos claros aparecia em cópias deslocadas em 4 pixels no Equilibrado e 6 pixels no Alto, inclusive fora do objeto. A amostragem discreta desenhava contornos secundários. |
| Profundidade de campo | Desligada no padrão dos três. Podia ser ativada manualmente. | Misturava duas amostras a 2 pixels na diagonal ponderadas pela profundidade, com peso total de até 28%. |

O Equilibrado estava selecionado no `runClient`, com os valores do próprio pack e sem sobrescrita local de profundidade de campo. A duplicação vem de amostras espaciais no resolve final. Ao mover a câmera, as cópias acompanham as bordas, o que dá a impressão de rastro. A ponte da C002.2 compõe o personagem e a armadura antes desse resolve, então o pós-processamento afetava o personagem e a casa juntos.

## Correção

O passe final lê **um texel na mesma posição do quadro resolvido** e aplica a mesma exposição, saturação, contraste e intensidade mágica. Retirei o antialiasing espacial que misturava pixels adjacentes, o bloom de amostras discretas e a profundidade de campo. Também retirei seus controles e traduções. Valores antigos no arquivo de preferências do Oculus deixam de ter efeito. O resultado preserva bordas nítidas, com serrilhado nativo visível em algumas diagonais e detalhes distantes.

| Preset | Mapa de sombras | Suavização | Água | Resolve final |
| --- | ---: | ---: | --- | --- |
| Leve | 512² | 1 amostra | Cor e Fresnel | Gradação por pixel |
| Equilibrado | 1024² | 9 amostras | Ondulação e horizonte | Gradação por pixel |
| Alto | 2048² | 25 amostras | Ondulação e reflexão analítica do céu | Gradação por pixel |

A iluminação, sombras, céu e água continuam nos mesmos passes de mundo. A tarefa reaproveita o R3, as armaduras e as rotas C002.2 da S001.1. Os modos vanilla continuam separados da rota Oculus. Nenhuma opção cinematográfica foi adicionada.

## Arquivos

- `shaderpacks/Tower of Entropy - Fantasy Foundation/shaders/final.fsh`, `lib/options.glsl`, `shaders.properties` e traduções `lang/en_us.lang` e `lang/pt_br.lang`.
- `shaderpacks/Tower of Entropy - Fantasy Foundation.zip`. O `runClient` sincroniza o ZIP atualizado na instância principal.
- `validation/s001/forge.init.gradle` e `validation/s001/java/com/towerofentropy/client/qa/ShaderBenchmark.java`. O roteiro adicional só é selecionado por `-PtoeS0013Qa`.

## Validação visual

A comparação usa cópias do mesmo mundo com uma rua de vila de teste, casas com arestas retas e vegetação. Exercita terceira pessoa parada, caminhada, sprint, rotação rápida, troca de ombro, água, combate e túnel com diamante. A câmera, o modelo e as posições seguem o mesmo roteiro antes e depois. A referência usa o ZIP oficial anterior, guardado apenas nos perfis isolados de QA.

Na vila parada, a mesma fachada começa em x=490 na linha y=180. No Equilibrado anterior, o céu é uniforme até x=485 e ganha um contorno de quatro pixels a partir de x=486. Com a correção, o céu fica em RGB (158, 210, 249) até x=489 e a transição para a fachada ocorre em x=490. No Alto, a faixa de seis pixels entre x=484 e x=489 também desaparece na mesma linha. A borda fica legível mesmo com o personagem e as casas estáticos. A diferença de luminosidade nas áreas claras vem da retirada da contribuição do bloom.

- Equilibrado, vila parada. [Antes](assets/s001_3/before-equilibrado-village_still.png) · [Depois](assets/s001_3/after-equilibrado-village_still.png)
- Alto, terceira pessoa parada. [Antes](assets/s001_3/before-alto-village_still.png) · [Depois](assets/s001_3/after-alto-village_still.png)
- Alto, rotação rápida. [Antes](assets/s001_3/before-alto-village_turn.png) · [Depois](assets/s001_3/after-alto-village_turn.png)
- Alto, sprint. [Antes](assets/s001_3/before-alto-village_sprint.png) · [Depois](assets/s001_3/after-alto-village_sprint.png)
- Equilibrado, cenários adicionais. [Troca de ombro](assets/s001_3/after-equilibrado-village_shoulder.png) · [Combate](assets/s001_3/after-equilibrado-combat.png) · [Água](assets/s001_3/after-equilibrado-water.png) · [Túnel](assets/s001_3/after-equilibrado-camera_tunnel.png)

O Oculus confirmou as três seleções e as resoluções 512, 1024 e 2048 no log. Os três perfis completaram caminhada, sprint, rotação, ombro, combate e água. As capturas da rotação e do sprint mostram uma única silhueta da armadura e os contornos das casas sem as cópias da referência. No túnel, o bloco de diamante ficou legível por trás do personagem. O log registrou opacidade de 0,12, desenho de armadura R3 e composição via Oculus nos três presets.

Também executei Fabulous nativo, sem Oculus. O túnel manteve opacidade de 0,12 com 530 desenhos de armadura e 530 composições na janela medida. A captura mostra o minério legível. Fast e Fancy nativos já haviam sido validados na C002.2 e ficaram fora desta rodada. [Fabulous sem Oculus](assets/s001_3/after-fabulous-camera_tunnel.png)

O comando habitual `.\gradlew runClient` executou `prepareRunClientShaderpack` e carregou o ZIP atualizado de `run/shaderpacks`. A verificação com a mira visível foi feita na cópia de teste da instância principal, com Oculus e o shader ativos. [Túnel no runClient principal](assets/s001_3/main-runclient-tunnel.png)

## Desempenho

Teste integrado no Forge 1.20.1, Oculus 1.8.0, Embeddium 0.3.31, GTX 1050 Ti, 854 × 480. VSync desligado, limite configurado de 260 FPS, 48 ticks de aquecimento e 60 ticks medidos por cenário. Cada célula usa **FPS médio / 1% low em FPS / p95 em ms**, antes → depois. O 1% low é o inverso da média dos tempos do 1% mais lento.

| Cena | Leve | Equilibrado | Alto |
| --- | ---: | ---: | ---: |
| Vila parada | 112 / 34 / 15,05 → 135 / 57 / 12,35 | 112 / 48 / 16,83 → 129 / 57 / 12,82 | 116 / 47 / 17,14 → 125 / 50 / 13,74 |
| Caminhada | 157 / 71 / 9,98 → 194 / 105 / 7,39 | 182 / 92 / 8,20 → 173 / 99 / 8,06 | 182 / 83 / 7,81 → 189 / 108 / 7,36 |
| Sprint | 214 / 91 / 7,03 → 233 / 106 / 6,03 | 220 / 81 / 6,51 → 203 / 92 / 7,27 | 214 / 28 / 6,13 → 206 / 90 / 7,26 |
| Rotação rápida | 204 / 87 / 7,58 → 211 / 91 / 7,29 | 220 / 102 / 6,61 → 199 / 95 / 7,20 | 215 / 109 / 6,69 → 211 / 91 / 6,58 |
| Troca de ombro | 227 / 123 / 6,45 → 259 / 154 / 5,19 | 221 / 110 / 6,38 → 233 / 123 / 6,00 | 245 / 125 / 5,57 → 223 / 129 / 6,05 |
| Combate | 196 / 86 / 7,80 → 232 / 154 / 5,70 | 211 / 136 / 6,39 → 211 / 130 / 6,41 | 228 / 95 / 5,78 → 211 / 138 / 6,13 |
| Água | 303 / 131 / 5,18 → 370 / 167 / 4,16 | 414 / 220 / 3,37 → 397 / 171 / 3,92 | 436 / 174 / 3,61 → 419 / 182 / 3,85 |
| Túnel | 252 / 123 / 5,70 → 249 / 159 / 5,21 | 257 / 167 / 5,10 → 266 / 142 / 5,39 | 262 / 152 / 5,11 → 262 / 120 / 5,17 |

A vila parada melhorou nos três presets, inclusive no 1% low e no p95. No Equilibrado e no Alto, as demais cenas tiveram variações nos dois sentidos. O Leve melhorou o p95 nas oito cenas. A rodada tem uma execução por combinação e várias cenas são curtas, então a dispersão de uma execução inteira também afeta a comparação. No código, cada pixel do passe final passou de 5 para 1 leituras de cor no Leve e de 9 para 1 no Equilibrado e no Alto.

Os registros brutos ficam em `.logs/s0013-before-{leve,equilibrado,alto}.log` e `.logs/s0013-after-{leve,equilibrado,alto}.log`, com as métricas `TOE_S001_RESULT`.

## Limites

A rodada cobre a S001 no Overworld e os perfis indicados. A leitura é uma comparação do framebuffer do jogo neste equipamento e resolução. A captura de quadro não mede a persistência do monitor. A aprovação visual definitiva fica com o usuário.
