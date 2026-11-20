# M002.R2 — Steve com skinning ponderado dentro do Minecraft

Data: 2026-11-12, America/Sao_Paulo. Minecraft 1.20.1, Forge 47.4.28, Java 17, CPM 0.6.26a.

**A malha ponderada do R1 foi renderizada dentro do Minecraft**, por um caminho experimental reversível. Cotovelos e joelhos usam influência combinada de ossos, com superfície contínua e textura Classic preservada nas capturas. O modelo não foi reconstruído. CPM continua sendo o padrão do projeto; esta revisão não adota definitivamente o backend e não transforma o tronco rígido do arquivo original em uma coluna articulada.

## Inspeção e preservação do R1

Foram lidos o relatório R1, o gerador original e os dados do GLB/Blend. Os arquivos originais não foram reexportados ou sobrescritos.

- GLB: `animation_pipeline/converted/m002_r1/steve-r1-weighted.glb`, SHA-256 `8529d3178a8e297eb8121eaed444ffc2c47b665015e0c9681197ebb9961fc8be`.
- Blend: `animation_pipeline/converted/m002_r1/steve-r1-weighted.blend`, preservado para rollback/revisão.
- Cópia de trabalho byte a byte em `animation_pipeline/converted/m002_r2/resources/assets/towerofentropy/models/gltf/r2_steve.glb`.
- [Baseline de 45 arquivos](../animation_pipeline/reports/M002_R2_BASELINE.json): código principal, modelos/dados aprovados, GLB/Blend R1 e seu gerador. Cópias em `.downloads/m002_r2/backup/`.
- [Inspeção glTF](../animation_pipeline/reports/M002_R2_GLTF_INSPECTION.json), [preservação e âncoras](../animation_pipeline/reports/M002_R2_PRESERVATION.json).

A malha contém 12 superfícies incluindo camadas externas, uma skin e dez joints. Braços/pernas têm transição de pesos junto a cotovelos/joelhos; cabeça e tronco usam ossos rígidos. O exportador mantém seções conectadas nas dobras. As separações entre cabeça/tronco/membros na construção geral são as partes clássicas do personagem, não metades de cotovelo desconectadas.

A auditoria do **modelo carregado pelo Polymesh**, não somente do JSON, encontrou:

- 736 vértices com pesos, 64 com mais de uma influência;
- erro máximo na soma de pesos: 0;
- dez juntas e erro máximo de identidade `globalBind × inverseBind`: 0;
- diferença máxima de aproximadamente **0,102527 bloco em espaço do modelo** entre a mistura ponderada e usar somente o osso dominante, em uma pose Jog;
- quatro animações embutidas com nomes e durações esperados.

Isso confirma que a renderização usa deformação real, sem simular uma junta com dois cubos. [Auditoria em runtime](../animation_pipeline/reports/M002_R2_RUNTIME_MODEL_AUDIT.json).

## Biblioteca e requisitos

Utilizado [Polymesh 1.0.0](https://github.com/phe6/polymesh/releases/tag/v1.0.0), código oficial no commit `f698f95f831306fe9d4010ba26f4c602b4dc7b0b`. A [documentação do projeto](https://github.com/phe6/polymesh) declara Forge/NeoForge 1.20.1, GLB e skinning. O código foi inspecionado e a execução foi comprovada especificamente em **Forge 47.4.28**. Não foi presumido que a biblioteca substituísse o jogador automaticamente.

| Artefato oficial local | SHA-256 |
|---|---|
| `polymesh-1.0.0-forge.jar` | `653b222ea124a3913d1094a9c32ebf3124421c10c0a11c3a12f59bc3c723c051` |
| `polymesh-1.0.0-dev.jar` (investigação inicial) | `b8c929902710f199f918ecbca9c367afb5bd6005c29c8cc7ef29f2ecff885f0e` |

Licença declarada: **LGPL-3.0-or-later**, com os textos LGPL/GPL no JAR. O runtime inclui jgltf 2.0.4 (MIT) e Jackson 2.13.4/2.13.4.2 (Apache-2.0), com seus avisos. Para uma distribuição futura, manter os avisos/licenças, disponibilizar o código correspondente da biblioteca conforme os termos e permitir a substituição/relink da parte LGPL; modificações da biblioteca exigiriam tratar também seu fonte correspondente. Nenhuma distribuição pública ou incorporação ao nosso JAR foi feita. [Licença upstream](https://github.com/phe6/polymesh/blob/master/LICENSE.LESSER).

O primeiro teste com `dev.jar` e dependências declaradas separadamente compilou, mas o runtime Forge não encontrou `GltfModelReader`. A configuração validada usa o **JAR oficial runtime com suas bibliotecas jar-in-jar**, remapeado pelo ForgeGradle:

```groovy
// Somente no perfil -PtoeR2
implementation fg.deobf('r2:polymesh:1.0.0:forge')
```

O repositório flatDir aponta apenas para `.downloads/m002_r2/`. Não foi necessário alterar o código Polymesh nem instalar ferramentas. O JAR dev permanece somente como artefato de investigação.

Full Motion não é carregado no R2. O Gradle recusa combinar R2 com os perfis R1/Full Motion. CPM permanece carregado para os comandos existentes, sincronização de ações e fallback. A alternativa [MCglTF Port](https://www.curseforge.com/minecraft/mc-mods/mcgltf-port) foi consultada, mas não instalada: Polymesh permitiu atingir a prova e não foi necessário manter outro backend simultâneo.

## Arquitetura de renderização

`SkinnedSteveRenderer` escuta `RenderPlayerEvent.Pre` com prioridade alta, somente no cliente de laboratório. A semântica de cancelamento foi conferida na fonte Forge 47.4.28.

1. O modo começa inativo fora da rotina automática de testes. `/toe_renderlab skinned` seleciona somente o jogador local.
2. Carrega o GLB por `PolymeshApi.loadModel`, cria `GltfModelRenderer` e usa o `AnimationController` da biblioteca.
3. O controlador amostra os canais embutidos e calcula matrizes globais. `CpuSkinningPipeline` aplica as matrizes de junta/bind e os pesos, emitindo vértices pelo vertex consumer Minecraft. Não foi criado um sistema de skinning substituto.
4. A matriz visual usa yaw interpolado do corpo, escala vanilla **0,9375** e conversão da orientação do GLB para o sistema de coordenadas Minecraft. A matriz normal é reconstruída pelo inverso transposto, incluindo as reflexões e a matriz do attachment.
5. A luz empacotada vem do próprio evento; o render usa a skin Classic atual do cliente, com amostragem nearest. A prova automatizada utiliza a textura Steve vanilla local 64×64.
6. Após um desenho bem-sucedido, cancela a passagem anterior. Assim, corpo/camadas/itens vanilla e CPM não são desenhados novamente para o jogador selecionado. Foram contadas passagens e itens para verificar exclusividade.
7. Outros jogadores não são selecionados e continuam pelo render anterior. A sombra simples do dispatcher permanece; não foi adicionada uma segunda sombra.
8. `off` apenas desliga a substituição. O modelo e controlador CPM continuam disponíveis, sem reatribuir modelos aprovados ou modificar estado de combate.

Não há escrita de posição, velocidade, hitbox, atributos ou colisões do jogador no renderizador. As transformações pertencem ao PoseStack. O carregamento ausente deixa o evento seguir para o render padrão; erro de desenho desativa o experimento. Logout limpa seleção e cache do wrapper. Recarregar recursos recria o controlador quando a referência do modelo muda.

A integração é um laboratório de cliente/servidor integrado. Os comandos R2 não constituem um protocolo de servidor dedicado; não foram promovidos ao mod normal.

## Animações e ligação com o combate

| Movimento de origem | Nome embutido no GLB | Duração |
|---|---|---:|
| UAL1 `Idle_Loop` | `UAL_idle` | 2,500 s |
| UAL1 `Jog_Fwd_Loop` | `UAL_jog` | 0,933333 s |
| UAL1 `Roll` | `UAL_roll` | 1,466667 s |
| UAL2 `Sword_Regular_A` | `UAL_sword` | 0,433333 s |

O arquivo GLB é a cópia exata do R1. Não houve novas animações, retiming ou novo retargeting. O tronco mantém a rotação rígida já existente; o Roll dobra pernas/cotovelos e gira o corpo, sem deformação interna adicional da coluna.

No modo automático, observa os parâmetros CPM existentes `toe_idle`, `toe_jog`, `toe_roll` e `toe_sword`. O servidor continua aceitando/rejeitando e temporizando ações pelo controlador aprovado. Roll/Sword reproduzem uma vez e retornam à locomoção. Não foi adicionada uma segunda solicitação da tecla J ou uma segunda aplicação de dano.

Os comandos R2 de clipe servem para inspeção: `/toe_renderlab idle`, `jog`, `roll`, `sword`. `/toe_renderlab skinned` limpa a seleção manual e volta a acompanhar o controlador aprovado. Os comandos `toe_animtest` e `toe_combat` permanecem no projeto.

## Attachment da espada

O GLB R1 não possui osso de mão independente. O wrapper usa o nó **103**, antebraço direito, e a compensação de pulso já aprovada nas poses CPM (âncora 303). Converte seus eixos para a base do osso e chama o `ItemRenderer` vanilla com o contexto de terceira pessoa da mão direita.

O item não foi adicionado ao GLB. Como a passagem vanilla/CPM é cancelada, a espada é emitida uma única vez pelo attachment. Nos checkpoints visuais, a contagem de itens coincide com a de desenhos. A validação das 162 amostras de âncora encontrou erro local máximo de aproximadamente `2,1e-14` pixel na compensação geométrica.

Foram capturadas fases inicial, impacto e final de Sword. Isso não representa homologação de cada quadro, de canhotos, offhand, todas as armas ou armaduras. A inspeção visual contínua da empunhadura ainda é recomendada. O cálculo de dano permaneceu completamente separado.

## Ambiente e reprodução

Tudo experimental fica em `run-r2/`, com cópia dos saves de teste, opções e modelos. Os arquivos originais `run/`, GLB/Blend e modelos aprovados são usados somente como fontes de cópia/leitura.

```powershell
# Exporta/copía recursos e abre somente o laboratório isolado.
.\scripts\start-r2.ps1

# Inspeção interativa sem programação ou modelagem manual.
.\scripts\start-r2.ps1 -Interactive
```

No jogo, `/toe_renderlab skinned` ativa e `/toe_renderlab off` restaura CPM. O laboratório interativo já prepara o Steve e a locomoção aprovados. Para combate: `/toe_combat on`, espada de ferro na mão principal e J. O modo de combate continua desligado por padrão até esse comando. V mantém Roll visual.

Para repetir os testes:

```powershell
node animation_pipeline/scripts/validate-r2.cjs
.\scripts\gradle.ps1 runClient -PtoeR2 -PtoeR2Mode=baseline --console=plain
.\scripts\gradle.ps1 runClient -PtoeR2 --console=plain
.\scripts\gradle.ps1 runClient -PtoeR2 -PtoeCombatTest --console=plain
```

O launcher exige o save descartável preexistente `run/saves/New World` e o copia para `run-r2/saves/TOE_R2`; a suíte de combate usa outra cópia em `run-r2/saves/New World`. As fixtures alteram plataforma, hora, inventário e alvos **somente nessas cópias**. A substituição de cache de skin para a prévia Steve é local ao processo, sem alteração de conta, autenticação ou licenciamento.

Rollback: fechar o laboratório e executar o projeto sem `-PtoeR2`. `clean build` normal exclui fontes, dependência e recursos R2. Nenhum JAR foi instalado em uma pasta global de mods ou no launcher.

## Comparação dentro do Minecraft

Abra o [comparador R2](../experiments/m002_r2/COMPARE.html). Todas as imagens desse comparador são do Minecraft, não renders Blender. O corpo gira independentemente da câmera para as vistas laterais/costas.

| Caso | CPM anterior | Malha com pesos |
|---|---|---|
| Idle frente | [antes](../animation_pipeline/reports/M002_R2_visual/r2-baseline-idle-0.png) | [depois](../animation_pipeline/reports/M002_R2_visual/r2-skinned-idle-0.png) |
| Idle lado | [antes](../animation_pipeline/reports/M002_R2_visual/r2-baseline-idle-90.png) | [depois](../animation_pipeline/reports/M002_R2_visual/r2-skinned-idle-90.png) |
| Idle costas | [antes](../animation_pipeline/reports/M002_R2_visual/r2-baseline-idle-180.png) | [depois](../animation_pipeline/reports/M002_R2_visual/r2-skinned-idle-180.png) |
| Jog frente | [antes](../animation_pipeline/reports/M002_R2_visual/r2-baseline-jog-0.png) | [depois](../animation_pipeline/reports/M002_R2_visual/r2-skinned-jog-0.png) |
| Jog lado | [antes](../animation_pipeline/reports/M002_R2_visual/r2-baseline-jog-90.png) | [depois](../animation_pipeline/reports/M002_R2_visual/r2-skinned-jog-90.png) |
| Sword impacto | [antes](../animation_pipeline/reports/M002_R2_visual/r2-baseline-sword-0.png) | [depois](../animation_pipeline/reports/M002_R2_visual/r2-skinned-sword-0.png) |
| Sword lado | [antes](../animation_pipeline/reports/M002_R2_visual/r2-baseline-sword-90.png) | [depois](../animation_pipeline/reports/M002_R2_visual/r2-skinned-sword-90.png) |
| Roll frente | [antes](../animation_pipeline/reports/M002_R2_visual/r2-baseline-roll-0.png) | [depois](../animation_pipeline/reports/M002_R2_visual/r2-skinned-roll-0.png) |
| Roll lado | [antes](../animation_pipeline/reports/M002_R2_visual/r2-baseline-roll-90.png) | [depois](../animation_pipeline/reports/M002_R2_visual/r2-skinned-roll-90.png) |

Complementos: [Sword inicial](../animation_pipeline/reports/M002_R2_visual/r2-skinned-sword-early-0.png), [Sword final](../animation_pipeline/reports/M002_R2_visual/r2-skinned-sword-late-0.png), [retorno ao CPM](../animation_pipeline/reports/M002_R2_visual/r2-skinned-off-cpm.png), [primeira pessoa](../animation_pipeline/reports/M002_R2_visual/r2-skinned-first-person.png), [outro jogador](../animation_pipeline/reports/M002_R2_visual/r2-skinned-other-player.png).

Nas vistas laterais, a superfície atravessa cotovelos/joelhos sem as tampas separadas do modelo rígido. A pele continua mapeada ao membro durante a flexão. Não foram identificados buracos graves nessas vistas. Dobras extremas ainda podem comprimir volume ou fazer superfícies de roupa se cruzarem: skinning linear não resolve volume, IK ou contato de pés automaticamente.

## Compatibilidade comprovada e limites

| Área | Estado |
|---|---|
| Classic Steve 64×64 | renderizado e comparado no cliente |
| Camadas externas da malha | presentes; não homologados todos os toggles de personalização |
| Cotovelos e joelhos | deformação ponderada comprovada numérica e visualmente |
| Tronco/coluna | rígido, limitação do modelo R1; não foi recriado nesta etapa |
| Armadura, capa, elytra, offhand | não implementados neste caminho experimental |
| Slim/Alex | fallback para o caminho anterior; não homologado |
| Primeira pessoa | mãos/render anterior; sem corpo/antebraços ponderados próprios |
| Outros jogadores | réplica cliente não selecionada segue pelo renderer anterior sem cancelamento; dois clientes reais ainda pendentes |
| Shader packs, servidor dedicado, latência elevada | não homologados |
| Sombras/luz | luz do evento e sombra simples vanilla; sem novo sistema de sombras |
| Física, colisões e dano | não modificados |

## Arquivos criados e modificados

| Arquivo/grupo | Alteração |
|---|---|
| `experiments/m002_r2/java/.../SkinnedSteveRenderer.java` | evento Pre, instância Polymesh, seleção de clipe, attachment, fallback e limpeza |
| `.../RenderLabCommands.java` | comandos opt-in de laboratório |
| `.../R2ModelAudit.java` | auditoria do modelo carregado e comparação de pesos com osso dominante |
| `.../R2VisualTest.java` | captura automática, câmera, réplica e observação de sincronização de combate |
| `animation_pipeline/scripts/prepare-r2.cjs` | backup, hashes, cópia exata e inspeção do GLB |
| `animation_pipeline/scripts/validate-r2.cjs` | preservação e compensação de pulso |
| `scripts/start-r2.ps1` | download oficial verificado e inicialização isolada |
| `experiments/m002_r2/COMPARE.html` | comparação de capturas locais |
| `build.gradle` | perfil condicional, dependência e diretório separados; exclusão de Full Motion |
| `.gitignore` | exclusão de `run-r2/`; fontes geradas já cobertas por `converted/` |
| README/índice/relatório/resultados | documentação e evidências |

Nenhum arquivo de `src/main/java` foi modificado. Modelos aprovados, Godot, animações e fontes do R1 permanecem preservados.

## Resultados e desempenho

- Comparação CPM: 15/15 checks no cliente, incluindo confirmação de pose CPM ativa.
- Malha ponderada: 17/17 checks, com vistas, arma única, erros OpenGL iguais a zero, fallback, reprodução embutida e réplica não selecionada.
- Na execução visual final, 2.219 desenhos tiveram média de aproximadamente **0,512 ms de CPU** por chamada e máximo de **8,691 ms**, incluindo emissão de vértices/item. Essa medida não é tempo total de GPU nem benchmark de muitos jogadores. O teste usou limite de 60 FPS e distância 5.
- Métricas de bind/pesos vêm do modelo efetivamente carregado pela biblioteca. Capturas foram inspecionadas, não apenas geradas.

Resultados estruturados e verificação de combate são consolidados no encerramento abaixo.

## Ocorrências e recomendação

Houve uma falha inicial de sintaxe da classe experimental, corrigida antes da execução. A variante dev da biblioteca expôs um problema de classloader para jgltf; o runtime oficial remapeado com jar-in-jar resolveu o carregamento. As primeiras capturas também motivaram correção de escala, câmera e matriz normal. As imagens finais foram recapturadas e a baseline exige confirmação do parâmetro CPM para evitar comparar uma pose não carregada.

O backend se mostrou viável para este Steve pequeno e para as quatro animações. Recomenda-se manter a separação entre **estado/autoridade de combate no servidor** e **backend visual selecionável no cliente**, conservando CPM como fallback. Não é necessário substituir o projeto por um renderizador inteiramente novo.

Antes de adoção definitiva: avaliar coluna articulada sem refazer o personagem inteiro, volume em flexões extremas, contato dos pés, armadura/capas/offhand, seleção de camadas, primeira pessoa e servidor remoto. A naturalidade do retargeting original ainda limita o resultado. Não houve avanço para M002.2 ou substituição definitiva do CPM.

## Encerramento da validação

| Verificação executada | Resultado |
|---|---|
| Baseline CPM com poses confirmadas | 15/15 |
| Render ponderado, vistas, arma única, GL, fallback e réplica | 17/17 |
| Suíte funcional M002.0 com malha ativa | **132/132** |
| Impactos comparados ao clipe renderizado | **18/18**, `UAL_sword` entre 0,186133 e 0,245117 s; quatro ticks no servidor |
| Erros do renderizador durante combate | zero |
| Testes Java de regras de combate | 18/18 |
| Testes anteriores de arbitragem e locomoção | passaram |
| Build R2 | `BUILD SUCCESSFUL in 18s` |
| Build normal limpo | `BUILD SUCCESSFUL in 21s` |
| `runClient` da suíte de combate | `BUILD SUCCESSFUL in 3m 24s` |
| Preservação | 45 hashes R2 e 171 hashes Godot conferidos; cópia GLB idêntica |
| Inspeção do JAR normal | sem classes R2, Polymesh, GLB ou JSON de poses R2 |

Na caminhada do teste de combate, referência e ataque mediram os mesmos **8,63060733762779 blocos**. O dano, seleção de alvo, paredes, alcance, invulnerabilidade vanilla, spam, cancelamento, Roll e reconexão passaram sem alteração do código principal. Durante essa execução houve 7.763 desenhos antes do encerramento do observador, média de 0,471 ms de CPU por chamada e zero erros de render. A janela de sincronização é observação do servidor integrado, não garantia de latência remota.

Evidências:

- [Baseline CPM](../animation_pipeline/reports/M002_R2_BASELINE_RUNTIME.json).
- [Verificações do render ponderado](../animation_pipeline/reports/M002_R2_SKINNED_RUNTIME.json).
- [132 resultados de combate](../animation_pipeline/reports/M002_R2_COMBAT_RUNTIME.json).
- [Amostras de sincronização visual](../animation_pipeline/reports/M002_R2_COMBAT_RENDER_TRACE.json).
- [Combate com malha ativa](../animation_pipeline/reports/M002_R2_visual/combat-iron-front.png) e [fase anterior ao impacto](../animation_pipeline/reports/M002_R2_visual/combat-before-impact.png).

Logs locais ignorados: `.logs/r2-baseline-validated.log`, `r2-skinned-validated.log`, `r2-combat.log`, `r2-lab-build.log`, `r2-production-build.log`. O JAR de laboratório foi guardado somente em `.logs/r2-artifacts/`; ele contém os recursos privados de teste e não é uma distribuição pública. O artefato em `build/libs/` ao encerrar é o mod normal, com rollback preservado.

O laboratório está **funcional no escopo testado**, com melhoria visível das juntas, quatro animações, textura Classic, arma e retorno ao CPM. Isso não homologa tronco elástico, todos os frames/skins/equipamentos, primeira pessoa ponderada ou multiplayer remoto. Nenhum cliente de teste foi deixado aberto.
