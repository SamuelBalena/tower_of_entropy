# Salto em oito direções e Sprint Jump

19 de novembro de 2026

## Padrão visual

**ORIGINAL** continua selecionado na inicialização e volta a ser selecionado ao desconectar. Não existe configuração persistida de perfil. As animações em uso são `UAL_wall_slide`, `UAL_ledge_hang` e `UAL_ledge_climb`. Os derivados de `Push_Loop` e `ClimbUp_1m` continuam empacotados para comparação manual. Os comandos de perfil continuam locais ao cliente.

## Orientação do salto

A locomoção já media o deslocamento horizontal, mas o renderer só aplicava essa orientação a Idle, Walk, Jog e Sprint. Os clipes aéreos usavam o yaw de corpo vanilla, ligado à direção em que Steve estava olhando.

O Polymesh R3 agora aplica o yaw do deslocamento real em `UAL_jump_start`, `UAL_jump_loop` e `UAL_jump_land`, com a mesma transformação de raiz usada no chão. A orientação é interpolada no menor arco e limitada a 35° por tick durante o salto e nos primeiros quatro ticks após aterrissar. Com deslocamento abaixo de 0,025 bloco/tick, mantém-se a última direção para que a passagem por velocidade zero não cause um giro. O cálculo observa o movimento em coordenadas do mundo e não altera a câmera, os ossos ou os clipes.

A aterrissagem de um salto iniciado no chão continua visível com velocidade horizontal, sem depender do limiar de parada. Wall Jumps continuam no ciclo de salto normal. Aderência e bordas mantêm sua orientação de parede e a seleção autorizada pelo servidor.

## Impulso adicional de Sprint

O Minecraft acrescenta aproximadamente **0,20 bloco/tick** no `jumpFromGround` durante Sprint, na direção da visão. O mod preserva o momento já presente e redireciona esse impulso pela última amostra de deslocamento no chão. Com o ajuste padrão de **15%**, o impulso de saída passa de **0,20 para 0,23 bloco/tick**, um acréscimo de **0,03 bloco/tick** no instante do salto. Essa porcentagem se refere ao impulso vanilla, e não à velocidade horizontal total nem à distância final.

O valor fica no TOML do mundo `serverconfig/towerofentropy-jump-server.toml`, na chave `sprintJumpBonus`. O intervalo é 0–0,30. Valor zero mantém o redirecionamento do impulso de Sprint e desativa o acréscimo. A configuração de servidor é sincronizada pelo Forge. O mesmo evento vanilla de salto é calculado no servidor e no cliente que faz a predição local. A amostra do servidor usa posições já aceitas. Servidor integrado e cliente mantêm amostras separadas pela instância do jogador, pois compartilham o mesmo ID de entidade. Um salto sem direção recente mantém o impulso vanilla. A física volta a usar normalmente a aceleração, o atrito e as colisões do Minecraft após a saída.

Pulo parado e a pé sem Sprint conservam o impulso anterior. O bônus não participa de Wall Jump, Ledge Climb ou Roll. Água, escadas, veículos, voo e demais estados já excluídos continuam fora do ajuste. Nenhum componente vertical foi acrescentado.

## Arquivos alterados

- `SprintJump.java`, `ParkourJump.java`, `WallSlide.java` e `TowerOfEntropy.java` registram e aplicam a opção no evento de salto e consultam o estado de parkour.
- `HorizontalLocomotion.java`, `PlayerRendererR3.java` e `VisualContext.java` orientam a raiz visual nos três clipes e preservam o estado de aterrissagem.
- `ExpansionRuntimeTest.java`, `build.gradle` e `.gitignore` adicionam casos e perfis isolados de QA.

## Medições e cobertura

Medi do tick anterior à ação de pular até o primeiro contato com o chão, na mesma plataforma plana de pedra, sem parede no caminho. Nos cenários em movimento, a entrada para frente foi mantida até aterrissar. O controle com Sprint e bônus zero foi medido separadamente para isolar o efeito do acréscimo.

| Cenário | Distância horizontal | Altura máxima | Impulso horizontal observado no evento do servidor |
| --- | ---: | ---: | ---: |
| Parado, sem Sprint | 0,000 bloco | 2,502 blocos | 0,000 bloco/tick |
| Caminhando, sem Sprint | 2,807 blocos | 2,502 blocos | 0,000 bloco/tick |
| Sprint com bônus zero | 4,768 blocos | 2,502 blocos | 0,200 bloco/tick |
| Sprint com bônus padrão | 4,935 blocos | 2,502 blocos | 0,230 bloco/tick |

O ganho medido foi **0,168 bloco**, cerca de **3,5% de distância neste percurso**. O teste observou um único evento de saída no servidor por salto. Wall Jump para longe percorreu aproximadamente 3,649 blocos, como no QA anterior. Os três clipes originais de parede e borda foram observados em seus estados. Os oito ângulos passaram com a câmera fixa e a reversão W → S foi exercitada durante o voo, inclusive a passagem por baixa velocidade.

O QA usa cópias novas de um save chamado `Global Survival one` em `run-eight-direction-qa/classic/saves`, `run-eight-direction-qa/slim/saves` e `run-eight-direction-qa/regression/saves`. Para repetir as medições, na pasta `minecraft` use `./scripts/gradle.ps1 runClient --offline -PtoeExpansionTest -PtoeEightDirectionQa`. Para Slim, acrescente `-PtoeGlobalSlim`. A regressão usa `-PtoeGlobalCoverage -PtoeGlobalSlim -PtoeGlobalSkinCheck -PtoeExpansionQa -PtoeEightDirectionQa`.

As capturas vão para `animation_pipeline/converted/eight-direction-qa`. Os arquivos 0 a 7 mostram frente, frente-esquerda, esquerda, trás-esquerda, trás, trás-direita, direita e frente-direita, nessa ordem.

## Limites e revisão visual

O teste usa Forge 47.4.28 com servidor integrado. O mod reusa a predição vanilla para saltos no chão e não altera o protocolo de movimento nem cria um pacote contendo velocidade fornecida pelo cliente. Servidor dedicado com latência, pisos de baixo atrito e outras configurações de movimento precisam de avaliação separada.

Em F5, compare saltos para trás, laterais e diagonais. Durante um salto para frente, inverta a direção sem virar a câmera e observe o reenquadramento do corpo. Repita com Sprint e observe o contato na aterrissagem e as transições próximas à parede e à borda. A aprovação visual segue pendente.

## Resultados da validação

- Build Forge final aprovado. Os 23 recursos de jogo anteriores conferem byte a byte com o JAR de referência.
- Integração Classic **29/29** e Slim **29/29**, sem erros de renderização. Passaram os oito saltos direcionais, o salto de Sprint para trás, a inversão no ar, as medições de altura e distância, a saída única, o Wall Jump sem bônus e a seleção original de parede e borda.
- Regressão Slim **13/13**, incluindo espada, Roll, arco com flecha vanilla, salto e reconexão. As cinco suítes existentes passaram, incluindo 43 casos de movimento horizontal, 18 de combate e 104 de Roll.
- [Captura Slim do salto para trás](../animation_pipeline/converted/eight-direction-qa/slim/screenshots/jump-direction-4.png) e [captura Classic de um salto diagonal](../animation_pipeline/converted/eight-direction-qa/classic/screenshots/jump-direction-1.png). Nenhuma captura histórica foi sobrescrita. Nenhum commit foi criado.

SHA-256 do JAR `9B975051C71BE8E21CD720E49F9C723FAE93EADD7A54BA041B282CCC60C4AAD5`.
