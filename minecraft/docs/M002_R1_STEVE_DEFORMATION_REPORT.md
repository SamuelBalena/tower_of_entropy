# M002.R1 — Steve: diagnóstico e provas de deformação

Data: 2026-11-12, America/Sao_Paulo. Minecraft 1.20.1, Forge 47.4.28, JDK 17, CPM 0.6.26a.

**Resultado: prova de conceito e diagnóstico entregues; correção elástica definitiva ainda não adotada.** Há uma correção conservadora de superfícies CPM, um adaptador experimental UAL→Full Motion e uma referência Blender/GLB de malha contínua com pesos. As imagens não sustentam considerar o personagem atual corrigido no padrão visual desejado. O combate e os modelos aprovados permanecem intactos.

## Inspeção, backup e isolamento

Foram inspecionados `retarget-idle.cjs`, `retarget-actions.cjs`, `actions-data.json`, projetos/binários CPM, exportadores nativos e APIs reais dos JARs. Não foi executada a conversão antiga novamente.

- [Baseline SHA-256](../animation_pipeline/reports/M002_R1_BASELINE.json): **42 arquivos** de código principal e dados/projetos/modelos dos marcos M001.1–M001.3. Cópias em `.downloads/m002_r1/backup/`.
- Código experimental em `experiments/m002_r1/java/`, incluído somente com `-PtoeR1`.
- O diretório `run-r1/` contém uma cópia do save descartável, opções, configurações e modelos locais. Os testes R1 não usam o save original em `run/`.
- Full Motion é dependência somente com `-PtoeR1FullMotion`; não foi colocado em `run/mods` nem declarado obrigatório no mod normal.
- Fontes geradas em `animation_pipeline/converted/m002_r1/`, ignoradas pelo Git, incluindo a textura Mojang extraída do cliente instalado. Nenhum asset Epic Fight foi baixado, copiado ou convertido.
- O render de comparação usa apenas uma substituição do cache de textura **local do cliente de laboratório**, para Steve Classic. Não altera GameProfile, skin de conta, autenticação ou licenciamento.

`build.gradle`, `.gitignore`, índices e documentação receberam alterações incrementais de laboratório. Não houve alteração em `src/main/java`, inclusive no código de combate. Não houve alteração no projeto Godot.

## Causas dos defeitos

### Geometria e articulações — causa estrutural comprovada

O Steve CPM possui dois cubos fechados de 4×6×4 por membro, com uma cópia de camada externa inflada em 0,25 px. Não há vértices compartilhados entre metades, pesos por vértice, malha contínua ou skinning elástico. O pivô do cotovelo fica em Y=4, pois o braço começa em Y=-2; o joelho fica em Y=6. As metades se encontram no repouso: não foi identificado um simples erro de deslocamento desses pivôs que explicasse tudo.

Ao girar uma metade, suas bordas deixam de coincidir com as bordas da outra. Para uma borda a 2 px do eixo, a separação entre pontos correspondentes pode atingir `4·sin(ângulo/2)` px — cerca de 2,83 px em 90°. Fechar cada cubo com uma tampa não cria a superfície de conexão da junta. O lado interno pode interpenetrar enquanto o externo mostra o corte. A inflação das roupas agrava o volume sobreposto e pode produzir costuras/faces concorrentes.

O retargeting também elimina Y/Z locais de antebraços/canelas para impor dobradiças X. Isso é uma simplificação cinemática, não uma transferência de pesos UAL. Pode limitar a naturalidade e descartar torção anatômica; suavizar a malha não corrige automaticamente essa limitação. Não foi declarado que toda pose UAL atual possui retargeting anatômico perfeito.

### UV e textura — causa localizada comprovada

As UVs laterais repartem corretamente as 12 linhas da skin Classic em duas faixas de seis. A imagem 64×64 não foi substituída por textura do manequim UAL. Entretanto, o gerador reutiliza faces de extremidade do membro nas tampas internas: a parte superior usa a tampa de pulso/tornozelo no corte, e a inferior reutiliza a tampa de ombro/quadril. Quando expostas, essas ilhas não representam a pele adjacente ao cotovelo/joelho.

A correção experimental altera somente essas tampas para amostrar linhas laterais contíguas ao corte e desativa a composição vanilla nas raízes enquanto as poses absolutas experimentais são aplicadas. Preserva pivôs, dimensões, UVs laterais, inflação das camadas, âncoras de itens e todos os arrays de animação.

**Limite da correção:** uma faixa de um texel preenchendo uma tampa 4×4 ainda pode apresentar repetição/alongamento visual; não é uma UV perfeita de uma junta deformável. As capturas permanecem muito semelhantes à referência nas vistas em que as tampas ficam ocultas. Essa alteração não fecha o critério de articulação suave ou ausência geral de sobreposição e não foi promovida ao pipeline aprovado.

Camadas externas foram mantidas e inspecionadas. A prova não homologa todas as combinações de botões de personalização de skin, armadura, Slim/Alex ou skins legadas 64×32.

## Referência Epic Fight

A [página solicitada](https://epicfight-docs.readthedocs.io/ko/Guides/page1/) descreve grupos de vértices/pesos, cortes de malha junto às dobras e correção das ilhas UV das regiões intermediárias. Isso pressupõe uma superfície deformável; subdividir dois cubos independentes sem ligar seus vértices não equivale ao mesmo processo.

A documentação é referência conceitual/visual. Não foram instalados Epic Fight, seu exportador, armas ou sistemas de combate. Nenhum código, modelo, textura ou animação Epic Fight foi incorporado. O gerador Blender é original e usa somente as proporções clássicas, a skin vanilla local e os dados UAL já aprovados.

## Full Motion: versão, licença e API verificadas

- [Projeto oficial](https://www.curseforge.com/minecraft/mc-mods/full-motion-animation-library).
- [Arquivo Forge 1.20.1, ID 9425804](https://www.curseforge.com/minecraft/mc-mods/full-motion-animation-library/files/9425804): `full-motion-forge-1.0.0.jar`.
- SHA-256 local: `251147913624538579837c02573b59e28414d82d54bbc2172f0b3cfe683fe358`.
- `mods.toml`: JavaFML 47+, Minecraft `[1.20.1,1.20.2)`, mod ID `fullmotion`, versão 1.0.0, licença MIT, confirmada também na [página de licença](https://www.curseforge.com/minecraft/mc-mods/full-motion-animation-library/license).
- JAR local em `.downloads/m002_r1/`, fora do versionamento e da distribuição do nosso mod. Não foi redistribuído ou incorporado por jar-in-jar.

A API pública foi inspecionada com `javap` do JDK e exercitada no cliente: `FullMotion.registerPose`, `PoseProvider`, `FullMotion.registerLocomotion`, `Clip.ofKeys`, `LimbSample`, `claimArm`. Não foi encontrado importador direto de GLB/FBX: é necessário adaptar as transformações. O laboratório implementa um `PoseProvider` próprio a partir das poses CPM retargetadas, com quatro membros e corpo/cabeça, sem copiar a implementação Full Motion.

O renderizador do JAR constrói partes `upper/lower` e `upperOuter/lowerOuter`, usando ModelPart. **Também são duas partes rígidas**, não uma malha com pesos contínuos. O fornecedor anuncia suavização de transições e animações nativas; isso não prova deformação elástica nem blending automático de nosso provedor UAL. O laboratório não assume essas capacidades.

### Compatibilidade observada

| Item | Resultado |
|---|---|
| Forge 47.4.28 / Java 17 | carregou e executou os quatro testes de pose |
| Controle pelo nosso mod | `PoseProvider` foi consultado em cada render; contadores registrados |
| UAL1/UAL2 | poses Idle, Jog, Sword e Roll renderizadas após adaptação; não houve importação nativa GLB |
| Skin Steve | textura vanilla 64×64 e modelo Classic confirmados no cliente de laboratório |
| Cotovelos/joelhos | articulam, mas continuam metades rígidas; não atingem o alvo elástico |
| Tronco | rotaciona como parte rígida; sem deformação ponderada nesta versão/prova |
| CPM instalado, sem modelo customizado ativo | Full Motion renderizou sem crash |
| CPM com modelo customizado ativo + Full Motion | ambos ativos e provedor consultado, sem crash; **não homologado visualmente** |
| Espada | orientação visual difere da referência com âncoras CPM de pulso; integração de empunhadura ainda insuficiente |
| Combate funcional com backend Full Motion | não integrado ou homologado; código de combate foi preservado |
| Armadura, remoto, Slim e outros mods de render | pendentes |

As capturas de coexistência não demonstram uma deformação combinada correta. Ambos os sistemas controlam regiões do mesmo personagem; diferenças de visibilidade, item e sobreposição permanecem uma área de conflito. Não foi afirmado que toda dupla superfície produz z-fighting visível em cada captura. Carregamento simultâneo e parâmetros ativos são evidência de execução, não de compatibilidade aprovada.

O JAR contém também recursos de locomoção/slide/dive. Ele foi mantido somente no laboratório; as opções de slide/dive ficaram desabilitadas e os testes não acionaram essas entradas. Nenhuma mecânica desse pacote foi adotada no projeto normal.

## Comparação visual

Abra [COMPARE.html](../experiments/m002_r1/COMPARE.html): seleciona movimento e ângulo e amplia imagens sem servidor web, dependências ou imagens sintetizadas.

As quatro linhas Minecraft usam o mesmo frame UAL congelado, skin, item e câmera. Há duas vistas de cada movimento. Isso permite inspecionar juntas sem confundir diferenças de fase. Os renders Blender abaixo têm outra câmera/iluminação e **não são capturas de Minecraft**.

| Movimento | CPM aprovado | CPM experimental | Full Motion | CPM + Full Motion | Blender ponderado, fora do jogo |
|---|---|---|---|---|---|
| Idle | [antes](../animation_pipeline/reports/M002_R1_visual/r1-baseline-idle-55.png) | [caps/UV](../animation_pipeline/reports/M002_R1_visual/r1-fixed-idle-55.png) | [FM](../animation_pipeline/reports/M002_R1_visual/r1-fullmotion-idle-55.png) | [coexistência](../animation_pipeline/reports/M002_R1_visual/r1-conflict-idle-55.png) | [referência](../animation_pipeline/reports/M002_R1_visual/weighted-idle.png) |
| Jog | [antes](../animation_pipeline/reports/M002_R1_visual/r1-baseline-jog-55.png) | [caps/UV](../animation_pipeline/reports/M002_R1_visual/r1-fixed-jog-55.png) | [FM](../animation_pipeline/reports/M002_R1_visual/r1-fullmotion-jog-55.png) | [coexistência](../animation_pipeline/reports/M002_R1_visual/r1-conflict-jog-55.png) | [referência](../animation_pipeline/reports/M002_R1_visual/weighted-jog.png) |
| Sword | [antes](../animation_pipeline/reports/M002_R1_visual/r1-baseline-sword-55.png) | [caps/UV](../animation_pipeline/reports/M002_R1_visual/r1-fixed-sword-55.png) | [FM](../animation_pipeline/reports/M002_R1_visual/r1-fullmotion-sword-55.png) | [coexistência](../animation_pipeline/reports/M002_R1_visual/r1-conflict-sword-55.png) | [referência](../animation_pipeline/reports/M002_R1_visual/weighted-sword.png) |
| Roll | [antes](../animation_pipeline/reports/M002_R1_visual/r1-baseline-roll-55.png) | [caps/UV](../animation_pipeline/reports/M002_R1_visual/r1-fixed-roll-55.png) | [FM](../animation_pipeline/reports/M002_R1_visual/r1-fullmotion-roll-55.png) | [coexistência](../animation_pipeline/reports/M002_R1_visual/r1-conflict-roll-55.png) | [referência](../animation_pipeline/reports/M002_R1_visual/weighted-roll.png) |

O comparador inclui também as vistas frontais `*-35.png`. As deformações rígidas continuam perceptíveis nas alternativas Minecraft. No Blender, a superfície lateral do membro atravessa a junta sem tampas internas; a malha continua cúbica fora da região dobrada. Dobras extremas ainda podem comprimir volume e roupas podem colidir; não há promessa de IK, volume perfeito ou contato em todo ângulo.

## Prova Blender: malha e pesos reais

Usado o Blender 5.2.3 LTS já instalado, em modo background. Nenhum add-on ou programa foi instalado.

`blender-r1-weighted.py` gera 12 superfícies (cabeça/tronco/membros e camadas), dez ossos, seções adicionais nas juntas e pesos normalizados entre os ossos superior/inferior. As faces laterais compartilham vértices entre as seções; há tampas somente nas extremidades. UVs laterais conservam a distribuição 64×64, com amostragem nearest. O tronco permanece rígido nesta prova.

Arquivos locais:

- `animation_pipeline/converted/m002_r1/steve-r1-weighted.blend`: armature, malhas, materiais e quatro ações `UAL_idle`, `UAL_jog`, `UAL_sword`, `UAL_roll`.
- `steve-r1-weighted.glb`: uma skin, dez joints, quatro animações. Validação encontrou 736 vértices exportados com pesos normalizados, 64 com influência combinada, UVs no domínio 0–1 e durações 2,5 / 0,933333 / 0,433333 / 1,466667 s.
- [Metadados de geometria](../animation_pipeline/reports/M002_R1_WEIGHTED_MESH.json) e [validação real do GLB](../animation_pipeline/reports/M002_R1_VALIDATION.json).

Os quatro arrays CPM aprovados foram preservados exatamente e reutilizados para animar a referência, incluindo suas simplificações de dobradiça. Não foram criadas animações substitutas. O GLB permite abrir/avaliar a malha em ferramenta 3D, mas **CPM e Full Motion 1.0.0 não recebem essa malha com pesos por suas APIs testadas**. Não há renderizador novo no mod. A referência não inclui uma nova arma nem uma prova de empunhadura Minecraft ponderada.

## Como reproduzir sem modelagem manual

Na pasta `minecraft`:

```powershell
# Comparação automática: exporta cópias, abre somente o save isolado e salva capturas.
.\scripts\start-r1.ps1 -Mode baseline
.\scripts\start-r1.ps1 -Mode fixed
.\scripts\start-r1.ps1 -Mode fullmotion
.\scripts\start-r1.ps1 -Mode conflict

# Prova interativa (nenhuma edição manual de modelo necessária).
.\scripts\start-r1.ps1 -Mode fixed -Interactive
```

No laboratório interativo, use `/toe_r1 fixed idle`, `/toe_r1 fixed jog`, `/toe_r1 fixed sword`, `/toe_r1 fixed roll` e `/toe_r1 off`. `baseline` usa o modelo aprovado; os perfis `fullmotion`/`conflict` exigem iniciar o script com uma dessas opções para carregar a biblioteca. Acrescente `freeze` após o movimento para inspecionar a pose usada nas comparações. As ações Sword/Roll são limitadas; `off` restaura o render anterior. Esses comandos não existem no JAR normal.

O script exige o save descartável `run/saves/New World` preexistente e copia-o uma vez para `run-r1/saves/TOE_R1`. Não abre nem modifica outro mundo. As edições de plataforma/inventário/câmera pertencem ao laboratório. Os movimentos de prova não aplicam dano; não substituem os comandos de combate.

```powershell
node animation_pipeline/scripts/validate-r1.cjs
& 'C:\Program Files\Blender Foundation\Blender 5.2\blender.exe' --background --python animation_pipeline/scripts/blender-r1-weighted.py -- "$PWD"
```

O caminho Blender documenta a instalação verificada. A textura usada no GLB/Blend permanece local/ignorada, sem distribuição pública.

## Testes, evidências e ocorrências

- Quatro perfis: **9/9 checks cada**, incluindo exportação e oito observações de poses/câmera. Esses checks comprovam carregamento, parâmetros e execução; não são uma nota de qualidade visual.
- [CPM baseline](../animation_pipeline/reports/M002_R1_BASELINE_RUNTIME.json), [CPM experimental](../animation_pipeline/reports/M002_R1_FIXED_RUNTIME.json), [Full Motion](../animation_pipeline/reports/M002_R1_FULLMOTION_RUNTIME.json), [coexistência](../animation_pipeline/reports/M002_R1_COEXISTENCE_RUNTIME.json).
- Reprodução interativa por comandos reais: **8/8**, incluindo Idle/Jog, Sword/Roll, parâmetros zerados ao terminar e `off`. [Resultados](../animation_pipeline/reports/M002_R1_PLAYBACK_RUNTIME.json).
- Testes Java anteriores de combate (18 regras), arbitragem e locomoção passaram.
- Build limpo do projeto normal, sem R1/Full Motion: `BUILD SUCCESSFUL in 19s`. Regressão original M001.3 na cópia isolada: **37/37**. [Resultados](../animation_pipeline/reports/M002_R1_PRODUCTION_REGRESSION.json). O código de combate não foi alterado; a suíte completa funcional M002.0 não foi repetida nesta revisão.
- A inspeção ZIP confirmou ausência de `R1Laboratory`, Full Motion, GLB/Blend e modelos CPM no JAR normal. A regressão encerrou com `BUILD SUCCESSFUL in 2m 55s`. As 32 capturas Minecraft, quatro renders Blender, links locais e sintaxe do launcher/comparador foram conferidos.
- A validação final confirmou os 42 hashes da baseline R1, os nove arquivos aprovados da baseline M001.3 e os 171 arquivos Godot inventariados. Nenhum cliente de teste foi deixado aberto.
- Build experimental com Full Motion passou. Os quatro clientes encerraram normalmente; logs locais ignorados `.logs/r1-*.log`.
- As primeiras capturas de laboratório foram rejeitadas: uma corrida reset→set permitia resposta de skin sobrescrever o modelo, e um nome de pose de teste estava incorreto. O laboratório passou a aplicar modelos sem reset concorrente e verificar pose CPM=1. As imagens válidas desta documentação foram recapturadas; as primeiras não são evidência de deformação.
- Uma primeira validação GLB detectou início em 1/30 s. A exportação passou a deslocar o início de cada ação a zero, conservando o intervalo original; o validador então passou.
- O carregamento de uma textura local sem dados CPM pode gerar aviso do carregador de modelo de skin no laboratório. Modelos explícitos e provedor foram verificados depois; esse aviso não foi tratado como prova de incompatibilidade nem motivou alteração de autenticação.

## Recomendação e trabalho restante

**Manter CPM e o combate aprovados como padrão. Não adotar Full Motion 1.0.0 como correção elástica.** A biblioteca é executável e controlável, mas seu modelo de duas metades rígidas não resolve a causa estrutural; empunhadura e dupla propriedade do render exigiriam adaptação adicional.

Para atingir o alvo semelhante ao Epic Fight, a referência demonstra a necessidade de uma superfície contínua com pesos e UVs de dobra, mais uma ponte de item no pulso. O próximo passo técnico seria avaliar um backend existente que consuma malhas ponderadas, usando o GLB/Blend gerado como fixture. Nenhum backend novo foi implementado nesta revisão.

Antes de uma adoção definitiva: validar pesos/dobras extremas de todos os frames, reconstruir torção anatômica quando necessário, verificar camadas/armadura e empunhadura contínua, resolver exclusividade entre renderizadores e sincronizar o backend escolhido ao início/término autorizado pelo servidor de combate. Também faltam avaliações remotas, primeira pessoa, Slim e terrenos variados.

Esta revisão não modifica dano, estado de combate, física ou mecânicas. Não avança para outro marco. A melhoria visual definitiva permanece pendente de uma escolha de renderização adequada e de homologação visual.
