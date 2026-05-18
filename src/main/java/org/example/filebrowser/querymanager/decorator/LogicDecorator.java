package org.example.filebrowser.querymanager.decorator;

public class LogicDecorator extends QueryDecorator {
    public LogicDecorator(IQueryBuilder wrapped) {
        super(wrapped);
    }

    @Override
    public String buildQuery(String input) {
        String query = wrapped.buildQuery(input);

        // we add :* at the end of each word
        String[] words = query.split(" ");
        for (int i = 0; i < words.length; i++) {
            String word = words[i];

            if (word.contains("(") || word.contains(")") || word.contains("|") ||  word.contains("&")) {
                // skip special characters
                continue;
            }

            words[i] = word + ":*";
        }

        return String.join(" ", words);
    }
}
