package pac3;

//comment0
//comment1
public class Class1{
    public int num = 0;
    public String str = "";
    public Class2 class2 = new Class2();
    public Class2[] class2s = new Class2[4];

    //comment2

    public void func(int num){
        this.class2.num = num;
        this.num = num;
        class2s[0].ints[2] = num;
        /*
        comment3

         */
    }
}
