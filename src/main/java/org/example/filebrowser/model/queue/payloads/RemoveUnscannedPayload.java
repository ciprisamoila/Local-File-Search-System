package org.example.filebrowser.model.queue.payloads;

public record RemoveUnscannedPayload(
        long scanId
) implements QueuePayload {
}
