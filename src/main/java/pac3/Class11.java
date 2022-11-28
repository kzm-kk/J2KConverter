package pac3;

import java.io.IOException;
import java.util.Scanner;

public class Class11 {

    public static void main(String[] args) throws Exception,IOException {
        int i = 0;
        Scanner sc = new Scanner(System.in);
        i = sc.nextInt();
        judge(i);
    }

    void ist(Object obj){
        if(!(obj instanceof String)){
            int i = 0;
            LOOP:while(i < 1){
                i--;
                i++;
                --i;
                ++i;
                i++;
            }
        }
    }

    void print(String s){
        System.out.println(s);
    }

    static void judge(int i) throws Exception {
        if(i < 0) {
            throw new Exception("iが指定範囲を超えています");
        }
    }

    void func(){
        Interface3 if31 = (String s) -> {return "lambda";};
        Interface3 if32 = (s) -> {return "lambda";};
        Interface3 if33 = s -> {return "lambda";};
        Interface3 if34 = s -> "lambda";
    }
}
