package pac3;

public class Class9 extends Class8{
     static int num = -1;
     int outerMember;

     public Class9(){
         super("null");
         super.outerMember = 0;
     }

    public Class9(String str) {
        super(str);
    }

    public static void main(String[] args){
        func();
        LOOP:for(int i = 0; i< 5; i++){
            int j = 0;
            while(true){
                if(j > 4) break LOOP;
                j++;
            }
        }
    }

    public static void func(){
         if(true){

        } else if(false){
             if(true);
         } else {
             if(false);
         }
        assert num > 0;
    }

    public static class Class9_In{

    }
}
