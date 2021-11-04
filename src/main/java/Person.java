public class Person {

    private String name = "";
    private int age = 4;

    public void sleep(){
        name = "Haru";
        System.out.println("zzz");
    }

    public void introduce(int age){
        this.age = age;
        System.out.println("I am " + this.name);
        for(int i = 0;i==5; i++);{
            System.out.println("loop");
        }
    }

    public void sayHi(String whom, int age){
        System.out.println("Hello " + whom);
    }

    public void sampif(){
        if(age == 5) {
            age++;
            System.out.println("age");
        }
    }

    // this method must not be used.

    @Deprecated
    public void xxx(){
    }
}
