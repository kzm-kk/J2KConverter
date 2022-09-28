package pac4.pac;

public class TrialClass {
    int[][] tmp = new int[3][3];
    int[][] nums = {{1,2,3},{1,2,3},{1,2,3}};
    String str = "hello,world";
    String[] strs = str.split(",");
    String[][] stt = new String[3][3];
    int[][][] numnum = {{{1,2,3},{1,2,3},{1,2,3}},{{1,2,3},{1,2,3},{1,2,3}}};
    static int vals = 100;

    static void Sfunc(){
        vals = 500;
    }

    public static void main(String[] args) {
        String str = "hello".split("")[0].toUpperCase().toLowerCase().substring(0);
    }



    public void func(){

        int num = 0;

        num = 4 + (5 + 4 + tmp[0][0]) ;

        if(tmp[0][0] == 2){
            this.str = "1";
        } else if(tmp[0][0] == 3){
            str = "3";
        } else {
            str = "2";
        }
    }

    public static class TMPO{
        static int numI = 0;
        double numD = 0;
        short numS = 0;
        long numL = 0;
        byte numB = 0;
        float numF = 0;
        char numC = '0';
        boolean bool = false;
    }
}
