package pac3;

public class Class10 {
    int num = 0;
    int num2 = 0;
    String str = null;

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
    }

    public int plus1(){
        return num + 1;
    }

}
