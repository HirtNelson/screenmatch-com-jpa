package br.com.alura.screenmatch.dto;

import br.com.alura.screenmatch.model.Categoria;

public record SerieDTO(long id,
                       String titulo,
                       Integer totalDeTemporadas,
                       Double avaliacao,
                       Categoria genero,
                       String atores,
                       String poster,
                       String sinopse){

}



