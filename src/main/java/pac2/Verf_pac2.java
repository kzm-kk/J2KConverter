package pac2;

import pac1.Verf_pac1;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class Verf_pac2 {
    Verf_pac1[] verf_pac1s = new Verf_pac1[5];
    ArrayList<Verf_pac1> verf_pac1array = new ArrayList<>();
    Set<Verf_pac1> verf_pac1Set = new HashSet<>();

    public void func(){
        int num = verf_pac1s[0].getTemp1();
        verf_pac1s[0].setTemp2(5);

        verf_pac1array.get(0).setTemp1(1);
        num = verf_pac1array.get(0).getTemp2();

        for(Verf_pac1 verf_pac1:verf_pac1s){
            System.out.println(verf_pac1.getTemp1());
        }

        for (Verf_pac1 verf_pac1:verf_pac1array){
            System.out.println(verf_pac1.exclude12());
        }

        for(Verf_pac1 verf_pac1:verf_pac1Set){
            System.out.println(verf_pac1.getMessage19());
        }
    }
}
