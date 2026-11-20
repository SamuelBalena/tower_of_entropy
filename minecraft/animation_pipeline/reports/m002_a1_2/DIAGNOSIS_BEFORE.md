# M002.A1.2 — Diagnóstico anterior às alterações

- Pesado: o recorte 60–130 começa inclinado/agachado; a troca direta de clipe produz grande diferença de pose. `setAnimationTime` limpa o blend nativo da biblioteca. Corpo e relógio de dano devem ser separados.
- C → Jog: o renderer reinicia explicitamente loops em zero e não interpola a hierarquia entre clips. O antebraço apresenta diferença global de referência de 114,3°. É necessário preservar fase locomotora e interpolar TRS locais, não matrizes globais.
- Cancelamento: `LocomotionController.cancelAction` notifica clipe vazio antes do pacote Roll pendente. O cliente escolhe Idle enquanto aguarda o callback CPM. O protocolo visual precisa comunicar a troca atomicamente; o início físico e relógio de i-frames devem permanecer no callback atual.
- Pés: o retargeting descarta translações da fonte e mantém o quadril alvo numa altura praticamente fixa. Na UAL2 B, o quadril original varia entre Y=0,478 e 0,571 m, abaixo do repouso Y=0,917 m, enquanto os dois ball joints ficam apoiados em Y≈0,015 m. Portanto a separação visual de B não corresponde a um salto original. Em C os toes podem chegar a 0,434/0,146 m: há fases sem contato e não se deve prender os pés nelas.
- Espada: a matriz de antebraço é consultada diretamente no controller e o anchor é amostrado sem interpolação. Um blend só no corpo separaria a espada da mão. Ambos precisam da mesma pose final e peso de transição.

Fontes: relatórios A1.1, 24 sequências, `AnimationController` e renderer atuais, e inspeção read-only `SOURCE_CONTACT.json`. Nenhum clipe/modelo foi alterado nesta inspeção.
