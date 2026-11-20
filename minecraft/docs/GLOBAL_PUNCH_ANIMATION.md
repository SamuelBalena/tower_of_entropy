# Substituição do swing genérico no Polymesh R3

## Causa e correção

O gatilho anterior dependia de eventos de ataque ou mineração e exigia mão principal vazia. Além disso, a seleção visual encaminhava ao vanilla swings sem um clipe reconhecido e certos tipos de item. O R3 também recusava equipamento na mão secundária, pois seu renderizador anexava apenas a pilha principal.

O cliente agora observa o swing efetivamente iniciado pelo Minecraft e a mão indicada pela entidade. Ataques no ar, ataques a entidades, mineração e interações que começam um swing padrão convergem para o mesmo caminho. Durante mineração contínua, o clipe recomeça na cadência visual vanilla, incluindo os efeitos de pressa e fadiga. A observação não altera pacotes, dano, cooldown, mineração, durabilidade ou lógica de itens.

A prioridade é ação específica integrada, soco genérico e, depois, a pose regular. Espada, ataque pesado e Roll mantêm a prioridade do controlador R3. Arco, consumo e corte de troncos usam seus próprios clipes. Os estados de parkour e as sequências de Wall Jump continuam selecionados pelas regras existentes. O R3 cancela a etapa vanilla/CPM após desenhar o modelo e seus itens.

O Jab e o Cross continuam alternando. Os nomes efetivamente reproduzidos são os das variantes frontais já usadas no jogo, `UAL_punch_jab_front` e `UAL_punch_cross_front`. Ambos permanecem no pacote junto com `UAL_punch_jab` e `UAL_punch_cross`. Para o ataque padrão com a mão principal vazia, cada clipe conserva seu braço original. Para swings com item ou na secundária, o R3 pode espelhar a pose dos braços para que o golpe acompanhe o lado correto. O espelhamento é feito sobre a deformação dos ossos em tempo de renderização. Geometria, hierarquia, pesos e UVs continuam vindo dos mesmos arquivos do Steve Classic e Slim.

Os itens são anexados aos antebraços correspondentes, uma vez por pilha. A posição dedicada do item nas poses de arco, consumo e tocha segue o clipe. A seleção do soco genérico independe do ID e da classe do item.

## Arquivos alterados

- `ClientSwingPose.java` observa o swing, seleciona o clipe e acompanha a cadência de mineração.
- `VisualContext.java` ordena ações dedicadas e socos e remove filtros de fallback associados a itens genéricos.
- `UpperBodyPose.java` reflete a deformação dos ossos superiores para o lado ativo.
- `PlayerRendererR3.java` compõe a pose e desenha as duas pilhas no lugar das camadas vanilla.
- `build.gradle`, `.gitignore`, `ExpansionRuntimeTest.java` e `GlobalPunchRuntimeTest.java` executam o cenário de validação em cópias separadas do mundo.
- `docs/GLOBAL_PUNCH_ANIMATION.md` registra a implementação e a validação.

## Limites

A mudança vale para o Steve local desenhado pelo R3 em terceira pessoa. Primeira pessoa, jogadores remotos, armadura, capa, camadas de skin desativadas, poses incompatíveis e modos de comparação legados ainda podem usar o renderer vanilla/CPM. Um item com seu próprio renderer ou animação que não sinalize o swing padrão fica fora desta substituição. O item genérico da suíte é registrado no Forge apenas no cliente de QA. A inspeção visual pelo usuário continua pendente.

## Como repetir a validação

A partir de `minecraft`, com JDK 17 e os caches locais do Forge disponíveis:

```powershell
.\scripts\gradle.ps1 build --offline --console=plain
.\scripts\gradle.ps1 runClient --offline --console=plain -PtoeExpansionTest -PtoeGlobalPunchQa -x downloadAssets
.\scripts\gradle.ps1 runClient --offline --console=plain -PtoeExpansionTest -PtoeGlobalSlim -PtoeGlobalPunchQa -x downloadAssets
.\scripts\gradle.ps1 runClient --offline --console=plain -PtoeGlobalCoverage -PtoeGlobalSlim -PtoeGlobalSkinCheck -PtoeExpansionQa -PtoeGlobalPunchQa -x downloadAssets
```

Cada perfil usa sua própria cópia do mundo `Global Survival one` em `run-global-punch-qa/`. As capturas ficam em `animation_pipeline/converted/global-punch-qa/`. A suíte dedicada verifica os contadores de desenho do R3 e de anexos e confirma a ausência do evento de pós-renderização vanilla durante os swings observados. O teste de entidade consulta a saúde do alvo no servidor. O teste de mineração segura o botão de ataque contra obsidiana e exige a reprodução dos dois clipes com a picareta equipada. O caso de corte usa machado e tronco para confirmar a prioridade de `UAL_chop`.

Passaram as cinco suítes de lógica `test-horizontal.ps1`, `test-combat.ps1`, `test-roll.ps1`, `test-locomotion.ps1` e `test-actions.ps1`. Cada modelo de produção, Classic e Slim, carrega 40 clipes e as quatro variantes de Jab/Cross.

A rodada Slim registrou 40/40 verificações, somando 11 casos desta correção e 29 de combate, caminhada, Sprint, salto e parkour. A regressão separada registrou 13/13, incluindo espada, Roll, arco com flecha real no servidor, consumo com alteração real de inventário, tocha, agachamento, salto e inicialização automática do R3. Os registros estão em `.logs/global-punch-slim.log` e `.logs/global-punch-regression.log`. As capturas dos relatórios anteriores permaneceram nos diretórios originais.

A repetição Classic com a empunhadura final registrou 40/40 em `.logs/global-punch-classic-final.log`. Classic e Slim tiveram zero erros reportados pelo R3. As capturas Classic de itens foram repetidas com a câmera nivelada. Alguns quadros da primeira passagem Slim foram feitos com a câmera apontada para cima e servem como registro do teste, com visibilidade limitada das mãos.

O build Forge de produção passou em `.logs/global-punch-build-final.log`. O JAR contém os modelos e metadados de pose de produção. A auditoria conferiu 16 entradas de modelos e poses byte a byte contra os recursos de build e confirmou a ausência das classes de QA.

- JAR: `build/libs/towerofentropy-0.0.1.jar`
- SHA-256: `A8F54711B5F3AE9CA823D64F1562C59B1BA1C25A149BC3BEC6D298E4F31BF19C`
