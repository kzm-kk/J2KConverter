package pac3;

import pac4.pac.TrialClass;

import java.util.ArrayList;
import java.util.List;

public class Class10 {
    int num = 0, num2 = 0;
    String str = null;
    List<Integer> listInt = new ArrayList<>();

    public void func(){
        if(num > 0 || num ==0){
            num2 = num + num2;
        } else {
            num2 += 2;
        }

        switch (num){
            case 1:
                num2 = 1;
                break;
            case 2:
                num2 = 2;
                break;
            default:
                num2 = 0;
                break;
        }

        TrialClass tc = new TrialClass();

    }

    public int plus1(){
        return num + 1;
    }

    public String retStr(){
        return "moji";
    }

}
