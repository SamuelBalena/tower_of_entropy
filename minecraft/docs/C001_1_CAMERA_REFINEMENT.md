# C001.1 — Refinamento da câmera e dano em mobs passivos

## Enquadramento e estabilidade

O offset padrão passou de **0,85 para 0,9775 bloco** por ombro, um aumento de 15%. A distância padrão continua em 4, com zoom entre 2 e 8 blocos. Altura, FOV, mira no centro e atalho B continuam com os valores e controles da C001. O offset continua configurável em `config/towerofentropy-camera-client.toml`. A primeira carga migra o valor antigo de 0,85 e conserva outras preferências gravadas. Em janelas muito estreitas, o afastamento lateral tem um limite simétrico calculado pelo espaço projetado.

A câmera anterior apontava diretamente para cada ponto de mira novo. A alternância entre um bloco próximo, uma entidade e o fundo mudava de uma vez a correção angular do ombro, mesmo que o jogador e o mouse estivessem parados. Uma segunda mudança imediata ocorria na recuperação da distância quando o trajeto da câmera ficava livre.

O filtro exponencial usa o tempo entre quadros. A posição do pivô acompanha os olhos com amortecimento leve e deslocamento limitado, maior na vertical para saltos e aterrissagens. O cálculo da mira filtra apenas o inverso da distância de convergência. A direção de visão do jogador entra no cálculo a cada quadro, sem filtro de yaw ou pitch do mouse. A câmera recua imediatamente até uma posição segura se houver colisão e recupera o afastamento suavemente quando o espaço volta a ficar livre. Trocas de mundo, teleporte e pausas longas reinicializam os filtros. Primeira pessoa e câmera frontal mantêm o caminho vanilla.

## Dano e seleção no servidor

O combo de espada cancelava o ataque vanilla para evitar dano duplicado. O resolvedor que deveria aplicar o golpe excluía qualquer entidade que não implementasse `Enemy`. Isso explica a diferença entre vacas e galinhas e mobs hostis com a espada personalizada. O filtro também excluía mobs de outros mods por namespace. Jab/Cross continua observando o swing vanilla e a câmera continua consumindo o alvo visual sem substituir `Minecraft.hitResult`.

O servidor aceita mobs vivos, atacáveis e vulneráveis. Mantém fora do combo jogadores, aliados e animais domesticados pertencentes ao atacante. A seleção dá preferência à hitbox que o raio dos olhos atingiu dentro das regras existentes de distância e arco. O raio termina no primeiro bloco sólido. Se não houver um alvo diretamente mirado, permanece a seleção por proximidade dentro do arco, com linha de visão até o centro da hitbox. A decisão usa estado do servidor e dispensa IDs de alvo vindos da câmera. O caminho dispara o evento de ataque do Forge e respeita seu cancelamento e o callback do item antes de chamar `hurt` uma vez. O bloqueio de ataques vanilla duplicados permanece ativo. Dano configurado, cooldown, atributos, animações e interações contextuais do botão direito continuam no fluxo anterior.

## Arquivos

- `CameraSettings.java`, `CameraDamping.java` e `ShoulderCamera.java` ajustam composição, migração, recuperação de distância e suavização.
- `SwordTargeting.java` e `VanillaAttackGuard.java` selecionam alvos e respeitam a proteção do Forge.
- `ClientCombatSmokeTest.java` atualiza a expectativa antiga que considerava a vaca inelegível.
- `CameraRuntimeTest.java`, `CameraRefinementRuntimeTest.java`, `ExpansionRuntimeTest.java`, `build.gradle` e `.gitignore` cobrem os casos em perfis isolados de QA.

## Limitações e validação visual

A convergência exata é atingida depois de o novo foco estabilizar. Durante uma mudança brusca de profundidade, a mira fica no centro enquanto o enquadramento converge para o alvo real. Alcance, validação de linha de visão e colisões continuam valendo durante a transição. O recorte imediato continua necessário quando uma parede invade o espaço atual da câmera. Transparência automática, adaptação a corredores, primeira pessoa emergencial e Camera Shake ficam para as etapas seguintes. Um blur leve exigiria um passe visual separado e permanece só como possibilidade. A nova composição e a sensação de movimento ficam pendentes de revisão do usuário.

## Validação

Passaram as cinco suítes de lógica existentes: `test-horizontal.ps1`, `test-combat.ps1`, `test-roll.ps1`, `test-locomotion.ps1` e `test-actions.ps1`.

A matriz Slim completou **63 ataques válidos** e confirmou **63 seleções do alvo pelo raycast**. Vaca, galinha, porco, ovelha, zumbi, creeper e esqueleto foram testados com mão vazia, espada e graveto, tanto no ombro direito com zoom 2 como no esquerdo com zoom 8 e em primeira pessoa. No Classic, a primeira rodada completou 21 combinações de mob e equipamento. Exemplos com espada na leitura do servidor foram vaca 10 → 6, galinha 4 → 0, porco 10 → 6, ovelha 8 → 4 e creeper 20 → 16. Variações entre golpes de combo e mitigação vanilla explicam outros valores de dano.

No Slim, o teste bloqueou o alvo fora de alcance e o alvo atrás de parede, aceitou a hitbox parcialmente exposta e confirmou o veto do evento Forge. O cálculo exponencial chegou ao mesmo resultado a 30, 60 e 144 FPS simulados. A câmera se estabilizou após o salto. As verificações de pixels confirmaram a mira centralizada em 854 × 480 e 1031 × 641. A migração leu uma configuração Slim com 0,85 e gravou 0,9775.

Duas medições iniciais exigiram correção dos cenários. O primeiro salto Classic ainda tinha blocos do teste anterior atrás da câmera. O primeiro caso vanilla de parede no Slim criou a vaca com a hitbox dentro do bloco, o que invalidou a leitura de dano. Os cenários corrigidos removem toda a parede no primeiro caso e deslocam a parede para que a vaca fique completamente atrás dela no segundo. Os logs iniciais ficaram preservados.

A partir de `minecraft`, os perfis de QA executados usam cópias dos mundos em `run-c001-1-camera-qa/` e escrevem novas capturas em `animation_pipeline/converted/c001-1-camera-qa/`. Os demais diretórios de captura são históricos.

```powershell
.\scripts\gradle.ps1 build --offline --console=plain
.\scripts\gradle.ps1 runClient --offline --console=plain -PtoeExpansionTest -PtoeC001RefinementQa -x downloadAssets
.\scripts\gradle.ps1 runClient --offline --console=plain -PtoeExpansionTest -PtoeGlobalSlim -PtoeC001RefinementQa -x downloadAssets
.\scripts\gradle.ps1 runClient --offline --console=plain -PtoeExpansionTest -PtoeC001RefinementQa -PtoeC001StabilityQa -x downloadAssets
.\scripts\gradle.ps1 runClient --offline --console=plain -PtoeGlobalCoverage -PtoeGlobalSlim -PtoeGlobalSkinCheck -PtoeExpansionQa -PtoeC001RefinementQa -x downloadAssets
.\scripts\gradle.ps1 runClient --offline --console=plain -PtoeCombatTest -PtoeC001LegacyCombatQa -x downloadAssets
```

A regressão atual do R3 passou em **18/18**, incluindo espada, Roll, arco, consumo, agachamento, salto e reinicialização. O servidor confirmou a flecha no alvo livre e a flecha parada na face da parede. As verificações de caminhada, Sprint e salto em oito direções, Sprint Jump, Wall Jump, Ledge Hang e Ledge Climb dos perfis Classic e Slim também passaram.

A suíte histórica M002.0 registrou **103/132**. Confirmou o dano na vaca e a exclusão de jogadores, além dos casos de parede, arco de ataque e alcance. As 29 divergências incluem expectativas de nenhum avanço do atacante, de dano único sob cliques enfileirados, de restrição à espada de ferro, de modo inicialmente desligado e de reprodução pelo CPM. Essas expectativas não foram reescritas para fazer a suíte histórica passar. A expectativa referente ao alvo passivo foi atualizada por fazer parte desta mudança. Os resultados detalhados ficam em `.logs/c0011-legacy-combat.log`; essa suíte rodou numa cópia de mundo separada.

A repetição Classic passou em **29/29**. Em espaço livre, confirmou afastamento de aproximadamente 0,9775 em ambos os lados, transição de foco distribuída por vários quadros e retorno à altura de repouso depois do salto. A repetição confirmou, no servidor, que o alcance e a parede impediram dano vanilla e de espada. A vaca inteira atrás da parede ficou com 10 pontos de vida. O teste acertou a parte realmente exposta de uma hitbox e o veto Forge bloqueou um novo golpe.

O build Forge final passou. A auditoria verificou os GLBs Classic e Slim byte a byte contra o pacote anterior e constatou zero classes de QA no JAR. Logs desta rodada ficam em `.logs/c0011-*.log`. Capturas históricas foram preservadas.

- JAR: `build/libs/towerofentropy-0.0.1.jar`
- SHA-256: `6EC0C037B9D382BAC698F9F536D1EED8450CA0599961F83C494A96C98B650A59`
