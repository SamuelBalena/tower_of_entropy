# A001 — Armaduras vanilla no Polymesh R3

## Causa

O evento `RenderPlayerEvent.Pre` consultava `PlayerRendererR3.equipmentReason` antes de calcular a pose. Qualquer peça de armadura devolvia o motivo `armadura` e entregava o jogador completo ao renderer vanilla/CPM. Essa rota usava o HumanoidModel e suas articulações vanilla. A armadura equipada, portanto, impedia que a malha do corpo e a do equipamento compartilhassem o relógio e as matrizes do R3.

## Implementação

- `src/main/java/com/towerofentropy/client/PlayerRendererR3.java` mantém o renderer do jogador local ativo com armaduras vanilla e chama o desenho da armadura depois do corpo. Ambos recebem o mesmo PoseStack e a mesma pose global, já interpolada e composta com as camadas superiores, inferiores e ajustes visuais do personagem.
- `src/main/java/com/towerofentropy/client/R3Armor.java` captura a geometria dos modelos vanilla interno e externo uma vez por instância do renderer. Conserva UVs e mapeia os capacetes à cabeça, o torso a peito, cintura e quadril, os braços a ombros e cotovelos, e calças e botas às cadeias do quadril, pernas e joelhos. A pose final de cada junta é multiplicada pela inversa de bind do R3. As normais são transformadas pela inversa transposta.
- As laterais dos cubos têm subdivisões nas transições do rig. O peso fica concentrado nos segmentos rígidos e varia nas dobras. A geometria do personagem permanece a aprovada. A malha Slim estreita a face externa dos braços em um pixel sem trocar as coordenadas UV da textura vanilla.
- A `HumanoidArmorLayer` continua selecionando os slots, camadas interna e externa, textura de material, cor e sobreposição do couro, trim e glint. A mesma geometria deformada é reaproveitada entre os passes. O inventário, atributos, durabilidade, combate, física e rede seguem a lógica existente.
- `build.gradle`, `validation/w002/java/com/towerofentropy/client/camera/C0021RuntimeTest.java` e `validation/expansion/java/com/towerofentropy/combat/ExpansionRuntimeTest.java` acrescentam os perfis de QA com equipamento. A cena de armadura da C002.2 agora verifica que o R3 e a armadura foram desenhados juntos. As condições originais de câmera, movimento e natação continuam nas fixtures.

## Transparência

A armadura é enviada ao mesmo `MultiBufferSource` do corpo e dos itens nas mãos. Com retração da câmera, a C002.2 captura o conjunto local uma vez e aplica o alpha da câmera na composição. Fast e Fancy continuam usando a composição direta. Fabulous recebe a camada na etapa de composição vanilla, com o teste de profundidade da C002.2. Em espaço livre, o corpo e a armadura voltam ao passe normal a 100%. O jogador remoto permanece no renderer habitual e fora da captura local.

## Evidências de execução

Os testes usam Forge 1.20.1 com servidor integrado, skin personalizada com recortes alpha, e perfis de execução isolados em `.logs/a001-*`. A fixture de câmera provoca a retração com blocos sólidos, mantém o minério e o ponto de mira idênticos na comparação A/B, registra a opacidade real da câmera e verifica se o R3 e a armadura entram no mesmo frame. O percurso aquático usa entradas reais de movimento e mede as oito direções, inversão, superfície, subida, descida, espera e retorno à terra. Amostras de clipes do GLB exercitam poses adicionais com diamante completo e usam o renderer em execução no jogo.

| Modo | Classic | Slim |
| --- | ---: | ---: |
| Fast | [93/93](../.logs/a001-fast-classic-final.log) | [51/51](../.logs/a001-slim-0.log) |
| Fancy | [85/85](../.logs/a001-classic-1-retry.log) | [81/81](../.logs/a001-slim-1.log) |
| Fabulous | [84/84](../.logs/a001-classic-2-retry.log) | [94/94](../.logs/a001-slim-2-retry.log) |

São 488 verificações aprovadas nas rodadas da tabela. A extensão do roteiro variou conforme a rodada, com amostragem de clipes, Roll ativo, arco, troca de skin e trocas rápidas acrescentados durante o QA. Todos os perfis confirmaram o modo gráfico, a transparência progressiva no corredor, 12% no túnel com diamante equipado e a restauração a 100%.

Na primeira rodada Fast Classic, a amostragem dependia do tick exato do servidor. O cliente pulou a ativação de uma amostra de borda e o log marcou falha no clipe. O reteste fez a etapa de poses avançar pelo contador do cliente e registrou os 30 clipes esperados. A cena de diamante em espaço aberto foi registrada na primeira rodada, pois o reteste pulou a coleta dessa cena. As duas rodadas preservaram o limite e as asserções da transparência. [Primeira rodada Fast Classic](../.logs/a001-classic-0-retry.log)

As capturas iniciais em Fabulous Classic mostram a diferença entre [armadura a 100%](../.logs/a001-classic-2/screenshots/a001-ore_opaque.png) e [armadura a 12%](../.logs/a001-classic-2/screenshots/a001-ore_fade.png), com o minério visível sob a mira na segunda imagem. No Slim Fast, as capturas mostram [diamante](../.logs/a001-slim-0/screenshots/a001-diamond_open.png), [couro tingido](../.logs/a001-slim-0/screenshots/a001-dyed_leather.png), [cota de malha](../.logs/a001-slim-0/screenshots/a001-chainmail.png), [equipamento misto com casco](../.logs/a001-slim-0/screenshots/a001-mixed_turtle.png) e [diamante com trim de ouro](../.logs/a001-slim-0/screenshots/a001-trim_glint.png). A peça de trim também recebeu encantamento. O arquivo de log registra as trocas para ferro, ouro e netherite, além da remoção do conjunto e do retorno do contador de armadura a zero.

O perfil final Fabulous Slim verificou a troca de skin durante a sessão e a volta à skin inicial com a armadura equipada. Verificou também a sequência rápida de equipamento misto, remoção completa e volta ao diamante nos quatro slots. Na remoção, o contador de desenho da armadura ficou parado. [Skin trocada](../.logs/a001-slim-2/screenshots/a001-skin-swap.png) · [Skin restaurada](../.logs/a001-slim-2/screenshots/a001-skin-restored.png) · [Jogador remoto simulado](../.logs/a001-slim-2/screenshots/a001-remote_opaque.png)

O ensaio de expansão equipa diamante antes de iniciar as ações. Usa as entradas reais de caminhada, corrida, inversão, salto, parkour e jab/cross. A captura frontal dos estados de borda pode ficar encoberta pela parede da fixture. As amostras em espaço livre complementam essa evidência, com duas posições do clipe selecionado no R3. O combo A/B/C de espada, pesado e variantes de parede foram incluídos nessa amostragem. A validação com entradas reais de combate inclui ataque leve, Roll e as fases de puxar, sustentar e soltar o arco.

- [Sprint com diamante na partida de parkour](../.logs/a001-parkour-classic/screenshots/a001-UAL_sprint.png)
- [Terceiro golpe do combo, pose amostrada](../.logs/a001-classic-2/screenshots/a001-pose-sword_c-late.png)
- [Wall Slide, pose amostrada](../.logs/a001-classic-2/screenshots/a001-pose-wall_slide-late.png)
- [Natação com diamante completo](../.logs/a001-classic-2/screenshots/a001-swim-2.png)
- [Arco sustentado durante o fade a 12%](../.logs/a001-classic-1/screenshots/a001-UAL_bow_hold.png)

O parkour com diamante passou em **30/30 no Classic** e **30/30 no Slim**. Cada perfil registrou 12 clipes distintos com desenho de armadura, incluindo os ciclos de salto e pouso, acerto de jab e cross, sprint, deslizamento e passagem pela borda. Logs em [Classic](../.logs/a001-parkour-classic.log) e [Slim](../.logs/a001-parkour-slim.log).

## Regressões

- Câmera C002, **26/26**, incluindo ataque aéreo, Roll, primeira pessoa, modo frontal e cenário com vegetação. [Log](../.logs/a001-camera.log)
- COMBAT W002 e W002.1, **59/59 Classic** e **59/59 Slim**, incluindo estados da arma, Roll, mineração e durabilidade. [Classic](../.logs/a001-combat-classic.log) e [Slim](../.logs/a001-combat-slim.log)
- Regressão ampla Slim, **50/50 W001**, **36/36 W001.1/C001.3** e **29/29 expansão e parkour**. [Log](../.logs/a001-regression-final.log)
- Suítes locais de movimento horizontal (43), locomoção, arbitragem de ações, combate (60) e Roll (104) concluídas com sucesso.

O primeiro ensaio da regressão ampla usou um save de câmera. Ele não tinha o datapack da W001 nem a tecla K esperada pela fixture Slim. Os cinco resultados correspondentes ficaram registrados. A nova rodada passou as cinco checagens com o save de regressão que continha ambos os dados. As asserções foram mantidas. [Primeira rodada](../.logs/a001-regression.log)

## Desempenho

Referência dentro do mesmo cliente, Slim em Fancy, 854 × 480, VSync a 60 FPS. O tempo da armadura mede CPU para deformação e emissão de vértices e comandos dos passes vanilla. O tempo de quadro inclui o jogo e o restante do renderer. Cada cena teve de 71 a 110 quadros medidos neste perfil.

| Cena | Quadro médio | CPU da armadura por desenho |
| --- | ---: | ---: |
| Sem armadura em espaço aberto | 16,771 ms | — |
| Diamante completo em espaço aberto | 16,781 ms | 0,284 ms |
| Netherite em espaço aberto | 16,726 ms | 0,301 ms |
| Diamante encantado com trim de ouro | 16,785 ms | 0,602 ms |
| Deslocamento rápido | 16,753 ms | 0,344 ms |
| COMBAT | 16,745 ms | 0,324 ms |
| Natação para frente | 16,727 ms | 0,229 ms |
| Corredor estreito com fade | 16,721 ms | 0,433 ms |

Os quadros médios acompanham o limite de VSync. O ensaio separa o custo de CPU da armadura, mas deixa em aberto o custo isolado de GPU e o desempenho acima de 60 FPS. A captura de cor e profundidade da C002.2 reaproveita os buffers existentes e ocorre uma vez por desenho local durante fade estável. A armadura reutiliza a mesma pose do corpo e a mesma geometria deformada entre cor, tingimento, trim e glint. Uma rodada Classic em Fast mediu 16,700 ms por quadro e 0,287 ms de CPU para a armadura na janela do Roll ativo. O diagnóstico W002.4 continua emitindo seus contadores. Dados brutos em [a001-slim-1.log](../.logs/a001-slim-1.log).

## Limites

A integração cobre o jogador local em terceira pessoa com as variantes Classic e Slim. A primeira pessoa mantém o caminho de braços e itens que já existia. O suporte a armaduras vanilla reaproveita a resolução de texturas do Minecraft. Packs que só substituem essas texturas seguem a mesma resolução. Packs que mudam a geometria, shaders de terceiros e outras GPUs precisam de validação própria. A troca de skin na mesma sessão passou com a armadura equipada, incluindo o retorno à variante original. O cenário com dois clientes reais permanece pendente. O teste de opacidade remota usa uma entidade de jogador simulada no cliente.

Uma armadura de outro mod pode fornecer modelos, esqueletos, RenderTypes ou UVs próprios pelo Forge. Esses itens seguem a compatibilidade vanilla/CPM existente, assim como os contextos que já eram encaminhados a ela, como capa ou partes de skin desabilitadas. Um suporte futuro pode mapear o modelo e os pontos de articulação específicos de cada mod ao rig R3. Esta versão mantém a malha vanilla como base e deixa ajustes finos de clipping nas poses extremas sujeitos à aprovação visual no jogo do usuário.

## Artefato

Build Forge 47.4.28 concluído com Gradle offline e JDK 17. O JAR reobfuscado inclui a integração de armadura e os shaders da C002.2. A inspeção confirmou 16/16 modelos e arquivos de pose com hashes idênticos aos assets de origem e zero classes das fixtures de QA.

[JAR atualizado](../build/libs/towerofentropy-0.0.1.jar) · [Log do build](../.logs/a001-release.log)

SHA-256 `1F4F77C4CF747363315A4A048341EEBFF370BF844BF4A215F20E83C2D4D11887`
