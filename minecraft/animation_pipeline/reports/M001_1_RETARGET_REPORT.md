# M001.1 — Steve articulado / UAL1 Idle_Loop

Data: 2026-11-12, America/Sao_Paulo.

**Resultado: funcional no teste local realizado.** O cliente Forge mostrou Steve clássico com o Idle importado, em terceira pessoa. Capturas em fases diferentes e amostras das rotações do modelo carregado confirmam mudança de pose. O teste não se limita ao resultado do build. O modelo continua experimental, com as limitações abaixo; M001.2 não foi iniciado.

## Ferramentas e diagnóstico prévio

- Minecraft 1.20.1 / Forge 47.4.28 / Microsoft OpenJDK 17.0.20.1 x64.
- Gradle 8.8, wrapper MDK; ForgeGradle 6.0.54.
- CPM Forge `CustomPlayerModels-1.20-0.6.26a.jar`, mod version `0.6.26a`. Compatibilidade 1.20/1.20.1 confirmada nos metadados oficiais usados no M000. SHA-512 conferido novamente com o registro M000.
- Node.js 24.16.0, sem pacotes adicionais. Nenhuma ferramenta foi instalada. Blender 5.2 está disponível, mas não foi executado; Blockbench não foi necessário.

O CPM não estava carregado no M000. Agora está em `implementation fg.deobf('local:CustomPlayerModels:1.20-0.6.26a')`, resolvido exclusivamente do JAR oficial em `dependencies/cpm/`; não é incorporado ao JAR Tower of Entropy. Os metadados do mod declaram CPM obrigatório.

A primeira inicialização encontrou uma falha de refmap Mixin: referências SRG de um mod de distribuição não correspondiam aos nomes Mojang do userdev. Corrigido com `mixin.env.remapRefMap=true`, `mixin.env.refMapRemappingFile=build/createSrgToMcp/output.srg` e dependência da tarefa `createSrgToMcp` em runClient. O cliente registrou **Customizable Player Models Initialized** e a API inicializada antes de qualquer conversão.

O verificador de versões do CPM informou uma versão posterior; a versão solicitada foi preservada. Avisos de Realms em sessão dev não foram contornados e nenhuma autenticação foi alterada.

## Referências investigadas e escolha do fluxo

- [API oficial CPM](https://github.com/tom5454/CustomPlayerModels/wiki/API-documentation): plugin Forge `@CPMPlugin` / `ICPMPlugin`; recebimento de `ICommonAPI`; aplicação de `ModelFile` por `setPlayerModel` e restauração por `resetPlayerModel`. As chamadas foram compiladas e executadas contra o JAR instalado.
- [Mosaicary](https://github.com/MopicMP/mosaicary): investigados README, limites e construção de `.cpmproject`, incluindo esquema de frames e interpolação. Seu importador completo reconstrói a malha da origem em cubos; não foi executado sobre o manequim UAL para substituir Steve. Não foi instalado o plugin nem enviado asset a um serviço externo.
- [Plugin oficial CPM para Blockbench](https://github.com/tom5454/CustomPlayerModels/tree/master/Blockbench): suporta projetos CPM, com aviso de beta e possíveis problemas de importação de animações. Não foi necessário utilizá-lo: o projeto foi carregado e o binário exportado pelo editor do próprio CPM no Minecraft.
- Checkout de investigação CPM: `129b76fc7561fb0947107fe45a954881ac774921`; Mosaicary: `f0433c8759f1f873120c602d2c06d5a0fe153ed0`. São referências de investigação de source atual, não substituições do runtime `0.6.26a`.

## Fonte e inspeção do Idle

Fonte: `animation_pipeline/input/UAL1/UAL1_Standard.glb`; SHA-256 `69591853d817488edaa8fd9bf8fc1d821eaeaf789f8627b3cd23b41c4ed67997`.

O clipe real `Idle_Loop` tem 2,5 segundos, 195 canais (posição, quaternion e escala dos 65 joints), 76 instantes de 0 a 2,5 s, espaçamento aproximadamente 1/30 s e interpolação LINEAR. O GLB não declara uma taxa de reprodução de engine: 30 FPS é inferido dos tempos dos keyframes. A skin é `Armature`; a cena contém `Armature`, que é pai de `root`. Hierarquia integral está em `asset-metadata.json` do M001.0.

`root.translation` permanece `[0,0,0]`. A translação de pelvis varia em coordenadas locais aproximadamente X=-0,002462..0,005351, Y=0,086232..0,088223, Z=0,868658..0,877172. Essas coordenadas não são posições do jogador Minecraft. Nenhuma variação de escala relevante foi encontrada. Canais, contagens, mínimos/máximos e mapeamento constam em [M001_1_SOURCE_ANALYSIS.json](M001_1_SOURCE_ANALYSIS.json).

## Modelo e mapeamento

Cabeça 8×8×8, tronco 8×12×4 e quatro membros 4×12×4, em pixels de modelo. Cada membro foi dividido em dois cubos de 4×6×4, com uma segunda camada de skin por segmento: 6 raízes vanilla e 16 cubos adicionais. As peças originais inteiras dos quatro membros ficam ocultas para evitar duplicação. Não há vértices, malha, textura ou proporções do manequim humanoide da UAL no Steve.

As UVs laterais preservam as regiões originais das skins 64×64 Classic, divididas em duas faixas de seis pixels. Cabeça e tronco continuam vanilla, com suas camadas. Cotovelos estão a seis pixels do topo do braço e joelhos a seis pixels do topo da perna. As juntas preservam pivôs e relações pai/filho, sem alongar os membros.

| Origem UAL1 | Destino Steve | Tratamento |
|---|---|---|
| `Head` (inclui ancestrais) | `head`, ID 0 | orientação global relativa ao bind |
| `spine_03` (inclui pelvis e coluna) | `body`, ID 1 | coluna condensada no tronco rígido |
| `upperarm_l` / `upperarm_r` | `left_arm` / `right_arm`, IDs 2/3 | alinhamento do eixo do braço ao vetor até lowerarm |
| `thigh_l` / `thigh_r` | `left_leg` / `right_leg`, IDs 4/5 | alinhamento ao vetor até calf |
| `lowerarm_l` / `lowerarm_r` | cotovelos, IDs 102/103 | orientação relativa ao braço, só flexão X |
| `calf_l` / `calf_r` | joelhos, IDs 104/105 | orientação relativa à coxa, só flexão X |
| `root`, `pelvis` e demais ancestrais | contribuição às orientações | translações de origem descartadas |
| dedos, mãos, pés e demais joints | sem peça independente | não geram geometria realista; movimentos distais não representados |

## Conversão executada

1. Leitura direta dos accessors GLB e da hierarquia; composição das orientações e posições bind/globais.
2. Amostragem de posições e escalas LINEAR e quaternions por SLERP; somente `Idle_Loop` é convertido.
3. Mudança de base por rotação de 180° em X, de Y-up/Z-forward para o espaço de modelo Y-down/Z-back. Correção da orientação bind de cada segmento usando seu eixo até o filho.
4. Conversão dos quaternions para Euler ZYX, convenção confirmada na classe `Rotation` do CPM. Cotovelos/joelhos mantêm a flexão, descartando torção axial e rotação lateral incompatíveis com as peças rígidas.
5. Reposicionamento dos pivôs de tronco, cabeça e ombros ao redor da cintura conforme a rotação do tronco, para manter as peças conectadas. Não há escrita de posição, velocidade ou escala da entidade Minecraft.
6. 75 frames uniformes a 30 Hz, com endpoint duplicado excluído; duração CPM=2500 ms, `loop=true`, `linear_loop`, estado vanilla `STANDING`. A diferença angular entre endpoints da fonte é cerca de 0,312°, não uma continuidade perfeita presumida.
7. Normalização de todos os ângulos para [0,360): o escritor CPM usa valores sem sinal. Um teste intermediário detectou perda de ângulos negativos; a representação foi corrigida e conferida novamente nas rotações em runtime.
8. Geração de ZIP `.cpmproject` com config e uma animação. Carregamento por `ProjectIO.loadProject`, validação de duração/frames/loop, exportação por `Exporter.exportModel`, releitura por `ModelFile.load` e cópia para `run/player_models/`.

Não é uma reprodução 1:1 do manequim: posição/escala da origem, dedos e torções das juntas foram adaptados ou descartados deliberadamente. Os ângulos e as variações temporais vêm da animação original; não foram substituídos por um Idle genérico.

## Arquivos gerados

Em `animation_pipeline/converted/m001_1/`, ignorado pelo Git:

| Arquivo | Uso | Tamanho |
|---|---|---:|
| `steve-ual1-idle.cpmproject` | projeto editável que usa a skin do jogador | 26.093 bytes |
| `steve-ual1-idle.cpmmodel` | modelo sem textura fixa, para skins Classic | 9.867 bytes |
| `steve-ual1-idle-preview.cpmproject` | projeto de prévia Steve com textura vanilla local | 23.171 bytes nesta execução |
| `steve-ual1-idle-preview.cpmmodel` | prévia Steve usada no teste visual | 12.170 bytes |
| `retarget-data.json` | dados intermediários para validação | gerado pelo script |

SHA-256 do modelo de skin variável: `9f5c560be8d9b61d3bd9e2494e60ea9de2191997de9d0e231533769e7103b74c`.
SHA-256 da prévia Steve: `90e202f0c4eafbcef82eb10974de18d1bf85c82440a34988b659c54d3ce9f815`.

Ambos também estão em `run/player_models/` com prefixo `toe-`. O usuário Dev recebe outra skin padrão no vanilla; por isso a prévia Steve é separada. Sua textura vem do recurso `minecraft:textures/entity/player/wide/steve.png` do cliente instalado, sem substituir skin da conta, autenticação ou arquivos Mojang. A versão principal continua usando a skin do jogador.

Scripts próprios: `retarget-idle.cjs` e `validate-idle.cjs`. Java mínimo de integração: `ToeCpmPlugin`, `AnimationTestCommands`, `ClientModelPreparation` e `ClientIdleSmokeTest`. Os dois últimos são diagnósticos desativados sem parâmetros explícitos; não criam nem abrem mundos no runClient normal.

## Testes e resultado visual

Veja [TEST_LOG_M001_1.md](../../docs/TEST_LOG_M001_1.md) e [validação estrutural](M001_1_VALIDATION.json).

As capturas são do Minecraft rodando, não de um preview sintetizado:

- [Steve / fase 0](M001_1_visual/idle-0.png)
- [Steve / fase 13 ticks depois](M001_1_visual/idle-13.png)
- [Desativado / skin vanilla anterior restaurada](M001_1_visual/off.png)

Visualmente: cabeça, tronco, braços cúbicos e skin Steve preservados; mudanças sutis de cabeça/ombros/braços entre fases; sem manequim realista ou erro grave de renderização observado nessas vistas. Folhagem encobre parte das pernas/pés; não foi feita uma inspeção completa de todos os ângulos e contatos.

Em runtime, o modelo registrou `state=LOADED`, `error=null`, `blocked=null`, um trigger STANDING, e o braço esquerdo variou de aproximadamente `(13,968°,19,655°,-17,784°)` para `(8,686°,21,353°,-16,615°)`. A posição permaneceu exatamente estável durante o Idle; o comando off restaurou o modelo anterior. Inventário Creative abriu e a movimentação vanilla respondeu no teste seguinte.

## Limitações e distribuição

- Retarget experimental: braços Classic de 4 pixels; skins Slim/Alex e skins legadas 64×32 não foram validadas. Não há detecção automática de largura Slim.
- Não foi validada a alternância das camadas de roupa dos cubos por todos os controles de personalização de skin. Armadura, itens empunhados, cape, primeira pessoa e multiplayer remoto precisam de testes próprios.
- Não há IK, dedos, articulações de pé/mão, deformação suave ou compensação de contato no chão. Contatos e interseções pequenas de cubos podem exigir ajuste após inspeção de outros ângulos.
- Apenas Idle STANDING. Não foram integrados Run, Roll, ataques ou transições de combate. A prova de movimentação foi feita após off; não é prova de todas as transições com o modelo ativo.
- O exportador usa classes internas reais do CPM, além da API pública de aplicação/restauração. Atualizar CPM exige revalidar esse exportador.
- Mundo isolado em `run/saves/New World`; não há alterações no projeto Godot. Os 171 arquivos inventariados mantiveram SHA-256.
- Fontes/conversões/modelos e a textura Mojang de prévia permanecem locais e ignorados; não foram publicados nem embutidos no JAR do mod. UAL1 declara CC0/Quaternius; não atribuir essa licença ao CPM ou aos assets Mojang. Human Archer e UAL2 não foram convertidos neste marco.
