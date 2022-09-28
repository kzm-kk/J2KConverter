package testpac;

public class Sample{
    String str, moji;
    public Sample(){
        str = "nothing";
    }
    public Sample(String moji){
        str = "something";
        this.moji = moji;
    }
    void func(String str2){
        int len =
                moji != null ? moji.length() : -1;
        String tmp = str2 != null ? str2 : "";
        if(tmp.length() > 5)
            str = tmp.substring(0, 3);
    }
}
