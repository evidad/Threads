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
    private final int sleepTime; // Sleep time for the thread
    private final Queue<Double> queue; // Shared queue

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
                Thread.sleep(sleepTime); // Each thread sleeps for a different amount of time
                queue.add(Double.parseDouble(String.format("%.2f", randomNumber))); // Add the random number to the queue
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Thread interrupted: " + e.getMessage());
                break;
            }
        }
    }

    public static void main(String[] args) {
        // Create 5 ConcurrentLinkedQueues for the producers
        List<Queue<Double>> queues = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            queues.add(new ConcurrentLinkedQueue<>());
        }

        // Create vectors to store the queue lengths after sorting
        List<Vector<Integer>> vectors = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            vectors.add(new Vector<>());
        }

        // Create a fixed thread pool with 5 threads using ExecutorService
        ExecutorService executorService = Executors.newFixedThreadPool(5);

        // Submit tasks (producer threads) to the ExecutorService
        executorService.submit(new Threads(5, 10, "Thread 1", 1000, queues.get(0)));
        executorService.submit(new Threads(15, 20, "Thread 2", 1500, queues.get(1)));
        executorService.submit(new Threads(25, 30, "Thread 3", 2000, queues.get(2)));
        executorService.submit(new Threads(25, 40, "Thread 4", 2500, queues.get(3)));
        executorService.submit(new Threads(45, 50, "Thread 5", 3000, queues.get(4)));

        // Start the sorting thread
        Thread sortingThread = new Thread(new QueueSorter(queues, vectors));
        sortingThread.start();

        // Schedule a task to stop all threads after 5 minutes
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.schedule(() -> {
            // Shut down all threads
            executorService.shutdownNow();
            sortingThread.interrupt();

            // Report final queue sizes and sorting extent
            System.out.println("\nFinal Report:");
            for (int i = 0; i < queues.size(); i++) {
                Queue<Double> queue = queues.get(i);
                List<Double> tempList = new ArrayList<>(queue);  // Copy current queue into a list
                System.out.printf("Queue %d size: %d%n", i + 1, tempList.size());

                // Check sorting extent
                int sortedCount = checkSorted(tempList);
                System.out.printf("Queue %d sorted count: %d/%d%n", i + 1, sortedCount, tempList.size());
            }
            scheduler.shutdown();
        }, 5, TimeUnit.MINUTES);

        // Shutdown the executor service gracefully after tasks completion
        try {
            // Wait for all tasks to finish before exiting
            if (!executorService.awaitTermination(5, TimeUnit.MINUTES)) {
                executorService.shutdownNow(); // Force shutdown if tasks take too long
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
    }

    // Helper function to check how many elements are sorted
    private static int checkSorted(List<Double> list) {
        if (list.size() <= 1) {
            return list.size();
        }
        int sortedCount = 1; // First element is always considered sorted
        for (int i = 1; i < list.size(); i++) {
            if (list.get(i) >= list.get(i - 1)) {
                sortedCount++;
            } else {
                break; // Stop counting at the first unsorted element
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
                // Sleep for a random time between 1 and 5 seconds
                Thread.sleep((random.nextInt(5) + 1) * 1000);

                // Sort each queue and record its length
                for (int i = 0; i < queues.size(); i++) {
                    Queue<Double> queue = queues.get(i);

                    // Remove all elements from the queue into a list
                    List<Double> tempList = new ArrayList<>(queue);
                    queue.clear(); // Clear the queue after copying the elements

                    // Sort the list
                    Collections.sort(tempList);

                    // Reinsert the sorted elements back into the queue
                    for (Double num : tempList) {
                        queue.add(num);
                    }

                    // Record the length of the queue in the corresponding vector
                    vectors.get(i).add(tempList.size());

                    // Print out the sorted queue and its length
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
