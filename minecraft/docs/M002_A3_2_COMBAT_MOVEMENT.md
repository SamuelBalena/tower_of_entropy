# M002.A3.2 — Compromisso e avanço dos ataques

14/11/2026. Minecraft 1.20.1 / Forge 47.4.28 / Java 17. Polymesh R3 permanece padrão, Classic e Slim preservados. Não há novos golpes, armas, dano, stamina ou progressão.

## Autorização e bloqueio

O servidor mantém a autoridade sobre aceitação, fila, ação atual, duração, impactos e cancelamentos. Ao aceitar cada golpe, publica direção horizontal inicial, distância configurada e janela do avanço. O protocolo passa a **7**; versões de rede incompatíveis são recusadas. Nenhum cliente escolhe distância ou velocidade por pacote.

`ClientAttackMotion` neutraliza os controles horizontais remapeados, incluindo diagonais, e desativa Sprint durante o golpe. Um bloqueio provisório curto impede um passo de corrida enquanto chega a autorização; não antecipa avanço nem dano. Rejeição sem ataque ativo encerra a previsão; há timeout defensivo. Encadeamentos autorizados mantêm o compromisso entre A/B/C. Ao terminar a sequência ou cancelar, o bloqueio acaba; a janela de continuação do combo não mantém o jogador preso.

| Golpe | Compromisso autorizado | Janela de avanço, ticks desde início | Distância padrão | Impacto preservado |
|---|---:|---:|---:|---:|
| A | 9 ticks / 450 ms | 1–4 | 0,18 bloco | tick 4 |
| B | 11 ticks / 550 ms | 2–5 | 0,22 bloco | tick 5 |
| C | 40 ticks / 2 s | 10–15 | 0,28 bloco | tick 14 |
| Pesado | 47 ticks / 2,35 s | 8–13 | 0,32 bloco | tick 12 |

As janelas foram escolhidas após inspecionar os quadros existentes de tronco e golpe, próximas ao trecho ofensivo/impacto. Os arquivos originais não são reamostrados ou regravados. O tempo físico/visual do ataque e os impactos existentes permanecem iguais.

Configuração de servidor em `[attackMovement]`: `lightABlocks`, `lightBBlocks`, `lightCBlocks`, `heavyBlocks`, entre 0 e 0,75. Cada distância é capturada quando o golpe é aceito. Zero desativa o avanço daquele golpe. Alcance, arco, dano e janela de combo anteriores permanecem independentes.

## Movimento e segurança

O cliente executa pequenos passos `Entity.move(SELF, ...)` exclusivamente sob uma autorização. O servidor observa a posição e valida o envelope da ação, com tolerância limitada e crédito para impulsos originados no servidor. Não há teleporte como método de avanço, perseguição ou auto-aim. Reconciliação por teleporte fica restrita à rejeição de movimento fora dos limites; os testes distinguem esse caso adversarial dos golpes normais.

O passo passa por colisão de blocos/entidades e amostragem de suporte do terreno nos cantos do pé. A busca reduz o passo perto de obstáculos ou bordas; não força a distância completa através deles. Degraus e terreno irregular podem reduzir conservadoramente o avanço. Gravidade e velocidade vertical continuam vanilla.

Os avanços não deixam velocidade horizontal própria acumulada. Forças externas são preservadas separadamente: impulsos conhecidos do servidor dão crédito ao validador, dano pode cancelar a ação sem suprimir seu knockback e movimento de pistão provoca liberação segura. Casos de mods de física, forças arbitrárias e latência não receberam certificação abrangente.

Ao solicitar Roll durante ataque, o cliente suspende o avanço enquanto aguarda a resposta e lê as associações WASD remapeadas, não os impulsos já neutralizados pelo ataque. O servidor valida antes de cancelar. Aceito o Roll, remove golpe/fila/impactos futuros e autorização de avanço antes de iniciar a esquiva. Rejeição preserva o ataque válido; timeout não cria travamento permanente.

## Renderização

O clipe de combate tem prioridade sobre locomoção no R3. O avanço físico não seleciona Walk/Sprint para as pernas durante o golpe. Empunhadura, modelos, UVs, pesos e clipes permanecem aprovados. Primeira pessoa/equipamentos continuam nos fallbacks anteriores. Testes, distâncias medidas e limites estão em [TEST_LOG_M002_A3_2.md](TEST_LOG_M002_A3_2.md).
