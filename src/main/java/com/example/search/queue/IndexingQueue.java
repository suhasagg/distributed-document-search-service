package com.example.search.queue;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.springframework.stereotype.Component;

@Component
public class IndexingQueue {
    private final BlockingQueue<IndexingTask> queue = new LinkedBlockingQueue<>();

    public void publish(IndexingTask task) {
        queue.offer(task);
    }

    public IndexingTask take() throws InterruptedException {
        return queue.take();
    }

    public int size() {
        return queue.size();
    }
}
