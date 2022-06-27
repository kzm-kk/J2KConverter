package testpac;

/**
 * if文とかの条件確認部分(condition)や、ブロック文内部、インクリメントなどのUnaryExprの確認
 */
public class TestClass3 {
    int num;

    {
        if(num > 10){
            num = 0;
        }
    }

    public TestClass3(){
        while(num < 10){
            num++;
        }

        do{

        }while(num < 10);
    }
}
