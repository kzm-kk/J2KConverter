import com.github.javaparser.Range;
import com.github.javaparser.ast.ImportDeclaration;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;

public class DataStore {
    public static String sourceRootPathName;
    public static HashMap<String, String> memoryClassLibrary;

    //key:pathAbs, value:ArrayList<ImportDeclaration>
    public static HashMap<String, ArrayList<ImportDeclaration>> memoryImport;
    public static HashMap<String, Boolean> memoryImportAlreadyOutput;
    public static void initMemoryAlreadyOutput(String pathAbs){
        memoryImportAlreadyOutput.putIfAbsent(pathAbs, false);
    }

    //key:pathDir, value:ArrayList<pathAbs>
    public static HashMap<String, ArrayList<String>> memoryPathAbs;
    //key1:pathAbs, key2:classname
    public static TwinKeyDataList<String, String, ClassInformation> memoryClass;

    public static TwinKeyDataList<String, String, ClassTemporaryStore> memoryBeforeCheck;

    //各ファイルにcompanion objectを作るためのimport kotlin.jvm.JvmStaticが必要かどうか
    //key:pathAbs, value:boolean
    public static HashMap<String, Boolean> memoryStatic;

    public static String searchImport(String identifier, String pathAbs){
        for(ImportDeclaration ID:memoryImport.get(pathAbs)){
            if(ID.getName().getIdentifier().equals(identifier)){
                return ID.getNameAsString();
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

    public static ClassInformation ClassCheck(String pathDir, String name){
        String pathName = pathDir + "/" + name + ".java";
        ClassInformation CI = DataStore.memoryClass.getData(pathName, name);
        if(CI != null) {
            return CI;
        } else {
            return null;
        }
    }

    public static void init(){
        sourceRootPathName = "";
        memoryClassLibrary = new HashMap<>();
        memoryImport = new HashMap<>();
        memoryImportAlreadyOutput = new HashMap<>();
        memoryPathAbs = new HashMap<>();
        memoryClass = new TwinKeyDataList<>();
        memoryBeforeCheck = new TwinKeyDataList<>();
        memoryStatic = new HashMap<>();
    }

}
