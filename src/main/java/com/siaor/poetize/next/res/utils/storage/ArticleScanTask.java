package com.siaor.poetize.next.res.utils.storage;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 文章扫描任务
 *
 * @author Siaor
 * @since 2025-02-23 11:50:16
 */
@Component
public class ArticleScanTask {
    private final BlockingQueue<String> taskQueue = new LinkedBlockingQueue<>();

    public String take() throws InterruptedException {
        return taskQueue.take();
    }

    public void put(String task) throws InterruptedException {
        taskQueue.put(task);
    }

    public void put(List<String> taskList) {
        for (String task : taskList) {
            taskQueue.offer(task);
        }
    }

    public boolean isEmpty() {
        return taskQueue.isEmpty();
    }

    public int getSize(){
        return taskQueue.size();
    }
}
