import com.github.javaparser.ParseResult;
import com.github.javaparser.Range;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.type.*;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.utils.SourceRoot;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

public class J2KConverter {
    static String indent4 = "    ";
    static boolean isJvmStatic = false;
    static StringBuilder Outputer = new StringBuilder("");
    static HashMap<String, StringBuilder> OutputerStore = new HashMap<>();
    public static String pathAbs = "";
    public static String pathDir = "";

    private static boolean isNonNullFlag = false;

    public static void setOutputer(String output) {
        Outputer.append(output);
    }

    public static void setOutputerln(String output) {
        setOutputer(output);
        Outputer.append("\n");
    }

    public static void main(String[] args) throws IOException {
        String path_root = "/Users/kzm0308/IdeaProjects/J2KConverter/src/main/java/pac1";
        if (args.length == 0);
        else {
            int size = args.length;
            path_root = args[0];
            if (size > 1) {
                for(int i = 1;i < size;i++){
                    if (args[i].equals("-nn")) isNonNullFlag = true;
                }
            }
        }

        SourceRoot root = new SourceRoot(Paths.get(path_root));
        List<ParseResult<CompilationUnit>> results = root.tryToParse("");

        for(ParseResult<CompilationUnit> result:results){
            Outputer = new StringBuilder();
            CompilationUnit cu = result.getResult().get();
            CompilationUnit.Storage cus = cu.getStorage().get();
            pathAbs = cus.getPath().toString();
            pathDir = cus.getDirectory().toString();
            VoidVisitor<?> visitor = new OutClassVisitor("", "", new ArrayDeque<>());
            cu.accept(visitor, null);
            OutputerStore.put(pathAbs, Outputer);
        }

        for(String path:OutputerStore.keySet()){
            if(path.equals("/Users/kzm0308/IdeaProjects/J2KConverter/src/main/java/pac3/Class3.java")) {
                //ConvertOutputer.ConvertAction(path_root, path);
                System.out.println("~~~~~~~~~~~~~");
                System.out.println(OutputerStore.get(path));
            }
            /*ConvertOutputer.ConvertAction(path_root, path);
            //System.out.println("~~~~~~~~~~~~~");
            System.out.println(OutputerStore.get(path));

             */
        }

    }

    private static class OutClassVisitor extends VoidVisitorAdapter<Void> {
        String indent = "";
        String packageName = "";
        String structure = "";
        ArrayDeque<Range> rangeStructure = new ArrayDeque<>();
        ArrayList<ImportDeclaration> ImportNameList = new ArrayList<>();

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
            String mod = ModifierConvert(md.getModifiers());
            String name = md.getNameAsString();
            setOutputer(mod + "annotation class " + name + "(");
            String typeParams = "";
            for (int i = 0; i < md.getMembers().size(); i++) {
                if (i > 0) setOutputer(", ");
                AnnotationMemberDeclaration AMD = (AnnotationMemberDeclaration) md.getMember(i);
                setOutputer("val " + AMD.getNameAsString() + ": ");
                TypeVisitor typeVisitor = new TypeVisitor(true);
                AMD.getType().accept(typeVisitor, null);
                setOutputer(typeVisitor.getRetType());
            }
            setOutputerln(typeParams + ")");
        }

        @Override
        public void visit(ImportDeclaration md, Void arg) {
            ImportNameList.add(md);
        }

        @Override
        public void visit(ClassOrInterfaceDeclaration md, Void arg) {

            String classname = md.getNameAsString();
            ClassInformation CI = DataStore.memoryClass.getData(pathAbs, classname);
            if(DataStore.memoryStatic.get(pathAbs) && !isJvmStatic) {
                setOutputerln("import kotlin.jvm.JvmStatic");
                isJvmStatic = true;
            }
            for(ImportDeclaration importDeclaration:ImportNameList)
                setOutputer(importDeclaration.toString());
            String mod = ModifierConvert(md.getModifiers(), CI);
            String openclass = CI.isOpen ? "open " : "";
            String CorI = "class";
            if (md.isInterface()) CorI = "interface";
            setOutputer(indent + mod + openclass + CorI + " " + classname);
            String typeParams = "";
            boolean isDia = false;
            if (md.getTypeParameters().size() != 0) {
                isDia = true;
                TypeParameter TP = md.getTypeParameter(0);
                if (TP.getTypeBound().size() != 0)
                    typeParams = "<" + TP.getNameAsString() + " : " + TP.getTypeBound().get(0) + "?" + ">";
                else
                    typeParams = "<" + TP.getNameAsString() + ">";
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
            String structureThis = this.structure + "/" + md.getNameAsString();
            ArrayDeque<Range> rangeStructure = this.rangeStructure.clone();
            Range range = md.getRange().get();
            rangeStructure.add(range);


            //if(!classname.equals("Class6") ) {
            if(true){
                if(CI.isContainStatic){
                    setOutputerln(indent + indent4 + "companion object{");
                    for (BodyDeclaration<?> bodyDeclaration : md.getMembers()) {
                        InClassVisitor CompanionVisitor = new InClassVisitor(indent + indent4, md.getNameAsString(), structureThis, rangeStructure, true);
                        bodyDeclaration.accept(CompanionVisitor, null);
                    }
                    setOutputerln(indent + indent4 + "}");
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
            String classname = md.getNameAsString();
            ClassInformation CI = DataStore.memoryClass.getData(pathAbs, classname);
            String mod = ModifierConvert(md.getModifiers(), CI);
            setOutputerln(indent + indent4 + mod + "enum class " + md.getNameAsString() + "{");
            int size = md.getEntries().size();
            for (int i = 0; i < size; i++) {
                if (i != 0) setOutputer(",\n");
                setOutputer(indent + indent4 + indent4 + md.getEntry(i));
            }
            setOutputer(";\n");

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
        ArrayDeque<BlockInformation> dequeBI = new ArrayDeque<>();
        ClassInformation CI = null;
        BlockInformation BI = null;
        boolean isCompanionConvert = false;
        private boolean isAnonymous = false;

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
            isAnonymous = false;
        }

        private InClassVisitor(String indent, String classname, String structure, Range range,
                               ArrayDeque<Range> rangeStructure, ArrayDeque<BlockInformation> dequeBI) {
            this.indent = indent;
            this.classname = classname;
            this.structure = structure;
            this.rangeStructure = rangeStructure;
            this.dequeBI = dequeBI.clone();
            BI = dequeBI.peekLast();
            isAnonymous = true;
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
            String methodName = md.getNameAsString();
            Range range = md.getRange().get();
            boolean flagConvert = false;
            MethodInformation MI = null;
            if(isAnonymous) MI = (MethodInformation) BI.getMemoryData(structure + "/" + methodName, range);
            else MI = (MethodInformation) CI.getMemoryData(CI.blockStructure + "/" + methodName, range);
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
                if(MI != null) isStatic = MI.isStatic;
                if (isStatic == isCompanionConvert) {
                    if (md.getThrownExceptions().size() != 0) {
                        setOutputer(indent4 + "@Throws(");
                        for(int i = 0; i < md.getThrownExceptions().size(); i++) {
                            if(i > 0) setOutputer(", ");
                            TypeVisitor Tvisitor = new TypeVisitor(false);
                            md.getThrownException(i).accept(Tvisitor, null);
                            String expt = Tvisitor.getRetType();
                            setOutputer(expt + "::class");
                        }
                        setOutputerln(")");
                    }
                    String mod = ModifierConvert(md.getModifiers(), true, indent + indent4, MI);
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
                        TypeVisitor Tvisitor = new TypeVisitor(false);
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
                if(dequeBI.peekLast() != null) range = dequeBI.peekLast().range;
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
                else if (md.asFieldAccessExpr().getNameAsString().equals("TYPE"))
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
        ArrayDeque<BlockInformation> dequeBI = new ArrayDeque<>();
        String indent = "";
        NodeList<Modifier> modifiers = new NodeList<>();
        boolean isLocal = false;
        boolean isWhile = false;

        private VariableVisitor(String classname, String methodname, String structure, Range range,
                                ArrayDeque<Range> rangeStructure, ArrayDeque<BlockInformation> dequeBI,
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
                                ArrayDeque<Range> rangeStructure, ArrayDeque<BlockInformation> dequeBI,
                                boolean flag, String indent, NodeList<Modifier> modifiers) {
            this(classname, methodname, structure, range, rangeStructure, dequeBI, flag, indent, modifiers, false);
        }

        @Override
        public void visit(VariableDeclarator md, Void arg) {
            String name = md.getNameAsString();
            String dec = "var";
            String type = "";
            String initial = "";
            int dim = 0;
            boolean assign = true;

            BlockInformation BI = dequeBI.peekLast();
            FieldInformation FI = BI.getMemoryF().get(name);

            String openMod = "";
            String overrideMod = "";

            String mod = ModifierConvert(modifiers, FI);

            if(FI != null){
                dim = FI.dim;
                assign = FI.assign;
                openMod = FI.isOpen ? "open " : "";
                overrideMod = FI.isOverride ? "override " : "";
            }
            setOutputer(indent + openMod + overrideMod + mod);
            boolean isLambda = false;
            if (assign || isWhile) dec = "var";
            else dec = "val";
            setOutputer(dec + " " + name);
            TypeVisitor Tvisitor = new TypeVisitor(false);
            md.getType().accept(Tvisitor, null);
            if (md.getInitializer().isPresent()) {
                if (md.getInitializer().get().isLambdaExpr()) {
                    isLambda = true;
                    setOutputer(" = ");
                    //BlockInformation BIlambda = BI.getMemoryData(structure + "/" + name, md.getRange().get());
                    //if(BIlambda != null) setOutputer("object: ");
                    setOutputer(Tvisitor.getRetType().replace("?", ""));
                } else {
                    setOutputer(": ");
                    if(isNonNullFlag) setOutputer(Tvisitor.getRetType());
                    else setOutputer(Tvisitor.getRetTypeNullable());
                    setOutputer(" = ");
                    AssignVisitor visitor =
                            new AssignVisitor(classname, structure + "/" + name, md.getRange().get(), rangeStructure, dequeBI, indent, md.getTypeAsString(), dim);
                    md.getInitializer().get().accept(visitor, null);
                }
            } else {
                if (md.getType().isPrimitiveType()) {
                    setOutputer(": ");
                    if(isNonNullFlag) setOutputer(Tvisitor.getRetType());
                    else setOutputer(Tvisitor.getRetTypeNullable());
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

                    if(isNonNullFlag) setOutputer(Tvisitor.getRetType());
                    else setOutputer(Tvisitor.getRetTypeNullable());
                    initial = initial.concat(" = null");
                }
            }
            setOutputer(initial);
            if (isLambda) {
                AssignVisitor visitor
                        = new AssignVisitor(classname,structure + "/" + name, md.getRange().get(), rangeStructure, dequeBI, indent, md.getTypeAsString(), dim, FI);
                md.getInitializer().get().accept(visitor, null);
                //MakeBlockVisitor visitor2 = new MakeBlockVisitor(classname, methodname, indent, indent);
                //LambdaExpr lambdaExpr = md.getInitializer().get().asLambdaExpr();
                //lambdaExpr.getBody().accept(visitor2, null);
            }
            boolean flagGS = false;
            if(false){
            //if (BI != null) {
                LOOP:
                for (Triplets<String, Range, BlockInformation> accessorData: BI.getMemoryKind("Method")) {
                    String targetField = "";
                    boolean flagAC = false;
                    MethodInformation accessor = (MethodInformation) accessorData.getRightValue();
                    if (accessor != null) {
                        targetField = accessor.accessField;
                        flagAC = accessor.fix;
                    }
                    if (!targetField.equals(name)) continue;
                    /*for (String accessor2 : DataStore.memory_method_info.get(classname).keySet()) {
                        if (accessor.equals(accessor2)) continue;
                        String targetField2 = "";
                        if (DataStore.memory_method_info.get(classname).get(accessor2) != null)
                            targetField2 = (String) DataStore.memory_method_info.get(classname).get(accessor2).get("field");
                        if (targetField.equals(targetField2)) {
                            flagGS = flagAC && (boolean) DataStore.memory_method_info.get(classname).get(accessor2).get("fix");
                            break LOOP;
                        }
                    }

                     */
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
        }


    }

    private static class MakeBlockVisitor extends VoidVisitorAdapter<Void> {
        String classname = "";
        String contentsName = "";
        String structure = "";
        Range range = null;
        ArrayDeque<Range> rangeStructure = new ArrayDeque<>();
        ArrayDeque<BlockInformation> dequeBI = new ArrayDeque<>();
        String indent = "";
        String indentBefore = "";

        private MakeBlockVisitor(String classname, String contentsName, String structure, Range range,
                                 ArrayDeque<Range> rangeStructure, ArrayDeque<BlockInformation> dequeBI,
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

        @Override
        public void visit(EmptyStmt md, Void arg){
            setOutputerln("{}");
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
        ArrayDeque<BlockInformation> dequeBI = new ArrayDeque<>();
        String indent = "";
        boolean isElseIf = false;
        boolean isConvertFor = true;
        boolean isWhile = false;
        boolean isLambda = false;
        String typeLambda = "";

        boolean usedLabalIndent = true;
        String indentPlusLabel = "";
        private String afterLabelIndent(){
            if(usedLabalIndent) return indent;
            else {
                usedLabalIndent = true;
                return indentPlusLabel;
            }
        }

        private InBlockVisitor(String classname, String contentsName, String structure, Range range,
                               ArrayDeque<Range> rangeStructure, ArrayDeque<BlockInformation> dequeBI,
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
            BlockInformation BI = dequeBI.peekLast().getMemoryData(structure, range);
            if(BI != null)this.dequeBI.add(BI);
        }

        private InBlockVisitor(String classname, String contentsName, String structure, Range range,
                               ArrayDeque<Range> rangeStructure, ArrayDeque<BlockInformation> dequeBI,
                               String indent, boolean flag) {
            this(classname, contentsName, structure, range, rangeStructure, dequeBI, indent, flag, false);
        }

        private InBlockVisitor(String classname, String structure, Range range,
                               ArrayDeque<Range> rangeStructure, ArrayDeque<BlockInformation> dequeBI,
                               String indent, boolean isLambda, String type) {
            this(classname, "", structure, range, rangeStructure, dequeBI, indent, false, false);
            this.isLambda = isLambda;
            this.typeLambda = type;
        }

        private InBlockVisitor(String classname, String contentsName, String structure, Range range,
                               ArrayDeque<Range> rangeStructure, ArrayDeque<BlockInformation> dequeBI,
                               String indent, String label) {
            this(classname, contentsName, structure, range, rangeStructure, dequeBI, indent, false, false);
            usedLabalIndent = false;
            indentPlusLabel = label;
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
        public void visit(EmptyStmt md, Void arg){
            setOutputerln("{}");
        }

        @Override
        public void visit(LabeledStmt md, Void arg) {
            InBlockVisitor visitor = new InBlockVisitor(classname, contentsName, structure, range, this.rangeStructure, dequeBI, indent, indent + md.getLabel() + "@ ");
            md.getStatement().accept(visitor, null);
        }

        @Override
        public void visit(ContinueStmt md, Void arg) {
            setOutputer(indent);
            AssignVisitor visitor = new AssignVisitor(classname, structure, range, rangeStructure, dequeBI, indent + indent4, "", 0);
            md.accept(visitor, null);
        }

        @Override
        public void visit(BreakStmt md, Void arg) {
            setOutputer(indent);
            AssignVisitor visitor = new AssignVisitor(classname, structure, range, rangeStructure, dequeBI, indent + indent4, "", 0);
            md.accept(visitor, null);
        }

        @Override
        public void visit(ReturnStmt md, Void arg) {
            setOutputer(indent);
            if(md.getExpression().isPresent()){
                Range exRange = md.getExpression().get().getRange().get();
                String type = "";
                if(isLambda) type = typeLambda;
                AssignVisitor visitor = new AssignVisitor(classname, structure, exRange, rangeStructure, dequeBI, indent + indent4, type, 0, isLambda);
                md.accept(visitor, null);
            } else {
                AssignVisitor visitor = new AssignVisitor(classname, structure, range, rangeStructure, dequeBI, indent + indent4, "", 0, isLambda);
                md.accept(visitor, null);
            }
        }

        @Override
        public void visit(ThrowStmt md, Void arg) {
            setOutputer(indent);
            AssignVisitor visitor = new AssignVisitor(classname, structure, range, rangeStructure, dequeBI, indent, "", 0);
            md.accept(visitor, null);
        }

        @Override
        public void visit(UnaryExpr md, Void arg) {
            setOutputer(indent);
            AssignVisitor visitor = new AssignVisitor(classname, structure, range, rangeStructure, dequeBI, indent, "", 0);
            md.accept(visitor, null);
            setOutputer("\n");
        }

        @Override
        public void visit(VariableDeclarationExpr md, Void arg) {
            NodeList<Modifier> modifiers = new NodeList<>();

            if (md.getModifiers().size() != 0) {
                modifiers = md.getModifiers();
            }
            Range range = md.getRange().get();
            VariableVisitor visitor = new VariableVisitor(classname, contentsName, structure, range, rangeStructure, dequeBI, true, afterLabelIndent(), modifiers, isWhile);

            for (VariableDeclarator vd : md.getVariables()) {
                vd.accept(visitor, null);
                setOutputer("\n");
            }
        }

        @Override
        public void visit(LocalClassDeclarationStmt md, Void arg) {
            OutClassVisitor outClassVisitor = new OutClassVisitor(indent, structure, rangeStructure);
            md.getClassDeclaration().accept(outClassVisitor, null);
        }

        @Override
        public void visit(SynchronizedStmt md, Void arg) {
            setOutputer(indent + "synchronized(");
            //setOutputer(md.getExpression().toString());
            AssignVisitor expressionVisitor = new AssignVisitor(classname, structure, range, rangeStructure, dequeBI, indent, "", 0);
            md.getExpression().accept(expressionVisitor, null);
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
            String structure = this.structure;
            setOutputer(indent);
            AssignVisitor targetVisitor = new AssignVisitor(classname, structure, range, rangeStructure, dequeBI, indent, true);
            md.getTarget().accept(targetVisitor, null);

            AssignExpr.Operator operator = md.getOperator();
            if(md.getOperator().name().equals("ASSIGN") || !(md.getParentNode().get() instanceof AssignExpr)) {
                String arithmeticOperator = AssignOperator(operator.name());
                setOutputer(arithmeticOperator);
            } else {
                setOutputer(" = ");
                targetVisitor = new AssignVisitor(classname, structure, range, rangeStructure, dequeBI, indent, false);
                md.getTarget().accept(targetVisitor, null);
                String arithmeticOperator = BinaryOperator(operator.name());
                setOutputer(arithmeticOperator + " ");
            }

            if(md.getValue().isObjectCreationExpr()) structure = structure + "/" + md.getTarget().toString();
            AssignVisitor valueVisitor = new AssignVisitor(classname, structure, range, rangeStructure, dequeBI, indent, "", 0);
            md.getValue().accept(valueVisitor, null);
            setOutputer("\n");
        }

        @Override
        public void visit(WhileStmt md, Void arg) {
            String structureThis = this.structure + "/While";
            ArrayDeque<Range> rangeStructure = this.rangeStructure.clone();
            Range range = md.getRange().get();
            rangeStructure.add(range);

            setOutputer(afterLabelIndent() + "while(");
            AssignVisitor conditionVisitor = new AssignVisitor(classname, structure, range, rangeStructure, dequeBI, indent, "", 0);
            md.getCondition().accept(conditionVisitor, null);
            setOutputer(")");
            if (md.getBody().isBlockStmt()) {
                VoidVisitor<?> visitor = new MakeBlockVisitor(classname, contentsName, structureThis, range, rangeStructure, dequeBI, indent, indent);
                md.getBody().accept(visitor, null);
                setOutputer("\n");
            } else super.visit(md, arg);
        }

        @Override
        public void visit(IfStmt md, Void arg) {
            String structureThis = this.structure + "/if";
            Range range = md.getThenStmt().getRange().get();
            ArrayDeque<Range> rangeStructure = this.rangeStructure.clone();
            rangeStructure.add(range);

            boolean isIndent = false;
            if (isElseIf) setOutputer("if(");
            else setOutputer(afterLabelIndent() + "if(");
            AssignVisitor conditionVisitor = new AssignVisitor(classname, structure, range, rangeStructure, dequeBI, indent, "", 0);
            md.getCondition().accept(conditionVisitor, null);
            setOutputer(")");
            VoidVisitor<?> visitor = new MakeBlockVisitor(classname, "", structureThis, range, this.rangeStructure, dequeBI, indent, indent);
            if (md.getThenStmt().isBlockStmt()) {
                md.getThenStmt().accept(visitor, null);
                isIndent = true;
            } else {
                visitor = new InBlockVisitor(classname, contentsName, structureThis, range, this.rangeStructure, dequeBI, " ",  false);
                md.getThenStmt().accept(visitor, null);
            }
            if (md.getElseStmt().isPresent()) {
                if (isIndent) setOutputer(" else ");
                else setOutputer(indent + "else");
                structureThis = this.structure + "/Else";
                range = md.getElseStmt().get().getRange().get();
                rangeStructure = this.rangeStructure.clone();
                rangeStructure.add(range);
                if (md.getElseStmt().get().isBlockStmt())
                    visitor = new MakeBlockVisitor(classname, "", structureThis, range, this.rangeStructure, dequeBI, indent, indent);
                else
                    visitor = new InBlockVisitor(classname, contentsName, structureThis, range, rangeStructure, dequeBI, indent,md.getElseStmt().get().isIfStmt());

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
            /*setOutputer(indent);
            AssignVisitor visitor = new AssignVisitor(classname, structure, range, rangeStructure, dequeBI, indent, "", 0);
            md.accept(visitor, null);
            setOutputer("\n");

             */
        }

        @Override
        public void visit(SwitchStmt md, Void arg) {
            String selector = md.getSelector().toString();
            setOutputerln(indent + "when(" + selector + "){");
            String structureThis = this.structure + "/Switch";
            Range range = md.getRange().get();
            ArrayDeque<Range> rangeStructure = this.rangeStructure.clone();
            rangeStructure.add(range);
            BlockInformation BI = dequeBI.peekLast().getMemoryData(structureThis, range);
            ArrayDeque<BlockInformation> dequeBISwitch = dequeBI.clone();
            if(dequeBISwitch != null && BI != null) dequeBISwitch.add(BI);
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
            setOutputer(afterLabelIndent() + "do");
            String structureThis = this.structure + "/Do-while";
            Range range = md.getRange().get();
            ArrayDeque<Range> rangeStructure = this.rangeStructure.clone();
            rangeStructure.add(range);
            if (md.getBody().isBlockStmt()) {
                VoidVisitor<?> visitor = new MakeBlockVisitor(classname, contentsName, structureThis, range, rangeStructure, dequeBI, indent, indent);
                md.getBody().accept(visitor, null);
            } else {
                VoidVisitor<?> visitor = new InBlockVisitor(classname, contentsName, structureThis, range, this.rangeStructure, dequeBI, " ",  false);
                md.getBody().accept(visitor, null);
                setOutputer(indent);
            }
            setOutputer("while(");
            AssignVisitor conditionVisitor = new AssignVisitor(classname, structure, range, rangeStructure, dequeBI, indent, "", 0);
            md.getCondition().accept(conditionVisitor, null);
            setOutputerln(")");
        }

        @Override
        public void visit(SwitchExpr md, Void arg) {
            AssignVisitor visitor = new AssignVisitor(classname, structure, range, rangeStructure, dequeBI, indent, "", 0);
            md.accept(visitor, null);
            setOutputer("\n");
        }

        @Override
        public void visit(ForEachStmt md, Void arg) {
            String value = md.getVariable().getVariable(0).getNameAsString();
            String iterable = md.getIterable().toString();
            setOutputerln(afterLabelIndent() + iterable + ".foreach{ " + value + " ->");
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

            /*for (Expression initial : md.getInitialization()) initial.accept(forvisitor, null);
            HashMap<String, String> initialMap = forvisitor.dataReturn("initial");

            md.getCompare().get().accept(forvisitor, null);
            HashMap<String, String> compareMap = forvisitor.dataReturn("compare");

            for (Expression update : md.getUpdate()) update.accept(forvisitor, null);
            HashMap<String, String> updateMap = forvisitor.dataReturn("update");

            isConvertFor = (initialMap.size() == compareMap.size()) && (initialMap.size() == updateMap.size());

            //setOutputer(indent + "for(");

             */
            for (Expression initial : md.getInitialization()) {
                InBlockVisitor visitor = new InBlockVisitor(classname, contentsName, structureThis, range, rangeStructure, dequeBI, indent, false, true);
                initial.accept(visitor, null);
            }
            setOutputer(afterLabelIndent() + "while(");
            String compare = md.getCompare().get().toString();
            if (compare.equals("")) {
                compare = "true";
                setOutputer(compare);
            } else {
                AssignVisitor compareVisitor = new AssignVisitor(classname, structure, range, rangeStructure, dequeBI, indent, "", 0);
                md.getCompare().get().accept(compareVisitor, null);
            }
            //setOutputer(Forlooper(initialMap, compareMap, updateMap));
            setOutputer(")");
            setOutputerln("{");
            if (md.getBody().isBlockStmt()) {
                VoidVisitor<?> visitor = new InBlockVisitor(classname, contentsName, structureThis, range, rangeStructure, dequeBI,indent + indent4, false);
                md.getBody().accept(visitor, null);
            } else super.visit(md, arg);
            for (Expression update : md.getUpdate()) {

                setOutputer(indent + indent4);
                AssignVisitor UpdateVisitor = new AssignVisitor(classname, structure, range, rangeStructure, dequeBI, indent, "", 0);
                update.accept(UpdateVisitor, null);
                setOutputer("\n");
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

        @Override
        public void visit(SuperExpr md, Void arg) {
            setOutputerln(indent + md.toString());
        }

        @Override
        public void visit(ThisExpr md, Void arg) {
            setOutputerln(indent + md.toString());
        }

        @Override
        public void visit(StringLiteralExpr md, Void arg) {
            setOutputerln(indent + md.toString());
        }

        @Override
        public void visit(IntegerLiteralExpr md, Void arg) {
            setOutputerln(indent + md.toString());
        }

        @Override
        public void visit(DoubleLiteralExpr md, Void arg) {
            setOutputerln(indent + md.toString());
        }

        @Override
        public void visit(BooleanLiteralExpr md, Void arg) {
            setOutputerln(indent + md.toString());
        }

        @Override
        public void visit(NullLiteralExpr md, Void arg) {
            setOutputerln(indent + md.toString());
        }

        @Override
        public void visit(CharLiteralExpr md, Void arg) {
            setOutputerln(indent + md.toString());
        }

        @Override
        public void visit(LongLiteralExpr md, Void arg) {
            setOutputerln(indent + md.toString());
        }

    }

    private static class InBlockVisitorSwitch extends InBlockVisitor {

        private InBlockVisitorSwitch(String classname, String contentsName, String structure, Range range,
                                     ArrayDeque<Range> rangeStructure, ArrayDeque<BlockInformation> dequeBI,
                                     String indent, boolean flag) {
            super(classname, contentsName, structure, range, rangeStructure, dequeBI, indent, flag);
        }

        @Override
        public void visit(BreakStmt md, Void arg){

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
        ArrayDeque<BlockInformation> dequeBI = new ArrayDeque<>();
        String indent = "";
        boolean isStmt = false;

        private SwitchEntryVisitor(String classname, String structure, Range range,
                                   ArrayDeque<Range> rangeStructure, ArrayDeque<BlockInformation> dequeBI,
                                   String indent, boolean flag) {
            this.classname = classname;
            this.structure = structure;
            this.range = range;
            this.rangeStructure = rangeStructure;
            this.dequeBI = dequeBI.clone();
            this.indent = indent;
            this.isStmt = flag;

            BlockInformation BI = dequeBI.peekLast().getMemoryData(structure, range);
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
                    VoidVisitor<?> visitor = new InBlockVisitorSwitch(this.classname, "", this.structure, range, rangeStructure, dequeBI, indent + indent4, false);
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
        ArrayDeque<BlockInformation> dequeBI = new ArrayDeque<>();
        String type = "";
        String indent = "";
        int dim = 0;
        String typeDef = "";
        boolean isArrayNullable = false;
        private boolean isEnumConvert = false;
        private boolean isLambda = false;
        private FieldInformation FI = null;
        private boolean isWriteTarget = false;

        AssignVisitor(String classname, String structure, Range range, ArrayDeque<Range> rangeStructure,
                      ArrayDeque<BlockInformation> dequeBI, String indent, String type, int dim) {
            this.classname = classname;
            this.structure = structure;
            this.range = range;
            this.rangeStructure = rangeStructure;
            this.dequeBI = dequeBI.clone();
            this.indent = indent;
            this.type = type;
            this.dim = dim;

            BlockInformation BI = dequeBI.peekLast().getMemoryData(structure, range);
            if(BI != null)this.dequeBI.add(BI);
        }

        AssignVisitor(String classname, String structure, Range range, ArrayDeque<Range> rangeStructure,
                      ArrayDeque<BlockInformation> dequeBI, String indent, String type, int dim, boolean isLambda){
            this(classname, structure, range, rangeStructure, dequeBI, "", type, dim);
            this.isLambda = isLambda;
        }

        AssignVisitor(String classname, String structure, Range range, ArrayDeque<Range> rangeStructure,
                      ArrayDeque<BlockInformation> dequeBI, String indent, String type, int dim, FieldInformation FI) {
            this(classname, structure, range, rangeStructure, dequeBI, "", type, dim);
            this.FI = FI;
        }

        AssignVisitor(String structure, Range range, ArrayDeque<Range> rangeStructure,
                      ArrayDeque<BlockInformation> dequeBI, String type) {
            this("", structure, range, rangeStructure, dequeBI, "", type, 0);
        }

        AssignVisitor(String classname, String structure, Range range, ArrayDeque<Range> rangeStructure,
                      ArrayDeque<BlockInformation> dequeBI, String type, boolean flag) {
            this(classname, structure, range, rangeStructure, dequeBI, "", type, 0);
            isWriteTarget = flag;
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
            setOutputer("return");
            if(isLambda) setOutputer("@" + type);
            setOutputer(" ");
            if(md.getExpression().isPresent()){
                AssignVisitor visitor = new AssignVisitor(this.classname, structure, range, rangeStructure, dequeBI, indent + indent4, "", 0);
                md.getExpression().get().accept(visitor, null);
            }
            setOutputer("\n");
        }

        @Override
        public void visit(ThrowStmt md, Void arg) {
            setOutputer("throw ");
            AssignVisitor visitor = new AssignVisitor(this.classname, structure, range, rangeStructure, dequeBI, indent + indent4, "", 0);
            md.getExpression().accept(visitor, null);
            setOutputer("\n");
        }

        @Override
        public void visit(UnaryExpr md, Void arg) {
            UnaryExpr.Operator operator = md.getOperator();
            boolean isSign = operator.asString().equals("+") || operator.asString().equals("-");
            if(operator.isPrefix() && isSign) setOutputer(operator.asString());
            AssignVisitor visitor = new AssignVisitor(classname, structure, range, rangeStructure, dequeBI, indent, true);
            md.getExpression().accept(visitor, null);

            if(!isSign) {
                setOutputer(" = ");
                visitor = new AssignVisitor(classname, structure, range, rangeStructure, dequeBI, indent, false);
                md.getExpression().accept(visitor, null);
                setOutputer(operator.asString().charAt(0) + " 1");
                //if(operator.isPostfix()) setOutputer(operator.asString());
            }
        }

        @Override
        public void visit(InstanceOfExpr md, Void arg){
            AssignVisitor visitor = new AssignVisitor(this.classname, structure, range, rangeStructure, dequeBI, indent + indent4, "", 0);
            md.getExpression().accept(visitor, null);
            setOutputer(" is ");
            TypeVisitor Tvisitor = new TypeVisitor(false);
            md.getType().accept(Tvisitor, null);
            setOutputer(Tvisitor.getRetType());
        }

        @Override
        public void visit(ArrayCreationExpr md, Void arg) {
            String type = md.getElementType().toString().substring(0, 1).toUpperCase() + md.getElementType().toString().substring(1);
            TypeVisitor Tvisitor = new TypeVisitor(false);
            md.getElementType().accept(Tvisitor, null);
            String dec = "";
            if (md.getElementType().isPrimitiveType()) {
                int size = md.getLevels().size();
                type = Tvisitor.getRetType();
                dec = type + "Array(" + md.getLevels().get(size - 1).toString().charAt(1) + ")";
                for (int i = size - 2; i >= 0; i--) {
                    dec = "Array(" + md.getLevels().get(i).toString().charAt(1) + ") {" + dec + "}";
                }
            } else {
                int size = md.getLevels().size();
                if(isNonNullFlag) type = Tvisitor.getRetType();
                else type = Tvisitor.getRetTypeNullable();
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
            StringBuilder argument = new StringBuilder();
            int size = md.getArguments().size();
            if (size != 0) {
                String tmp = md.getArgument(0).toString();
                String[] str = tmp.split("\\.");
                for (int i = 0; i < str.length; i++) {
                    String substr = "";
                    if (i != 0) substr = substr.concat(".");
                    if (str[i].equals("in"))
                        substr = substr.concat(str[i].replace("in", "`in`"));
                    else substr = substr.concat(str[i]);
                    argument.append(substr);
                }
            }
            if (md.getAnonymousClassBody().isPresent()) {
                setOutputer(" object: " + name);
                if(size > 0)setOutputer("(" + argument + ")");
                setOutputerln("{");
                InClassVisitor visitor = new InClassVisitor(indent, name, structure, range, rangeStructure, dequeBI);
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
                        if(md.getArgument(i).isObjectCreationExpr()) structure = structure + "/" + md.getNameAsString();
                        AssignVisitor visitor = new AssignVisitor(classname, structure, range, rangeStructure, dequeBI, indent, type, dim);
                        md.getArgument(i).accept(visitor, null);
                    }
                }
                setOutputer(")");
                boolean isGetInfo = false;
                if(dequeBI.peekLast() != null){
                    BlockInformation BI = dequeBI.peekLast();
                    if (BI.kind.equals("Methods")) {
                        MethodInformation MI = (MethodInformation) BI;
                        isGetInfo = true;
                        if (isNonNullFlag) {
                            if (MI.nullable)
                                setOutputer("!!");
                        } else {
                            if (!MI.type.equals("void"))
                                setOutputer("!!");
                        }
                        typeDef = MI.type;
                    }
                }
                if(!isGetInfo)setOutputer("!!");

            }
        }

        @Override
        public void visit(MethodReferenceExpr md, Void arg) {
            /*setOutputer("{");
            setOutputer(md.toString());
            setOutputer("}");

             */
        }

        @Override
        public void visit(ConditionalExpr md, Void arg) {
            setOutputer("if(");
            AssignVisitor conditionVisitor = new AssignVisitor(classname, structure, range, rangeStructure, dequeBI, indent, type, dim);
            md.getCondition().accept(conditionVisitor, null);
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
            boolean notExist = true;
            String indentThis = indent + indent4;
            setOutputer("{");
            //interface search

            /*MethodInformation MI = null;
            if(this.FI != null){
                ClassInformation CI = DataStore.ClassCheck(pathDir, FI.type);
                if(CI != null){
                    notExist = false;
                    ArrayList<Triplets<String, Range, BlockInformation>> triplets
                            = CI.getMemoryKind("Method");
                    MI = (MethodInformation) triplets.get(0).getRightValue();

                    setOutputer("\n" + indentThis + "override fun " + MI.name + "(");
                }
            }*/

            if (md.getParameters().size() != 0) {
                for (int i = 0; i < md.getParameters().size(); i++) {
                    allowFlag = !notExist;
                    if (i != 0) setOutputer(", ");
                    Parameter param = md.getParameter(i);
                    String paramName = param.getNameAsString();
                    String paramType = "";
                    TypeVisitor Tvisitor = new TypeVisitor(false);
                    param.getType().accept(Tvisitor, null);
                    if(isNonNullFlag) paramType = Tvisitor.getRetType();
                    else paramType = Tvisitor.getRetTypeNullable();
                    if (param.getTypeAsString().length() == 0)paramType = "Any?";
                    //else paramType = ": " + param.getTypeAsString().substring(0, 1).toUpperCase() + param.getTypeAsString().substring(1);

                    /*if (param.getType().isArrayType()) {
                        paramType = ": Array<" + param.getType().toArrayType().get().getComponentType() + ">";
                    }

                     */
                    setOutputer(paramName + ": " + paramType);
                }
            }
            if (allowFlag) setOutputer(" -> ");
            else setOutputer("):" + "{");
            setOutputer("\n");

            String indentLambda = notExist ? indentThis : indentThis + indent4;
            VoidVisitor<?> visitor = new InBlockVisitor(classname, structure, md.getRange().get(), rangeStructure, dequeBI, indentLambda, true, type);
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
                if(!isWriteTarget)setOutputer("!!");
            }
            typeDef = nameVisitor.getTypeDef();
        }

        @Override
        public void visit(FieldAccessExpr md, Void arg) {
            AssignVisitor visitor = new AssignVisitor(classname, structure, range, rangeStructure, dequeBI, indent, type, dim);
            md.getScope().accept(visitor, null);
            String name = md.getNameAsString();
            boolean flag = isParentNodeArray(md);
            boolean flag2 = visitor.isEnumConvert();
            boolean isGetInfoThis = false;
            ClassInformation CIThis = DataStore.memoryClass.getData(pathAbs, classname);
            if(CIThis != null)  isGetInfoThis = true;
            if (md.getScope().isThisExpr() && isGetInfoThis) {
                if(CIThis.getMemoryF().get(name) != null){
                    typeDef = CIThis.getMemoryF().get(name).type;
                }
            } else if (md.getScope().isSuperExpr() && isGetInfoThis) {
                ClassInformation CIEx = DataStore.memoryClass.getData(pathAbs, CIThis.classEx);
                if(CIEx != null){
                    if(CIEx.getMemoryF().get(name) != null){
                        typeDef =
                                DataStore.memoryClass.getData(pathAbs, CIThis.classEx).getMemoryF().get(name).type;
                    }
                }
            } else if(flag2){
                typeDef = visitor.getTypeDef();
                isEnumConvert = !flag2;
            } else {
                String typeDefTmp = visitor.getTypeDef();
                if(!typeDefTmp.equals("") && !typeDefTmp.equals(null)){
                    if(DataStore.memoryClass.getData(pathAbs, typeDefTmp) != null){
                        ClassInformation CI = DataStore.memoryClass.getData(pathAbs, typeDefTmp);
                        if(CI.getMemoryF().get(name)!= null){
                            typeDef = CI.getMemoryF().get(name).type;
                            if (flag)isArrayNullable = CI.getMemoryF().get(name).nullable;

                        }
                    } else {
                        String importStr = DataStore.searchImport(typeDefTmp, pathAbs);
                        String pathImport = DataStore.memoryClassLibrary.get(importStr);
                        ClassInformation CI = DataStore.memoryClass.getData(pathImport, typeDefTmp);
                        if(CI != null) {
                            if (CI.getMemoryF().get(name) != null) {
                                typeDef = CI.getMemoryF().get(name).type;
                                if (flag) isArrayNullable = CI.getMemoryF().get(name).nullable;
                            }
                        }
                    }
                }
                isEnumConvert
                        = isEnumClass(name) || isEnumClass(typeDef);
            }
            setOutputer("." + name);
            if (!flag2 || !isNonNullFlag) {
                if(!isWriteTarget)setOutputer("!!");
            }
        }

        @Override
        public void visit(NameExpr md, Void arg) {
            String name = md.getNameAsString();
            isEnumConvert = isEnumClass(name);
            if(isEnumConvert) typeDef = name;
            boolean flag = isParentNodeArray(md);
            if(DataStore.memoryClass.getData(pathAbs, classname) != null){
                ClassInformation CI = DataStore.memoryClass.getData(pathAbs, classname);
                if(CI.getMemoryF().get(name)!= null){
                    typeDef = CI.getMemoryF().get(name).type;
                    isEnumConvert = isEnumConvert || isEnumClass(typeDef);
                    if (flag)isArrayNullable = CI.getMemoryF().get(name).nullable;

                }
            }
            setOutputer(name);
            if (!isEnumConvert || !isNonNullFlag) {
                String importStr = DataStore.searchImport(name, pathAbs);
                if(!isWriteTarget && importStr == null){
                    if(DataStore.memoryClass.getData(pathAbs, name) == null)setOutputer("!!");
                }
            }
        }

        @Override
        public void visit(SwitchExpr md, Void arg) {
            String selector = md.getSelector().toString();
            setOutputerln("when(" + selector + "){");
            String structureThis = this.structure + "/Switch";
            Range range = md.getRange().get();
            ArrayDeque<Range> rangeStructure = this.rangeStructure.clone();
            rangeStructure.add(range);
            BlockInformation BI = dequeBI.peekLast().getMemoryData(structureThis, range);
            ArrayDeque<BlockInformation> dequeBISwitch = dequeBI.clone();
            System.out.println(structureThis + " range:" + range);
            //if(dequeBISwitch != null && BI != null)
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
                SwitchEntryVisitor visitor = new SwitchEntryVisitor(classname, structureLabel, rangeEntry, rangeStructureEntry, dequeBISwitch, indent + indent4 + indent4, false);
                entry.accept(visitor, null);
            }
            String indentSwitch = indent + indent4;
            setOutputer(indentSwitch + "}");
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
            if(DataStore.memoryClass.getData(pathAbs, name) != null){
                ClassInformation CI = DataStore.memoryClass.getData(pathAbs, name);
                return CI.isEnum;
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
        private boolean isAsterisk = false;
        private boolean isConvertAnno = false;
        public TypeVisitor(boolean flag){
            isConvertAnno = flag;
        }

        @Override//参照型はここ
        public void visit(ClassOrInterfaceType md, Void arg) {
            boolean flag = isNonNullFlag || isConvertAnno;

            //setOutputer(md.getNameAsString());
            switch (md.getNameAsString()) {
                case "Integer" -> retType.append("Int");
                case "Character" -> retType.append("Char");
                case "Object" -> retType.append("Any");
                default -> retType.append(md.getNameAsString());
            }

            if (md.getTypeArguments().isPresent()) {

                retType.append("<");
                for(Type type:md.getTypeArguments().get()){
                    TypeVisitor visitor = new TypeVisitor(false);
                    type.accept(visitor, null);
                    if(flag || visitor.isAsterisk()) retType.append(visitor.getRetType());
                    else retType.append(visitor.getRetTypeNullable());
                }
                retType.append(">");

                /*retType.append("<");
                super.visit(md, arg);
                if(isNonNullFlag) retType = new StringBuilder(getRetType());
                else retType = new StringBuilder(getRetTypeNullable());
                retType.append(">");

                 */
                //setOutputer("<");

                //setOutputer(">");
                /*TypeVisitor visitor = new TypeVisitor(false);
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
                TypeVisitor visitor = new TypeVisitor(false);
                md.getComponentType().accept(visitor, null);
                retType.append(visitor.getRetType()).append("Array");
            } else {
                /*
                setOutputer("Array<");
                super.visit(md, arg);
                setOutputer(">");

                 */

                TypeVisitor visitor = new TypeVisitor(false);
                md.getComponentType().accept(visitor, null);
                retType.append("Array<");

                if(isConvertAnno) retType.append(visitor.getRetType());
                else retType.append(visitor.getRetTypeNullable());

                retType.append(">");
            }
            //if (!isNonNullFlag) retType = new StringBuilder(getRetTypeNullable());
        }

        @Override//extendedType：extend, superType：superが取得できる
        public void visit(WildcardType md, Void arg) {
            if (md.getExtendedType().isPresent()) {

                TypeVisitor visitor = new TypeVisitor(false);
                md.getExtendedType().get().accept(visitor, null);
                retType.append(visitor.getRetType());

                /*super.visit(md, arg);
                retType = new StringBuilder(getRetType());

                 */
                //if(isNonNullFlag) retType = new StringBuilder(getRetType());
                //else retType = new StringBuilder(getRetTypeNullable());
                /*TypeVisitor visitor = new TypeVisitor(false);
                md.getExtendedType().get().accept(visitor, null);
                if(isNonNullFlag) retType.append(visitor.getRetType());
                else retType.append(visitor.getRetTypeNullable());*/
            } else if (md.getSuperType().isPresent()) {
                //setOutputer("in ");
                retType.append("in "); //エラーになるので模索中
                TypeVisitor visitor = new TypeVisitor(false);
                md.getSuperType().get().accept(visitor, null);
                retType.append(visitor.getRetType());
                /*super.visit(md, arg);
                retType = new StringBuilder(getRetType());

                 */
                //if(isNonNullFlag) retType = new StringBuilder(getRetType());
                //else retType = new StringBuilder(getRetTypeNullable());
                /*TypeVisitor visitor = new TypeVisitor(false);
                md.getSuperType().get().accept(visitor, null);
                if(isNonNullFlag) retType.append("in ").append(visitor.getRetType());
                else retType.append("in ").append(visitor.getRetTypeNullable());*/
            } else {
                retType.append("*");
                isAsterisk = true;
                //setOutputer("*");
            }
        }

        @Override//引数の型
        public void visit(TypeParameter md, Void arg) {
            boolean flag = isNonNullFlag || isConvertAnno;
            TypeVisitor visitor = new TypeVisitor(false);
            md.getTypeBound().get(0).accept(visitor, null);
            retType.append(md.getNameAsString()).append(":");
            if (flag) retType.append(visitor.getRetType());
            else retType.append(visitor.getRetTypeNullable());
            retType.append(">");
            //setOutputer("<" + md.getNameAsString() + ":" + md.getTypeBound().get(0) + ">");
        }

        public boolean isAsterisk() {
            return this.isAsterisk;
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
                                  ArrayDeque<Range> rangeStructure, ArrayDeque<BlockInformation> dequeBI,
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
            AssignVisitor valueVisitor = new AssignVisitor(classname, structure, range, rangeStructure, dequeBI, indent, "", 0);
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

    public static String ModifierConvert(NodeList<Modifier> modifiers, boolean isMethod, String indent, BaseInformation BI){
        StringBuilder mod = new StringBuilder();
        if (modifiers.size() != 0) {
            for (Modifier modifier : modifiers) {
                String tmp = modifier.toString();
                if (tmp.startsWith("public")) continue;
                if (tmp.startsWith("static")) continue;
                if (tmp.startsWith("synchronized")) {
                    if(isMethod) setOutputerln(indent + "@Synchronized");
                    continue;
                }
                if (tmp.startsWith("private")){
                    if(BI != null)
                        if (!BI.isKotlinPrivate) continue;
                }
                mod.append(tmp);
            }
        }
        return mod.toString();
    }

    public static String ModifierConvert(NodeList<Modifier> modifiers, BaseInformation BI){
        return ModifierConvert(modifiers, false, "", BI);
    }

    public static String ModifierConvert(NodeList<Modifier> modifiers){
        return ModifierConvert(modifiers, false, "", null);
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
        TypeVisitor Tvisitor = new TypeVisitor(false);
        parameter.getType().accept(Tvisitor, null);
        if(isNonNullFlag) setOutputer(Tvisitor.getRetType());
        else setOutputer(Tvisitor.getRetTypeNullable());
    }

}

