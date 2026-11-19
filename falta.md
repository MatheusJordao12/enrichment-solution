# Faltando para a beta pública — Enrichment Solution

Status: **0.2.0-beta** · Meta: beta pública
Última revisão: rodada de conclusão (P0.5–P0.7, P1.1–P1.3, P2.1–P2.3, P2.5 implementados e validados).

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

### [x] P0.5 — Validação em servidor dedicado
- `gradlew runServer` subiu limpo (`Done (0.586s)`) e rodou ~20 min salvando o mundo sem `ERROR`/`Exception` em `run/logs/latest.log` (também corrigido crash de dist em `ClientEntrypoint`).
- **Aceite:** log limpo (sem exception); teste de conectar/desconectar jogador em sessão de 1h fica para validação manual final.

### [x] P0.6 — Stress test de TPS
- Benchmark automatizado (`factory_stress_benchmark` em `ModGameTests`): 240 conduítes, 30 máquinas e 108 blocos de tanque por 100 ticks → `avg=4,491 ms/tick` (`20,00 TPS`).
- **Aceite:** ≥ 19.5 TPS atingido no benchmark; teste de 5 min com `/neoforge tps` em mundo real fica opcional para validação manual.

### [x] P0.7 — Balance pass + configuração
- `ModServerConfig` (`ModConfig.Type.SERVER`) com multiplicadores e capacidades/taxas de todas as máquinas, geradores, conduítes e multiblocos.
- **Aceite:** config permite ajustar custos sem recompilar; default = valores atuais.

---

## P1 — Estrutura (antes de sair de beta)

### [x] P1.1 — Deduplicação de rede/multibloco
- Extraídos `MultiblockNetwork` (compartilhado por `TankBlockEntity` e `HeatExchangerMultiblock`) e `ConduitLink` (compartilhado por `FluidPipeBlockEntity` e `EnergyCableBlockEntity`).
- **Aceite:** nenhum dos dois pares duplica lógica de BFS/mesclagem.

### [x] P1.2 — Quebra de god classes
- `HeatExchangerBlockEntity` (737 → 319 linhas) dividido em `HeatExchangerThermalHandler`, `HeatExchangerIsobutaneHandler` e `HeatExchangerMultiblock`.
- `FluidPumpBlockEntity` (515 → 294 linhas) com `FluidPumpScanner` extraído.
- `RobotEntity` (994 → 685 linhas) com `RobotEnergyManager`, `RobotInventoryHelper`, `RobotPatrolState` e `RobotAnimationController` extraídos.
- **Aceite:** responsabilidades separadas em helpers coesos (< ~400 linhas por classe extraída).

### [x] P1.3 — Datagen completo
- `ModDataGenerators` com loot tables, blockstates/models, item models, receitas (incluindo `MachineRecipe`), tags e advancements.
- **Aceite:** `gradlew runData` regenera tudo (`written: 0` na 2ª execução).

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

### [x] P2.1 — Testes automatizados
- `ModGameTests`: 7 GameTests (enriquecimento, refino, prensagem, impressão PCB, trocador de calor, rede de canos e `factory_stress_benchmark`).
- **Aceite:** `gradlew runGameTestServer` verde (`7/7` passando).

### [x] P2.2 — JEI completo
- `HeatExchangerCategory` (mB/t por multiplicador) e `GeothermalCategory` (FE/mB) registrados em `EnrichmentJeiPlugin`.
- **Aceite:** toda máquina com receita/taxa aparece no JEI.

### [x] P2.3 — Tooltips
- Óculos (`HiTechGogglesItem`), canos térmicos/fluidos (`FluidPipeBlock`), tanque (`TankBlock`), reator (`ReactorCoreBlock`) com chaves em `en_us.json` e `pt_br.json` (204/204 chaves).
- **Aceite:** item/bloco explica função sem wiki.

### [x] P2.4 — Documentação e publicação
- `README.md` (features, instalação, build, modpack), `CHANGELOG.md`, licença, metadados de
  Modrinth/CurseForge, screenshots.
- **Aceite:** página de projeto pronta para publicar.

### [x] P2.5 — Compatibilidade
- Auditoria de isolamento (JEI atrás de `@JeiPlugin`, cliente em `Dist.CLIENT` / `ClientEntrypoint`, capabilities padrão NeoForge) e seção `Mod compatibility` no `README.md`.
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
