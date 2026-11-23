# S002.8.1 — Luz dinâmica de equipamentos

## Diagnóstico

A S002.8 dependia de `heldBlockLightValue` e `heldBlockLightValue2`, escolhia a contribuição mais forte e aplicava a luz perto da câmera. A fonte mudava de lugar ao trocar de perspectiva. O shader também desconhecia a geometria entre a fonte e o receptor.

## Fontes e ancoragem

O cliente lê os itens efetivamente equipados na mão principal e na secundária. Cada fonte tem ID, proprietário, posição em coordenadas do mundo, socket, cor, força e raio. As tochas comuns e as lanternas usam cores quentes. As duas variantes Soul usam ciano, e a tocha de redstone tem menor força e alcance. Os perfis vanilla usam os níveis de emissão 14, 15, 10, 10 e 7, respectivamente. A variação da chama é contínua e pequena, e as lanternas usam emissão constante.

No R3 em terceira pessoa, a origem usa a matriz com que o renderer posiciona o item, capturada depois das transformações do antebraço, do pulso e do encaixe. Os sockets das costas e do cinto usam a matriz do tronco. A ponte toma a última pose renderizada e reaplica a translação interpolada do jogador. Em primeira pessoa e nos caminhos sem pose R3 recente, o fallback calcula o ponto no espaço do corpo usando tamanho, braço dominante, orientação corporal, passada e ataque. Uma transição de 90 ms pode suavizar a troca entre pose R3 e fallback. A presença do item é avaliada no frame atual. A retirada da fonte dispensa um histórico de intensidade.

Leve processa uma fonte, Equilibrado duas e Alto até quatro. O limite de alcance individual é de 7,5 blocos, reduzido nos dois primeiros presets. Brilho e cor entram na mesma etapa da luz de bloco e usam a compressão existente para combinar fontes. O `DYNAMIC_LIGHT` mantém seus quatro valores. O sistema funciona no cliente e usa exclusivamente estado de renderização.

## Oclusão

A ponte registra um sampler no Oculus 1.8.0. Uma textura de 256 × 49 contém os metadados das fontes e uma região de 23³ voxels perto do jogador. A região é consultada quando o jogador muda de célula ou quando avança o tick, e o cliente envia o volume à GPU quando os limites mudam. Cada voxel guarda um AABB quantizado. A busca no fragment shader atravessa as células do segmento entre receptor e emissor e intersecta sua caixa. A geometria pode estar fora da tela. A direção da normal e o raio rejeitam muitos receptores antes da busca. O mapa solar continua atendendo à luz do sol.

Paredes opacas usam a forma de colisão completa, e portas, slabs e alçapões usam a forma do estado atual. Vidro e folhas deixam passar a luz. Escadas, cercas e grades que têm várias caixas são aproximadas pela caixa envolvente. Essa aproximação pode bloquear vãos de uma conexão ou liberar parte de uma cerca acima da altura do voxel. O sistema produz oclusão por blocos para os emissores pontuais. Sombras de entidades e penumbras pontuais ficam fora dessa técnica.

## API para futuros attachments

Um mod Forge registra um provider exclusivamente no cliente. A informação de equipamento deve vir do slot de montagem do próprio mod. O provider pode retornar zero ou mais fontes, com um ID estável para cada emissor. Passe `null` como posição para usar o socket ou informe a posição mundial calculada pelo seu renderer. Passe `null` como emissão para selecionar o perfil vanilla do item, ou forneça uma `Emission` para um item do seu mod. O limite de posição é de três blocos do proprietário.

```java
var providerId = new ResourceLocation("meu_mod", "lanterna_mochila");
var registration = DynamicLightAttachments.register(providerId, (owner, partialTick) -> {
    ItemStack mounted = readEquippedLantern(owner);
    if (mounted.isEmpty()) return List.of();
    return List.of(new DynamicLightAttachments.Attachment(
        providerId, DynamicLightAttachments.Socket.BACK_CENTER, mounted, null, null, true));
});
```

Use `registration.close()` para remover o provider. IDs e sockets duplicados são filtrados para o mesmo proprietário. O registrador aceita chamadas concorrentes e consulta seus providers na thread de renderização. `EquippedLightSources.socketPosition(owner, socket, partialTick)` expõe o ponto mundial para um renderer de acessórios. A coleta atual considera o jogador local. A interface recebe o proprietário para permitir ampliar a coleta futura, com limites de distância e orçamento.

No gameplay normal, mão principal e secundária funcionam automaticamente. A galeria isolada registra temporariamente uma lanterna vanilla em `BACK_CENTER`, mostra seu modelo e mantém as duas mãos vazias. O registro é removido ao trocar de cena. A mochila real e seus slots pertencem ao futuro mod.

## Reproduzir

A partir de `minecraft/`, use `validation/s001/benchmark-atmosphere.ps1` com uma instância de QA em `-Template`. As cenas `equipped_torch_main`, `equipped_torch_off`, `equipped_lantern_main`, `equipped_lantern_off`, `equipped_dual`, `equipped_back`, `equipped_wall` e `equipped_glass` verificam os caminhos principais. `-EffectMatrix 'DYNAMIC_LIGHT=equipped_wall,DYNAMIC_LIGHT=equipped_glass'` alterna somente a opção de luz. A galeria fixa câmera e horário e verifica os itens equipados no cliente antes de coletar FPS. Use `-Slim` para o preset de skin Slim do mod e `-Scenes 'equipped_back_motion,equipped_swap'` para registrar a caminhada, a troca de perspectiva e a retirada da tocha.

Para reproduzir a comparação de pixels, substitua o caminho da instância de Forge no primeiro comando.

```powershell
$template = 'C:\caminho\instancia-forge'
$root = '.logs/s00281-occlusion-example'
.\validation\s001\benchmark-atmosphere.ps1 -Template $template -RunRoot $root `
    -Size 1280x720 -Variant Equilibrado -Warmup 65 `
    -EffectMatrix 'DYNAMIC_LIGHT=equipped_wall,DYNAMIC_LIGHT=equipped_glass,DYNAMIC_LIGHT=equipped_inventory'
.\validation\s001\check-equipped-light-occlusion.ps1 `
    -Screenshots "$root/1280x720-Equilibrado-Effects/screenshots"
```

## Medição na parede

A comparação em 1280 × 720 usou o canal vermelho das capturas reais com a opção ligada e desligada. Os 3.048 pontos no piso atrás da parede tiveram diferença máxima de zero. Os 6.350 pontos na face frontal ganharam em média 14,54 níveis de vermelho em 255. No cenário com vidro, a região atrás ganhou em média 0,74 nível e apresentou diferença máxima de quatro níveis. Uma tocha guardada no inventário produziu diferença zero nas duas regiões. Na S002.8 anterior, a mesma região atrás da parede ganhou em média 1,96 nível de vermelho e chegou a sete níveis acima da imagem de referência com a luz desligada. O roteiro `validation/s001/check-equipped-light-occlusion.ps1` reproduz as verificações do pacote novo.

[S002.8 anterior](assets/s0028_1/wall-before.png) · [Par da parede ligado](assets/s0028_1/wall-on.png) · [Parede desligado](assets/s0028_1/wall-off.png) · [Vidro ligado](assets/s0028_1/glass-on.png) · [Vidro desligado](assets/s0028_1/glass-off.png) · [Lanterna traseira](assets/s0028_1/back-lantern.png) · [Lanterna comum e Soul simultâneas](assets/s0028_1/dual-lanterns.png)

[Tocha na mão principal](assets/s0028_1/torch-main.png) · [Tocha na mão secundária](assets/s0028_1/torch-off.png) · [Lanterna na mão principal](assets/s0028_1/lantern-main.png) · [Lanterna na mão secundária](assets/s0028_1/lantern-off.png) · [Tocha de redstone](assets/s0028_1/redstone.png) · [Tocha Soul](assets/s0028_1/soul.png)

[Porta fechada](assets/s0028_1/door-closed.png) · [Porta aberta](assets/s0028_1/door-open.png) · [Inventário com luz ligada](assets/s0028_1/inventory-on.png) · [Inventário com luz desligada](assets/s0028_1/inventory-off.png) · [Duas fontes com luz ligada](assets/s0028_1/dual-on.png) · [Duas fontes com luz desligada](assets/s0028_1/dual-off.png)

As cenas adicionais mostram a luz em um corredor de um bloco, na face próxima de uma divisória entre salas e ao redor de uma coluna. A verificação numérica de vazamento usa a cena de parede opaca. [Corredor](assets/s0028_1/corridor.png) · [Divisória em terceira pessoa](assets/s0028_1/partition.png) · [Coluna](assets/s0028_1/column.png)

## Desempenho

Medições no cliente completo com GTX 1050 Ti, oito chunks e VSync desligado. Cada janela coletou 60 ticks após 65 ticks de aquecimento. O primeiro cenário de cada processo também absorve parte do custo inicial de carregamento, principalmente no 1% low. As rodadas de resolução nominal 1920 × 1080 renderizaram efetivamente a 1604 × 881.

| Framebuffer | Cena | S002.8 anterior | S002.8.1 Leve | S002.8.1 Equilibrado | S002.8.1 Alto |
| --- | --- | ---: | ---: | ---: | ---: |
| 1280 × 720 | Tocha na mão principal | 257,85 | 261,91 | 229,00 | 158,88 |
| 1280 × 720 | Parede opaca | 254,25 | 296,45 | 219,74 | 183,40 |
| 1280 × 720 | Lanterna traseira de QA | 256,65 | 285,17 | 212,76 | 179,50 |
| 1280 × 720 | Duas lanternas | 255,35 | 250,72 | 200,04 | 166,61 |

No Equilibrado, o custo em FPS a 720p ficou em cerca de 11% na tocha principal e 14% na parede em relação ao ZIP anterior nessas rodadas. A S002.8 anterior não emitia luz com as mãos vazias na cena traseira e usava uma única contribuição na cena de duas fontes. Em 1604 × 881, o Equilibrado registrou 153,13 FPS na parede, 154,63 com a lanterna traseira e 142,43 com duas fontes. No par de parede ligado/desligado a 720p, os primeiros ensaios registraram 141,74 e 241,28 FPS. A repetição a 720p registrou 192,96 FPS na parede, 179,08 FPS na lanterna traseira e 191,90 FPS com duas fontes. A diferença entre rodadas mostra sensibilidade ao aquecimento e ao cenário.

O CSV inclui FPS médio, 1% low, p95 e p99 em milissegundos, uso aproximado de GPU e pico de VRAM quando disponíveis. [Medições completas](S002_8_1_ADVANCED_DYNAMIC_LIGHTING_BENCHMARK.csv)

Em 854 × 480, o Equilibrado registrou 339,61 FPS na parede, 246,74 FPS na lanterna traseira e 301,57 FPS com duas fontes. A tocha principal, que abriu a rodada a 480p, registrou 163,18 FPS e 1% low de 24,65 FPS.

Na rodada adicional de 720p com Equilibrado, a tocha no corredor estreito registrou 108,96 FPS com a luz ligada e 198,02 FPS com a luz desligada. A cena da divisória registrou 202,55 e 282,44 FPS, e a coluna 213,34 e 267,30 FPS, na mesma ordem.

## Regressões de renderização

A suíte C0021 passou 95 verificações com Classic e 95 com Slim. A suíte de parkour e combate passou 30 verificações, incluindo os clips de parede e os casos com armadura A001. A galeria registrou transparência contextual de 12% no túnel e executou as cenas de água submersa, lava, aurora e SMAA em movimento. A rodada LabPBR executou as cenas de metais e emissão e a lanterna na mão secundária. As suítes de animação exercitam a pose e o movimento do personagem. A galeria de luz registra instantes específicos de caminhada e troca de equipamento.

A galeria usou o modelo vanilla da lanterna suspensa no socket das costas. Nas cenas de movimento, o jogador caminhou com as mãos vazias e alternou da terceira pessoa para a primeira e de volta. Outra cena retirou a tocha durante a caminhada e confirmou a remoção no cliente. A luz no piso desaparece na captura seguinte. [Classic](assets/s0028_1/back-motion-classic.png) · [Slim](assets/s0028_1/back-motion-slim.png) · [Primeira pessoa](assets/s0028_1/back-first-person.png) · [Tocha equipada](assets/s0028_1/swap-equipped.png) · [Tocha retirada](assets/s0028_1/swap-removed.png)

## Pacote instalável

O ZIP contém os 67 arquivos do shaderpack editável, conferidos individualmente por SHA-256. O SHA-256 do pacote é `BE09CFF13EF5F74876606C56AA86BE2DFF2574DAEB6AC4112C0CEBA682F9A357`. [ZIP instalável](../shaderpacks/Tower%20of%20Entropy%20-%20Fantasy%20Foundation.zip)

O build offline gerou o JAR com as classes de iluminação e excluiu a galeria de QA. A configuração standalone da galeria compilou. A cópia do ZIP instalada pela tarefa de desenvolvimento tem o mesmo SHA-256. `runClient` e `runClient -PtoeNoOculus` abriram o menu e encerraram com código zero. As medições no mundo usaram Oculus.
