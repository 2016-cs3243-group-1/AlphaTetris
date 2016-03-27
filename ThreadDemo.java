import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class ThreadDemo {
    public static BlockingQueue<Integer> queue = new ArrayBlockingQueue<Integer>(10);

    public static void main(String[] args) throws InterruptedException {
        PlayerSkeleton foo = new PlayerSkeleton();
        ArrayList<double[]> bar = new ArrayList<double[]>();
//        System.out.println(foo.run(new String[1]));

        int[] array = new int[10];
        for (int i = 0; i < 10; i++) {
            array[i] = i;
        }
        printArray(array);
        modifyArray(array);
        printArray(array);



        Thread t1 = new Thread(new Runnable() {
            public void run() {
                try {
                    producer();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        Thread t2 = new Thread(new Runnable() {
            public void run() {
                try {
                    consumer();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        t1.start();
        t2.start();

        t1.join();
        t2.join();
    }

    public static int[] modifyArray(int[] arr) {
        for (int i = 0; i < 10; i++) {
            arr[i] = 0;
        }
        return arr;
    }

    private static void printArray(int[] arr) {
        for (int i = 0; i < arr.length; i++) {
            System.out.print(arr[i] + " ");
        }
        System.out.println();
    }

    private static void producer() throws InterruptedException {
        Random random = new Random();
        while(true) {
            queue.put(random.nextInt(100));
        }
    }

    private static void consumer() throws InterruptedException {
        Random random = new Random();
        while(true) {
            Thread.sleep(100);
            if (random.nextInt(10) == 0) {
                Integer value = queue.take();
                System.out.println("Taken value: " + value + "; Queue size is: " + queue.size());
            }
        }
    }

    public ThreadDemo() {

    }

























}