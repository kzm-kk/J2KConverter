package pac3;

import java.util.ArrayList;

public class Class6 {
    static {
        System.out.println("static initialize");
    }
    {
        System.out.println("initialize");
    }
    {
        System.out.println("ok");
    }
    Class5<? super Integer> class5;

    Interface1 interface1 = new Interface1(){
        private String msg = "no name class";
        public void func(){
            System.out.println("msg");
        }
    };

    public void func6(){
        ArrayList<Integer> list = new ArrayList<>();

        for(int i = 0 ; i < 10 ; i++) {
            list.add(i);
        }

        list.forEach((s) -> {System.out.println(s);});

        // メソッド参照
        //list.forEach(Consumer { obj: Int? -> print(obj) })
        list.forEach(System.out::print);
    }
}
