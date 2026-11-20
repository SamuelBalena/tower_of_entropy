# M002.A1 — Entradas, animações e combos

13/11/2026. Minecraft 1.20.1, Forge 47.4.28, Java 17. Polymesh R3 continua padrão; CPM e R3.1 permanecem preservados.

## Uso

Ative `/toe_combat on`, equipe uma espada na mão principal e use os controles **Atacar/Destruir** e **Usar item/Colocar bloco** configurados nas opções do Minecraft. ATTACK inicia os leves; USE inicia o pesado quando não há uso contextual prioritário. V mantém o Roll físico. J continua como diagnóstico legado opcional. `/toe_combat off` restaura o comportamento anterior. O modo é transitório e não persiste após reconexão/respawn.

## Linha do tempo autoritativa

| Estado / categoria SWORD | Origem UAL2 | Duração fonte | Recuperação do controlador | Impacto único |
|---|---|---:|---:|---:|
| LIGHT_ATTACK_A | Sword_Regular_A | 0,433 s | 9 ticks | tick 4 / 200 ms |
| LIGHT_ATTACK_B | Sword_Regular_B | 0,533 s | 11 ticks | tick 5 / 250 ms |
| LIGHT_ATTACK_C | Sword_Regular_C | 2,000 s | 40 ticks | tick 14 / 700 ms |
| HEAVY_ATTACK | Sword_Heavy_Combo, quadros 60–130 | 2,333 s | 47 ticks | tick 12 / 600 ms |

As durações de A/B são arredondadas para cima em ticks de 50 ms. Cada golpe consome sua tentativa de impacto uma vez, mesmo sem alvo. `AttackState` e `SwordTargeting` anteriores foram mantidos; alcance, arco, linha de visão, elegibilidade e proteção vanilla continuam aplicáveis. Leves usam `basicIronSword.damage`, padrão 4 HP brutos. O pesado usa `heavyDamage`, padrão 6 HP. Os números são provisórios.

`KEYFRAME_ANALYSIS.json` contém amostras e variações angulares dos braços/punho. B cruza a região de golpe nos quadros 7–9; C tem a passagem principal nos quadros 19–23, justificando 250 e 700 ms. A conserva o impacto aprovado em 200 ms. Variações de Euler isoladas podem incluir mudanças de representação; não são tratadas como detector físico de contato. São janelas de gameplay calibradas por amostras e imagens, sem teste de colisão por vértice da espada.

## Combo e estados

`CombatController` mantém arma, `CombatMove`, próximo leve, fila, prazo e snapshot de dano separados. A sequência é A → B → C. Cliques extras enfileiram no máximo duas entradas; mensagens repetidas no mesmo tick são descartadas. Não há repetição automática por botão mantido. Ao fim de C a fila é limpa e o próximo é A. Sem continuação em 24 ticks (1,2 s) **após a recuperação**, o próximo retorna a A. Configuração: `comboWindowTicks` no TOML de servidor.

Pesado só começa sem ataque/Roll ativo, limpa a continuidade leve ao terminar e não avança A/B/C. Entrada leve durante pesado e entrada pesada durante leve são rejeitadas; não há combos mistos. Roll válido cancela o ataque, fila, continuidade e impactos pendentes. Ataques não interrompem Roll.

IDLE, JOG, LIGHT_ATTACK_A/B/C, HEAVY_ATTACK, ROLL e VANILLA_FALLBACK são apresentados por `CombatController.actionStatus`. `CombatMove.WeaponCategory` reserva SWORD, SPEAR, AXE, BOW e UNARMED; somente `SwordItem` tem conjunto implementado. Nenhuma arma foi criada.

O protocolo de rede passou a 4: cliente envia intenção leve/pesado; servidor decide clipe, início, duração, impacto e cancelamento. `ClientCombatState` sincroniza o tempo visual local por mensagem de início e cancela por mensagem explícita, sem provocar dano local. A ponte CPM permanece responsável pela locomoção/fallback e início físico do Roll; os novos ataques não esperam o registro de gestos CPM. Não há reinício por tick do clipe.

A mensagem inclui o tick absoluto de início do servidor; o cliente calcula a fase pelo relógio de mundo sincronizado vanilla. Uma nova geração do mesmo clipe também reinicia corretamente. A configuração antiga `basicIronSword.impactTicks` foi mantida apenas para migração; A1 usa as janelas individuais de `CombatMove`. O seletor de locomoção não cancela um ataque válido durante sua inicialização/histerese; estados realmente inválidos continuam cancelando.

## Entradas e prioridades

Foi inspecionado `InputEvent.InteractionKeyMappingTriggered` no sources JAR oficial do Forge 47.4.28. O evento identifica a KeyMapping e é disparado para ambas as mãos em USE, mesmo com cancelamento. O código compara as KeyMappings existentes, cancela o swing vanilla junto da ação consumida e envia somente pela mão principal. `VanillaAttackGuard` bloqueia no servidor o ataque de entidade vanilla com espada em modo de combate, evitando um segundo caminho de dano. `SwordTargeting` usa uma chamada direta de `hurt`, sem chamar `Player.attack`.

- ATTACK mirando bloco mantém mineração vanilla. Outros itens mantêm ataque vanilla.
- USE mirando qualquer bloco mantém prioridade vanilla, incluindo portas e baús. Não exige uma lista frágil de blocos interativos.
- USE mirando entidade não hostil mantém interação vanilla.
- Qualquer item no offhand reserva USE ao vanilla; escudo impede pesado enquanto equipado. Ataque leve ainda pode funcionar com fallback visual de equipamento.
- Menus, chat, overlays, jogador morto e espectador não geram ações pelo interceptor.
- Cada botão tem trava até soltura; segurar USE não carrega nem repete pesado.

## Assets, preservação e limitações

`prepare-a1.cjs` reaproveita as transformações aprovadas de retargeting, sem executar os gravadores antigos. `blender-a1.py` aplica as novas poses a uma cópia do Blend R3, incluindo orientação do punho. `package-a1.cjs` acrescenta animações ao GLB de runtime e preserva literalmente o buffer original, nós, meshes, skins, materiais, UVs, pesos e quatro clipes existentes. O GLB aprovado R3 não é sobrescrito. `verify-a1.cjs` confere esses invariantes, os modelos experimentais e 171 arquivos Godot.

Armadura, capa, offhand, primeira pessoa, Slim e demais fallbacks anteriores continuam disponíveis. **Os novos B/C/pesado não foram recriados como gestos CPM:** o fallback preserva equipamento/renderização vanilla, sem prometer esses movimentos no corpo CPM. R3 remoto e paridade visual multiplayer continuam pendentes da arquitetura anterior. O servidor continua autoritativo, mas esta validação usa servidor integrado; não equivale a um teste com dois computadores e latência artificial.

A aprovação visual anterior do modelo R3 não aprova automaticamente as novas animações. As capturas técnicas não substituem a avaliação estética do usuário. Não houve alteração de Godot, animações originais, geometria do personagem, física do Roll, stamina ou progressão.
