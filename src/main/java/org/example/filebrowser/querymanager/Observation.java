package org.example.filebrowser.querymanager;

import java.util.List;

public record Observation(
        List<Long> searchFileIds,
        String query
) {
}
