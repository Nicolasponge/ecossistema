# Ecossistema

Simulador de ecossistema em Java + JavaFX. Um mundo de ilhas gerado com Perlin noise,
com coelhos, lobos e arbustos que se afetam entre si. Os dois animais sobrevivem sozinhos,
sem ninguém mexer, com a população subindo e descendo em ciclos, como na natureza.

## Como rodar

Precisa de Java 21 ou mais novo.

**IntelliJ:** File > Open > escolha a pasta do projeto (ele reconhece o `pom.xml`).
Depois, na aba Maven da direita: Plugins > javafx > `javafx:run`.

**Terminal:**

```
mvn javafx:run
```

## Controles

**Mapa**

- Roda do mouse: zoom no ponto do cursor. Tecla `0` volta ao mapa inteiro.
- Arrastar com o botão direito (ou o do meio): mover o mapa.
- Shift + roda, ou as teclas `[` e `]`: tamanho do pincel.
- Clique sem ferramenta marcada: seleciona o animal mais perto e abre a ficha dele
  (espécie, sexo, idade, energia, velocidade, visão, geração, pais e filhos). `Esc` fecha.

**Barra de baixo, estilo WorldBox**

- Tempo: pausar e velocidade (0.5x, 1x, 2x, 5x).
- Terreno: planície, floresta, praia e apagar (vira água; quem estava em cima morre e a costa é recalculada).
- Vida: coelho, lobo e arbusto (um por clique).
- Limpar: remove seres dentro do pincel sem mexer no terreno. A caixinha escolhe o quê: tudo, coelhos, lobos ou arbustos.
- Mundo: gerar um mundo novo.

**Painel da direita**

- Gráfico com duas abas. "População" mostra o ciclo predador-presa. "Evolução" mostra a média do
  gene de velocidade de cada espécie (1,0 = como começou).
- Tabela de relações: o quanto cada bicho é puxado (positivo) ou empurrado (negativo) por outro. Muda ao vivo.
- Dados: "Exportar dados" cria `dados/<seed>_<data-hora>/` com quatro CSVs. "Importar relações" lê um
  `relacoes.csv` e aplica na tabela.

| Arquivo exportado | O que tem |
|---|---|
| `populacao.csv` | a cada 0,5 s: coelhos, lobos, arbustos e média do gene de velocidade |
| `relacoes.csv` | a tabela de relações (`quem,por_quem,valor`); é o mesmo formato que o importar lê |
| `parametros.csv` | seed, tamanho do mundo, tempo simulado e os números de cada espécie |
| `animais.csv` | a ficha de cada animal vivo (genes, geração, pais, filhos, posição) |

Sprites ficam em `src/main/resources/sprites` (animais 16x8, arbusto 16x16, olhando pra direita).

## Onde está cada conceito (pra apresentação)

| Conceito | Onde | Em uma frase |
|---|---|---|
| MVC | pacotes `model`, `view`, `controller` | o `model` não importa nada de JavaFX, por isso dá pra simular no terminal (pasta `ferramentas/`) |
| Herança e classe abstrata | `Entity` > `Animal` > `Rabbit` / `Wolf`, e `Entity` > `Bush` | o que é comum fica em cima, o que muda fica na espécie |
| Polimorfismo | `behave()`, `ehComida()`, `consegueVer()` em cada animal; `apply()` em cada ferramenta | o mesmo chamado faz coisas diferentes conforme o objeto |
| Interfaces | `Tool`, `WorldObserver` | contratos: quem implementa promete ter aqueles métodos |
| Encapsulamento | genes e energia privados em `Animal`, só com getters | a tela consegue ler a ficha, mas não consegue mexer nela |
| Template Method (GoF) | `Animal.update()` (é `final`) | a receita da vida é a mesma; cada espécie preenche só as partes dela |
| Strategy (GoF) | `Tool`: `PaintBiomeTool`, `EraseTool`, `SpawnTool`, `ClearEntitiesTool` | o controller só chama `apply()`, sem saber qual ferramenta é |
| Observer (GoF) | `WorldObserver`, `World.addObserver()`, `StatsPanel` | o mundo avisa "mudou", e o gráfico se atualiza sozinho |
| Creator (GRASP) | `createOffspring()` e `SpawnTool(Rabbit::new)` | cada espécie sabe criar o próprio filhote; não existe fábrica central |
| Information Expert (GRASP) | `World.applyCoastRules()`, `World.nearestAnimal()`, `Animal.fome()` | quem tem a informação é quem faz a conta |
| Controller (GRASP) | `GameController` (`start()` e `loop(deltaTime)`) | recebe o mouse e o teclado e coordena model e view |
| Exceções | `InvalidPlacementException` (bicho na água); `IOException` no exportar/importar com mensagem amigável | o erro vira uma frase no rodapé, o jogo não fecha |
| I/O (arquivos) | `DadosIO.exportar()` e `DadosIO.importarRelacoes()`; sprites em `Sprites` | escreve e lê arquivos nos dois sentidos |
| Records e enums | `Stats`, `Vec2` (records); `Biome`, `EntityType`, `Sex` (enums) | dados imutáveis e listas fechadas de opções |
| Coleções e lambdas | grade espacial em `World`, `List.of(...)` das ferramentas de limpar, `Rabbit::new` | |

## Referências da vida real usadas

**Mundo**

- Litoral fractal (Mandelbrot): por que somamos camadas de ruído.
- Perlin noise (Ken Perlin, criado para o filme Tron).
- Zonação costeira e ecótono: mar, praia, planície e só depois floresta, com transição.

**Movimento**

- Boids (Craig Reynolds, 1986): comportamento de bando a partir de regras simples entre vizinhos.
  A atração do bando usa a média dos vizinhos (não a soma), senão bandos grandes viram bolotas.

**Equilíbrio sem intervenção** (cada item foi medido com `ferramentas/Timeline.java`)

- Lotka-Volterra e Isle Royale: ciclos de predador e presa.
- Experimento de Gause: sem refúgio o predador extingue a presa. Aqui a floresta é o refúgio:
  o lobo anda mais devagar nela e só enxerga coelho escondido a 2 tiles.
- Resposta funcional de Holling tipo III: com poucos coelhos por perto, o lobo erra mais o bote.
  Quando a presa fica rara, sobra uma reserva.
- Crescimento logístico (capacidade de suporte): coelho com muitos vizinhos para de ter filhote.
- Modelo de Leslie-Gower: o lobo só tem filhote se houver coelhos suficientes no território dele.
- Efeito Allee: população pequena demais pode sumir por azar. Por isso o sexo alterna entre os
  nascimentos e o lobo "uiva" (acha parceiro de mais longe).
- Envelhecimento: depois da metade da vida o animal vai ficando mais lento (até 60% no fim).

**Herança e evolução**

- Cada filhote herda a média da velocidade e da visão dos pais, com uma pequena mutação.
  Ser rápido custa mais energia, então não é "quanto mais, melhor": é seleção natural com custo.

## Visual

Inspirado no WorldBox. A luz vem de cima e a terra é um degrau acima do mar: a borda de baixo da terra
que encosta na água fica mais escura (barranco), a terra joga sombra na água logo abaixo, e a floresta
tem a borda de baixo escura e a de cima clara, como se as copas fossem mais altas.

## Trabalhos futuros

- Arbustos se espalhando sozinhos (sementes perto de arbustos adultos).
- Imigração rara quando uma espécie zera.
- Mais genes herdados (tamanho, fertilidade) e mais espécies.
- Salvar e carregar o mundo inteiro, não só as relações.
