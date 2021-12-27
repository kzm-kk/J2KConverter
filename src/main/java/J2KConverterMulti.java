import com.github.javaparser.ParseResult;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
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

public class J2KConverterMulti {
    static String indent4 = "    ";
    static boolean isJvmStatic = false;
    private static PrintStream sysOut = System.out;

    public static void main(String[] args) throws IOException {

        String path_root = "/Users/kzm0308/IdeaProjects/J2KConverter/src/main/java/pac1";
        if(args.length == 0)
            path_root = DataStore.pathName;
        else path_root = args[0];

        //情報収集フェーズ(構文解析&情報切り出し)
        for(String path:ConvertOutputer.dumpFile(path_root, path_root, 0)) {
            CompilationUnit cu = StaticJavaParser.parse(new File(path));
            System.setOut(new PrintStream(ConvertOutputer.CreateConvertFile(path_root, path)));
            System.setOut(sysOut);
            VoidVisitor<?> visitor = new FirstStepVisitor();
            cu.accept(visitor, null);
        }

    }

    private static class FirstStepVisitor extends VoidVisitorAdapter<Void> {
        ArrayList<ImportDeclaration> Import_list = new ArrayList<>();

        @Override
        public void visit(ImportDeclaration md, Void arg){
            super.visit(md, arg);
            Import_list.add(md);

        }

        @Override
        public void visit(ClassOrInterfaceDeclaration md, Void arg){
            //memory_import.put(md.getNameAsString(), Import_list);
            DataStore.isStaticF = false;
            DataStore.isStaticM = false;
            isJvmStatic = false;
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
        public void visit(PackageDeclaration md, Void arg){
            System.out.println(md.toString());
        }

        @Override
        public void visit(ImportDeclaration md, Void arg){
            System.out.println(md.toString());
        }

        @Override
        public void visit(ClassOrInterfaceDeclaration md, Void arg){
            //System.out.println(classname + " " +md.getNameAsString() + " " + md.isInnerClass());
            if(md.getNameAsString().equals(classname)) {
                if(!isJvmStatic){
                    if(DataStore.isStaticF || DataStore.isStaticM) System.out.println("import kotlin.jvm.JvmStatic");
                    //if(DataStore.isStaticM) System.out.println();

                    isJvmStatic = true;

                    System.out.print("\n");
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
                System.out.print(indent + mod + CorI + " " + classname);
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
                if(sizeExIm != 0)
                    System.out.print(":" + extend + implement);
                System.out.println("{");
                boolean isCompanion = false;
                for(String field:DataStore.memory_field_info.get(classname).keySet()){
                    isCompanion = Boolean.parseBoolean(DataStore.memory_field_info.get(classname).get(field).get("static"));
                    if(isCompanion) break;
                }
                for(String method:DataStore.memory_method_info.get(classname).keySet()){
                    if(isCompanion) break;
                    isCompanion = (boolean)DataStore.memory_method_info.get(classname).get(method).get("static");
                }
                if (isCompanion) {
                    System.out.println(indent + indent4 + "companion object{");
                    CompanionObjectVisitor visitor = new CompanionObjectVisitor(indent + indent4, classname);
                    md.accept(visitor, null);
                    System.out.println(indent + indent4 + "}");
                }
                super.visit(md, arg);
                System.out.println(indent + "}");
            } else {
                OutClassVisitor visitor = new OutClassVisitor(indent + indent4, md.getNameAsString());
                md.accept(visitor, null);
            }

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
                boolean isStatic = (boolean)DataStore.memory_method_info.get(classname).get(methodName).get("static");
                if(!isStatic) {
                    if (md.getThrownExceptions().size() != 0) {
                        String expt = md.getThrownException(0).toString();
                        System.out.println(indent4 + "@Throws(" + expt + "::class)");
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
                    System.out.print(indent + indent4 + override + mod + "fun " + methodName + "(");
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
                    System.out.print(allParam + ")");
                    String type = "";
                    if (!md.getTypeAsString().equals("void"))
                        type = ": " + md.getTypeAsString().substring(0, 1).toUpperCase() + md.getTypeAsString().substring(1);
                    System.out.print(type);
                    VoidVisitor<?> visitor = new BlockVisitor(classname, methodName, indent + indent4, indent + indent4);
                    md.accept(visitor, null);
                    System.out.print("\n");
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
            }
        }

        @Override
        public void visit(ConstructorDeclaration md, Void arg){
            System.out.print(indent + indent4 + md.getNameAsString() + "(");
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
            System.out.print(allParam + ")");
            VoidVisitor<?> visitor = new BlockVisitor(classname, "",indent + indent4, indent + indent4);
            md.accept(visitor, null);
            System.out.print("\n");
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
                    System.out.println(indent4 + "@Throws(" + expt + "::class)");
                }

                boolean isStatic = false;
                if(DataStore.memory_method_info.get(classname) != null) {
                    if(DataStore.memory_method_info.get(classname).get(methodName) != null)
                    isStatic = (boolean) DataStore.memory_method_info.get(classname).get(methodName).get("static");
                }
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
                    System.out.println(indent + indent4 + "@JvmStatic");
                    System.out.print(indent + indent4 + override + mod + "fun " + methodName + "(");
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
                    System.out.print(allParam + ")");
                    String type = "";
                    if (!md.getTypeAsString().equals("void"))
                        type = ": " + md.getTypeAsString().substring(0, 1).toUpperCase() + md.getTypeAsString().substring(1);
                    System.out.print(type);
                    VoidVisitor<?> visitor = new BlockVisitor(classname, methodName, indent + indent4, indent + indent4);
                    md.accept(visitor, null);
                    System.out.print("\n");
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
                System.out.println(indent + indent4 + "@JvmField");
                VariableVisitor visitor = new VariableVisitor(classname, "", false, indent + indent4, modifiers);
                md.accept(visitor, null);
            }
        }
    }

    private static class VariableVisitor extends VoidVisitorAdapter<Void>{
        String classname = "";
        String methodname = "";
        String indent = "";
        NodeList<Modifier> modifiers = new NodeList<>();
        boolean isLocal = false;
        private VariableVisitor(String classname, String methodname, boolean flag, String indent, NodeList<Modifier> modifiers){
            this.classname = classname;
            this.methodname = methodname;
            this.indent = indent;
            this.modifiers = modifiers;
            this.isLocal = flag;
        }

        @Override
        public void visit(VariableDeclarator md, Void arg){
            String name = md.getNameAsString();
            String dec = "";
            String initial = "";
            int dim = 0;
            boolean assign = false;

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
                if (DataStore.memory_field_info.get(classname).get(name) != null) {
                    dim = Integer.parseInt(DataStore.memory_field_info.get(classname).get(name).get("dim"));
                    assign = Boolean.parseBoolean(DataStore.memory_field_info.get(classname).get(name).get("assign"));
                }
            }
            if(md.getInitializer().isPresent()) {
                AssignVisitor visitor = new AssignVisitor(classname, md.getTypeAsString(), dim);
                md.getInitializer().get().accept(visitor, null);
                initial = " = " + visitor.getAssignRight();
            } else {
                initial = ": " + md.getTypeAsString().substring(0,1).toUpperCase() + md.getTypeAsString().substring(1);
                if(md.getType().isPrimitiveType()){
                    switch (md.getTypeAsString()){
                        case "int":
                            initial = initial.concat(" = 0");
                            break;
                        case "double" :
                            initial = initial.concat(" = 0.0");
                            break;
                        case "long":
                            initial = initial.concat(" = 0");
                            break;
                        case "float":
                            initial = initial.concat(" = 0");
                            break;
                        case "boolean":
                            initial = initial.concat(" = false");
                            break;
                        case "byte":
                            initial = initial.concat(" = 0");
                            break;
                        case "short":
                            initial = initial.concat(" = 0");
                            break;
                        case "char":
                            initial = initial.concat(" = '0'");
                            break;
                        default:
                            initial = initial.concat(" = 0");
                            break;
                    };
                }
            }
            if(assign) dec = "var";
            else dec = "val";

            boolean flagGS = false;
            LOOP:for(String accessor:DataStore.memory_method_info.get(classname).keySet()) {
                String targetField = "";
                boolean flagAC = false;
                if(DataStore.memory_method_info.get(classname).get(accessor) != null) {
                    targetField = (String) DataStore.memory_method_info.get(classname).get(accessor).get("field");
                    flagAC = (boolean) DataStore.memory_method_info.get(classname).get(accessor).get("fix");
                }
                if(!targetField.equals(name)) continue;
                for (String accessor2 : DataStore.memory_method_info.get(classname).keySet()) {
                    if (accessor.equals(accessor2)) continue;
                    String targetField2 = "";
                    if(DataStore.memory_method_info.get(classname).get(accessor2) != null)
                        targetField2 = (String) DataStore.memory_method_info.get(classname).get(accessor2).get("field");
                    if (targetField.equals(targetField2)) {
                        flagGS = flagAC && (boolean) DataStore.memory_method_info.get(classname).get(accessor2).get("fix");
                        break LOOP;
                    }
                }
            }

            if(flagGS) mod = "";
            System.out.println(indent + mod + dec + " " + name + initial);

            int lines = 0;
            boolean flagG = false;
            boolean flagS = false;
            String typeUpper = "";
            for(String methodname:DataStore.memory_method_info.get(classname).keySet()){
                String property = "";
                if(DataStore.memory_method_info.get(classname).get(methodname) != null)
                    property = (String)DataStore.memory_method_info.get(classname).get(methodname).get("field");
                if(name.equals(property)){
                    lines = (int)DataStore.memory_method_info.get(classname).get(methodname).get("lines");
                    if(lines > 1){
                        typeUpper = md.getTypeAsString().substring(0,1).toUpperCase() + md.getTypeAsString().substring(1);
                        for(MethodDeclaration methodDeclaration:DataStore.memory_classmethod.get(classname)){
                            if(methodDeclaration.getNameAsString().equals(methodname)){
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
            System.out.println("{");
            VoidVisitor<?> visitor = new InClassVisitor(classname, methodname,indent + indent4, false);
            md.accept(visitor, null);
            System.out.print(indentBefore + "}");
        }

    }

    private static class InClassVisitor extends VoidVisitorAdapter<Void>{
        String classname = "";
        String methodname = "";
        String indent = "";
        boolean isElseIf = false;
        private InClassVisitor(String classname, String methodname, String indent, boolean flag){
            this.classname = classname;
            this.methodname = methodname;
            this.indent = indent;
            this.isElseIf = flag;
        }

        @Override
        public void visit(ContinueStmt md, Void arg){
            System.out.println(md.toString().replace(";",""));
        }

        @Override
        public void visit(BreakStmt md, Void arg){
            System.out.println(md.toString().replace(";",""));
        }

        @Override
        public void visit(ReturnStmt md, Void arg){
            System.out.println(indent + md.toString().replace(";", ""));
        }

        @Override
        public void visit(VariableDeclarationExpr md, Void arg){
            NodeList<Modifier> modifiers = new NodeList<>();;
            if(md.getModifiers().size() != 0){
                modifiers = md.getModifiers();
            }
            VariableVisitor visitor = new VariableVisitor(classname, methodname, true, indent, modifiers);
            md.accept(visitor,null);
        }

        @Override
        public void visit(UnaryExpr md, Void arg){
            System.out.println(indent + md);
        }

        @Override
        public void visit(AssignExpr md, Void arg){
            String target = md.getTarget().toString();
            String operator = md.getOperator().toString();
            String arithmeticOperator = "";
            switch (operator){
                case "ASSIGN":
                    arithmeticOperator = "=";
                    break;
                default:
                    arithmeticOperator = "";
            }
            AssignVisitor visitor = new AssignVisitor("");
            md.getValue().accept(visitor, null);
            System.out.print(indent + target + " " + arithmeticOperator + " ");
            System.out.println(visitor.getAssignRight());
        }

        @Override
        public void visit(WhileStmt md, Void arg){
            String condition = md.getCondition().toString();
            System.out.print(indent + "while(" + condition + ")");
            if (md.getBody().isBlockStmt()) {
                VoidVisitor<?> visitor = new BlockVisitor(classname, methodname, indent, indent);
                md.getBody().accept(visitor, null);
                System.out.print("\n");
            } else super.visit(md, arg);
        }

        @Override
        public void visit(IfStmt md, Void arg){
            Boolean isIndent = false;
            String condition = md.getCondition().toString();
            if(isElseIf) System.out.print("if(" + condition + ")");
            else System.out.print(indent + "if(" + condition + ")");
            VoidVisitor<?> visitor = new BlockVisitor(classname, "", indent, indent);
            if(md.getThenStmt().isBlockStmt()) {
                md.getThenStmt().accept(visitor, null);
                isIndent = true;
            } else {
                visitor = new InClassVisitor(classname,methodname, " ", false);
                md.getThenStmt().accept(visitor,null);
            }
            if(md.getElseStmt().isPresent()) {
                if(isIndent) System.out.print(" else ");
                else System.out.print(indent + "else");
                if(md.getElseStmt().get().isIfStmt()) visitor = new InClassVisitor(classname, methodname, indent, true);
                md.getElseStmt().get().accept(visitor, null);
            }
            System.out.print("\n");
        }

        @Override
        public void visit(MethodCallExpr md, Void arg){
            String scopeName = "";
            if(md.getScope().isPresent())
                scopeName = md.getScope().get().toString()+ ".";
            String methodName = md.getNameAsString();
            if(md.getNameAsString().matches("println")){
                System.out.print(indent + methodName +"(");
                for(Node argument:md.getArguments())
                    System.out.print(argument.toString());
                System.out.println(")");
            } else if(md.getNameAsString().matches("print")){
                System.out.print(indent + methodName +"(");
                for(Node argument:md.getArguments())
                    System.out.print(argument.toString());
                System.out.println(")");
            } else if (md.getNameAsString().matches("get[A-Z].*")) {
                String showLine = indent + md.toString();
                if(DataStore.memory_method_info.get(classname) != null){
                    if(DataStore.memory_method_info.get(classname).get(methodName)!= null){
                        if((boolean) DataStore.memory_method_info.get(classname).get(methodName).get("fix")){
                            String property = (String) DataStore.memory_method_info.get(classname).get(methodName).get("field");
                            showLine = indent + md.toString().replace(methodName+"()", property);
                        }
                    }
                }
                System.out.println(showLine);
            } else if (md.getNameAsString().matches("set[A-Z].*")) {
                String showLine = indent + md.toString();
                if(DataStore.memory_method_info.get(classname) != null){
                    if(DataStore.memory_method_info.get(classname).get(methodName)!= null){
                        if((boolean) DataStore.memory_method_info.get(classname).get(methodName).get("fix")){
                            String property = (String) DataStore.memory_method_info.get(classname).get(methodName).get("field");
                            showLine = indent + md.toString().replace(methodName+"()", property);

                        }
                    }
                }
                System.out.println(showLine);
            } else {
                System.out.print(indent + scopeName + methodName +"(");
                for(Expression expression:md.getArguments()){
                    AssignVisitor visitor = new AssignVisitor("");
                    expression.accept(visitor,null);
                    System.out.print(visitor.getAssignRight());
                }
                System.out.println(")");
            }
        }

        @Override
        public void visit(SwitchStmt md, Void arg){
            String selector = md.getSelector().toString();
            System.out.println(indent + "when(" + selector + "){");
            for(SwitchEntry entry:md.getEntries()){
                SwitchEntryVisitor visitor = new SwitchEntryVisitor(indent + indent4);
                entry.accept(visitor, null);
            }
            System.out.println(indent + "}");
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

            System.out.print(indent + "for(");
            System.out.print(Forlooper(initialMap, compareMap, updateMap));
            System.out.print(")");
            if (md.getBody().isBlockStmt()) {
                VoidVisitor<?> visitor = new BlockVisitor(classname, methodname, indent, indent);
                md.getBody().accept(visitor, null);
                System.out.print("\n");
            } else super.visit(md, arg);
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
                    case "MORE":
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
        private SwitchEntryVisitor(String indent){
            this.indent = indent;
        }

        @Override
        public void visit(SwitchEntry md, Void arg){
            String label = "";
            if(md.getLabels().size()!=0) label = md.getLabels().get(0).toString();
            else label = "else";
            System.out.println(indent + label + " -> " + "{");
            for(Statement statement:md.getStatements()){
                System.out.println(indent + indent4 + statement);
            }
            System.out.println(indent + "}");
        }

    }

    private static class AssignVisitor extends VoidVisitorAdapter<Void>{
        String classname = "";
        String assignRight = "";
        String type = "";
        int dim = 0;
        AssignVisitor(String type){
            this.type = type;
        }

        AssignVisitor(String classname, String type, int dim){
            this.classname = classname;
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
            assignRight = dec;
        }

        @Override
        public void visit(ArrayInitializerExpr md, Void arg){
            int size = md.getValues().size();
            String str = "";
            str = str.concat("arrayOf(");//arrayOfのままにする処理は後実装
            dim--;
            if(isPrimitiveArray(type) && dim == 0)
                str = str.replace("array",
                        type.replace("[]","") + "Array");
            for(int i = 0; i < size ; i++){
                if(i != 0) str = str.concat(", ");
                AssignVisitor visitor = new AssignVisitor(classname, type, dim);
                md.getValues().get(i).accept(visitor, null);
                str = str.concat(visitor.getAssignRight());
            }
            str = str.concat(")");
            assignRight = str;
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
            assignRight = name + "(" + argument + ")";
        }

        @Override
        public void visit(MethodCallExpr md, Void arg){
            String methodname = md.getNameAsString();
            if(methodname.equals("concat")){
                assignRight = md.getScope().get().toString() + " + " + md.getArgument(0).toString();
            } else if (methodname.matches("get[A-Z].*")) {
                assignRight = md.toString();
                if(DataStore.memory_method_info.get(classname) != null){
                    if(DataStore.memory_method_info.get(classname).get(methodname)!= null){
                        if((boolean) DataStore.memory_method_info.get(classname).get(methodname).get("fix")){
                            String property = (String) DataStore.memory_method_info.get(classname).get(methodname).get("field");
                            assignRight = md.toString().replace(methodname+"()", property);
                        }
                    }
                }
            } else if (methodname.matches("set[A-Z].*")) {
                assignRight = md.toString();
                if(DataStore.memory_method_info.get(classname) != null){
                    if(DataStore.memory_method_info.get(classname).get(methodname)!= null){
                        if((boolean) DataStore.memory_method_info.get(classname).get(methodname).get("fix")){
                            String property = (String) DataStore.memory_method_info.get(classname).get(methodname).get("field");
                            assignRight = md.toString().replace(methodname+"()", property);
                        }
                    }
                }
            } else assignRight = md.toString();
        }

        @Override
        public void visit(BinaryExpr md, Void arg){
            String left = md.getLeft().toString();
            String operator = md.getOperator().toString();
            String arithmeticOperator = "";
            switch (operator){
                case "ASSIGN":
                    arithmeticOperator = " = ";
                    break;
                case "PLUS":
                    arithmeticOperator = " + ";
                    break;
                case "MINUS":
                    arithmeticOperator = " - ";
                    break;
                default:
                    arithmeticOperator = "";
            }
            assignRight = left + arithmeticOperator + md.getRight().toString();
            //AssignVisitor visitor = new AssignVisitor("");
            //md.getRight().accept(visitor, null);
        }

        @Override//bad code
        public void visit(EnclosedExpr md, Void arg){
            System.out.print("(");
            super.visit(md, arg);
            System.out.print(")");
        }

        @Override
        public void visit(ArrayAccessExpr md, Void arg){
            assignRight = md.toString();
        }

        @Override
        public void visit(NameExpr md, Void arg){
            assignRight = md.getNameAsString();
        }

        @Override
        public void visit(FieldAccessExpr md, Void arg){
            assignRight = md.toString();
        }

        @Override
        public void visit(StringLiteralExpr md, Void arg){
            assignRight = md.toString();
        }

        @Override
        public void visit(IntegerLiteralExpr md, Void arg){
            assignRight = md.toString();
        }

        @Override
        public void visit(DoubleLiteralExpr md, Void arg){
            assignRight = md.toString();
        }

        @Override
        public void visit(BooleanLiteralExpr md, Void arg){
            assignRight = md.toString();
        }

        @Override
        public void visit(NullLiteralExpr md, Void arg){
            assignRight = md.toString();
        }

        @Override
        public void visit(CharLiteralExpr md, Void arg){
            assignRight = md.toString();
        }

        @Override
        public void visit(LongLiteralExpr md, Void arg){
            assignRight = md.toString();
        }
//LiteralStringValueExpr


        public String getAssignRight(){
            return assignRight;
        }

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
            if(flagG)System.out.print(indent + "get()");
            if(flagS)System.out.print(indent + "set(" + param + ": " + type + ")");
            System.out.println("{");
            InClassVisitor_CAC visitor = new InClassVisitor_CAC(classname, indent + indent4, false, targetField, param);
            md.accept(visitor, null);
            System.out.println(indent + "}");
        }


    }

    private static class InClassVisitor_CAC extends InClassVisitor{
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
            String operator = md.getOperator().toString();
            String arithmeticOperator = "";
            switch (operator){
                case "ASSIGN":
                    arithmeticOperator = "=";
                    break;
                default:
                    arithmeticOperator = "";
            }
            AssignVisitor visitor = new AssignVisitor("");
            md.getValue().accept(visitor, null);
            System.out.print(indent + target + " " + arithmeticOperator + " ");
            System.out.println(visitor.getAssignRight());
        }

        @Override
        public void visit(ReturnStmt md, Void arg){
            FieldAccessExpr fae = md.getExpression().get().asFieldAccessExpr();
            if(fae.getScope().toString().equals("this") && fae.getNameAsString().equals(targetField)){
                String after = md.toString().replace("this."+targetField, "field");
                System.out.println(indent + after.replace(";", ""));
            }
        }

    }

}

