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
    private static boolean isCustomAccessFlag = false;

    private static DataStore.ConvertFor_i convertFor_i = new DataStore.ConvertFor_i();

    public static void setOutputer(String output) {
        Outputer.append(output);
    }

    public static void setOutputerln(String output) {
        setOutputer(output);
        Outputer.append("\n");
    }

    public static void main(String[] args) throws IOException {
        String path_root = "/Users/kzm0308/IdeaProjects/J2KConverter/src/main/java/pac3";
        if (args.length == 0);
        else {
            int size = args.length;
            path_root = args[0];
            if (size > 1) {
                for(int i = 1;i < size;i++){
                    if (args[i].equals("-nn")) isNonNullFlag = true;
                    if (args[i].equals("-ca")) isCustomAccessFlag = true;
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
            VoidVisitor<?> visitor = new OutClassVisitor("", "", new ArrayDeque<>(), false);
            cu.accept(visitor, null);
            OutputerStore.put(pathAbs, Outputer);
        }

        System.out.println("Finish Convert Kotlin:" + path_root);

        for(String path:OutputerStore.keySet()){
            /*if(path.equals("/Users/kzm0308/IdeaProjects/J2KConverter/src/main/java/pac3/Class9.java")) {
                //ConvertOutputer.ConvertAction(path_root, path);
                System.out.println("~~~~~~~~~~~~~");
                System.out.println(OutputerStore.get(path));
            }*/
            ConvertOutputer.ConvertAction(path_root, path);
            //System.out.println("~~~~~~~~~~~~~");
            System.out.println(OutputerStore.get(path));
        }

    }

    private static class OutClassVisitor extends VoidVisitorAdapter<Void> {
        String indent = "";
        String packageName = "";
        String structure = "";
        boolean isInner = false;
        ArrayDeque<Range> rangeStructure = new ArrayDeque<>();
        ArrayList<ImportDeclaration> ImportNameList = new ArrayList<>();

        public OutClassVisitor(String indent, String structure, ArrayDeque<Range> rangeStructure, boolean isInner) {
            this.indent = indent;
            this.structure = structure;
            this.rangeStructure = rangeStructure;
            this.isInner = isInner;
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
                setOutputerln("import kotlin.jvm.JvmStatic\n");
                isJvmStatic = true;
            }
            if(DataStore.memoryImportAlreadyOutput.get(pathAbs));// Do nothing
            else {
                for (ImportDeclaration importDeclaration : ImportNameList) {
                    setOutputer("import " + ImportMaker(importDeclaration.getName()));
                    if(importDeclaration.isAsterisk()) setOutputer(".*");
                    setOutputer("\n");
                }
                DataStore.memoryImportAlreadyOutput.put(pathAbs, true);
            }
            setOutputer("\n");
            String mod = ModifierConvert(md.getModifiers(), CI);
            String openclass = CI.isOpen ? "open " : "";
            String innerclass = md.isInnerClass() && isInner ? "inner " :"";
            String CorI = "class";
            if (md.isInterface()) CorI = "interface";
            setOutputer(indent + mod + openclass + innerclass + CorI + " " + classname);
            String typeParams = "";
            boolean isDia = false;
            if (md.getTypeParameters().size() != 0) {
                isDia = true;
                TypeParameter TP = md.getTypeParameter(0);
                TypeVisitor Tvisitor = new TypeVisitor(false);
                TP.accept(Tvisitor, null);
                typeParams = "<" +Tvisitor.getRetType() + ">";
                /*if (TP.getTypeBound().size() != 0)
                    typeParams = "<" + TP.getNameAsString() + " : " + TP.getTypeBound().get(0) + "?" + ">";
                else
                    typeParams = "<" + TP.getNameAsString() + ">";

                 */
            }
            StringBuilder extend = new StringBuilder("");
            int sizeExIm = 0;
            if (md.getExtendedTypes().size() != 0) {
                TypeVisitor Tvisitor = new TypeVisitor(false);
                md.getExtendedTypes().get(0).accept(Tvisitor, null);
                extend.append(Tvisitor.getRetType());
                //ActivityとFragmentの何もないコンストラクタによるエラーの回避
                if(Tvisitor.getRetType().endsWith("Activity")) extend.append("()");
                else if(Tvisitor.getRetType().endsWith("Fragment")) extend.append("()");
                else if(Tvisitor.getRetType().endsWith("CallBack")) extend.append("()");
                sizeExIm += md.getExtendedTypes().size();
            }
            StringBuilder implement = new StringBuilder("");
            if (md.getImplementedTypes().size() != 0) {
                if (sizeExIm != 0) extend.append(", ");
                int sizeIm = md.getImplementedTypes().size();
                for(int i = 0;i < sizeIm;i++){
                    TypeVisitor Tvisitor = new TypeVisitor(false);
                    md.getImplementedTypes(i).accept(Tvisitor, null);
                    implement.append(Tvisitor.getRetType());
                    if(i < sizeIm -1) implement.append(", ");
                    sizeExIm++;
                }
            }
            if (isDia) setOutputer(typeParams);
            if (sizeExIm != 0)
                setOutputer(":" + extend + implement);
            setOutputerln("{");
            String structureThis = this.structure + "/" + md.getNameAsString();
            ArrayDeque<Range> rangeStructure = this.rangeStructure.clone();
            Range range = md.getRange().get();
            rangeStructure.add(range);

            if (CI.isContainStatic) {
                setOutputerln(indent + indent4 + "companion object{");
                for (BodyDeclaration<?> bodyDeclaration : md.getMembers()) {
                    InClassVisitor CompanionVisitor = new InClassVisitor(indent + indent4, md.getNameAsString(), structureThis, rangeStructure, true);
                    bodyDeclaration.accept(CompanionVisitor, null);
                }
                setOutputerln(indent + indent4 + "}");
            }
            for (BodyDeclaration<?> bodyDeclaration : md.getMembers()) {
                InClassVisitor nonCompanionVisitor = new InClassVisitor(indent, md.getNameAsString(), structureThis, rangeStructure, false);
                bodyDeclaration.accept(nonCompanionVisitor, null);
            }
            setOutputerln(indent + "}");

        }

        @Override
        public void visit(EnumDeclaration md, Void arg) {
            String classname = md.getNameAsString();
            ClassInformation CI = DataStore.memoryClass.getData(pathAbs, classname);
            String mod = ModifierConvert(md.getModifiers(), CI);
            setOutputerln(indent + mod + "enum class " + md.getNameAsString() + "{");
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

            if (CI.isContainStatic) {
                setOutputerln(indent + indent4 + "companion object{");
                for (BodyDeclaration<?> bodyDeclaration : md.getMembers()) {
                    InClassVisitor CompanionVisitor = new InClassVisitor(indent + indent4, md.getNameAsString(), structureThis, rangeStructure, true);
                    bodyDeclaration.accept(CompanionVisitor, null);
                }
                setOutputerln(indent + indent4 + "}");
            }
            for (BodyDeclaration<?> bodyDeclaration : md.getMembers()) {
                InClassVisitor nonCompanionVisitor = new InClassVisitor(indent, md.getNameAsString(), structureThis, rangeStructure, false);
                bodyDeclaration.accept(nonCompanionVisitor, null);
            }
            setOutputerln(indent + "}");
        }

    }

    public static String ImportMaker(Name name){
        String str = "";
        if(name.getQualifier().isPresent()) str = ImportMaker(name.getQualifier().get()) + ".";
        String CompanionStr = "";
        if(!str.equals("")) {
            String importStr = "";
            if(DataStore.memoryClassLibrary.get(str + name.getIdentifier()) != null) {
                importStr = DataStore.memoryClassLibrary.get(str + name.getIdentifier());
            }
            ClassInformation CI = DataStore.memoryClass.getData(importStr, name.getIdentifier());
            if(CI != null){
                if(CI.isStatic) CompanionStr = "Companion.";
            }
        }
        return str + CompanionStr +  name.getIdentifier();
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
            BI = dequeBI.peekLast().getMemoryData(structure, range);
            if(BI != null) this.dequeBI.add(BI);
            isAnonymous = true;
        }

        @Override
        public void visit(ClassOrInterfaceDeclaration md, Void arg) {
            boolean isStatic = false;
            if (md.getModifiers().size() != 0) {
                NodeList<Modifier> modifiers = md.getModifiers();
                for (Modifier modifier : modifiers) {
                    if (modifier.toString().startsWith("static ")) isStatic = true;
                }
            }
            if (isStatic == isCompanionConvert) {
                OutClassVisitor visitor = new OutClassVisitor(indent + indent4, this.structure, this.rangeStructure, !md.isInterface());
                md.accept(visitor, null);
            }
        }

        @Override
        public void visit(MethodDeclaration md, Void arg) {
            String methodName = md.getNameAsString();
            Range range = md.getRange().get();
            boolean flagConvert = false;
            MethodInformation MI = null;
            if(isAnonymous) {
                if(BI != null)MI = (MethodInformation) BI.getMemoryData(structure + "/" + methodName, range);
            }
            else MI = (MethodInformation) CI.getMemoryData(CI.blockStructure + "/" + methodName, range);
            if(isCustomAccessFlag) {
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
                    if(methodName.equals("main")) setOutputerln(indent + indent4 +  "@JvmStatic");
                    String mod = ModifierConvert(md.getModifiers(), true, indent + indent4, MI, false);
                    String openMod = "";
                    if(MI != null) openMod = MI.isOpen ? "open " : "";
                    String override = "";
                    boolean isNonNull = false;
                    if (md.getAnnotations().size() != 0) {
                        for (int i = 0; i < md.getAnnotations().size(); i++) {
                            if (md.getAnnotation(i).getNameAsString().equals("Override")) {
                                override = "override ";
                            } else if (md.getAnnotation(i).getNameAsString().equals("NonNull")) {
                                isNonNull = true;
                            } else if (md.getAnnotation(i).getNameAsString().equals("Nullable")) {
                                isNonNull = false;
                            } else {
                                AnnotationVisitor annotationVisitor = new AnnotationVisitor(classname, indent);
                                md.getAnnotation(i).accept(annotationVisitor, null);
                            }
                        }
                    }
                    setOutputer(indent + indent4 + openMod + override + mod + "fun " + methodName + "(");
                    for (int i = 0; i < md.getParameters().size(); i++) {
                        if (i != 0) setOutputer(", ");
                        boolean nonNullFlag = SFP_NC(methodName, i);
                        ParameterConvert(md.getParameter(i), MI, nonNullFlag);
                    }
                    setOutputer(")");
                    if (!md.getType().isVoidType()) {
                        setOutputer(": ");
                        Type type = md.getType();
                        boolean nullable = true;
                        if(MI != null){
                            if(MI.isConvertReturnType) type = MI.returnType;
                            nullable = MI.nullable;
                        }
                        TypeVisitor Tvisitor = new TypeVisitor(false);
                        type.accept(Tvisitor, null);
                        if(md.getType().isPrimitiveType()) setOutputer(Tvisitor.getRetType());
                        else if(isNonNull)setOutputer(Tvisitor.getRetType());
                        else if(isNonNullFlag) {
                            if(nullable)setOutputer(Tvisitor.getRetTypeNullable());
                            else setOutputer(Tvisitor.getRetType());
                        }
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

        // SFP_NC: Specific Function`s Paramter Non-null Compulsion
        // 特定の関数の引数をnon-null化する(ライブラリなどの解析の代わりに使用)
        private boolean SFP_NC(String methodName, int num){
            return switch (methodName){
                case "onRequestPermissionsResult" -> true;
                case "onCreateViewHolder" -> true;
                case "onBindViewHolder" -> true;
                case "onScrolled" -> true;
                case "getItemOffsets" -> true;
                case "onCreateView" -> num == 0;//最初の引数だけnon-null
                case "onViewCreated" -> num == 0;//最初の引数だけnon-null
                case "onCreate" -> false;
                case "onCoWatchingStateChanged" -> true;
                case "onCoDoingStateChanged" -> true;
                case "onMeetingEnded" -> true;
                case "onSaveInstanceState" -> true;
                case "onFailure" -> true;
                case "onUpdated" -> true;
                default -> false;
            };
        }

        @Override
        public void visit(FieldDeclaration md, Void arg) {
            NodeList<Modifier> modifiers = new NodeList<>();
            boolean isStatic = false;
            if (md.getModifiers().size() != 0) {
                modifiers = md.getModifiers();
                for (Modifier modifier : modifiers) {
                    if (modifier.toString().startsWith("static ")) isStatic = true;
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
            boolean isStatic = false;
            if (md.getModifiers().size() != 0) {
                NodeList<Modifier> modifiers = md.getModifiers();
                for (Modifier modifier : modifiers) {
                    if (modifier.toString().startsWith("static ")) isStatic = true;
                }
            }
            if (isStatic == isCompanionConvert) {
                String structureThis = this.structure + "/" + md.getNameAsString();
                ArrayDeque<Range> rangeStructure = this.rangeStructure.clone();
                Range range = md.getRange().get();
                rangeStructure.add(range);
                BlockInformation BI = CI != null ? CI.getMemoryData(structureThis, range) :null;
                setOutputer(indent + indent4 + "constructor(");
                for (int i = 0; i < md.getParameters().size(); i++) {
                    if (i != 0) setOutputer(", ");
                    ParameterConvert(md.getParameter(i),BI, false);
                }
                setOutputer(")");
                VoidVisitor<?> visitor = new MakeBlockVisitor(classname, md.getNameAsString(), structureThis, range, rangeStructure, dequeBI, indent + indent4, indent + indent4);
                md.accept(visitor, null);
                setOutputer("\n");
            }
        }

        @Override
        public void visit(EnumDeclaration md, Void arg) {
            boolean isStatic = false;
            if (md.getModifiers().size() != 0) {
                NodeList<Modifier> modifiers = md.getModifiers();
                for (Modifier modifier : modifiers) {
                    if (modifier.toString().startsWith("static ")) isStatic = true;
                }
            }
            if (isStatic == isCompanionConvert) {
                OutClassVisitor visitor = new OutClassVisitor(indent, structure, rangeStructure, false);
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
            String initial = "";
            int dim = 0;
            boolean assign = true;
            Type type = md.getType();

            BlockInformation BI = dequeBI.peekLast();
            FieldInformation FI = BI.getMemoryF().get(name);

            String openMod = "";
            String overrideMod = "";

            String mod = ModifierConvert(modifiers, FI, isLocal);
            boolean flagOver = false;
            boolean isNullable = true;

            if(FI != null){
                dim = FI.dim;
                assign = FI.assign;
                openMod = FI.isOpen ? "open " : "";
                overrideMod = FI.isOverride ? "override " : "";
                flagOver = FI.isOverride;
                isNullable = FI.nullable;
                if(FI.isConvertValueType) type = FI.valueType;
            }
            setOutputer(indent + openMod + overrideMod + mod);
            boolean isLambda = false;
            if (assign || isWhile || flagOver) dec = "var";
            else dec = "val";
            boolean isReservedWord = CheckReservedWord(name);
            boolean isNonOutputDec = false;
            if(name.equals("i")) {
                isNonOutputDec = convertFor_i.UseCheck(pathAbs, structure, rangeStructure.clone());
            }
            if(isNonOutputDec) dec = "";
            setOutputer(dec + " ");
            if(isReservedWord) setOutputer("`");
            setOutputer(name);
            if(isReservedWord) setOutputer("`");
            TypeVisitor Tvisitor = new TypeVisitor(false);
            type.accept(Tvisitor, null);
            if (md.getInitializer().isPresent()) {
                if(!isNonOutputDec) {
                    setOutputer(": ");
                    if (type.isPrimitiveType()) setOutputer(Tvisitor.getRetType());
                    else {
                        if(isNonNullFlag && !isNullable) setOutputer(Tvisitor.getRetType());
                        else setOutputer(Tvisitor.getRetTypeNullable());
                    }
                }
                setOutputer(" = ");
                if (md.getInitializer().get().isLambdaExpr()) {
                    isLambda = true;
                } else {
                    //AssignVisitor visitor = new AssignVisitor(classname, name, structure + "/" + name, md.getRange().get(), rangeStructure, dequeBI, indent, md.getType(), dim);
                    AssignVisitor_Literal visitor =
                            new AssignVisitor_Literal(classname, name, structure + "/" + name, md.getRange().get(), rangeStructure, dequeBI, indent, md.getType(), dim);

                    md.getInitializer().get().accept(visitor, null);
                    if(!(md.getInitializer().get() instanceof LiteralExpr) && !(md.getInitializer().get() instanceof CastExpr)) {
                        boolean castFlag = false;
                        if (visitor.getTypeDef() != null) {
                            if (!visitor.getTypeDef().toString().equals(Tvisitor.getRetType()))
                                castFlag = true;
                        } else castFlag = true;

                        if (castFlag) {
                            //omit
                            //if (type.isPrimitiveType()) setOutputer(" as " + Tvisitor.getRetType());
                        }
                    }
                }
            } else {
                if (type.isPrimitiveType()) {
                    setOutputer(": ");
                    setOutputer(Tvisitor.getRetType());
                    initial = switch (md.getTypeAsString()) {
                        case "int" -> initial.concat(" = 0");
                        case "double" -> initial.concat(" = 0.0");
                        case "long" -> initial.concat(" = 0L");
                        case "float" -> initial.concat(" = 0F");
                        case "boolean" -> initial.concat(" = false");
                        case "byte" -> initial.concat(" = 0");
                        case "short" -> initial.concat(" = 0");
                        case "char" -> initial.concat(" = '0'");
                        default -> initial.concat(" = 0");
                    };
                } else {
                    setOutputer(": ");

                    if(isNonNullFlag && !isNullable) setOutputer(Tvisitor.getRetType());
                    else setOutputer(Tvisitor.getRetTypeNullable());
                    initial = initial.concat(" = null");
                }
            }
            setOutputer(initial);
            if (isLambda) {
                AssignVisitor visitor
                        = new AssignVisitor(classname, name, structure + "/" + name, md.getRange().get(), rangeStructure, dequeBI, indent, md.getType(), dim, FI);
                md.getInitializer().get().accept(visitor, null);
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
            int startCount = 0;
            int size = md.getStatements().size();
            if (size != 0) {
                if (md.getStatement(0).isExplicitConstructorInvocationStmt()) {
                    setOutputer(":");
                    ECISconvert(md.getStatement(0).asExplicitConstructorInvocationStmt());
                    startCount = 1;
                }
            }
            setOutputerln("{");
            VoidVisitor<?> visitor = new InBlockVisitor(classname, contentsName, structure, range, rangeStructure, dequeBI, indent + indent4, false);
            for (int i = startCount; i < md.getStatements().size(); i++) {
                md.getStatement(i).accept(visitor, null);
            }
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
                    AssignVisitor visitor = new AssignVisitor(classname, contentsName, structure, range, rangeStructure, dequeBI, indent);
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
        Type typeLambda = null;

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
                               String indent, boolean isLambda, Type type) {
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
            AssignVisitor visitor = new AssignVisitor(classname, contentsName, structure, range, rangeStructure, dequeBI, indent + indent4);
            md.accept(visitor, null);
        }

        @Override
        public void visit(BreakStmt md, Void arg) {
            setOutputer(indent);
            AssignVisitor visitor = new AssignVisitor(classname, contentsName, structure, range, rangeStructure, dequeBI, indent + indent4);
            md.accept(visitor, null);
        }

        @Override
        public void visit(ReturnStmt md, Void arg) {
            setOutputer(indent);
            Range exRange = md.getExpression().isPresent() ? md.getExpression().get().getRange().get() : range;
            if(isLambda){
                AssignVisitor_Lambda visitor = new AssignVisitor_Lambda(classname, contentsName, structure, exRange, rangeStructure, dequeBI, indent, typeLambda);
                md.accept(visitor, null);
            } else if(md.getExpression().isPresent()){
                AssignVisitor visitor = new AssignVisitor(classname, contentsName, structure, exRange, rangeStructure, dequeBI, indent + indent4, null, 0, isLambda);
                md.accept(visitor, null);
            } else {
                AssignVisitor visitor = new AssignVisitor(classname, contentsName, structure, range, rangeStructure, dequeBI, indent + indent4);
                md.accept(visitor, null);
            }
        }

        @Override
        public void visit(ThrowStmt md, Void arg) {
            setOutputer(indent);
            AssignVisitor visitor = new AssignVisitor(classname, contentsName, structure, range, rangeStructure, dequeBI, indent);
            md.accept(visitor, null);
        }

        @Override
        public void visit(UnaryExpr md, Void arg) {
            setOutputer(indent);
            AssignVisitor visitor = new AssignVisitor(classname, contentsName, structure, range, rangeStructure, dequeBI, indent);
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
            OutClassVisitor outClassVisitor = new OutClassVisitor(indent, structure, rangeStructure, false);
            md.getClassDeclaration().accept(outClassVisitor, null);
        }

        @Override
        public void visit(SynchronizedStmt md, Void arg) {
            setOutputer(indent + "synchronized(");
            AssignVisitor expressionVisitor = new AssignVisitor(classname, contentsName, structure, range, rangeStructure, dequeBI, indent);
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
            setOutputer(indent + "assert(" );
            AssignVisitor conditionVisitor = new AssignVisitor(classname, contentsName, structure, range, rangeStructure, dequeBI, indent);
            md.getCheck().accept(conditionVisitor, null);
            setOutputer(")");
            if (md.getMessage().isPresent()) {
                setOutputer(" {" + md.getMessage().get() + "}");
            }
            setOutputer("\n");
        }

        @Override
        public void visit(TryStmt md, Void arg) {
            setOutputer(indent + "try");
            String structureThis = this.structure + "/Try";
            ArrayDeque<Range> rangeStructure = this.rangeStructure.clone();
            Range range = md.getRange().get();
            rangeStructure.add(range);
            MakeBlockVisitor visitor = new MakeBlockVisitor(classname, contentsName, structureThis, range, rangeStructure, dequeBI, indent, indent);
            md.getTryBlock().accept(visitor, null);
            for (CatchClause catchClause : md.getCatchClauses()) {
                String paramName = catchClause.getParameter().getNameAsString();
                Parameter parameter = catchClause.getParameter();

                structureThis = this.structure + "/Catch";
                range = catchClause.getRange().get();
                rangeStructure = this.rangeStructure.clone();
                rangeStructure.add(range);

                if (parameter.getType().isUnionType()) {
                    UnionType unionType = (UnionType) parameter.getType();
                    for (ReferenceType ref : unionType.getElements()) {
                        setOutputer("catch(" + paramName + ": " + ref + ")");
                        visitor = new MakeBlockVisitor(classname, contentsName, structureThis, range, rangeStructure, dequeBI, indent, indent);
                        catchClause.getBody().accept(visitor, null);
                    }
                } else {
                    setOutputer("catch(" + paramName + ": " + parameter.getTypeAsString() + ")");
                    visitor = new MakeBlockVisitor(classname, contentsName, structureThis, range, rangeStructure, dequeBI, indent, indent);
                    catchClause.getBody().accept(visitor, null);
                }
            }
            if (md.getFinallyBlock().isPresent()) {
                structureThis = this.structure + "/Finally";
                range = md.getFinallyBlock().get().getRange().get();
                rangeStructure = this.rangeStructure.clone();
                rangeStructure.add(range);
                setOutputer("finally");
                visitor = new MakeBlockVisitor(classname, contentsName, structureThis, range, rangeStructure, dequeBI, indent, indent);
                md.getFinallyBlock().get().accept(visitor, null);
            }
            setOutputer("\n");

        }

        @Override
        public void visit(AssignExpr md, Void arg) {
            String structure = this.structure;
            setOutputer(indent);
            boolean isParentAssign = md.getParentNode().get() instanceof AssignExpr;
            boolean isValueAssign = md.getValue().isAssignExpr();
            boolean isMultiAssign = !isParentAssign && isValueAssign;
            if(isMultiAssign){
                setOutputerln("run{");
                if(md.getValue().isObjectCreationExpr()) structure = structure + "/" + md.getTarget().toString();
                structure = DataStore.structureChangeCheck(md.getValue(), this.structure, md.getTarget().toString());
                InBlockVisitor visitor = new InBlockVisitor(classname, contentsName, structure, range, this.rangeStructure, dequeBI, indent + indent4,  false);
                md.getValue().accept(visitor, null);
            }
            if(isMultiAssign) setOutputer(indent + indent4);
            AssignVisitor targetVisitor = new AssignVisitor(classname, contentsName, structure, range, rangeStructure, dequeBI, indent, true);
            md.getTarget().accept(targetVisitor, null);
            Type type = targetVisitor.getTypeDef();

            AssignExpr.Operator operator = md.getOperator();
            if(md.getOperator().name().equals("ASSIGN") ){//|| !(md.getParentNode().get() instanceof AssignExpr)) {
                String arithmeticOperator = AssignOperator(operator.name());
                setOutputer(arithmeticOperator);
            } else {
                setOutputer(" = ");
                targetVisitor = new AssignVisitor(classname, contentsName, structure, range, rangeStructure, dequeBI, indent, type, false);
                md.getTarget().accept(targetVisitor, null);
                String arithmeticOperator = BinaryOperator(operator.name());
                setOutputer(arithmeticOperator + " ");
            }

            if(isMultiAssign){
                AssignVisitor valueVisitor = new AssignVisitor(classname, contentsName, structure, range, rangeStructure, dequeBI, indent, type, false);
                md.getValue().asAssignExpr().getTarget().accept(valueVisitor, null);
                setOutputerln("\n" + indent + "}");
            } else {
                if (md.getValue().isObjectCreationExpr()) structure = structure + "/" + md.getTarget().toString();
                structure = DataStore.structureChangeCheck(md.getValue(), this.structure, md.getTarget().toString());
                AssignVisitor_Literal valueVisitor = new AssignVisitor_Literal(classname, contentsName, structure, range, rangeStructure, dequeBI, indent, type, 0);
                if (md.getValue().isLambdaExpr()) {
                    FieldInformation FI = targetVisitor.backFI();
                    Type typeLambda = FI != null ? FI.type : null;
                    AssignVisitor valueVisitorL = new AssignVisitor(classname, contentsName, structure + "/" + md.getTarget().toString(), md.getRange().get(), rangeStructure, dequeBI, indent, typeLambda, 0, FI);
                    md.getValue().accept(valueVisitorL, null);
                } else md.getValue().accept(valueVisitor, null);

                setOutputer("\n");
            }
        }

        @Override
        public void visit(WhileStmt md, Void arg) {
            String structureThis = this.structure + "/While";
            ArrayDeque<Range> rangeStructure = this.rangeStructure.clone();
            Range range = md.getRange().get();
            rangeStructure.add(range);

            setOutputer(afterLabelIndent() + "while(");
            AssignVisitor conditionVisitor = new AssignVisitor(classname, contentsName, structure, range, rangeStructure, dequeBI, indent);
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
            AssignVisitor conditionVisitor = new AssignVisitor(classname, contentsName, structure, range, rangeStructure, dequeBI, indent);
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
                else setOutputer(indent + "else ");
                structureThis = this.structure + "/Else";
                range = md.getElseStmt().get().getRange().get();
                rangeStructure = this.rangeStructure.clone();
                rangeStructure.add(range);
                if (md.getElseStmt().get().isBlockStmt())
                    visitor = new MakeBlockVisitor(classname, "", structureThis, range, this.rangeStructure, dequeBI, indent, indent);
                else
                    visitor = new InBlockVisitor(classname, contentsName, structureThis, range, rangeStructure, dequeBI, indent, md.getElseStmt().get().isIfStmt());

                md.getElseStmt().get().accept(visitor, null);
            }
            setOutputer("\n");
        }

        @Override
        public void visit(ObjectCreationExpr md, Void arg) {
            setOutputer(indent);
            AssignVisitor visitor = new AssignVisitor(classname, contentsName, structure, range, rangeStructure, dequeBI, indent);
            md.accept(visitor, null);
            setOutputer("\n");
        }

        @Override
        public void visit(MethodCallExpr md, Void arg) {
            setOutputer(indent);
            AssignVisitor visitor = new AssignVisitor(classname, contentsName, structure, range, rangeStructure, dequeBI, indent);
            md.accept(visitor, null);
            setOutputer("\n");
        }

        @Override
        public void visit(MethodReferenceExpr md, Void arg) {
            setOutputer(indent);
            AssignVisitor visitor = new AssignVisitor(classname, contentsName, structure, range, rangeStructure, dequeBI, indent);
            md.accept(visitor, null);
            setOutputer("\n");
        }

        @Override
        public void visit(SwitchStmt md, Void arg) {
            setOutputer(indent + "when(");
            AssignVisitor selectorVisitor = new AssignVisitor(classname, contentsName, structure, range, rangeStructure, dequeBI, indent);
            md.getSelector().accept(selectorVisitor, null);
            setOutputerln("){");
            Type selectType = selectorVisitor.getTypeDef();
            String structureThis = this.structure + "/Switch";
            Range range = md.getRange().get();
            ArrayDeque<Range> rangeStructure = this.rangeStructure.clone();
            rangeStructure.add(range);
            BlockInformation BI = dequeBI.peekLast().getMemoryData(structureThis, range);
            ArrayDeque<BlockInformation> dequeBISwitch = dequeBI.clone();
            if(dequeBISwitch != null && BI != null) dequeBISwitch.add(BI);
            boolean isNoElse = true;
            for (SwitchEntry entry : md.getEntries()) {
                String structureLabel = structureThis + "/";
                StringBuilder label = new StringBuilder();
                for(Expression expression:entry.getLabels()) label.append(expression);
                if(entry.getLabels().size() == 0) {
                    label.append("default");
                    isNoElse = false;
                }
                structureLabel = structureLabel + label;
                Range rangeEntry = entry.getRange().get();
                ArrayDeque<Range> rangeStructureEntry = rangeStructure.clone();
                rangeStructureEntry.add(rangeEntry);

                SwitchEntryVisitor visitor = new SwitchEntryVisitor(classname, contentsName, structureLabel, rangeEntry, rangeStructureEntry, dequeBISwitch, indent + indent4, true, selectType);
                entry.accept(visitor, null);
            }
            if(isNoElse) setOutputerln(indent + indent4 + "else -> {}");
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
            AssignVisitor conditionVisitor = new AssignVisitor(classname, contentsName, structure, range, rangeStructure, dequeBI, indent);
            md.getCondition().accept(conditionVisitor, null);
            setOutputerln(")");
        }

        @Override
        public void visit(SwitchExpr md, Void arg) {
            AssignVisitor visitor = new AssignVisitor(classname, contentsName, structure, range, rangeStructure, dequeBI, indent);
            md.accept(visitor, null);
            setOutputer("\n");
        }

        @Override
        public void visit(ForEachStmt md, Void arg) {
            String structureThis = this.structure + "/ForEach";
            Range range = md.getRange().get();
            ArrayDeque<Range> rangeStructure = this.rangeStructure.clone();
            rangeStructure.add(range);
            String value = md.getVariable().getVariable(0).getNameAsString();
            setOutputer(afterLabelIndent() + "for(" + value + " in ");
            AssignVisitor iterableVisitor = new AssignVisitor(classname, contentsName, structure, range, rangeStructure, dequeBI, indent);
            md.getIterable().accept(iterableVisitor, null);
            setOutputerln("){");
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
                AssignVisitor compareVisitor = new AssignVisitor(classname, contentsName, structure, range, rangeStructure, dequeBI, indent);
                md.getCompare().get().accept(compareVisitor, null);
            }
            //setOutputer(Forlooper(initialMap, compareMap, updateMap));
            setOutputer(")");
            setOutputerln("{");
            VoidVisitor<?> visitor = new InBlockVisitor(classname, contentsName, structureThis, range, rangeStructure, dequeBI, indent + indent4, false);
            md.getBody().accept(visitor, null);
            for (Expression update : md.getUpdate()) {

                setOutputer(indent + indent4);
                AssignVisitor UpdateVisitor = new AssignVisitor(classname, contentsName, structure, range, rangeStructure, dequeBI, indent);
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
        public void visit(IntegerLiteralExpr md, Void arg) {
            setOutputerln(indent + md.toString());
        }

        @Override
        public void visit(DoubleLiteralExpr md, Void arg) {
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
            //Do nothing
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
        String contentsname = "";
        String structure = "";
        Range range = null;
        ArrayDeque<Range> rangeStructure = new ArrayDeque<>();
        ArrayDeque<BlockInformation> dequeBI = new ArrayDeque<>();
        String indent = "";
        boolean isStmt = false;
        Type selectType = null;

        private SwitchEntryVisitor(String classname, String contentsname, String structure, Range range,
                                   ArrayDeque<Range> rangeStructure, ArrayDeque<BlockInformation> dequeBI,
                                   String indent, boolean isStmt, Type selectType) {
            this.classname = classname;
            this.contentsname = contentsname;
            this.structure = structure;
            this.range = range;
            this.rangeStructure = rangeStructure;
            this.dequeBI = dequeBI.clone();
            this.indent = indent;
            this.isStmt = isStmt;
            this.selectType = selectType;

            BlockInformation BI = dequeBI.peekLast().getMemoryData(structure, range);
            if(BI != null)this.dequeBI.add(BI);
        }

        @Override
        public void visit(SwitchEntry md, Void arg) {
            String label = "";
            if (md.getLabels().size() != 0) {
                label = md.getLabels().get(0).toString();
                if(isVariable(md.getLabels().get(0))) {
                    if(selectType != null) {
                        if(!selectType.isPrimitiveType() && !selectType.toString().startsWith("String"))
                            label = selectType.toString() + "." + label;
                    }
                }
            }
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
                    AssignVisitor visitor = new AssignVisitor(classname, contentsname, structure, range, rangeStructure, dequeBI, "");
                    statement.accept(visitor, null);
                    setOutputer("\n");
                }
            }
        }

        private boolean isVariable(Node node){
            if(node instanceof NameExpr) return true;
            else if(node instanceof FieldAccessExpr) return true;
            else if(node instanceof ArrayAccessExpr) return true;
            else return false;
        }

    }

    private static class AssignVisitor extends VoidVisitorAdapter<Void> {
        String classname = "";
        String contentsName = "";
        String structure = "";
        Range range = null;
        ArrayDeque<Range> rangeStructure = new ArrayDeque<>();
        ArrayDeque<BlockInformation> dequeBI = new ArrayDeque<>();
        Type type = null;
        String indent = "";
        int dim = 0;
        Type typeDef = null;
        boolean isArrayNullable = false;
        private boolean isEnumConvert = false;
        private boolean isLambda = false;
        private FieldInformation FI = null;
        private boolean isWriteTarget = false;

        AssignVisitor(String classname, String contentsName, String structure, Range range, ArrayDeque<Range> rangeStructure,
                      ArrayDeque<BlockInformation> dequeBI, String indent, Type type, int dim) {
            this.classname = classname;
            this.contentsName = contentsName;
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


        AssignVisitor(String classname, String contentsName, String structure, Range range, ArrayDeque<Range> rangeStructure,
                      ArrayDeque<BlockInformation> dequeBI, String indent, Type type, int dim, boolean isLambda){
            this(classname, contentsName, structure, range, rangeStructure, dequeBI, indent, type, dim);
            this.isLambda = isLambda;
        }

        AssignVisitor(String classname, String contentsName, String structure, Range range, ArrayDeque<Range> rangeStructure,
                      ArrayDeque<BlockInformation> dequeBI, String indent, Type type, int dim, FieldInformation FI) {
            this(classname, contentsName, structure, range, rangeStructure, dequeBI, indent, type, dim);
            this.FI = FI;
        }

        AssignVisitor(String classname, String contentsName, String structure, Range range, ArrayDeque<Range> rangeStructure,
                      ArrayDeque<BlockInformation> dequeBI, String indent) {
            this(classname, contentsName, structure, range, rangeStructure, dequeBI, indent, null, 0);
        }

        AssignVisitor(String structure, Range range, ArrayDeque<Range> rangeStructure,
                      ArrayDeque<BlockInformation> dequeBI, String indent) {
            this("", "", structure, range, rangeStructure, dequeBI, indent, null, 0);
        }

        AssignVisitor(String classname, String contentsName, String structure, Range range, ArrayDeque<Range> rangeStructure,
                      ArrayDeque<BlockInformation> dequeBI, String indent, Type type, boolean flag) {
            this(classname, contentsName, structure, range, rangeStructure, dequeBI, indent, type, 0);
            isWriteTarget = flag;
        }

        AssignVisitor(String classname, String contentsName, String structure, Range range, ArrayDeque<Range> rangeStructure,
                      ArrayDeque<BlockInformation> dequeBI, String indent, boolean flag) {
            this(classname, contentsName, structure, range, rangeStructure, dequeBI, indent, null, 0);
            isWriteTarget = flag;
        }

        AssignVisitor(String classname, String contentsName, String structure, Range range, ArrayDeque<Range> rangeStructure,
                      ArrayDeque<BlockInformation> dequeBI, Type type, boolean flag) {
            this(classname, contentsName, structure, range, rangeStructure, dequeBI, "", type, 0);
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
            if(isLambda && type != null) setOutputer("@" + type);
            setOutputer(" ");
            if(md.getExpression().isPresent()){
                AssignVisitor visitor = new AssignVisitor(this.classname, contentsName, structure, range, rangeStructure, dequeBI, indent);
                md.getExpression().get().accept(visitor, null);
            }
            setOutputer("\n");
        }

        @Override
        public void visit(ThrowStmt md, Void arg) {
            setOutputer("throw ");
            AssignVisitor visitor = new AssignVisitor(this.classname, contentsName, structure, range, rangeStructure, dequeBI, indent + indent4);
            md.getExpression().accept(visitor, null);
            setOutputer("\n");
        }

        @Override
        public void visit(UnaryExpr md, Void arg) {
            UnaryExpr.Operator operator = md.getOperator();
            boolean isSign_Not
                    = operator.asString().equals("+") || operator.asString().equals("-") || operator.asString().equals("!");
            if(operator.isPrefix() && isSign_Not) setOutputer(operator.asString());
            AssignVisitor visitor = new AssignVisitor(classname, structure, contentsName, range, rangeStructure, dequeBI, indent, true);
            md.getExpression().accept(visitor, null);

            if(!isSign_Not) {
                setOutputer(" = ");
                visitor = new AssignVisitor(classname, contentsName, structure, range, rangeStructure, dequeBI, indent, false);
                md.getExpression().accept(visitor, null);
                setOutputer(operator.asString().charAt(0) + " 1");
                //if(operator.isPostfix()) setOutputer(operator.asString());
            }
        }

        @Override
        public void visit(InstanceOfExpr md, Void arg){
            AssignVisitor visitor = new AssignVisitor(this.classname, contentsName, structure, range, rangeStructure, dequeBI, indent + indent4);
            md.getExpression().accept(visitor, null);
            setOutputer(" is ");
            TypeVisitor Tvisitor = new TypeVisitor(false);
            md.getType().accept(Tvisitor, null);
            setOutputer(Tvisitor.getRetType());
        }

        @Override
        public void visit(ArrayCreationExpr md, Void arg) {
            if(md.getInitializer().isPresent()){
                AssignVisitor visitor =
                        new AssignVisitor(classname, contentsName, structure, md.getRange().get(), rangeStructure, dequeBI, indent, md.getElementType(), dim);
                md.getInitializer().get().accept(visitor, null);
            } else {
                String type = "";
                TypeVisitor Tvisitor = new TypeVisitor(false);
                md.getElementType().accept(Tvisitor, null);
                String dec = "";
                ArrayConverter(md.getLevels(), Tvisitor, md.getLevels().size(), md.getElementType().isPrimitiveType());
                if (md.getElementType().isPrimitiveType()) {
                    int size = md.getLevels().size();
                    type = Tvisitor.getRetType();
                    /*dec = type + "Array(" + md.getLevels().get(size - 1).toString().charAt(1) + ")";
                    for (int i = size - 2; i >= 0; i--) {
                        dec = "Array(" + md.getLevels().get(i).toString().charAt(1) + ") {" + dec + "}";
                    }

                     */
                } else {
                    int size = md.getLevels().size();
                    if (isNonNullFlag) type = Tvisitor.getRetType();
                    else type = Tvisitor.getRetTypeNullable();
                    /*dec = "arrayOfNulls<" + type + ">(" + md.getLevels().get(size - 1).toString().charAt(1) + ")";
                    for (int i = size - 2; i >= 0; i--) {
                        dec = "Array(" + md.getLevels().get(i).toString().charAt(1) + ") {" + dec + "}";
                    }

                     */
                }
                //setOutputer(dec);
            }
        }

        public void ArrayConverter(NodeList<ArrayCreationLevel> listAcl, TypeVisitor tv, int level, boolean flag){
            int sizeMax = listAcl.size();
            if(level > 1) {
                setOutputer("Array(");
                AssignVisitor visitor = new AssignVisitor(classname, contentsName, structure, range, rangeStructure, dequeBI, indent, type, dim);
                listAcl.get(sizeMax - level).accept(visitor, null);
                setOutputer(") {");
            }
            int underLevel = level - 1;
            if(underLevel < 2){
                if(flag){
                    setOutputer(tv.getRetType() + "Array");
                }else{
                    if (isNonNullFlag) setOutputer("arrayOfNulls<" + tv.getRetType() + ">");
                    else setOutputer("arrayOfNulls<" + tv.getRetTypeNullable() + ">");
                }
                setOutputer("(");
                AssignVisitor visitor = new AssignVisitor(classname, contentsName, structure, range, rangeStructure, dequeBI, indent, type, dim);
                listAcl.get(sizeMax - 1).accept(visitor, null);
                setOutputer(")");
            }
            else ArrayConverter(listAcl, tv, underLevel, flag);
            if(level > 1)setOutputer("}");
        }

        @Override
        public void visit(ArrayInitializerExpr md, Void arg) {
            int size = md.getValues().size();
            String str = "";
            str = str.concat("arrayOf(");
            dim--;
            if (isPrimitiveArray(type) && dim == 0)
                str = str.replace("array",
                        type.toString().replace("[]", "") + "Array");
            setOutputer(str);
            for (int i = 0; i < size; i++) {
                if (i != 0) setOutputer(", ");
                AssignVisitor visitor = new AssignVisitor(classname, contentsName, structure, range, rangeStructure, dequeBI, indent, type, dim);
                md.getValues().get(i).accept(visitor, null);
            }
            setOutputer(")");
        }

        @Override
        public void visit(ObjectCreationExpr md, Void arg) {
            TypeVisitor Tvisitor = new TypeVisitor(false);
            md.getType().accept(Tvisitor, null);
            String name = Tvisitor.getRetType();
            boolean isAnonymous = md.getAnonymousClassBody().isPresent();
            if(isAnonymous) setOutputer(" object: ");
            setOutputer(name);
            int size = md.getArguments().size();
            if(!isAnonymous || size != 0)setOutputer("(");

            if (size != 0) {
                for (int i = 0; i < md.getArguments().size(); i++) {
                    if (i != 0) setOutputer(",");
                    String structureThis = structure;
                    if(md.getArgument(i).isObjectCreationExpr()) structureThis = structure + "/" + md.getType();
                    structureThis = DataStore.structureChangeCheck(md.getArgument(i), this.structure, md.getType().toString());
                    AssignVisitor visitor = new AssignVisitor(classname, md.getType().toString(), structureThis, range, rangeStructure, dequeBI, indent, type, dim);
                    md.getArgument(i).accept(visitor, null);
                }

            }
            if(!isAnonymous || size != 0)setOutputer(")");
            if (isAnonymous) {
                String structureThis = this.structure + "/" + md.getType();
                setOutputerln("{");
                InClassVisitor visitor = new InClassVisitor(indent, name, structureThis, md.getRange().get(), rangeStructure, dequeBI);
                for (BodyDeclaration<?> bd : md.getAnonymousClassBody().get()) {
                    bd.accept(visitor, null);
                }
                setOutputerln(indent + "}");
                setOutputer("\n" + indent);
            }
        }

        @Override
        public void visit(MethodCallExpr md, Void arg) {
            AssignVisitor visitor = null;
            boolean isExistScope = md.getScope().isPresent();
            String methodname = md.getNameAsString();
            boolean isCastPrimitive = false;
            ClassInformation CI = null;
            MethodInformation MI = null;
            if (isExistScope) {
                String str = md.getScope().get() + "." + methodname;
                boolean isNormalConvert = true;
                if (str.equals("System.out.println")) isNormalConvert = false;// Do nothing
                else if (str.equals("System.out.print")) isNormalConvert = false;//Do nothing
                else if(isPrimitiveWrapper(md.getScope().get().toString())){
                    boolean flagToString = methodname.equals("toString");
                    boolean flagParse = methodname.startsWith("parse");
                    if(flagToString || flagParse) {
                        if (md.getArguments().size() != 0) {
                            for (int i = 0; i < md.getArguments().size(); i++) {
                                if (i != 0) setOutputer(",");
                                String structureThis = structure;
                                if(md.getArgument(i).isObjectCreationExpr()) structureThis = structure + "/" + md.getNameAsString();
                                structureThis = DataStore.structureChangeCheck(md.getArgument(i), this.structure, md.getNameAsString());
                                AssignVisitor_Literal argVisitor = new AssignVisitor_Literal(classname, contentsName, structureThis, range, rangeStructure, dequeBI, indent, type, dim);
                                md.getArgument(i).accept(argVisitor, null);
                            }
                        }
                        setOutputer(".");
                        isCastPrimitive = true;
                        isNormalConvert = false;
                    }
                    if(flagParse){
                        methodname = methodname.replace("parse", "to").replace("Integer", "Int").replace("Character", "Char");
                    }
                }
                if(isNormalConvert){
                    visitor = new AssignVisitor(classname, contentsName, structure, range, rangeStructure, dequeBI, indent, type, dim);
                    md.getScope().get().accept(visitor, null);
                    if(visitor.getTypeDef() != null){
                        CI = DataStore.ClassCheck(pathDir, visitor.getTypeDef().toString());
                        if (CI != null) {
                            String structure = CI.blockStructure + "/" + methodname;
                            if (CI.getMemoryKind("Method") != null) {
                                ArrayList<Triplets<String, Range, BlockInformation>> arrayList = CI.getMemoryKind("Method");
                                for (Triplets<String, Range, BlockInformation> triplets : arrayList) {
                                    if (triplets.getLeftValue().equals(structure)) {
                                        MI = (MethodInformation) triplets.getRightValue();
                                        if (md.getArguments().size() == MI.paramTypes.length) {
                                            break;
                                        } else MI = null;
                                    }
                                }
                            }
                        }
                    }
                }
            }
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
                boolean flagIgnore = false;
                boolean isNeedEnclose = true;
                boolean isSplit = false;
                boolean isNeedNullable = true;
                if (methodname.matches("concat")) {
                    setOutputer(" + ");
                    isNeedEnclose = false;
                }else if(methodname.matches("equalsIgnoreCase")){
                    setOutputer(".equals");
                    flagIgnore = true;
                }else if(methodname.matches("size")){
                    //ArrayListとかのsize関数がKotlinだと変数
                    setOutputer("." + methodname);
                    if(isExistScope) {
                        if(visitor.getTypeDef() != null) {
                            if (visitor.getTypeDef().toString().split("<")[0].endsWith("List")) {
                                isNeedEnclose = false;
                            } else if (visitor.getTypeDef().toString().split("<")[0].endsWith("Set")) {
                                isNeedEnclose = false;
                            } else if (visitor.getTypeDef().toString().endsWith("StringBuilder")) {
                                isNeedEnclose = false;
                            }
                        }
                    }
                    isNeedNullable = false;
                }else if(methodname.matches("remove")){
                    setOutputer("." + methodname);
                    if(isExistScope) {
                        if(visitor.getTypeDef() != null) {
                            if (visitor.getTypeDef().toString().split("<")[0].endsWith("MutableList")) {
                                setOutputer("At");
                            }
                        }
                    }
                }else if(methodname.matches("length")){
                    //Stringとかのlength関数がKotlinだと変数
                    setOutputer("." + methodname);
                    if(isExistScope) {
                        if(visitor.getTypeDef() != null) {
                            if (visitor.getTypeDef().toString().startsWith("String")) {
                                isNeedEnclose = false;
                            } else if (visitor.getTypeDef().toString().split("<")[0].endsWith("List")) {
                                isNeedEnclose = false;
                            }
                        }
                    }
                    isNeedNullable = false;
                }else if(methodname.matches("ordinal")){
                    //Enumとかのordinal関数がKotlinだと変数
                    setOutputer("." + methodname);
                    isNeedEnclose = false;//仮置き
                    /*if(isExistScope) {
                        if(visitor.getTypeDef().isArrayType()){
                            isNeedEnclose = false;
                        }
                    }*/
                }else if(methodname.matches("getBytes")){
                    //StringとかのgetBytes関数がKotlinだと変数
                    boolean flag = true;
                    if(isExistScope) {
                        if(visitor.getTypeDef() != null) {
                            if (visitor.getTypeDef().toString().equals("String")) {
                                setOutputer(".toByteArray");
                                flag = false;
                            }
                        }
                    }
                    if(flag)setOutputer(methodname);
                }else if(methodname.matches("getMessage")){
                    //excption系のgetMessage関数がKotlinだと変数
                    setOutputer(".message");
                    isNeedEnclose = false;
                } else if(methodname.matches("split")){
                    setOutputer("." + methodname);
                    typeDef = new ArrayType(new ClassOrInterfaceType(null, "String"));
                    isSplit = true;
                } else if(methodname.matches("getCause")){
                    if(isExistScope) {
                        if(visitor.getTypeDef() != null) {
                            if (visitor.getTypeDef().toString().equals("Throwable")) {
                                setOutputer(".cause");
                                isNeedEnclose = false;
                            }
                        }
                    }
                }else{
                    setOutputer("." + methodname);
                }
                if(isNeedEnclose)setOutputer("(");
                if (md.getArguments().size() != 0) {
                    int size = md.getArguments().size();
                    Type[] types = new Type[size];
                    for (int i = 0; i < size; i++) {
                        if(isCastPrimitive) break;
                        if (i != 0) setOutputer(",");
                        String structureThis = structure;
                        if(md.getArgument(i).isObjectCreationExpr()) structureThis = structure + "/" + md.getNameAsString();
                        structureThis = DataStore.structureChangeCheck(md.getArgument(i), this.structure, md.getNameAsString());
                        Type typeTmp = type;
                        if(MI != null) typeTmp = MI.paramTypes[i];
                        AssignVisitor_Literal argVisitor = new AssignVisitor_Literal(classname, contentsName, structureThis, range, rangeStructure, dequeBI, indent, typeTmp, dim);
                        md.getArgument(i).accept(argVisitor, null);
                        types[i] = argVisitor.getTypeDef();
                    }

                }
                if(flagIgnore) setOutputer(", true");
                if(isNeedEnclose) setOutputer(")");
                if(isSplit) setOutputer(".toTypedArray()");//Kotlin Split = List<> Java Split = Array
                boolean isGetInfo = false;
                isGetInfo = MI != null;
                boolean isPrimitive = false;
                if(isGetInfo){
                    if (isNonNullFlag) {
                        if (MI.nullable)
                            setOutputer("!!");
                    } else {
                        if(isNeedNullable){
                            if (!MI.type.toString().equals("void"))
                                setOutputer("!!");
                        }
                    }
                    typeDef = MI.type;
                    isPrimitive = typeDef.isPrimitiveType();
                }
                /*if(dequeBI.peekLast() != null){
                    BlockInformation BI = dequeBI.peekLast();
                    if (BI.kind.equals("Methods")) {//機能させていない、ここのアルゴリズムが不明
                        MethodInformation MITmp = (MethodInformation) BI;
                        isGetInfo = true;
                        if (isNonNullFlag) {
                            if (MITmp.nullable)
                                setOutputer("!!");
                        } else {
                            if(isNeedNullable){
                                if (!MITmp.type.toString().equals("void"))
                                    setOutputer("!!");
                            }
                        }
                        typeDef = MITmp.type;
                        isPrimitive = typeDef.isPrimitiveType();
                    }
                }*/
                if(!isGetInfo && !isPrimitive && isNeedNullable){
                    if(methodname.equals("wait") || methodname.equals("notify") || methodname.equals("notifyAll")) ;
                    else setOutputer("!!");
                }

            }
        }

        @Override
        public void visit(MethodReferenceExpr md, Void arg) {
            if(md.getScope().isTypeExpr()) {
                TypeVisitor visitor = new TypeVisitor(false);
                md.getScope().accept(visitor, null);
                setOutputer(visitor.getRetType());
                setOutputer("!!");
                /*if(md.getScope().toString().equals("System.out"))setOutputer(visitor.getRetType());
                else {
                    if(isNonNullFlag)setOutputer(visitor.getRetType());
                    else setOutputer(visitor.getRetTypeNullable());
                }*/
            } else {
                AssignVisitor visitor = new AssignVisitor(classname, contentsName, structure, range, rangeStructure, dequeBI, indent, type, dim);
                md.getScope().accept(visitor, null);
            }
            setOutputer("::" + md.getIdentifier());
        }

        @Override
        public void visit(ConditionalExpr md, Void arg) {
            setOutputer("if(");
            AssignVisitor conditionVisitor = new AssignVisitor(classname, contentsName, structure, range, rangeStructure, dequeBI, indent, type, dim);
            md.getCondition().accept(conditionVisitor, null);
            setOutputer(")");
            AssignVisitor trueVisitor = new AssignVisitor(classname, contentsName, structure, range, rangeStructure, dequeBI, indent, type, dim);
            md.getThenExpr().accept(trueVisitor, null);
            setOutputer(" else ");
            AssignVisitor falseVisitor = new AssignVisitor(classname, contentsName, structure, range, rangeStructure, dequeBI, indent, type, dim);
            md.getElseExpr().accept(falseVisitor, null);

        }

        @Override
        public void visit(BinaryExpr md, Void arg) {
            AssignVisitor visitor = new AssignVisitor(classname, contentsName, structure, range, rangeStructure, dequeBI, indent, type, dim);
            md.getLeft().accept(visitor, null);
            String arithmeticOperator = BinaryOperator(md.getOperator().name());
            setOutputer(arithmeticOperator);
            visitor = new AssignVisitor(classname, contentsName, structure, range, rangeStructure, dequeBI, indent, type, dim);
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
            super.visit(md, arg);
            boolean isNeedCast = true;
            //findViewByIdはas Typeによるキャストでエラー出力の可能性あり
            if(md.getExpression().isMethodCallExpr()){
                if(md.getExpression().asMethodCallExpr().getName().toString().equals("findViewById")) {
                    isNeedCast = false;
                }
            }
            if(isNeedCast) {
                TypeVisitor visitor = new TypeVisitor(false);
                md.getType().accept(visitor, null);
                if (md.getType().isPrimitiveType()) setOutputer(" as " + visitor.getRetType());
                else {
                    setOutputer(" as " + visitor.getRetType());
                }
            }
        }

        @Override
        public void visit(LambdaExpr md, Void arg) {
            boolean notExist = true;
            String indentThis = indent + indent4;
            String structureThis = this.structure + "/" + contentsName;
            Type type = null;

            //interface search
            MethodInformation MI = null;
            if(this.FI != null){
                type = this.FI.type;
            } else if (this.type != null) {
                type = this.type;
            }
            if(type != null){
                ClassInformation CI = DataStore.ClassCheck(pathDir, type.toString());
                if(CI != null){
                    notExist = false;
                    ArrayList<Triplets<String, Range, BlockInformation>> triplets = CI.getMemoryKind("Method");
                    MI = (MethodInformation) triplets.get(0).getRightValue();


                }
            }

            if(!notExist) setOutputer("object: ");
            TypeVisitor Tvisitor = new TypeVisitor(false);
            if(type != null){
                type.accept(Tvisitor, null);

                //argumentの中身取り出し
                if(type.isClassOrInterfaceType()){
                    ClassOrInterfaceType type1 = type.asClassOrInterfaceType();
                    if(type1.getTypeArguments().isPresent()){
                        if(type1.toString().startsWith("Optional")) {
                            Tvisitor = new TypeVisitor(false);
                            type1.getTypeArguments().get().get(0).accept(Tvisitor, null);
                            type = type1.getTypeArguments().get().get(0);
                        }
                    }
                }
                setOutputer(Tvisitor.getRetType().replace("?", ""));
            }
            setOutputer("{");
            if(!notExist) setOutputer("\n" + indentThis + "override fun " + MI.name + "(");

            if (md.getParameters().size() != 0) {
                for (int i = 0; i < md.getParameters().size(); i++) {
                    if (i != 0) setOutputer(", ");
                    Parameter param = md.getParameter(i);
                    String paramName = param.getNameAsString();
                    Tvisitor = new TypeVisitor(false);
                    if(notExist) param.getType().accept(Tvisitor, null);
                    else {
                        MI.paramTypes[i].accept(Tvisitor, null);
                    }

                    //else paramType = ": " + param.getTypeAsString().substring(0, 1).toUpperCase() + param.getTypeAsString().substring(1);

                    /*if (param.getType().isArrayType()) {
                        paramType = ": Array<" + param.getType().toArrayType().get().getComponentType() + ">";
                    }

                     */
                    if (notExist && param.getType().isUnknownType())setOutputer(paramName);
                    else {
                        if(isNonNullFlag) setOutputer(paramName + ": " + Tvisitor.getRetType());
                        else setOutputer(paramName + ": " + Tvisitor.getRetTypeNullable());
                    }
                }
            }
            if (notExist) setOutputer(" -> ");
            else{
                Tvisitor = new TypeVisitor(false);
                MI.type.accept(Tvisitor, null);
                if(MI.type.isVoidType()) setOutputer(")");
                else {
                    setOutputer("):");
                    if (MI.nullable) setOutputer(Tvisitor.getRetTypeNullable());
                    else setOutputer(Tvisitor.getRetType());
                }
            }
            boolean flag = true;
            if(md.getBody().isExpressionStmt()){
                Statement statement = md.getBody();
                if(!notExist)setOutputer(" = ");
                AssignVisitor_Lambda visitor = new AssignVisitor_Lambda(classname, contentsName, structureThis, md.getRange().get(), rangeStructure, dequeBI, indent, type);
                statement.accept(visitor, null);
                flag = false;
                setOutputer("\n");
            }
            if(flag) {
                if(!notExist)setOutputer("{");
                setOutputer("\n");
                String indentLambda = notExist ? indentThis : indentThis + indent4;
                VoidVisitor<?> visitor = new InBlockVisitor(classname, structureThis, md.getRange().get(), rangeStructure, dequeBI, indentLambda, notExist, type);
                md.getBody().accept(visitor, null);
                if(!notExist)setOutputer(indentThis + "}");
                setOutputer("\n");
            }
            setOutputer(indent + "}");
        }

        @Override
        public void visit(ArrayAccessExpr md, Void arg) {
            AssignVisitor nameVisitor = new AssignVisitor(classname, contentsName, structure, range, rangeStructure, dequeBI, indent, type, dim);
            md.getName().accept(nameVisitor, null);
            setOutputer("[");
            AssignVisitor indexVisitor = new AssignVisitor(classname, contentsName, structure, range, rangeStructure, dequeBI, indent, type, dim);
            md.getIndex().accept(indexVisitor, null);
            setOutputer("]");
            if(isWriteTarget) {
                if (isParentNodeArray(md)) {
                    isArrayNullable = nameVisitor.isArrayNullable();
                    setOutputer("!!");
                } else {
                    if (nameVisitor.isArrayNullable()) {
                        setOutputer("!!");
                    }
                }
            }else if (isNonNullFlag) {
                if (isParentNodeArray(md)) {
                    isArrayNullable = nameVisitor.isArrayNullable();
                    setOutputer("!!");
                } else {
                    if (nameVisitor.isArrayNullable()) {
                        setOutputer("!!");
                    }
                }
            }
            typeDef = nameVisitor.getTypeDef();
        }

        @Override
        public void visit(FieldAccessExpr md, Void arg) {
            AssignVisitor visitor = new AssignVisitor(classname, contentsName, structure, range, rangeStructure, dequeBI, indent, type, dim);
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
                    FI = CIThis.getMemoryF().get(name);
                }
            } else if (md.getScope().isSuperExpr() && isGetInfoThis) {
                ClassInformation CIEx = DataStore.memoryClass.getData(pathAbs, CIThis.classEx.toString());
                if(CIEx == null){
                    if(CIThis.classEx.isClassOrInterfaceType())
                        CIEx = DataStore.memoryClass.getData(pathAbs, CIThis.classEx.asClassOrInterfaceType().getNameAsString());
                }
                if(CIEx != null){
                    if(CIEx.getMemoryF().get(name) != null){
                        typeDef = CIEx.getMemoryF().get(name).type;
                        FI = CIEx.getMemoryF().get(name);
                    }
                }
            } else if(flag2){
                typeDef = visitor.getTypeDef();
                isEnumConvert = !flag2;
            } else {
                Type typeDefTmp = visitor.getTypeDef();
                if(typeDefTmp != null){
                    if(DataStore.memoryClass.getData(pathAbs, typeDefTmp.toString()) != null){
                        ClassInformation CI = DataStore.memoryClass.getData(pathAbs, typeDefTmp.toString());
                        if(CI.getMemoryF().get(name)!= null){
                            typeDef = CI.getMemoryF().get(name).type;
                            if (flag)isArrayNullable = CI.getMemoryF().get(name).nullable;
                            FI = CI.getMemoryF().get(name);
                        }
                    } else {
                        String importStr = DataStore.searchImport(typeDefTmp.toString(), pathAbs);
                        String pathImport = DataStore.memoryClassLibrary.get(importStr);
                        ClassInformation CI = DataStore.memoryClass.getData(pathImport, typeDefTmp.toString());
                        if(CI != null) {
                            if (CI.getMemoryF().get(name) != null) {
                                typeDef = CI.getMemoryF().get(name).type;
                                if (flag) isArrayNullable = CI.getMemoryF().get(name).nullable;
                                FI = CI.getMemoryF().get(name);
                            }
                        }
                    }
                }
                isEnumConvert
                        = isEnumClass(name) || isEnumClass(typeDef);
            }
            if(name.equals("length")){
                if(visitor.getTypeDef() != null) {
                    if(visitor.getTypeDef().isArrayType())name = "size";
                }
            }
            setOutputer(".");
            boolean isReservedWord = CheckReservedWord(name);
            if(isReservedWord) setOutputer("`");
            setOutputer(name);
            if(isReservedWord) setOutputer("`");
            boolean isPrimitive = FI != null ? FI.type.isPrimitiveType() : false;
            if (!flag2 || !isNonNullFlag || !isPrimitive) {
                if(!isWriteTarget){
                    boolean isResource = md.getScope().toString().equals("R");//Android
                    boolean isBuild = md.getScope().toString().equals("Build");//Android SDK VERSION
                    boolean isAndroid = md.getScope().toString().startsWith("android");
                    boolean isManifest = md.getScope().toString().equals("Manifest");//Android Manifast
                    boolean isFirstUpper = name.substring(0, 1).equals(name.substring(0, 1).toUpperCase());
                    boolean isFullUpper = name.equals(name.toUpperCase());
                    if(!isResource && !isFirstUpper && !isBuild && !isAndroid && !isManifest)
                        setOutputer("!!");
                    else if(isFirstUpper){
                        if(isFullUpper && !isBuild && !isAndroid && !isManifest) setOutputer("!!");
                    }
                }
            }
        }

        @Override
        public void visit(NameExpr md, Void arg) {
            String name = md.getNameAsString();
            isEnumConvert = isEnumClass(name);
            if(isEnumConvert) {
                //dummy type node
                ClassOrInterfaceType typeTmp = new ClassOrInterfaceType(null, name);
                typeDef = typeTmp;
            }
            boolean flag = isParentNodeArray(md);

            ArrayDeque<BlockInformation> dequeBI = this.dequeBI.clone();
            BlockInformation BI = dequeBI.pollLast();
            FieldInformation FI = BI.getMemoryF().get(name);

            if(FI != null){
                if (DataStore.rangeDefinitionCheck(FI.range, md.getRange().get())) ;
                else FI = null;
            }
            while (FI == null){
                if(dequeBI.peekLast() == null) break;
                if(dequeBI.peekLast().getMemoryF() != null){
                    BI = dequeBI.pollLast();
                    FI = BI.getMemoryF().get(name);
                    if(FI != null){
                        if (DataStore.rangeDefinitionCheck(FI.range, md.getRange().get())) ;
                        else FI = null;
                    }
                }
            }

            if(FI != null){
                typeDef = FI.type;
                isEnumConvert = isEnumConvert || isEnumClass(typeDef);
                if (flag)isArrayNullable = FI.nullable;
                this.FI = FI;
            }
            if(name.equals("String")) setOutputer("java.lang.");
            boolean isReservedWord = CheckReservedWord(name);
            if(isReservedWord) setOutputer("`");
            setOutputer(name);
            if(isReservedWord) setOutputer("`");
            boolean isPrimitive = FI != null ? FI.type.isPrimitiveType() : false;
            if (!isEnumConvert || !isNonNullFlag || !isPrimitive) {
                String importStr = DataStore.searchImport(name, pathAbs);
                if(!isWriteTarget && importStr == null){
                    boolean isResource = name.equals("R");
                    boolean isAndroid = name.equals("android");
                    boolean isFirstUpper = name.substring(0, 1).equals(name.substring(0, 1).toUpperCase());
                    boolean isFullUpper = name.equals(name.toUpperCase());
                    if(DataStore.memoryClass.getData(pathAbs, name) == null){
                        if(!isFirstUpper && !isResource && !isAndroid) setOutputer("!!");
                        else if(isFirstUpper && !isResource && !isAndroid){
                            if(isFullUpper) setOutputer("!!");
                        }
                    }
                }
            }
        }

        @Override
        public void visit(ClassExpr md, Void arg){
            TypeVisitor visitor = new TypeVisitor(false);
            md.getType().accept(visitor, null);
            setOutputer(visitor.getRetType() + "::class.java");
            typeDef = md.getType();
        }

        @Override
        public void visit(SwitchExpr md, Void arg) {
            setOutputer("when(");
            AssignVisitor selectorVisitor = new AssignVisitor(classname, contentsName, structure, range, rangeStructure, dequeBI, indent);
            md.getSelector().accept(selectorVisitor, null);
            setOutputerln("){");
            Type selectType = selectorVisitor.getTypeDef();
            String structureThis = this.structure + "/Switch";
            Range range = md.getRange().get();
            ArrayDeque<Range> rangeStructure = this.rangeStructure.clone();
            rangeStructure.add(range);
            BlockInformation BI = dequeBI.peekLast().getMemoryData(structureThis, range);
            ArrayDeque<BlockInformation> dequeBISwitch = dequeBI.clone();
            //if(dequeBISwitch != null && BI != null)
            dequeBISwitch.add(BI);
            boolean isNoElse = true;
            for (SwitchEntry entry : md.getEntries()) {
                String structureLabel = structureThis + "/";
                StringBuilder label = new StringBuilder();
                for(Expression expression:entry.getLabels()) {
                    label.append(expression);
                }
                if(entry.getLabels().size() == 0) {
                    label.append("default");
                    isNoElse = false;
                }
                structureLabel = structureLabel + label;
                Range rangeEntry = entry.getRange().get();
                ArrayDeque<Range> rangeStructureEntry = rangeStructure.clone();
                rangeStructureEntry.add(rangeEntry);
                SwitchEntryVisitor visitor = new SwitchEntryVisitor(classname, contentsName, structureLabel, rangeEntry, rangeStructureEntry, dequeBISwitch, indent, false, selectType);
                entry.accept(visitor, null);
            }
            if(isNoElse) setOutputerln(indent + "else -> {}");
            String indentSwitch = indent.substring(0, indent.length() - 4);
            setOutputer(indentSwitch + "}");
        }

        @Override
        public void visit(AssignExpr md, Void arg){

            structure = DataStore.structureChangeCheck(md.getValue(), this.structure, md.getTarget().toString());
            AssignVisitor valueVisitor = new AssignVisitor(classname, contentsName, structure, range, rangeStructure, dequeBI, indent, type, false);
            md.getValue().accept(valueVisitor, null);
            setOutputer(".also {");
            AssignVisitor targetVisitor = new AssignVisitor(classname, contentsName, structure, range, rangeStructure, dequeBI, indent, true);
            md.getTarget().accept(targetVisitor, null);
            AssignExpr.Operator operator = md.getOperator();
            if(md.getOperator().name().equals("ASSIGN") ){//|| !(md.getParentNode().get() instanceof AssignExpr)) {
                String arithmeticOperator = AssignOperator(operator.name());
                setOutputer(arithmeticOperator);
            } else {
                setOutputer(" = ");
                targetVisitor = new AssignVisitor(classname, contentsName, structure, range, rangeStructure, dequeBI, indent, type, false);
                md.getTarget().accept(targetVisitor, null);
                String arithmeticOperator = BinaryOperator(operator.name());
                setOutputer(arithmeticOperator + " ");
            }
            setOutputer("it");
            setOutputer(" }");
        }

        @Override
        public void visit(SuperExpr md, Void arg) {
            setOutputer(md.toString());
        }

        @Override
        public void visit(ThisExpr md, Void arg) {
            setOutputer("this");
            if(md.getTypeName().isPresent()) setOutputer("@" + md.getTypeName().get());
        }

        @Override
        public void visit(StringLiteralExpr md, Void arg) {
            setOutputer(md.toString());
        }

        @Override
        public void visit(NullLiteralExpr md, Void arg) {
            setOutputer(md.toString());
        }

        @Override
        public void visit(BooleanLiteralExpr md, Void arg) {
            setOutputer(md.toString());
        }

        @Override
        public void visit(CharLiteralExpr md, Void arg) {
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
        public void visit(LongLiteralExpr md, Void arg) {
            setOutputer( md.toString());
        }

        public FieldInformation backFI(){
            return this.FI;
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

        public boolean isEnumClass(Type type){
            if(type == null) return false;
            String name = type.isArrayType() ? type.asArrayType().getComponentType().toString() : type.toString();

            if(DataStore.memoryClass.getData(pathAbs, name) != null){
                ClassInformation CI = DataStore.memoryClass.getData(pathAbs, name);
                return CI.isEnum;
            }
            return false;
        }

        public Type getTypeDef() {
            return this.typeDef;
        }

        public boolean isArrayNullable() {
            return this.isArrayNullable;
        }

        public boolean isEnumConvert(){
            return this.isEnumConvert;
        }

        public boolean isPrimitiveArray(Type type) {
            String str = type.toString().replace("[]", "");
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

        public boolean isPrimitiveWrapper(String type) {
            return switch (type) {
                case "Integer" -> true;
                case "Double" -> true;
                case "Long" -> true;
                case "Float" -> true;
                case "Boolean" -> true;
                case "Byte" -> true;
                case "Short" -> true;
                case "Character" -> true;
                default -> false;
            };
        }

    }

    private static class AssignVisitor_Literal extends AssignVisitor{


        public AssignVisitor_Literal(String classname, String contentsName, String structure, Range range, ArrayDeque<Range> rangeStructure,
                      ArrayDeque<BlockInformation> dequeBI, String indent, Type type, int dim) {
            super(classname, contentsName, structure, range, rangeStructure, dequeBI, indent, type, dim);
        }

        @Override
        public void visit(CharLiteralExpr md, Void arg) {
            boolean isChar
                    = type != null ?
                    type.toString().equalsIgnoreCase("char") || type.toString().equalsIgnoreCase("character")
                    : false;
            setOutputer(md.toString() + PrimitiveCast(type, isChar));
        }

        @Override
        public void visit(IntegerLiteralExpr md, Void arg) {
            boolean isInt
                    = type != null ?
                    type.toString().equalsIgnoreCase("int") || type.toString().equalsIgnoreCase("integer")
                    : false;
            setOutputer(md.toString() + PrimitiveCast(type, isInt));
        }

        @Override
        public void visit(DoubleLiteralExpr md, Void arg) {
            boolean isDouble
                    = type != null ?type.toString().equalsIgnoreCase("double") : false;
            setOutputer(md.toString() + PrimitiveCast(type, isDouble));
        }

        @Override
        public void visit(LongLiteralExpr md, Void arg) {
            boolean isLong
                    = type != null ? type.toString().equalsIgnoreCase("long"): false;
            setOutputer(md.toString() + PrimitiveCast(type, isLong));
        }

    }

    private static class AssignVisitor_Lambda extends AssignVisitor{
        Type retLabel = null;

        public AssignVisitor_Lambda(String classname, String contentsname, String structure, Range range,
                                    ArrayDeque<Range> rangeStructure,
                                    ArrayDeque<BlockInformation> dequeBI, String indent, Type type){
            super(classname, contentsname, structure, range, rangeStructure, dequeBI, indent);
            retLabel = type;
        }

        @Override
        public void visit(ReturnStmt md, Void arg){
            if(md.getExpression().isPresent()){
                AssignVisitor visitor = new AssignVisitor(this.classname, contentsName, structure, range, rangeStructure, dequeBI, indent);
                md.getExpression().get().accept(visitor, null);
            }
            setOutputer("\n");
        }
    }

    public static class TypeVisitor extends VoidVisitorAdapter<Void> {
        private StringBuilder retType = new StringBuilder("");
        private boolean isAsterisk = false;
        private boolean isConvertAnno = false;
        public TypeVisitor(boolean flag){
            isConvertAnno = flag;
        }

        @Override//参照型はここ
        public void visit(ClassOrInterfaceType md, Void arg) {
            boolean flag = isNonNullFlag || isConvertAnno;
            boolean flagScope = false;
            TypeVisitor scopeVisitor = new TypeVisitor(false);
            if(md.getScope().isPresent()){
                md.getScope().get().accept(scopeVisitor, null);
                retType.append(scopeVisitor.getRetType());
                retType.append(".");
                flagScope = true;
            }

            ClassInformation CI = DataStore.memoryClass.getData(pathAbs, md.getNameAsString());
            if(CI == null){
                if(DataStore.memoryClass.getDataListKey2(md.getNameAsString()) != null) {
                    for (Triplets<String, String, ClassInformation> triplets : DataStore.memoryClass.getDataListKey2(md.getNameAsString())) {
                        CI = triplets.getRightValue();
                        if(flagScope){
                            ClassOrInterfaceType typeTmp = md.getScope().get();
                            String[] strs = typeTmp.toString().split("\\.");
                            String str = strs[strs.length - 1];
                            boolean flagTmp = true;
                            for(String partPath:CI.blockStructure.split("/")){
                                if(partPath.equals(str)){
                                    flagTmp = false;
                                    break;
                                }
                            }
                            if(flagTmp) CI = null;
                        }
                    }
                }
            }

            if(CI != null){
                if(CI.isStatic){
                    if(ImportStaticCheck(pathAbs, md.getNameAsString()))retType.append("Companion.");
                }
            }

            //setOutputer(md.getNameAsString());
            switch (md.getNameAsString()) {
                case "Integer" -> retType.append("Int");
                case "Character" -> retType.append("Char");
                case "Object" -> retType.append("Any");
                default -> retType.append(md.getNameAsString());
            }

            if (md.getTypeArguments().isPresent()) {
                NodeList<Type> nodeList = md.getTypeArguments().get();
                if(nodeList.size() != 0) {
                    retType.append("<");
                    int sizeArg = nodeList.size();
                    for (int i = 0; i < sizeArg; i++) {
                        if(i > 0) retType.append(", ");
                        TypeVisitor visitor = new TypeVisitor(false);
                        nodeList.get(i).accept(visitor, null);
                        if (flag || visitor.isAsterisk()) retType.append(visitor.getRetType());
                        else retType.append(visitor.getRetTypeNullable());
                    }
                    retType.append(">");
                }

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

                retType.append(visitor.getRetType());

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
            retType.append(md.getNameAsString());
            if(md.getTypeBound().size() != 0){
                TypeVisitor visitor = new TypeVisitor(false);
                md.getTypeBound().get(0).accept(visitor, null);
                retType.append(":");
                if (flag) retType.append(visitor.getRetType());
                else retType.append(visitor.getRetTypeNullable());
                //retType.append(">");
                //setOutputer("<" + md.getNameAsString() + ":" + md.getTypeBound().get(0) + ">");
            }
        }

        public boolean ImportStaticCheck(String pathAbs, String name){
            if(DataStore.memoryImport.get(pathAbs) != null){
                ArrayList<ImportDeclaration> arrayList = DataStore.memoryImport.get(pathAbs);
                for(ImportDeclaration declaration:arrayList){
                    if(name.equals(declaration.getName().getIdentifier())) return false;
                }
            }
            return true;
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
            AssignVisitor visitor = new AssignVisitor(structure, range, rangeStructure, dequeBI, indent);
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

    public static boolean CheckReservedWord(String name){
        return switch (name){
            case "in" -> true;
            case "is" -> true;
            case "object" -> true;
            default -> false;
        };
    }

    public static String ModifierConvert(NodeList<Modifier> modifiers, boolean isMethod, String indent, BaseInformation BI, boolean isLocal){
        StringBuilder mod = new StringBuilder();
        if (modifiers.size() != 0) {
            for (Modifier modifier : modifiers) {
                String tmp = modifier.toString();
                if (tmp.startsWith("public")) continue;
                if (tmp.startsWith("static")) continue;
                if (tmp.startsWith("default")) continue;
                if (tmp.startsWith("final")){
                    if(isLocal) continue;
                }
                if (tmp.startsWith("synchronized")) {
                    if(isMethod) setOutputerln(indent + "@Synchronized");
                    continue;
                }
                if (tmp.startsWith("volatile")) {
                    if(isMethod) setOutputerln(indent + "@Volatile");
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

    public static String ModifierConvert(NodeList<Modifier> modifiers, BaseInformation BI, boolean isLocal){
        return ModifierConvert(modifiers, false, "", BI, isLocal);
    }

    public static String ModifierConvert(NodeList<Modifier> modifiers, BaseInformation BI){
        return ModifierConvert(modifiers, false, "", BI, false);
    }

    public static String ModifierConvert(NodeList<Modifier> modifiers){
        return ModifierConvert(modifiers, false, "", null, false);
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
            case "BINARY_OR" -> " or ";// not "|"
            case "BINARY_AND" -> " and ";// not "&"
            case "XOR" -> " ^ ";
            case "EQUALS" -> " == ";
            case "NOT_EQUALS" -> " != ";
            case "LESS" -> " < ";
            case "GREATER" -> " > ";
            case "LESS_EQUALS" -> " <= ";
            case "GREATER_EQUALS" -> " >= ";
            case "LEFT_SHIFT" -> " shl ";
            case "SIGNED_RIGHT_SHIFT" -> " shr ";
            case "UNSIGNED_RIGHT_SHIFT" -> " ushr ";
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

    public static String PrimitiveCast(Type type, boolean flag){
        if(type == null || flag) return "";
        if(!type.isPrimitiveType()) return "";
        return switch (type.toString()){
            case "Integer" -> ".toInt()";
            case "Character" -> ".toChar()";
            default -> ".to" + type.toString().substring(0, 1).toUpperCase() + type.toString().substring(1)  + "()";
        };
    }

    public static void ParameterConvert(Parameter parameter, BlockInformation BI, boolean nonNullCompulsion) {
        if (parameter.isVarArgs()) setOutputer("vararg ");
        boolean isReservedWord = CheckReservedWord(parameter.getNameAsString());
        if(isReservedWord) setOutputer("`");
        setOutputer(parameter.getNameAsString());
        if(isReservedWord) setOutputer("`");
        setOutputer(": ");
        TypeVisitor Tvisitor = new TypeVisitor(false);
        parameter.getType().accept(Tvisitor, null);
        boolean isNonNull = parameter.getType().isPrimitiveType();
        if(isNonNullFlag) {
            if(BI != null){
                FieldInformation FI = BI.getMemoryF().get(parameter.getNameAsString());
                if(FI != null) isNonNull = !FI.nullable || isNonNull;
            }
        }
        isNonNull = isNonNull || nonNullCompulsion;
        if(isNonNull)setOutputer(Tvisitor.getRetType());
        else setOutputer(Tvisitor.getRetTypeNullable());
        /*if(isNonNullFlag) setOutputer(Tvisitor.getRetType());
        else {
            if(parameter.getType().isPrimitiveType())setOutputer(Tvisitor.getRetType());
            else setOutputer(Tvisitor.getRetTypeNullable());
        }*/
    }

}

