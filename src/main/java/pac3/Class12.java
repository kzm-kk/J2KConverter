package pac3;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Class12 {

    public static void main(String[] args) throws IOException {
        Interface3 if31 = (String s) -> {return "lambda";};
        Interface3 if32 = (s) -> {return "lambda";};
        Interface3 if33 = s -> {return "lambda";};
        Interface3 if34 = s -> "lambda";
        Interface3 if35 = new Interface3() {
            @Override
            public String func(String s) {
                return "lambda";
            }
        };
        if32 = (s) -> {return "lambda";};

        func();
        System.out.println(if31.func(""));
    }

    public static void func(){
        ArrayList<Integer> list = new ArrayList<>();
        int k = -1;

        for(int i = 0 ; i < 10 ; i++) {
            list.add(i);
        }

        if(k > 1);

        list.forEach((s) -> {System.out.print(s);});

        // メソッド参照
        //list.forEach(Consumer { obj: Int? -> print(obj) })
        list.forEach(System.out::print);
    }
}
