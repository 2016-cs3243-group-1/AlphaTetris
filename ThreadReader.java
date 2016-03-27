import java.io.File;
import java.util.concurrent.*;

public class ThreadReader {
    static int counter = 0;
    public static void main(String[] args) throws Exception {

        final BlockingQueue<Integer> queue = new ArrayBlockingQueue<Integer>(1000);
        for (int i = 0; i < 1000; i++) {
            queue.add(i);
        }

        ExecutorService pool = Executors.newFixedThreadPool(5);


        for (int j = 0; j < 1000; j++) {
            Runnable r = new Runnable(){
                public void run() {
                    try {
//                        counter++;
//                        System.out.println(queue.take());
                        PlayerSkeleton ps = new PlayerSkeleton();
                        ps.main(new String[1]);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };
            pool.execute(r);
        }
        pool.shutdown();
    }
}

//class Fiddler implements Runnable{
//    public Fiddler(BlockingQueue<Integer> queue) {
//        System.out.println("hey im created");
//    }
//
//    public void run() {
//
//    }
//}