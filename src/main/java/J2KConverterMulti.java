import com.github.javaparser.JavaParser;
import com.github.javaparser.Range;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.comments.BlockComment;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.ast.comments.LineComment;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.type.*;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;

public class J2KConverterMulti {
    static String indent4 = "    ";
    static boolean isJvmStatic = false;
    private static PrintStream sysOut = System.out;
    static StringBuilder Outputer = new StringBuilder("");
    static ArrayList<StringBuilder> OutputerStore = new ArrayList<>();
    public static String pathAbs = "";

    private static boolean isNonNullFlag = false;

    public static void setOutputer(String output) {
        Outputer.append(output);
    }

    public static void setOutputerln(String output) {
        setOutputer(output);
        Outputer.append("\n");
    }

    public static void main(String[] args) throws IOException {

        /*File file;
        if(args.length == 0)
            file = new File("/Users/kzm0308/IdeaProjects/J2KConverter/src/main/java/pac3/Class4.java");

        else {
            int size = args.length;
            file = new File(args[0]);
            if(size > 1) {
                if (args[1].equals("-nn")) isNonNullFlag = true;
            }
        }
        JavaParser parser = new JavaParser();
        CompilationUnit cu = parser.parse(file).getResult().get();

        VoidVisitor<?> visitor = new OutClassVisitor("");
        cu.accept(visitor, null);
        
        System.out.println(Outputer);
*/
        PrintStream sysOut = System.out;
        String path_root = "/Users/kzm0308/IdeaProjects/J2KConverter/src/main/java/pac1";
        if (args.length == 0)
            path_root = DataStore.pathName;
        else {
            int size = args.length;
            path_root = args[0];
            if (size > 1) {
                if (args[1].equals("-nn")) isNonNullFlag = true;
            }
        }

        for (String path : ConvertOutputer.dumpFile(path_root, path_root, 0)) {
            Outputer = new StringBuilder();
            JavaParser parser = new JavaParser();
            CompilationUnit cu = parser.parse(new File(path)).getResult().get();
            pathAbs = cu.getStorage().get().getPath().toString();
            //System.setOut(new PrintStream(ConvertOutputer.CreateConvertFile(path_root, path)));
            System.setOut(sysOut);
            VoidVisitor<?> visitor = new OutClassVisitor("", "", new ArrayDeque<>());
            cu.accept(visitor, null);
            OutputerStore.add(Outputer);
            //System.out.println(Outputer);
        }

        for (StringBuilder sb : OutputerStore) {
            System.out.println("~~~~~~~~~~~~~");
            System.out.println(sb);
        }
    }

    private static class OutClassVisitor extends VoidVisitorAdapter<Void> {
        String indent = "";
        String packageName = "";
        String structure = "";
        ArrayDeque<Range> rangeStructure = new ArrayDeque<>();
        ArrayList<String> ImportNameList = new ArrayList<>();

        public OutClassVisitor(String indent, String structure, ArrayDeque<Range> rangeStructure) {
            this.indent = indent;
            this.structure = structure;
            this.rangeStructure = rangeStructure;
        }

        @Override
        public void visit(PackageDeclaration md, Void arg) {
            setOutputer(md.toString());
            packageName = md.getNameAsString();
        }

        @Override
        public void visit(AnnotationDeclaration md, Void arg) {
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
            String name = md.getNameAsString();
            setOutputer(mod + "annotation class " + name + "(");
            String typeParams = "";
            for (int i = 0; i < md.getMembers().size(); i++) {
                if (i > 0) setOutputer(", ");
                AnnotationMemberDeclaration AMD = (AnnotationMemberDeclaration) md.getMember(i);
                setOutputer("val " + AMD.getNameAsString() + ": ");
                TypeVisitor typeVisitor = new TypeVisitor();
                AMD.getType().accept(typeVisitor, null);
                if(isNonNullFlag) setOutputer(typeVisitor.getRetType());
                else setOutputer(typeVisitor.getRetTypeNullable());
            }
            setOutputerln(typeParams + ")");
        }

        @Override
        public void visit(ImportDeclaration md, Void arg) {
            ImportNameList.add(md.toString());
        }

        @Override
        public void visit(ClassOrInterfaceDeclaration md, Void arg) {
            //System.out.println("class:" + md.getNameAsString());
            //System.out.println("now: " + md.getRange().get());

            if(DataStore.memoryStatic.get(pathAbs) && !isJvmStatic) {
                setOutputerln("import kotlin.jvm.JvmStatic");
                isJvmStatic = true;
            }
            for(String importName:ImportNameList) setOutputer(importName);
            //import必須なこれの扱い、早急に考えるべき
            /*if (!isJvmStatic) {
                if (DataStore.isStaticF || DataStore.isStaticM || DataStore.isStaticI) {
                    setOutputerln("import kotlin.jvm.JvmStatic");
                    isJvmStatic = true;
                    setOutputer("\n");
                }
            }*/
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
            String classname = md.getNameAsString();
            String CorI = "class";
            if (md.isInterface()) CorI = "interface";
            setOutputer(indent + mod + CorI + " " + classname);
            String typeParams = "";
            boolean isDia = false;
            if (md.getTypeParameters().size() != 0) {
                isDia = true;
                TypeParameter TP = md.getTypeParameter(0);
                typeParams = "<" + TP.getNameAsString() + " : " + TP.getTypeBound().get(0) + "?" + ">";
            }
            String extend = "";
            int sizeExIm = 0;
            if (md.getExtendedTypes().size() != 0) {
                extend = md.getExtendedTypes().get(0).toString();
                sizeExIm += md.getExtendedTypes().size();
            }
            String implement = "";
            if (md.getImplementedTypes().size() != 0) {
                if (sizeExIm != 0) extend = extend.concat(", ");
                for (ClassOrInterfaceType typeIm : md.getImplementedTypes()) {
                    implement = implement.concat(typeIm.getNameAsString() + ", ");
                    sizeExIm++;
                }
                implement = implement.substring(0, implement.length() - 2);
            }
            if (isDia) setOutputer(typeParams);
            if (sizeExIm != 0)
                setOutputer(":" + extend + implement);
            setOutputerln("{");
            boolean isCompanion = false;
            String structureThis = this.structure + "/" + md.getNameAsString();
            ArrayDeque<Range> rangeStructure = this.rangeStructure.clone();
            Range range = md.getRange().get();
            rangeStructure.add(range);
            ClassInfomation CI = DataStore.memoryClass.getData(pathAbs, classname);

            //static周りは大幅に変更
            /*for (String field : DataStore.memory_field_info.get(classname).keySet()) {
                isCompanion = Boolean.parseBoolean(DataStore.memory_field_info.get(classname).get(field).get("static"));
                if (isCompanion) break;
            }
            for (String method : DataStore.memory_method_info.get(classname).keySet()) {
                if (isCompanion) break;
                isCompanion = (boolean) DataStore.memory_method_info.get(classname).get(method).get("static");
            }
            isCompanion = isCompanion || DataStore.isStaticI;
            */

            if(!classname.equals("Class6") && !classname.equals("Class8")) {
                if(CI.isContainStatic){
                //if (isCompanion) {
                    setOutputerln(indent + indent4 + "companion object{");
                    for (BodyDeclaration<?> bodyDeclaration : md.getMembers()) {
                        InClassVisitor CompanionVisitor = new InClassVisitor(indent + indent4, md.getNameAsString(), structureThis, rangeStructure, true);
                        bodyDeclaration.accept(CompanionVisitor, null);
                    }
                    setOutputerln("\n" + indent + indent4 + "}");
                }
                for (BodyDeclaration<?> bodyDeclaration : md.getMembers()) {
                    InClassVisitor CompanionVisitor = new InClassVisitor(indent, md.getNameAsString(), structureThis, rangeStructure, false);
                    bodyDeclaration.accept(CompanionVisitor, null);
                }
            }
            setOutputerln(indent + "}");

        }

        @Override
        public void visit(EnumDeclaration md, Void arg) {
            NodeList<Modifier> modifiers = md.getModifiers();
            String mod = "";
            boolean isStatic = false;
            if (modifiers.size() != 0) {
                for (Modifier modifier : modifiers) {
                    String tmp = modifier.toString();
                    if (tmp.equals("public ")) tmp = "";
                    if (tmp.equals("static ")) {
                        tmp = "";
                        isStatic = true;
                    }
                    mod = mod.concat(tmp);
                }
            }
            setOutputerln(indent + indent4 + mod + "enum class " + md.getNameAsString() + "{");
            int size = md.getEntries().size();
            for (int i = 0; i < size; i++) {
                if (i != 0) setOutputer(",\n");
                setOutputer(indent + indent4 + indent4 + md.getEntry(i));
            }
            setOutputer("\n");

            String structureThis = this.structure + "/" + md.getNameAsString();
            ArrayDeque<Range> rangeStructure = this.rangeStructure.clone();
            Range range = md.getRange().get();
            rangeStructure.add(range);
            for (Node member : md.getMembers()) {
                InClassVisitor visitor = new InClassVisitor(indent + indent4, md.getNameAsString(), structureThis, rangeStructure, false);
                member.accept(visitor, null);
            }
            setOutputerln(indent + indent4 + "}");
        }

    }

    private static class InClassVisitor extends VoidVisitorAdapter<Void> {
        String indent = "";
        String classname = "";
        String structure = "";
        ArrayDeque<Range> rangeStructure = new ArrayDeque<>();
        ArrayDeque<BlockInfomation> dequeBI = new ArrayDeque<>();
        ClassInfomation CI = null;
        boolean isCompanionConvert = false;

        private InClassVisitor(String indent, String classname, String structure,
                               ArrayDeque<Range> rangeStructure, boolean isCompanionConvert) {
            this.indent = indent;
            this.classname = classname;
            this.structure = structure;
            this.rangeStructure = rangeStructure;
            this.isCompanionConvert = isCompanionConvert;

            CI = DataStore.memoryClass.getData(pathAbs, classname);
            if(CI != null)dequeBI.add(CI);
            //上記の行はいずれ変更する
        }

        @Override
        public void visit(ClassOrInterfaceDeclaration md, Void arg) {
            NodeList<Modifier> modifiers = new NodeList<>();
            boolean isStatic = false;
            if (md.getModifiers().size() != 0) {
                modifiers = md.getModifiers();
                for (Modifier modifier : modifiers) {
                    if (modifier.toString().equals("static ")) isStatic = true;
                }
            }
            if (isStatic == isCompanionConvert) {
                OutClassVisitor visitor = new OutClassVisitor(indent + indent4, this.structure, this.rangeStructure);
                md.accept(visitor, null);
            }
        }

        @Override
        public void visit(MethodDeclaration md, Void arg) {
            //System.out.println("now: " + md.getRange().get());
            String methodName = md.getNameAsString();
            Range range = md.getRange().get();
            boolean flagConvert = false;
            System.out.println("now convert:" + methodName + " range:" + range + " structure:" + structure);
            MethodInfomation MI = (MethodInfomation) CI.getMemoryData(CI.blockStructure + "/" + methodName, range);
            if(isNonNullFlag) {
                if (MI != null) {
                    if(MI.access) flagConvert = true;
                    String targetField = MI.accessField;

                    /*for (String accessor : DataStore.memory_method_info.get(classname).keySet()) {
                        if (!(boolean) DataStore.memory_method_info.get(classname).get(accessor).get("access")) break;
                        if (methodName.equals(accessor)) {
                            String targetField = (String) DataStore.memory_method_info.get(classname).get(methodName).get("field");
                            for (String accessor2 : DataStore.memory_method_info.get(classname).keySet()) {
                                if (accessor.equals(accessor2)) continue;
                                String targetField2 = (String) DataStore.memory_method_info.get(classname).get(accessor2).get("field");
                                if (targetField.equals(targetField2)) {
                                    flagConvert = false;
                                    break LOOP;
                                }
                            }
                        }
                    }*/
                }
            }
            /*if(isNonNullFlag) {
                if (DataStore.memory_method_info.get(classname) != null) {
                    LOOP:
                    for (String accessor : DataStore.memory_method_info.get(classname).keySet()) {
                        if (!(boolean) DataStore.memory_method_info.get(classname).get(accessor).get("access")) break;
                        if (methodName.equals(accessor)) {
                            String targetField = (String) DataStore.memory_method_info.get(classname).get(methodName).get("field");
                            for (String accessor2 : DataStore.memory_method_info.get(classname).keySet()) {
                                if (accessor.equals(accessor2)) continue;
                                String targetField2 = (String) DataStore.memory_method_info.get(classname).get(accessor2).get("field");
                                if (targetField.equals(targetField2)) {
                                    flagConvert = false;
                                    break LOOP;
                                }
                            }
                        }
                    }
                }
            }*/
            if (!flagConvert) {
                boolean isStatic = false;
                if (DataStore.memory_method_info.get(classname) != null)
                    isStatic = (boolean) DataStore.memory_method_info.get(classname).get(methodName).get("static");
                if(MI != null) isStatic = MI.isStatic;
                if (isStatic == isCompanionConvert) {
                    if (md.getThrownExceptions().size() != 0) {
                        String expt = md.getThrownException(0).toString();
                        setOutputerln(indent4 + "@Throws(" + expt + "::class)");
                    }
                    NodeList<Modifier> modifiers = md.getModifiers();
                    String mod = "";
                    if (modifiers.size() != 0) {
                        for (Modifier modifier : modifiers) {
                            String tmp = modifier.toString();
                            if (tmp.equals("public ")) tmp = "";
                            if (tmp.equals("static ")) tmp = "";
                            if (tmp.equals("synchronized ")) setOutputerln(indent + indent4 + "@Synchronized");
                            mod = mod.concat(tmp);
                        }
                    }
                    String override = "";
                    if (md.getAnnotations().size() != 0) {
                        for (int i = 0; i < md.getAnnotations().size(); i++) {
                            if (md.getAnnotation(i).getNameAsString().equals("Override")) {
                                override = "override";
                            } else {
                                AnnotationVisitor annotationVisitor = new AnnotationVisitor(classname, indent);
                                md.getAnnotation(i).accept(annotationVisitor, null);
                            }
                        }
                    }
                    setOutputer(indent + indent4 + override + mod + "fun " + methodName + "(");
                    for (int i = 0; i < md.getParameters().size(); i++) {
                        if (i != 0) setOutputer(", ");
                        ParameterConvert(md.getParameter(i));
                    }
                    setOutputer(")");
                    if (!md.getType().isVoidType()) {
                        setOutputer(": ");
                        TypeVisitor Tvisitor = new TypeVisitor();
                        md.getType().accept(Tvisitor, null);
                        if(isNonNullFlag) setOutputer(Tvisitor.getRetType());
                        else setOutputer(Tvisitor.getRetTypeNullable());
                        /*if (!isNonNullFlag && md.getType().isPrimitiveType())
                            setOutputer(Tvisitor.getRetTypeNullable());
                        else setOutputer(Tvisitor.getRetType());

                         */
                    }
                    String structureThis = this.structure + "/" + methodName;
                    ArrayDeque<Range> rangeStructure = this.rangeStructure.clone();
                    rangeStructure.add(range);
                    VoidVisitor<?> visitor = new MakeBlockVisitor(classname, methodName, structureThis, range, rangeStructure, dequeBI, indent + indent4, indent + indent4);
                    md.accept(visitor, null);
                    setOutputer("\n");
                }
            }
        }

        @Override
        public void visit(FieldDeclaration md, Void arg) {
            NodeList<Modifier> modifiers = new NodeList<>();
            boolean isStatic = false;
            if (md.getModifiers().size() != 0) {
                modifiers = md.getModifiers();
                for (Modifier modifier : modifiers) {
                    if (modifier.toString().equals("static ")) isStatic = true;
                }
            }
            if (isStatic == isCompanionConvert) {
                //以下エラー回避のためのダミー行
                Range range = md.getRange().get();
                if(CI != null) range = CI.range;
                VariableVisitor visitor = new VariableVisitor(classname, "", structure, range, rangeStructure, dequeBI, false, indent + indent4, modifiers);
                for(VariableDeclarator VD:md.getVariables()){
                    VD.accept(visitor, null);
                    setOutputer("\n");
                }
            }
        }

        @Override
        public void visit(ConstructorDeclaration md, Void arg) {
            NodeList<Modifier> modifiers = new NodeList<>();
            ;
            boolean isStatic = false;
            if (md.getModifiers().size() != 0) {
                modifiers = md.getModifiers();
                for (Modifier modifier : modifiers) {
                    if (modifier.toString().equals("static ")) isStatic = true;
                }
            }
            if (isStatic == isCompanionConvert) {
                setOutputer(indent + indent4 + "constructor(");
                for (int i = 0; i < md.getParameters().size(); i++) {
                    if (i != 0) setOutputer(", ");
                    ParameterConvert(md.getParameter(i));
                }
                setOutputer(")");
                String structureThis = this.structure + "/" + md.getNameAsString();
                ArrayDeque<Range> rangeStructure = this.rangeStructure.clone();
                Range range = md.getRange().get();
                rangeStructure.add(range);
                VoidVisitor<?> visitor = new MakeBlockVisitor(classname, md.getNameAsString(), structureThis, range, rangeStructure, dequeBI, indent + indent4, indent + indent4);
                md.accept(visitor, null);
                setOutputer("\n");
            }
        }

        @Override
        public void visit(EnumDeclaration md, Void arg) {
            NodeList<Modifier> modifiers = new NodeList<>();
            boolean isStatic = false;
            if (md.getModifiers().size() != 0) {
                modifiers = md.getModifiers();
                for (Modifier modifier : modifiers) {
                    if (modifier.toString().equals("static ")) isStatic = true;
                }
            }
            if (isStatic == isCompanionConvert) {
                OutClassVisitor visitor = new OutClassVisitor(indent, structure, rangeStructure);
                md.accept(visitor, null);
            }
        }

        @Override
        public void visit(InitializerDeclaration md, Void arg) {
            if (md.isStatic() == isCompanionConvert) {
                setOutputer(indent + indent4 + "init");
                String structureThis = this.structure + "/Initializer";
                ArrayDeque<Range> rangeStructure = this.rangeStructure.clone();
                Range range = md.getRange().get();
                rangeStructure.add(range);
                MakeBlockVisitor visitor = new MakeBlockVisitor(classname, "Initializer", structureThis,  range, rangeStructure, dequeBI, indent + indent4, indent + indent4);
                md.getBody().accept(visitor, null);
                setOutputer("\n");
            }
        }
    }

    private static class AnnotationVisitor extends VoidVisitorAdapter<Void> {
        String classname = "";
        String indent = "";

        public AnnotationVisitor(String classname, String indent) {
            this.classname = classname;
            this.indent = indent;
        }

        @Override
        public void visit(MarkerAnnotationExpr md, Void arg) {
            if (!md.getNameAsString().equals("Override"))
                setOutputerln(indent + indent4 + md.toString());
        }

        @Override
        public void visit(NormalAnnotationExpr md, Void arg) {
            int loopSize = 1;
            NodeList<Expression> memValue = new NodeList<>();
            String ktAnnotation = "";
            for (MemberValuePair memberValuePair : md.getPairs()) {
                String MVPstr = memberValuePair.getNameAsString() + " = ";
                if (memberValuePair.getValue().isArrayInitializerExpr()) {
                    loopSize = memberValuePair.getValue().asArrayInitializerExpr().getValues().size();
                    memValue = memberValuePair.getValue().asArrayInitializerExpr().getValues();
                } else {
                    memValue.add(memberValuePair.getValue());
                }
                MVPstr = MVPstr + "[";
                for (int i = 0; i < loopSize; i++) {
                    if (i > 0) MVPstr = MVPstr + ", ";
                    MVPstr = MVPstr + AnnotationConverter(memValue.get(i));
                }
                ktAnnotation = ktAnnotation + MVPstr + "], ";
            }
            setOutputerln(indent + indent4 + "@" + md.getNameAsString() + "(" + ktAnnotation.substring(0, ktAnnotation.length() - 2) + ")");

        }

        @Override
        public void visit(SingleMemberAnnotationExpr md, Void arg) {
            int loopSize = 1;
            NodeList<Expression> memValue = new NodeList<>();
            String ktAnnotation = "";
            if (md.getMemberValue().isArrayInitializerExpr()) {
                loopSize = md.getMemberValue().asArrayInitializerExpr().getValues().size();
                memValue = md.getMemberValue().asArrayInitializerExpr().getValues();
            } else {
                memValue.add(md.getMemberValue());
            }
            for (int i = 0; i < loopSize; i++) {
                if (i > 0) ktAnnotation = ktAnnotation + ", ";
                ktAnnotation = ktAnnotation + AnnotationConverter(memValue.get(i));
            }
            setOutputerln(indent + indent4 + "@" + md.getNameAsString() + "(" + ktAnnotation + ")");

        }

        public String AnnotationConverter(Expression md) {
            if (md.isNameExpr()) {
                if (md.asNameExpr().getNameAsString().equals("METHOD"))
                    return "AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER";
                else if (md.asNameExpr().getNameAsString().equals("TYPE"))
                    return "AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS";
                else return md.toString();
            } else if (md.isFieldAccessExpr()) {
                if (md.asFieldAccessExpr().getNameAsString().equals("METHOD"))
                    return "AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER";
                else if (md.asNameExpr().getNameAsString().equals("TYPE"))
                    return "AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS";
                else if (md.asFieldAccessExpr().getScope().equals("ElementType"))
                    return "AnnotationTarget." + md.asFieldAccessExpr().getName();
                else return md.toString();
            } else return md.toString();
        }

    }

    private static class VariableVisitor extends VoidVisitorAdapter<Void> {
        String classname = "";
        String methodname = "";
        String structure = "";
        Range range = null;
        ArrayDeque<Range> rangeStructure = new ArrayDeque<>();
        ArrayDeque<BlockInfomation> dequeBI = new ArrayDeque<>();
        String indent = "";
        NodeList<Modifier> modifiers = new NodeList<>();
        boolean isLocal = false;
        boolean isWhile = false;

        private VariableVisitor(String classname, String methodname, String structure, Range range,
                                ArrayDeque<Range> rangeStructure, ArrayDeque<BlockInfomation> dequeBI,
                                boolean flag, String indent, NodeList<Modifier> modifiers, boolean isWhile) {
            this.classname = classname;
            this.methodname = methodname;
            this.structure = structure;
            this.range = range;
            this.rangeStructure = rangeStructure;
            this.dequeBI = dequeBI.clone();
            this.indent = indent;
            this.modifiers = modifiers;
            this.isLocal = flag;
            this.isWhile = isWhile;
        }

        private VariableVisitor(String classname, String methodname, String structure, Range range,
                                ArrayDeque<Range> rangeStructure, ArrayDeque<BlockInfomation> dequeBI,
                                boolean flag, String indent, NodeList<Modifier> modifiers) {
            this(classname, methodname, structure, range, rangeStructure, dequeBI, flag, indent, modifiers, false);
        }

        @Override
        public void visit(VariableDeclarator md, Void arg) {
            String name = md.getNameAsString();
            String dec = "var";
            String initial = "";
            int dim = 0;
            boolean assign = true;
            System.out.println("now convert:" + name + " range:" + range + " structure:" + structure);

            String mod = "";
            if (modifiers.size() != 0) {
                for (Modifier modifier : modifiers) {
                    String tmp = modifier.toString();
                    if (tmp.equals("public ")) tmp = "";
                    if (tmp.equals("static ")) tmp = "";
                    mod = mod.concat(tmp);
                }
            }

            BlockInfomation BI = dequeBI.peekLast();
            FieldInfomation FI = BI.getMemoryF().get(name);
            if(FI != null){
                dim = FI.dim;
                assign = FI.assign;
            }
            /*if (isLocal) {
                if (DataStore.memory_localValue_info.get(classname).get(range) != null) {
                    if (DataStore.memory_localValue_info.get(classname).get(range).get(name) != null) {
                        dim = Integer.parseInt(DataStore.memory_localValue_info.get(classname).get(range).get(name).get("dim"));
                        assign = Boolean.parseBoolean(DataStore.memory_localValue_info.get(classname).get(range).get(name).get("assign"));
                    }
                }
            } else {
                if (DataStore.memory_method_info.get(classname) != null) {
                    if (DataStore.memory_field_info.get(classname).get(name) != null) {
                        dim = Integer.parseInt(DataStore.memory_field_info.get(classname).get(name).get("dim"));
                        assign = Boolean.parseBoolean(DataStore.memory_field_info.get(classname).get(name).get("assign"));
                    }
                }
            }*/
            boolean isLambda = false;
            if (assign || isWhile) dec = "var";
            else dec = "val";
            setOutputer(indent + mod + dec + " " + name);
            if (md.getInitializer().isPresent()) {
                if (md.getInitializer().get().isLambdaExpr()) {
                    isLambda = true;
                    setOutputer(" = ");
                    TypeVisitor Tvisitor = new TypeVisitor();
                    md.getType().accept(Tvisitor, null);
                    setOutputer(Tvisitor.getRetType().replace("?", ""));
                } else {
                    setOutputer(": ");
                    TypeVisitor Tvisitor = new TypeVisitor();
                    md.getType().accept(Tvisitor, null);
                    if(isNonNullFlag) setOutputer(Tvisitor.getRetType());
                    else setOutputer(Tvisitor.getRetTypeNullable());
                    setOutputer(" = ");
                    AssignVisitor visitor =
                            new AssignVisitor(classname, structure + "/" + name, md.getRange().get(), rangeStructure, dequeBI, indent, md.getTypeAsString(), dim);
                    md.getInitializer().get().accept(visitor, null);
                }
            } else {
                if (md.getType().isPrimitiveType()) {
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
                    if(isNonNullFlag) setOutputer(Tvisitor.getRetType());
                    else setOutputer(Tvisitor.getRetTypeNullable());
                }
            }
            setOutputer(initial);
            if (isLambda) {
                AssignVisitor visitor
                        = new AssignVisitor(classname, structure + "/" + name, md.getRange().get(), rangeStructure, dequeBI, indent, md.getTypeAsString(), dim);
                md.getInitializer().get().accept(visitor, null);
                //MakeBlockVisitor visitor2 = new MakeBlockVisitor(classname, methodname, indent, indent);
                //LambdaExpr lambdaExpr = md.getInitializer().get().asLambdaExpr();
                //lambdaExpr.getBody().accept(visitor2, null);
            }
            boolean flagGS = false;
            if(false){
            //if (BI != null) {
                LOOP:
                for (Triplets<String, Range, BlockInfomation> accessorData: BI.getMemoryKind("Method")) {
                    String targetField = "";
                    boolean flagAC = false;
                    MethodInfomation accessor = (MethodInfomation) accessorData.getRightValue();
                    if (accessor != null) {
                        targetField = accessor.accessField;
                        flagAC = accessor.fix;
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
                /*if (isNonNullFlag) {
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
                                        CustomAccessorConvertor cac = new CustomAccessorConvertor(classname, structure, rangeStructure, indent + indent4, name, flagG, flagS);
                                        methodDeclaration.accept(cac, null);
                                    }
                                }
                            }
                        }
                    }
                }*/
            }
            //古いやつ
            /*if (DataStore.memory_method_info.get(classname) != null) {
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
                if (isLambda) {
                    md.getInitializer().get().accept(visitor, null);
                    //MakeBlockVisitor visitor2 = new MakeBlockVisitor(classname, methodname, indent, indent);
                    //LambdaExpr lambdaExpr = md.getInitializer().get().asLambdaExpr();
                    //lambdaExpr.getBody().accept(visitor2, null);
                }
                if(isNonNullFlag) {
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
                                        CustomAccessorConvertor cac = new CustomAccessorConvertor(classname, structure, rangeStructure, indent + indent4, name, flagG, flagS);
                                        methodDeclaration.accept(cac, null);
                                    }
                                }
                            }
                        }
                    }
                }
            }*/
        }


    }

    private static class MakeBlockVisitor extends VoidVisitorAdapter<Void> {
        String classname = "";
        String contentsName = "";
        String structure = "";
        Range range = null;
        ArrayDeque<Range> rangeStructure = new ArrayDeque<>();
        ArrayDeque<BlockInfomation> dequeBI = new ArrayDeque<>();
        String indent = "";
        String indentBefore = "";

        private MakeBlockVisitor(String classname, String contentsName, String structure, Range range,
                                 ArrayDeque<Range> rangeStructure, ArrayDeque<BlockInfomation> dequeBI,
                                 String indent, String indentBefore) {
            this.classname = classname;
            this.contentsName = contentsName;
            this.structure = structure;
            this.range = range;
            this.rangeStructure = rangeStructure;
            this.dequeBI = dequeBI.clone();
            this.indent = indent;
            this.indentBefore = indentBefore;
        }

        @Override
        public void visit(BlockStmt md, Void arg) {
            if (md.getStatements().size() != 0) {
                if (md.getStatement(0).isExplicitConstructorInvocationStmt()) {
                    setOutputer(":");
                    ECISconvert(md.getStatement(0).asExplicitConstructorInvocationStmt());
                }
            }
            setOutputerln("{");
            VoidVisitor<?> visitor = new InBlockVisitor(classname, contentsName, structure, range, rangeStructure, dequeBI, indent + indent4, false);
            md.accept(visitor, null);
            setOutputer(indentBefore + "}");
        }

        //ECIS = ExplicitConstructorInvocationStmt
        private void ECISconvert(ExplicitConstructorInvocationStmt ECIS) {
            String outEnclosing = "";
            if (ECIS.isThis()) outEnclosing = "this";
            else outEnclosing = "super";
            setOutputer(outEnclosing + "(");
            if (ECIS.getArguments().size() != 0) {
                for (int i = 0; i < ECIS.getArguments().size(); i++) {
                    if (i != 0) setOutputer(",");
                    AssignVisitor visitor = new AssignVisitor(classname, structure, range, rangeStructure, dequeBI, indent, "", 0);
                    ECIS.getArgument(i).accept(visitor, null);
                }
            }
            setOutputer(")");
        }

    }

    private static class InBlockVisitor extends VoidVisitorAdapter<Void> {
        String classname = "";
        String contentsName = "";
        String structure = "";
        Range range = null;
        ArrayDeque<Range> rangeStructure = new ArrayDeque<>();
        ArrayDeque<BlockInfomation> dequeBI = new ArrayDeque<>();
        String indent = "";
        boolean isElseIf = false;
        boolean isConvertFor = true;
        boolean isWhile = false;
        boolean isLambda = false;

        private InBlockVisitor(String classname, String contentsName, String structure, Range range,
                               ArrayDeque<Range> rangeStructure, ArrayDeque<BlockInfomation> dequeBI,
                               String indent, boolean flag, boolean isWhile) {
            this.classname = classname;
            this.contentsName = contentsName;
            this.structure = structure;
            this.range = range;
            this.rangeStructure = rangeStructure;
            this.dequeBI = dequeBI.clone();
            this.indent = indent;
            this.isElseIf = flag;
            this.isWhile = isWhile;
            BlockInfomation BI = dequeBI.peekLast().getMemoryData(structure, range);
            if(false){
            //if(classname.equals("Class10")){
                System.out.println("name:" + BI.name + " kind:" +BI.kind);
                for(Triplets<String, Range, BlockInfomation> tri:BI.getMemory().getDataList()){
                    System.out.println("st:" + tri.getLeftValue() + " range:" + tri.getCenterValue());
                    System.out.println("contents:" + tri.getRightValue().name);
                }
            }
            if(BI != null)this.dequeBI.add(BI);
        }

        private InBlockVisitor(String classname, String contentsName, String structure, Range range,
                               ArrayDeque<Range> rangeStructure, ArrayDeque<BlockInfomation> dequeBI,
                               String indent, boolean flag) {
            this(classname, contentsName, structure, range, rangeStructure, dequeBI, indent, flag, false);
        }

        private InBlockVisitor(String classname, String structure, Range range,
                               ArrayDeque<Range> rangeStructure, ArrayDeque<BlockInfomation> dequeBI,
                               String indent, boolean isLambda) {
            this(classname, "", structure, range, rangeStructure, dequeBI, indent, false, false);
            this.isLambda = isLambda;
        }

        /*@Override
        public void visit(LineComment md, Void arg) {
            setOutputerln(indent + md.toString());
        }

        @Override
        public void visit(JavadocComment md, Void arg) {
            setOutputerln(indent + md.toString());
        }

        @Override
        public void visit(BlockComment md, Void arg) {
            setOutputerln(indent + md.toString());
        }

         */

        @Override
        public void visit(LabeledStmt md, Void arg) {
            setOutputer(indent + md.getLabel().toString().replace(":", "@"));
            String indentTmp = indent;
            super.visit(md, arg);

        }

        @Override
        public void visit(ContinueStmt md, Void arg) {
            setOutputer(indent);
            AssignVisitor visitor = new AssignVisitor(classname, structure, range, rangeStructure, dequeBI, indent, "", 0);
            md.accept(visitor, null);
        }

        @Override
        public void visit(BreakStmt md, Void arg) {
            setOutputer(indent);
            AssignVisitor visitor = new AssignVisitor(classname, structure, range, rangeStructure, dequeBI, indent, "", 0);
            md.accept(visitor, null);
        }

        @Override
        public void visit(ReturnStmt md, Void arg) {
            if (isLambda) setOutputer(indent);
            if(md.getExpression().isPresent()){
                Range exRange = md.getExpression().get().getRange().get();
                AssignVisitor visitor = new AssignVisitor(classname, structure, exRange, rangeStructure, dequeBI, indent, "", 0);
                md.accept(visitor, null);
            } else {
                AssignVisitor visitor = new AssignVisitor(classname, structure, range, rangeStructure, dequeBI, indent, "", 0);
                md.accept(visitor, null);
            }
        }

        @Override
        public void visit(VariableDeclarationExpr md, Void arg) {
            NodeList<Modifier> modifiers = new NodeList<>();
            ;
            if (md.getModifiers().size() != 0) {
                modifiers = md.getModifiers();
            }
            VariableVisitor visitor = new VariableVisitor(classname, contentsName, structure, range, rangeStructure, dequeBI, true, indent, modifiers, isWhile);
            for (VariableDeclarator vd : md.getVariables()) {
                vd.accept(visitor, null);
                setOutputer("\n");
            }
        }

        @Override
        public void visit(UnaryExpr md, Void arg) {
            setOutputerln(indent + md);
        }

        @Override
        public void visit(LocalClassDeclarationStmt md, Void arg) {
            OutClassVisitor outClassVisitor = new OutClassVisitor(indent, structure, rangeStructure);
            md.getClassDeclaration().accept(outClassVisitor, null);
        }

        @Override
        public void visit(SynchronizedStmt md, Void arg) {
            setOutputer(indent + "synchronized(");
            setOutputer(md.getExpression().toString());
            setOutputer(")");
            String structureThis = this.structure + "/Synchronized";
            ArrayDeque<Range> rangeStructure = this.rangeStructure.clone();
            Range range = md.getRange().get();
            rangeStructure.add(range);
            MakeBlockVisitor visitor = new MakeBlockVisitor(classname, contentsName, structureThis, range, rangeStructure, dequeBI, indent, indent);
            md.getBody().accept(visitor, null);
            setOutputer("\n");
        }

        @Override
        public void visit(AssertStmt md, Void arg) {
            setOutputer(indent + "assert(" + md.getCheck() + ")");
            if (md.getMessage().isPresent()) {
                setOutputer(" {" + md.getMessage().get() + "}");
            }
            setOutputer("\n");
        }

        @Override
        public void visit(TryStmt md, Void arg) {
            setOutputer(indent + "try");
            MakeBlockVisitor visitor = new MakeBlockVisitor(classname, contentsName, structure, range, rangeStructure, dequeBI, indent, indent);
            md.getTryBlock().accept(visitor, null);
            for (CatchClause catchClause : md.getCatchClauses()) {
                String paramName = catchClause.getParameter().getNameAsString();
                Parameter parameter = catchClause.getParameter();
                if (parameter.getType().isUnionType()) {
                    UnionType unionType = (UnionType) parameter.getType();
                    for (ReferenceType ref : unionType.getElements()) {
                        setOutputer("catch(" + paramName + ": " + ref + ")");
                        catchClause.getBody().accept(visitor, null);
                    }
                } else {
                    setOutputer("catch(" + paramName + ": " + parameter.getTypeAsString() + ")");
                    catchClause.getBody().accept(visitor, null);
                }
            }
            if (md.getFinallyBlock().isPresent()) {
                setOutputer("finally");
                md.getFinallyBlock().get().accept(visitor, null);
                setOutputer("\n");
            }

        }

        @Override
        public void visit(AssignExpr md, Void arg) {
            //System.out.println(md.getRange().get().toString());
            String target = md.getTarget().toString();
            String arithmeticOperator = AssignOperator(md.getOperator().name());
            setOutputer(indent + target + arithmeticOperator);
            AssignVisitor visitor = new AssignVisitor(classname, structure, range, rangeStructure, dequeBI, indent, "", 0);
            md.getValue().accept(visitor, null);
            setOutputer("\n");
        }

        @Override
        public void visit(WhileStmt md, Void arg) {
            String condition = md.getCondition().toString();
            setOutputer(indent + "while(" + condition + ")");
            String structureThis = this.structure + "/While";
            ArrayDeque<Range> rangeStructure = this.rangeStructure.clone();
            Range range = md.getRange().get();
            rangeStructure.add(range);
            if (md.getBody().isBlockStmt()) {
                VoidVisitor<?> visitor = new MakeBlockVisitor(classname, contentsName, structureThis, range, rangeStructure, dequeBI, indent, indent);
                md.getBody().accept(visitor, null);
                setOutputer("\n");
            } else super.visit(md, arg);
        }

        @Override
        public void visit(IfStmt md, Void arg) {
            boolean isIndent = false;
            String condition = md.getCondition().toString();
            if (isElseIf) setOutputer("if(" + condition + ")");
            else setOutputer(indent + "if(" + condition + ")");
            String structureThis = this.structure + "/if";
            Range range = md.getThenStmt().getRange().get();
            ArrayDeque<Range> rangeStructure = this.rangeStructure.clone();
            rangeStructure.add(range);
            VoidVisitor<?> visitor = new MakeBlockVisitor(classname, "", structureThis, range, this.rangeStructure, dequeBI, indent, indent);
            if (md.getThenStmt().isBlockStmt()) {
                md.getThenStmt().accept(visitor, null);
                isIndent = true;
            } else {
                visitor = new InBlockVisitor(classname, contentsName, structureThis, range, this.rangeStructure, dequeBI, " ", false);
                md.getThenStmt().accept(visitor, null);
            }
            if (md.getElseStmt().isPresent()) {
                if (isIndent) setOutputer(" else ");
                else setOutputer(indent + "else");
                structureThis = this.structure + "/Else";
                range = md.getElseStmt().get().getRange().get();
                rangeStructure = this.rangeStructure.clone();
                rangeStructure.add(range);
                if (md.getElseStmt().get().isIfStmt())
                    visitor = new InBlockVisitor(classname, contentsName, structureThis, range, rangeStructure, dequeBI, indent, true);
                md.getElseStmt().get().accept(visitor, null);
            }
            setOutputer("\n");
        }

        @Override
        public void visit(MethodCallExpr md, Void arg) {
            setOutputer(indent);
            AssignVisitor visitor = new AssignVisitor(classname, structure, range, rangeStructure, dequeBI, indent, "", 0);
            md.accept(visitor, null);
            setOutputer("\n");
        }

        @Override
        public void visit(MethodReferenceExpr md, Void arg) {
            AssignVisitor visitor = new AssignVisitor(classname, structure, range, rangeStructure, dequeBI, indent, "", 0);
            md.accept(visitor, null);
        }

        @Override
        public void visit(SwitchStmt md, Void arg) {
            String selector = md.getSelector().toString();
            setOutputerln(indent + "when(" + selector + "){");
            String structureThis = this.structure + "/Switch";
            Range range = md.getRange().get();
            ArrayDeque<Range> rangeStructure = this.rangeStructure.clone();
            rangeStructure.add(range);
            BlockInfomation BI = dequeBI.peekLast().getMemoryData(structureThis, range);
            ArrayDeque<BlockInfomation> dequeBISwitch = dequeBI.clone();
            dequeBISwitch.add(BI);
            for (SwitchEntry entry : md.getEntries()) {
                String structureLabel = structureThis + "/";
                StringBuilder label = new StringBuilder();
                for(Expression expression:entry.getLabels()) label.append(expression);
                if(entry.getLabels().size() == 0) label.append("default");
                structureLabel = structureLabel + label;
                Range rangeEntry = entry.getRange().get();
                ArrayDeque<Range> rangeStructureEntry = rangeStructure.clone();
                rangeStructureEntry.add(rangeEntry);

                SwitchEntryVisitor visitor = new SwitchEntryVisitor(classname, structureLabel, rangeEntry, rangeStructureEntry, dequeBISwitch, indent + indent4, true);
                entry.accept(visitor, null);
            }
            setOutputerln(indent + "}");
        }

        @Override
        public void visit(DoStmt md, Void arg) {
            String condition = md.getCondition().toString();
            setOutputer(indent + "do");
            String structureThis = this.structure + "/Do-while";
            Range range = md.getRange().get();
            ArrayDeque<Range> rangeStructure = this.rangeStructure.clone();
            rangeStructure.add(range);
            if (md.getBody().isBlockStmt()) {
                VoidVisitor<?> visitor = new MakeBlockVisitor(classname, contentsName, structureThis, range, rangeStructure, dequeBI, indent, indent);
                md.getBody().accept(visitor, null);
            } else {
                super.visit(md, arg);
                setOutputer(indent);
            }
            setOutputerln("while(" + condition + ")");
        }

        @Override
        public void visit(SwitchExpr md, Void arg) {
            AssignVisitor visitor = new AssignVisitor(classname, structure, range, rangeStructure, dequeBI, indent, "", 0);
            md.accept(visitor, null);
            setOutputerln(indent + indent4 + indent4 + "}");
        }

        @Override
        public void visit(ForEachStmt md, Void arg) {
            String value = md.getVariable().getVariable(0).getNameAsString();
            String iterable = md.getIterable().toString();
            setOutputerln(indent + iterable + ".foreach{ " + value + " ->");
            String structureThis = this.structure + "/ForEach";
            Range range = md.getRange().get();
            ArrayDeque<Range> rangeStructure = this.rangeStructure.clone();
            rangeStructure.add(range);
            VoidVisitor<?> visitor = new InBlockVisitor(classname, "", structureThis, range, rangeStructure, dequeBI, indent + indent4, false);
            md.getBody().accept(visitor, null);
            setOutputerln(indent + "}");
        }

        @Override
        public void visit(ForStmt md, Void arg) {
            ForVisitor forvisitor = new ForVisitor();
            String structureThis = this.structure + "/For";
            Range range = md.getRange().get();
            ArrayDeque<Range> rangeStructure = this.rangeStructure.clone();
            rangeStructure.add(range);

            for (Expression initial : md.getInitialization()) initial.accept(forvisitor, null);
            HashMap<String, String> initialMap = forvisitor.dataReturn("initial");

            md.getCompare().get().accept(forvisitor, null);
            HashMap<String, String> compareMap = forvisitor.dataReturn("compare");

            for (Expression update : md.getUpdate()) update.accept(forvisitor, null);
            HashMap<String, String> updateMap = forvisitor.dataReturn("update");

            isConvertFor = (initialMap.size() == compareMap.size()) && (initialMap.size() == updateMap.size());

            //setOutputer(indent + "for(");
            for (Expression initial : md.getInitialization()) {
                InBlockVisitor visitor = new InBlockVisitor(classname, contentsName, structureThis, range, rangeStructure, dequeBI, indent, false, true);
                initial.accept(visitor, null);
            }
            setOutputer(indent + "while(");
            String compare = md.getCompare().get().toString();
            if (compare.equals("")) compare = "true";
            setOutputer(compare);
            //setOutputer(Forlooper(initialMap, compareMap, updateMap));
            setOutputer(")");
            setOutputerln("{");
            if (md.getBody().isBlockStmt()) {
                VoidVisitor<?> visitor = new InBlockVisitor(classname, contentsName, structureThis, range, rangeStructure, dequeBI,indent + indent4, false);
                md.getBody().accept(visitor, null);
            } else super.visit(md, arg);
            for (Expression update : md.getUpdate()) {
                setOutputerln(indent + indent4 + update.toString());
            }
            setOutputerln(indent + "}");
        }

        private String Forlooper(HashMap<String, String> initialMap, HashMap<String, String> compareMap, HashMap<String, String> updateMap) {
            String returns = "";
            for (String value : initialMap.keySet()) {
                String initial = initialMap.get(value);
                String compareRight = compareMap.get(value).split("-")[0];
                String operator = compareMap.get(value).split("-")[1];
                String update = updateMap.get(value);
                Pattern pattern = Pattern.compile("^[0-9]+$|-[0-9]+$");

                switch (operator) {
                    case "LESS":
                        if (pattern.matcher(compareRight).matches())
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

    private static class ForVisitor extends VoidVisitorAdapter<Void> {
        HashMap<String, String> initialMap = new HashMap<String, String>();
        HashMap<String, String> compareMap = new HashMap<String, String>();
        HashMap<String, String> updateMap = new HashMap<String, String>();

        public HashMap<String, String> dataReturn(String select) {
            return switch (select) {
                case "initial" -> initialMap;
                case "compare" -> compareMap;
                case "update" -> updateMap;
                default -> new HashMap<>();
            };
        }

        @Override
        public void visit(AssignExpr md, Void arg) {
            initialMap.put(md.getTarget().toString(), md.getValue().toString());
        }

        @Override
        public void visit(VariableDeclarator md, Void arg) {
            String initialValue = "";
            if (md.getInitializer().isPresent())
                initialValue = md.getInitializer().get().toString();
            initialMap.put(md.getNameAsString(), initialValue);
        }

        @Override
        public void visit(BinaryExpr md, Void arg) {
            compareMap.put(md.getLeft().toString(), md.getRight() + "-" + md.getOperator());
        }

        @Override
        public void visit(UnaryExpr md, Void arg) {
            updateMap.put(md.getExpression().toString(), md.getOperator().toString());
        }

    }

    private static class SwitchEntryVisitor extends VoidVisitorAdapter<Void> {
        String classname = "";
        String structure = "";
        Range range = null;
        ArrayDeque<Range> rangeStructure = new ArrayDeque<>();
        ArrayDeque<BlockInfomation> dequeBI = new ArrayDeque<>();
        String indent = "";
        boolean isStmt = false;

        private SwitchEntryVisitor(String classname, String structure, Range range,
                                   ArrayDeque<Range> rangeStructure, ArrayDeque<BlockInfomation> dequeBI,
                                   String indent, boolean flag) {
            this.classname = classname;
            this.structure = structure;
            this.range = range;
            this.rangeStructure = rangeStructure;
            this.dequeBI = dequeBI.clone();
            this.indent = indent;
            this.isStmt = flag;

            BlockInfomation BI = dequeBI.peekLast().getMemoryData(structure, range);
            if(BI != null)this.dequeBI.add(BI);
        }

        @Override
        public void visit(SwitchEntry md, Void arg) {
            String label = "";
            if (md.getLabels().size() != 0) label = md.getLabels().get(0).toString();
            else label = "else";
            setOutputer(indent + label + " -> ");
            if (isStmt) {
                setOutputerln("{");
                for (Statement statement : md.getStatements()) {
                    VoidVisitor<?> visitor = new InBlockVisitor(this.classname, "", this.structure, range, rangeStructure, dequeBI, indent + indent4, false);
                    statement.accept(visitor, null);
                }
                setOutputerln(indent + "}");
            } else {
                for (Statement statement : md.getStatements()) {
                    AssignVisitor visitor = new AssignVisitor(classname, structure, range, rangeStructure, dequeBI, "", "", 0);
                    statement.accept(visitor, null);
                    setOutputer("\n");
                }
            }
        }

    }

    private static class AssignVisitor extends VoidVisitorAdapter<Void> {
        String classname = "";
        String structure = "";
        Range range = null;
        ArrayDeque<Range> rangeStructure = new ArrayDeque<>();
        ArrayDeque<BlockInfomation> dequeBI = new ArrayDeque<>();
        String type = "";
        String indent = "";
        int dim = 0;
        String typeDef = "";
        boolean isArrayNullable = false;
        private boolean isEnumConvert = false;

        AssignVisitor(String classname, String structure, Range range, ArrayDeque<Range> rangeStructure,
                      ArrayDeque<BlockInfomation> dequeBI, String indent, String type, int dim) {
            this.classname = classname;
            this.structure = structure;
            this.range = range;
            this.rangeStructure = rangeStructure;
            this.dequeBI = dequeBI.clone();
            this.indent = indent;
            this.type = type;
            this.dim = dim;

            BlockInfomation BI = dequeBI.peekLast().getMemoryData(structure, range);
            if(classname.equals("Class3") && BI != null){
            System.out.println("name:" + BI.name + " kind:" +BI.kind);
            for(Triplets<String, Range, BlockInfomation> tri:BI.getMemory().getDataList()){
                System.out.println("st:" + tri.getLeftValue() + " range:" + tri.getCenterValue());
                System.out.println("contents:" + tri.getRightValue().name);
            }
        }
            if(BI != null)this.dequeBI.add(BI);
        }

        AssignVisitor(String structure, Range range, ArrayDeque<Range> rangeStructure,
                      ArrayDeque<BlockInfomation> dequeBI, String type) {
            this("", structure, range, rangeStructure, dequeBI, "", type, 0);
        }

        @Override
        public void visit(ContinueStmt md, Void arg) {
            String label = "";
            if (md.getLabel().isPresent()) label = "@" + md.getLabel().get().toString();
            setOutputerln("continue" + label);
        }

        @Override
        public void visit(BreakStmt md, Void arg) {
            String label = "";
            if (md.getLabel().isPresent()) label = "@" + md.getLabel().get().toString();
            setOutputerln("break" + label);
        }

        @Override
        public void visit(ReturnStmt md, Void arg) {
            setOutputer("return ");
            AssignVisitor visitor = new AssignVisitor(this.classname, structure, range, rangeStructure, dequeBI, indent + indent4, "", 0);
            md.getExpression().get().accept(visitor, null);
            setOutputer("\n");
        }

        @Override
        public void visit(ArrayCreationExpr md, Void arg) {
            String type = md.getElementType().toString().substring(0, 1).toUpperCase() + md.getElementType().toString().substring(1);
            TypeVisitor Tvisitor = new TypeVisitor();
            md.getElementType().accept(Tvisitor, null);
            if(isNonNullFlag) type = Tvisitor.getRetType();
            else type = Tvisitor.getRetTypeNullable();
            String dec = "";
            if (md.getElementType().isPrimitiveType()) {
                int size = md.getLevels().size();
                dec = type + "Array(" + md.getLevels().get(size - 1).toString().charAt(1) + ")";
                for (int i = size - 2; i >= 0; i--) {
                    dec = "Array(" + md.getLevels().get(i).toString().charAt(1) + ") {" + dec + "}";
                }
            } else {
                int size = md.getLevels().size();
                dec = "arrayOfNulls<" + type + ">(" + md.getLevels().get(size - 1).toString().charAt(1) + ")";
                for (int i = size - 2; i >= 0; i--) {
                    dec = "Array(" + md.getLevels().get(i).toString().charAt(1) + ") {" + dec + "}";
                }
            }
            setOutputer(dec);
        }

        @Override
        public void visit(ArrayInitializerExpr md, Void arg) {
            int size = md.getValues().size();
            String str = "";
            str = str.concat("arrayOf(");
            dim--;
            if (isPrimitiveArray(type) && dim == 0)
                str = str.replace("array",
                        type.replace("[]", "") + "Array");
            setOutputer(str);
            for (int i = 0; i < size; i++) {
                if (i != 0) setOutputer(", ");
                AssignVisitor visitor = new AssignVisitor(classname, structure, range, rangeStructure, dequeBI, indent, type, dim);
                md.getValues().get(i).accept(visitor, null);
            }
            setOutputer(")");
        }

        @Override
        public void visit(ObjectCreationExpr md, Void arg) {
            String name = md.getTypeAsString();
            String argument = "";
            if (md.getArguments().size() != 0) {
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
            if (md.getAnonymousClassBody().isPresent()) {
                setOutputerln(" object: " + name + "(" + argument + ")" + "{");
                InClassVisitor visitor = new InClassVisitor(indent, name, structure, rangeStructure, false);
                for (BodyDeclaration bd : md.getAnonymousClassBody().get()) {
                    bd.accept(visitor, null);
                }
                setOutputerln(indent + "}");

            } else setOutputer(name + "(" + argument + ")");
        }

        @Override
        public void visit(MethodCallExpr md, Void arg) {
            String scopeName = "";
            if (md.getScope().isPresent()) {
                String str = md.getScope().get() + "." + md.getNameAsString();
                if (str.equals("System.out.println")) ;
                else if (str.equals("System.out.print")) ;
                else {
                    AssignVisitor visitor = new AssignVisitor(classname, structure, range, rangeStructure, dequeBI, indent, type, dim);
                    md.getScope().get().accept(visitor, null);
                    setOutputer(".");
                }
            }
            String methodname = md.getNameAsString();
            boolean flagOutputNormal = true;
            /*if(isNonNullFlag) {
                if (methodname.matches("get[A-Z].*")) {
                    if (DataStore.memory_method_info.get(classname) != null) {
                        if (DataStore.memory_method_info.get(classname).get(methodname) != null) {
                            if ((boolean) DataStore.memory_method_info.get(classname).get(methodname).get("fix")) {
                                String property = (String) DataStore.memory_method_info.get(classname).get(methodname).get("field");
                                setOutputer(scopeName + property);
                                flagOutputNormal = false;
                            }
                        }
                    }
                } else if (methodname.matches("set[A-Z].*")) {
                    if (DataStore.memory_method_info.get(classname) != null) {
                        if (DataStore.memory_method_info.get(classname).get(methodname) != null) {
                            if ((boolean) DataStore.memory_method_info.get(classname).get(methodname).get("fix")) {
                                String property = (String) DataStore.memory_method_info.get(classname).get(methodname).get("field");
                                setOutputer(scopeName + property + " = ");
                                if (md.getArguments().size() != 0) {
                                    for (int i = 0; i < md.getArguments().size(); i++) {
                                        if (i != 0) setOutputer(",");
                                        AssignVisitor visitor = new AssignVisitor(classname, structure, range, rangeStructure, dequeBI, indent, type, dim);
                                        md.getArgument(i).accept(visitor, null);
                                    }
                                }
                                flagOutputNormal = false;
                            }
                        }
                    }
                }
            }*/
            if (flagOutputNormal) {
                if (methodname.matches("concat")) {
                    setOutputer(scopeName + " + ");
                } else {
                    setOutputer(scopeName + methodname + "(");
                }
                if (md.getArguments().size() != 0) {
                    for (int i = 0; i < md.getArguments().size(); i++) {
                        if (i != 0) setOutputer(",");
                        AssignVisitor visitor = new AssignVisitor(classname, structure, range, rangeStructure, dequeBI, indent, type, dim);
                        md.getArgument(i).accept(visitor, null);
                    }
                }
                setOutputer(")");
                if(false){
                //if (DataStore.memory_method_info.get(classname).get(methodname) != null) {
                    if (isNonNullFlag) {
                        if ((boolean) DataStore.memory_method_info.get(classname).get(methodname).get("nullable"))
                            setOutputer("!!");
                    } else {
                        if (!DataStore.memory_method_info.get(classname).get(methodname).get("type").equals("void"))
                            setOutputer("!!");
                    }
                    typeDef = (String) DataStore.memory_method_info.get(classname).get(methodname).get("type");

                }
            }
        }

        @Override
        public void visit(MethodReferenceExpr md, Void arg) {
            setOutputer(md.toString());
        }

        @Override
        public void visit(ConditionalExpr md, Void arg) {
            setOutputer("if(" + md.getCondition().toString());
            setOutputer(")");
            AssignVisitor trueVisitor = new AssignVisitor(classname, structure, range, rangeStructure, dequeBI, indent, type, dim);
            md.getThenExpr().accept(trueVisitor, null);
            setOutputer(" else ");
            AssignVisitor falseVisitor = new AssignVisitor(classname, structure, range, rangeStructure, dequeBI, indent, type, dim);
            md.getElseExpr().accept(falseVisitor, null);

        }

        @Override
        public void visit(BinaryExpr md, Void arg) {
            AssignVisitor visitor = new AssignVisitor(classname, structure, range, rangeStructure, dequeBI, indent, type, dim);
            md.getLeft().accept(visitor, null);
            String arithmeticOperator = BinaryOperator(md.getOperator().name());
            setOutputer(arithmeticOperator);
            visitor = new AssignVisitor(classname, structure, range, rangeStructure, dequeBI, indent, type, dim);
            md.getRight().accept(visitor, null);

        }

        @Override
        public void visit(EnclosedExpr md, Void arg) {
            setOutputer("(");
            super.visit(md, arg);
            setOutputer(")");
        }

        @Override
        public void visit(CastExpr md, Void arg) {
            String castType = md.getTypeAsString();
            super.visit(md, arg);
            setOutputer(" as " + castType.substring(0, 1).toUpperCase() + castType.substring(1));
        }

        @Override
        public void visit(LambdaExpr md, Void arg) {
            boolean allowFlag = false;
            setOutputer("{");

            if (md.getParameters().size() != 0) {
                for (int i = 0; i < md.getParameters().size(); i++) {
                    allowFlag = true;
                    if (i != 0) setOutputer(", ");
                    Parameter param = md.getParameter(i);
                    String paramName = param.getNameAsString();
                    String paramType = "";
                    TypeVisitor Tvisitor = new TypeVisitor();
                    param.getType().accept(Tvisitor, null);
                    if(isNonNullFlag) paramType = Tvisitor.getRetType();
                    else paramType = Tvisitor.getRetTypeNullable();
                    if (param.getTypeAsString().length() == 0)paramType = "Any";
                    //else paramType = ": " + param.getTypeAsString().substring(0, 1).toUpperCase() + param.getTypeAsString().substring(1);

                    /*if (param.getType().isArrayType()) {
                        paramType = ": Array<" + param.getType().toArrayType().get().getComponentType() + ">";
                    }

                     */
                    setOutputer(paramName + ": " + paramType);
                }
            }
            if (allowFlag) setOutputer(" -> ");
            setOutputer("\n");

            VoidVisitor<?> visitor = new InBlockVisitor(classname, structure, md.getRange().get(), rangeStructure, dequeBI, indent + indent4, true);
            md.getBody().accept(visitor, null);
            setOutputer(indent + "}");
        }

        @Override
        public void visit(ArrayAccessExpr md, Void arg) {
            AssignVisitor nameVisitor = new AssignVisitor(classname, structure, range, rangeStructure, dequeBI, indent, type, dim);
            md.getName().accept(nameVisitor, null);
            setOutputer("[");
            AssignVisitor indexVisitor = new AssignVisitor(classname, structure, range, rangeStructure, dequeBI, indent, type, dim);
            md.getIndex().accept(indexVisitor, null);
            setOutputer("]");
            if (isNonNullFlag) {
                if (isParentNodeArray(md)) {
                    isArrayNullable = nameVisitor.isArrayNullable();
                } else {
                    if (nameVisitor.isArrayNullable()) {
                        setOutputer("!!");
                    }
                }
            } else {
                if (!isParentNodeArray(md))
                    setOutputer("!!");
            }
            typeDef = nameVisitor.getTypeDef();
        }

        @Override
        public void visit(FieldAccessExpr md, Void arg) {
            AssignVisitor visitor = new AssignVisitor(classname, structure, range, rangeStructure, dequeBI, indent, type, dim);
            md.getScope().accept(visitor, null);
            setOutputer("." + md.getNameAsString());
            boolean flag = isParentNodeArray(md);
            boolean flag2 = visitor.isEnumConvert();
            if (!flag && !flag2)
                setOutputer("!!");
            ClassInfomation CIThis = DataStore.memoryClass.getData(pathAbs, classname);
            if (md.getScope().isThisExpr()) {
                //typeDef = DataStore.memory_field_info.get(classname).get(md.getNameAsString()).get("type");
                typeDef = CIThis.getMemoryF().get(md.getNameAsString()).type;
            } else if (md.getScope().isSuperExpr()) {
                //typeDef = DataStore.memory_field_info.get(DataStore.memory_extend.get(classname)).get(md.getNameAsString()).get("type");
                typeDef = DataStore.memoryClass.getData(pathAbs, CIThis.classEx).getMemoryF().get(md.getNameAsString()).type;
            } else if(flag2){
                typeDef = visitor.getTypeDef();
                isEnumConvert = !flag2;
            } else {
                String typeDefTmp = visitor.getTypeDef();
                //typeDef = DataStore.memory_field_info.get(typeDefTmp).get(md.getNameAsString()).get("type");
                typeDef = DataStore.memoryClass.getData(pathAbs, typeDefTmp).getMemoryF().get(md.getNameAsString()).type;
                if (flag) isArrayNullable = DataStore.memoryClass.getData(pathAbs, typeDefTmp).getMemoryF().get(md.getNameAsString()).nullable;
                    //isArrayNullable = Boolean.parseBoolean(DataStore.memory_field_info.get(typeDefTmp).get(md.getNameAsString()).get("nullable"));

                isEnumConvert
                        = isEnumClass(md.getNameAsString()) || isEnumClass(typeDef);
            }
        }

        @Override
        public void visit(NameExpr md, Void arg) {
            String name = md.getNameAsString();
            setOutputer(name);
            isEnumConvert = isEnumClass(name);
            if(isEnumConvert) typeDef = name;
            if (DataStore.memoryClass.getData(pathAbs, classname).getMemoryF().get(md.getNameAsString()) != null) {
                typeDef = DataStore.memoryClass.getData(pathAbs, classname).getMemoryF().get(md.getNameAsString()).type;
                isEnumConvert = isEnumConvert || isEnumClass(typeDef);
            }
            boolean flag = isParentNodeArray(md);
            /*if (!isNonNullFlag) {
                if (!flag || !isEnumConvert)
                    setOutputer("!!");
            }*/
            if (!flag && !isEnumConvert)
                setOutputer("!!");
            if (flag)isArrayNullable = DataStore.memoryClass.getData(pathAbs, classname).getMemoryF().get(md.getNameAsString()).nullable;
            //isArrayNullable = Boolean.parseBoolean(DataStore.memory_field_info.get(classname).get(md.getNameAsString()).get("nullable"));
        }

        @Override
        public void visit(SwitchExpr md, Void arg) {
            String selector = md.getSelector().toString();
            setOutputerln("when(" + selector + "){");
            String structureThis = this.structure + "/Switch";
            Range range = md.getRange().get();
            ArrayDeque<Range> rangeStructure = this.rangeStructure.clone();
            rangeStructure.add(range);
            BlockInfomation BI = dequeBI.peekLast().getMemoryData(structureThis, range);
            ArrayDeque<BlockInfomation> dequeBISwitch = dequeBI.clone();
            dequeBISwitch.add(BI);
            for (SwitchEntry entry : md.getEntries()) {
                String structureLabel = structureThis + "/";
                StringBuilder label = new StringBuilder();
                for(Expression expression:entry.getLabels()) label.append(expression);
                if(entry.getLabels().size() == 0) label.append("default");
                structureLabel = structureLabel + label;
                Range rangeEntry = entry.getRange().get();
                ArrayDeque<Range> rangeStructureEntry = rangeStructure.clone();
                rangeStructureEntry.add(rangeEntry);
                SwitchEntryVisitor visitor = new SwitchEntryVisitor(classname, structureLabel, rangeEntry, rangeStructureEntry, dequeBISwitch, indent, false);
                entry.accept(visitor, null);
            }
        }

        @Override
        public void visit(SuperExpr md, Void arg) {
            setOutputer(md.toString());
        }

        @Override
        public void visit(ThisExpr md, Void arg) {
            setOutputer(md.toString());
        }

        @Override
        public void visit(StringLiteralExpr md, Void arg) {
            setOutputer(md.toString());
        }

        @Override
        public void visit(IntegerLiteralExpr md, Void arg) {
            setOutputer(md.toString());
        }

        @Override
        public void visit(DoubleLiteralExpr md, Void arg) {
            setOutputer(md.toString());
        }

        @Override
        public void visit(BooleanLiteralExpr md, Void arg) {
            setOutputer(md.toString());
        }

        @Override
        public void visit(NullLiteralExpr md, Void arg) {
            setOutputer(md.toString());
        }

        @Override
        public void visit(CharLiteralExpr md, Void arg) {
            setOutputer(md.toString());
        }

        @Override
        public void visit(LongLiteralExpr md, Void arg) {
            setOutputer(md.toString());
        }

        public boolean isParentNodeArray(Expression md) {
            if (md.getParentNode().isPresent()) {
                return md.getParentNode().get().getClass().getSimpleName().equals("ArrayAccessExpr");
            }
            return false;
        }

        public boolean isEnumClass(String name){
            for(String classname:DataStore.memory_enum){
                if(classname.equals(name)) return true;
            }
            return false;
        }

        public String getTypeDef() {
            if (typeDef.contains("-array"))
                typeDef = typeDef.replace("-array", "");
            return this.typeDef;
        }

        public boolean isArrayNullable() {
            return this.isArrayNullable;
        }

        public boolean isEnumConvert(){
            return this.isEnumConvert;
        }

        public boolean isPrimitiveArray(String type) {
            String str = type.replace("[]", "");
            return switch (str) {
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

    private static class TypeVisitor extends VoidVisitorAdapter<Void> {
        private StringBuilder retType = new StringBuilder("");

        @Override//参照型はここ
        public void visit(ClassOrInterfaceType md, Void arg) {

            //setOutputer(md.getNameAsString());
            retType.append(md.getNameAsString());
            if (md.getTypeArguments().isPresent()) {

                retType.append("<");
                super.visit(md, arg);
                if(isNonNullFlag) retType = new StringBuilder(getRetType());
                else retType = new StringBuilder(getRetTypeNullable());
                retType.append(">");
                //setOutputer("<");

                //setOutputer(">");
                /*TypeVisitor visitor = new TypeVisitor();
                md.getTypeArguments().get().accept(visitor, null);
                if(isNonNullFlag) retType = retType + "<" + visitor.getRetType() + ">";
                else retType = retType + "<" + visitor.getRetTypeNullable() + ">";

                 */
            }
            //if (!isNonNullFlag) retType = new StringBuilder(getRetTypeNullable());
        }

        @Override//プリミティブ型
        public void visit(PrimitiveType md, Void arg) {
            String type = md.toString();
            //setOutputer(type.substring(0,1).toUpperCase() + type.substring(1));
            retType.append(type.substring(0, 1).toUpperCase()).append(type.substring(1));
            //if (!isNonNullFlag) retType = new StringBuilder(getRetTypeNullable());
        }

        @Override//componentTypeで配列の型を取得できる
        public void visit(ArrayType md, Void arg) {
            if (md.getComponentType().isPrimitiveType()) {
                /*
                super.visit(md,arg);
                setOutputer("Array");

                 */
                TypeVisitor visitor = new TypeVisitor();
                md.getComponentType().accept(visitor, null);
                retType.append(visitor.getRetType()).append("Array");
            } else {
                /*
                setOutputer("Array<");
                super.visit(md, arg);
                setOutputer(">");

                 */

                TypeVisitor visitor = new TypeVisitor();
                md.getComponentType().accept(visitor, null);
                retType.append("Array<").append(visitor.getRetTypeNullable()).append(">");
            }
            //if (!isNonNullFlag) retType = new StringBuilder(getRetTypeNullable());
        }

        @Override//extendedType：extend, superType：superが取得できる
        public void visit(WildcardType md, Void arg) {
            if (md.getExtendedType().isPresent()) {

                super.visit(md, arg);
                retType = new StringBuilder(getRetType());
                //if(isNonNullFlag) retType = new StringBuilder(getRetType());
                //else retType = new StringBuilder(getRetTypeNullable());
                /*TypeVisitor visitor = new TypeVisitor();
                md.getExtendedType().get().accept(visitor, null);
                if(isNonNullFlag) retType.append(visitor.getRetType());
                else retType.append(visitor.getRetTypeNullable());*/
            } else if (md.getSuperType().isPresent()) {
                //setOutputer("in ");
                retType.append("in　");
                super.visit(md, arg);
                retType = new StringBuilder(getRetType());
                //if(isNonNullFlag) retType = new StringBuilder(getRetType());
                //else retType = new StringBuilder(getRetTypeNullable());
                /*TypeVisitor visitor = new TypeVisitor();
                md.getSuperType().get().accept(visitor, null);
                if(isNonNullFlag) retType.append("in ").append(visitor.getRetType());
                else retType.append("in ").append(visitor.getRetTypeNullable());*/
            } else {
                retType.append("*");
                //setOutputer("*");
            }
        }

        @Override//引数の型
        public void visit(TypeParameter md, Void arg) {
            TypeVisitor visitor = new TypeVisitor();
            md.getTypeBound().get(0).accept(visitor, null);
            retType.append(md.getNameAsString()).append(":");
            if (isNonNullFlag) retType.append(visitor.getRetType());
            else retType.append(visitor.getRetTypeNullable());
            retType.append(">");
            //setOutputer("<" + md.getNameAsString() + ":" + md.getTypeBound().get(0) + ">");
        }

        public String getRetType() {
            return this.retType.toString();
        }

        public String getRetTypeNullable() {
            return this.retType.append("?").toString();
        }
    }

    private static class CustomAccessorConvertor extends InClassVisitor {
        String targetField = "";
        String indentDeep = "";
        boolean flagG = false;
        boolean flagS = false;
        String param = "";

        public CustomAccessorConvertor(String classname, String structure, ArrayDeque<Range> rangeStructure,
                                       String targetField, String indent, boolean flagG, boolean flagS) {
            super(indent, classname, structure, rangeStructure, false);
            this.targetField = targetField;
            this.indentDeep = indent + indent4;
            this.flagG = flagG;
            this.flagS = flagS;
        }
        @Override
        public void visit(MethodDeclaration md, Void arg) {
            Range range = md.getRange().get();
            String type = "";
            if (md.getParameters().size() != 0) {
                param = md.getParameter(0).getNameAsString();
                type = md.getParameter(0).getTypeAsString();
            }
            if (flagG) setOutputer(indent + "get()");
            if (flagS) setOutputer(indent + "set(" + param + ": " + type + ")");
            setOutputerln("{");//この下一時エラー削除
            InBlockVisitor_CAC visitor = new InBlockVisitor_CAC(classname, indent + indent4, "", range, rangeStructure, dequeBI, false, targetField, param);
            md.accept(visitor, null);
            setOutputerln(indent + "}");
        }

        /*@Override
        public void visit(BlockStmt md, Void arg) {
            if (flagG) setOutputer(indent + "get()");
            if (flagS) setOutputer(indent + "set(" + param + ": " + type + ")");
            setOutputerln("{");//この下一時エラー削除
            InBlockVisitor_CAC visitor = new InBlockVisitor_CAC(classname, indent + indent4, "", range, new ArrayDeque<>(), false, targetField, param);
            md.accept(visitor, null);
            setOutputerln(indent + "}");
        }*/


    }

    //CAC:CustomAccessorConverter
    private static class InBlockVisitor_CAC extends InBlockVisitor {
        String targetField = "";
        String param = "";

        public InBlockVisitor_CAC(String classname, String indent, String structure, Range range,
                                  ArrayDeque<Range> rangeStructure, ArrayDeque<BlockInfomation> dequeBI,
                                  boolean flag, String targetField, String param) {
            super(classname, "", structure, range, rangeStructure, dequeBI, indent, flag);
            this.targetField = targetField;
            this.param = param;
        }

        @Override
        public void visit(AssignExpr md, Void arg) {
            String target = md.getTarget().toString();
            if (target.equals(targetField) || target.equals("this." + targetField)) target = "field";
            String arithmeticOperator = BinaryOperator(md.getOperator().toString());
            setOutputer(indent + target + " " + arithmeticOperator + " ");
            AssignVisitor visitor = new AssignVisitor(structure, range, rangeStructure, dequeBI, "");
            md.getValue().accept(visitor, null);
        }

        @Override
        public void visit(ReturnStmt md, Void arg) {
            FieldAccessExpr fae = md.getExpression().get().asFieldAccessExpr();
            if (fae.getScope().toString().equals("this") && fae.getNameAsString().equals(targetField)) {
                String after = md.toString().replace("this." + targetField, "field");
                setOutputerln(indent + after.replace(";", ""));
            }
        }

    }

    public static String BinaryOperator(String operator) {
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

    public static String AssignOperator(String operator) {
        return switch (operator) {
            case "ASSIGN" -> " = ";
            case "PLUS" -> " += ";
            case "MINUS" -> " -= ";
            case "MULTIPLY" -> " *= ";
            case "DIVIDE" -> " /= ";
            case "BINARY_OR" -> " |= ";
            case "BINARY_AND" -> " &= ";
            case "XOR" -> " ^= ";
            case "REMAINDER" -> " %= ";
            case "LEFT_SHIFT" -> " <<= ";
            case "SIGNED_RIGHT_SHIFT" -> " >>= ";
            case "UNSIGNED_RIGHT_SHIFT" -> " >>>= ";
            default -> "";
        };
    }

    public static void ParameterConvert(Parameter parameter) {
        if (parameter.isVarArgs()) setOutputer("vararg ");
        setOutputer(parameter.getNameAsString() + ": ");
        TypeVisitor Tvisitor = new TypeVisitor();
        parameter.getType().accept(Tvisitor, null);
        if(isNonNullFlag) setOutputer(Tvisitor.getRetType());
        else setOutputer(Tvisitor.getRetTypeNullable());
    }

}

