# M002.R3.1 — Registro de testes

12–13/11/2026, Windows, JDK 17, Minecraft 1.20.1/Forge 47.4.28, CPM e Polymesh 1.0.0. Mundo isolado `run-r31/saves/TOE_R31`; combate utiliza cópia independente `run-r31/saves/New World`. Nenhum mundo de jogo aprovado foi sobrescrito.

## Validação estrutural e diagnóstico

`node animation_pipeline/scripts/validate-r31.cjs`: PASS. 68 arquivos aprovados preservados, 12 ossos, 1.088 vértices exportados, 288 com pesos misturados, pesos normalizados e bind correto. Limites de repouso idênticos ao R3. Pares posição/UV originais preservados, ignorando duplicatas necessárias às normais de morph; apenas novos loops do quadril interpolam UVs. Todas as superfícies permanecem fechadas e normais de repouso são unitárias.

Os doze ossos têm **erro zero** de posição e orientação em relação ao R3 nos tempos originais dos quatro clipes. Nenhuma duração mudou. São 193 morph targets por malha, amostrados a partir do GLB original, com pesos zero em repouso e canais de pesos somente em `UAL_roll`. Accessors densos e todos os deltas finitos verificados. Foram regularizados 48 vértices com pesos 50/50 para 60/40 a favor do osso proximal.

A validação cobre 705 tempos, não só os 45 quadros originais. A seção fica entre 99,96% e 104,91% da área de repouso. O modelo continua sujeito a contato entre partes; o teste de seção não é teste de colisão de superfícies.

`blender-r31-diagnose.py`: avaliou os 45 quadros antes/depois, com LBS e as keys corretivas ativas. O relatório técnico lista os quadros críticos 4/11/15/23/26/27/30. Seções mínimas R3: cotovelos 17,6%/18,6%, joelhos 30,0%/47,0%; a correção recupera aproximadamente 100% nas amostras. Área de seção não é volume total nem ausência de interseção. Contagens BVH não são comparáveis diretamente entre topologias diferentes.

## Execuções nativas

Comandos, a partir de `minecraft`:

```powershell
.\scripts\gradle.ps1 runClient -PtoeR31 --console=plain
.\scripts\gradle.ps1 runClient -PtoeR31 -PtoeR31Mode=r3 --console=plain
.\scripts\gradle.ps1 runClient -PtoeR31 -PtoeR31Mode=r2 --console=plain
.\scripts\gradle.ps1 runClient -PtoeR31 -PtoeR31Mode=cpm --console=plain
.\scripts\gradle.ps1 runClient -PtoeR31 -PtoeCombatTest --console=plain
.\scripts\gradle.ps1 runClient -PtoeR31 -PtoeR31Restart --console=plain
.\scripts\gradle.ps1 build -PtoeR31 --console=plain
.\scripts\gradle.ps1 clean build --console=plain
```

Evidências finais: `animation_pipeline/reports/M002_R3_1_TEST_RESULTS.json`, relatórios individuais `..._RUNTIME.json` e `..._visual/`. Logs brutos locais `.logs/r31-*.log/json`. O [comparador](../experiments/m002_r3_1/COMPARE.html) usa quatro versões, quatro ângulos e sete quadros críticos, além dos outros clipes.

| Teste final | Resultado |
|---|---:|
| Perfil CPM original, incluindo export dos quadros críticos | 58/58 |
| GLB R2 no wrapper isolado | 56/56 |
| GLB R3 intacto no wrapper isolado | 57/57 |
| R3.1 final, após correção de interpolação | 58/58 |
| Auditoria nativa dos quadros originais com morphs | 45/45 |
| Regressores completos de combate M002.0 | 132/132 |
| Sincronização da animação no impacto servidor | 18/18 |
| Nova JVM começa com laboratório/combate desligados | 1/1 |
| Build experimental | passou, 12 s |
| Build normal limpo | passou, 17 s |
| Preservação | 68 arquivos aprovados e 171 Godot |
| Capturas arquivadas, com SHA-256 | 210 |

No combate final houve 7.562 desenhos, média de 0,523 ms CPU e zero erros de renderização. Impacto mantém quatro ticks; a caminhada com/sem ataque conserva o resultado vanilla da suíte. Os testes Java anteriores passaram: 18 regras de combate, arbitragem de ações e máquina de locomoção.

O JAR final normal é `build/libs/towerofentropy-0.0.1.jar`, 93.440 bytes, 50 entradas, SHA-256 `0267304C9B70A109AF62987421AEFF6C9287FB323482C791F098E8511CB90467`. A inspeção ZIP confirmou ausência de classes do laboratório/Polymesh, GLB, FBX, Blend e modelos CPM privados. O JAR experimental está somente em `.logs/r31-artifacts/`, não deve ser publicado. Nenhum cliente de teste ficou aberto.

A execução R3.1 com accessors densos passou em **58/58** verificações: corpo ponderado, pesos de morph nos 45 quadros, 44 poses, arma única, GLerror zero, retorno por comando real ao CPM, primeira pessoa, réplica remota intocada, fallbacks de equipamento, retomada, Roll contínuo retornando ao Idle e reconexão. A auditoria nativa vê 12 nós com peso total 1 em cada quadro, totalizando 12, e mede posições após morph + LBS; as capturas confirmam visualmente a execução. O torso da auditoria Java inclui apenas `body`, enquanto o validador GLB inclui também `body_outer`.

O ensaio contínuo usa o mesmo controller não cíclico durante toda a duração e verifica retorno ao Idle. Não há root motion novo, deslocamento de esquiva ou alterações de colisão. A suíte de combate compara a caminhada vanilla com e sem ataque, além de testar rejeições, dano, alcance, arco, spam, sessão e reconexão. V/J e seus handlers não foram modificados; não se alega teste físico humano de todas as teclas.

## Problemas encontrados e resolvidos

- Um import amplo de JOML tornou `Math` ambíguo na auditoria nova; corrigido com imports explícitos, sem tocar no código principal.
- Primeiro GLB com morph targets: loader falhou com `AccessorData ... data is null` em accessors sparse do Blender. O cliente permaneceu no fallback e o teste marcou `model_load=false`, apesar de Gradle terminar com sucesso. Corrigiu-se o empacotamento para dados densos e o ensaio nativo passou. Não foi necessário alterar Polymesh ou instalar biblioteca.
- O validador inicial comparava quantidades de entradas UV; o exportador de morph normals duplica vértices em limites de faces. A validação passou a comparar pares únicos posição/UV, mantendo a exigência de correspondência real com a superfície original.
- A primeira correção DQ passava nos 45 quadros, mas uma avaliação de tempos intermediários revelou contração. Foi substituída por rotação polar de seções, pesos regularizados e 193 amostras corretivas, mantendo os canais da animação. A versão final foi reexecutada no cliente e na regressão de combate; os resultados antigos não foram usados como aprovação final.

Os relatos de falha iniciais não são considerados aprovação visual. O arquivo `.logs/r31-initial-model-unavailable.json` preserva a primeira falha de carregamento.

## Limites e desempenho

As médias/máximos do consolidado medem somente CPU no bloco Polymesh de desenho, incluindo arma. São ensaios a 60 FPS com aquecimento/capturas, não benchmark de FPS/GPU. O perfil CPM só usa esse cronômetro na seção posterior de testes Polymesh; seu valor não é custo do renderer CPM. R3.1 usa o GLB R3 nas animações não Roll, portanto sua média mistura ambos os modelos. A nova malha exportada é maior e as keys corretivas acrescentam memória e avaliação de pesos.

| Perfil no mesmo wrapper | Desenhos | Média CPU | Máximo observado |
|---|---:|---:|---:|
| R2 | 5.680 | 0,526 ms | 7,980 ms |
| R3 | 5.739 | 0,529 ms | 7,163 ms |
| R3.1 final | 5.730 | 0,809 ms | 13,103 ms |

Aumento médio observado contra R3: +0,280 ms (+52,97%). Não é um aumento de 53% no tempo de todo o frame ou queda equivalente de FPS. O GLB corretivo local tem 7.331.312 bytes; seu tamanho e o custo dos 193 targets são motivos para manter adoção experimental. Não foi feito benchmark de memória residente ou servidor dedicado.

Capturas dos quadros críticos mostram ganho de espessura, com continuidade da skin. O contato coxa/abdômen no quadro 23 e proximidade do chão continuam documentados. Não se afirma melhoria em todo tipo de sobreposição; recuperar volume pode evidenciar contatos existentes. A aprovação estética continua com o usuário.

Cape/offhand/armadura permanecem fallback, sem sistema novo. Capa é fixture de presença de textura, réplica remota é entidade cliente; não são homologações de capa real ou multiplayer dedicado. Fontes e derivados de animação permanecem privados e ignorados pelo Git. Nenhuma fase seguinte iniciada.
