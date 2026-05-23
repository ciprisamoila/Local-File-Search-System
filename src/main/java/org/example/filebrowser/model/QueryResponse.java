package org.example.filebrowser.model;

import java.util.List;

public record QueryResponse(
        QueryInducedType queryInducedType,
        List<QueryFileModel> queryFileModels
) {
}
