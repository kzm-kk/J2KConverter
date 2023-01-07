import com.github.javaparser.Range;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.type.Type;

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

    public static boolean CheckMethod(Type[] types, MethodInformation MI){
        boolean checkFlag = true;
        if(types.length != MI.paramTypes.length) checkFlag = false;
        return checkFlag;
    }

    public static Type[] ParamToTypes(NodeList<Parameter> parameters){
        int size = parameters.size();
        Type[] types = new Type[size];
        for(int i = 0;i < size;i++){
            types[i] = parameters.get(i).getType();
        }
         return types;
    }

    public static String searchImport(String identifier, String pathAbs){
        if(memoryImport.get(pathAbs) == null) return null;
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

    public static String structureChangeCheck(Expression expression, String structure, String contents){
        if(expression.isObjectCreationExpr()) return structure + "/" + contents;
        else if(expression.isLambdaExpr()) return structure + "/" + contents;
        else return structure;
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

    public static class ConvertFor_i{
        String pathAbs;
        String structure;
        Range range;
        boolean isUse = false;

        public ConvertFor_i(){
            pathAbs = "";
            structure = "";
            range = null;
        }

        public boolean UseCheck(String pathAbs, String structure, ArrayDeque<Range> ranges){
            if(this.pathAbs.equals(pathAbs)){
                if(this.structure.equals(structure)){
                    if(ranges.peekLast() != null) {
                        Range range1 = ranges.peekLast();
                        if (this.range.begin.line < range1.begin.line && this.range.end.line > range1.end.line) {
                            isUse = true;
                        } else isUse = false;
                    } else isUse = false;
                } else isUse = false;
            } else isUse = false;
            if(!isUse){
                this.pathAbs = pathAbs;
                this.structure = structure;
                ranges.pollLast();
                this.range = ranges.peekLast();
            }
            return isUse;
        }
    }

}
