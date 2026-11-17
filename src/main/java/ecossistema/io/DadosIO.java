package ecossistema.io;

import ecossistema.model.RelationTable;
import ecossistema.model.Stats;
import ecossistema.model.World;
import ecossistema.model.entity.Animal;
import ecossistema.model.entity.Entity;
import ecossistema.model.entity.EntityType;
import ecossistema.model.entity.Sex;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Entrada e saída de dados para estudo (I/O nos dois sentidos).
 * Exporta uma pasta com CSVs que abrem no Excel/Python, e importa a tabela de relações.
 * Não depende de JavaFX: só lê e escreve arquivos.
 */
public final class DadosIO {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private DadosIO() {
    }

    // ---------------- Exportar ----------------

    /**
     * Cria dados/&lt;seed&gt;_&lt;data-hora&gt;/ com quatro arquivos e devolve a pasta criada.
     * populacao.csv: a cada meio segundo, quantos de cada e a média do gene de velocidade.
     * relacoes.csv: a tabela de relações do jeito que estava.
     * parametros.csv: a seed e os números de cada espécie.
     * animais.csv: a ficha de cada animal vivo naquele instante.
     */
    public static Path exportar(Path raiz, long seed, World world, RelationTable relacoes, List<Stats> historico)
            throws IOException {
        Path pasta = raiz.resolve(seed + "_" + LocalDateTime.now().format(HORA));
        Files.createDirectories(pasta);

        escreverPopulacao(pasta.resolve("populacao.csv"), historico);
        escreverRelacoes(pasta.resolve("relacoes.csv"), relacoes);
        escreverParametros(pasta.resolve("parametros.csv"), seed, world);
        escreverAnimais(pasta.resolve("animais.csv"), world);
        return pasta;
    }

    private static void escreverPopulacao(Path arquivo, List<Stats> historico) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(arquivo)) {
            linha(w, "tempo,coelhos,lobos,arbustos,gene_vel_coelhos,gene_vel_lobos");
            for (Stats s : historico) {
                linha(w, fmt("%.1f,%d,%d,%d,%.4f,%.4f", s.tempo(), s.coelhos(), s.lobos(), s.arbustos(),
                        s.geneVelCoelhos(), s.geneVelLobos()));
            }
        }
    }

    private static void escreverRelacoes(Path arquivo, RelationTable relacoes) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(arquivo)) {
            linha(w, "quem,por_quem,valor");
            for (EntityType de : EntityType.values()) {
                for (EntityType para : EntityType.values()) {
                    linha(w, fmt("%s,%s,%.2f", de.getNome(), para.getNome(), relacoes.get(de, para)));
                }
            }
        }
    }

    private static void escreverParametros(Path arquivo, long seed, World world) throws IOException {
        // pega um representante vivo de cada espécie (os números da espécie são iguais para todos)
        Map<EntityType, Animal> exemplo = new EnumMap<>(EntityType.class);
        for (Entity e : world.getEntidades()) {
            if (e.isAlive() && e instanceof Animal a) exemplo.putIfAbsent(a.type(), a);
        }

        try (BufferedWriter w = Files.newBufferedWriter(arquivo)) {
            linha(w, "especie,parametro,valor");
            linha(w, "mundo,seed," + seed);
            linha(w, "mundo,largura," + world.getLargura());
            linha(w, "mundo,altura," + world.getAltura());
            linha(w, fmt("mundo,tempo_simulado,%.1f", world.getTempo()));
            for (Animal a : exemplo.values()) {
                for (Map.Entry<String, Double> p : a.parametrosDaEspecie().entrySet()) {
                    linha(w, fmt("%s,%s,%.4f", a.type().getNome(), p.getKey(), p.getValue()));
                }
            }
        }
    }

    private static void escreverAnimais(Path arquivo, World world) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(arquivo)) {
            linha(w, "id,especie,sexo,idade,energia,gene_velocidade,gene_visao,velocidade_atual,visao,"
                    + "geracao,id_mae,id_pai,filhos,x,y");
            for (Entity e : world.getEntidades()) {
                if (!e.isAlive() || !(e instanceof Animal a)) continue;
                linha(w, fmt("%d,%s,%s,%.1f,%.1f,%.4f,%.4f,%.3f,%.2f,%d,%d,%d,%d,%.2f,%.2f",
                        a.getId(), a.type().getNome(), a.getSexo() == Sex.MALE ? "macho" : "femea",
                        a.getIdade(), a.getEnergia(), a.getGenVelocidade(), a.getGenVisao(),
                        a.getVelocidadeAtual(), a.getVisao(), a.getGeracao(), a.getIdMae(), a.getIdPai(),
                        a.getFilhos(), a.getPos().x(), a.getPos().y()));
            }
        }
    }

    // ---------------- Importar ----------------

    /**
     * Lê um relacoes.csv (o mesmo formato que o exportar gera) e aplica na tabela.
     * Se alguma linha estiver errada, nada é aplicado e a mensagem diz qual linha.
     */
    public static int importarRelacoes(Path arquivo, RelationTable relacoes) throws IOException {
        List<String> linhas;
        try {
            linhas = Files.readAllLines(arquivo);
        } catch (NoSuchFileException ex) {
            throw new IOException("o arquivo não existe mais", ex);
        } catch (MalformedInputException ex) {
            throw new IOException("o arquivo não é um texto UTF-8 (salve o CSV como UTF-8)", ex);
        }
        RelationTable nova = new RelationTable();
        int lidas = 0;

        for (int i = 1; i < linhas.size(); i++) { // linha 0 é o cabeçalho
            String texto = linhas.get(i).trim();
            if (texto.isEmpty()) continue;
            String[] partes = texto.split(",");
            if (partes.length != 3) {
                throw new IOException("linha " + (i + 1) + ": esperava 3 colunas (quem,por_quem,valor)");
            }
            EntityType de = tipoPorNome(partes[0], i);
            EntityType para = tipoPorNome(partes[1], i);
            double valor;
            try {
                valor = Double.parseDouble(partes[2].trim());
            } catch (NumberFormatException ex) {
                throw new IOException("linha " + (i + 1) + ": \"" + partes[2] + "\" não é um número");
            }
            nova.set(de, para, Math.max(-5, Math.min(5, valor))); // mesmo limite da tela
            lidas++;
        }
        if (lidas == 0) throw new IOException("o arquivo não tem nenhuma relação");

        // só copia para a tabela de verdade depois de ler tudo sem erro
        for (EntityType de : EntityType.values()) {
            for (EntityType para : EntityType.values()) relacoes.set(de, para, nova.get(de, para));
        }
        return lidas;
    }

    private static EntityType tipoPorNome(String nome, int linha) throws IOException {
        for (EntityType t : EntityType.values()) {
            if (t.getNome().equalsIgnoreCase(nome.trim())) return t;
        }
        throw new IOException("linha " + (linha + 1) + ": não conheço \"" + nome.trim()
                + "\" (use coelho, lobo ou arbusto)");
    }

    // ---------------- utilitários ----------------

    private static void linha(BufferedWriter w, String texto) throws IOException {
        w.write(texto);
        w.newLine();
    }

    /** Sempre com ponto decimal, senão a vírgula brasileira quebra o CSV. */
    private static String fmt(String f, Object... args) {
        return String.format(Locale.US, f, args);
    }
}
