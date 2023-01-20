package pac3;

public class SpeedCompareSample {

    public static void main(String[] args){
        long timeStart = System.currentTimeMillis();
        int num = 40;
        System.out.println(fib(num));
        long timeEnd = System.currentTimeMillis();
        System.out.println("time:" + (timeEnd - timeStart));
    }

    static int fib(int num){
        if(num == 0) return 0;
        else if(num == 1) return 1;
        else return fib(num - 1) + fib(num - 2);
    }

}
