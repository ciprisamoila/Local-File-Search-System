package org.example.filebrowser.querymanager.tracking;

import java.util.List;

public record Observation(
        List<Long> searchFileIds,
        String query
) {
}
