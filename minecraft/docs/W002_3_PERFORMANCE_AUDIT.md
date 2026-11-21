# W002.3 — Auditoria preventiva de desempenho

## TAB e ciclo da arma

O cliente aceita a borda física `GLFW_PRESS` no contexto de jogo e mantém um booleano pendente até o fim do tick. Dois pressionamentos entregues antes desse consumo ocupam a mesma posição. Repetição do sistema operacional, tecla solta e eventos de menus ficam fora do envio. A ação usa a associação configurada e o tratamento da lista de jogadores da W002.2.

O pedido chega pelo canal de ações ao thread do servidor. Cada jogador tem uma sessão com a referência da pilha, o slot, a postura e um único prazo de seis ticks. Pedidos recebidos em DRAWING ou SHEATHING são descartados. Uma troca de equipamento invalida a postura anterior, limpa o prazo e cancela a ação de espada associada. Uma transição aceita apaga progresso de mineração em curso. O estado enviado usa o par postura/modo e só é publicado quando um dos dois muda, além dos reinícios de sessão. A conclusão da transição e uma nova borda podem produzir mudanças legítimas no mesmo tick.

O cliente substitui o estado recebido e valida slot e item. O R3 escolhe o ponto de montagem da arma a partir desse estado. O ciclo de TAB não enfileira clipes nem cria uma segunda sessão de ação. As filas de ataques existentes têm limite próprio. Cada pressão transmitida cria uma mensagem curta. Uma atualização de estado só é criada para publicação ou reinício de sessão. Não há coleção de solicitações de TAB mantida entre ticks. A leitura dos eventos Forge encontrou um ponto de captura para cada dispositivo, um tratamento do pedido de alternância no canal e um ponto de avanço da transição no tick do servidor.

## Detecção de aggro

A arma em DRAWN executa no máximo uma consulta a cada cinco ticks por jogador. O índice espacial visita seções de entidades carregadas que intersectam a região de busca. O filtro usa mob vivo, alvo atual igual ao jogador, ausência de aliança, raio de 24 blocos e classificação hostil ou neutra elegível. A consulta termina no primeiro resultado válido. Sem um alvo elegível, o custo acompanha os mobs que o índice retorna naquela região. O índice é reutilizado e a consulta não monta uma lista de resultados. Um evento de mudança de alvo não cria consulta adicional. A memória da última ameaça mantém COMBAT pela janela de graça. O modificador com ID fixo muda na entrada ou saída de COMBAT.

A instrumentação visita a região completa em uma amostra por janela somente quando ativada. Esse censo tem contagem e duração próprias. As outras consultas continuam terminando no primeiro alvo. `examined` mede as entradas entregues ao filtro no caminho normal, e não as verificações internas do índice. `aiming` conta os candidatos que passaram em todos os filtros na amostra. A diferença entre os dois mostra por que o custo de detecção pode ficar quase constante com muitos inimigos mirando o mesmo jogador.

## Ensaios em Forge

Duas execuções concluídas com cópias do mesmo mundo, servidor integrado, JDK 17, renderização a seis chunks e VSync limitado a 60 FPS. Cada fase estabiliza por 30 ticks e mede por 90 ticks. Os zumbis recebem alvo ao nascer e executam IA vanilla. O jogador fica invulnerável para repetir os cenários. B e F injetam bordas rápidas de TAB e eventos GLFW de repetição no barramento Forge do cliente. G envia pedidos de ataque leve, pesado e Roll pelo mesmo canal usado em jogo. O controle H usa 30 zumbis com a espada guardada e sem ações. Ele conserva o custo da IA e retira busca, zoom e movimento de COMBAT enquanto guarda a espada.

Em cada célula, `/` separa a rodada 1 da rodada 2. Onde há dois indicadores, `·` os separa, na ordem do cabeçalho. O 1% low usa a média do 1% mais lento de quadros. Cada fase registra 90 ticks e aproximadamente 260 a 271 quadros.

| Cena | FPS médio | 1% low | Quadro p95 / p99, ms | Tick médio / p95, ms | Heap máx., MiB | GC, contagem e ms |
| --- | ---: | ---: | --- | --- | ---: | --- |
| A | 59,79 / 59,68 | 48,52 / 44,06 | 19,262 / 19,630 · 20,403 / 20,929 | 3,092 / 3,011 · 4,148 / 4,149 | 1017 / 819 | 0 / 0 · 0 / 0 |
| B | 59,81 / 59,83 | 50,89 / 50,72 | 18,780 / 18,819 · 19,462 / 19,291 | 2,552 / 2,244 · 4,051 / 2,766 | 1101 / 901 | 0 / 0 · 0 / 0 |
| C | 59,58 / 59,76 | 47,11 / 49,30 | 18,998 / 19,073 · 20,773 / 19,981 | 2,186 / 2,149 · 3,131 / 2,762 | 1179 / 983 | 0 / 0 · 0 / 0 |
| D | 59,77 / 59,69 | 49,94 / 47,15 | 19,075 / 18,749 · 19,839 / 20,416 | 2,550 / 2,345 · 3,347 / 3,087 | 1271 / 1075 | 0 / 0 · 0 / 0 |
| E | 59,71 / 59,75 | 47,17 / 50,10 | 18,671 / 18,891 · 19,533 / 19,388 | 2,913 / 2,670 · 4,579 / 3,594 | 1351 / 1193 | 1 / 0 · 7 / 0 |
| F | 59,80 / 59,15 | 49,67 / 29,62 | 19,059 / 19,268 · 19,900 / 21,396 | 2,687 / 3,074 · 3,816 / 4,544 | 1030 / 1291 | 0 / 1 · 0 / 6 |
| G | 59,73 / 59,49 | 46,74 / 38,83 | 18,944 / 19,319 · 19,995 / 21,767 | 3,043 / 3,391 · 4,343 / 6,983 | 1178 / 806 | 0 / 0 · 0 / 0 |
| H | 57,39 / 59,72 | 12,52 / 50,30 | 19,343 / 18,963 · 20,419 / 19,681 | 3,047 / 2,663 · 4,236 / 3,501 | 1294 / 928 | 0 / 0 · 0 / 0 |

| Cena | Pedidos TAB aceitos/recebidos | Pacotes recebidos / enviados | Estados / velocidade | Buscas / candidatos | Censo / mirando | Busca normal máx., µs |
| --- | ---: | --- | --- | --- | --- | ---: |
| A | 0 / 0 | 0 / 0 · 0 / 0 | 0 / 0 · 0 / 0 | 0 / 0 · 0 / 0 | 0 / 0 · 0 / 0 | 0 / 0 |
| B | 15 de 43 / 15 de 44 | 43 / 44 · 29 / 29 | 29 / 29 · 0 / 0 | 6 / 6 · 0 / 0 | 0 / 0 · 0 / 0 | 29 / 23 |
| C | 0 / 0 | 0 / 0 · 0 / 0 | 0 / 0 · 0 / 0 | 17 / 17 · 17 / 17 | 1 / 1 · 1 / 1 | 13 / 25 |
| D | 0 / 0 | 0 / 0 · 0 / 0 | 0 / 0 · 0 / 0 | 17 / 17 · 17 / 17 | 10 / 10 · 10 / 10 | 10 / 9 |
| E | 0 / 0 | 0 / 0 · 0 / 0 | 0 / 0 · 0 / 0 | 17 / 17 · 17 / 17 | 30 / 30 · 30 / 30 | 35 / 7 |
| F | 15 de 44 / 15 de 44 | 44 / 44 · 29 / 29 | 29 / 29 · 15 / 15 | 7 / 7 · 7 / 7 | 30 / 30 · 30 / 30 | 10 / 9 |
| G | 0 / 0 | 5 / 5 · 11 / 11 | 0 / 0 · 0 / 0 | 17 / 17 · 17 / 17 | 30 / 30 · 30 / 30 | 9 / 10 |
| H | 0 / 0 | 0 / 0 · 0 / 0 | 0 / 0 · 0 / 0 | 0 / 0 · 0 / 0 | 0 / 0 · 0 / 0 | 0 / 0 |

- B e F aceitaram 15 alternâncias em cada rodada, com 29 publicações em cada janela. B recebeu 43 e 44 pedidos. F recebeu 44 nas duas rodadas. Os demais pedidos não prolongaram nem reiniciaram as transições. F alterou o atributo 15 vezes enquanto entrava e saía de COMBAT. C, D e E ficaram estáveis em COMBAT depois do aquecimento e tiveram zero publicações e zero alterações de velocidade.
- C, D, E e G fizeram 18 buscas por janela com arma sacada, sendo 17 normais e um censo. O caminho normal avaliou 17 candidatos em 90 ticks, cerca de 0,19 por tick. Em E, essas 17 consultas consumiram 148 e 116 µs no total. O censo dos 30 zumbis encontrou 30 alvos elegíveis e levou 42 e 33 µs. Nas cenas A e H o diagnóstico não executou censo, portanto o zero nessa coluna indica ausência de amostra.
- G registrou impactos após 4 e 12 ticks, correspondentes ao leve e ao pesado. O Roll iniciou e completou em ambas as rodadas. Os impactos foram resolvidos sem alvo no alcance naquele instante. As regressões de dano com alvo aparecem abaixo.
- Com 30 zumbis, o tick médio de E foi 2,91 e 2,67 ms. No controle H, foi 3,05 e 2,66 ms. A variação entre rodadas e a ordem das fases limitam a atribuição de diferença ao COMBAT. A comparação mantém o mod instalado e usa a postura guardada como controle parcial. A auditoria preservou a lógica W002.2, então não há uma correção de gameplay para medir num A/B entre versões.
- F manteve p95 abaixo de 19,3 ms nas duas rodadas, com 1% low de 49,67 e 29,62 FPS. O controle H também teve cauda variável, com 12,52 e 50,30 FPS. As duas rodadas não reproduziram o travamento severo relatado. As janelas curtas e os quadros mais lentos ainda justificam a captura no perfil real.

Logs brutos em `.logs/w0023-run1.log` e `.logs/w0023-run2.log`. Para repetir a fixture fora do JAR distribuído, prepare uma cópia do mundo `Global Survival one` e das opções numa pasta isolada. Dentro de `minecraft`, execute o comando abaixo com o caminho do perfil.

```powershell
.\scripts\gradle.ps1 --offline runClient -PtoeW0023Qa '-PtoeW002GameDir=.logs/w0023-run1'
```

## Diagnóstico em jogo

1. Entre com permissão de operador e rode `/toe_combat diagnostics on` com o jogador que será observado. O diagnóstico acompanha um jogador por servidor. Um novo `on` encerra a coleta anterior.
2. Reproduza com a câmera e o equipamento do caso original. Espere pelo menos dez segundos por cenário. Alterne TAB, arme-se diante de um zumbi e repita com grupos maiores. Anote o horário de cada travamento.
3. Rode `/toe_combat diagnostics off`. O comando imprime a janela parcial e desativa a coleta sem reinício. O diagnóstico também se encerra ao sair do servidor.
4. No `logs/latest.log` do servidor, procure `TOE_W0023`. Cada linha resume até 200 ticks e registra postura, ameaça, alternâncias aceitas/recebidas, publicações, alterações de velocidade, pacotes de combate nos dois sentidos, buscas, candidatos, censo, Roll e exceções interceptadas pelo canal de combate.
5. Para medir o quadro completo no cliente, pressione **F3+L** e recolha o ZIP criado em `debug/profiling`. Procure `toeThreatScan`, `toeR3`, `toeR3Pose` e `toeShoulderCamera`. Para pausas de GC e eventos de JVM, use `/jfr start`, reproduza o cenário e finalize com `/jfr stop`. Envie o perfil, o trecho de log e o horário correspondente.

A coleta vem desabilitada. Sem uma sessão ativa, os caminhos de combate não leem o relógio de diagnóstico, não fazem o censo e não escrevem logs agregados. Com ela ativa, os logs omitem nomes, UUIDs e coordenadas. A contagem de pacotes se refere às mensagens de arma, ataque, modo e Roll da Tower of Entropy, medida nas fronteiras de envio e tratamento. Tráfego vanilla e parkour fica fora do total.

## Correções e validação

A auditoria mantém as regras de W002.2. A W002.3 acrescenta contadores, o comando de diagnóstico e uma fixture isolada de carga. A revisão do caminho de combate não mostrou uma fila crescente de TAB ou a necessidade de alterar cadência, raio, recuperação, dano, combos, Roll ou apresentação.

- Build Forge 1.20.1 (47.4.28) em Gradle offline e JDK 17. A auditoria do JAR comparou 16/16 arquivos de modelo e pose com as fontes geradas e confirmou que a fixture de carga ficou fora do pacote.
- Suítes locais concluídas para movimento horizontal (43), locomoção, arbitragem de ações, combate (60) e Roll (104).
- Classic e Slim passaram em 59/59 checagens cada, com TAB, zoom, Roll, ataques no ar, poses R3 e interações com a espada guardada. Logs em `.logs/w0023-classic.log` e `.logs/w0023-slim.log`.
- Regressão Slim concluída com 50/50 para W001, 36/36 para W001.1/C001.3 e 29/29 na expansão de movimento e parkour. Log em `.logs/w0023-regression.log`.

JAR atualizado em `build/libs/towerofentropy-0.0.1.jar`, com SHA-256 `8F18F8C2293B432DB0B9A12E63664D162682BB44ACE0D3F04FAD9CC7325A72AA`.

Os cenários automatizados cobrem janelas curtas com servidor integrado e um cliente. FPS acima do limite de VSync fica fora da medição. O tempo de coleta do MXBean é agregado da JVM e inclui o cliente no servidor integrado. Ele não substitui um perfil de pausas individuais. Validação em servidor dedicado, sessões longas, outros conjuntos de mods e o travamento original seguem em aberto.
