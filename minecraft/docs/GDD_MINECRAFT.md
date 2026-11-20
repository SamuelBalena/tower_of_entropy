# Tower of Entropy — GDD Minecraft, escopo implementado

Atualização M002.A3.3, 14/11/2026. Documento específico do mod Java, sem alterar o projeto Godot.

A3.3: avanços padrão A/B/C/pesado 0,40/0,50/0,60/0,70 bloco; pesado termina em 35 ticks, com apenas o trecho final remapeado em 250 ms. Pulo voluntário/Auto-Jump não cancela ataques; uma tecla mantida exige liberação e nova pressão. Roll, dano e i-frames preservados. [Ajustes e limites](M002_A3_3_COMBAT_REFINEMENTS.md), [testes](TEST_LOG_M002_A3_3.md).

Ataques de espada bloqueiam WASD durante a ação e recebem avanços físicos curtos configuráveis; Roll aceita Sprint e recupera controle ao concluir 14 passos. Dano, i-frames, modelos e clipes originais preservados. [Arquitetura e testes](M002_A3_2_COMBAT_MOVEMENT.md).

As animações suportadas são padrão em qualquer mundo Singleplayer, sem comandos ou circuito. Classic aprovado preservado; Slim tem variante derivada de braços/UVs com seleção automática pela skin. [Instalação e arquitetura global](M002_A3_1_GLOBAL_DEFAULT_ANIMATIONS.md), [skins](M002_A3_1_SKIN_COMPATIBILITY.md), [testes e limites](TEST_LOG_M002_A3_1.md).

O lote A3 acrescenta direções Archer, crouch, salto/queda/aterrissagem, natação, reações curtas de dano, morte, arco parado, consumo, tocha e corte visual de madeira. Não altera física, dano ou uso vanilla de itens. Arco mirando em movimento/combinações não aceitas usa vanilla; picareta UASSET está bloqueada por exportação/licença não verificada; interações sem associação segura e ações excluídas mantêm fallback. Não há promessa de 100%, RPG ou multiplayer remoto homologado. [Cobertura](M002_A3_FULL_ANIMATION_COVERAGE.md), [compatibilidade](M002_A3_ASSET_COMPATIBILITY.md), [testes](TEST_LOG_M002_A3.md).

Locomoção horizontal visual automática: Idle/Walk/Jog/Sprint por deslocamento real e sprint vanilla, com fase compartilhada e blend configurável de 140 ms. Marcha reversa e strafe são reconhecidos, mas permanecem em fallback por ausência de clipes adequados. Velocidades, física, controles e combate preservados. [Arquitetura](M002_A2_LOCOMOTION_ARCHITECTURE.md), [assets](M002_A2_ANIMATION_ASSETS.md), [QA visual](M002_A2_VISUAL_QA.md), [testes](TEST_LOG_M002_A2.md). O conjunto A2 foi ampliado pelo lote A3 descrito acima.

Polimento visual A1.2: entrada pesada 120 ms, retorno a Idle/Jog 140 ms com fase contínua, cancelamento direto para Roll com blend visual de 60 ms e apoio vertical limitado B/C derivado da fonte. Clipes, geometria, regras, dano, física e i-frames preservados. [Arquitetura](M002_A1_2_ANIMATION_TRANSITIONS.md), [comparação visual](M002_A1_2_VISUAL_QA.md) e [testes](TEST_LOG_M002_A1_2.md). Aprovação estética segue pendente.

Minecraft Java 1.20.1 / Forge 47.4.28 / JDK 17. Personagem 3D em terceira pessoa com aparência Classic Steve. Polymesh R3 é o renderizador padrão aprovado do corpo; CPM permanece ponte técnica/fallback; R3.1 continua experimental. Combate próprio, autoritativo no servidor.

Implementado neste recorte: Idle/Jog UAL, Roll físico UAL1 em V, leves de espada UAL2 A → B → C por ATTACK e um pesado derivado da frase final de `Sword_Heavy_Combo` por USE contextual. AUTO é padrão: espada na mão principal ativa o conjunto sem comando. ON/OFF são diagnósticos. Dano bruto provisório 4/6 HP. Roll cancela ataques e tem i-frames de combate nos ticks 3–9. Não há stamina, mana, classes, passivas ou progressão.

Somente espadas têm conjunto de combate. Categorias lança, machado, arco e desarmado estão reservadas, sem mecânicas, armas ou animações implementadas. Combos pesados completos e mistos também estão apenas reservados. A animação pesada e as novas transições aguardam aprovação estética do usuário.

Armaduras, capas, offhand e primeira pessoa usam os fallbacks preservados; não há promessa de paridade dos novos clipes no CPM ou no R3 remoto. Não houve alteração de autenticação, licenciamento ou instalação de mod de gameplay. Nenhum próximo marco foi iniciado.

Detalhes: [entradas e combos](M002_A1_ANIMATION_INPUT_AND_COMBOS.md), [pesado](M002_A1_HEAVY_ATTACKS.md), [i-frames](M002_A1_IFRAMES.md), [testes](TEST_LOG_M002_A1.md).

M002.A1.1: ativação automática e material local de QA, sem novos clipes, mudanças de geometria ou balanceamento. Consulte [QA visual](M002_A1_1_VISUAL_QA.md) para os achados pendentes.
