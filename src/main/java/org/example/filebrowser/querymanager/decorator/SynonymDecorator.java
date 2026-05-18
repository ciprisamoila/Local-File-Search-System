package org.example.filebrowser.querymanager.decorator;

import org.example.filebrowser.querymanager.decorator.dictionary.ISynonymDictionary;

import java.util.ArrayList;
import java.util.List;

public class SynonymDecorator extends QueryDecorator {

    private final ISynonymDictionary dictionary;

    public SynonymDecorator(IQueryBuilder wrapped, ISynonymDictionary dictionary) {
        super(wrapped);
        this.dictionary = dictionary;
    }

    @Override
    public String buildQuery(String input) {
        String query = wrapped.buildQuery(input);

        String[] words = query.split(" ");
        List<String> processed = new ArrayList<>();

        for (String word : words) {
            List<String> synonyms = dictionary.getSynonyms(word);
            if (synonyms != null) {
                // we need to wrap the word and synonyms like that ( word | syn1 | syn2 | ... )
                List<String> allTerms = new ArrayList<>();
                allTerms.add(word);
                allTerms.addAll(synonyms);
                processed.add("( " + String.join(" | ", allTerms) + " )");
            } else {
                // we do nothing
                processed.add(word);
            }
        }

        // we concatenate all by & operator
        return String.join(" & ", processed);
    }
}
