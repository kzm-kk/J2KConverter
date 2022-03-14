import com.github.javaparser.JavaParser;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.visitor.GenericVisitorAdapter;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class J2KConverterSupporter {

    public static HashMap<String, HashMap<String, String>> memory_field_Tmp = new HashMap<>();
    public static HashMap<String, HashMap<String, Object>> memory_method_Tmp = new HashMap<>();

    public static void main(String[] args) throws IOException {
        DataStore.init();
        /*String path_root = "/Users/kzm0308/IdeaProjects/WarningTool/src";
        SourceRoot root = new SourceRoot(Paths.get(path_root));
        List<ParseResult<CompilationUnit>> cu2 = root.tryToParse("");

        //情報収集フェーズ(構文解析&情報切り出し)
        for(int i = 0; i < cu2.size(); i++){
            VoidVisitor<?> visitor = new FirstVisitor();
            cu2.get(i).getResult().get().accept(visitor, null);
        }*/

        File file;
        if(args.length == 0)
            file = new File(DataStore.pathName);
        else file = new File(args[0]);
        //file = new File(DataStore.pathName);

        JavaParser parser = new JavaParser();
        CompilationUnit cu = parser.parse(file).getResult().get();
        VoidVisitor<?> visitor = new FirstVisitor();
        cu.accept(visitor, null);

        //判断フェーズ(変数のvar/val、get/set判断、変数名/メソッド名の被り)
        for(String classname:DataStore.memory_classname){
            for(FieldDeclaration fd:DataStore.memory_classfield.get(classname)){
                FieldVisitor fieldVisitor = new FieldVisitor(classname);
                fd.accept(fieldVisitor, null);
            }
            //DataStore.memory_field_info.put(classname, memory_field_Tmp);

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

            //デバッグ用の出力
            /*System.out.println("class:" + classname);
            System.out.println("field information");
            for(String fieldname:DataStore.memory_field_info.get(classname).keySet()){
                String type = DataStore.memory_field_info.get(classname).get(fieldname).get("type");
                String dim = DataStore.memory_field_info.get(classname).get(fieldname).get("dim");
                String assign = DataStore.memory_field_info.get(classname).get(fieldname).get("assign");
                String nullable = DataStore.memory_field_info.get(classname).get(fieldname).get("nullable");
                String isStatic = DataStore.memory_field_info.get(classname).get(fieldname).get("static");
                System.out.println(fieldname + " type:" + type +" dim:" + dim
                        + " assign:" + assign + " nullable:" + nullable + " static:" + isStatic);
            }*/
            /*System.out.println("method information");
            for(String mname:DataStore.memory_method_info.get(classname).keySet()){
                boolean flag = (boolean)DataStore.memory_method_info.get(classname).get(mname).get("static");
                boolean flag2 = (boolean)DataStore.memory_method_info.get(classname).get(mname).get("access");
                System.out.println(mname + " static:" + flag +" access:" + flag2);
            }*/
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
        }

    }

    private static class SomeVisitor extends VoidVisitorAdapter<Void> {
        String classname = "";
        ArrayList<FieldDeclaration> fds = new ArrayList<>();
        ArrayList<String> inner_list = new ArrayList<>();

        public SomeVisitor(String name){
            classname = name;
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
                        flag = cd.accept(checker, null);
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
            classname = name;

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

            for(String fieldname:DataStore.memory_field_info.get(classname).keySet()){
                if(target.equals(fieldname)){
                    if(!flag || scope.equals("this")) {
                        DataStore.memory_field_info.get(classname).get(fieldname).put("assign", "true");

                        if (value.equals("null"))
                            DataStore.memory_field_info.get(classname).get(fieldname).put("nullable", "true");
                    }
                }

            }
        }

        private boolean duplicateCheck(String target){
            for(String fieldname:DataStore.memory_field_info.get(classname).keySet()){
                for(String local:paramList){
                    if(fieldname.equals(local) && fieldname.equals(target)) return true;
                }
            }
            return false;
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

            for(String local:DataStore.memory_localValue_info.get(classname).get(methodname).keySet()){
                if(target.equals(local)){
                    DataStore.memory_localValue_info.get(classname).get(methodname).get(local).put("assign", "true");
                    if (value.equals("null"))
                        DataStore.memory_localValue_info.get(classname).get(methodname).get(local).put("nullable", "true");
                }
            }

            for(String fieldname:DataStore.memory_field_info.get(classname).keySet()){
                if(target.equals(fieldname)){
                    if(!flag || scope.equals("this")) {
                        DataStore.memory_field_info.get(classname).get(fieldname).put("assign", "true");

                        if (value.equals("null"))
                            DataStore.memory_field_info.get(classname).get(fieldname).put("nullable", "true");
                    }
                }

                if(isAccessorCheckS){
                    for(String paramName: paramList){
                        if(target.equals(fieldname) && value.equals(paramName)){
                            isFixableS = true;
                            this.fieldname = fieldname;
                            //setData(isFixableS, fieldname);
                        }
                    }
                }
            }
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
            if(md.getTarget().toString().equals(fieldname)) return true;
            return false;
        }
    }

}
