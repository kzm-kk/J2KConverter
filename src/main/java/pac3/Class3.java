package pac3;

import com.github.javaparser.ParseException;

import java.io.IOException;
import java.sql.SQLException;

public class Class3 {
    public boolean isPrimitiveArray(String type){
        System.out.print("\n");
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

    public enum Fruits {
        Banana(0),
        Orange(1),
        Apple(2);

        private int id; // フィールドの定義

        private Fruits(int id) { // コンストラクタの定義
            this.id = id;
        }

    }


    public void func2(int... nums){
        int i = 0;
        Object obj = "coral";
        String str = ((String) obj).substring(1);
        do {
            i++;
        }while(i < 5);
        do i++;
        while (i<5);
        i = Fruits.Banana.id;
    }

    public void func(){
        try {
            int score = 76;
            String result = score > 70 ? "OK" : "NG";
            System.out.println("try");
        } catch (NumberFormatException | NullPointerException e){
            System.out.println(e);
        } finally {
            Runnable runner = () -> {
                System.out.println("Hello Lambda!");
            };
            System.out.println("finally");
        }
    }
}
