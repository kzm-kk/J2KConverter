package pac3;

public class Class5<T extends Number>{
    T value;

    public int getValueInt(){
        return this.value.intValue();
    }

    public void func(){
        for(int i = 0, j = 5; i<6|| j <3;i++,j--){
            System.out.print("loop exam");
        }
    }
}
