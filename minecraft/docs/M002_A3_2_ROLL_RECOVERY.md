# M002.A3.2 — Roll responsivo

14/11/2026. O alvo físico permanece **2,5 blocos em 14 ticks / 0,7 s**, com direção normalizada e velocidade nominal de 2,5/14 bloco por passo. Sprint não soma velocidade à esquiva.

## Causas encontradas

`LocomotionController.supported` excluía `isSprinting()`, e esse mesmo predicado validava a entrada do Roll. A preparação visual CPM também podia impor cerca de 12 ticks após abandonar Sprint para recolocar o modelo/registro. Por fim, o servidor mantinha a ação ocupada por 32 ticks, apesar de o deslocamento acabar no tick 14.

Agora `supportedAction` mantém as validações de vida, pose, terreno, água/lava, montaria, voo, uso de item, dano e menus, permitindo a intenção em Sprint. A observação de locomoção e a elegibilidade de uma ação são separadas. Após autorização, Sprint cede prioridade ao Roll.

O relógio físico/visual próprio inicia na aceitação do servidor, sem esperar o registro CPM. A compatibilidade CPM continua sendo preparada, mas não controla o início do Polymesh. O encerramento limpa apenas o slot de Roll, sem cancelar um novo golpe por engano.

## Recuperação

O cliente devolve o controle após os 14 passos efetivos e confirma o término pelo mesmo canal ordenado, depois do pacote vanilla de posição final. O servidor valida essa posição antes de encerrar a ação; há watchdog de 20 ticks para confirmação ausente. Esse watchdog não é uma recuperação adicional imposta ao cliente. O Roll deixa de receber sobrescritas de velocidade do servidor a cada tick, evitando que pacotes atrasados cortem ou prolonguem seus passos previstos. O arquivo UAL1 continua com 45 amostras e duração nominal 1,467 s.

O sampler remapeia os primeiros 0,5 s para 0–1 s do clipe e os últimos 0,2 s para 1–1,4667 s. A rotação completa aparece no trecho inicial; os quadros finais de recuperação chegam a uma postura próxima da locomoção. A saída usa blend de **80 ms**, por snapshot local de pose, preservando comprimentos dos ossos. Não se corta o personagem invertido nem se edita o GLB.

Controle horizontal volta ao terminar a fase física, sujeito à autorização/rede e impedimentos reais. Continuando as entradas, o observador retoma Walk/Jog/Sprint; não há obrigação de passar por Idle. O retorno a Sprint acompanha a retomada vanilla, sem aceleração adicional do Roll.

I-frames continuam nos ticks **3 ≤ idade < 10**, isto é, 150–500 ms desde o início autoritativo. `RollDamageFilter` e a regra desses limites não foram alterados. Colisões, proteção de limites e cancelamentos inválidos continuam ativos. O observador de locomoção usa o mesmo limite de passo de 0,8 bloco já aplicado pelo envelope do Roll; o limite genérico de teleporte continua 0,5 fora da ação. Isso evita que três passos válidos agrupados sejam rejeitados por um segundo limite incompatível.

Consulte medições, capturas e as ocorrências das rodadas iniciais no [registro de testes](TEST_LOG_M002_A3_2.md). Aprovação estética da compressão temporal continua reservada ao usuário. Servidor dedicado, latência e execução sob travamentos prolongados não são homologados por testes Singleplayer.
