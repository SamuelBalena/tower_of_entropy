# M002.0 — Primeiro ataque funcional de espada

Protótipo opt-in para Minecraft 1.20.1 / Forge 47.4.28 / JDK 17 / CPM 0.6.26a. Usa o mesmo modelo Classic e o mesmo `Sword_Regular_A` UAL2 de 433 ms/14 quadros do M001.3. Não há conversões, animações, armas ou inimigos novos.

## Uso e ciclo de vida

Em um mundo com cheats/permissão de operador 2:

```text
/toe_animtest auto_steve
/toe_combat on
/toe_combat status
```

Com espada de ferro vanilla na mão principal, J solicita um ataque. V permanece Roll visual. Sem espada válida, J é recusado em modo combate, sem substituir o ataque por um gesto visual. Fora do modo combate, J volta ao teste visual M001.3, inclusive com mão vazia.

`/toe_combat off` desabilita o modo e cancela seu golpe pendente/ativo, mantendo a locomoção automática. `/toe_animtest off` restaura o modelo anterior e cancela qualquer golpe pendente; não desabilita a preferência de combate da sessão. Um J posterior poderá reativar a locomoção. Os comandos `idle`, `idle_steve`, `auto`, `auto_steve`, `off`, `roll` e `sword` continuam disponíveis. `toe_animtest sword` sempre é exclusivamente visual.

O modo começa desligado. Não é salvo em NBT, conta ou arquivo de configuração. Logout, novo login, respawn e encerramento do servidor removem a sessão; morte e estados incompatíveis cancelam o ataque. Não há fila, combo, stamina ou invulnerabilidade própria.

Os modelos locais do M001.3 precisam estar preparados em `run/player_models/`. O JAR não incorpora modelos CPM, GLBs, texturas Mojang nem o mod CPM. O modo não constitui um pacote público autossuficiente.

## Componentes e alterações

| Arquivo | Responsabilidade |
|---|---|
| `animation/ActionKeys.java` (preservado) | Um clique por pressionamento; menus/chat bloqueados; V/J remapeáveis |
| `animation/ActionNetwork.java` | Mesmo pacote de intenção; rota de J escolhida pelo servidor; protocolo elevado para 2 |
| `combat/CombatCommands.java` | Comandos opt-in por jogador, somente operador |
| `combat/CombatController.java` | Autorização, sessão, arma, cancelamento e impacto na thread lógica do servidor |
| `combat/AttackState.java` | Linha do tempo independente da duração visual; um consumo de impacto |
| `combat/AttackGeometry.java` | Alcance e arco, sem dependências Minecraft, testáveis isoladamente |
| `combat/SwordTargeting.java` | Elegibilidade, visão, ordenação de um alvo e uma chamada vanilla de dano |
| `combat/CombatConfig.java` | Parâmetros Forge SERVER, capturados por ataque |
| `animation/LocomotionController.java` | Guarda de estado compartilhada e notificações reais de início/cancelamento CPM |
| `TowerOfEntropy.java` | Registro da configuração SERVER |
| `combat/ClientCombatSmokeTest.java` | Suíte opt-in no save descartável, sem execução no cliente normal |
| `scripts/test-combat.ps1`, `scripts/tests/CombatRulesTest.java` | Testes Java isolados de geometria/linha do tempo |
| `build.gradle` | Flags explícitas de teste/reinício |

README, índice e relatórios foram atualizados. Não houve alteração nos modelos aprovados, retargeting, keybindings ou projeto Godot.

Os textos de J em `lang/en_us.json` e `lang/pt_br.json` passaram a descrever um ataque experimental; a tecla, categoria e mecanismo de entrada foram preservados.

`gradle.properties` atualiza a descrição do mod para refletir o protótipo; versões da stack e ID permanecem os mesmos.

## Fluxo servidor/CPM

1. J consome um único clique no KeyMapping Forge e envia apenas o enum `SWORD_ATTACK` pelo canal existente.
2. `SimpleChannel` restringe direção C2S. `context.getSender()` identifica o jogador; `enqueueWork` executa a lógica na thread principal do servidor. O pacote não contém alvo, posição, orientação, momento ou valor de dano.
3. Com modo desligado, permanece o teste visual. Com modo ligado, valida espada de ferro na mão principal, vida, não espectador, menu de inventário normal e estado suportado. Valida também disponibilidade do controlador CPM. Qualquer ação ocupada rejeita o pedido; não há cancelamento de Roll para priorizar ataque.
4. Aceito o pedido, cria-se um ataque pendente. A espera de carregamento/registro do CPM não avança o relógio do dano.
5. Somente após `ICommonAPI.playAnimation(..., "toe_sword", 1)` retornar com sucesso o controlador notifica `animationStarted`; registra-se o gameTime lógico do início.
6. No tick de impacto, revalida arma, vida, estado e dimensão. O alvo é escolhido nesse momento usando coordenadas reais do servidor, sem alvo travado no pedido.
7. O token de impacto é consumido **antes** da seleção/dano: um miss ou bloqueio de invulnerabilidade também gasta a oportunidade; não há novas tentativas durante o golpe.
8. A linha do tempo funcional termina após nove ticks (arredondamento superior de 433 ms). O controlador M001.3 conserva sua margem visual de dois ticks: novos pedidos ainda podem ser recusados até encerrar a janela visual de onze ticks. A duração do clipe CPM permanece 433 ms.

Fallback, comando de modelo, desativação, troca de arma, pulo, uso de item, sprint, água/lava, montaria, escalada, voo, morte, teleporte detectado ou falha CPM cancelam antes do impacto. A fila visual tem timeout existente de 150 ticks; um registro ausente nunca autoriza dano. Depois do impacto, cancelar não desfaz dano já aplicado.

A sincronização usa o instante da chamada real no servidor, não um retorno confiável do primeiro frame apresentado ao cliente. Rede/TPS/FPS podem introduzir diferença perceptível; não foi inventado callback de conclusão CPM. O servidor mantém autoridade mesmo sem o relógio/render do cliente.

## Região e seleção

Valores iniciais: alcance 3 blocos, arco horizontal total 90°, dano bruto 4 HP, impacto após 4 ticks (0,20 s a 20 TPS).

- Origem: pés do jogador. Distância euclidiana 3D até o **centro da bounding box** do alvo. É uma definição conservadora de alcance, não a ponta gráfica da espada nem o ponto mais próximo do collider.
- Direção horizontal: yaw real do servidor. O produto escalar deve estar dentro de metade do arco para cada lado. Pitch não inclina o arco; o alcance 3D limita diferenças de altura. Centro exatamente sobre o jogador não possui direção horizontal e é recusado.
- Consulta local em AABB, seguida de filtro esférico/arco. Não há posições ou chunks arbitrários recebidos do cliente.
- Elegíveis: `Mob` que implementa `Enemy`, vivo, não invulnerável, namespace de EntityType `minecraft`. Exclui jogadores, animais passivos, armor stands e mobs de mods. Não interpreta um animal irritado como monstro elegível. Não exige que o monstro tenha adquirido aggro.
- Aliados são excluídos. `hasLineOfSight` e `Level.clip` de olhos até centro, com `ClipContext.Block.COLLIDER`/`Fluid.NONE`, precisam estar livres. Parede sólida bloqueia o golpe. Plantas sem colisão não equivalem a paredes; líquidos não são barreira visual própria.
- Escolhe somente o candidato válido mais próximo; empate resolvido por ID de entidade do servidor. Se o escolhido resistir por invulnerabilidade vanilla, não procura outro alvo.

## Dano e feedback

Uma única chamada `LivingEntity.hurt(player.damageSources().playerAttack(player), damage)` delega armadura, resistência, invulnerabilidade temporal, eventos de dano, reação, som de dor e morte ao Minecraft/Forge. Quando `hurt` aceita o dano, toca `PLAYER_ATTACK_STRONG` no local. Não há chamada `Player.attack`, sweep, crítico, swing vanilla, partículas customizadas ou dano duplicado de J. O clique esquerdo vanilla permanece independente; não deve ser usado simultaneamente para medir o dano deste protótipo.

O dano é fixo antes das regras vanilla: não calcula atributos de ataque, encantamentos, força de recarga, durabilidade de espada ou combos. A reação/knockback própria do alvo ao receber dano continua vanilla. Não se escreve posição, velocidade, collider, atributos ou invulnerabilidade do jogador na lógica de combate. Edições de mundo/inventário e alvos sem IA pertencem somente à fixture explícita de testes.

## Configuração

Forge cria `serverconfig/towerofentropy-combat-server.toml` por mundo (no cliente dev, `run/saves/New World/serverconfig/`). O modo opt-in não é habilitado por esse arquivo.

```toml
[basicIronSword]
reach = 3.0
arcDegrees = 90.0
damage = 4.0
impactTicks = 4
```

Limites: reach 0,25–6; arco 1–180; dano 0,1–40; impacto 1–8 ticks. Cada ataque usa um snapshot das opções aceitas. A animação não é alterada pela configuração. Modificar parâmetros exige repetir a avaliação de sincronização e balanceamento.

## Referências e validação

- [Forge 1.20.1 — SimpleImpl](https://docs.minecraftforge.net/en/1.20.1/networking/simpleimpl/): canal, direção, remetente e `enqueueWork`.
- [API CPM](https://github.com/tom5454/CustomPlayerModels/wiki/API-documentation), conferida contra o JAR 0.6.26a e a implementação já validada no M001.3.
- Minecraft com mappings oficiais 1.20.1 e Forge 47.4.28 locais: chamadas compiladas e executadas, sem dependência de APIs de outras versões.

Resultados e limitações em [TEST_LOG_M002_0.md](TEST_LOG_M002_0.md). [NEXT_M002_1.md](NEXT_M002_1.md) registra apenas propostas; M002.1 não foi iniciado.
