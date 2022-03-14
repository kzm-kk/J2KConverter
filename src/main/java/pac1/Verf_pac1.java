package pac1;

public class Verf_pac1 {

    /*命名規則
    フィールド名tmpに対してgetterがgetTmp, setterがsetTmpのように
    xxxに対してgetXxxかつsetXxxとつけること
    具体的には、xxx部分はフィールドとメソッドで同じかつ、メソッド側が先頭大文字*/



    //case1：純粋なgetter/setter
    private int temp1;
    public void setTemp1(int value){
        this.temp1 = value;
    }
    public int getTemp1(){
        return this.temp1;
    }

    //case2：フィールド名とメソッド名が命名規則から外れる
    private int tmp2;
    public void setTemp2(int value){
        this.tmp2 = value;
    }
    public int getTemp2(){
        return this.tmp2;
    }

    //case3：getterのみ命名規則から外れる
    private int temp3;
    public void setTemp3(int value){
        this.temp3 = value;
    }
    public int getTmp3(){
        return this.temp3;
    }

    //case4：setterのみ命名規則から外れる
    private int temp4;
    public void setTmp4(int value){
        this.temp4 = value;
    }
    public int getTemp4(){
        return this.temp4;
    }

    //case5：メソッドの返り値が純粋なgetter/setterとは異なる(int/void)
    private int temp5;
    public int setTemp5(int value){
        this.temp5 = value;
        return 1;
    }
    public void getTemp5(){
        int clock = 0;
    }

    //case6：getterのみ返り値が純粋なgetterとは異なる(int)
    private int temp6;
    public void setTemp6(int value){
        this.temp6 = value;
    }
    public void getTemp6(){
        int clock = 0;
    }

    //case7：setterのみ返り値が純粋なsetterとは異なる(void)
    private int temp7;
    public int setTemp7(int value){
        this.temp7 = value;
        return 1;
    }
    public int getTemp7(){
        return this.temp7;
    }

    //case8：メソッドの引数の数が純粋なgetter/setterとは異なる(0/1)
    private int temp8;
    public void setTemp8(int value, int c){
        this.temp8 = value;
    }
    public int getTemp8(int value){
        return this.temp8;
    }

    //case9：getterのみ引数の数が純粋なgetterとは異なる(0)
    private int temp9;
    public void setTemp9(int value){
        this.temp9 = value;
    }
    public int getTemp9(int value){
        return this.temp9;
    }

    //case10：setterのみ引数の数が純粋なsetterとは異なる(1)
    private int temp10;
    public void setTemp10(int value, int c){
        this.temp10 = value;
    }
    public int getTemp10(){
        return this.temp10;
    }

    //case11：メソッド名がgetXXX/setXXXではない
    private int temp11;
    public void include11(int value){
        this.temp11 = value;
    }
    public int exclude11(){
        return this.temp11;
    }

    //case12：getterのみgetXXXではない
    private int temp12;
    public void setTemp12(int value){
        this.temp12 = value;
    }
    public int exclude12(){
        return this.temp12;
    }

    //case13：setterのみsetXXXではない
    private int temp13;
    public void include13(int value){
        this.temp13 = value;
    }
    public int getTemp13(){
        return this.temp13;
    }

    //case14：フィールド名は存在しないがメソッド名は命名規則に従う
    public void setTemp14(int value){

    }
    public int getTemp14(){
        return 1;
    }

    //case15：case14において、getterとsetterが命名規則に従いつつ別々の名付けの時
    public void setTmp15(int value){

    }
    public int getTemp15(){
        return 1;
    }

    //case16：case14において、getterの返り値がvoidの時
    public void setTemp16(int value){

    }
    public void getTemp16(){
        int cvlock = 0;
        double pio = 0.3;
        System.out.println("45");
    }

    //case17：case14において、setterの返り値がvoid以外の時
    public int setTemp17(int value){
        return 1;

    }
    public int getTemp17(){
        return 1;
    }

    //case18：フィールドはあるが、getXXXの返り値がフィールドと違い、全く関係ないメソッドの時
    private String message18 = "";
    public void getMessage18(){
        System.out.print("length");
    }

    //case19：case18において、setXXXの引数がフィールドと違い、getの返り値と同じ時
    private String message19 = "";
    public int getMessage19(){
        System.out.print("length");
        return 0;
    }
    public void setMessage19(int value){

    }
}
