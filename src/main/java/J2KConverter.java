import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.type.*;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.utils.SourceRoot;
import org.checkerframework.checker.units.qual.A;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

public class J2KConverter {
    static String indent4 = "    ";
    static boolean isJvmStatic = false;
    private static PrintStream sysOut = System.out;
    static String Outputer = "";
    static ArrayList<String> OutputerStore = new ArrayList<>();

    public static void setOutputer(String output){
        Outputer = Outputer + output;
    }

    public static void setOutputerln(String output){
        Outputer = Outputer + output + "\n";
    }

    public static void main(String[] args) throws IOException {

        File file;
        if(args.length == 0)
            file = new File("/Users/kzm0308/IdeaProjects/J2KConverter/src/main/java/pac3/Class4.java");

        else file = new File(args[0]);
        //file = new File(DataStore.pathName);
        JavaParser parser = new JavaParser();
        CompilationUnit cu = parser.parse(file).getResult().get();

        VoidVisitor<?> visitor = new FirstStepVisitor();
        //VoidVisitor<?> visitor = new DummyVisitor();
        cu.accept(visitor, null);

        System.out.println(Outputer);
/*
        PrintStream sysOut = System.out;
        String path_root = "/Users/kzm0308/IdeaProjects/J2KConverter/src/main/java/pac1";
        if(args.length == 0)
            path_root = DataStore.pathName;
        else path_root = args[0];

        //情報収集フェーズ(構文解析&情報切り出し)
        for(String path:ConvertOutputer.dumpFile(path_root, path_root, 0)) {
            JavaParser parser = new JavaParser();
            CompilationUnit cu = parser.parse(new File(path)).getResult().get();
            System.setOut(new PrintStream(ConvertOutputer.CreateConvertFile(path_root, path)));
            System.setOut(sysOut);
            VoidVisitor<?> visitor = new FirstStepVisitor();
            cu.accept(visitor, null);
        }*/
    }

    private static class FirstStepVisitor extends VoidVisitorAdapter<Void> {
        String packageName = "";
        ArrayList<String> ImportNameList = new ArrayList<>();

        @Override
        public void visit(PackageDeclaration md, Void arg){
            packageName = md.getNameAsString();
        }

        @Override
        public void visit(ImportDeclaration md, Void arg){
            ImportNameList.add(md.getNameAsString());
        }

        @Override
        public void visit(ClassOrInterfaceDeclaration md, Void arg){
            if(!packageName.equals(""))setOutputerln("package " + packageName + "\n");
            for(String ImportName:ImportNameList){
                setOutputerln("import " + ImportName);
            }
            OutClassVisitor visitor = new OutClassVisitor("", md.getNameAsString());
            md.accept(visitor, null);
        }

    }

    private static class OutClassVisitor extends VoidVisitorAdapter<Void>{
        String indent = "";
        String classname = "";
        private OutClassVisitor(String indent, String classname){
            this.indent = indent;
            this.classname = classname;
        }

        @Override
        public void visit(ClassOrInterfaceDeclaration md, Void arg){
            if(md.getNameAsString().equals(classname)) {
                if(!isJvmStatic){
                    if(DataStore.isStaticF || DataStore.isStaticM || DataStore.isStaticI) {
                        setOutputerln("import kotlin.jvm.JvmStatic");
                        isJvmStatic = true;
                        setOutputer("\n");
                    }
                }
                NodeList<Modifier> modifiers = md.getModifiers();
                String mod = "";
                if(modifiers.size() != 0){
                    for(Modifier modifier:modifiers) {
                        String tmp = modifier.toString();
                        if (tmp.equals("public ")) tmp = "";
                        if (tmp.equals("static ")) tmp = "";
                        mod = mod.concat(tmp);
                    }
                }
                String classname = md.getNameAsString();
                String CorI = "class";
                if(md.isInterface()) CorI = "interface";
                setOutputer(indent + mod + CorI + " " + classname);
                String typeParams = "";
                boolean isDia = false;
                if(md.getTypeParameters().size() != 0){
                    isDia = true;
                    TypeParameter TP = md.getTypeParameter(0);
                    typeParams = "<" + TP.getNameAsString() + " : " + TP.getTypeBound().get(0) + "?" + ">";
                }
                String extend = "";
                int sizeExIm = 0;
                if(md.getExtendedTypes().size() != 0) {
                    extend = md.getExtendedTypes().get(0).toString();
                    sizeExIm += md.getExtendedTypes().size();
                }
                String implement = "";
                if(md.getImplementedTypes().size() != 0){
                    if(sizeExIm != 0) extend = extend.concat(", ");
                    for(ClassOrInterfaceType typeIm:md.getImplementedTypes()){
                        implement = implement.concat(typeIm.getNameAsString() + ", ");
                        sizeExIm++;
                    }
                    implement = implement.substring(0, implement.length() - 2);
                }
                if(isDia) setOutputer(typeParams);
                if(sizeExIm != 0)
                    setOutputer(":" + extend + implement);
                setOutputerln("{");
                boolean isCompanion = false;
                for(String field:DataStore.memory_field_info.get(classname).keySet()){
                    isCompanion = Boolean.parseBoolean(DataStore.memory_field_info.get(classname).get(field).get("static"));
                    if(isCompanion) break;
                }
                for(String method:DataStore.memory_method_info.get(classname).keySet()){
                    if(isCompanion) break;
                    isCompanion = (boolean)DataStore.memory_method_info.get(classname).get(method).get("static");
                }
                isCompanion = isCompanion || DataStore.isStaticI;
                if (isCompanion) {
                    setOutputerln(indent + indent4 + "companion object{");
                    CompanionObjectVisitor visitor = new CompanionObjectVisitor(indent + indent4, classname);
                    md.accept(visitor, null);
                    setOutputerln("\n" + indent + indent4 + "}");
                }
                super.visit(md, arg);
                setOutputerln(indent + "}");
            } else {
                OutClassVisitor visitor = new OutClassVisitor(indent + indent4, md.getNameAsString());
                md.accept(visitor, null);
            }

        }

        @Override
        public void visit(MethodDeclaration md, Void arg){
            String methodName = md.getNameAsString();
            boolean flag = false;
            if(DataStore.memory_method_info.get(classname) != null) {
                LOOP:
                for (String accessor : DataStore.memory_method_info.get(classname).keySet()) {
                    if (!(boolean) DataStore.memory_method_info.get(classname).get(accessor).get("access")) break;
                    if (methodName.equals(accessor)) {
                        String targetField = (String) DataStore.memory_method_info.get(classname).get(methodName).get("field");
                        for (String accessor2 : DataStore.memory_method_info.get(classname).keySet()) {
                            if (accessor.equals(accessor2)) continue;
                            String targetField2 = (String) DataStore.memory_method_info.get(classname).get(accessor2).get("field");
                            if (targetField.equals(targetField2)) {
                                flag = true;
                                break LOOP;
                            }
                        }
                    }
                }
            }
            if(!flag) {
                boolean isStatic = false;
                if(DataStore.memory_method_info.get(classname) !=null)
                    isStatic = (boolean)DataStore.memory_method_info.get(classname).get(methodName).get("static");
                if(!isStatic) {
                    if (md.getThrownExceptions().size() != 0) {
                        String expt = md.getThrownException(0).toString();
                        setOutputerln(indent4 + "@Throws(" + expt + "::class)");
                    }
                    NodeList<Modifier> modifiers = md.getModifiers();
                    String mod = "";
                    if(modifiers.size() != 0){
                        for(Modifier modifier:modifiers) {
                            String tmp = modifier.toString();
                            if (tmp.equals("public ")) tmp = "";
                            if (tmp.equals("static ")) tmp = "";
                            mod = mod.concat(tmp);
                        }
                    }
                    String override = "";
                    if (md.getAnnotations().size() != 0) override = md.getAnnotation(0).toString();
                    if (override.equals("@Override")) override = "override";
                    setOutputer(indent + indent4 + override + mod + "fun " + methodName + "(");
                    String allParam = "";
                    for (int i = 0; i < md.getParameters().size(); i++) {
                        if (i != 0) allParam = allParam.concat(", ");
                        Parameter param = md.getParameter(i);
                        String paramName = param.getNameAsString();
                        String paramType = ": " + param.getTypeAsString().substring(0, 1).toUpperCase() + param.getTypeAsString().substring(1);
                        if (param.getType().isArrayType()) {
                            paramType = ": Array<" + param.getType().toArrayType().get().getComponentType() + ">";
                        }
                        allParam = allParam.concat(paramName + paramType);
                    }
                    setOutputer(allParam + ")");
                    if (!md.getType().isVoidType()) setOutputer(": ");
                    TypeVisitor Tvisitor = new TypeVisitor();
                    md.getType().accept(Tvisitor, null);
                    /*String type = "";
                    if (!md.getTypeAsString().equals("void"))
                        type = ": " + md.getTypeAsString().substring(0, 1).toUpperCase() + md.getTypeAsString().substring(1);
                    setOutputer(type);*/
                    VoidVisitor<?> visitor = new BlockVisitor(classname, methodName, indent + indent4, indent + indent4);
                    md.accept(visitor, null);
                    setOutputer("\n");
                }
            }
        }

        @Override
        public void visit(FieldDeclaration md, Void arg){
            NodeList<Modifier> modifiers = new NodeList<>();;
            boolean isStatic = false;
            if(md.getModifiers().size() != 0) {
                modifiers = md.getModifiers();
                for(Modifier modifier:modifiers){
                    if(modifier.toString().equals("static ")) isStatic = true;
                }
            }
            if(!isStatic) {
                VariableVisitor visitor = new VariableVisitor(classname, "", false, indent + indent4, modifiers);
                md.accept(visitor, null);
                setOutputer("\n");
            }
        }

        @Override
        public void visit(ConstructorDeclaration md, Void arg){
            setOutputer(indent + indent4 + md.getNameAsString() + "(");
            String allParam = "";
            for(int i = 0;i < md.getParameters().size(); i++) {
                if(i != 0) allParam = allParam.concat(", ");
                Parameter param = md.getParameter(i);
                String paramName = param.getNameAsString();
                String paramType = ": " + param.getTypeAsString().substring(0,1).toUpperCase() + param.getTypeAsString().substring(1);
                if (param.getType().isArrayType()) {
                    paramType = ": Array<" + param.getType().toArrayType().get().getComponentType() + ">";
                }
                allParam = allParam.concat(paramName + paramType);
            }
            setOutputer(allParam + ")");
            VoidVisitor<?> visitor = new BlockVisitor(classname, "",indent + indent4, indent + indent4);
            md.accept(visitor, null);
            setOutputer("\n");
        }

        @Override
        public void visit(EnumDeclaration md, Void arg){
            NodeList<Modifier> modifiers = md.getModifiers();
            String mod = "";
            if (modifiers.size() != 0) {
                for (Modifier modifier : modifiers) {
                    String tmp = modifier.toString();
                    if (tmp.equals("public ")) tmp = "";
                    if (tmp.equals("static ")) tmp = "";
                    mod = mod.concat(tmp);
                }
            }
            setOutputerln(indent + indent4 + mod + "enum class " + md.getNameAsString() + "{");
            int size = md.getEntries().size();
            for(int i = 0; i< size; i++){
                if(i != 0) setOutputer(",\n");
                setOutputer(indent + indent4 + indent4 + md.getEntry(i));
            }
            setOutputer("\n");
            for(Node member:md.getMembers()){
                OutClassVisitor visitor = new OutClassVisitor(indent + indent4, classname);
                member.accept(visitor, null);
            }
            setOutputerln(indent + indent4 + "}");
        }

        @Override
        public void visit(InitializerDeclaration md, Void arg){
            if(!md.isStatic()) {
                setOutputer(indent + indent4 + "init");
                BlockVisitor visitor = new BlockVisitor(classname, "",indent + indent4, indent + indent4);
                md.getBody().accept(visitor, null);
                setOutputer("\n");
            }
        }
    }

    private static class CompanionObjectVisitor extends VoidVisitorAdapter<Void>{
        String indent = "";
        String classname = "";
        private CompanionObjectVisitor(String indent, String classname){
            this.indent = indent;
            this.classname = classname;
        }

        @Override
        public void visit(MethodDeclaration md, Void arg){
            String methodName = md.getNameAsString();
            boolean flag = false;
            LOOP:for(String accessor:DataStore.memory_method_info.get(classname).keySet()){
                if(!(boolean)DataStore.memory_method_info.get(classname).get(accessor).get("access")) break;
                if(methodName.equals(accessor)){
                    String targetField = (String)DataStore.memory_method_info.get(classname).get(methodName).get("field");
                    for(String accessor2:DataStore.memory_method_info.get(classname).keySet()){
                        if(accessor.equals(accessor2)) continue;
                        String targetField2 = (String)DataStore.memory_method_info.get(classname).get(accessor2).get("field");
                        if(targetField.equals(targetField2)) {
                            flag = true;
                            break LOOP;
                        }
                    }
                }
            }
            if(!flag) {
                if (md.getThrownExceptions().size() != 0) {
                    String expt = md.getThrownException(0).toString();
                    setOutputerln(indent4 + "@Throws(" + expt + "::class)");
                }

                boolean isStatic = (boolean)DataStore.memory_method_info.get(classname).get(methodName).get("static");
                if (isStatic) {
                    NodeList<Modifier> modifiers = md.getModifiers();
                    String mod = "";
                    if(modifiers.size() != 0){
                        for(Modifier modifier:modifiers) {
                            String tmp = modifier.toString();
                            if (tmp.equals("public ")) tmp = "";
                            if (tmp.equals("static ")) tmp = "";
                            mod = mod.concat(tmp);
                        }
                    }
                    String override = "";
                    if (md.getAnnotations().size() != 0) override = md.getAnnotation(0).toString();
                    if (override.equals("@Override")) override = "override";
                    setOutputerln(indent + indent4 + "@JvmStatic");
                    setOutputer(indent + indent4 + override + mod + "fun " + methodName + "(");
                    String allParam = "";
                    for (int i = 0; i < md.getParameters().size(); i++) {
                        if (i != 0) allParam = allParam.concat(", ");
                        Parameter param = md.getParameter(i);
                        String paramName = param.getNameAsString();
                        String paramType = ": " + param.getTypeAsString().substring(0, 1).toUpperCase() + param.getTypeAsString().substring(1);
                        if (param.getType().isArrayType()) {
                            paramType = ": Array<" + param.getType().toArrayType().get().getComponentType() + ">";
                        }
                        allParam = allParam.concat(paramName + paramType);
                    }
                    setOutputer(allParam + ")");
                    String type = "";
                    if (!md.getTypeAsString().equals("void"))
                        type = ": " + md.getTypeAsString().substring(0, 1).toUpperCase() + md.getTypeAsString().substring(1);
                    setOutputer(type);
                    VoidVisitor<?> visitor = new BlockVisitor(classname, methodName, indent + indent4, indent + indent4);
                    md.accept(visitor, null);
                    setOutputer("\n");
                }
            }
        }

        @Override
        public void visit(FieldDeclaration md, Void arg){
            NodeList<Modifier> modifiers = new NodeList<>();;
            boolean isStatic = false;
            if(md.getModifiers().size() != 0) {
                modifiers = md.getModifiers();
                for(Modifier modifier:modifiers){
                    if(modifier.toString().equals("static ")) isStatic = true;
                }
            }
            if(isStatic) {
                setOutputerln(indent + indent4 + "@JvmField");
                VariableVisitor visitor = new VariableVisitor(classname, "", false, indent + indent4, modifiers);
                md.accept(visitor, null);
                setOutputer("\n");
            }
        }

        @Override
        public void visit(InitializerDeclaration md, Void arg){
            if(md.isStatic()) {
                setOutputer(indent + indent4 + "init");
                BlockVisitor visitor = new BlockVisitor(classname, "",indent + indent4, indent + indent4);
                md.getBody().accept(visitor, null);
            }
        }
    }

    private static class VariableVisitor extends VoidVisitorAdapter<Void>{
        String classname = "";
        String methodname = "";
        String indent = "";
        NodeList<Modifier> modifiers = new NodeList<>();
        boolean isLocal = false;
        boolean isWhile = false;
        private VariableVisitor(String classname, String methodname, boolean flag, String indent, NodeList<Modifier> modifiers){
            this(classname, methodname, flag, indent, modifiers, false);
        }

        private VariableVisitor(String classname, String methodname, boolean flag, String indent, NodeList<Modifier> modifiers, boolean isWhile){
            this.classname = classname;
            this.methodname = methodname;
            this.indent = indent;
            this.modifiers = modifiers;
            this.isLocal = flag;
            this.isWhile = isWhile;
        }

        @Override
        public void visit(VariableDeclarator md, Void arg){
            String name = md.getNameAsString();
            String dec = "var";
            String initial = "";
            int dim = 0;
            boolean assign = true;

            String mod = "";
            if(modifiers.size() != 0){
                for(Modifier modifier:modifiers) {
                    String tmp = modifier.toString();
                    if (tmp.equals("public ")) tmp = "";
                    if (tmp.equals("static ")) tmp = "";
                    mod = mod.concat(tmp);
                }
            }
            if(isLocal){
                if (DataStore.memory_localValue_info.get(classname).get(methodname) != null) {
                    if(DataStore.memory_localValue_info.get(classname).get(methodname).get(name) != null){
                        dim = Integer.parseInt(DataStore.memory_localValue_info.get(classname).get(methodname).get(name).get("dim"));
                        assign = Boolean.parseBoolean(DataStore.memory_localValue_info.get(classname).get(methodname).get(name).get("assign"));
                    }
                }
            } else {
                if(DataStore.memory_method_info.get(classname) != null) {
                    if (DataStore.memory_field_info.get(classname).get(name) != null) {
                        dim = Integer.parseInt(DataStore.memory_field_info.get(classname).get(name).get("dim"));
                        assign = Boolean.parseBoolean(DataStore.memory_field_info.get(classname).get(name).get("assign"));
                    }
                }
            }
            boolean isLambda = false;
            if(assign || isWhile) dec = "var";
            else dec = "val";
            setOutputer(indent + mod + dec + " " + name);
            AssignVisitor visitor = new AssignVisitor(classname, indent, md.getTypeAsString(), dim);
            if(md.getInitializer().isPresent()) {
                if(md.getInitializer().get().isLambdaExpr()){
                    isLambda = true;
                    setOutputer(" = ");
                    TypeVisitor Tvisitor = new TypeVisitor();
                    md.getType().accept(Tvisitor, null);
                } else {
                    setOutputer(": ");
                    TypeVisitor Tvisitor = new TypeVisitor();
                    md.getType().accept(Tvisitor, null);
                    setOutputer(" = ");
                    md.getInitializer().get().accept(visitor, null);
                }
            } else {
                if(md.getType().isPrimitiveType()){
                    initial = switch (md.getTypeAsString()) {
                        case "int" -> initial.concat(" = 0");
                        case "double" -> initial.concat(" = 0.0");
                        case "long" -> initial.concat(" = 0");
                        case "float" -> initial.concat(" = 0");
                        case "boolean" -> initial.concat(" = false");
                        case "byte" -> initial.concat(" = 0");
                        case "short" -> initial.concat(" = 0");
                        case "char" -> initial.concat(" = '0'");
                        default -> initial.concat(" = 0");
                    };
                } else {
                    setOutputer(": ");
                    TypeVisitor Tvisitor = new TypeVisitor();
                    md.getType().accept(Tvisitor, null);
                }
            }

            boolean flagGS = false;
            if(DataStore.memory_method_info.get(classname) != null) {
                LOOP:
                for (String accessor : DataStore.memory_method_info.get(classname).keySet()) {
                    String targetField = "";
                    boolean flagAC = false;
                    if (DataStore.memory_method_info.get(classname).get(accessor) != null) {
                        targetField = (String) DataStore.memory_method_info.get(classname).get(accessor).get("field");
                        flagAC = (boolean) DataStore.memory_method_info.get(classname).get(accessor).get("fix");
                    }
                    if (!targetField.equals(name)) continue;
                    for (String accessor2 : DataStore.memory_method_info.get(classname).keySet()) {
                        if (accessor.equals(accessor2)) continue;
                        String targetField2 = "";
                        if (DataStore.memory_method_info.get(classname).get(accessor2) != null)
                            targetField2 = (String) DataStore.memory_method_info.get(classname).get(accessor2).get("field");
                        if (targetField.equals(targetField2)) {
                            flagGS = flagAC && (boolean) DataStore.memory_method_info.get(classname).get(accessor2).get("fix");
                            break LOOP;
                        }
                    }
                }

                if (flagGS) mod = "";
                setOutputer(initial);
                if(isLambda) {
                    md.getInitializer().get().accept(visitor, null);
                    //BlockVisitor visitor2 = new BlockVisitor(classname, methodname, indent, indent);
                    //LambdaExpr lambdaExpr = md.getInitializer().get().asLambdaExpr();
                    //lambdaExpr.getBody().accept(visitor2, null);
                }
                int lines = 0;
                boolean flagG = false;
                boolean flagS = false;
                String typeUpper = "";
                for (String methodname : DataStore.memory_method_info.get(classname).keySet()) {
                    String property = "";
                    if (DataStore.memory_method_info.get(classname).get(methodname) != null)
                        property = (String) DataStore.memory_method_info.get(classname).get(methodname).get("field");
                    if (name.equals(property)) {
                        lines = (int) DataStore.memory_method_info.get(classname).get(methodname).get("lines");
                        if (lines > 1) {
                            typeUpper = md.getTypeAsString().substring(0, 1).toUpperCase() + md.getTypeAsString().substring(1);
                            for (MethodDeclaration methodDeclaration : DataStore.memory_classmethod.get(classname)) {
                                if (methodDeclaration.getNameAsString().equals(methodname)) {
                                    flagG = J2KConverterSupporter.search_get(methodDeclaration);
                                    flagS = J2KConverterSupporter.search_set(methodDeclaration);
                                    CustomAccessorConvertor cac = new CustomAccessorConvertor(classname, name, indent + indent4, typeUpper, flagG, flagS);
                                    methodDeclaration.accept(cac, null);
                                }
                            }
                        }
                    }
                }
            }
        }


    }

    private static class BlockVisitor extends VoidVisitorAdapter<Void>{
        String classname = "";
        String methodname = "";
        String indent = "";
        String indentBefore = "";
        private BlockVisitor(String classname, String methodname, String indent, String indentBefore){
            this.classname = classname;
            this.methodname = methodname;
            this.indent = indent;
            this.indentBefore = indentBefore;
        }

        @Override
        public void visit(BlockStmt md, Void arg){
            setOutputerln("{");
            VoidVisitor<?> visitor = new InClassVisitor(classname, methodname,indent + indent4, false);
            md.accept(visitor, null);
            setOutputer(indentBefore + "}");
        }

    }

    private static class InClassVisitor extends VoidVisitorAdapter<Void>{
        String classname = "";
        String methodname = "";
        String indent = "";
        boolean isElseIf = false;
        boolean isConvertFor = true;
        boolean isWhile = false;
        boolean isLambda = false;
        private InClassVisitor(String classname, String methodname, String indent, boolean flag){
            this.classname = classname;
            this.methodname = methodname;
            this.indent = indent;
            this.isElseIf = flag;
        }

        private InClassVisitor(String classname, String methodname, String indent, boolean flag, boolean isWhile){
            this.classname = classname;
            this.methodname = methodname;
            this.indent = indent;
            this.isElseIf = flag;
            this.isWhile = isWhile;
        }

        private InClassVisitor(String classname, String indent, boolean isLambda){
            this.classname = classname;
            this.indent = indent;
            this.isLambda = isLambda;
        }

        @Override
        public void visit(ContinueStmt md, Void arg){
            setOutputerln(md.toString().replace(";",""));
        }

        @Override
        public void visit(BreakStmt md, Void arg){
            setOutputerln(md.toString().replace(";",""));
        }

        @Override
        public void visit(ReturnStmt md, Void arg){
            if(isLambda) setOutputer(indent);
            else setOutputer(indent + "return ");
            //InClassVisitor visitor = new InClassVisitor(this.classname, this.methodname, indent, false);
            AssignVisitor visitor = new AssignVisitor(this.classname, indent + indent4, "", 0);
            md.getExpression().get().accept(visitor, null);
            setOutputer("\n");
        }

        @Override
        public void visit(VariableDeclarationExpr md, Void arg){
            NodeList<Modifier> modifiers = new NodeList<>();;
            if(md.getModifiers().size() != 0){
                modifiers = md.getModifiers();
            }
            VariableVisitor visitor = new VariableVisitor(classname, methodname, true, indent, modifiers, isWhile);
            for(VariableDeclarator vd:md.getVariables()){
                vd.accept(visitor,null);
                setOutputer("\n");
            }
        }

        @Override
        public void visit(UnaryExpr md, Void arg){
            setOutputerln(indent + md);
        }

        @Override
        public void visit(TryStmt md, Void arg){
            setOutputer(indent + "try");
            BlockVisitor visitor = new BlockVisitor(classname, methodname, indent, indent);
            md.getTryBlock().accept(visitor, null);
            for(CatchClause catchClause:md.getCatchClauses()){
                String paramName = catchClause.getParameter().getNameAsString();
                Parameter parameter = catchClause.getParameter();
                if(parameter.getType().isUnionType()){
                    UnionType unionType = (UnionType) parameter.getType();
                    for(ReferenceType ref:unionType.getElements()){
                        setOutputer("catch(" + paramName + ": " + ref + ")");
                        catchClause.getBody().accept(visitor, null);
                    }
                } else {
                    setOutputer("catch(" + paramName + ": " + parameter.getTypeAsString() + ")");
                    catchClause.getBody().accept(visitor, null);
                }
            }
            if(md.getFinallyBlock().isPresent()) {
                setOutputer("finally");
                md.getFinallyBlock().get().accept(visitor, null);
                setOutputer("\n");
            }

        }

        @Override
        public void visit(AssignExpr md, Void arg){
            String target = md.getTarget().toString();
            String arithmeticOperator = SignOperator(md.getOperator().toString());
            setOutputer(indent + target + arithmeticOperator);
            AssignVisitor visitor = new AssignVisitor("");
            md.getValue().accept(visitor, null);
            setOutputer("\n");
        }

        @Override
        public void visit(WhileStmt md, Void arg){
            String condition = md.getCondition().toString();
            setOutputer(indent + "while(" + condition + ")");
            if (md.getBody().isBlockStmt()) {
                VoidVisitor<?> visitor = new BlockVisitor(classname, methodname, indent, indent);
                md.getBody().accept(visitor, null);
                setOutputer("\n");
            } else super.visit(md, arg);
        }

        @Override
        public void visit(IfStmt md, Void arg){
            boolean isIndent = false;
            String condition = md.getCondition().toString();
            if(isElseIf) setOutputer("if(" + condition + ")");
            else setOutputer(indent + "if(" + condition + ")");
            VoidVisitor<?> visitor = new BlockVisitor(classname, "", indent, indent);
            if(md.getThenStmt().isBlockStmt()) {
                md.getThenStmt().accept(visitor, null);
                isIndent = true;
            } else {
                visitor = new InClassVisitor(classname,methodname, " ", false);
                md.getThenStmt().accept(visitor,null);
            }
            if(md.getElseStmt().isPresent()) {
                if(isIndent) setOutputer(" else ");
                else setOutputer(indent + "else");
                if(md.getElseStmt().get().isIfStmt()) visitor = new InClassVisitor(classname, methodname, indent, true);
                md.getElseStmt().get().accept(visitor, null);
            }
            setOutputer("\n");
        }

        @Override
        public void visit(MethodCallExpr md, Void arg){
            setOutputer(indent);
            AssignVisitor visitor = new AssignVisitor(classname, indent, "", 0);
            md.accept(visitor, null);
            setOutputer("\n");
        }

        @Override
        public void visit(MethodReferenceExpr md, Void arg){
            AssignVisitor visitor = new AssignVisitor(classname, indent, "", 0);
            md.accept(visitor, null);
        }

        @Override
        public void visit(SwitchStmt md, Void arg){
            String selector = md.getSelector().toString();
            setOutputerln(indent + "when(" + selector + "){");
            for(SwitchEntry entry:md.getEntries()){
                SwitchEntryVisitor visitor = new SwitchEntryVisitor(indent + indent4, true);
                entry.accept(visitor, null);
            }
            setOutputerln(indent + "}");
        }

        @Override
        public void visit(DoStmt md, Void arg){
            String condition = md.getCondition().toString();
            setOutputer(indent + "do");
            if (md.getBody().isBlockStmt()) {
                VoidVisitor<?> visitor = new BlockVisitor(classname, methodname, indent, indent);
                md.getBody().accept(visitor, null);
            } else {
                super.visit(md, arg);
                setOutputer(indent);
            }
            setOutputerln("while(" + condition + ")");
        }

        @Override
        public void visit(SwitchExpr md, Void arg){
            String selector = md.getSelector().toString();
            setOutputerln("when(" + selector + "){");
            for(SwitchEntry entry:md.getEntries()){
                SwitchEntryVisitor visitor = new SwitchEntryVisitor(indent + indent4, false);
                entry.accept(visitor, null);
            }
            setOutputerln(indent + "}");
        }

        @Override
        public void visit(ForEachStmt md, Void arg){
            String value = md.getVariable().getVariable(0).getNameAsString();
            String iterable = md.getIterable().toString();
            setOutputerln(indent + iterable+ ".foreach{ " + value + " ->");

            VoidVisitor<?> visitor = new InClassVisitor(classname, "", indent+indent4, false);
            md.getBody().accept(visitor, null);
            setOutputerln(indent + "}");
        }

        @Override
        public void visit(ForStmt md, Void arg){
            ForVisitor forvisitor = new ForVisitor();

            for(Expression initial:md.getInitialization()) initial.accept(forvisitor,null);
            HashMap<String,String> initialMap = forvisitor.dataReturn("initial");

            md.getCompare().get().accept(forvisitor, null);
            HashMap<String,String> compareMap = forvisitor.dataReturn("compare");

            for(Expression update:md.getUpdate()) update.accept(forvisitor,null);
            HashMap<String,String> updateMap = forvisitor.dataReturn("update");

            isConvertFor = (initialMap.size() == compareMap.size()) && (initialMap.size() == updateMap.size());

            //setOutputer(indent + "for(");
            for(Expression initial:md.getInitialization()){
                InClassVisitor visitor = new InClassVisitor(classname, methodname, indent, false, true);
                initial.accept(visitor,null);
            }
            setOutputer(indent + "while(");
            String compare = md.getCompare().get().toString();
            if(compare.equals("")) compare = "true";
            setOutputer(compare);
            //setOutputer(Forlooper(initialMap, compareMap, updateMap));
            setOutputer(")");
            setOutputerln("{");
            if (md.getBody().isBlockStmt()) {
                VoidVisitor<?> visitor = new InClassVisitor(classname, methodname, indent + indent4, false);
                md.getBody().accept(visitor, null);
            } else super.visit(md, arg);
            for(Expression update:md.getUpdate()){
                setOutputerln(indent + indent4 + update.toString());
            }
            setOutputerln(indent + "}");
        }

        private String Forlooper(HashMap<String, String> initialMap,HashMap<String, String> compareMap,HashMap<String, String> updateMap){
            String returns = "";
            for(String value:initialMap.keySet()){
                String initial = initialMap.get(value);
                String compareRight = compareMap.get(value).split("-")[0];
                String operator = compareMap.get(value).split("-")[1];
                String update = updateMap.get(value);
                Pattern pattern = Pattern.compile("^[0-9]+$|-[0-9]+$");

                switch (operator){
                    case "LESS":
                        if(pattern.matcher(compareRight).matches())
                            compareRight = Integer.toString(Integer.parseInt(compareRight) - 1);
                        else compareRight = compareRight.concat(" - 1");
                        break;
                    case "GREATER":
                        break;
                    default:
                        break;
                }


                returns = value + " in " + initial + ".." + compareRight;

            }
            return returns;
        }
    }

    private static class ForVisitor extends VoidVisitorAdapter<Void>{
        HashMap<String,String> initialMap = new HashMap<String, String>();
        HashMap<String,String> compareMap = new HashMap<String, String>();
        HashMap<String,String> updateMap = new HashMap<String, String>();

        public HashMap<String, String> dataReturn(String select){
            return switch (select) {
                case "initial" -> initialMap;
                case "compare" -> compareMap;
                case "update" -> updateMap;
                default -> new HashMap<>();
            };
        }

        @Override
        public void visit(AssignExpr md, Void arg){
            initialMap.put(md.getTarget().toString(),md.getValue().toString());
        }

        @Override
        public void visit(VariableDeclarator md, Void arg){
            String initialValue = "";
            if(md.getInitializer().isPresent())
                initialValue =  md.getInitializer().get().toString();
            initialMap.put(md.getNameAsString(),initialValue);
        }

        @Override
        public void visit(BinaryExpr md, Void arg){
            compareMap.put(md.getLeft().toString(), md.getRight() + "-" + md.getOperator());
        }

        @Override
        public void visit(UnaryExpr md, Void arg){
            updateMap.put(md.getExpression().toString(), md.getOperator().toString());
        }

    }

    private static class SwitchEntryVisitor extends VoidVisitorAdapter<Void>{
        String indent = "";
        boolean isStmt = false;
        private SwitchEntryVisitor(String indent,boolean flag){
            this.indent = indent;
            this.isStmt = flag;
        }

        @Override
        public void visit(SwitchEntry md, Void arg){
            String label = "";
            if(md.getLabels().size()!=0) label = md.getLabels().get(0).toString();
            else label = "else";
            setOutputer(indent + label + " -> ");
            if(isStmt)setOutputerln("{");
            for(Statement statement:md.getStatements()){
                if(isStmt)setOutputer(indent + indent4);
                setOutputerln(statement.toString());
            }
            if(isStmt)setOutputerln(indent + "}");
        }

    }

    private static class AssignVisitor extends VoidVisitorAdapter<Void>{
        String classname = "";
        String type = "";
        String indent = "";
        int dim = 0;
        AssignVisitor(String type){
            this.type = type;
        }

        AssignVisitor(String classname, String indent, String type, int dim){
            this.classname = classname;
            this.indent = indent;
            this.type = type;
            this.dim = dim;
        }

        @Override
        public void visit(ArrayCreationExpr md, Void arg){
            String type = md.getElementType().toString().substring(0,1).toUpperCase() + md.getElementType().toString().substring(1);
            String dec = "";
            if(md.getElementType().isPrimitiveType()){
                int size = md.getLevels().size();
                dec = type + "Array(" + md.getLevels().get(size-1).toString().charAt(1) +")";
                for(int i = size - 2; i >= 0; i--){
                    dec = "Array(" + md.getLevels().get(i).toString().charAt(1) + ") {" + dec + "}";
                }
            } else {
                int size = md.getLevels().size();
                dec = "arrayOfNulls<" + type + ">(" + md.getLevels().get(size-1).toString().charAt(1) +")";
                for(int i = size - 2; i >= 0; i--){
                    dec = "Array(" + md.getLevels().get(i).toString().charAt(1) + ") {" + dec + "}";
                }
            }
            setOutputer(dec);
        }

        @Override
        public void visit(ArrayInitializerExpr md, Void arg){
            int size = md.getValues().size();
            String str = "";
            str = str.concat("arrayOf(");
            dim--;
            if(isPrimitiveArray(type) && dim == 0)
                str = str.replace("array",
                        type.replace("[]","") + "Array");
            setOutputer(str);
            for(int i = 0; i < size ; i++){
                if(i != 0) setOutputer(", ");
                AssignVisitor visitor = new AssignVisitor(classname, indent, type, dim);
                md.getValues().get(i).accept(visitor, null);
            }
            setOutputer(")");
        }

        @Override
        public void visit(ObjectCreationExpr md, Void arg){
            String name = md.getTypeAsString();
            String argument = "";
            if(md.getArguments().size() != 0) {
                String tmp = md.getArgument(0).toString();
                String[] str = tmp.split("\\.");
                for (int i = 0; i < str.length; i++) {
                    String substr = "";
                    if (i != 0) substr = substr.concat(".");
                    if (str[i].equals("in"))
                        substr = substr.concat(str[i].replace("in", "`in`"));
                    else substr = substr.concat(str[i]);
                    argument = argument.concat(substr);
                }
            }
            if(md.getAnonymousClassBody().isPresent()){
                setOutputerln(" object: " + name + "(" + argument + ")" + "{");
                OutClassVisitor visitor = new OutClassVisitor(indent, name);
                for(BodyDeclaration bd:md.getAnonymousClassBody().get()){
                    bd.accept(visitor, null);
                }
                setOutputerln(indent + "}");

            }
            else setOutputer(name + "(" + argument + ")");
        }

        @Override
        public void visit(MethodCallExpr md, Void arg){
            String scopeName = "";
            if(md.getScope().isPresent()) {
                String str = md.getScope().get() + "." + md.getNameAsString();
                if(str.equals("System.out.println"));
                else if(str.equals("System.out.print"));
                else {
                    AssignVisitor visitor = new AssignVisitor(classname, indent, type, dim);
                    md.getScope().get().accept(visitor, null);
                    setOutputer(".");
                }
            }
            String methodname = md.getNameAsString();
            setOutputer(scopeName + methodname + "(");
            if (methodname.matches("get[A-Z].*")) {
                if(DataStore.memory_method_info.get(classname) != null){
                    if(DataStore.memory_method_info.get(classname).get(methodname)!= null){
                        if((boolean) DataStore.memory_method_info.get(classname).get(methodname).get("fix")){
                            String property = (String) DataStore.memory_method_info.get(classname).get(methodname).get("field");
                            setOutputer(scopeName + property);
                        }
                    }
                }
            } else if (methodname.matches("set[A-Z].*")) {
                if(DataStore.memory_method_info.get(classname) != null){
                    if(DataStore.memory_method_info.get(classname).get(methodname)!= null){
                        if((boolean) DataStore.memory_method_info.get(classname).get(methodname).get("fix")){
                            String property = (String) DataStore.memory_method_info.get(classname).get(methodname).get("field");
                            setOutputer(scopeName + property);
                        }
                    }
                }
            } else {
                if(methodname.matches("concat")){
                    setOutputer(scopeName + " + ");
                }
                if(md.getArguments().size() != 0){
                    for(int i = 0;i<md.getArguments().size();i++){
                        if(i != 0)setOutputer(",");
                        AssignVisitor visitor = new AssignVisitor(classname, indent, type, dim);
                        md.getArgument(i).accept(visitor, null);
                    }
                }
                setOutputer(")");
            }
        }

        @Override
        public void visit(MethodReferenceExpr md, Void arg){
            setOutputer(md.toString());
        }

        @Override
        public void visit(ConditionalExpr md, Void arg){
            setOutputer("if(" + md.getCondition().toString());
            setOutputer(")");
            AssignVisitor trueVisitor = new AssignVisitor(classname, indent, type, dim);
            md.getThenExpr().accept(trueVisitor, null);
            setOutputer(" else ");
            AssignVisitor falseVisitor = new AssignVisitor(classname, indent, type, dim);
            md.getElseExpr().accept(falseVisitor, null);

        }

        @Override
        public void visit(BinaryExpr md, Void arg){
            AssignVisitor visitor = new AssignVisitor(classname, indent, type, dim);
            md.getLeft().accept(visitor, null);
            String arithmeticOperator = SignOperator(md.getOperator().name());
            setOutputer(arithmeticOperator);
            visitor = new AssignVisitor(classname, indent, type, dim);
            md.getRight().accept(visitor, null);

        }

        @Override
        public void visit(EnclosedExpr md, Void arg){
            setOutputer("(");
            super.visit(md, arg);
            setOutputer(")");
        }

        @Override
        public void visit(CastExpr md, Void arg){
            String castType = md.getTypeAsString();
            super.visit(md, arg);
            setOutputer(" as " + castType.substring(0, 1).toUpperCase() + castType.substring(1));
        }

        @Override
        public void visit(LambdaExpr md, Void arg){
            boolean allowFlag = false;
            setOutputer("{");

            if(md.getParameters().size() != 0){
                /*for(int i = 0;i < md.getParameters().size();i++){
                    if(i != 0) setOutputer(", ");
                    InClassVisitor paramVisitor = new InClassVisitor(classname, "", indent + indent4, false);
                    md.getParameter(i).accept(paramVisitor, null);
                    //setOutputer(md.getParameter(i).toString());
                }*/
                for (int i = 0; i < md.getParameters().size(); i++) {
                    allowFlag = true;
                    if(i != 0) setOutputer(", ");
                    Parameter param = md.getParameter(i);
                    String paramName = param.getNameAsString();
                    String paramType = "";
                    if(param.getTypeAsString().length() != 0)
                        paramType = ": " + param.getTypeAsString().substring(0, 1).toUpperCase() + param.getTypeAsString().substring(1);
                    else paramType = ": Any";
                    if (param.getType().isArrayType()) {
                        paramType = ": Array<" + param.getType().toArrayType().get().getComponentType() + ">";
                    }
                    setOutputer(paramName + paramType);
                }
            }
            if(allowFlag) setOutputer(" -> ");
            setOutputer("\n");

            VoidVisitor<?> visitor = new InClassVisitor(classname, indent + indent4, true);
            md.getBody().accept(visitor, null);
            setOutputer(indent + "}");
        }

        @Override
        public void visit(ArrayAccessExpr md, Void arg){
            setOutputer(md.toString());
        }

        @Override
        public void visit(NameExpr md, Void arg){
            setOutputer(md.getNameAsString());
        }

        @Override
        public void visit(FieldAccessExpr md, Void arg){
            setOutputer(md.toString());
        }

        @Override
        public void visit(StringLiteralExpr md, Void arg){
            setOutputer(md.toString());
        }

        @Override
        public void visit(IntegerLiteralExpr md, Void arg){
            setOutputer(md.toString());
        }

        @Override
        public void visit(DoubleLiteralExpr md, Void arg){
            setOutputer(md.toString());
        }

        @Override
        public void visit(BooleanLiteralExpr md, Void arg){
            setOutputer(md.toString());
        }

        @Override
        public void visit(NullLiteralExpr md, Void arg){
            setOutputer(md.toString());
        }

        @Override
        public void visit(CharLiteralExpr md, Void arg){
            setOutputer(md.toString());
        }

        @Override
        public void visit(LongLiteralExpr md, Void arg){
            setOutputer(md.toString());
        }
//LiteralStringValueExpr

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

    private static class TypeVisitor extends VoidVisitorAdapter<Void>{
        String initialValue = "";


        @Override//参照型はここ
        public void visit(ClassOrInterfaceType md, Void arg){

            setOutputer(md.getNameAsString());
            if(md.getTypeArguments().isPresent()) {
                setOutputer("<");
                super.visit(md, arg);
                setOutputer(">");
            }
        }

        @Override//まんま表示
        public void visit(PrimitiveType md, Void arg){
            String type = md.toString();
            setOutputer(type.substring(0,1).toUpperCase() + type.substring(1));
        }

        @Override//componentTypeで配列の型を取得できる
        public void visit(ArrayType md, Void arg){
            if(md.getComponentType().isPrimitiveType()){
                super.visit(md,arg);
                setOutputer("Array");
            } else {
                setOutputer("Array<");
                super.visit(md, arg);
                setOutputer(">");
            }
        }

        @Override//extendedType：extend, superType：superが取得できる
        public void visit(WildcardType md, Void arg){
            if(md.getExtendedType().isPresent()){
                super.visit(md, arg);
            } else if (md.getSuperType().isPresent()){
                setOutputer("in ");
                super.visit(md, arg);
            } else {
                setOutputer("*");
            }
        }

        @Override
        public void visit(TypeParameter md, Void arg){
            setOutputer("<" + md.getNameAsString() + ":" + md.getTypeBound().get(0) + ">");
        }
    }

    private static class CustomAccessorConvertor extends VoidVisitorAdapter<Void>{
        String classname = "";
        String targetField = "";
        String indent = "";
        String indentDeep = "";
        String type = "";
        boolean flagG = false;
        boolean flagS = false;
        String param = "";

        public CustomAccessorConvertor(String classname, String targetField, String indent, String type, boolean flagG, boolean flagS){
            this.classname = classname;
            this.targetField = targetField;
            this.indent = indent;
            this.indentDeep = indent + indent4;
            this.type = type;
            this.flagG = flagG;
            this.flagS = flagS;
        }

        @Override
        public void visit(MethodDeclaration md, Void arg) {
            if(md.getParameters().size() != 0)
                param = md.getParameter(0).getNameAsString();
            super.visit(md, arg);
        }

        @Override
        public void visit(BlockStmt md, Void arg){
            if(flagG)setOutputer(indent + "get()");
            if(flagS)setOutputer(indent + "set(" + param + ": " + type + ")");
            setOutputerln("{");
            InClassVisitor_CAC visitor = new InClassVisitor_CAC(classname, indent + indent4, false, targetField, param);
            md.accept(visitor, null);
            setOutputerln(indent + "}");
        }


    }

    //CAC:CustomAccessorConverter
    private static class InClassVisitor_CAC extends InClassVisitor {
        String targetField = "";
        String param = "";

        public InClassVisitor_CAC(String classname, String indent, boolean flag, String targetField, String param) {
            super(classname, "", indent, flag);
            this.targetField = targetField;
            this.param = param;
        }

        @Override
        public void visit(AssignExpr md, Void arg){
            String target = md.getTarget().toString();
            if(target.equals(targetField) || target.equals("this."+targetField)) target = "field";
            String arithmeticOperator = SignOperator(md.getOperator().toString());
            setOutputer(indent + target + " " + arithmeticOperator + " ");
            AssignVisitor visitor = new AssignVisitor("");
            md.getValue().accept(visitor, null);
        }

        @Override
        public void visit(ReturnStmt md, Void arg){
            FieldAccessExpr fae = md.getExpression().get().asFieldAccessExpr();
            if(fae.getScope().toString().equals("this") && fae.getNameAsString().equals(targetField)){
                String after = md.toString().replace("this."+targetField, "field");
                setOutputerln(indent + after.replace(";", ""));
            }
        }

    }

    public static String SignOperator(String operator){
        return switch (operator) {
            case "ASSIGN" -> " = ";
            case "PLUS" -> " + ";
            case "MINUS" -> " - ";
            case "MULTIPLY" -> " * ";
            case "DIVIDE" -> " / ";
            case "REMAINDER" -> " % ";
            case "OR" -> " || ";
            case "AND" -> " && ";
            case "BINARY_OR" -> " | ";
            case "BINARY_AND" -> " & ";
            case "XOR" -> " ^ ";
            case "EQUALS" -> " == ";
            case "NOT_EQUALS" -> " != ";
            case "LESS" -> " < ";
            case "GREATER" -> " > ";
            case "LESS_EQUALS" -> " <= ";
            case "GREATER_EQUALS" -> " >= ";
            case "LEFT_SHIFT" -> " << ";
            case "SIGNED_RIGHT_SHIFT" -> " >> ";
            case "UNSIGNED_RIGHT_SHIFT" -> " >>> ";
            default -> "";
        };
    }

}

