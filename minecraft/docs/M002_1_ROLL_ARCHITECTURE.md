# M002.1 — Roll físico funcional

13/11/2026. Implementado somente depois da validação da promoção R3, registrada no [relatório R4](M002_R4_POLYMESH_PROMOTION.md). O renderizador normal é Polymesh R3; o código de movimento não depende de flags ou comandos experimentais.

## Comportamento

Com `/toe_combat on`, V solicita esquiva na direção WASD. Diagonais são normalizadas; sem direção pressionada, utiliza-se frente. A direção é capturada no pedido e convertida usando o yaw conhecido pelo servidor, permanecendo fixa durante a ação. Mouse/câmera continuam independentes. Com combate desligado, V preserva o Roll visual anterior, sem deslocamento físico.

| Parâmetro | Valor |
|---|---:|
| Animação | UAL1 `Roll`, 45 quadros, inalterada |
| Duração visual | 1,466667 s, arredondada para 1,467 s |
| Distância desejada em chão livre/plano | 2,5 blocos |
| Janela de deslocamento | 14 ticks = 0,7 s a 20 TPS |
| Velocidade horizontal por passo | 2,5 / 14 = 0,178571 bloco/tick |
| Reserva de ação/recuperação | 32 ticks, incluindo a margem de transporte já existente |
| Dano de espada | inalterado, 4 HP brutos por impacto elegível |

A animação não foi acelerada, substituída ou editada. Após 14 passos o impulso horizontal termina; a ação permanece em recuperação até o término autoritativo. As teclas de movimento voltam a funcionar ao liberar a ação. Idle/Jog retornam conforme estado suportado; queda/estado incompatível usa fallback seguro.

Colisão e terreno podem encurtar o deslocamento. Não há teleport para garantir 2,5 blocos através de paredes. A janela é em ticks; sob TPS baixo sua duração real pode variar. O teste de referência é chão livre a 20 TPS.

## Autoridade e fluxo

1. `ActionKeys` lê os impulsos reais de movimento quando V é acionado; `ActionNetwork` envia ação + dois floats de direção. Não envia posição-alvo, distância, velocidade, dano ou ID de alvo.
2. `RollController.request` valida modo de combate, jogador, menu/estado, direção finita e limitada a [-1,1], além de exclusão de ataque/Roll. O servidor calcula a direção unitária em espaço de mundo e cria uma sessão pendente.
3. A arbitragem `VisualActionWindow` continua sendo primeira solicitação vence, sem fila ou extensão por spam. O carregamento frio aguarda a ponte de compatibilidade estar pronta; nesse período o input é suspenso para não somar caminhada involuntária ao dash.
4. Após o play da animação ser confirmado, o servidor fixa o tick inicial e envia o estado/direção aprovada ao cliente. A previsão local aplica **exatamente 14 passos**, contando ticks locais; não subtrai diretamente os relógios de dois níveis, evitando um passo duplicado observado na primeira prova.
5. O servidor também aplica o impulso limitado; `travel/move` vanilla continuam resolvendo colisões, chão e gravidade. O componente Y é preservado; não se escreveu uma nova fórmula de gravidade, movimento vertical ou caixa de colisão.
6. O servidor acompanha progresso longitudinal, desvio lateral, salto de posição e subida por tick. Excesso cancela a ação e reconcilia ao último ponto seguro. O fim normal não usa essa correção: a suíte mede 2,5 blocos e razão `complete`, sem overshoot/recuo.
7. Ao terminar/cancelar, limpa-se a velocidade X/Z nos dois lados, preservando Y. Pacotes de parada e handlers de morte, respawn, logout e reinício limpam a sessão.

O cliente suprime WASD/jump durante pedido aceito, movimento e recuperação; não soma aceleração vanilla ao impulso. O servidor continua sendo autoridade, e o cliente não escolhe duração/distância. A contagem local serve à previsão e a renderização, não concede permissão de ação ou dano.

## Validação de movimento

O limite adicional admite até 0,15 bloco de tolerância longitudinal, 0,35 lateral, 0,8 por atualização e 0,7 de subida antes de considerar uma mudança inválida. Esses limites não alteram a velocidade nominal, nem substituem a validação de movimento/collision do Minecraft. São margens para detectar desvios/teleportes durante a sessão. Nenhuma dessas margens é usada para aumentar a distância normal.

Parede bloqueia o avanço pela física vanilla; não dispara um salto ou atravessamento. Ao sair de uma borda, o Roll mantém sua janela horizontal e cai normalmente. Água/lava, escadas, montaria/voo, menu, uso de item, dano e estados inválidos interrompem a ação. O Roll não impede dano, não concede invulnerabilidade, não altera resistência/armadura e não modifica death/fall damage.

## Exclusão e animação

`CombatController.requestSword` rejeita ataques enquanto há Roll pendente/ativo. O pedido de Roll rejeita o ataque em andamento, além de respeitar a janela de animação existente. Spam não reinicia o timer nem acumula velocidade. O dano, seleção de alvo, alcance, arco e impacto no quarto tick continuam nos mesmos arquivos/regras anteriores.

O renderer R3 usa o estado aprovado de Roll para fase e yaw visual. Isso orienta a malha para o vetor de deslocamento sem mudar o clipe original ou a rotação física/câmera do jogador. A espada permanece ligada ao mesmo osso e acompanha o modelo. A ponte CPM continua necessária para os comandos/ações legados e fallbacks; o render principal é Polymesh.

O observador antigo de locomoção tinha histerese e fallback em marcha lateral/traseira. Enquanto um Roll válido está pendente/ativo, sua direção não é classificada como Jog para frente nem cancelada durante essa histerese. Fora do Roll, a seleção anterior continua funcionando.

## Rede, sessões e compatibilidade

O canal existente `towerofentropy:visual_actions` passou do protocolo 2 para **3**: C2S ação/direção e S2C estado aprovado. Cliente e servidor devem usar esta versão do mod; a mudança não toca autenticação/licenciamento. O transporte é ordenado e processado na thread lógica correta.

Combat mode continua opt-in por sessão. Login/respawn/reconexão não mantêm um Roll antigo ou combate habilitado. O renderer R3, ao contrário, volta habilitado como padrão. O histórico diagnóstico da última esquiva não deve ser interpretado como uma nova ação; a suíte compara o tick do caso atual para não confundir registros antigos com pedidos rejeitados.

Com armadura, offhand, capa ou primeira pessoa, a mesma física continua funcionando e a visualização usa fallback. A orientação/qualidade do fallback pode diferir do R3. Outros jogadores seguem o caminho visual compatível ainda não homologado como malha R3 remota; a posição física continua sendo replicada pelo Minecraft.

## Arquivos e manutenção

- `combat/RollRules.java`: parâmetros, direção normalizada e limites puros.
- `combat/RollController.java`: sessão lógica de servidor, início, impulso, validação e limpeza.
- `client/ClientRollMotion.java`: previsão limitada, bloqueio de input e fase visual.
- `animation/ActionNetwork.java` / `ActionKeys.java`: intentos WASD/V e estado S2C.
- `animation/LocomotionController.java`: compatibilidade de arbitragens e callback de início.
- `combat/CombatController.java`: exclusão entre ações; lógica de dano preservada.
- `validation/roll/`: fixture isolada, excluída do JAR normal.

Veja [testes e evidências](TEST_LOG_M002_1.md). Não há stamina, i-frames, combos, nova malha, novas bibliotecas de gameplay ou M002.2. Latência remota/TPS degradado ainda exige ensaio próprio; não se declara homologação em servidor público a partir de um servidor integrado local.

Resultado final integrado: 166/166 checks físicos, 132/132 de combate, 34/34 de render e 18/18 amostras de impacto. Oito sentidos mediram 2,500 blocos em chão livre, parede bloqueou em 0,200 e a borda preservou a queda. O JAR normal oficial contém R3 e compatibilidade CPM, e exclui R3.1/fixtures. Não houve avanço para M002.2.
