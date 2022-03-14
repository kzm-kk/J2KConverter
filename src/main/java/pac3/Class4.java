package pac3;

public class Class4 {
    int num = 5;

    public void func(int num){
        int plus = 4 + num + this.num;
        int minus = 5 - num + this.num;
        int multi = (int)Math.pow(4, 2) * 4;
        int divide = (6 / 3) / 2;
        int mod = 6 % 4;
        int sum = plus + minus + multi + divide + mod;
        System.out.println("get");
        boolean tmp = 3 > 2;
    }
}
