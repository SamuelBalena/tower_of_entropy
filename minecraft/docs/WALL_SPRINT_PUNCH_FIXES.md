# Correções de parede, Sprint e socos

## M001 — Aderência automática

O servidor procura contato nas quatro faces laterais do tronco, com tolerância de 0,035 bloco. A sondagem usa a forma de colisão, exige um bloco completo e fica entre os pés e o topo do personagem para excluir o piso e o teto. Cercas, degraus e outras formas incompletas ficam fora desta primeira implementação.

Qualquer contato lateral válido no ar pode iniciar a aderência. A mecânica mantém as exclusões existentes para água, voo, montaria, escalada, uso de item, dano recente e ações de combate. O servidor consome o vigor durante o contato e ajusta a velocidade vertical. Uma perda de contato encerra a aderência. Quando o vigor se esgota, o bloqueio vale para o restante daquele período no ar, inclusive ao reencontrar a mesma parede. Tocar o chão reinicia o vigor. A pose temporária continua sendo uma amostra de `UAL_jump_loop`.

## M002 — Sprint direcional

O cliente Forge amplia as duas verificações de impulso do Minecraft 1.20.1. A tecla remapeável de Sprint pode iniciar a corrida com impulso lateral ou para trás. Depois de iniciada, a corrida aceita troca de direção, mesmo após liberar a tecla de Sprint, enquanto as condições vanilla de continuidade forem válidas. Duplo toque para frente, fome, uso de itens, colisões e nado continuam a cargo do Minecraft. A extensão se aplica à locomoção a pé e exclui montaria, voo e fluidos.

Os eixos de entrada, a física e a rotação da câmera seguem vanilla. O R3 continua usando `UAL_sprint`, fase compartilhada com a caminhada e orientação suave pela direção medida do deslocamento. Com o arco equipado, correr usa essa mesma seleção quando o arco não está em uso.

## M003 — Socos no ar

O evento Forge `LeftClickEmpty` passa pelo mesmo observador de ataques a blocos e entidades. Jab e Cross alternam uma vez por swing vanilla efetivamente iniciado, sem emitir dano, interação ou pacote de ataque adicional. Itens equipados continuam no caminho específico já existente.

## Arquivos alterados

- `WallSlide.java` implementa a sondagem lateral autoritativa.
- `DirectionalSprint.java`, `META-INF/coremods.json` e `coremods/directional_sprint.js` estendem as verificações de Sprint do cliente.
- `VisualContext.java` mantém o clipe de Sprint em deslocamento normal com arco equipado.
- `ClientSwingPose.java` inclui cliques no ar.
- `ExpansionRuntimeTest.java`, `build.gradle` e `.gitignore` cobrem o cenário atualizado e o perfil isolado `run-corrections-qa`.

O renderizador, o rig e os clipes do Polymesh R3 usam os mesmos recursos da expansão. O protocolo de rede permanece na versão 9. O ajuste de Sprint foi feito para o bytecode do Minecraft 1.20.1 com Forge 47.4.28.

## Validação

- Build Forge normal aprovado. Os 17 recursos de `assets/towerofentropy` conferem byte a byte com o JAR anterior, incluindo GLBs Classic/Slim, clipes e metadados de pose. O JAR inclui o transformador de Sprint e exclui as classes de QA.
- As cinco suítes existentes passaram. Foram 43 casos de movimento horizontal, 18 de combate e 104 de Roll, além das suítes de locomoção e janelas de ação.
- Integração Classic no Minecraft Forge aprovada em 8/8, com zero erros de renderização. Confirmou Jab e Cross contra entidade, na quebra de terra e em cliques no vazio. Confirmou caminhada, abertura e manutenção de Sprint nas oito direções e reversão de W para S após liberar a tecla de Sprint. Na parede, confirmou aderência olhando para longe e sem agachar, esgotamento sem reativação no ar, recuperação ao aterrissar e soltura após a remoção da parede.
- Regressão Slim no Minecraft Forge aprovada em 13/13, com zero erros de renderização. Os quadros da execução registraram espada, Roll e carga, sustentação e soltura do arco. O servidor confirmou o consumo da flecha, o projétil vanilla e o consumo de comida. A reconexão voltou ao R3 automático.
- Os resultados Slim e as novas capturas estão em `animation_pipeline/converted/corrections-qa/runtime`. Os logs estão em `.logs/corrections-*.log`. O QA foi executado em cópias do mundo. Nenhuma captura histórica foi sobrescrita.

SHA-256 do JAR final: `127EE2C5B22AD71DCEC1630674552B90F02611C91A51CCCE47F032BF9D01B772`.

## Limites e revisão visual

O teste executou cliente e servidor integrado no Forge 47.4.28. Um servidor dedicado e outros builds do Forge ficam fora desta validação. A representação visual da parede permanece provisória.

Em F5, salte junto a uma parede sólida olhando para longe e sem agachar. Confirme a aderência, a soltura ao esgotar o vigor e a recarga após aterrissar. Compare com piso, teto e cercas para verificar as exclusões de contato. Em terreno plano, percorra as oito direções usando a tecla de Sprint e inverta de W para S durante a corrida. Por fim, clique duas vezes no vazio com a mão vazia e reveja a alternância dos braços também em combate e na quebra de blocos.

Nenhum commit foi criado.
