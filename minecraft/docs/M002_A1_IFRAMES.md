# M002.A1 — Cancelamentos e i-frames do Roll

`RollController.request` valida direção, estado e modo de combate antes de cancelar um ataque. Se o Roll for válido, `LocomotionController.cancelAction` limpa o ataque, fila e continuidade; depois inicia o Roll físico anterior. Um pedido inválido por NaN, menu, estado não suportado ou Roll já ativo não cancela o ataque. Não se pode atacar durante Roll pendente/ativo.

Nenhuma constante ou integração de movimento foi alterada: 2,5 blocos, 14 ticks / 0,7 s de deslocamento, direção WASD normalizada e recuperação anterior de 32 ticks. Colisão, gravidade, limites e sincronização existentes continuam sendo os do M002.1.

## Janela autoritativa

O relógio começa em `RollController.animationStarted`, quando o servidor confirma o início visual/físico, não ao receber um pedido ainda pendente da ponte CPM. A função `iframe` verifica `3 <= gameTime - startTick < 10`.

| Tempo desde início confirmado | Proteção |
|---|---|
| 0–149 ms / ticks 0–2 | Inativa |
| 150–499 ms / ticks 3–9 | Ativa |
| 500 ms em diante / tick 10+ | Inativa |

Um tick de servidor equivale nominalmente a 50 ms; sob TPS reduzido a duração real acompanha o servidor. Não existe temporizador de dano controlado pelo cliente.

## Danos elegíveis

`RollDamageFilter` cancela `LivingAttackEvent` no servidor durante a janela:

- Ataques diretos atribuídos a uma entidade viva, inclusive melee de monstros/jogadores.
- Projéteis cuja fonte é marcada `IS_PROJECTILE` e cujo proprietário atribuído implementa `Enemy` (hostil).

Não cancela dano sem atacante direto elegível, projétil sem proprietário hostil, nem fontes `BYPASSES_INVULNERABILITY`. Lava, queda, vazio e outros danos ambientais seguem vanilla. Não se escreve `setInvulnerable`, não se altera `invulnerableTime` e não se concede imunidade global.

Cancelar o evento antes do dano também impede que um golpe protegido inicie `hurtTime` e encerre indevidamente o Roll. Fora da janela, dano e interrupções anteriores continuam aplicáveis. Limpeza em cancelamento, morte, respawn, logout e parada do servidor elimina a sessão e a proteção. Estado de combate do cliente é explicitamente limpo após respawn.

Os testes de fronteira usam os ticks 2, 3, 9 e 10 com eventos reais Forge para melee, flecha hostil, lava, queda e vazio. Isso verifica a política e cancelamento do evento; não equivale a uma batalha manual com um esqueleto. Resultados detalhados estão no registro de testes A1.
