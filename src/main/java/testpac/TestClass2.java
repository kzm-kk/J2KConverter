package testpac;

/**
 * 計算式とコンストラクタ、初期化子による初期化タイミングの違いの確認
 */
public class TestClass2 {
    String strInitDec = "";
    String strInitConWR;
    String strInitIni;
    String strInitConRW;
    int num;

    {
        strInitIni = "initialize: Initializer";
        strInitDec = strInitConRW;
    }

    public TestClass2(){
        strInitConWR = "initialize: Constructor";
        strInitConRW = "initialize: Constructor";

        num = num + 5 * 7;
    }

}
