# W002 — Armas embainhadas, combate dinâmico e ajustes de golpes

## Controles e estados

A tecla padrão **H**, remapeável nas opções do Minecraft, alterna entre sacar e guardar a arma da mão principal. A tag `toe:weapons` continua sendo a fonte de classificação. Armas recém-equipadas começam em `SHEATHED`. `DRAWING` e `SHEATHING` duram seis ticks e bloqueiam ataques até a arma chegar a `DRAWN`. Trocar de slot ou pilha, morrer, trocar de dimensão ou desconectar limpa o estado anterior. A mão principal é o slot de arma ativo nesta etapa. Itens comuns na mão secundária continuam disponíveis para as interações vanilla.

| Modo | Condição | Locomoção |
| --- | --- | --- |
| `EXPLORATION` | Sem arma sacada | Velocidade normal |
| `ARMED` | Arma sacada sem ameaça recente | Velocidade normal |
| `COMBAT` | Arma sacada e ameaça ativa ou recente | −5% na velocidade de deslocamento e Roll contextual |

O servidor busca alvos efetivos de mobs hostis ou neutros a cada cinco ticks, no raio de 24 blocos, e ignora aliados. Um mob precisa selecionar o jogador como alvo. A configuração `combatGraceTicks` mantém `COMBAT` por 60 ticks após a última detecção, por padrão. Um único modificador transitório aplica os 5%, inclusive diante de múltiplos inimigos. A penalidade sai ao retornar a `ARMED` ou `EXPLORATION`. O servidor envia mudanças de estado ao dono, vinculadas ao item e slot. A versão do canal é 11 e requer atualização em ambas as pontas de uma conexão.

O Roll contextual usa o primeiro pressionamento do comando configurado de Sprint durante `COMBAT`. Segurar o comando durante a entrada em combate não dispara Roll. Após a esquiva, manter o comando permite Sprint e soltar encerra a corrida. O Roll mantém a janela existente de indisponibilidade enquanto em execução ou recuperação e os i-frames dos ticks 3–9. O caminho contextual recusa ataques ativos. O atalho manual do Roll continua disponível e também respeita o bloqueio do pesado aéreo. A câmera C001.3 mantém o zoom manual de 2 a 8 blocos, os dois ombros e a correção de clipping existente.

## Recovery e deslocamento

| Golpe | Tempo lógico anterior → novo | Impacto | Trecho final retimado |
| --- | --- | --- | --- |
| Terceiro leve | 40 → 35 ticks | Tick 14 | A partir de 1,0 s. Recovery de 20 → 15 ticks, −25%. |
| Pesado | 35 → 34 ticks | Tick 12 | A partir de 1,5 s. Recovery de 5 → 4 ticks, −20%. |

A retimagem atua no fim do clipe. Os dados de dano, alcance, ângulo, progressão do combo e lunge no chão continuam vindo das regras existentes. O pesado aéreo registra a origem horizontal no servidor e limita as coordenadas antes da validação vanilla de movimento. O raio progride de 0,40 até 0,85 bloco. O cliente ignora direção, Sprint e novo pulo durante o comprometimento e reduz a velocidade horizontal remanescente gradualmente. A proteção atravessa a aterrissagem até o fim do recovery e deixa as coordenadas Y, gravidade e colisões a cargo do Minecraft. O limite aceita deslocamentos por knockback e não puxa o personagem de volta à origem. A aderência e o Wall Jump aguardam o fim do ataque comprometido; a elegibilidade do parkour fora dessa janela segue as regras anteriores. As regras de movimento dos ataques leves no chão e no ar continuam sendo as da W001.1.

## Arquivos

- `combat/WeaponReadiness.java`, `combat/HeavyAirLock.java` e `client/ClientWeaponState.java` implementam estados, leitura de aggro, limite aéreo e sincronização local.
- `combat/CombatConfig.java`, `combat/CombatMove.java`, `combat/CombatController.java` e `combat/RollController.java` integram configuração, recovery, ataques autorizados e Roll contextual.
- `animation/ActionKeys.java`, `animation/ActionNetwork.java`, `client/ClientCombatState.java`, `client/ClientAttackMotion.java` e `client/CombatInput.java` integram controles, mensagens e movimento previsto.
- `client/PlayerRendererR3.java` posiciona a arma guardada no torso no R3. `assets/towerofentropy/lang/{en_us,pt_br}.json` registra o novo controle.
- `movement/WallSlide.java` inclui o pesado aéreo na regra de exclusão mútua entre ataque comprometido e parkour.
- `META-INF/coremods.json` e `coremods/heavy_air_horizontal.js` integram o limite de movimento à recepção vanilla de pacotes no servidor.
- `build.gradle`, `validation/w002/java`, `validation/a3/java/com/towerofentropy/combat/A3RuntimeTest.java` e as fixtures `WeaponBlockRuntimeTest.java` e `AirSwordCameraRuntimeTest.java` em `validation/expansion/java` dão suporte à nova rodada. As fixtures que já precisavam empunhar espada passaram a sacá-la antes das verificações existentes. `ExpansionRuntimeTest.java` ganhou um rastreio opcional da amostra de caminhada, com a mesma asserção.

## Limites e revisão visual

Os conjuntos de clipes já integrados ao R3 não continham animações compatíveis de sacar e guardar. Os estados já funcionam e a arma passa entre o ponto de fixação no torso e a mão. Clipes por família de arma e o refinamento da posição no torso ficam pendentes. A representação no torso é local em terceira pessoa pelo R3. A primeira pessoa e a visualização de outros jogadores usam suas renderizações existentes. A decisão de ataque permanece no servidor. O posicionamento e o ritmo visual aguardam avaliação do usuário no Minecraft.

## Validação

- W002 Classic e Slim passaram em **40/40** cada, em servidor integrado. A distância da câmera permaneceu em aproximadamente 4,125 blocos na medição euclidiana, incluindo o offset lateral, antes e durante `COMBAT`. O teste remapeou Sprint para K e observou o Roll de 2,5 blocos, os i-frames, a retomada da corrida com a tecla mantida e o fim da corrida ao soltá-la mantendo a direção. O servidor limitou diretamente um pedido horizontal de 4 blocos por eixo e manteve Y. No salto de teste, a aterrissagem ocorreu ainda sob bloqueio e o jogador retomou o deslocamento após o recovery.
- A regressão R3 Slim passou em **18/18**, incluindo espada, Roll, consumo, pulo, arco com projétil vanilla e reinicialização.
- A execução combinada Slim registrou **50/50** em W001, **35/36** em W001.1/C001.3 e **28/29** na expansão. Passaram combo A/B/C, dano no salto e no Sprint Jump, vegetação intacta, parede sólida bloqueando dano, zoom manual, ombros, primeira pessoa, câmera frontal, clipping, direção de corrida nas oito direções e parkour. O caso `migrated_b` recebeu o perfil de opções Classic, em que a tecla legada é desatribuída. A checagem Slim esperava K, um remapeamento personalizado. A repetição focalizada com as opções Slim passou em **36/36**, incluindo a persistência de K. A amostra única de caminhada falhou, enquanto a reversão de Sprint passou. A repetição isolada da expansão, com o perfil Slim, reproduziu a divergência e ficou em **7/8**. O rastreio registrou `UAL_jog`, velocidade horizontal residual de 0,122 bloco/tick, jogador no chão, W pressionado e Sprint desligado. A seleção de `Walk` nessa amostra segue pendente. Os limiares de locomoção e as asserções foram preservados. A inspeção dos saves confirmou o mesmo atributo base vanilla de velocidade, sem modificador persistido. A primeira tentativa da suíte combinada também registrou uma corrida do Gradle entre conjuntos diferentes de classes de QA. A repetição completou a matriz.
- A fixture usou mobs com IA desligada e alvos controlados para isolar o sinal do servidor. O knockback foi verificado exercitando a exceção de deslocamento do servidor. Multiplayer com dois clientes, latência externa, morte e troca de dimensão não foram exercitados nesta rodada.
- As rodadas preliminares expuseram o conflito com a tecla G do CPM e dois problemas de preparação da fixture, durante o carregamento de terreno e quando o primeiro Sprint começava dentro do período de segurança. Os logs preliminares permanecem em `.logs/w002-*.log`.
- Suítes locais `test-horizontal.ps1`, `test-combat.ps1`, `test-roll.ps1`, `test-locomotion.ps1` e `test-actions.ps1`.
- Capturas desta rodada em `animation_pipeline/converted/w002-qa/`, com a matriz W002 em `classic-release-edge/` e `slim-release-edge/`. Os logs correspondentes e os retestes de regressão ficam em `.logs/w002-*.log`.
- JAR em `build/libs/towerofentropy-0.0.1.jar`.


O build de distribuição passou. A auditoria comparou os bytes dos **16/16** arquivos de modelos e poses com o JAR de referência local. O pacote contém a tag W001 e a proteção de pacote W002. A auditoria encontrou zero classes de QA no JAR.

O SHA-256 do JAR é `C3CABBA7EEDDB5024B1B7DE5E7F956232584E7C1055C35ABA27D4ADC59567C53`

A partir da pasta `minecraft`, com o mundo `Global Survival one` e o `options.txt` de QA copiados para o perfil isolado correspondente:

```powershell
.\scripts\gradle.ps1 build --offline --console=plain
.\scripts\gradle.ps1 runClient --offline --console=plain -PtoeW002Qa -PtoeW002Run=local -x downloadAssets
.\scripts\gradle.ps1 runClient --offline --console=plain -PtoeW002Qa -PtoeW002Run=local -PtoeGlobalSlim -x downloadAssets
```

Para preservar as capturas, escolha outro valor em `-PtoeW002Run` e copie um mundo de teste para o perfil novo antes de repetir a execução.