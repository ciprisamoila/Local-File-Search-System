package org.example.filebrowser.model.queue.payloads;

public record SearchPayload (
        String path
) implements QueuePayload {
}
