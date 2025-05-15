package io.improt.vai.util.stream;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class SnippetHandler {
    private final BlockingQueue<String> snippetQueue = new LinkedBlockingQueue<>();
    private final Thread worker;
    private final ISnippetAction action;
    private final Runnable onComplete;
    private final AtomicBoolean processingComplete = new AtomicBoolean(false); // Flag for completion

    public SnippetHandler(ISnippetAction action, Runnable onComplete) {
        this.action = action;
        this.onComplete = onComplete;

        worker = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    // Process existing queue items first
                    while (!snippetQueue.isEmpty()) {
                        String snippet = snippetQueue.poll(); // Use poll, don't block indefinitely here
                        if (snippet != null) {
                            processSnippet(snippet);
                        }
                    }
                    // Check if completion has been signaled and the queue is empty
                    if (processingComplete.get() && snippetQueue.isEmpty()) {
                        break; // Exit loop if done processing and queue is drained
                    }
                    // Wait for new items or interruption
                    String snippet = snippetQueue.take(); // Block here if queue is empty and not done
                    processSnippet(snippet);
                }
            } catch (InterruptedException e) {
                // Thread interrupted, likely during take()
                Thread.currentThread().interrupt(); // Re-interrupt thread
            } finally {
                // Ensure onComplete is called when the loop finishes naturally or via interruption
                 if (this.onComplete != null) {
                    this.onComplete.run();
                 }
            }
        });
        worker.start();
    }

    public void addSnippet(String snippet) {
        if (!processingComplete.get()) { // Only add if not marked as complete
            snippetQueue.offer(snippet);
        }
    }

    // Signal that no more snippets will be added
    public void signalComplete() {
        processingComplete.set(true);
         // Add a sentinel value or interrupt to unblock take() if needed,
         // though checking the flag before take() might be sufficient.
         // Adding a special marker might be safer:
         snippetQueue.offer(""); // Offer an empty string to potentially wake up the worker thread
    }


    private void processSnippet(String snippet) {
        // Avoid processing the sentinel empty string
        if (!snippet.isEmpty()) {
            this.action.doAction(snippet);
        }
    }

    public void shutdown() {
        processingComplete.set(true); // Ensure completion is signaled
        worker.interrupt(); // Interrupt the worker thread
    }
}
