# M002.R4 — Promoção oficial do Polymesh R3

13/11/2026. Decisão do usuário: R3 aprovado visualmente e promovido a renderer padrão. Esta decisão substitui a condição de laboratório dos relatórios R2/R3 e a recomendação provisória do QA. R3.1 permanece experimental e preservado.

## Arquitetura oficial

Minecraft 1.20.1 / Forge 47.4.28 / Java 17 / Polymesh 1.0.0 / CPM 0.6.26a. O build normal agora declara Polymesh como dependência de cliente e inclui uma cópia **byte a byte** do GLB R3 aprovado, com 12 ossos, skinning linear e as quatro animações originais. Não inclui a malha corretiva R3.1 como padrão. Nenhum asset Epic Fight foi copiado.

`client/PlayerRendererR3` é o adaptador oficial sobre a biblioteca existente. Na terceira pessoa suportada, ele inicia habilitado e desenha o jogador local sem `/toe_renderlab skinned`. Idle/Jog são selecionados pela movimentação/ponte de estado existente; Roll e Sword seguem as ações aceitas. A espada mantém o osso `103` e anchor `303`, com um desenho por corpo e cancelamento do desenho vanilla/CPM somente após sucesso.

O render não escreve posição, velocidade, colisão, alcance ou dano. A skin atual Classic é aplicada à mesma UV 64×64. O modelo aprovado e suas orientações/pivôs não foram remodelados ou convertidos novamente. O renderer normal fica desativado automaticamente quando um laboratório R1/R2/R3/R3.1/QA é iniciado, evitando duas implementações concorrentes.

### CPM como compatibilidade

CPM continua instalado e declarado como dependência. Ele deixou de desenhar o corpo principal na situação suportada, mas permanece como caminho de equipamento/poses não homologadas e como ponte técnica da infraestrutura anterior de ações. O início de ação ainda aguarda o registro/play bem-sucedido do CPM; o relógio de dano e a física permanecem no servidor. Remover essa ponte nesta etapa arriscaria regressão desnecessária.

Os modelos de compatibilidade de ações, antes disponíveis somente em `player_models/`, agora também acompanham o mod como recursos. Na ausência de arquivos legados, são extraídos para `config/towerofentropy/compat/`, cache dedicado do mod. Isso não sobrescreve modelos do usuário, skins, arquivos Godot ou `player_models/`. A validação foi feita em `run-r4/` sem a preparação manual dessa pasta.

Se houver apenas um modelo legado de locomoção, sem ações, o recurso completo empacotado também é escolhido; o arquivo legado permanece intacto. Esse cenário de migração/carregamento frio foi validado separadamente em 7/7 checks, incluindo Roll funcional e reconexão.

Futuros movimentos devem priorizar o pipeline Polymesh e estado autoritativo do mod. O uso atual de parâmetros CPM como adaptador é uma dependência técnica conhecida, não uma adoção do sistema de combate de terceiros.

### Fallbacks e diagnósticos

Primeira pessoa, qualquer armadura, offhand, capa habilitada com textura, Slim, mão principal esquerda, camadas da skin desabilitadas e estados não suportados liberam o evento ao caminho CPM/vanilla. Incluem água/lava, escada, montaria, voo, sprint, agachamento, uso de item, dano e invisibilidade/espectador. O Roll físico pode continuar visualmente no ar enquanto a gravidade o leva de uma borda; estados inválidos cancelam sua ação no servidor.

Equipamento continua visível; não foi implementado renderer de armaduras/capas. Outros jogadores permanecem no caminho de compatibilidade, pois o R3 remoto completo ainda não foi homologado. Isso é uma limitação explícita, não uma promessa de paridade visual multiplayer.

Comandos de cliente, sem autoridade de gameplay:

- `/toe_renderlab skinned`: volta ao R3 automático.
- `/toe_renderlab off`: libera o desenho para CPM/vanilla nesta sessão.
- `/toe_renderlab status`: mostra seleção/fallback.
- `/toe_renderlab idle|jog|roll|sword`: prévias visuais de diagnóstico.

Uma reconexão restaura o padrão R3. Combate continua opt-in com `/toe_combat on|off`; esses comandos permanecem protegidos por permissão de servidor. R3.1 continua disponível em seu perfil `-PtoeR31` e script anterior, sem remoção de arquivos.

O launcher `start-r31.ps1` reconhece a promoção e reutiliza o R3.1 aprovado, conferindo seus hashes em vez de regenerar a malha ou exigir os hashes antigos do código de combate. O ramo de preparação histórico foi preservado e há backup do launcher anterior. Os relatórios/benchmarks antigos permanecem históricos; suas expectativas de “JAR normal sem Polymesh/assets” não se aplicam à arquitetura promovida.

## Gate da Fase A

Antes de escrever o Roll físico, foram executados:

| Verificação | Resultado |
|---|---:|
| Cliente normal, modelo padrão antes de comandos/observador | passou |
| Animações, quatro ângulos, arma única, fallbacks e reconexão | 31/31 |
| Combate M002.0 na promoção | 132/132 |
| Sincronização visual dos impactos | 18/18 |
| Build normal | passou |
| Build experimental R3.1 preservado | passou |

Evidência do gate: `animation_pipeline/reports/M002_R4_PHASE_A_GATE.json`, `..._PROMOTION_RUNTIME.json`, `..._COMBAT_RUNTIME.json` e `..._COMBAT_RENDER_TRACE.json`. Não apareceram incompatibilidades graves; somente após esse gate começou o M002.1. As regressões finais pós-Roll passaram em 34/34 para renderização, 132/132 para combate e 166/166 para física; reinício também passou. Detalhes e capturas estão no [registro M002.1](TEST_LOG_M002_1.md).

## Configuração, preservação e execução

O task `prepareOfficialR3`, dependência de `processResources`, prepara cópias dos recursos locais aprovados antes de compilar. Os derivados continuam em `animation_pipeline/converted/m002_r4/`, ignorados pelo Git. O JAR normal contém apenas o R3 oficial e a compatibilidade necessária, não os recursos R3.1 ou testes. Polymesh/CPM continuam JARs separados; não são incorporados/shaded no JAR Tower of Entropy.

Na pasta `minecraft`:

```powershell
.\scripts\gradle.ps1 build --console=plain
.\scripts\gradle.ps1 runClient --console=plain
```

Entre normalmente, utilize uma skin Classic e F5. Nenhum comando de laboratório é necessário. Em primeira pessoa o fallback é esperado. Para jogar, instalar o JAR Tower of Entropy junto às versões compatíveis de Forge, CPM e Polymesh. Nenhuma autenticação ou configuração de launcher foi alterada.

Backup dos arquivos anteriores em `.downloads/m002_r4/backup/`; hashes e mudanças autorizadas em `M002_R4_BASELINE.json` / `M002_1_PRESERVATION.json`. R3, R3.1, código antigo e experimentos não foram removidos. Os movimentos de terceiros continuam sujeitos às licenças de origem; esta entrega local não autoriza publicar seus assets.

## Limites herdados

Persistem o contato coxa/abdômen e achatamentos do R3 aprovados pelo usuário. O Roll físico não troca a malha nem tenta resolver esses problemas. A avaliação de desempenho do QA em 854×480 é histórica e não foi repetida como benchmark neste marco. A promoção não homologa todas as skins, equipamentos, efeitos visuais, render remoto ou todas as condições de latência. CPM permanece fallback técnico onde necessário.
