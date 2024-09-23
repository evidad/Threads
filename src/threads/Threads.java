/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package threads;

import java.util.Random;
import java.util.stream.DoubleStream;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Vector;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author errol
 */

public class Threads implements Runnable {

    private final double min;
    private final double max;
    private final String threadName;
    private final int sleepTime;
    private final Queue<Double> queue;

    public Threads(double min, double max, String threadName, int sleepTime, Queue<Double> queue) {
        this.min = min;
        this.max = max;
        this.threadName = threadName;
        this.sleepTime = sleepTime;
        this.queue = queue;
    }

    @Override
    public void run() {
        Random random = new Random();
        while (!Thread.currentThread().isInterrupted()) {
            try {
                double randomNumber = min + (max - min) * random.nextDouble();
                System.out.printf("%s generated: %.2f%n", threadName, randomNumber);
                Thread.sleep(sleepTime);
                queue.add(Double.parseDouble(String.format("%.2f", randomNumber)));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Thread interrupted: " + e.getMessage());
                break;
            }
        }
    }

    public static void main(String[] args) {
        List<Queue<Double>> queues = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            queues.add(new ConcurrentLinkedQueue<>());
        }

        List<Vector<Integer>> vectors = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            vectors.add(new Vector<>());
        }

        ExecutorService executorService = Executors.newFixedThreadPool(5);

        executorService.submit(new Threads(5, 10, "Thread 1", 1000, queues.get(0)));
        executorService.submit(new Threads(15, 20, "Thread 2", 1500, queues.get(1)));
        executorService.submit(new Threads(25, 30, "Thread 3", 2000, queues.get(2)));
        executorService.submit(new Threads(25, 40, "Thread 4", 2500, queues.get(3)));
        executorService.submit(new Threads(45, 50, "Thread 5", 3000, queues.get(4)));

        Thread sortingThread = new Thread(new QueueSorter(queues, vectors));
        sortingThread.start();

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.schedule(() -> {
            executorService.shutdownNow();
            sortingThread.interrupt();

            System.out.println("\nFinal Report:");
            for (int i = 0; i < queues.size(); i++) {
                Queue<Double> queue = queues.get(i);
                List<Double> tempList = new ArrayList<>(queue);
                System.out.printf("Queue %d size: %d%n", i + 1, tempList.size());

                int sortedCount = checkSorted(tempList);
                System.out.printf("Queue %d sorted count: %d/%d%n", i + 1, sortedCount, tempList.size());
            }
            scheduler.shutdown();
        }, 5, TimeUnit.MINUTES);

        try {
            if (!executorService.awaitTermination(5, TimeUnit.MINUTES)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
    }

    private static int checkSorted(List<Double> list) {
        if (list.size() <= 1) {
            return list.size();
        }
        int sortedCount = 1;
        for (int i = 1; i < list.size(); i++) {
            if (list.get(i) >= list.get(i - 1)) {
                sortedCount++;
            } else {
                break;
            }
        }
        return sortedCount;
    }
}

class QueueSorter implements Runnable {

    private final List<Queue<Double>> queues;
    private final List<Vector<Integer>> vectors;
    private final Random random = new Random();

    public QueueSorter(List<Queue<Double>> queues, List<Vector<Integer>> vectors) {
        this.queues = queues;
        this.vectors = vectors;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep((random.nextInt(5) + 1) * 1000);

                for (int i = 0; i < queues.size(); i++) {
                    Queue<Double> queue = queues.get(i);

                    List<Double> tempList = new ArrayList<>(queue);
                    queue.clear(); 
                    Collections.sort(tempList);

                    for (Double num : tempList) {
                        queue.add(num);
                    }

                    vectors.get(i).add(tempList.size());

                    System.out.printf("Queue %d sorted: %s | Length recorded: %d%n", i + 1, tempList, tempList.size());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Sorting thread interrupted: " + e.getMessage());
                break;
            }
        }
    }
}
