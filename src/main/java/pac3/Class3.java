package pac3;

public class Class3 {
    public boolean isPrimitiveArray(String type){
        String str = type.replace("[]","");
        return switch (str){
            case "int" -> true;
            case "double" -> true;
            case "long" -> true;
            case "float" -> true;
            case "boolean" -> true;
            case "byte" -> true;
            case "short" -> true;
            case "char" -> true;
            default -> false;
        };
    }
}
