package br.com.alura.screenmatch.principal;

import br.com.alura.screenmatch.model.*;
import br.com.alura.screenmatch.repository.SerieRepository;
import br.com.alura.screenmatch.service.ConsumoApi;
import br.com.alura.screenmatch.service.ConverteDados;

import java.util.*;
import java.util.stream.Collectors;

public class Principal {

    private Scanner leitura = new Scanner(System.in);
    private ConsumoApi consumo = new ConsumoApi();
    private ConverteDados conversor = new ConverteDados();
    private final String ENDERECO = "https://www.omdbapi.com/?t=";
    private final String API_KEY = "&apikey=*******";
    private List<DadosSerie> dadosSeries = new ArrayList<>();
    private SerieRepository repositorio;
    private List<Serie> series = new ArrayList<>();
    private Optional<Serie> serieBusca;

    public Principal(SerieRepository repositorio) {
        this.repositorio = repositorio;
    }

   public void exibeMenu() {

        var opcao = -1;

        while (opcao != 0) {
            var menu = """
                1 - Buscar séries
                2 - Buscar episódios por série
                3 - Listar séries buscadas
                4 - Buscar séries pelo titulo
                5 - Buscar séries pelo Ator
                6 - Top5 melhores Series
                7 - Busca Séries por Categoria
                8 - Busca Séries por Quantidade de Temporadas e Avaliação
                9 - Buscar Epsódio por trecho
                10 - Top5 melhores episodios por Série
                11 - Buscar Episódios a partir de uma data
                0 - Sair
                """;

            System.out.println(menu);
            opcao = leitura.nextInt();
            leitura.nextLine();

            switch (opcao) {
                case 1:
                    buscarSerieWeb();
                    break;
                case 2:
                    buscarEpisodioPorSerie();
                    break;
                case 3:
                    listarSeriesBuscadas();
                    break;
                case 4:
                    buscarSeriePorTitulo();
                    break;
                case 5:
                    buscarSeriePorAtor();
                    break;
                case 6:
                    buscarTop5Series();
                    break;
                case 7:
                    buscarSeriesPorCategoria();
                    break;
                case 8:
                    buscarSeriesPorQuantidadeTemporada();
                    break;
                case 9:
                    buscarEpisodioPorTecho();
                    break;
                case 10:
                    topEpisodiosPorSerie();
                    break;
                case 11:
                    buscarEpisodioPorData();
                    break;

                case 0:
                    System.out.println("Saindo...");
                    break;
                default:
                    System.out.println("Opção Inválida.");
            }
        }
    }

    private void buscarSerieWeb() {
        DadosSerie dados = getDadosSerie();
        Serie serie = new Serie(dados);
        repositorio.save(serie);
        System.out.println(dados);
    }

    private DadosSerie getDadosSerie() {
        System.out.println("Digite o nome da série para buscar:");
        var nomeSerie = leitura.nextLine();
        var json = consumo.obterDados(ENDERECO + nomeSerie.replace(" ", "+") + API_KEY);
        DadosSerie dados = conversor.obterDados(json, DadosSerie.class);
        return dados;
    }

    private void buscarEpisodioPorSerie() {
        listarSeriesBuscadas();
        System.out.println("Escolha uma serie pelo nome: ");
        var nomeSerie = leitura.nextLine();

        Optional<Serie> serie = repositorio.findByTituloContainingIgnoreCase(nomeSerie);

        if (serie.isPresent()) {

            var serieEncontrada = serie.get();

            List<DadosTemporada> temporadas = new ArrayList<>();

            for (int i = 1; i <= serieEncontrada.getTotalDeTemporadas(); i++) {
                var json = consumo.obterDados(ENDERECO + serieEncontrada.getTitulo().replace(" ", "+") + "&season=" + i + API_KEY);
                DadosTemporada dadosTemporada = conversor.obterDados(json, DadosTemporada.class);
                temporadas.add(dadosTemporada);
            }
            temporadas.forEach(System.out::println);

            List<Episodio> episodios = temporadas.stream()
                    .flatMap(d -> d.episodios().stream()
                            .map(e -> new Episodio(d.numero(), e)))
                    .collect(Collectors.toList());

            serieEncontrada.setEpisodios(episodios);
            repositorio.save(serieEncontrada);
        } else {
            System.out.println("Série não encontrada!");
        }
    }

    private void listarSeriesBuscadas() {
        series = repositorio.findAll();
        series.stream()
                .sorted(Comparator.comparing(Serie::getGenero))
                .forEach(System.out::println);
    }

    private void buscarSeriePorTitulo() {
        System.out.println("Escolha uma serie pelo nome: ");
        var nomeSerie = leitura.nextLine();
        serieBusca = repositorio.findByTituloContainingIgnoreCase(nomeSerie);
        if (serieBusca.isPresent()){
            System.out.println("Dados da Série: " + serieBusca);
        }else {
            System.out.println("Série não encontrada.");
        }
    }

    private void buscarSeriePorAtor(){
        System.out.println("Digite o nome do Ator: ");
        var nomeAtor = leitura.nextLine();
        System.out.println("A partir de qual avaliação: ");
        var avaliacao = leitura.nextDouble();
        List<Serie> seriesEncontradas = repositorio.findByAtoresContainingIgnoreCaseAndAvaliacaoGreaterThanEqual(nomeAtor, avaliacao);
        System.out.println("Séries em que " + nomeAtor + " trabalhou: ");
        seriesEncontradas.forEach(s -> System.out.println(s.getTitulo() + "Avaliação" + s.getAvaliacao()));
    }

    private void buscarTop5Series(){
        List<Serie> seriesTop = repositorio.findTop5ByOrderByAvaliacaoDesc();
        seriesTop.forEach(s -> System.out.println(s.getTitulo() + " Avaliaçao " + s.getAvaliacao()));
    }

    private void buscarSeriesPorCategoria(){
        System.out.println("Qual o genero da série para busca: ");
        var nomeGenero = leitura.nextLine();
        Categoria categoria = Categoria.fromPortugues(nomeGenero);
        List<Serie> seriesbuscadas = repositorio.findByGenero(categoria);
        System.out.println("Séries da Categoria: " + nomeGenero);
        seriesbuscadas.forEach(s -> System.out.println("Séire: " + s.getTitulo() + " Categoria: " + s.getGenero()));
    }

    private void buscarSeriesPorQuantidadeTemporada(){
        System.out.println("Até quantas Temporadas a série pode ter: ");
        var serieQuantidade = leitura.nextInt();
        System.out.println("Até que avaliação buscar: ");
        var serieAvaliacao = leitura.nextDouble();
        List<Serie> filtroSeries = repositorio.seriesPorTemporadaEAvaliacao(serieQuantidade, serieAvaliacao);
        System.out.println("As Séries com " + serieQuantidade + " Temporadas e com " + serieAvaliacao + " de avaliação são: ");
        filtroSeries.forEach(s -> System.out.println("Nome " + s.getTitulo() + " Temporadas " + s.getTotalDeTemporadas() + " Avaliação " + s.getAvaliacao()));
    }

    private void buscarEpisodioPorTecho(){
        System.out.println("Digite o trecho do nome do Episódio: ");
        var trechoEpisodio = leitura.nextLine();
        List<Episodio> episodiosPorTrecho = repositorio.episodiosPorTrecho(trechoEpisodio);
        episodiosPorTrecho.forEach(System.out::println);
    }

    private void topEpisodiosPorSerie(){
        buscarSeriePorTitulo();
            if (serieBusca.isPresent()){
                Serie serie = serieBusca.get();
                List<Episodio> topEpisodios = repositorio.topEpisodiosPorSerie(serie);
                topEpisodios.forEach(System.out::println);
            }
    }

    private void buscarEpisodioPorData(){
        buscarSeriePorTitulo();
        if (serieBusca.isPresent()){
            Serie serie = serieBusca.get();
            System.out.println("Digite o ano limite: ");
            var anoLimite = leitura.nextInt();
            leitura.nextLine();
            List<Episodio> episodioPorData = repositorio.buscaEpisodioPorData(serie, anoLimite);
            episodioPorData.forEach(System.out::println);
        }
    }
}
