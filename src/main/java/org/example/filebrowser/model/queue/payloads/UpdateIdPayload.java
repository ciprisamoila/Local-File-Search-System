package org.example.filebrowser.model.queue.payloads;

public record UpdateIdPayload(
        long fileId,
        long scanId
) implements QueuePayload {
}
