package org.example.filebrowser.model.queue.payloads;

import org.example.filebrowser.model.index.FileModel;

public record UpdateFilePayload(
        long fileId,
        FileModel fileModel
) implements QueuePayload {
}
