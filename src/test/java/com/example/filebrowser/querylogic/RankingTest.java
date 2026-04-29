package com.example.filebrowser.querylogic;

import org.example.filebrowser.model.QueryFileModel;
import org.example.filebrowser.model.QuerySpecs;
import org.example.filebrowser.model.RankingStrategy;
import org.example.filebrowser.querylogic.IQuerier;
import org.example.filebrowser.querylogic.QueryParser;
import org.example.filebrowser.utils.exceptions.QueryManagerException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.text.Collator;
import java.util.List;
import java.util.Locale;

public class RankingTest {
    static IQuerier querier;
    static int MAX_RESULTS = 1000;
    @BeforeAll
    public static void setup() throws QueryManagerException {
        querier = new QueryParser();
    }
    @Test
    public void testAlphabeticalAscending() throws QueryManagerException {
        List<QueryFileModel> results = querier.getNextFilesMatching(
                new QuerySpecs(
                        MAX_RESULTS, 0,
                        RankingStrategy.ALPHABETICAL,
                        true
                ),
                "NOT size:-1", // gets all results
                true
        );

        // now we check if indeed the results are sorted accordingly
        boolean failed = false;
        for (int i = 0; i < results.size() - 1; i++) {
            if (results.get(i).fullName().compareTo(results.get(i + 1).fullName()) > 0 ) {
                failed = true;
                break;
            }
        }
        assert (!failed);
    }
    @Test
    public void testAlphabeticalDescending() throws QueryManagerException {
        List<QueryFileModel> results = querier.getNextFilesMatching(
                new QuerySpecs(
                        MAX_RESULTS, 0,
                        RankingStrategy.ALPHABETICAL,
                        false
                ),
                "NOT size:-1", // gets all results
                true
        );

        // now we check if indeed the results are sorted accordingly
        boolean failed = false;
        for (int i = 0; i < results.size() - 1; i++) {
            if (results.get(i).fullName().compareTo(results.get(i + 1).fullName()) < 0 ) {
                failed = true;
                break;
            }
        }
        assert (!failed);
    }
    @Test
    public void testDateAccessedAscending() throws QueryManagerException {
        List<QueryFileModel> results = querier.getNextFilesMatching(
                new QuerySpecs(
                        MAX_RESULTS, 0,
                        RankingStrategy.DATE_ACCESSED,
                        true
                ),
                "NOT size:-1", // gets all results
                true
        );

        // now we check if indeed the results are sorted accordingly
        boolean failed = false;
        List<Timestamp> timestamps = results.stream()
                .map(file -> Timestamp.valueOf(file.last_accessed_time()))
                .toList();
        for (int i = 0; i < timestamps.size() - 1; i++) {
            if (timestamps.get(i).compareTo(timestamps.get(i + 1)) > 0 ) {
                failed = true;
                break;
            }
        }
        assert (!failed);
    }
    @Test
    public void testDateAccessedDescending() throws QueryManagerException {
        List<QueryFileModel> results = querier.getNextFilesMatching(
                new QuerySpecs(
                        MAX_RESULTS, 0,
                        RankingStrategy.DATE_ACCESSED,
                        false
                ),
                "NOT size:-1", // gets all results
                true
        );

        // now we check if indeed the results are sorted accordingly
        boolean failed = false;
        List<Timestamp> timestamps = results.stream()
                .map(file -> Timestamp.valueOf(file.last_accessed_time()))
                .toList();
        for (int i = 0; i < timestamps.size() - 1; i++) {
            if (timestamps.get(i).compareTo(timestamps.get(i + 1)) < 0 ) {
                failed = true;
                break;
            }
        }
        assert (!failed);
    }
}
