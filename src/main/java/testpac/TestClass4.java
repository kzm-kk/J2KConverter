package testpac;

/**
 * switch文やfor文、メソッド呼び出し時の引数に使われた時の挙動を確認する
 */
public class TestClass4 {
    int num;
    int num2;

    int num3;

    int num4 = 0;

    public void func(){
        for(; num2 < 5; num2++){
            setNum(num3);

            switch (num2){
                case 1:
                    num4 = 1;
                    break;
                case 2:
                    num4 = 2;
                    break;
                default:
                    num4 = 0;
                    break;
            }
        }
    }

    public void setNum(int num){
        this.num += num;
    }

    public int getNum3(){
        return num3;
    }

    public String NullStr(){
        return null;
    }
}
