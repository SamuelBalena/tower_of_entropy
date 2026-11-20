# Testes M001.3 — Roll e Sword Attack visuais

Data: 2026-11-12, America/Sao_Paulo. Stack preservada: Minecraft 1.20.1, Forge 47.4.28, CPM 0.6.26a, Microsoft JDK 17.0.20.1 x64, Gradle 8.8 / ForgeGradle 6.0.54, Node 24.16.0. Nenhuma ferramenta ou mod novo foi instalado.

## Resultado comprovado

- Suíte de ações no cliente real: **37/37 checks** na execução final.
- Suíte original de locomoção M001.2, com os modelos novos presentes: **22/22 checks**.
- Cliente reiniciado: **1/1 check**, sem sessão ou ações presas antes de habilitar os testes.
- Validadores GLB/rig/CPM e testes Java de arbitragem/histerese passaram.
- Projetos/modelos aprovados: nove hashes preservados. Godot: 171 hashes preservados.
- Build e runClient concluíram com sucesso; evidências de encerramento ao final.

O build isolado não é usado como prova visual. As verificações de runtime leem o status do controlador, os parâmetros de ação na API CPM e as rotações renderizadas; as capturas vêm do Minecraft rodando.

| Caso testado no cliente | Resultado |
|---|---|
| Idle→Roll→Idle | passou, gesto único e parâmetro zerado depois |
| Jog→Roll→Jog | passou, sem reiniciar a locomoção a cada tick |
| Idle→Sword→Idle | passou, espada de ferro equipada |
| Jog→Sword→Jog | passou |
| V remapeado temporariamente para B | acionou Roll pelo KeyMapping/pacote real; remapeamento restaurado |
| Repetição por tecla mantida | apenas um início de Roll |
| Repetir o comando da mesma ação | recusado enquanto ocupado, sem estender o prazo |
| Roll durante Sword | recusado, Sword termina normalmente |
| Sword durante Roll | recusado, Roll termina normalmente |
| V/J em chat, inventário e controles | nenhum novo início; eventos descartados |
| Categoria e defaults | Tower of Entropy, V e J registrados; menu capturado |
| Sword com mão vazia | gesto visual funciona, sem criar arma ou dano |
| Off no meio do Roll | sessão removida, parâmetros ausentes, modelo anterior restaurado |
| Pulo durante Roll | gesto cancelado e fallback vanilla |
| Aterrissagem após cancelamento | Idle sem rotação residual de Roll |
| Reconexão durante Roll | nova sessão sem ação/controlador |
| Reinício do cliente | sem ação/controlador antes da ativação |
| Idle/auto/off antigos | código preservado; suíte M001.2 confirmou locomoção e comandos antigos |

Na suíte final de ações, deslocamento estacionário em Roll e Sword: **0**. Em 40 ticks de avanço, referência sem ação, Roll e Sword mediram exatamente `8.374761235223474` blocos. Na regressão isolada, vanilla e auto também mediram `8.374761235223474` blocos.

Não foram implementados efeitos de dano, ataques de entidade, hitboxes, invulnerabilidade, stamina, impulso ou alteração de velocidade. O teste usa uma espada de ferro vanilla e não pressiona o ataque vanilla para acionar J.

## Gerar e executar

Na pasta `minecraft`, com os arquivos M001.1/M001.2 já existentes:

```powershell
node animation_pipeline/scripts/retarget-actions.cjs
node animation_pipeline/scripts/validate-actions.cjs
node animation_pipeline/scripts/validate-idle.cjs
node animation_pipeline/scripts/validate-locomotion.cjs
.\scripts\test-actions.ps1
.\scripts\test-locomotion.ps1
.\scripts\gradle.ps1 build --console=plain
.\scripts\gradle.ps1 runClient -PtoePrepareActions --console=plain
```

Espere os dois logs `TOE_M001_3 EXPORT_VALID`, referentes à skin atual e à prévia Steve. O exportador carrega o projeto no CPM, confere quatro clipes e gera/reabre os binários. As conversões ficam em `animation_pipeline/converted/m001_3/`; modelos utilizáveis ficam em `run/player_models/`. Nenhum desses assets é embutido no JAR.

Os testes Java são executados diretamente com javac/java, sem dependência nova de JUnit. Gradle `test NO-SOURCE` não representa os testes executados por esses scripts.

Em mundo local de teste, com cheats para os comandos de desenvolvimento:

```text
/toe_animtest auto_steve
```

Use F5, pare/avance e pressione V (Roll) ou J (Sword). As teclas são remapeáveis em Opções→Controles→Teclas→Tower of Entropy. Para testar a arma, equipe uma espada de ferro vanilla. Não use cliques de ataque para tentar acionar a animação de J.

Também disponíveis:

```text
/toe_animtest roll
/toe_animtest sword
/toe_animtest off
```

Roll/Sword sem sessão habilitam a experiência com a skin atual; use auto_steve antes se quiser a prévia Steve. `idle`, `idle_steve`, `auto`, `auto_steve` e `off` continuam funcionando. Off limpa ações pendentes/ativas e restaura o modelo anterior. Pedidos durante outra ação são recusados; pressione novamente quando a atual terminar.

## Suítes automatizadas — somente save descartável

```powershell
.\scripts\gradle.ps1 runClient -PtoePrepareActions -PtoeActionTest --console=plain
```

Usa exclusivamente o save `run/saves/New World` criado pelos testes anteriores. **Não use um mundo pessoal renomeado como New World.** A rotina prepara uma faixa de pedra/ar, limpa entidades não jogadoras locais, muda hora/clima para visualização, equipa/desequipa uma espada de ferro num slot de teste, simula entradas vanilla e sai/reentra no mundo. As entidades de teste são removidas por discard, sem dano/drops ou escrita de estatísticas de combate.

Tudo isso pertence à fixture opt-in, não ao controlador: não há geração de faixa, limpeza de entidades, mudança de inventário ou câmera num runClient normal. Câmera/FOV, HUD, pausa por perda de foco, slot selecionado e a tecla remapeada para a verificação são restaurados ao terminar. Space e o ataque vanilla não são remapeados.

Para verificar reinício e repetir a suíte original M001.2 no mesmo novo processo:

```powershell
.\scripts\gradle.ps1 runClient -PtoeLocomotionTest -PtoeActionRestartCheck --console=plain
```

O observador M001.3 isola a faixa do save descartável, confere a ausência de ações antes da ativação e não assume os controles do teste M001.2. Também é possível usar somente `-PtoeActionRestartCheck`, que abre o save e confere os estados sem habilitar ações.

As rotinas M001.1 e M001.2 não foram removidas. Nenhuma suíte de diagnóstico executa sem seu parâmetro explícito.

## Evidências preservadas

- [Relatório técnico e capturas](../animation_pipeline/reports/M001_3_COMBAT_ANIMATIONS_REPORT.md).
- [37 checks de ações](../animation_pipeline/reports/M001_3_RUNTIME_TESTS.json).
- [22 checks de regressão](../animation_pipeline/reports/M001_3_LOCOMOTION_REGRESSION.json).
- [Check de reinício](../animation_pipeline/reports/M001_3_RESTART_TEST.json).
- [Validação estrutural e preservação](../animation_pipeline/reports/M001_3_VALIDATION.json).
- Logs locais ignorados: `.logs/m001_3-final-verification.log`, `.logs/m001_3-regression-final.log`, `.logs/m001_3-final-build.log`.
- Capturas originais em `run/screenshots/toe-m001_3-*.png`; cópias selecionadas em `animation_pipeline/reports/M001_3_visual/`.

## Ocorrências e correções

1. A primeira execução de ações passou em 37/37, mas as capturas estavam escuras e com mensagens sobre o personagem. A fixture passou a usar luz do dia e HUD temporariamente oculto; não houve edição/síntese das imagens.
2. Roll retargetado inicialmente flutuava no meio do giro porque só havia compensação de penetração no chão. O ajuste visual passou a alinhar o limite inferior dos cubos ao plano local das solas, elevando ou baixando a figura. A posição física continuou inalterada.
3. Uma repetição em outra região do save encontrou obstáculo no trajeto, resultando em 35/37 checks. A faixa livre foi ampliada em altura e a suíte passou em 37/37 sem mudar o controlador para mascarar a colisão.
4. A primeira regressão M001.2 passou em 21/22: a comparação de distância sofreu desvio lateral, compatível com interferência de entidades naturais. A fixture foi isolada, removendo somente entidades não jogadoras do save descartável; a nova execução passou em 22/22 com distância idêntica. A tolerância do teste e a física não foram alteradas.
5. Avisos já conhecidos de Realms na sessão dev e de depreciação futura do Gradle não impediram os testes. Não foram alteradas autenticação, licenciamento ou versões da stack.

## O que ainda exige avaliação humana

As capturas e logs confirmam execução, gestos distintos, espada junto à mão em fases observadas e limpeza de estados. O usuário ainda deve avaliar continuamente:

- naturalidade do Roll parado e em movimento;
- cortes de entrada/saída, especialmente Sword A sem clipe de recuperação;
- empunhadura da espada durante todo o golpe e em outros ângulos;
- contato em rampas/escadas e limites do ajuste vertical de Roll;
- teclado físico, remapeamento e hábitos de uso com menus.

Sem homologação: mão esquerda dominante, outras armas, armaduras, Slim/64×32, primeira pessoa, multiplayer remoto, latência ou TPS/FPS extremos. Permanecem os limites de foot sliding do Jog. Não há blending temporal nem callbacks públicos de término inventados; a janela de segurança é limitada e testada no servidor integrado.

Nenhum arquivo Godot foi modificado. M002 não iniciado.

## Encerramento final

Build final ap?s as altera??es de c?digo: `BUILD SUCCESSFUL in 14s` em `.logs/m001_3-final-build.log`. Cliente da su?te final encerrado normalmente: `BUILD SUCCESSFUL in 4m 59s` em `.logs/m001_3-final-verification.log`.
