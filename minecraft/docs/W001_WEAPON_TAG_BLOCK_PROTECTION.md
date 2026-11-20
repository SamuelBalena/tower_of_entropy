
# W001 — Tag de armas e bloqueio de mineração

## Regra

O Forge 47.4.28 e o Minecraft 1.20.1 carregam `toe:weapons` como uma tag de itens. O mod mantém o ID `towerofentropy`. O namespace `toe` é usado apenas pela tag pública. Seu arquivo fica em `data/toe/tags/items/weapons.json` e inclui as seis espadas vanilla, de madeira, pedra, ferro, ouro, diamante e netherita. O projeto atual não registra outras armas. Um datapack pode acrescentar itens próprios ou referências a outras tags com `replace: false`.

Em Survival e Creative, um item marcado bloqueia a ação de ataque a bloco na mão ativa. No Minecraft padrão, a mineração usa a mão principal. A presença de um item marcado na mão secundária não impede o uso de uma ferramenta da mão principal. O bloqueio independe do modo de combate. Ferramentas comuns, inclusive machados, ficam fora da tag padrão.

## Cliente

`InteractionKeyMappingTriggered` identifica um ataque a bloco com a mão informada pelo Forge. O evento é cancelado com swing desativado antes de o Minecraft minerar. Uma interação de mineração em curso passa pelo cancelamento vanilla para limpar a rachadura e avisar o servidor. `LeftClickBlock` também bloqueia o início ou a continuação disparada diretamente pelo controlador de mineração. O R3 exclui itens marcados do caminho de Jab/Cross e do corte visual de blocos da C001.2. Um ataque real a entidade ou ao ar continua seguindo o seletor de combate existente.

## Servidor

`LeftClickBlock` bloqueia a ação no servidor antes de o controlador aplicar efeitos ou dano ao bloco, inclusive em Creative. ABORT fica disponível para limpeza de sessão. A resposta de estado para um bloco já carregado e ao alcance mantém a previsão cliente em sincronia. `PlayerEvent.BreakSpeed` zera a velocidade enquanto a mão principal segura uma arma marcada, e `BlockEvent.BreakEvent` rejeita tentativas diretas de finalizar a quebra com essa arma.

Na troca de ferramenta por arma, o tick do jogador encerra as sessões normais e adiadas do controlador vanilla e limpa as rachaduras. Esses campos internos são acessados pelos nomes SRG mapeados pelo Forge desta versão. Comandos que modificam o mundo sem passar pela mineração do jogador, como `/setblock`, continuam no caminho próprio.

A tag não muda o controlador dos combos, alcance, dano, hitboxes, cooldown, regra de durabilidade, proteções Forge ou PvP. Uma arma que usa o fluxo vanilla de entidades continua nesse fluxo. As espadas que já usavam o fluxo R3 continuam com os clipes aprovados. Uso com botão direito, projéteis e efeitos de mundo que não passam pela mineração do jogador continuam seguindo suas próprias regras.

## Arquivos

- `WeaponTags.java` expõe a classificação compartilhada. `data/toe/tags/items/weapons.json` declara a lista padrão.
- `WeaponBlockProtection.java` registra os eventos Forge e encerra sessões antigas de mineração.
- `CombatInput.java`, `ClientSwingPose.java` e `SwingItemPolicy.java` aplicam o bloqueio cliente e a prioridade visual.
- `WeaponBlockRuntimeTest.java`, `ExpansionRuntimeTest.java`, `build.gradle` e `.gitignore` organizam a validação em mundos e diretórios de captura separados.

## Limites

O acesso ao estado interno da mineração foi conferido para Forge 47.4.28 com a versão 1.20.1. Um mod que altere blocos fora do fluxo de mineração pode ter regras próprias. O cenário de teste da C001.2 que esperava a quebra de plantas por espadas representa a regra anterior. Seus registros e capturas continuam disponíveis. A aprovação visual desta rodada fica com o usuário.

## Validação

O Classic passou nas **48 verificações W001** e nas **29 verificações da expansão**. A matriz acionou o botão de ataque uma vez e o manteve pressionado. Os contadores de desenho e o estado da mão verificaram ausência de swing vanilla, Jab/Cross e clipe de mineração de espada nas tentativas bloqueadas. A verificação servidor chamou as entradas de início, conclusão e quebra direta após o teste de entrada cliente. O segmento de troca de equipamento abriu uma sessão de mineração com ferramenta, confirmou a sessão adiada e constatou seu encerramento após equipar a espada. O comando administrativo `/setblock` continuou operando com a arma equipada.

Um datapack só dos mundos de QA acrescentou `minecraft:blaze_rod` à tag. A vara, que não é uma espada, foi bloqueada no cliente e no servidor em Survival e Creative. A matriz confirmou quebra por picareta, machado, pá, enxada, tesoura, mão vazia, tocha e bloco comum. Os cinco cenários de entidade registraram um impacto do combo e dano verdadeiro no servidor. A seleção final observada foi de espada, sem etapa vanilla concorrente.

As cinco suítes de lógica existentes passaram: `test-horizontal.ps1`, `test-combat.ps1`, `test-roll.ps1`, `test-locomotion.ps1` e `test-actions.ps1`. Os perfis de QA usam cópias do mundo em `run-w001-qa/` e capturas novas em `animation_pipeline/converted/w001-qa/`.

```powershell
.\scripts\gradle.ps1 build --offline --console=plain
.\scripts\gradle.ps1 runClient --offline --console=plain -PtoeExpansionTest -PtoeW001Qa -x downloadAssets
.\scripts\gradle.ps1 runClient --offline --console=plain -PtoeExpansionTest -PtoeGlobalSlim -PtoeW001Qa -x downloadAssets
.\scripts\gradle.ps1 runClient --offline --console=plain -PtoeGlobalCoverage -PtoeGlobalSlim -PtoeGlobalSkinCheck -PtoeExpansionQa -PtoeW001Qa -x downloadAssets
```

O Slim passou nas **50 verificações W001** e nas **29 da expansão**. Seus dois casos adicionais comprovaram no cliente e no servidor que uma espada na mão secundária deixa a picareta da mão principal minerar normalmente. Os perfis usaram o servidor integrado. A suíte R3 passou em **18/18**, incluindo espada, Roll, arco, consumo e reinicialização.

O build Forge final passou. A auditoria confirmou as seis espadas no JAR, a vara de blaze restrita ao datapack de QA, zero classes de teste e 16 recursos de modelos e poses idênticos aos anteriores. Os logs desta rodada usam `.logs/w001-*.log`. As capturas históricas foram preservadas.

- JAR: `build/libs/towerofentropy-0.0.1.jar`
- SHA-256: `9EBCC25C127303EB33038463AC558B317D2BE99B7C871CCCDDAFA37F6A06A4D1`
