import com.github.javaparser.ParseResult;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.visitor.GenericVisitorAdapter;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.utils.SourceRoot;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class J2KConverterSupporterMulti {

    public static HashMap<String, HashMap<String, String>> memory_field_Tmp = new HashMap<>();
    public static HashMap<String, HashMap<String, Object>> memory_method_Tmp = new HashMap<>();
    public static ArrayList<InitializerDeclaration> initializer_list = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        DataStore.init();

        String path_root = "/Users/kzm0308/IdeaProjects/J2KConverter/src/main/java/pac1";
        if(args.length == 0)
            path_root = DataStore.pathName;
        else path_root = args[0];
        SourceRoot root = new SourceRoot(Paths.get(path_root));
        List<ParseResult<CompilationUnit>> cu2 = root.tryToParse("");

        //情報収集フェーズ(構文解析&情報切り出し)
        for(ParseResult<CompilationUnit> result:cu2){
            VoidVisitor<?> visitor = new FirstVisitor();
            result.getResult().get().accept(visitor, null);
        }

        //判断フェーズ(変数のvar/val、get/set判断、変数名/メソッド名の被り)
        for(String classname:DataStore.memory_classname){
            memory_field_Tmp = new HashMap<>();
            memory_method_Tmp = new HashMap<>();
            for(FieldDeclaration fd:DataStore.memory_classfield.get(classname)){
                FieldVisitor fieldVisitor = new FieldVisitor(classname);
                fd.accept(fieldVisitor, null);
            }
            /*
            サブクラスのインスタンス生成時の動きから見る、変数のチェック順番
            ０：フィールドの初期化
            １：スーパークラスのstatic初期化子
            ２：サブクラスのstatic初期化子
            ３：スーパークラスの初期化子
            ４：スーパークラスのコンストラクタ
            ５：サブクラスの初期化子
            ６：サブクラスのコンストラクタ
            ７：サブクラスの他の使用箇所(メソッドとか？)
             */

            if(DataStore.memory_Initializer.get(classname) != null) {
                for (InitializerDeclaration id : DataStore.memory_Initializer.get(classname)) {
                    InitializerVisitor initializerVisitor = new InitializerVisitor(classname);
                    id.accept(initializerVisitor, null);
                }
            }

            for(ConstructorDeclaration cd:DataStore.memory_constructor.get(classname)){
                ConstructorVisitor constructorVisitor = new ConstructorVisitor(classname);
                cd.accept(constructorVisitor, null);
            }


            DataStore.memory_field_info.put(classname, memory_field_Tmp);

            for(MethodDeclaration md:DataStore.memory_classmethod.get(classname)){
                MethodVisitor methodVisitor = new MethodVisitor(classname);
                md.accept(methodVisitor, null);
            }
            DataStore.memory_method_info.put(classname, memory_method_Tmp);
        }

        for(String classname:DataStore.memory_classname){
            //デバッグ用の出力
            System.out.println("class:" + classname);
            System.out.println("field information");
            for(String fieldname:DataStore.memory_field_info.get(classname).keySet()){
                String type = DataStore.memory_field_info.get(classname).get(fieldname).get("type");
                String dim = DataStore.memory_field_info.get(classname).get(fieldname).get("dim");
                String assign = DataStore.memory_field_info.get(classname).get(fieldname).get("assign");
                String nullable = DataStore.memory_field_info.get(classname).get(fieldname).get("nullable");
                String isStatic = DataStore.memory_field_info.get(classname).get(fieldname).get("static");
                System.out.println(fieldname + " type:" + type +" dim:" + dim
                        + " assign:" + assign + " nullable:" + nullable + " static:" + isStatic);
            }
            System.out.println("method information");
            for(String mname:DataStore.memory_method_info.get(classname).keySet()){
                boolean flag = (boolean)DataStore.memory_method_info.get(classname).get(mname).get("static");
                boolean flag2 = (boolean)DataStore.memory_method_info.get(classname).get(mname).get("access");
                System.out.println(mname + " static:" + flag +" access:" + flag2);
            }
            /*for(String methodname:DataStore.memory_localValue_info.get(classname).keySet()){
                for(String fieldname:DataStore.memory_localValue_info.get(classname).get(methodname).keySet()){
                    String assign = DataStore.memory_localValue_info.get(classname).get(methodname).get(fieldname).get("assign");
                    String nullable = DataStore.memory_localValue_info.get(classname).get(methodname).get(fieldname).get("nullable");
                    System.out.println(fieldname + " " + assign + " " + nullable);
                }
            }*/
        }

        //変換フェーズ

        for(String name:DataStore.memory_classlibrary){
            System.out.println(name);
        }

    }

    //FirstVisitor,SomeVisitor:情報収集用のvisitor
    private static class FirstVisitor extends VoidVisitorAdapter<Void> {
        ArrayList<ImportDeclaration> Import_list = new ArrayList<>();

        @Override
        public void visit(ImportDeclaration md, Void arg){
            super.visit(md, arg);
            Import_list.add(md);
        }

        @Override
        public void visit(ClassOrInterfaceDeclaration md, Void arg){
            DataStore.memory_import.put(md.getNameAsString(), Import_list);
            SomeVisitor visitor = new SomeVisitor(md.getNameAsString());
            md.accept(visitor, null);

            DataStore.memory_Initializer.put(md.getNameAsString(), initializer_list);
        }

    }

    private static class SomeVisitor extends VoidVisitorAdapter<Void> {
        String classname = "";
        ArrayList<FieldDeclaration> fds = new ArrayList<>();
        ArrayList<String> inner_list = new ArrayList<>();

        public SomeVisitor(String name){
            classname = name;
            initializer_list = new ArrayList<>();
        }

        @Override
        public void visit(ClassOrInterfaceDeclaration md, Void arg){
            if(classname.equals(md.getNameAsString())){
                DataStore.memory_classname.add(classname);
                int size_extend = md.getExtendedTypes().size();
                int size_implement = md.getImplementedTypes().size();
                if(size_extend != 0){
                    DataStore.memory_extend.put(classname, md.getExtendedTypes().get(0).getNameAsString());
                }
                if(size_implement != 0){
                    ArrayList<String> names = new ArrayList<>();
                    for(int i = 0; i < size_implement ;i++){
                        names.add(md.getImplementedTypes(i).getNameAsString());
                    }
                    DataStore.memory_implement.put(classname, names);
                }

                for (FieldDeclaration field : md.getFields()) {
                        fds.add(field);
                }
                DataStore.memory_classfield.put(classname, fds);

                DataStore.memory_classmethod.put(classname, md.getMethods());
                DataStore.memory_constructor.put(classname, md.getConstructors());

                super.visit(md, arg);
            } else {
                if(DataStore.memory_innerclass.get(classname) == null) inner_list = new ArrayList<>();
                else inner_list = DataStore.memory_innerclass.get(classname);
                inner_list.add(md.getNameAsString());
                DataStore.memory_innerclass.put(classname, inner_list);
                SomeVisitor visitor = new SomeVisitor(md.getNameAsString());
                md.accept(visitor, null);
            }
        }

        @Override
        public void visit(InitializerDeclaration md, Void arg){
            DataStore.isStaticI = DataStore.isStaticI || md.isStatic();
            initializer_list.add(md);
        }
    }

    private static class FieldVisitor extends VoidVisitorAdapter<Void>{
        String classname = "";
        public FieldVisitor(String name){
            classname = name;
        }

        @Override
        public void visit(FieldDeclaration md, Void arg){
            NodeList<Modifier> modifiers = new NodeList<>();
            if(md.getModifiers().size() != 0) modifiers = md.getModifiers();
            for(VariableDeclarator vd: md.getVariables()) {
                VariableVisitor VarVisitor = new VariableVisitor(classname, modifiers);
                vd.accept(VarVisitor, null);
            }
        }
    }

    private static class VariableVisitor extends VoidVisitorAdapter<Void>{
        String classname = "";
        NodeList<Modifier> modifiers = new NodeList<>();

        public VariableVisitor(String name, NodeList<Modifier> modifiers){
            classname = name;
            this.modifiers = modifiers;
        }


        @Override
        public void visit(VariableDeclarator md, Void arg){
            HashMap<String,String> data = new HashMap<>();
            String fieldname = md.getNameAsString();
            String type = md.getTypeAsString();
            String isStatic = "false";
            String dim = "0";
            String assign = "false";
            String nullable = "false";

            if(md.getInitializer().isEmpty()) {
                boolean flag = true;
                CheckConstructor checker = new CheckConstructor(classname, fieldname);

                if(DataStore.memory_constructor.get(classname) != null) {
                    for(ConstructorDeclaration cd:DataStore.memory_constructor.get(classname)){
                        if(cd.accept(checker, null) == null) flag = true;
                        else cd.accept(checker, null);
                        if(!flag){
                            assign = "true";
                            nullable = "true";
                            break;
                        }
                    }
                }
            }

            if(md.getType().isArrayType()){
                int dimNum = type.length();
                type = type.replace("[]", "");
                dimNum = dimNum - type.length();
                dim = Integer.toString(dimNum / 2);
                type = type.concat("-array");
            }

            for(Modifier modifier:modifiers){
                if(modifier.toString().equals("static ")) {
                    DataStore.isStaticF = true;
                    isStatic = "true";
                    break;
                }
            }

            data.put("type", type);
            data.put("static", isStatic);
            data.put("dim", dim);
            data.put("assign", assign);
            data.put("nullable", nullable);

            memory_field_Tmp.put(fieldname, data);

        }
    }

    private static class ConstructorVisitor extends VoidVisitorAdapter<Void>{
        String classname = "";
        ArrayList<String> paramList = new ArrayList<>();

        public ConstructorVisitor(String name){
            this.classname = name;

        }

        @Override
        public void visit(ConstructorDeclaration md, Void arg){

            for(Parameter tmp:md.getParameters()){
                paramList.add(tmp.getNameAsString());
            }

            super.visit(md, arg);
        }

        @Override
        public void visit(AssignExpr md, Void arg){
            String str = md.getTarget().toString();
            String scope = "";
            if(md.getTarget().isFieldAccessExpr()){
                FieldAccessExpr fae = (FieldAccessExpr) md.getTarget();
                scope = fae.getScope().toString();
                str = fae.getNameAsString();
            }
            String target = str;
            String value = md.getValue().toString();
            boolean flag = duplicateCheck(target);

            if(DataStore.memory_field_info.get(this.classname) != null) {

                for (String fieldname : DataStore.memory_field_info.get(this.classname).keySet()) {
                    if (target.equals(fieldname)) {
                        if (!flag || scope.equals("this")) {
                            DataStore.memory_field_info.get(this.classname).get(fieldname).put("assign", "true");

                            if (value.equals("null"))
                                DataStore.memory_field_info.get(this.classname).get(fieldname).put("nullable", "true");
                        }
                    }

                }

                String[] scopes = scope.split("\\.");
                if(scopes.length >= 1){
                    if(DataStore.memory_field_info.get(this.classname).get(scopes[0]) != null) {
                        String scopeClass = DataStore.memory_field_info.get(this.classname).get(scopes[0]).get("type");
                        if (DataStore.memory_field_info.get(scopeClass) != null) {
                            for (String fieldname : DataStore.memory_field_info.get(scopeClass).keySet()) {

                                if (target.equals(fieldname)) {
                                    DataStore.memory_field_info.get(scopeClass).get(fieldname).put("assign", "true");

                                    if (value.equals("null"))
                                        DataStore.memory_field_info.get(scopeClass).get(fieldname).put("nullable", "true");
                                }
                            }
                        }
                    }
                }

            }
        }

        private boolean duplicateCheck(String target){
            if(DataStore.memory_field_info.get(this.classname) != null) {
                for (String fieldname : DataStore.memory_field_info.get(this.classname).keySet()) {
                    for (String local : paramList) {
                        if (fieldname.equals(local) && fieldname.equals(target)) return true;
                    }
                }
            }
            return false;
        }

    }

    private static class InitializerVisitor extends VoidVisitorAdapter<Void>{
        String classname = "";

        public InitializerVisitor(String name){
            classname = name;

        }

        @Override
        public void visit(AssignExpr md, Void arg){
            String str = md.getTarget().toString();
            String scope = "";
            if(md.getTarget().isFieldAccessExpr()){
                FieldAccessExpr fae = (FieldAccessExpr) md.getTarget();
                scope = fae.getScope().toString();
                str = fae.getNameAsString();
            }
            String target = str;
            String value = md.getValue().toString();

            for(String fieldname:DataStore.memory_field_info.get(classname).keySet()){
                if(target.equals(fieldname)){
                    if(scope.equals("this")) {
                        DataStore.memory_field_info.get(classname).get(fieldname).put("assign", "true");

                        if (value.equals("null"))
                            DataStore.memory_field_info.get(classname).get(fieldname).put("nullable", "true");
                    }
                }

            }
        }

    }


    private static class MethodVisitor extends VoidVisitorAdapter<Void>{
        String classname = "";
        String methodname = "";
        String fieldname = "";
        boolean isStatic = false;
        boolean isAccessorCheckG = false;
        boolean isAccessorCheckS = false;
        boolean isFixableG = false;
        boolean isFixableS = false;
        int numLine = 0;
        ArrayList<String> paramList = new ArrayList<>();
        HashMap<String, HashMap<String, String>> memory_local_Tmp = new HashMap<>();


        public MethodVisitor(String name){
            classname = name;

        }

        @Override
        public void visit(MethodDeclaration md, Void arg){
            isAccessorCheckG = search_get(md);
            isAccessorCheckS = search_set(md);
            methodname = md.getNameAsString();

            for(Parameter param:md.getParameters()){
                paramList.add(param.getNameAsString());
            }

            for(Modifier mod:md.getModifiers()){
                if(mod.toString().equals("static ")) {
                    DataStore.isStaticM = true;
                    isStatic = true;
                    break;
                }
            }

            if(md.getBody().isPresent())
                numLine = md.getBody().get().getChildNodes().size();

            if(DataStore.memory_localValue_info.get(classname) == null) {
                DataStore.memory_localValue_info.put(classname, new HashMap<>());
            }
            if (DataStore.memory_localValue_info.get(classname).get(methodname) == null){
                DataStore.memory_localValue_info.get(classname).put(methodname, new HashMap<>());
            }

            super.visit(md, arg);

            setData(isFixableS||isFixableG, fieldname);
        }

        @Override
        public void visit(VariableDeclarator md, Void arg){
            HashMap<String,String> data = new HashMap<>();
            String fieldname = md.getNameAsString();
            String type = md.getTypeAsString();
            String dim = "0";
            String assign = "false";
            String nullable = "false";

            if(md.getInitializer().isEmpty()) {
                assign = "true";
                nullable = "true";
            }

            if(md.getType().isArrayType()){
                int dimNum = type.length();
                type = type.replace("[]", "");
                dimNum = dimNum - type.length();
                dim = Integer.toString(dimNum / 2);
                type = type.concat("-array");
            }

            data.put("type", type);
            data.put("dim", dim);
            data.put("assign", assign);
            data.put("nullable", nullable);

            DataStore.memory_localValue_info.get(classname).get(methodname).put(fieldname, data);


        }

        @Override
        public void visit(AssignExpr md, Void arg){
            String name = "";
            Expression scopeNode = md.getTarget();
            String target = md.getTarget().toString();
            String value = md.getValue().toString();
            boolean flag = false;

            //自クラス以外の使用を見る
            String[] scopes = target.split("\\.");
            //local,param,fieldの重複を探る。scopeの最も左の値を探る
            if(DataStore.memory_field_info.get(this.classname).get(scopes[0]) == null) //fieldが存在していない
                flag = false;
            else if(scopes[0].equals("this"))//最初がthisから始まるアクセス記述
                flag = false;
            else {
                flag = duplicateCheck(scopes[0]);//重複する場合はtrue,しないならfalse
            }

            if(isAccessorCheckS){
                for(String paramName: paramList){
                    if(scopes[0].equals("this")){
                        if(scopes[1].equals(paramName) && value.equals(paramName)){
                            isFixableS = true;
                            this.fieldname = fieldname;
                            //setData(isFixableS, fieldname);
                        }
                    } else {
                        if (scopes[0].equals(paramName) && value.equals(paramName)) {
                            isFixableS = true;
                            this.fieldname = fieldname;
                            //setData(isFixableS, fieldname);
                        }
                    }
                }
            }

            int countScope = 0;
            int indexBeforeArray = 0;
            String classname = this.classname;
            String valName = "";
            do{
                indexBeforeArray = scopes[countScope].indexOf("[");
                if(indexBeforeArray < 0) indexBeforeArray = scopes[countScope].length();
                valName = scopes[countScope].substring(0, indexBeforeArray);

                //ローカル変数の使用箇所を見る
                if(DataStore.memory_localValue_info.get(classname) != null){
                    if(DataStore.memory_localValue_info.get(classname).get(methodname) != null){
                        for(String local:DataStore.memory_localValue_info.get(classname).get(methodname).keySet()){
                            if(valName.equals(local)){
                                DataStore.memory_localValue_info.get(classname).get(methodname).get(local).put("assign", "true");
                                if (value.equals("null"))
                                    DataStore.memory_localValue_info.get(classname).get(methodname).get(local).put("nullable", "true");
                            }
                        }
                    }
                }


                //変数(フィールド)の使用箇所のうち、自クラスのものを見る
                for(String fieldname:DataStore.memory_field_info.get(classname).keySet()){
                    if(valName.equals(fieldname)){
                        if(!flag || target.equals("this")) {
                            DataStore.memory_field_info.get(classname).get(fieldname).put("assign", "true");

                            if (value.equals("null"))
                                DataStore.memory_field_info.get(classname).get(fieldname).put("nullable", "true");
                        }
                    }
                }

                countScope++;
                if(DataStore.memory_field_info.get(classname).get(valName) != null)
                    classname = DataStore.memory_field_info.get(classname).get(valName).get("type");
                if(classname.contains("-array"))
                    classname = classname.replace("-array", "");
                /*
                for(String fieldname:DataStore.memory_field_info.get(classname).keySet()){
                    if(name.equals(fieldname)){
                        if(!flag || target.equals("this")) {
                            DataStore.memory_field_info.get(classname).get(fieldname).put("assign", "true");

                            if (value.equals("null"))
                                DataStore.memory_field_info.get(classname).get(fieldname).put("nullable", "true");
                        }
                    }
                }


                if(scopeNode.isNameExpr()){
                    name = scopeNode.asNameExpr().getNameAsString();
                    scopeNode = null;
                } else if(scopeNode.isArrayAccessExpr()){
                    ArrayAccessExpr arrayAccessExpr = scopeNode.asArrayAccessExpr();
                    if(arrayAccessExpr.getName().isFieldAccessExpr()){
                        FieldAccessExpr fieldAccessExpr = arrayAccessExpr.getName().asFieldAccessExpr();
                        name = fieldAccessExpr.getNameAsString();
                        scopeNode = fieldAccessExpr.getScope();
                    } else if(arrayAccessExpr.getName().isNameExpr()){
                        name = arrayAccessExpr.getName().asNameExpr().getNameAsString();
                        scopeNode = null;
                    }
                } else if(scopeNode.isFieldAccessExpr()){
                    name = scopeNode.asFieldAccessExpr().getNameAsString();
                    scopeNode = scopeNode.asFieldAccessExpr().getScope();
                }

                if(DataStore.memory_field_info.get(this.classname).get(scopes[0]) != null) {
                    String scopeClass = DataStore.memory_field_info.get(this.classname).get(scopes[0]).get("type");
                    if (DataStore.memory_field_info.get(scopeClass) != null) {
                        for (String fieldname : DataStore.memory_field_info.get(scopeClass).keySet()) {

                            if (target.equals(fieldname)) {
                                DataStore.memory_field_info.get(scopeClass).get(fieldname).put("assign", "true");

                                if (value.equals("null"))
                                    DataStore.memory_field_info.get(scopeClass).get(fieldname).put("nullable", "true");
                            }
                        }
                    }
                }*/
            }while(countScope < scopes.length);


            /*if(scopes.length >= 1){
                if(DataStore.memory_field_info.get(this.classname).get(scopes[0]) != null) {
                    String scopeClass = DataStore.memory_field_info.get(this.classname).get(scopes[0]).get("type");
                    if (DataStore.memory_field_info.get(scopeClass) != null) {
                        for (String fieldname : DataStore.memory_field_info.get(scopeClass).keySet()) {

                            if (target.equals(fieldname)) {
                                DataStore.memory_field_info.get(scopeClass).get(fieldname).put("assign", "true");

                                if (value.equals("null"))
                                    DataStore.memory_field_info.get(scopeClass).get(fieldname).put("nullable", "true");
                            }
                        }
                    }
                }
            }*/
        }

        @Override
        public void visit(ReturnStmt md, Void arg){
            if(isAccessorCheckG){
                for(String fieldname:DataStore.memory_field_info.get(classname).keySet()){
                    if(md.getExpression().isPresent()){
                        String str = md.getExpression().get().toString().replace("this.", "");
                        if (str.equals(fieldname)) {
                            isFixableG = true;
                            this.fieldname = fieldname;
                            //setData(isFixableG, fieldname);
                        }
                    }
                }
            }
        }

        private void setData(boolean flag, String fieldname){
            HashMap<String, Object> tmpHash = new HashMap<>();
            tmpHash.put("static", isStatic);
            tmpHash.put("access", (isAccessorCheckG || isAccessorCheckS));
            tmpHash.put("fix", flag);
            tmpHash.put("field", fieldname);
            tmpHash.put("lines", numLine);
            //System.out.println(methodname + " " + flag + " " + numLine + " " + fieldname);
            memory_method_Tmp.put(methodname, tmpHash);
        }

        private boolean duplicateCheck(String target){
            for(String fieldname:DataStore.memory_field_info.get(classname).keySet()){
                for(String local:DataStore.memory_localValue_info.get(classname).get(methodname).keySet()){
                    if(fieldname.equals(local) && fieldname.equals(target)) return true;
                }
                for(String param:paramList){
                    if(fieldname.equals(param) && fieldname.equals(target)) return true;
                }
            }
            return false;
        }

    }

    public static boolean search_get(MethodDeclaration detail){
        String methodname = detail.getNameAsString();
        boolean flag = false;

        if (methodname.matches("get[A-Z].*")) {
            if (detail.getParameters().isEmpty()) {
                String returns = detail.getTypeAsString();
                if (!returns.equals("void")) {
                    flag = true;
                }
            }
        }
        return flag;
    }

    public static boolean search_set(MethodDeclaration detail){
        String methodname = detail.getNameAsString();
        boolean flag = false;
        if (methodname.matches("set[A-Z].*")) {
            int size_param = detail.getParameters().size();
            if (size_param == 1) {
                String returns = detail.getTypeAsString();
                if (returns.equals("void")) {
                    flag = true;
                }
            }
        }
        return flag;
    }

    public static class CheckConstructor extends GenericVisitorAdapter<Boolean,Void> {
        String classname = "";
        String fieldname = "";

        public CheckConstructor(String classname, String fieldname) {
            this.classname = classname;
            this.fieldname = fieldname;
        }

        @Override
        public Boolean visit(AssignExpr md, Void arg){
            return md.getTarget().toString().equals(fieldname);
        }
    }

}
