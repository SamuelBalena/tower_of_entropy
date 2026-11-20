# M002.A3.1 — Registro de testes

13/11/2026. Java Microsoft 17.0.20.1 x64; Forge 47.4.28; Minecraft 1.20.1; Gradle 8.8; Polymesh 1.0.0; CPM 1.20-0.6.26a. Nenhuma ferramenta instalada neste marco.

Resultados consolidados em `animation_pipeline/reports/m002_a3_1`. Os logs completos ficam em `.logs/m002-a3-1-*.log`.

## Método

- Driver global cria mundos vanilla Survival e Creative, com seeds diferentes, sem cheats/comandos de ativação, e reabre Survival. Outra JVM repete a entrada nos mundos existentes. Nenhum desses testes precisa de TOE_A3.
- A entrada natural é observada antes de qualquer troca de skin de QA. Depois são comparados Classic e Slim e o retorno a Classic.
- A bateria A3 é reaplicada ao mundo comum `Global Survival one`. Fixtures alteram somente esse save isolado para preparar água, obstáculos e equipamentos; não escrevem marcadores de ativação.
- Regressões de combate e Roll usam o mesmo mundo comum, com relatórios separados dos históricos.
- Build normal e experimental são executados separadamente; a entrega final deve ser o build normal. O auditor do JAR verifica recursos e ausência de classes de QA/experimento.

## Ocorrências

A primeira bateria ampla Slim teve menus sobrepostos em parte das capturas. Embora seus checks existentes tenham passado, essas imagens não homologam os movimentos afetados. O driver de QA foi corrigido para fechar Pause/Options durante essa bateria isolada e os trechos afetados são repetidos. Isso não altera menus no gameplay normal.

A primeira regressão de combate obteve 143/145: as duas asserções antigas de respawn pressupunham inventário perdido e sistema desativado. O save comum tinha `keepInventory`, portanto AUTO corretamente reativava a espada retida. As asserções passaram a verificar ausência de ação presa e sessão de servidor ociosa, preservando a regra de gameplay. O resultado inicial fica em `regression_a1/FIRST_PASS.json`; a repetição completa é registrada separadamente.

A bateria ampla Classic registrou uma falha de mineração vanilla. Uma repetição específica de ações vanilla verifica também o raio real até o bloco-alvo, com câmera de QA determinística e mouse liberado. Não se deve converter o resultado inicial em aprovação sem essa repetição. Esses ajustes pertencem aos drivers de QA, não aos controles do mod.

O raio ao bloco foi confirmado correto, mas a repetição ainda falhou: a automação liberava o mouse. A inspeção local de `Minecraft.handleKeybinds` por `javap` confirmou que `continueAttack` exige `mouseHandler.isMouseGrabbed()`. O driver passou a capturar o mouse durante mineração/corte de madeira. Essa falha da automação não foi tratada como defeito de gameplay nem corrigida modificando ações vanilla.

A primeira regressão de Roll obteve 165/168. O primeiro Roll após teleporte/carregamento foi cancelado com segurança por descontinuidade de posição observada de 0,5357 bloco/tick; as demais direções completaram. O driver passou a preparar o cenário antes da medição, permitindo estabilização dos chunks/teleporte. A outra asserção falha de respawn tinha a mesma hipótese incorreta sobre `keepInventory`. `ROLL_FIRST_PASS.json` preserva as falhas iniciais. O cancelamento protetor sob descontinuidades não foi removido nem o limite de movimento alterado.

A segunda rodada, com distância 12 e limite de 120 FPS, ainda apresentou deslocamento incompleto em Back e cold_model durante atrasos entre cliente/servidor (`ROLL_LOAD_PASS.json`). Não foi declarado que o aquecimento sozinho resolvesse essa condição. A rodada controlada usa 6 chunks, simulação 5 e **limite**, não FPS medido, de 60; configuração em `QA_CONFIG.json`, somente em `run-global`. As entradas globais/skins em mundos novos foram verificadas antes com os valores originais 12/12/120. Os limites de movimento e a física continuam iguais; estresse sob travamentos não está homologado.

Warnings de `ResourceLocation` deprecated, tangentes GLTF geradas e recursos Gradle incompatíveis com Gradle 9 são herdados; devem ser distinguidos de falhas de compilação/renderização. Realms não é usado para os testes Singleplayer.

A primeira execução do projeto separado de JAR falhou no mixin CPM porque faltava o remapeamento SRG → Mojang exigido pelo userdev. O harness recebeu as mesmas propriedades de refmap já usadas pelo projeto principal; CPM e o JAR do mod não foram modificados. A repetição passou. Essas propriedades pertencem ao ambiente de desenvolvimento, não são instruções de instalação para um perfil Forge de produção.

## Limites de comprovação

Testar `runClient` e auditar o ZIP/JAR são verificações distintas de iniciar o artefato reobfuscado em uma instalação de produção. A inicialização em perfil Forge externo autenticado só será declarada validada se houver execução específica registrada. Não houve alteração de autenticação, licenciamento ou perfil pessoal do launcher.

A obrigatoriedade de CPM/Polymesh foi conferida no metadata do artefato. Não foi executado cliente removendo deliberadamente cada biblioteca; a recusa de carregamento por dependência ausente é o comportamento esperado pelas declarações `mandatory`, não um cenário de remoção homologado neste registro.

Aprovação estética e homologação de multiplayer remoto permanecem pendentes do usuário/testes próprios. Não houve avanço de marco.

## Reprodução

Na pasta `minecraft`, usando `scripts/gradle.ps1` e o wrapper oficial:

```powershell
.\scripts\gradle.ps1 runClient -PtoeGlobalTest --offline --console=plain
.\scripts\gradle.ps1 runClient -PtoeGlobalTest -PtoeGlobalFresh --offline --console=plain
.\scripts\gradle.ps1 runClient -PtoeGlobalCoverage --offline --console=plain
.\scripts\gradle.ps1 runClient -PtoeGlobalCoverage -PtoeGlobalSlim -PtoeGlobalSkinCheck --offline --console=plain
.\scripts\gradle.ps1 runClient -PtoeGlobalCoverage -PtoeGlobalVanillaCheck --offline --console=plain
.\scripts\gradle.ps1 runClient -PtoeA1Test -PtoeGlobalRegression --offline --console=plain
.\scripts\gradle.ps1 runClient -PtoeRollTest -PtoeA1Regression -PtoeGlobalRegression --offline --console=plain
.\scripts\gradle.ps1 clean build -PtoeR31 --offline --console=plain
.\scripts\gradle.ps1 clean build --offline --console=plain
.\scripts\audit-a3-global-artifact.ps1
.\scripts\gradle.ps1 -p validation/packaged runClient --offline --console=plain
node animation_pipeline/scripts/validate-a3-global.cjs
node animation_pipeline/scripts/review-a3-global.cjs
node animation_pipeline/scripts/finish-a3-global.cjs
```

O teste global cria os saves ausentes por APIs vanilla. As baterias de cobertura/regressão esperam que esse passo tenha sido executado; não precisam de TOE_A3. Os scripts de validação exigem as fontes privadas locais já existentes. `finish-a3-global.cjs` confere os resultados; não gera aprovações fictícias.
<!-- RESULTS -->

## Resultados finais

| Verificação | Resultado |
|---|---:|
| fresh | 24/24 |
| restart | 24/24 |
| slim | 13/13 |
| combat | 145/145 |
| roll | 168/168 |
| vanilla | 11/11 |
| packaged | 31/31 |
| Classic amplo, primeira rodada | 42/43; mineração repetida com sucesso |
| Build R3.1 experimental | passou — 36s |
| Build normal final | passou — 26s |
| Auditoria de JAR / preservação | passou |

JAR normal final: `build/libs/towerofentropy-0.0.1.jar`, 1979998 bytes, SHA-256 `2BD34B66B68B5105B87F0339BD54B050462F51CAC98029775B8CC495D98294DC`. Sem classes de QA/R3.1/SmokeTest. Inclui Classic, Slim, poses, suporte visual e cache CPM.

O teste **packaged** foi um projeto ForgeGradle separado, com o JAR normal como dependência remapeada, sem compilar os fontes principais. O probe exige origem `.jar` para classe e recurso Slim. O cliente abriu os dois saves comuns, alternou skins e mundos e executou espada/Roll pelo canal real do mod, sem comandos de ativação. É uma execução real do artefato empacotado em ambiente de desenvolvimento, não homologação de um perfil externo de launcher.

As 30 animações e todo o binário fora das posições/UVs dos braços Slim permaneceram intactos. Classic SHA-256 preservado; 171 arquivos Godot e código de combate conferidos. As falhas iniciais não foram apagadas: consulte `FIRST_PASS.json`, `ROLL_FIRST_PASS.json` e os logs das rodadas anteriores.

Evidência consolidada: [RESULTS.json](../animation_pipeline/reports/m002_a3_1/RESULTS.json). [Comparador Classic/Slim](../animation_pipeline/reports/m002_a3_1/SKIN_COMPARISON.html), [seleção PNG](../animation_pipeline/reports/m002_a3_1/REVIEW/README.md) e `REVIEW_COMPACT.zip`. A aprovação estética e os limites descritos acima permanecem pendentes.
