# C002.1 — Transparência contextual e natação em oito direções

## Transparência

A C002 levava a câmera para uma posição segura em corredores e sob tetos baixos. Com o olho da câmera perto do Steve, a projeção da cabeça e do tronco passava a ocupar boa parte do quadro, inclusive a região central da mira.

A C002.1 usa a fração de deslocamento seguro que a câmera já calcula e a distância efetiva ao olho do jogador, ajustada pelo FOV como aproximação do enquadramento. Zoom manual e aproximação de COMBAT em espaço livre mantêm a opacidade total. A transição começa quando a câmera está retraída por colisão e o tamanho projetado ultrapassa o limite confortável. A curva cúbica suave vai de 100% na distância equivalente de 2,35 blocos a 12% em 0,95 bloco. A aproximação visual usa amortecimento exponencial de 65 ms e a recuperação usa 140 ms. Os parâmetros estão em `[characterFade]` no arquivo `config/towerofentropy-camera-client.toml`. A câmera conserva preferência de zoom, ombro e perfil de colisão.

O despachante Forge encaminha a renderização já existente do jogador local para um alvo de cor e profundidade reutilizável durante a retração. O R3 continua enviando seus vértices deformados, materiais, skin e acessórios pelo mesmo caminho. A profundidade interna seleciona as superfícies visíveis antes da composição, de modo que camadas sobrepostas do corpo não acumulem a opacidade global. O shader mistura a camada uma vez com o cenário e testa a profundidade da camada contra a cena, sem escrever essa profundidade no mundo. O shader é registrado na recarga de recursos. O alvo é redimensionado junto com a janela e liberado na saída do mundo. A distância aberta usa o caminho opaco direto.

Espadas na mão ou nas costas, arcos e objetos usam o buffer do próprio desenho. A armadura usa o fallback que o projeto já aplica para equipamento ainda sem rig R3. Suas camadas continuam no mesmo desenho e recebem a composição global. A A001 segue como a etapa de rig de armaduras. Outros jogadores mantêm seus buffers normais. A transparência é local e não altera entidades, hitboxes, aggro, dano nem pacotes.

### Capturas e custo

No túnel da fixture, o Classic atingiu 12% de opacidade. A captura com fade revela a textura e o contorno de seleção do minério no centro. Na mesma posição, a captura com opacidade total mostra a cabeça sobre esse trecho. Na região central de 121 × 125 pixels, uma contagem simples de tons ciano passou de 0 para 3.069 pixels. A captura separada do jogador remoto simulado mantém sua silhueta opaca enquanto o jogador local aparece transparente.

Em espaço livre e com zoom manual de dois blocos, a opacidade ficou em 100% e a contagem de composições ficou em zero. No corredor estreito da fixture, a opacidade estabilizou em 15,2%. Após remover o teto do túnel, a primeira amostra registrou cerca de 33% e a câmera recuperou 100%. O ensaio em COMBAT registrou impacto do ataque e Roll concluído durante a cena de aproximação. Espadas na mão e nas costas, arco e capacete foram exercitados. Classic e Slim usaram skins dinâmicas personalizadas, com o desenho R3 mantido para corpo e acessórios compatíveis.

| Cena Classic | Quadro médio | CPU do desenho isolado | CPU da composição |
| --- | ---: | ---: | ---: |
| Espaço livre | 16,709 ms | — | — |
| Corredor | 16,702 ms | 0,869 ms | 0,048 ms |
| Túnel a 12% | 16,719 ms | 0,748 ms | 0,038 ms |
| COMBAT sob teto | 16,724 ms | 0,802 ms | 0,031 ms |
| Deslocamento rápido | 16,741 ms | 0,705 ms | 0,026 ms |

O desenho isolado inclui a renderização do personagem e o envio dos buffers. A coluna de composição mede CPU, não o tempo da GPU. O teste usou Forge, 854 × 480, modo Detalhado e VSync a 60 FPS. Em cada cena foram amostrados cerca de 109–110 intervalos. As cenas com R3 ativo registraram uma composição por desenho de personagem quando o fade estava ativo. Essas amostras não estimam a capacidade de FPS acima de 60.

Capturas do Classic em `.logs/c0021-visual/screenshots`, incluindo `c0021-ore_opaque.png`, `c0021-ore_fade.png` e `c0021-remote_opaque.png`. O reteste isolou a captura do minério da captura com o outro jogador e preservou os registros históricos.

## Natação

Os predicados vanilla de começo e continuidade de Sprint privilegiavam impulso para frente. A C002.1 aceita impulso horizontal lateral, traseiro ou diagonal no começo da natação com Sprint quando o jogador está submerso e preserva a continuidade na água. As outras condições da natação continuam a cargo de Minecraft. A física e a normalização de WASD continuam no movimento relativo vanilla, inclusive as diagonais. Subir, mergulhar e a resposta ao ângulo de visão seguem o mesmo controlador vertical.

O R3 usa o deslocamento horizontal no referencial já usado em terra para determinar o yaw e amortece mudanças de direção. Pequenos deslocamentos horizontais, impulso WASD e ações verticais de subir ou descer distinguem as poses de movimento e repouso. A flutuação ou o afundamento passivo mantêm a pose de repouso. Os clips reais são `UAL_swim` para todas as direções e `UAL_swim_idle` para repouso. A distinção visual entre as oito direções vem da orientação e da transição da pose compartilhada.

Classic e Slim passaram individualmente em frente, frente-esquerda, esquerda, trás-esquerda, trás, trás-direita, direita e frente-direita. Cada direção foi exercitada nos dois ombros entre os ensaios. A média diagonal ficou em 102,3% da média cardinal no Classic e 102,0% no Slim. Os dois ensaios também passaram na inversão rápida, superfície, subida, descida, repouso com `UAL_swim_idle` e retorno ao chão.

Logs finais em `.logs/c0021-visual.log` e `.logs/c0021-slim-final.log`. Cada perfil passou em 27/27 checagens. As capturas de natação ficam junto às capturas de transparência em `.logs/c0021-visual/screenshots` e `.logs/c0021-slim-final/screenshots`.

## Arquivos e validação

- `client/camera/CharacterOpacity.java`, `CameraSettings.java` e `ShoulderCamera.java` resolvem a opacidade com os dados da C002.
- `client/ContextualPlayerLayer.java`, `coremods/player_fade.js`, `META-INF/coremods.json` e os recursos do shader `player_fade` integram a composição ao despachante.
- `client/DirectionalSprint.java`, `HorizontalLocomotion.java`, `VisualContext.java` e `PlayerRendererR3.java` tratam os predicados aquáticos e as poses.
- `build.gradle` e `validation/w002/java/com/towerofentropy/client/camera/C0021RuntimeTest.java` integram a fixture em perfis isolados.

- As suítes locais passaram para movimento horizontal (43), locomoção, arbitragem de ações, combate (60) e Roll (104).
- A regressão de câmera C002 passou em 26/26. Classic e Slim passaram em 59/59 cada na regressão W002. Os ensaios exercitaram zoom COMBAT, mira, Roll, arma guardada e os comandos de diagnóstico de desenvolvimento. Logs em `.logs/c0021-camera.log`, `.logs/c0021-combat-classic.log` e `.logs/c0021-combat-slim.log`.
- A regressão Slim concluiu W001 com 50/50, W001.1/C001.3 com 36/36 e expansão e parkour com 29/29. Log em `.logs/c0021-regression.log`.
- Build Forge 1.20.1 (47.4.28) com JDK 17 e Gradle offline. O JAR inclui o compositor e os shaders. A auditoria conferiu 16/16 arquivos aprovados de modelo ou pose e zero fixtures de QA.

JAR Forge em `build/libs/towerofentropy-0.0.1.jar`. SHA-256 `74396725B5184F70DAD9A4E4E6257B66EB890D4DBE6539CB918CC16A5B617F5C`.

## Limitações

A composição foi direcionada aos modos Rápido e Detalhado do Forge com o alvo de renderização principal. O modo Fabulous conserva a renderização opaca, pois usa alvos de transparência separados para itens e partículas. Shaders externos, ordenação com água ou vidro e sessões multiplayer com dois clientes precisam de validação específica. Sombra e fogo emitidos separadamente pelo despachante conservam o comportamento vanilla. O ensaio de um segundo jogador usa uma entidade remota visível no cliente.
