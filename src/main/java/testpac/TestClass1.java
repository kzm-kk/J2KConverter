package testpac;

/**
 * フィールドと簡単なアクセサメソッドに対する挙動の確認
 */
public class TestClass1 {
    private String str;
    private int num = 0;

    public TestClass1(){
        str = "str1";
    }

    public String getStr() {
        return str;
    }

    public void setStr(String str) {
        this.str = str;
    }

    public int getNum() {
        return num;
    }

    public void setNum(int num) {
        this.num = num;
    }
}
