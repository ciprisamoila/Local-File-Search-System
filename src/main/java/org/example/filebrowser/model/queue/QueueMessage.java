package org.example.filebrowser.model.queue;

import org.example.filebrowser.model.index.UpdateValidationData;
import org.example.filebrowser.model.queue.payloads.QueuePayload;

import java.util.concurrent.CompletableFuture;

public record QueueMessage(
        QueuePayload payload,
        MessageType type,
        CompletableFuture<UpdateValidationData> future
) {
}
