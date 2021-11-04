import java.util.Random;
import java.util.Scanner;

public class numer0n{
    public static void main(String[] args) throws Exception{
        Scanner sc = new Scanner(System.in);
        String input = sc.next();

        int[] numer0ns = new int[3];
        int j;
        Random rnd = new Random();
        for(int i=0;i<3;i++){
            while(true){
                numer0ns[i] = rnd.nextInt(10);
                for(j=0;j<=i;j++){
                    if(i == j) continue;
                    if(numer0ns[i] == numer0ns[j]) break;
                }
                if(i < j)break;
            }
        }

        for(int i=0;i<3;i++){
            while(true){
                numer0ns[i] = rnd.nextInt(10);
                for(j=0;j<=i;j++){
                    if(i == j) continue;
                    if(numer0ns[i] == numer0ns[j]) break;
                }
                if(i < j)break;
            }
        }
        System.out.println(judgement(input, numer0ns));
        System.out.println("Answer:" + numer0ns[0] + numer0ns[1] + numer0ns[2]);
    }
    public static String judgement(String now, int[] numer0ns) {
        String judge = "";
        int Hit = 0;
        int Blow = 0;
        String[] str = now.split("");
        int[] nowanswer = new int[3];
        for(int i=0;i<3;i++){
            nowanswer[i] = Integer.parseInt(str[i]);
            if(nowanswer[i] == numer0ns[i]){
                Hit++;
            } else {
                for(int j=0;j<3;j++){
                    if(i == j) continue;
                    else if(nowanswer[i] == numer0ns[j]) Blow++;
                }
            }
            judge = judge.concat("" + nowanswer[i]);
        }
        judge = judge.concat("," + Hit + "H" + Blow + "B");
        return judge;
    }
}