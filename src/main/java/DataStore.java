import com.github.javaparser.Range;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.utils.Pair;

import java.util.ArrayDeque;
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
    public static ArrayList<String> memory_enum;
    public static HashMap<String, String> memory_extend;
    public static HashMap<String, ArrayList<String>> memory_implement;
    public static HashMap<String, ArrayList<FieldDeclaration>> memory_classfield;
    public static HashMap<String, List<MethodDeclaration>> memory_classmethod;
    public static HashMap<String, ArrayList<String>> memory_innerclass;
    public static HashMap<String, List<ConstructorDeclaration>> memory_constructor;
    public static HashMap<String, ArrayList<InitializerDeclaration>> memory_Initializer;

    public static HashMap<String, HashMap<String, HashMap<String, String>>> memory_field_info;
    public static HashMap<String, HashMap<Range, HashMap<String, HashMap<String, String>>>> memory_localValue_info;
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
    //type:メソッドの返り値
    //range:このメソッドの記述範囲、localvalueを引き出すために使う

    //情報収集の保存用Hashmapの新バージョン

    //key:pathDir, value:ArrayList<pathAbs>
    public static HashMap<String, ArrayList<String>> memoryPathAbs;
    //key1:pathAbs, key2:classname
    public static TwinKeyDataList<String, String, ClassInfomation> memoryClass;

    public static TwinKeyDataList<String, String, ClassTemporaryStore> memoryBeforeCheck;

    //各ファイルにcompanion objectを作るためのimport kotlin.jvm.JvmStaticが必要かどうか
    //key:pathAbs, value:boolean
    public static HashMap<String, Boolean> memoryStatic;


    public static void init(){
        pathName = "";
        isStaticF = false;
        isStaticI = false;
        isStaticM = false;
        memory_classlibrary = new ArrayList<>();
        memory_import = new HashMap<>();
        memory_classname = new ArrayList<>();
        memory_enum = new ArrayList<>();
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
        memoryPathAbs = new HashMap<>();
        memoryClass = new TwinKeyDataList<>();
        memoryBeforeCheck = new TwinKeyDataList<>();
        memoryStatic = new HashMap<>();
    }

    //自クラス内のスコープの捜索
    //target:探したい変数の設定場所, range:現在解析している場所の範囲
    public static Pair<ArrayDeque<BlockInfomation>, FieldInfomation> searchDefinitionLocal(String target, Range range, ArrayDeque<BlockInfomation> deque){
        ArrayDeque<BlockInfomation> dequeBI = deque;

        if(dequeBI == null) return null;

        while(dequeBI.peek() != null) {
            BlockInfomation BI = dequeBI.peekLast();
            if (rangeScopeCheck(BI.range, range)) {
                for (String name : BI.getMemoryF().keySet()) {
                    FieldInfomation FI = BI.getMemoryF().get(name);
                    if (name.equals(target) && rangeDefinitionCheck(FI.range, range)) {
                        return new Pair(dequeBI, FI);
                    }
                }
            }
            dequeBI.pollLast();
        }
        return null;
    }

    //自クラス以外の同一ディレクトリを探す
    public static Triplets<String, ClassInfomation, FieldInfomation> searchDefinitionField(String target, String defClass, String pathAbs, String classname){

        //同一ファイル最上位、フィールド定義
        ClassInfomation CI = DataStore.memoryClass.getData(pathAbs, classname);
        for(String name:CI.getMemoryF().keySet()){
            if(name.equals(target)){
                FieldInfomation FI = CI.getMemoryF().get(name);
                return new Triplets(pathAbs, CI, FI);
            }
        }
        String pathDirThis = CI.pathDir;
        if(DataStore.memoryPathAbs.get(pathDirThis) != null) {
            for (String path : DataStore.memoryPathAbs.get(pathDirThis)) {
                for (Triplets<String, String, ClassInfomation> tripletsCI : DataStore.memoryClass.getDataListKey1(path)) {
                    //自クラス、extends, implementは飛ばす
                    if (tripletsCI.getCenterValue().equals(classname)) continue;
                    else if (tripletsCI.getCenterValue().equals(defClass)) {
                        for (String name : tripletsCI.getRightValue().getMemoryF().keySet()) {
                            if (name.equals(target)) {
                                FieldInfomation FI = tripletsCI.getRightValue().getMemoryF().get(name);
                                return new Triplets(path, tripletsCI.rightValue, FI);
                            }
                        }
                    }
                }
            }
        }
        //全範囲から検索(同一ディレクトリを除く)
        //hashmap.keysetでできるかも
        for(String pathDir:DataStore.memoryPathAbs.keySet()){
            if(pathDir.equals(pathDirThis)) continue;
            for(String path:DataStore.memoryPathAbs.get(pathDir)){
                for(Triplets<String, String, ClassInfomation> tripletsCI:DataStore.memoryClass.getDataListKey1(path)){
                    //自クラス、extends, implementは飛ばす

                    for(String name:tripletsCI.getRightValue().getMemoryF().keySet()){
                        if(name.equals(target)){
                            FieldInfomation FI = tripletsCI.getRightValue().getMemoryF().get(name);
                            return new Triplets(path,tripletsCI.rightValue, FI);
                        }
                    }
                }
            }
        }
        return null;
    }

    //定義場所が、今探している範囲よりも前であるかどうかをチェック
    //range1:定義場所、range2:探している範囲
    public static boolean rangeDefinitionCheck(Range range1, Range range2){
        if(range1.begin.line < range2.begin.line) return true;
        else if((range1.begin.line == range2.begin.line) && range1.begin.column <= range2.begin.column)
            return true;
        else return false;
    }

    //今探している範囲を含むスコープであるかどうかを確認する
    //range1:今見ている情報群のスコープ、range2:探している範囲
    public static boolean rangeScopeCheck(Range range1, Range range2){
        if((range1.begin.line <= range2.begin.line) && (range1.end.line >= range2.end.line))
            return true;
        else return false;
    }
}
