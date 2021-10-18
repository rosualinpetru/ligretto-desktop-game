package events.impl;

import events.EventBus;
import events.EventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

public class ConcurrentEventBus<T> implements EventBus<T> {

    private final ExecutorService pool;

    private final List<EventListener<T>> listeners = new ArrayList<>();
    private final Queue<T> eventsQueue = new ConcurrentLinkedQueue<>();

    private final Semaphore listenersSemaphore = new Semaphore(1);
    private final Semaphore publishSemaphore = new Semaphore(1);
    private final Semaphore readSemaphore = new Semaphore(Integer.MAX_VALUE - 1);

    private boolean shouldDispose = false;
    private final Thread readFromQueueThread;

    public ConcurrentEventBus() {
        pool = Executors.newFixedThreadPool(4);
        readFromQueueThread = new Thread(this::readFromQueue);
        readFromQueueThread.start();
    }

    public void publish(T value) {
        try {
            // lock the eventsQueue for adding
            publishSemaphore.acquire();
            eventsQueue.add(value);
            publishSemaphore.release();

            // mark that there are events to be read in the queue
            readSemaphore.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void registerListener(EventListener<T> eventListener) {
        try {
            listenersSemaphore.acquire();
            listeners.add(eventListener);
            listenersSemaphore.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void unregisterListener(EventListener<T> eventListener) {
        try {
            listenersSemaphore.acquire();
            listeners.remove(eventListener);
            listenersSemaphore.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void dispose() {
        try {
            listenersSemaphore.acquire();
            listeners.clear();
            listenersSemaphore.release();
        } catch (InterruptedException ignored) {
        }

        shouldDispose = true;
        readFromQueueThread.interrupt();
        pool.shutdownNow();
    }

    private void readFromQueue() {
        try {
            while (!shouldDispose) {
                readSemaphore.acquire(); // wait until there are events to be read from the queue

                publishSemaphore.acquire();
                T event = eventsQueue.poll(); // get the next event
                publishSemaphore.release();

                // notify all listeners

                listenersSemaphore.acquire();
                if (event != null) {
                    listeners.stream()
                            .map(listener -> new Thread(() -> listener.notify(event)))
                            .forEach(pool::execute);
                }
                listenersSemaphore.release();
            }
        } catch (InterruptedException ignored) {
            // the thread must stop because EventBus is being disposed
        }
    }
}