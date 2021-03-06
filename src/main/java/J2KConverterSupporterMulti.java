import com.github.javaparser.ParseResult;
import com.github.javaparser.Range;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
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
            VoidVisitor<?> visitor = new FirstVisitor("");
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

            DataStore.memory_field_info.put(classname, memory_field_Tmp);
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
                //static初期化子チェック
                for (InitializerDeclaration id : DataStore.memory_Initializer.get(classname)) {
                    InitializerVisitor initializerVisitor = new InitializerVisitor(classname);
                    for(AnnotationExpr annotationExpr:id.getAnnotations()){
                        if(annotationExpr.getNameAsString().equals("static"))
                            id.accept(initializerVisitor, null);
                    }
                }
                //通常の初期化子チェック
                NonStatic:for (InitializerDeclaration id : DataStore.memory_Initializer.get(classname)) {
                    InitializerVisitor initializerVisitor = new InitializerVisitor(classname);
                    for(AnnotationExpr annotationExpr:id.getAnnotations()){
                        if(annotationExpr.getNameAsString().equals("static"))
                            continue NonStatic;
                    }
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
            //OutputFieldInfo(classname);
            //OutputMethodInfo(classname);
        }

        //変換フェーズ

        for(String name:DataStore.memory_classlibrary){
            System.out.println(name);
        }

    }

    //デバッグ用の出力メソッド

    private static void OutputFieldInfo(String classname){
        System.out.println("\nfield information:" + classname);
        for(String fieldname:DataStore.memory_field_info.get(classname).keySet()){
            String type = DataStore.memory_field_info.get(classname).get(fieldname).get("type");
            String dim = DataStore.memory_field_info.get(classname).get(fieldname).get("dim");
            String assign = DataStore.memory_field_info.get(classname).get(fieldname).get("assign");
            String nullable = DataStore.memory_field_info.get(classname).get(fieldname).get("nullable");
            String isStatic = DataStore.memory_field_info.get(classname).get(fieldname).get("static");
            String initializable = DataStore.memory_field_info.get(classname).get(fieldname).get("initializable");
            String formerRW = DataStore.memory_field_info.get(classname).get(fieldname).get("formerRW");
            System.out.println(fieldname + " type:" + type +" dim:" + dim
                    + " assign:" + assign + " nullable:" + nullable + " static:" + isStatic
                    + " initializable:" + initializable + " formerRW:" + formerRW);
        }
    }
    private static void OutputMethodInfo(String classname){
        System.out.println("\nmethod information:" + classname);
        for(String mname:DataStore.memory_method_info.get(classname).keySet()){
            boolean flag = (boolean)DataStore.memory_method_info.get(classname).get(mname).get("static");
            boolean flag2 = (boolean)DataStore.memory_method_info.get(classname).get(mname).get("access");
            boolean flag3 = (boolean)DataStore.memory_method_info.get(classname).get(mname).get("fix");
            String field = (String)DataStore.memory_method_info.get(classname).get(mname).get("field");
            int lines = (int)DataStore.memory_method_info.get(classname).get(mname).get("lines");
            boolean flag4 = (boolean)DataStore.memory_method_info.get(classname).get(mname).get("nullable");
            String type = (String)DataStore.memory_method_info.get(classname).get(mname).get("type");
            System.out.println(mname + " static:" + flag +" access:" + flag2 + " fix:" + flag3
                    + " field:" + field + " lines:" + lines + " nullable:" + flag4 + " type:" + type);
        }
        System.out.println("\nmethod local information");
        if(DataStore.memory_localValue_info.get(classname) != null) {
            for (Range range : DataStore.memory_localValue_info.get(classname).keySet()) {
                for (String fieldname : DataStore.memory_localValue_info.get(classname).get(range).keySet()) {
                    String assign = DataStore.memory_localValue_info.get(classname).get(range).get(fieldname).get("assign");
                    String nullable = DataStore.memory_localValue_info.get(classname).get(range).get(fieldname).get("nullable");
                    System.out.println(fieldname + " assign:" + assign + " nullable:" + nullable);
                }
            }
        }
    }

    //FirstVisitor,SomeVisitor:情報収集用のvisitor
    private static class FirstVisitor extends VoidVisitorAdapter<Void> {
        String classname = "";
        ArrayList<ImportDeclaration> Import_list = new ArrayList<>();
        ArrayList<FieldDeclaration> fds = new ArrayList<>();
        ArrayList<String> inner_list = new ArrayList<>();

        public FirstVisitor(String classname){
            this.classname = classname;
        }

        @Override
        public void visit(ImportDeclaration md, Void arg){
            super.visit(md, arg);
            Import_list.add(md);
        }

        @Override
        public void visit(ClassOrInterfaceDeclaration md, Void arg){
            String classname = md.getNameAsString();
            DataStore.memory_import.put(classname, Import_list);

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

            for (BodyDeclaration<?> bodyDeclaration : md.getMembers()) {
                SomeVisitor visitor = new SomeVisitor(classname);
                bodyDeclaration.accept(visitor, null);
            }
        }

    }

    private static class SomeVisitor extends VoidVisitorAdapter<Void> {
        String classname = "";
        ArrayList<FieldDeclaration> fds = new ArrayList<>();
        ArrayList<String> inner_list = new ArrayList<>();
        List<InitializerDeclaration> initializer_list = new ArrayList<>();

        public SomeVisitor(String classname){
            this.classname = classname;
            initializer_list = new ArrayList<>();
        }

        @Override
        public void visit(ClassOrInterfaceDeclaration md, Void arg) {
            if (DataStore.memory_innerclass.get(classname) == null) inner_list = new ArrayList<>();
            else inner_list = DataStore.memory_innerclass.get(classname);
            inner_list.add(md.getNameAsString());
            DataStore.memory_innerclass.put(classname, inner_list);
            FirstVisitor visitor = new FirstVisitor(md.getNameAsString());
            md.accept(visitor, null);
        }

        @Override
        public void visit(InitializerDeclaration md, Void arg){
            DataStore.isStaticI = DataStore.isStaticI || md.isStatic();
            List<InitializerDeclaration> initializer_list
                    = DataStore.memory_Initializer.get(classname);
            if(initializer_list != null)
                this.initializer_list = initializer_list;
            this.initializer_list.add(md);
            DataStore.memory_Initializer.put(classname, this.initializer_list);
        }

        @Override
        public void visit(EnumDeclaration md, Void arg){
            String classname = md.getNameAsString();
            DataStore.memory_classname.add(classname);
            DataStore.memory_enum.add(classname);
            fds = new ArrayList<>();
            for (FieldDeclaration field : md.getFields()) {
                fds.add(field);
            }
            DataStore.memory_classfield.put(classname, fds);
            DataStore.memory_classmethod.put(classname, md.getMethods());
            DataStore.memory_constructor.put(classname, md.getConstructors());
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
            String initializable = "true";
            String formerRW = "";

            if(md.getInitializer().isEmpty()) {
                initializable = "false";
                //formerRW = "R";
            } else formerRW = "W";

            
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
            data.put("initializable", initializable);
            data.put("formerRW", formerRW);

            memory_field_Tmp.put(fieldname, data);

        }
    }

    private static class ConstructorVisitor extends VoidVisitorAdapter<Void>{
        String classname = "";
        ArrayList<String> paramList = new ArrayList<>();

        public ConstructorVisitor(String name){
            classname = name;
        }

        @Override
        public void visit(ConstructorDeclaration md, Void arg){
            for(Parameter tmp:md.getParameters()){
                paramList.add(tmp.getNameAsString());
            }

            Range range = md.getRange().get();
            initLocalValueList(classname, range);

            InBlockVisitorS visitor = new InBlockVisitorS(classname, range, paramList);
            md.getBody().accept(visitor, null);
        }

    }

    //動作的にはなくてもいいけど読みやすくするためにあえてVisitorを入れてます
    private static class InitializerVisitor extends VoidVisitorAdapter<Void>{
        String classname = "";

        public InitializerVisitor(String name){
            classname = name;
        }

        @Override
        public void visit(InitializerDeclaration md, Void arg){
            Range range = md.getRange().get();
            initLocalValueList(classname, range);

            InBlockVisitorS visitor = new InBlockVisitorS(classname, range);
            md.getBody().accept(visitor, null);
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
        String type = "";
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
            type = md.getTypeAsString();
            Range range = md.getRange().get();

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

            if(md.getType().isArrayType()){
                type = type.replace("[]", "").concat("-array");
            }

            if(md.getBody().isPresent())
                numLine = md.getBody().get().getChildNodes().size();

            initLocalValueList(classname, range);

            boolean isReturnNull = false;
            if(md.getBody().isPresent()) {
                InBlockVisitorS visitor = new InBlockVisitorS(classname, methodname, range, isAccessorCheckG, isAccessorCheckS, paramList);
                md.getBody().get().accept(visitor, null);
                fieldname = visitor.getFieldname();
                isFixableG = visitor.getIsFixableG();
                isFixableS = visitor.getIsFixableS();
                isReturnNull = visitor.getIsReturnNull();
            }

            setData(isFixableS||isFixableG, fieldname, range, isReturnNull);
        }

        private void setData(boolean flag, String fieldname, Range range, boolean isReturnNull){
            HashMap<String, Object> tmpHash = new HashMap<>();
            tmpHash.put("static", isStatic);
            tmpHash.put("access", (isAccessorCheckG || isAccessorCheckS));
            tmpHash.put("fix", flag);
            tmpHash.put("field", fieldname);
            tmpHash.put("lines", numLine);
            tmpHash.put("nullable", isReturnNull);
            tmpHash.put("type", type);
            tmpHash.put("range", range);
            memory_method_Tmp.put(methodname, tmpHash);
        }

    }

    public static class InBlockVisitorS extends VoidVisitorAdapter<Void>{
        String classname = "";
        String methodname = "";
        Range range = null;
        private String fieldname = "";
        private boolean isAccessorCheckG = false;
        private boolean isAccessorCheckS = false;
        private boolean isFixableG = false;
        private boolean isFixableS = false;
        private boolean isReturnNull = false;
        ArrayList<String> paramList = new ArrayList<>();

        //Method用のコンストラクタ
        public InBlockVisitorS
                (String classname, String methodname, Range range,
                 boolean isAccessorCheckG, boolean isAccessorCheckS, ArrayList<String> paramList){
            this.classname = classname;
            this.methodname = methodname;
            this.range = range;
            this.isAccessorCheckG = isAccessorCheckG;
            this.isAccessorCheckS = isAccessorCheckS;
            this.paramList = paramList;
        }

        //Constructor用のコンストラクタ
        public InBlockVisitorS(String classname, Range range, ArrayList<String> paramList){
            this(classname, "", range, false, false, paramList);
        }

        //Initializer用のコンストラクタ
        public InBlockVisitorS(String classname, Range range){
            this(classname, "", range, false, false, new ArrayList<String>());
        }

        @Override
        public void visit(VariableDeclarator md, Void arg){
            HashMap<String,String> data = new HashMap<>();
            String fieldname = md.getNameAsString();
            String type = md.getTypeAsString();
            String dim = "0";
            String assign = "false";
            String nullable = "false";
            String initializable = "true";
            String formerRW = "";

            if(md.getInitializer().isEmpty()) {
                assign = "true";
                nullable = "true";
                initializable = "false";
                formerRW = "R";
            } else formerRW = "W";

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
            data.put("initializable", initializable);
            data.put("formerRW", formerRW);

            DataStore.memory_localValue_info.get(classname).get(range).put(fieldname, data);
        }

        @Override
        public void visit(AssignExpr md, Void arg){
            //式の右側の解析
            AssignVisitorS valueVisitor = new AssignVisitorS(classname, methodname, range, isAccessorCheckG, isAccessorCheckS, paramList, false);
            md.getValue().accept(valueVisitor, null);

            //式の左側の解析
            String value = md.getValue().toString();

            //代入以外のAssign文の場合、一度値を参照してから代入するので、先にReadのチェック
            if (!md.getOperator().toString().equals("ASSIGN")) {
                AssignVisitorS targetVisitor = new AssignVisitorS(classname, methodname, range, isAccessorCheckG, isAccessorCheckS, paramList, false, value);
                md.getTarget().accept(targetVisitor, null);
            }
            AssignVisitorS targetVisitor = new AssignVisitorS(classname, methodname, range, isAccessorCheckG, isAccessorCheckS, paramList, true, value);
            md.getTarget().accept(targetVisitor, null);
            isFixableS = targetVisitor.getIsFixableS();
            fieldname = targetVisitor.getFieldname();
        }

        @Override
        public void visit(ReturnStmt md, Void arg){
            if(md.getExpression().isPresent()) {
                AssignVisitorS visitor = new AssignVisitorS(classname, methodname, range, isAccessorCheckG, isAccessorCheckS, paramList, false);
                md.getExpression().get().accept(visitor, null);
                isFixableG = visitor.getIsFixableG();
                fieldname = visitor.getFieldname();
                isReturnNull = visitor.getIsReturnNull();
            }
        }

        @Override
        public void visit(MethodCallExpr md, Void arg){
            //メソッド呼び出しの引数に使われているかどうかを確認
            //ここでは何もせず、visitorに明け渡す
            AssignVisitorS visitor = new AssignVisitorS(classname, methodname, range, isAccessorCheckG, isAccessorCheckS, paramList, false);
            md.accept(visitor, null);

        }

        @Override
        public void visit(DoStmt md, Void arg){
            //do-whileのconditionにあたる部分の読み込み変数のチェックとブロック文内部の確認

            //conditionチェック
            AssignVisitorS conditionVisitor = new AssignVisitorS(classname, methodname, range, isAccessorCheckG, isAccessorCheckS, paramList, false);
            md.getCondition().accept(conditionVisitor, null);

            //内部チェック
            AssignVisitorS bodyVisitor = new AssignVisitorS(classname, methodname, range, isAccessorCheckG, isAccessorCheckS, paramList, false);
            md.getBody().accept(bodyVisitor, null);
        }

        @Override
        public void visit(WhileStmt md, Void arg){
            //whileのconditionにあたる部分の読み込み変数のチェックとブロック文内部の確認

            //conditionチェック
            AssignVisitorS conditionVisitor = new AssignVisitorS(classname, methodname, range, isAccessorCheckG, isAccessorCheckS, paramList, false);
            md.getCondition().accept(conditionVisitor, null);

            //内部チェック
            AssignVisitorS bodyVisitor = new AssignVisitorS(classname, methodname, range, isAccessorCheckG, isAccessorCheckS, paramList, false);
            md.getBody().accept(bodyVisitor, null);
        }

        @Override
        public void visit(ForStmt md, Void arg){
            //forのinitial, compare, updateにあたる部分の読み込み変数のチェックとブロック文内部の確認

            //initialチェック
            for(Expression expression:md.getInitialization()) {
                AssignVisitorS initVisitor = new AssignVisitorS(classname, methodname, range, isAccessorCheckG, isAccessorCheckS, paramList, true);
                expression.accept(initVisitor, null);
            }

            //compareチェック
            if(md.getCompare().isPresent()) {
                AssignVisitorS compareVisitor = new AssignVisitorS(classname, methodname, range, isAccessorCheckG, isAccessorCheckS, paramList, false);
                md.getCompare().get().accept(compareVisitor, null);
            }

            //updateチェック
            for(Expression expression:md.getUpdate()){
                AssignVisitorS updateVisitor = new AssignVisitorS(classname, methodname, range, isAccessorCheckG, isAccessorCheckS, paramList, false);
                expression.accept(updateVisitor, null);
            }

            //内部チェック
            if(md.getBody().isBlockStmt()){
                InBlockVisitorS inBlockVisitor = new InBlockVisitorS(classname, methodname, range, isAccessorCheckG, isAccessorCheckS, paramList);
                md.getBody().accept(inBlockVisitor, null);
            } else {
                AssignVisitorS inVisitor = new AssignVisitorS(classname, methodname, range, isAccessorCheckG, isAccessorCheckS, paramList, false);
                md.getBody().accept(inVisitor, null);
            }

        }

        @Override
        public void visit(ForEachStmt md, Void arg){
            //foreachのiterableにあたる部分の読み込み変数のチェックとブロック文内部の確認
        }

        @Override
        public void visit(IfStmt md, Void arg){
            //ifのconditionにあたる部分の読み込み変数のチェックとブロック文内部の確認

            //conditionチェック
            AssignVisitorS conditionVisitor = new AssignVisitorS(classname, methodname, range, isAccessorCheckG, isAccessorCheckS, paramList, false);
            md.getCondition().accept(conditionVisitor, null);

            //内部チェック
            if(md.getThenStmt().isBlockStmt()){
                InBlockVisitorS thenBlockVisitor = new InBlockVisitorS(classname, methodname, range, isAccessorCheckG, isAccessorCheckS, paramList);
                md.getThenStmt().accept(thenBlockVisitor, null);
            } else {
                AssignVisitorS thenVisitor = new AssignVisitorS(classname, methodname, range, isAccessorCheckG, isAccessorCheckS, paramList, false);
                md.getThenStmt().accept(thenVisitor, null);
            }

            //elseチェック
            if(md.getElseStmt().isPresent()){
                if(md.getElseStmt().get().isIfStmt()){
                    InBlockVisitorS elseIfVisitor = new InBlockVisitorS(classname, methodname, range, isAccessorCheckG, isAccessorCheckS, paramList);
                    md.getElseStmt().get().accept(elseIfVisitor, null);
                } else {
                    AssignVisitorS elseVisitor = new AssignVisitorS(classname, methodname, range, isAccessorCheckG, isAccessorCheckS, paramList, false);
                    md.getElseStmt().get().accept(elseVisitor, null);
                }
            }
        }

        @Override
        public void visit(SwitchStmt md, Void arg){
            //switchのselectorにあたる部分の読み込み変数のチェックとブロック文内部の確認
            AssignVisitorS selectorVisitor = new AssignVisitorS(classname, methodname, range, isAccessorCheckG, isAccessorCheckS, paramList, false);
            md.getSelector().accept(selectorVisitor, null);

            for(SwitchEntry entry:md.getEntries()){
                for(Statement statement:entry.getStatements()){
                    InBlockVisitorS statementVisitor = new InBlockVisitorS(classname, methodname, range, isAccessorCheckG, isAccessorCheckS, paramList);
                    statement.accept(statementVisitor, null);
                }
            }

        }

        @Override
        public void visit(SwitchExpr md, Void arg){
            //switchのselectorにあたる部分の読み込み変数のチェックとブロック文内部の確認
            AssignVisitorS selectorVisitor = new AssignVisitorS(classname, methodname, range, isAccessorCheckG, isAccessorCheckS, paramList, false);
            md.getSelector().accept(selectorVisitor, null);

            for(SwitchEntry entry:md.getEntries()){
                for(Statement statement:entry.getStatements()){
                    InBlockVisitorS statementVisitor = new InBlockVisitorS(classname, methodname, range, isAccessorCheckG, isAccessorCheckS, paramList);
                    statement.accept(statementVisitor, null);
                }
            }
        }

        public String getFieldname(){
            return this.fieldname;
        }

        public boolean getIsFixableG(){
            return this.isFixableG;
        }

        public boolean getIsFixableS(){
            return this.isFixableS;
        }

        public boolean getIsReturnNull(){
            return this.isReturnNull;
        }

    }

    public static class AssignVisitorS extends VoidVisitorAdapter<Void> {
        String classname = "";
        String methodname = "";
        Range range = null;
        private String fieldname = "";
        private boolean isAccessorCheckG = false;
        private boolean isAccessorCheckS = false;
        private boolean isFixableG = false;
        private boolean isFixableS = false;
        ArrayList<String> paramList = new ArrayList<>();
        private boolean isWriteCheck = false;
        private String nullCheckValue = "";
        private boolean isReturnNull = false;

        public AssignVisitorS
        (String classname, String methodname, Range range, 
         boolean isAccessorCheckG, boolean isAccessorCheckS, ArrayList<String> paramList, boolean flag, String value) {
            this.classname = classname;
            this.methodname = methodname;
            this.range = range;
            this.isAccessorCheckG = isAccessorCheckG;
            this.isAccessorCheckS = isAccessorCheckS;
            this.paramList = paramList;
            this.isWriteCheck = flag;
            this.nullCheckValue = value;
        }

        public AssignVisitorS
                (String classname, String methodname, Range range,
                 boolean isAccessorCheckG, boolean isAccessorCheckS, ArrayList<String> paramList, boolean flag) {
            this(classname, methodname, range, isAccessorCheckG, isAccessorCheckS, paramList, flag, "");
        }

        @Override
        public void visit(BlockStmt md, Void arg){
            for(Statement statement:md.getStatements()){
                AssignVisitorS visitor = new AssignVisitorS(classname, methodname, range, isAccessorCheckG, isAccessorCheckS, paramList, isWriteCheck, nullCheckValue);
                statement.accept(visitor, null);
            }
        }

        @Override
        public void visit(BinaryExpr md, Void arg){
            //計算式に使われている変数の確認
            //最左分岐なので左側解析
            AssignVisitorS leftVisitor = new AssignVisitorS(classname, methodname, range, isAccessorCheckG, isAccessorCheckS, paramList, isWriteCheck, nullCheckValue);
            md.getLeft().accept(leftVisitor, null);

            //右側解析
            AssignVisitorS rightVisitor = new AssignVisitorS(classname, methodname, range, isAccessorCheckG, isAccessorCheckS, paramList, isWriteCheck, nullCheckValue);
            md.getRight().accept(rightVisitor, null);
        }

        @Override
        public void visit(UnaryExpr md, Void arg){
            AssignVisitorS visitor = new AssignVisitorS(classname, methodname, range, isAccessorCheckG, isAccessorCheckS, paramList, false);
            md.getExpression().accept(visitor, null);
        }

        @Override
        public void visit(EnclosedExpr md, Void arg){
            super.visit(md, arg);
        }

        @Override
        public void visit(CastExpr md, Void arg){
            super.visit(md, arg);
        }

        @Override
        public void visit(MethodCallExpr md, Void arg){
            //メソッド呼び出しの引数を確認
            if(md.getArguments().size() != 0){
                for(Expression expression:md.getArguments()){
                    AssignVisitorS visitor = new AssignVisitorS(classname, methodname, range, isAccessorCheckG, isAccessorCheckS, paramList, false);
                    expression.accept(visitor, null);
                }
            }
        }


        @Override
        public void visit(ObjectCreationExpr md, Void arg){
            //インスタンス生成に必要な引数の部分の解析
        }

        @Override
        public void visit(ArrayInitializerExpr md, Void arg){
            //要素ありの配列初期化の際の、要素の解析
        }

        @Override
        public void visit(NameExpr md, Void arg){
            //変数(スコープなし)の確認
            if(isWriteCheck) variableWriteCheck(md.toString(), nullCheckValue);
            else variableReadCheck(md.toString());
        }

        @Override
        public void visit(FieldAccessExpr md, Void arg){
            //変数の直接参照の確認、計算式とかからもこっちに飛んでくる
            if(isWriteCheck) variableWriteCheck(md.toString(), nullCheckValue);
            else variableReadCheck(md.toString());
        }

        @Override
        public void visit(NullLiteralExpr md, Void arg){
            isReturnNull = true;
        }

        private void variableReadCheck(String target){
            boolean duplicateFlag = false;
            //自クラス以外の使用を見る
            String[] scopes = target.split("\\.");
            //local,param,fieldの重複を探る。scopeの最も左の値を探る
            if (DataStore.memory_field_info.get(this.classname).get(scopes[0]) == null) //fieldが存在していない
                duplicateFlag = false;
            else if (scopes[0].equals("this"))//最初がthisから始まるアクセス記述
                duplicateFlag = false;
            else {
                duplicateFlag = duplicateCheck(scopes[0]);//重複する場合はtrue,しないならfalse
            }

            int countScope = 0;
            int indexBeforeArray = 0;
            String classname = this.classname;
            String valName = "";
            do {
                indexBeforeArray = scopes[countScope].indexOf("[");
                if (indexBeforeArray < 0) indexBeforeArray = scopes[countScope].length();
                valName = scopes[countScope].substring(0, indexBeforeArray);

                if(DataStore.memory_localValue_info.get(classname) != null) {
                    if (DataStore.memory_localValue_info.get(classname).get(range) != null) {
                        for(String local:DataStore.memory_localValue_info.get(classname).get(range).keySet()){
                            if(valName.equals(local)){
                                if (DataStore.memory_localValue_info.get(classname).get(range).get(local).get("formerRW").equals("R")
                                        && DataStore.memory_localValue_info.get(classname).get(range).get(local).get("initializable").equals("false"))
                                    DataStore.memory_localValue_info.get(classname).get(range).get(local).put("nullable", "true");

                                if(DataStore.memory_localValue_info.get(classname).get(range).get(local).get("nullable").equals("true"))
                                    isReturnNull = true;
                            }
                        }
                    }
                }

                if(DataStore.memory_field_info.get(classname) != null) {
                    for(String fieldname:DataStore.memory_field_info.get(classname).keySet()){
                        if(valName.equals(fieldname)){
                            if(!duplicateFlag || scopes[0].equals("this")) {
                                if (DataStore.memory_field_info.get(classname).get(fieldname).get("formerRW").equals("")
                                        && DataStore.memory_field_info.get(classname).get(fieldname).get("initializable").equals("false")
                                        && DataStore.memory_field_info.get(classname).get(fieldname).get("assign").equals("false") ) {
                                    DataStore.memory_field_info.get(classname).get(fieldname).put("formerRW", "R");
                                    DataStore.memory_field_info.get(classname).get(fieldname).put("nullable", "true");
                                }

                                if(DataStore.memory_field_info.get(classname).get(fieldname).get("nullable").equals("true"))
                                    isReturnNull = true;

                                if (isAccessorCheckG) {
                                    isFixableG = true;
                                    this.fieldname = fieldname;
                                }
                            }
                        }
                    }
                }

                countScope++;
                if (DataStore.memory_field_info.get(classname).get(valName) != null)
                    classname = DataStore.memory_field_info.get(classname).get(valName).get("type");
                if (classname.contains("-array"))
                    classname = classname.replace("-array", "");
            } while (countScope < scopes.length);
        }

        private void variableWriteCheck(String target, String value){
            boolean duplicateFlag = false;
            //自クラス以外の使用を見る
            String[] scopes = target.split("\\.");
            //local,param,fieldの重複を探る。scopeの最も左の値を探る
            if (DataStore.memory_field_info.get(this.classname).get(scopes[0]) == null) //fieldが存在していない
                duplicateFlag = false;
            else if (scopes[0].equals("this"))//最初がthisから始まるアクセス記述
                duplicateFlag = false;
            else {
                duplicateFlag = duplicateCheck(scopes[0]);//重複する場合はtrue,しないならfalse
            }

            int countScope = 0;
            int indexBeforeArray = 0;
            String classname = this.classname;
            String valName = "";
            do {
                indexBeforeArray = scopes[countScope].indexOf("[");
                if (indexBeforeArray < 0) indexBeforeArray = scopes[countScope].length();
                valName = scopes[countScope].substring(0, indexBeforeArray);

                //ローカル変数の使用箇所を見る
                if (DataStore.memory_localValue_info.get(classname) != null) {
                    if (DataStore.memory_localValue_info.get(classname).get(range) != null) {
                        for (String local : DataStore.memory_localValue_info.get(classname).get(range).keySet()) {
                            if (valName.equals(local)) {
                                DataStore.memory_localValue_info.get(classname).get(range).get(local).put("assign", "true");
                                if (value.equals("null"))
                                    DataStore.memory_localValue_info.get(classname).get(range).get(local).put("nullable", "true");
                            }
                        }
                    }
                }


                //変数(フィールド)の使用箇所のうち、自クラスのものを見る
                for (String fieldname : DataStore.memory_field_info.get(classname).keySet()) {
                    if (valName.equals(fieldname)) {
                        if (!duplicateFlag || scopes[0].equals("this")) {
                            DataStore.memory_field_info.get(classname).get(fieldname).put("assign", "true");

                            if (DataStore.memory_field_info.get(classname).get(fieldname).get("formerRW").equals("")
                                    && DataStore.memory_field_info.get(classname).get(fieldname).get("initializable").equals("false")) {
                                DataStore.memory_field_info.get(classname).get(fieldname).put("formerRW", "W");
                            }

                            if (value.equals("null"))
                                DataStore.memory_field_info.get(classname).get(fieldname).put("nullable", "true");

                            if (isAccessorCheckS) {
                                for (String paramName : paramList) {
                                    if (scopes[0].equals("this")) {
                                        if (scopes[1].equals(paramName) && value.equals(paramName)) {
                                            isFixableS = true;
                                            this.fieldname = fieldname;
                                        }
                                    } else {
                                        if (scopes[0].equals(paramName) && value.equals(paramName)) {
                                            isFixableS = true;
                                            this.fieldname = fieldname;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                countScope++;
                if (DataStore.memory_field_info.get(classname).get(valName) != null)
                    classname = DataStore.memory_field_info.get(classname).get(valName).get("type");
                if (classname.contains("-array"))
                    classname = classname.replace("-array", "");
            } while (countScope < scopes.length);
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

        public String getFieldname(){
            return this.fieldname;
        }

        public boolean getIsFixableG(){
            return this.isFixableG;
        }

        public boolean getIsFixableS(){
            return this.isFixableS;
        }

        public boolean getIsReturnNull(){
            return this.isReturnNull;
        }

    }

    public static void initLocalValueList(String classname, Range range){
        if(DataStore.memory_localValue_info.get(classname) == null) {
            DataStore.memory_localValue_info.put(classname, new HashMap<>());
        }
        if (DataStore.memory_localValue_info.get(classname).get(range) == null){
            DataStore.memory_localValue_info.get(classname).put(range, new HashMap<>());
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

}