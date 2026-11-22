# S002.13 — Otimização e estabilidade temporal

SMAA 1x permanece nos presets Equilibrado e Alto com os níveis de qualidade anteriores, e FXAA permanece no Leve. Oclusão e SSR usam o passe opaco existente. Bloom usa a máscara de emissão e evita o custo de amostrar cor final em posições deslocadas. Caminhos desativados usam pré-processamento. O Oculus pula os dois passes SMAA para FXAA ou AA desligado, e os alvos auxiliares de reflexão e emissão são selecionados conforme os efeitos ativos. Céu, profundidade, distância e peso de material limitam o trabalho nos pixels elegíveis.

Oclusão, raios e reflexos usam amostras determinísticas por quadro. O TAA requer vetores de movimento da vegetação, entidades R3 e transparências, além de reprojeção de câmera e rejeição de histórico. A avaliação de dados do pipeline indicou manter o antialiasing espacial como alternativa. Sem histórico acumulado, shimmer subpixel continua possível.

A galeria de QA usa `aa_motion`, `combat`, `camera_tunnel` e as medições de cada preset. [Resultados e procedimento](S002_GRAPHICS_EXPANSION_SUMMARY.md)

[Medições da versão entregue](S002_GRAPHICS_EXPANSION_BENCHMARK.csv)
