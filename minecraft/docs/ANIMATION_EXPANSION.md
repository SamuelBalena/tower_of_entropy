# Expansão das animações do Steve

19 de novembro de 2026

## Resultado

- **Combate e mineração com mão vazia.** `UAL_punch_jab` e `UAL_punch_cross` alternam o braço principal do golpe nos swings vanilla direcionados a uma entidade ou a um bloco. O cliente observa os eventos Forge e o swing real. As pernas seguem a locomoção e o golpe é amostrado no ritmo do swing vanilla. Dano, alcance, recarga, quebra e drops seguem as regras existentes.
- **Locomoção.** A caminhada normal seleciona o `UAL_walk` já empacotado, oriundo de `Walk_Loop`. Idle, Walk, Jog e Sprint continuam partilhando a fase do ciclo e o blending R3. Nas oito direções dos controles vanilla, o tronco segue a direção medida do deslocamento. Jog e Sprint usam seus clipes atuais voltados para a direção do movimento. Arco, agachamento, água, equipamento e ações de combate mantêm suas prioridades e regras de compatibilidade.
- **Parede.** Mantenha a tecla de agachar durante a descida, perto de uma parede bloqueante e olhando para ela. O servidor detecta formas de colisão à frente do tronco e autoriza a descida com a velocidade limitada. O vigor padrão dura 35 ticks de aderência e recarrega ao tocar o chão. Soltar a tecla, perder contato ou esgotar o vigor libera a queda. A gravidade e o cálculo de dano de queda continuam vanilla. `towerofentropy-wall-server.toml` permite configurar a ativação, o vigor e a velocidade vertical. O cliente recebe o estado autorizado e usa uma pose temporária de `UAL_jump_loop` enquanto desliza, retomando o clipe de queda existente ao soltar. `/toe_renderlab status` mostra `wallSlide`.

A cobertura visual R3 continua no jogador local em terceira pessoa, com Classic e Slim. Primeira pessoa, jogadores remotos e equipamentos com fallback continuam usando CPM/vanilla. Cliente e servidor precisam do mesmo build por causa da versão 9 do canal de ações.

## Auditoria de fontes

UAL1 e UAL2 Standard incluem GLB e FBX. Os GLBs de animação têm 65 juntas no skin e o mapeamento humanoide utilizado pelo retargeting existente. Cada pacote contém um `License.txt` CC0 1.0. As durações e o deslocamento foram medidos nas curvas GLB. Os arquivos `_RM` incluem translação para parte da locomoção. O build usa os clips in-place e o deslocamento físico continua a cargo do Minecraft.

| Fonte | Candidatos e duração | Decisão |
| --- | --- | --- |
| UAL1 | `Punch_Jab` 0,867 s e `Punch_Cross` 1,000 s | Integrados. As curvas retargetadas mostram maior atividade no cotovelo esquerdo no jab e no braço e cotovelo direitos no cross. A raiz permanece no lugar. |
| UAL1 | `Walk_Loop` e `Walk_Formal_Loop` 1,333 s, `Jog_Fwd_Loop` 0,933 s e `Sprint_Loop` 0,667 s | Walk normal, Jog e Sprint reaproveitados. No GLB RM, o deslocamento longitudinal é respectivamente 1,3 m, 1,3 m, 5 m e 5,5 m. |
| UAL1 | `Jump_Loop` 2,500 s | Queda existente e pose temporária na parede. |
| UAL2 | `TreeChopping_Loop` 0,967 s, `Walk_Carry_Loop` 2,000 s, `Zombie_Walk_Fwd_Loop` 1,333 s, `ClimbUp_1m` 0,667 s | O golpe de machado já atende troncos. Caminhada com carga e marcha zumbi ficaram como alternativas; a subida curta não fornece uma descida sustentada. |
| Free100Animations | `AS_MinerSwingingPicaxe` 6,000 s e `AS_MineRock` 6,733 s | Candidatos de picareta. Ambos têm excursão nos dois braços, raiz estacionária e vários segundos de atuação. Exigem recorte e revisão do contato. |
| Free100Animations | `AS_DigWithShovel` 13,133 s e `AS_FarmerUsingHoe` 9,233 s | Candidatos de pá e enxada. Ambos têm raiz estacionária e excursão ampla nas mãos. Início e fim ocupam posições diferentes, por isso recortar um golpe antes de avaliar repetição e âncora. |
| Free100Animations | Oito `MF_Unarmed_Walk_*` de 1,500–2,133 s e oito `MF_Unarmed_Jog_*` de 1,500–1,767 s | Root motion habilitado. As curvas à frente deslocam a raiz 450 cm e 1.060,6 cm respectivamente. |
| Free100Animations | `AS_LeaningOnWall` 10,400 s, `MM_WallJump` 0,467 s, `MM_Fall_Loop` 3,000 s | A primeira sequência tem pouca excursão das mãos e da pelve. A segunda é um gesto curto e assimétrico. A terceira é uma candidata para queda. Nenhuma das três fornece uma descida lenta com as mãos sustentadas na parede. |

O Free100Animations é um projeto UE 5.8 com pacotes `.uasset`. O `UnrealEditor-Cmd.exe` conseguiu carregar os `AnimSequence`, confirmar `SK_Mannequin` e amostrar curvas em uma cópia de auditoria. O [extrato de metadados](../animation_pipeline/reports/expansion/FREE100_METADADOS.txt) inclui durações, flags e amostras de translação. Os arquivos originais ficaram intactos. Os clipes ferramentas exigem exportação pelo Unreal, conversão do esqueleto para as convenções do sampler R3 e ajuste visual de empunhadura.

O pacote local Free100Animations veio sem uma licença aplicável identificável. A descrição pública correspondente anuncia um download gratuito e não explicita autorização comercial ou uso fora do Unreal. Os clipes de picareta, pá e enxada ficaram pendentes, com o swing de ferramenta vanilla/CPM em uso. Nenhum asset Unreal entrou no JAR. [Descrição pública do pacote](https://zolotube.com/watch?v=21fqps4fWZ8)

## Arquivos e preservação

- Build e pipeline em `build.gradle`, `prepare-a3.cjs`, `blender-a3.py` e `package-expansion.cjs`. A bake reutiliza o mesmo projeto Blender R3 e gera `steve_a3_expansion.glb`, `steve_a3_slim_expansion.glb` e `expansion-poses.json` em `animation_pipeline/converted/expansion/`. Use `TOE_BLENDER` para indicar o executável se o Blender não estiver em PATH ou na instalação local conhecida.
- Cliente em `ClientSwingPose.java`, `HorizontalLocomotion.java`, `VisualContext.java`, `PlayerRendererR3.java`, `R3VisualPose.java`, `RendererCommands.java` e `ClientWallVisual.java`.
- Parede e rede em `WallSlide.java`, `TowerOfEntropy.java` e `ActionNetwork.java`.
- QA em `validation/expansion/…/ExpansionRuntimeTest.java` e em `.gitignore` para o perfil isolado `run-expansion-qa`.

Comparei os GLBs Classic/Slim e `a3-poses.json` da base com o JAR anterior. Os três hashes eram idênticos. O empacotador também verifica nós, malhas, skinning, materiais, texturas, accessors, clipes anteriores e o prefixo binário das duas variantes. O modelo derivado usa os mesmos 12 ossos e mantém as âncoras 302 e 303. Espada, pesado, Roll, arco e transições existentes seguem as prioridades atuais.

## Testes

- Os testes existentes `test-horizontal.ps1`, `test-combat.ps1`, `test-roll.ps1`, `test-locomotion.ps1` e `test-actions.ps1` passaram. As três primeiras suítes cobriram respectivamente 43, 18 e 104 casos.
- O teste de integração `-PtoeExpansionTest` passou em 5/5 no Minecraft Forge com Classic. Observou os dois clipes em ataques contra uma entidade com dano no servidor e na quebra efetiva de terra. Validou deslocamento nas oito direções e erro de orientação inferior a 0,01 grau no fim de cada percurso. Na parede, observou 35 ticks de aderência, liberação e aterrissagem. A velocidade típica medida durante a aderência foi de 0,065 bloco/tick, com pico pontual de 0,142 entre atualizações, e chegou a 0,833 durante a queda livre. O renderer registrou zero erros.
- A regressão A3 em Slim passou em 13/13 com zero erros de renderização, incluindo espada, Roll, arco com consumo de flecha e projétil vanilla, comida, tocha, agachamento, salto e reconexão. O teste anterior nesse perfil foi interrompido porque reutilizava o save alterado pelo teste de parede e abriu a tela de morte. A repetição usou uma cópia limpa.
- O build Forge final passou. Os 14 recursos anteriores do mod no JAR conferem byte a byte com o JAR de referência. O novo JAR tem 2.687.146 bytes e SHA-256 0F71EEA49C38FE95745839767547CAB864C3C8C04DCF2CA4931012E25598F134.
- O build base foi conferido individualmente para Classic e Slim. Ambos os derivados têm 12 juntas e 32 clipes, incluindo os 30 anteriores e os dois socos novos.

A primeira invocação da regressão usou por engano o perfil `run-global`. Interrompi o cliente e restaurei o save `Global Survival one` da cópia feita antes da execução. Foram sobrescritas 96 capturas no diretório histórico de cobertura A3.1, nas séries `crouch_idle`, `crouch_move` e `jump`. As novas rodadas usaram `run-expansion-qa`. Nenhum commit foi criado.

Para repetir as suítes Forge, use uma cópia nova a cada execução. Copie um save de QA chamado `Global Survival one` para `run-expansion-qa/saves`. Execute o teste específico com `./scripts/gradle.ps1 runClient --offline -PtoeExpansionTest`. Para a cobertura Slim use `./scripts/gradle.ps1 runClient --offline -PtoeGlobalCoverage -PtoeGlobalSlim -PtoeGlobalSkinCheck -PtoeExpansionQa`.

## Revisão visual no Minecraft

1. Rode `./scripts/gradle.ps1 runClient --console=plain` na pasta `minecraft`. Teste num mundo de teste, em F5, com mão direita, sem equipamento na outra mão, sem armadura e com todas as camadas da skin ligadas.
2. Em Sobrevivência, ataque uma entidade duas vezes de mão vazia e quebre terra segurando o ataque. Observe a alternância entre os braços, a transição para Idle e o movimento das pernas andando. Repita com Classic e Slim. Faça também um golpe no ar e confira o swing vanilla.
3. Caminhe normalmente, acelere e faça Sprint. Rode a câmera e percorra as oito combinações sem alterar os controles. Confira a orientação nas diagonais, a transição ao parar e o deslizamento dos pés.
4. Salte junto a uma parede sólida de vários blocos de altura, olhe para a parede e segure agachar na descida. Observe a pose temporária e use `/toe_renderlab status` para conferir a adesão. Continue segurando até o vigor terminar. Confirme que ocorre a queda e que uma nova tentativa funciona após tocar o chão. Para testar só a física, monte um chão que amorteça a queda.
5. Repita os casos anteriores de espada, ataque pesado, Roll e arco. Confira a empunhadura e a continuidade de pose. Os clipes de ferramenta e de parede dedicados continuam pendentes de revisão.

