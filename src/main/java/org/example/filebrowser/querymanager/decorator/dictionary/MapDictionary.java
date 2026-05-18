package org.example.filebrowser.querymanager.decorator.dictionary;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// an in memory stub dictionary
// possible improvement: a small table with all synonyms
public class MapDictionary implements ISynonymDictionary {

    private final Map<String, List<String>> synonyms = new HashMap<>();

    public MapDictionary() {
        synonyms.put("img", List.of("image", "photo", "picture"));
        synonyms.put("house", List.of("home", "residence"));
        synonyms.put("video", List.of("movie", "film"));
    }

    @Override
    public List<String> getSynonyms(String word) {
        return synonyms.get(word);
    }
}
