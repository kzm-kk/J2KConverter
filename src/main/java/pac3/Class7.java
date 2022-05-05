package pac3;

import java.util.ArrayList;
import java.util.List;

public class Class7<T extends Number> {

    List<? extends Number> list = new ArrayList<>();
    List<? super Number> list2 = new ArrayList<>();
    List<?> list3 = new ArrayList<>();
    List<Integer> listInt = new ArrayList<>();
    List<Long> listLong = new ArrayList<>();
    List<T> tlist = new ArrayList<T>();
    int[] ints = new int[3];
    int num = 5;
    String str = "";
    String[] strs = new String[3];

    void func(int... ints){
        list = listInt; // Integer 型の代入を許容
        list = listLong; // Long 型の代入を許容
        list2 = new ArrayList<Object>();
    }


    Interface2 interface2 = (String str1, String str2) -> {
        System.out.println("lambda");
        return str1+str2;
    };

    Interface2 interface22 = (String str1, String str2) -> {
        return str1+str2;
    };
}
