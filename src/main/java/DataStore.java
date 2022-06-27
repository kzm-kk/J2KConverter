import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DataStore {
    public static String pathName;
    public static boolean isStaticF;
    public static boolean isStaticI;
    public static boolean isStaticM;
    public static ArrayList<String> memory_classlibrary;
    public static HashMap<String, ArrayList<ImportDeclaration>> memory_import;
    public static ArrayList<String> memory_classname;
    public static HashMap<String, String> memory_extend;
    public static HashMap<String, ArrayList<String>> memory_implement;
    public static HashMap<String, ArrayList<FieldDeclaration>> memory_classfield;
    public static HashMap<String, List<MethodDeclaration>> memory_classmethod;
    public static HashMap<String, ArrayList<String>> memory_innerclass;
    public static HashMap<String, List<ConstructorDeclaration>> memory_constructor;
    public static HashMap<String, List<InitializerDeclaration>> memory_Initializer;

    public static HashMap<String, HashMap<String, HashMap<String, String>>> memory_field_info;
    public static HashMap<String, HashMap<String, HashMap<String, HashMap<String, String>>>> memory_localValue_info;
    //name:名前、type:型、dim:配列の深さなければ0、assign:代入の有無、nullable:nullになり得るか？ static:staticか否か
    //initializable:初期値の有無、formerRW:ReadとWriteどちらが先(former)か
    public static HashMap<String, HashMap<String, HashMap<String, Object>>> memory_method_info;
    //name:名前
    //static:staticか否か
    //access:アクセサメソッドか否か
    //fix:get/setだとして、暗黙的なアクセサもしくはそのカスタムアクセサに直せるか否か
    //field:どのフィールドのアクセサか
    //lines:メソッドの中身の行数。2以上ならfixがtrueでカスタムアクセサにできる可能性がある
    //nullable:メソッドの返り値がnullになり得るか

    public static void init(){
        pathName = "";
        isStaticF = false;
        isStaticI = false;
        isStaticM = false;
        memory_classlibrary = new ArrayList<>();
        memory_import = new HashMap<>();
        memory_classname = new ArrayList<>();
        memory_extend = new HashMap<>();
        memory_implement = new HashMap<>();
        memory_classfield = new HashMap<>();
        memory_classmethod = new HashMap<>();
        memory_innerclass = new HashMap<>();
        memory_constructor = new HashMap<>();
        memory_Initializer = new HashMap<>();
        memory_field_info = new HashMap<>();
        memory_localValue_info = new HashMap<>();
        memory_method_info = new HashMap<>();
    }
}
