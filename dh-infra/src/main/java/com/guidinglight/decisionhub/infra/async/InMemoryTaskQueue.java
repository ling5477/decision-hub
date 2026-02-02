package com.guidinglight.decisionhub.infra.async;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class InMemoryTaskQueue<T> {

  private final BlockingQueue<T> q = new LinkedBlockingQueue<>();

  public void offer(T t) {
    q.offer(t);
  }

  public T take() throws InterruptedException {
    return q.take();
  }

  public int size() {
    return q.size();
  }
}
