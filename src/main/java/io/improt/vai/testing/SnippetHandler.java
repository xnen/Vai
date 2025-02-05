package io.improt.vai.testing;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class SnippetHandler {
    private final BlockingQueue<String> snippetQueue = new LinkedBlockingQueue<>();
    private final Thread worker;

    private ISnippetAction action;
    public SnippetHandler(ISnippetAction action) {
        worker = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    String snippet = snippetQueue.take();
                    processSnippet(snippet); // Your processing code here.
                    Thread.yield();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        worker.start();

        this.action = action;
    }

    public void addSnippet(String snippet) {
        snippetQueue.offer(snippet);
    }

    private void processSnippet(String snippet) {
        // Process the snippet in this thread.
        System.out.println("Processing snippet: " + snippet);
        this.action.doAction(snippet);
    }

    public void shutdown() {
        worker.interrupt();
    }
}