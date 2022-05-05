package pac4;

public class ClassD {
    ClassA classA = new ClassA();

    public void funcD(){
        int tmp = classA.classBs[2].classC.num;
        classA.classBs[1].classC.num = 3;
    }
}
