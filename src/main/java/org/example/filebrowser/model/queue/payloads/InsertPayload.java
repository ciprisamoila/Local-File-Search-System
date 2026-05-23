package org.example.filebrowser.model.queue.payloads;

import org.example.filebrowser.model.index.FileModel;

public record InsertPayload(
        FileModel fileModel
) implements QueuePayload {
}
