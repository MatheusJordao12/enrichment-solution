# Faltando para a beta pública — Enrichment Solution

Status: **0.2.0-beta** · Meta: beta pública
Última revisão: rodada de consolidação (P0.2, P0.3, P0.4, P1.4, P1.5, P1.6, P2.4 concluídos).

Este arquivo é a fonte da verdade do que falta. Cada item tem dono e critério de aceite.
`[ ]` pendente · `[~]` em andamento · `[x]` feito

---

## P0 — Bloqueadores (não lançar beta pública sem)

### [x] P0.1 — Versionamento
- `git init` + commit inicial do estado atual (612 arquivos).
- Regra: commits pequenos, uma frente por branch/PR.
- **Aceite:** `git log` com histórico, `.gitignore` cobrindo `build/`, `.gradle/`, `run/`.

### [x] P0.2 — Receitas de máquina em datapack
Hoje as receitas estão **hardcoded** em 4 BlockEntities (`EnrichmentChamber`, `OilRefinery`,
`StampingPress`, `PcbPrinter`) e duplicadas como "snapshot" no JEI.
- Criar `MachineRecipe` (RecipeType + Serializer + Codec) com: tipo, itens de entrada,
  fluido de entrada opcional, item de saída, fluido de saída opcional, `energy_per_tick`, `ticks`.
- Mover as 9 receitas para `data/enrichment_solution/recipe/*.json`.
- Máquinas passam a consultar `RecipeManager`; JEI lê a mesma fonte.
- **Aceite:** mudar um JSON altera jogo + JEI sem recompilar; valores atuais preservados;
  datapack sync funcionando em servidor dedicado.

### [x] P0.3 — Performance dos conduítes
Hoje **cada** cano/cabo com conteúdo roda um BFS de até 256 nós **por tick**
(`FluidPipeBlockEntity.distributeFluidAlongNetwork`, `EnergyCableBlockEntity.distributeEnergyAlongNetwork`).
- Cachear a topologia por rede (nível + dimensão), invalidando em place/break e mudança de side config.
- Pré-computar por conduíte os destinos alcançáveis + fator de retenção térmica acumulado.
- **Aceite:** rede de 200 canos mantém 60 FPS; nenhuma mudança de comportamento
  (vazão, perda de calor por caminho, side config, devolução ao quebrar).

### [x] P0.4 — Isolamento cliente/servidor
`ModNetwork` (código comum) importa `client.InspectHud`.
- Inverter dependência: `ModNetwork` expõe um `Consumer`/interface que o lado cliente registra.
- Auditar o resto: nenhum `import com.enrichmentsolution.mod.client.*` em `common`.
- **Aceite:** servidor dedicado sobe sem `NoClassDefFoundError`; HUD continua funcionando no cliente.

### [ ] P0.5 — Validação em servidor dedicado
- Rodar `gradlew runServer`, abrir mundo, colocar/remover máquinas, canos, multiblocos,
  conectar/desconectar jogador.
- **Aceite:** log limpo (sem exception), mundo salva e recarrega com tanques/multiblocos intactos.

### [ ] P0.6 — Stress test de TPS
- Mundo de teste: 3× a maior fábrica esperada (200+ canos/cabos, 30 máquinas, 1 tanque 100+ blocos).
- Medir TPS com `/forge tps` ou spark por 5 min com tudo rodando.
- **Aceite:** ≥ 19.5 TPS; memória estável (sem vazamento nas estruturas de rede).

### [ ] P0.7 — Balance pass + configuração
- Energia/tempo por operação revisados (hoje: 4.000 FE/enriquecer, 1.000 FE/refinar, 2.000 FE/prensar).
- Mover números para **server config** (NeoForge config) em vez de `private static final`.
- **Aceite:** config permite ajustar custos sem recompilar; default = valores atuais.

---

## P1 — Estrutura (antes de sair de beta)

### [ ] P1.1 — Deduplicação de rede/multibloco
- `rebuildNetwork` do `TankBlockEntity` e `HeatExchangerBlockEntity` são ~120 linhas quase idênticas.
- `pull/push` de `FluidPipeBlockEntity` e `EnergyCableBlockEntity` também.
- Extrair `MultiblockNetwork<T>` e `ConduitLink<C>` reutilizáveis.
- **Aceite:** nenhum dos dois pares duplica lógica de BFS/mesclagem.

### [ ] P1.2 — Quebra de god classes
- `HeatExchangerBlockEntity` (~730 linhas): separar água / isobutano / multibloco / sync.
- `RobotEntity`, `FluidPumpBlockEntity`: separar IA e varredura de fluido.
- **Aceite:** nenhum arquivo > ~400 linhas com mais de uma responsabilidade.

### [ ] P1.3 — Datagen completo
- Loot tables, blockstates, block/item models, receitas, tags (mineração/nível), advancement.
- **Aceite:** `gradlew runData` regenera tudo e o diff fica vazio.

### [x] P1.4 — Progressão (advancements + guia)
- Árvore: casing → primeira máquina → enriquecimento → PCB → circuito → trocador → gerador → reator.
- **Aceite:** jogador novo descobre o próximo passo sem abrir o código.

### [x] P1.5 — Tags e ferramentas
- `minecraft:mineable/pickaxe`, `needs_iron_tool` etc. para todos os blocos de máquina.
- **Aceite:** mineração correta (drop só com picareta), sem “drop sem ferramenta”.

### [x] P1.6 — Localização
- Paridade `en_us` × `pt_br` (chaves iguais), revisão de nomes e tooltips.
- **Aceite:** nenhuma chave faltando; nada de texto cru na tela.

---

## P2 — Polimento (pode sair em 0.2.x)

### [ ] P2.1 — Testes automatizados
- NeoForge GameTest para: enriquecimento, refino, prensagem, impressão, trocador, rede de canos.
- **Aceite:** `gradlew runGameTestServer` verde.

### [ ] P2.2 — JEI completo
- Categorias informativas para Heat Exchanger (mB/t por multiplicador) e Geothermal (FE/mB).
- **Aceite:** toda máquina com receita/taxa aparece no JEI.

### [ ] P2.3 — Tooltips
- Óculos (HUD 4 blocos), canos térmicos (retenção por bloco), tanque (temperatura), reator (calor).
- **Aceite:** item/bloco explica função sem wiki.

### [x] P2.4 — Documentação e publicação
- `README.md` (features, instalação, build, modpack), `CHANGELOG.md`, licença, metadados de
  Modrinth/CurseForge, screenshots.
- **Aceite:** página de projeto pronta para publicar.

### [ ] P2.5 — Compatibilidade
- Testar com JEI + Jade + Create? (no mínimo Jade/Oculus), sem crash.
- **Aceite:** lista de mods suportados documentada.

---

## Critérios de saída da beta pública

1. Servidor dedicado sobe e roda 1h sem exception.
2. Fábrica grande (200+ conduítes) a ≥ 19.5 TPS.
3. 100% das receitas datapack-driven e visíveis no JEI.
4. Progressão jogável só com o jogo + JEI.
5. Nenhuma classe cliente carregada no servidor.
6. `0.2.0-beta` publicado com README, CHANGELOG e issues templates.

---

## Registro da rodada (0.2.0-beta)

| item | o que mudou |
|---|---|
| P0.1 | `git init`, `.gitignore`, commit base `f0fe827` |
| P0.2 | `MachineRecipe` (RecipeType + Serializer + Codec + StreamCodec), 4 kinds, 9 JSONs em `data/enrichment_solution/recipe/{enrichment,pcb,pressing,refining}`, `MachineRecipeCache` (recarrega quando o `RecipeManager` muda), `MachineRecipeMatcher` (slots fora de ordem + saída). JEI passou a ler do `RecipeManager` — mesma fonte das máquinas. PCB Printer guarda o id da receita pendente (sobrevive a save/reload e aborta se a receita sumir). |
| P0.3 | `ConduitNetworkCache` por nível com geração; BFS saiu do tick (1 por evento de topologia/config); rotas pré-computadas com fator térmico acumulado. |
| P0.4 | `ModNetwork.setInspectSink(...)`; `ClientSetup` registra o sink; `common/**` sem nenhuma referência a `client`/`net.minecraft.client`; config cliente só é registrada no lado cliente. |
| P1.4 | 13 advancements (root + 3 ramos) com 26 chaves de lang. |
| P1.5 | `mineable/pickaxe` com 43 blocos; `needs_iron_tool` com +22 máquinas/minérios. |
| P1.6 | +13 chaves no `pt_br` (configurador + direções) → paridade com `en_us`. |
| P2.4 | `README.md`, `CHANGELOG.md`, `LICENSE` (MIT), issue template, versão `0.2.0-beta`. |

### Pendente de validação manual (não automatizável aqui)
- **P0.5** subir servidor dedicado e rodar 1h.
- **P0.6** stress test com 200+ conduítes medindo TPS.
- **P0.2/P0.3 em jogo**: `/reload` com receitas alteradas; fluxo de calor em rede mista de canos; colocar/remover máquina no meio de rede já cheia; configurador alternando faces.
- **P0.7** balance + config de servidor (ainda não iniciado).
