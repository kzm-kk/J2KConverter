package pac3;

import java.util.ArrayList;

public class Class14 {

    ArrayList<String> arrayList = new ArrayList<>();

    public void func(){
        for(int i= 0; i< 5; i++)arrayList.add(Integer.toString(i));

        for(String str:arrayList){
            System.out.println(str);
        }

        Interface3 if32 = (s) -> {return s;};
        Interface4 if4;
        String str = String.join(",","a", "b", "c");

        /*
        for (str in arrayList) {
            println(str)
        }
         */
    }
}
