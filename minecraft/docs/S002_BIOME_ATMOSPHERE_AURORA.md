# S002 — Atmosfera regional e aurora boreal

## Arquitetura

O Oculus 1.8.0 gera constantes `BIOME_*` a partir do registry compatível com Forge 1.20.1 e fornece o ID, a categoria e o clima no cliente. Os perfis usam a posição do jogador, inclusive em terceira pessoa. A amostragem é regional. Um vale distante continua recebendo o perfil do observador até que o jogador atravesse a fronteira. Os pesos de frio, vegetação, umidade, montanha e secura são suavizados na CPU antes de chegar ao GLSL. Nether e End ficam fora do perfil de superfície. [Uniforms de bioma no Iris](https://shaders.properties/current/reference/uniforms/biome/)

A lista fria inclui Snowy Plains, Ice Spikes, Frozen River, Frozen Ocean, Deep Frozen Ocean, Snowy Taiga, Grove, Jagged Peaks, Frozen Peaks, Snowy Slopes e Snowy Beach. Stony Peaks não participa da aurora. Florestas, taiga e selva recebem um perfil úmido leve. Rios e pântanos carregam mais névoa baixa. Montanhas enfatizam profundidade e desertos e savanas recebem uma resposta seca. Planícies usam o perfil geral.

## Cortinas e integração

O skydome projeta cortinas onduladas ao longo de uma faixa direcional contínua. A altura do tecido, as dobras e os filamentos têm movimentos lentos e parcialmente independentes. A emissão usa verde-esmeralda, ciano e violeta no alto. Leve avalia uma cortina, Equilibrado duas e Alto três. A camada é composta com o céu antes de estrelas, lua e nuvens. As nuvens atenuam a aurora de acordo com sua cobertura. A contribuição desaparece perto do horizonte, com a elevação do sol, durante chuva forte e ao sair de uma região fria.

Uma função determinística do dia controla a ocorrência. Sua intensidade é modulada pela noite e pelo clima interpolado. O menu **Aurora boreal** controla estado, frequência, intensidade, velocidade e qualidade. A implementação afeta o skydome principal. Reflexos existentes, iluminação de blocos e SSR conservam seus caminhos.

## Capturas e desempenho

As cenas usam um mundo Forge e o mesmo enquadramento, com o bioma aplicado pelo comando vanilla e conferido pelo servidor. A frequência foi ajustada para 100% nas capturas.

| Cenário | Captura |
| --- | --- |
| aurora_snowy_plains | [aurora_snowy_plains-aurora.png](assets/s002/aurora_snowy_plains-aurora.png) |
| aurora_ice_spikes | [aurora_ice_spikes-aurora.png](assets/s002/aurora_ice_spikes-aurora.png) |
| aurora_frozen_ocean | [aurora_frozen_ocean-aurora.png](assets/s002/aurora_frozen_ocean-aurora.png) |
| aurora_snowy_taiga | [aurora_snowy_taiga-aurora.png](assets/s002/aurora_snowy_taiga-aurora.png) |
| aurora_grove | [aurora_grove-aurora.png](assets/s002/aurora_grove-aurora.png) |
| aurora_frozen_peaks | [aurora_frozen_peaks-aurora.png](assets/s002/aurora_frozen_peaks-aurora.png) |
| aurora_dusk | [aurora_dusk-aurora.png](assets/s002/aurora_dusk-aurora.png) |
| aurora_desert | [aurora_desert-aurora.png](assets/s002/aurora_desert-aurora.png) |

Comparação com aurora desligada em Snowy Plains. [aurora_snowy_plains-off.png](assets/s002/aurora_snowy_plains-off.png)
Fronteira no tick 25 · [boundary-25.png](assets/s002/boundary-25.png)
Fronteira no tick 45 · [boundary-45.png](assets/s002/boundary-45.png)
Fronteira no tick 95 · [boundary-95.png](assets/s002/boundary-95.png)

A 854 × 480 em Snowy Plains, a GTX 1050 Ti registrou 231 FPS médios com FXAA e neblina automática e 226 FPS com a aurora habilitada, nas mesmas condições. O p95 variou de 5,83 para 5,50 ms entre as duas rodadas. A oscilação do servidor integrado afeta os percentis. O custo da aurora fica concentrado nos pixels de céu em noites elegíveis. A comparação em três resoluções e a rodada com aurora isolada integram a matriz. [Resultados de integração](S002_1_CINEMATIC_FOG.md)

No ensaio de fronteira, o servidor confirmou a passagem de Snowy Plains para Plains. Os quadros mostram o brilho na área fria e a atenuação após a passagem. As nuvens continuam capazes de esconder trechos das cortinas e a fase do movimento pode variar entre rodadas.

A aprovação artística fica com o usuário.
