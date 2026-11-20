# Tower of Entropy — inventário M001.0

Data: 2026-11-12, America/Sao_Paulo. Escopo: preparar fontes para uma integração futura com Minecraft Forge 1.20.1 e CPM. Nenhuma conversão ou implementação foi realizada.

## Localização e procedência

Projeto Godot original identificado em `C:/Users/balen/Desktop/tower-of-entroy`, com `project.godot`, `assets/source/` e `assets/prototypes_3d/`. Essa pasta foi acessada somente para leitura; Godot não foi iniciado.

As três bibliotecas foram encontradas primeiro em `assets/source/` do original:

| Pacote | Caminho relativo ao projeto Godot | Formatos encontrados |
|---|---|---|
| UAL1 | `assets/source/Universal Animation Library[Standard]/` | GLB e FBX, variantes Standard e Standard_RM, README e licença |
| UAL2 | `assets/source/Universal Animation Library 2[Standard]/` | GLB e FBX Standard/Standard_RM; manequim feminino GLB/FBX/BLEND; README e licença |
| Human Archer | `assets/source/Human Archer Animations FREE/` | 142 FBX (136 com AnimationStack e seis modelos/props), ZIP com fontes Blender, PDF, PNG e .gdignore |

Os dois caminhos solicitados foram confirmados:

- `assets/prototypes_3d/ual1_standard.glb`: 7.618.436 bytes, 43 clipes. SHA-256 idêntico ao UAL1 Standard da pasta source; não foi copiado novamente.
- `assets/source/Universal Animation Library 2[Standard]/Unreal-Godot/UAL2_Standard.glb`: 8.091.444 bytes, 43 clipes.

A pasta atual `towerofentropy/assets/source/` também contém UAL1 e UAL2 em pastas duplicadas, por exemplo `Universal Animation Library[Standard]/Universal Animation Library[Standard]/`. Elas não foram alteradas nem usadas como fonte das cópias. Human Archer foi localizado no projeto Godot original.

## Cópias mínimas e estrutura criada

```text
minecraft/animation_pipeline/
  README.md
  input/
    .gitkeep
    UAL1/{UAL1_Standard.glb, License.txt, README.txt}
    UAL2/{UAL2_Standard.glb, License.txt, README.txt}
  converted/
    .gitkeep
  scripts/
    inspect-assets.cjs
  reports/
    M001_ASSET_INVENTORY.md
    asset-metadata.json
```

Apenas dois GLB e quatro textos de procedência foram copiados: 15.711.948 bytes no total. Os GLB já são contêineres com vários clipes; separar os quatro clipes exigiria extração/conversão, fora deste marco. Não foram copiados FBX, variantes RM, manequim feminino, ZIP ou Human Archer.

As cópias foram comparadas por SHA-256 com os originais. `minecraft/.gitignore` ignora todo o conteúdo de `input/` e `converted/`, exceto `.gitkeep`. Scripts e relatórios permanecem disponíveis para versionamento. Não houve commit, publicação ou inclusão de arquivos de terceiros no JAR.

| Cópia de trabalho | SHA-256 |
|---|---|
| `input/UAL1/UAL1_Standard.glb` | `69591853d817488edaa8fd9bf8fc1d821eaeaf789f8627b3cd23b41c4ed67997` |
| `input/UAL2/UAL2_Standard.glb` | `8cee20ab1bc55130092447e810e26df22dd2803eccc54f52137a7d54d7ab88a8` |

## Quatro movimentos selecionados para o primeiro teste futuro

| Movimento | Biblioteca / arquivo de trabalho | Nome real do clipe | Duração |
|---|---|---|---:|
| Idle | UAL1 / `input/UAL1/UAL1_Standard.glb` | `Idle_Loop` | 2,500000 s |
| Run — corrida leve | UAL1 / `input/UAL1/UAL1_Standard.glb` | `Jog_Fwd_Loop` | 0,933333 s |
| Dodge/Roll | UAL1 / `input/UAL1/UAL1_Standard.glb` | `Roll` | 1,466667 s |
| Ataque de espada | UAL2 / `input/UAL2/UAL2_Standard.glb` | `Sword_Regular_A` | 0,433333 s |

Seleção funcional inferida dos nomes; não houve reprodução visual. Não existe clipe literalmente chamado `Run` no UAL1 Standard. `Jog_Fwd_Loop` foi escolhido como corrida inicial, com `Sprint_Loop` (0,666667 s) como alternativa mais rápida. UAL1 também oferece `Sword_Attack` (1,533333 s). UAL2 oferece `Sword_Regular_A_Rec` (0,966667 s), B/C e combos; a recuperação não integra os quatro movimentos selecionados e será avaliada somente em fase futura. Não se deve presumir que o ataque A sozinho seja um ciclo completo de ataque e retorno.

## Formatos, tempos e esqueletos

Os GLB são glTF binário 2.0, gerados pelo exportador Khronos glTF Blender I/O v4.5.48 nos dois arquivos principais. Cada GLB principal tem uma skin `Armature` com 65 joints. A raiz de cena é `Armature`; `root` é filho de `Armature`. O campo opcional `skin.skeleton` não está declarado: a hierarquia foi recuperada dos filhos dos nodes, sem inventar uma referência ausente.

UAL1 e UAL2 compartilham os mesmos nomes e relações de parentesco dos 65 joints nos arquivos inspecionados. Isso facilita a comparação, mas não comprova equivalência de bind pose, proporções, comportamento de animação ou compatibilidade com CPM. A lista integral e as transformações locais declaradas estão no JSON. O manequim feminino da UAL2 tem 65 joints e zero clipes; não é uma biblioteca adicional de movimentos.

As durações GLB foram calculadas lendo os valores float32 dos accessors de tempo dos samplers: máximo menos mínimo, em segundos. A tabela ao final contém todos os clipes dos dois arquivos principais. O JSON registra início, fim, canais, interpolação, paths e nodes animados, além das variantes RM e da cópia de protótipo. `_Loop` é parte do nome, não uma comprovação visual de continuidade do ciclo; glTF não fornece aqui um loop de engine validado.

Human Archer usa FBX binário; versão 7300 verificada em `HumanM@Idle01`. Nesse arquivo o rig possui 56 nós do tipo LimbNode. Nomes reais incluem `B-root`, `B-hips`, `B-spine`, `B-chest`, `B-spineProxy`, `B-neck`, `B-head`, `B-jaw`, `B-shoulder.L`, `B-upperArm.L`, `B-forearm.L`, `B-hand.L`, `B-handProp.L`, `B-thigh.L`, `B-shin.L`, `B-foot.L` e `B-toe.L`, com equivalentes `.R` e dedos. A hierarquia completa de cada FBX está no JSON; não foi presumida uma única estrutura para todos os props e arquivos.

Foram lidos os nomes AnimationStack dos 136 FBX de animação. Durações usam LocalStart/LocalStop quando presentes ou `Takes/Take/LocalTime`, com 46.186.158.000 ticks por segundo. Exemplos verificados: `HumanM@Idle01` = 2,7 s e `HumanM@Run01_Forward` = 0,6 s. O inventário JSON inclui nomes e durações dos demais FBX. O ZIP Blender foi inventariado e recebeu hash, sem extração ou leitura de Actions; o PDF foi identificado como documentação, sem afirmar que seus termos foram extraídos.

## Possíveis problemas de retargeting — análise, não implementação

1. **Rig e aparência:** UAL usa 65 joints, Archer usa nomenclatura e hierarquia diferentes. O Steve tradicional e o futuro rig CPM precisam de um mapeamento explícito. Joelhos, cotovelos, dedos e coluna segmentada não têm correspondência automática com as peças tradicionais do Steve. Nenhuma mudança de geometria ou skin foi feita.
2. **Pose de repouso e pivôs:** preservar inverse bind matrices, rotações locais e pivôs. O joint `root` dos GLB declara quaternion `[-0.707106769, 0, 0, 0.707106769]`; copiar Euler de um osso nominalmente parecido pode aplicar um giro incorreto. Não foi realizado cálculo de retargeting.
3. **Eixos e unidades:** glTF e FBX exigem tratamento separado. O FBX Archer de amostra declara UpAxis=2, FrontAxis=1/Sign=-1 e UnitScaleFactor=1. Não presumir a mesma orientação, escala ou unidade que será usada no CPM. As propriedades brutas estão no JSON.
4. **Root motion:** README dos UAL declara que `_RM` incorpora root motion e Standard o desabilita. As cópias escolhidas são Standard. Isso não prova ausência de deslocamentos locais em pelvis/root; amplitudes e trajetória ainda não foram medidas. Nenhum deslocamento de personagem foi implementado.
5. **Proporções e contatos:** braços/pernas, empunhadura de espada, posição das mãos e contato com chão podem exigir ajustes para Steve; preservar aparência não implica preservar proporções do manequim. Não foram importados modelos de armas ou personagens.
6. **Tempos e continuidade:** não assumir equivalência entre frequência dos keyframes e ticks Minecraft. Será necessário avaliar interpolação, resampling e costuras de loop numa fase autorizada. `Sword_Regular_A` possui recuperação separada.
7. **Formato de destino:** estes GLB/FBX são fontes de trabalho, sem garantia de carregamento direto no CPM. Não foi escolhido ou executado exportador, conversor ou API de integração.

## Licenças e distribuição

**UAL1 e UAL2:** `License.txt` e README locais identificam Quaternius e CC0 1.0 Universal / Public Domain Dedication. Os textos foram preservados ao lado de cada cópia local. Referência declarada pelos pacotes: https://creativecommons.org/publicdomain/zero/1.0/. A identificação usa os textos efetivamente existentes nos pacotes; não atribui CC0 aos outros assets do projeto.

**Human Archer:** autoria Kevin Iglesias. Não foi encontrado License.txt independente na pasta local; o PDF é documentação de clipes. A [página oficial do pacote](https://kevdev.itch.io/human-archer-animations-free) confirma o produto. A [licença no site do autor](https://www.keviniglesias.com/#license), consultada em 2026-11-12, distingue Unity Asset Store EULA de licença direta para itch.io/Gumroad; autoriza uso em produtos e restringe redistribuição avulsa/reutilizável e compartilhamento fora da entidade licenciada. Não é CC0. A origem exata do download local e os termos vigentes na aquisição não foram comprovados; devem ser confirmados antes de distribuir qualquer derivado. Human Archer não foi copiado nem integrado neste marco.

Nenhuma fonte foi publicada. O ignore é uma proteção de versionamento, não uma autorização de redistribuição. As cópias UAL em `towerofentropy/assets/source/`, fora da pasta Minecraft, são preexistentes e não são cobertas pelo `.gitignore` interno de Minecraft: não publicar a pasta superior inteira como se todo o conteúdo fosse código próprio.

## Verificações e limites

- Script de inspeção executado com Node.js v24.16.0, sem pacotes externos; 171 arquivos catalogados nas três bibliotecas e no protótipo solicitado.
- GLB: 43 clipes em UAL1 e 43 em UAL2, mais variantes RM; nomes e tempos obtidos dos arquivos.
- FBX: 136 AnimationStacks encontrados nos 142 FBX Human Archer; nomes, tempos e ossos obtidos dos arquivos, sem conversão.
- Cópias mínimas verificadas por SHA-256. A verificação final de preservação e ignore está registrada abaixo.
- Python/py no PATH apontaram para executáveis indisponíveis no ambiente. Foi usado Node já instalado; nenhuma ferramenta foi instalada.
- Não foram iniciados Godot, Blender, Forge ou CPM. Não houve novo build: o código do mod e a configuração de compilação não mudaram.
- Sem conversão, retargeting, importação, reprodução visual, gameplay, alteração de autenticação, commit ou avanço de fase.

## Anexos gerados dos arquivos

As tabelas a seguir são metadados de inventário, sem keyframes completos ou recursos visuais de terceiros.

### Preserva??o final

Os 171 arquivos inventariados foram relidos ao final: todos os SHA-256 continuam id?nticos ao registro inicial. As seis c?pias de trabalho tamb?m coincidem com seus originais. Nenhum arquivo inventariado do Godot foi modificado.

### UAL1 ? todos os 43 clipes do Standard

| Nome real | Dura??o (s) | Canais | Interpola??o |
|---|---:|---:|---|
| `A_TPose` | 2.500000 | 195 | LINEAR |
| `Crouch_Fwd_Loop` | 2.000000 | 195 | LINEAR |
| `Crouch_Idle_Loop` | 2.933333 | 195 | LINEAR |
| `Dance_Loop` | 1.000000 | 195 | LINEAR |
| `Death01` | 2.400000 | 195 | LINEAR |
| `Driving_Loop` | 1.666667 | 195 | LINEAR |
| `Fixing_Kneeling` | 5.200000 | 195 | LINEAR |
| `Hit_Chest` | 0.333333 | 195 | LINEAR |
| `Hit_Head` | 0.433333 | 195 | LINEAR |
| `Idle_Loop` | 2.500000 | 195 | LINEAR |
| `Idle_Talking_Loop` | 2.933333 | 195 | LINEAR |
| `Idle_Torch_Loop` | 1.266667 | 195 | LINEAR |
| `Interact` | 2.000000 | 195 | LINEAR |
| `Jog_Fwd_Loop` | 0.933333 | 195 | LINEAR |
| `Jump_Land` | 1.266667 | 195 | LINEAR |
| `Jump_Loop` | 2.500000 | 195 | LINEAR |
| `Jump_Start` | 1.333333 | 195 | LINEAR |
| `PickUp_Table` | 0.833333 | 195 | LINEAR |
| `Pistol_Aim_Down` | 0.166667 | 195 | LINEAR |
| `Pistol_Aim_Neutral` | 0.166667 | 195 | LINEAR |
| `Pistol_Aim_Up` | 0.166667 | 195 | LINEAR |
| `Pistol_Idle_Loop` | 1.666667 | 195 | LINEAR |
| `Pistol_Reload` | 1.666667 | 195 | LINEAR |
| `Pistol_Shoot` | 0.633333 | 195 | LINEAR |
| `Punch_Cross` | 1.000000 | 195 | LINEAR |
| `Punch_Jab` | 0.866667 | 195 | LINEAR |
| `Push_Loop` | 2.666667 | 195 | LINEAR |
| `Roll` | 1.466667 | 195 | LINEAR |
| `Sitting_Enter` | 1.300000 | 195 | LINEAR |
| `Sitting_Exit` | 1.033333 | 195 | LINEAR |
| `Sitting_Idle_Loop` | 1.666667 | 195 | LINEAR |
| `Sitting_Talking_Loop` | 2.933333 | 195 | LINEAR |
| `Spell_Simple_Enter` | 0.533333 | 195 | LINEAR |
| `Spell_Simple_Exit` | 0.433333 | 195 | LINEAR |
| `Spell_Simple_Idle_Loop` | 2.100000 | 195 | LINEAR |
| `Spell_Simple_Shoot` | 0.500000 | 195 | LINEAR |
| `Sprint_Loop` | 0.666667 | 195 | LINEAR |
| `Swim_Fwd_Loop` | 1.333333 | 195 | LINEAR |
| `Swim_Idle_Loop` | 3.333333 | 195 | LINEAR |
| `Sword_Attack` | 1.533333 | 195 | LINEAR |
| `Sword_Idle` | 1.666667 | 195 | LINEAR |
| `Walk_Formal_Loop` | 1.333333 | 195 | LINEAR |
| `Walk_Loop` | 1.333333 | 195 | LINEAR |

### UAL1 ? todos os joints (65)

| Nome real | Pai |
|---|---|
| `root` | `Armature` |
| `pelvis` | `root` |
| `spine_01` | `pelvis` |
| `spine_02` | `spine_01` |
| `spine_03` | `spine_02` |
| `neck_01` | `spine_03` |
| `Head` | `neck_01` |
| `clavicle_l` | `spine_03` |
| `upperarm_l` | `clavicle_l` |
| `lowerarm_l` | `upperarm_l` |
| `hand_l` | `lowerarm_l` |
| `index_01_l` | `hand_l` |
| `index_02_l` | `index_01_l` |
| `index_03_l` | `index_02_l` |
| `index_04_leaf_l` | `index_03_l` |
| `middle_01_l` | `hand_l` |
| `middle_02_l` | `middle_01_l` |
| `middle_03_l` | `middle_02_l` |
| `middle_04_leaf_l` | `middle_03_l` |
| `pinky_01_l` | `hand_l` |
| `pinky_02_l` | `pinky_01_l` |
| `pinky_03_l` | `pinky_02_l` |
| `pinky_04_leaf_l` | `pinky_03_l` |
| `ring_01_l` | `hand_l` |
| `ring_02_l` | `ring_01_l` |
| `ring_03_l` | `ring_02_l` |
| `ring_04_leaf_l` | `ring_03_l` |
| `thumb_01_l` | `hand_l` |
| `thumb_02_l` | `thumb_01_l` |
| `thumb_03_l` | `thumb_02_l` |
| `thumb_04_leaf_l` | `thumb_03_l` |
| `clavicle_r` | `spine_03` |
| `upperarm_r` | `clavicle_r` |
| `lowerarm_r` | `upperarm_r` |
| `hand_r` | `lowerarm_r` |
| `index_01_r` | `hand_r` |
| `index_02_r` | `index_01_r` |
| `index_03_r` | `index_02_r` |
| `index_04_leaf_r` | `index_03_r` |
| `middle_01_r` | `hand_r` |
| `middle_02_r` | `middle_01_r` |
| `middle_03_r` | `middle_02_r` |
| `middle_04_leaf_r` | `middle_03_r` |
| `pinky_01_r` | `hand_r` |
| `pinky_02_r` | `pinky_01_r` |
| `pinky_03_r` | `pinky_02_r` |
| `pinky_04_leaf_r` | `pinky_03_r` |
| `ring_01_r` | `hand_r` |
| `ring_02_r` | `ring_01_r` |
| `ring_03_r` | `ring_02_r` |
| `ring_04_leaf_r` | `ring_03_r` |
| `thumb_01_r` | `hand_r` |
| `thumb_02_r` | `thumb_01_r` |
| `thumb_03_r` | `thumb_02_r` |
| `thumb_04_leaf_r` | `thumb_03_r` |
| `thigh_l` | `pelvis` |
| `calf_l` | `thigh_l` |
| `foot_l` | `calf_l` |
| `ball_l` | `foot_l` |
| `ball_leaf_l` | `ball_l` |
| `thigh_r` | `pelvis` |
| `calf_r` | `thigh_r` |
| `foot_r` | `calf_r` |
| `ball_r` | `foot_r` |
| `ball_leaf_r` | `ball_r` |

### UAL2 ? todos os 43 clipes do Standard

| Nome real | Dura??o (s) | Canais | Interpola??o |
|---|---:|---:|---|
| `A_TPose` | 2.500000 | 195 | LINEAR |
| `Chest_Open` | 1.366667 | 195 | LINEAR |
| `ClimbUp_1m` | 0.666667 | 195 | LINEAR |
| `Consume` | 1.333333 | 195 | LINEAR |
| `Farm_Harvest` | 2.500000 | 195 | LINEAR |
| `Farm_PlantSeed` | 2.766667 | 195 | LINEAR |
| `Farm_Watering` | 3.800000 | 195 | LINEAR |
| `Hit_Knockback` | 0.833333 | 195 | LINEAR |
| `Idle_FoldArms_Loop` | 2.500000 | 195 | LINEAR |
| `Idle_Lantern_Loop` | 2.500000 | 195 | LINEAR |
| `Idle_No_Loop` | 2.500000 | 195 | LINEAR |
| `Idle_Rail_Call` | 2.500000 | 195 | LINEAR |
| `Idle_Rail_Loop` | 2.500000 | 195 | LINEAR |
| `Idle_Shield_Break` | 1.066667 | 195 | LINEAR |
| `Idle_Shield_Loop` | 2.500000 | 195 | LINEAR |
| `Idle_TalkingPhone_Loop` | 2.933333 | 195 | LINEAR |
| `LayToIdle` | 1.533333 | 195 | LINEAR |
| `Melee_Hook` | 0.466667 | 195 | LINEAR |
| `Melee_Hook_Rec` | 0.600000 | 195 | LINEAR |
| `NinjaJump_Idle_Loop` | 2.000000 | 195 | LINEAR |
| `NinjaJump_Land` | 1.266667 | 195 | LINEAR |
| `NinjaJump_Start` | 0.966667 | 195 | LINEAR |
| `OverhandThrow` | 1.333333 | 195 | LINEAR |
| `Shield_Dash` | 1.100000 | 195 | LINEAR |
| `Shield_OneShot` | 0.833333 | 195 | LINEAR |
| `Slide_Exit` | 0.500000 | 195 | LINEAR |
| `Slide_Loop` | 2.000000 | 195 | LINEAR |
| `Slide_Start` | 0.833333 | 195 | LINEAR |
| `Sword_Block` | 1.233333 | 195 | LINEAR |
| `Sword_Dash` | 1.566667 | 195 | LINEAR |
| `Sword_Heavy_Combo` | 4.333333 | 195 | LINEAR |
| `Sword_Regular_A` | 0.433333 | 195 | LINEAR |
| `Sword_Regular_A_Rec` | 0.966667 | 195 | LINEAR |
| `Sword_Regular_B` | 0.533333 | 195 | LINEAR |
| `Sword_Regular_B_Rec` | 1.033333 | 195 | LINEAR |
| `Sword_Regular_C` | 2.000000 | 195 | LINEAR |
| `Sword_Regular_Combo` | 3.000000 | 195 | LINEAR |
| `TreeChopping_Loop` | 0.966667 | 195 | LINEAR |
| `Walk_Carry_Loop` | 2.000000 | 195 | LINEAR |
| `Yes` | 2.500000 | 195 | LINEAR |
| `Zombie_Idle_Loop` | 1.333333 | 195 | LINEAR |
| `Zombie_Scratch` | 1.800000 | 195 | LINEAR |
| `Zombie_Walk_Fwd_Loop` | 1.333333 | 195 | LINEAR |

### UAL2 ? todos os joints (65)

| Nome real | Pai |
|---|---|
| `root` | `Armature` |
| `pelvis` | `root` |
| `spine_01` | `pelvis` |
| `spine_02` | `spine_01` |
| `spine_03` | `spine_02` |
| `neck_01` | `spine_03` |
| `Head` | `neck_01` |
| `clavicle_l` | `spine_03` |
| `upperarm_l` | `clavicle_l` |
| `lowerarm_l` | `upperarm_l` |
| `hand_l` | `lowerarm_l` |
| `index_01_l` | `hand_l` |
| `index_02_l` | `index_01_l` |
| `index_03_l` | `index_02_l` |
| `index_04_leaf_l` | `index_03_l` |
| `middle_01_l` | `hand_l` |
| `middle_02_l` | `middle_01_l` |
| `middle_03_l` | `middle_02_l` |
| `middle_04_leaf_l` | `middle_03_l` |
| `pinky_01_l` | `hand_l` |
| `pinky_02_l` | `pinky_01_l` |
| `pinky_03_l` | `pinky_02_l` |
| `pinky_04_leaf_l` | `pinky_03_l` |
| `ring_01_l` | `hand_l` |
| `ring_02_l` | `ring_01_l` |
| `ring_03_l` | `ring_02_l` |
| `ring_04_leaf_l` | `ring_03_l` |
| `thumb_01_l` | `hand_l` |
| `thumb_02_l` | `thumb_01_l` |
| `thumb_03_l` | `thumb_02_l` |
| `thumb_04_leaf_l` | `thumb_03_l` |
| `clavicle_r` | `spine_03` |
| `upperarm_r` | `clavicle_r` |
| `lowerarm_r` | `upperarm_r` |
| `hand_r` | `lowerarm_r` |
| `index_01_r` | `hand_r` |
| `index_02_r` | `index_01_r` |
| `index_03_r` | `index_02_r` |
| `index_04_leaf_r` | `index_03_r` |
| `middle_01_r` | `hand_r` |
| `middle_02_r` | `middle_01_r` |
| `middle_03_r` | `middle_02_r` |
| `middle_04_leaf_r` | `middle_03_r` |
| `pinky_01_r` | `hand_r` |
| `pinky_02_r` | `pinky_01_r` |
| `pinky_03_r` | `pinky_02_r` |
| `pinky_04_leaf_r` | `pinky_03_r` |
| `ring_01_r` | `hand_r` |
| `ring_02_r` | `ring_01_r` |
| `ring_03_r` | `ring_02_r` |
| `ring_04_leaf_r` | `ring_03_r` |
| `thumb_01_r` | `hand_r` |
| `thumb_02_r` | `thumb_01_r` |
| `thumb_03_r` | `thumb_02_r` |
| `thumb_04_leaf_r` | `thumb_03_r` |
| `thigh_l` | `pelvis` |
| `calf_l` | `thigh_l` |
| `foot_l` | `calf_l` |
| `ball_l` | `foot_l` |
| `ball_leaf_l` | `ball_l` |
| `thigh_r` | `pelvis` |
| `calf_r` | `thigh_r` |
| `foot_r` | `calf_r` |
| `ball_r` | `foot_r` |
| `ball_leaf_r` | `ball_r` |

### Human Archer ? ossos de HumanM@Idle01

| Nome real | Pai |
|---|---|
| `B-root` | `Rig` |
| `B-hips` | `B-root` |
| `B-hand.L` | `B-forearm.L` |
| `B-indexFinger01.L` | `B-hand.L` |
| `B-spineProxy` | `B-root` |
| `B-indexFinger02.L` | `B-indexFinger01.L` |
| `B-indexFinger03.L` | `B-indexFinger02.L` |
| `B-middleFinger01.L` | `B-hand.L` |
| `B-middleFinger02.L` | `B-middleFinger01.L` |
| `B-middleFinger03.L` | `B-middleFinger02.L` |
| `B-ringFinger01.L` | `B-hand.L` |
| `B-ringFinger02.L` | `B-ringFinger01.L` |
| `B-ringFinger03.L` | `B-ringFinger02.L` |
| `B-spine` | `B-hips` |
| `B-pinky01.L` | `B-hand.L` |
| `B-pinky02.L` | `B-pinky01.L` |
| `B-pinky03.L` | `B-pinky02.L` |
| `B-thumb01.L` | `B-hand.L` |
| `B-thumb02.L` | `B-thumb01.L` |
| `B-thumb03.L` | `B-thumb02.L` |
| `B-chest` | `B-spine` |
| `B-handProp.L` | `B-hand.L` |
| `B-shoulder.R` | `B-chest` |
| `B-neck` | `B-chest` |
| `B-upperArm.R` | `B-shoulder.R` |
| `B-forearm.R` | `B-upperArm.R` |
| `B-hand.R` | `B-forearm.R` |
| `B-indexFinger01.R` | `B-hand.R` |
| `B-indexFinger02.R` | `B-indexFinger01.R` |
| `B-indexFinger03.R` | `B-indexFinger02.R` |
| `B-middleFinger01.R` | `B-hand.R` |
| `B-middleFinger02.R` | `B-middleFinger01.R` |
| `B-middleFinger03.R` | `B-middleFinger02.R` |
| `B-ringFinger01.R` | `B-hand.R` |
| `B-head` | `B-neck` |
| `B-ringFinger02.R` | `B-ringFinger01.R` |
| `B-ringFinger03.R` | `B-ringFinger02.R` |
| `B-pinky01.R` | `B-hand.R` |
| `B-pinky02.R` | `B-pinky01.R` |
| `B-pinky03.R` | `B-pinky02.R` |
| `B-thumb01.R` | `B-hand.R` |
| `B-thumb02.R` | `B-thumb01.R` |
| `B-thumb03.R` | `B-thumb02.R` |
| `B-jaw` | `B-head` |
| `B-handProp.R` | `B-hand.R` |
| `B-shoulder.L` | `B-chest` |
| `B-thigh.L` | `B-hips` |
| `B-shin.L` | `B-thigh.L` |
| `B-foot.L` | `B-shin.L` |
| `B-toe.L` | `B-foot.L` |
| `B-thigh.R` | `B-hips` |
| `B-shin.R` | `B-thigh.R` |
| `B-foot.R` | `B-shin.R` |
| `B-toe.R` | `B-foot.R` |
| `B-upperArm.L` | `B-shoulder.L` |
| `B-forearm.L` | `B-upperArm.L` |

### Validação das regras de ignore

Regras verificadas com `git check-ignore --no-index` em repositório temporário isolado sob `minecraft/.logs/m001-ignore-check/`, usando uma cópia do `.gitignore` do projeto. GLB/licença em input e arquivo temporário em converted foram ignorados; script, relatórios e os dois .gitkeep não foram ignorados. Nenhum repositório foi inicializado na raiz do projeto e não houve commit. O diretório temporário está dentro de .logs, já ignorado.