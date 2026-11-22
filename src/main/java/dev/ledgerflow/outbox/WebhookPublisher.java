package dev.ledgerflow.outbox;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;

/** POSTs events to a configured URL with signature headers. Any non-2xx response is a failure. */
public class WebhookPublisher implements EventPublisher {

    private final HttpClient client;
    private final URI url;
    private final WebhookSigner signer;

    public WebhookPublisher(URI url, WebhookSigner signer) {
        this.url = url;
        this.signer = signer;
        this.client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    @Override
    public void publish(OutboxEvent event) throws Exception {
        long ts = Instant.now().getEpochSecond();
        HttpRequest request = HttpRequest.newBuilder(url)
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .header("X-LedgerFlow-Event", event.getEventType())
                .header("X-LedgerFlow-Event-Id", event.getId().toString())
                .header("X-LedgerFlow-Timestamp", String.valueOf(ts))
                .header("X-LedgerFlow-Signature", signer.sign(ts, event.getPayload()))
                .POST(HttpRequest.BodyPublishers.ofString(event.getPayload()))
                .build();
        HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
        if (response.statusCode() / 100 != 2) {
            throw new IllegalStateException("Webhook returned HTTP " + response.statusCode());
        }
    }
}
