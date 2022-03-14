package pac2;

import pac1.Verf_pac1;

public class Verf_pac2_3 {

    private pac2.Verf_pac2_2 verfPac22;
    private pac2.Verf_pac2_4 verfPac24;

    public Verf_pac2_3(){

        verfPac22 = new pac2.Verf_pac2_2();
        verfPac24 = new pac2.Verf_pac2_4();
    }

    public void func(){
        int num = verfPac22.getVerf_pac1().getTemp1();
        verfPac22.getVerf_pac1().setTemp2(4);
        verfPac24.getVerf_pac1().setTemp3(verfPac22.getVerf_pac1().getTemp2());
        num = verfPac24.getVerf_pac1().getTemp4();

        Verf_pac1 verf_pac1 = verfPac22.getVerf_pac1();
    }

}
