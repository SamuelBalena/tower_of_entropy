# Política de animações para ferramentas e ataques genéricos

## Causa

A substituição anterior reagia a todo swing vanilla. O estado do braço indicava qual mão se mexeu, com pouca informação sobre a ação que iniciou o gesto. Um golpe de picareta, o lançamento de uma linha de pesca e a colocação bem-sucedida de um bloco podiam chegar pelo mesmo caminho visual. O R3 interceptava esse gesto e desenhava os clipes de Jab e Cross.

## Política

1. O renderer mantém clipes já integrados, incluindo combos de espada, Roll, arco em uso e disparo, consumo e corte de troncos com machado.
2. Ferramentas, armas, equipamentos especializados e itens ambíguos entregam o gesto ao renderer vanilla/CPM quando falta um clipe dedicado. O próprio renderer vanilla desenha as mãos, o item e a pose da ação durante esse intervalo.
3. O R3 usa Jab/Cross em ataques genéricos verificados com mão vazia ou item comum da mão ativa. O código mantém as variantes frontais já usadas pelo projeto, `UAL_punch_jab_front` e `UAL_punch_cross_front`.

A elegibilidade combina classes de item e equipamento, durabilidade declarada, animação de uso, ações de ferramenta do Forge e tags de item. Itens das classes base `Item`, `BlockItem` e `StandingAndWallBlockItem` são reconhecidos como comuns após esses filtros. Tochas e blocos comuns passam por essa regra. Uma subclasse desconhecida fica com o visual vanilla, a menos que seja identificada pela tag de itens comuns. A tag `forge:tools` e as ações padrão de picareta, pá, enxada, machado, espada, tesoura, escudo e vara de pesca ajudam a reconhecer implementações de outros mods.

Um datapack pode marcar itens em `towerofentropy:specialized_equipment` ou `towerofentropy:generic_attack_items`. Os sinais de equipamento têm prioridade sobre a tag de itens comuns. Mods que implementam equipamentos por dados ou capacidades fora dessas convenções podem usar a tag de equipamento para garantir o fallback.

Eventos de ataque a entidades, ataque no ar e mineração qualificam o swing observado. O clique de uso e os eventos de interação limpam essa intenção. A mineração contínua conserva a cadência visual em itens elegíveis. O estado real de dano e quebra continua nas rotinas vanilla ou no combate já existente. A mão secundária só qualifica um soco quando há um ataque identificado para ela. O gesto de uso ou uma chamada direta a `swing` sem origem de ataque usa o visual vanilla. Equipar um escudo na secundária ainda permite um ataque elegível com a principal.

## Arquivos

- `SwingItemPolicy.java` classifica a pilha usada.
- `ClientSwingPose.java` associa ação, swing e mão e mantém a alternância.
- `VisualContext.java` e `PlayerRendererR3.java` aplicam a prioridade e deixam o gesto vanilla passar quando necessário.
- `ToolPolicyRuntimeTest.java`, `GlobalPunchRuntimeTest.java`, `ExpansionRuntimeTest.java`, `build.gradle` e `.gitignore` registram e isolam os cenários automatizados.

## Limitações

O fallback aplica a pose completa do renderer vanilla/CPM por alguns quadros. As malhas, UVs, pesos e clipes R3 continuam vindo dos mesmos recursos. Uma ação de mod que emite somente um swing, sem um evento que indique ataque, mantém o comportamento vanilla. Picareta, pá, enxada, tesoura, lançamento e recolhimento de linha, escudo, besta e tridente ainda precisam de clipes dedicados. Ações de machado além do corte de troncos e golpes de armas em contextos sem clipe próprio também usam o fallback.

A avaliação visual no Minecraft fica pendente com o usuário.

## Validação

O perfil Classic passou em **61/61**. A matriz dedicada teve 32 verificações e a suíte de regressão da expansão teve 29. A matriz verificou o clipe exibido quando o R3 desenhava o quadro e os eventos de pós-renderização vanilla quando o R3 devolvia a ação. Cobriu mão vazia, tocha, bloco comum, espada, machado, picareta, pá, enxada, vara de pesca, tesoura, escudo, arco, besta e tridente.

Também verificou o ataque de cada mão, a alternância dos socos, o dano real do ataque com picareta a um zumbi, o swing de mineração contínua, a colocação real de um bloco, o corte de troncos, o bloqueio com escudo, o preparo do arco e o consumo. As três classes de QA registradas no Forge simularam material comum, ferramenta que anuncia `PICKAXE_DIG` e item de subclasse ambígua. O teste reproduz essas convenções sem depender de instalar um mod externo.

Passaram as cinco suítes de lógica `test-horizontal.ps1`, `test-combat.ps1`, `test-roll.ps1`, `test-locomotion.ps1` e `test-actions.ps1`.

As cópias de mundo ficam em `run-tool-policy-qa/`. As novas capturas ficam em `animation_pipeline/converted/tool-policy-qa/`. Os relatórios anteriores continuam nos diretórios históricos.

```powershell
.\scripts\gradle.ps1 build --offline --console=plain
.\scripts\gradle.ps1 runClient --offline --console=plain -PtoeExpansionTest -PtoeToolPolicyQa -x downloadAssets
.\scripts\gradle.ps1 runClient --offline --console=plain -PtoeExpansionTest -PtoeGlobalSlim -PtoeToolPolicyQa -x downloadAssets
.\scripts\gradle.ps1 runClient --offline --console=plain -PtoeGlobalCoverage -PtoeGlobalSlim -PtoeGlobalSkinCheck -PtoeExpansionQa -PtoeToolPolicyQa -x downloadAssets
```

O Slim também passou em **61/61**. A regressão R3 passou em **18/18**, incluindo espada, Roll, arco, consumo, salto e reinicialização. O servidor confirmou o disparo e o bloqueio da flecha pela parede. As duas variantes do Steve passaram sem erro de renderização.

O build Forge final passou. A auditoria do JAR conferiu 16 entradas de modelos e poses byte a byte contra o pacote anterior e confirmou a ausência de classes de QA. Os logs novos usam o prefixo `.logs/tool-policy-`. As capturas históricas foram preservadas.

- JAR: `build/libs/towerofentropy-0.0.1.jar`
- SHA-256: `2F24189C51D17B8055A3572FFDA4901CB6F5110AC0733C5BB820351EC6BA9E46`
