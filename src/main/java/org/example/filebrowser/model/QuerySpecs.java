package org.example.filebrowser.model;

public record QuerySpecs(
        int nrFiles,
        int offset,
        RankingStrategy rankingStrategy,
        boolean increasing
) {
}
