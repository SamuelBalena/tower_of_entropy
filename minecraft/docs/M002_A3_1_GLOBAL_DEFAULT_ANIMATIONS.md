# M002.A3.1 — Animações globais por padrão

13/11/2026. Minecraft Java 1.20.1, Forge 47.4.28, Java 17 x64.

## Funcionamento

Polymesh R3 está ativo desde a inicialização normal. O observador de estados usa o jogador local, movimento real, pose, água, dano e uso dos itens. Não consulta nome, seed, diretório ou marcador de mundo. `CombatController` inicia em AUTO e reconhece espada na mão principal. Nenhum comando ou circuito ativa esses sistemas.

Shift, Espaço, ATTACK e USE seguem os controles remapeados do Minecraft. V é a associação inicial, remapeável, do Roll. O servidor continua validando combate, impactos, esquiva e i-frames. Este marco não altera balanceamento, colisões, velocidade ou animações originais.

Além do Classic aprovado, o renderizador seleciona `steve_a3_slim` quando `getModelName()` retorna `slim`. A textura vem do próprio jogador. Detalhes em [compatibilidade de skins](M002_A3_1_SKIN_COMPATIBILITY.md).

O A3 já era independente do circuito; o bloqueio concreto encontrado era a exclusão de Slim. Foi acrescentada também verificação de presença do canal de rede antes de interceptar controles personalizados. O protocolo obrigatório continua sendo 6: conexão sem protocolo compatível não é homologada nem artificialmente aceita.

O cache de compatibilidade e a consulta opcional a `player_models` usam agora os diretórios do perfil informados pelo Forge (`CONFIGDIR`/`GAMEDIR`), sem depender do diretório de trabalho escolhido pelo launcher. O fallback incorporado continua funcionando quando não existe modelo externo.

## Instalação normal

1. Use um perfil Minecraft **Java 1.20.1 / Forge 47.4.28**, com Java 17 x64.
2. Com o jogo fechado, coloque `towerofentropy-0.0.1.jar`, o **Polymesh Forge 1.0.0 para 1.20.1** e **CustomPlayerModels Forge 1.20-0.6.26a** na pasta `mods` desse perfil. Não instale as variantes Fabric nem Full Motion/Epic Fight para esta implementação.
3. Inicie o perfil e crie ou abra qualquer mundo Singleplayer. F5 permite observar o corpo. Equipe espada para o combate personalizado; ações contextuais prioritárias e itens não suportados continuam vanilla.

Nesta máquina, os JARs de dependência já verificados ficam em `dependencies/cpm/CustomPlayerModels-1.20-0.6.26a.jar` e `.downloads/m002_r2/polymesh-1.0.0-forge.jar`. Use o artefato **forge**, não `polymesh-1.0.0-dev.jar`. São dependências locais separadas, não arquivos incorporados ao nosso JAR.

Não copie `animation_pipeline`, scripts, saves, Blender, Node, GLBs soltos ou arquivos da máquina de desenvolvimento para jogar. Modelos e dados de poses são carregados pelo gerenciador de recursos a partir de `assets/towerofentropy` no JAR. O cache CPM dedicado em `config/towerofentropy/compat` é extraído dos recursos incorporados; não substitui `player_models` do usuário.

Dependências declaradas em `META-INF/mods.toml`: Minecraft `[1.20.1]`, Forge `[47.4.28,48)`, CPM `[0.6.26a,)` nos dois lados, Polymesh `[1.0.0,1.1)` no cliente. Sem dependência obrigatória, o Forge deve recusar o carregamento e informar o mod ausente; isso não equivale ao fallback de equipamento. Sem recurso GLB carregado, o evento de renderização fica disponível ao caminho CPM/vanilla. Uma falha da variante Slim não desativa o Classic.

Polymesh/CPM permanecem JARs separados. Não houve instalação ou download novo de dependências. Polymesh declara LGPL-3.0-or-later; seus componentes mantêm licenças/avisos próprios, conforme o relatório R2. CPM e os movimentos também mantêm seus termos. Consulte [procedência do A3](M002_A3_ASSET_COMPATIBILITY.md); este marco não autoriza redistribuir pacotes de origem como biblioteca de assets.

## Cobertura e limites

Continuam Idle, Walk, Jog, Sprint, direções, crouch, salto/queda/aterrissagem, água, dano, morte, espada leve/pesada, Roll, arco nos contextos aprovados, consumo, tocha e corte visual de madeira. Os 30 clipes empacotados não significam cobertura de todos os estados: as exclusões documentadas no A3 permanecem.

Primeira pessoa, armadura, offhand, capa visível, mão principal esquerda, camadas da skin desabilitadas e contextos ainda não suportados liberam a renderização a CPM/vanilla. Outros jogadores não recebem o renderizador local. Multiplayer remoto não está homologado.

`/toe_combat auto|on|off` e `/toe_renderlab` continuam diagnósticos opcionais. Reconexão restaura AUTO/R3. R3.1 continua experimental em `-PtoeR31`, sem alteração de seus assets. Perfis históricos de comparação continuam disponíveis.

## Desenvolvimento e evidências

Build normal: `powershell -ExecutionPolicy Bypass -File scripts/gradle.ps1 clean build --offline --console=plain`.

Build experimental: acrescente `-PtoeR31`. Não distribua o JAR experimental como padrão.

O gerador `package-a3-slim.cjs` roda depois de `prepareFullR3`. Não precisa de Blender durante o build. As fontes privadas locais continuam necessárias para reconstruir os derivados de desenvolvimento; o JAR final incorpora os recursos de runtime.

Os drivers `toeGlobalTest`, `toeGlobalCoverage` e `toeGlobalRegression` são exclusivamente de QA. Eles criam/abrem mundos comuns e não participam do JAR normal. Harnesses antigos `*SmokeTest` também ficam excluídos do build normal, preservados em seus perfis explícitos. Resultados e limites de homologação em [TEST_LOG_M002_A3_1.md](TEST_LOG_M002_A3_1.md).
