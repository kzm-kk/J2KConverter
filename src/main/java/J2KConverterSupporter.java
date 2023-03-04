import com.github.javaparser.JavaParser;
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
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.utils.SourceRoot;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.*;
import java.util.zip.ZipFile;

public class J2KConverterSupporter {
    public static String pathAbs = "";
    public static String pathDir = "";
    public static boolean isOrderCheck = false;
    public static boolean isLibraryCheck = false;

    public static void main(String[] args) throws IOException {
        DataStore.init();

        // library analysis
        String pathLibRoot = "/Library/Java/JavaVirtualMachines/jdk-15.jdk/Contents/Home/lib/src.zip";
        ZipFile zipFile = new ZipFile(new File(pathLibRoot));
        isLibraryCheck = true;
        /*zipFile.stream().forEach(entry -> {
            String[] strings = entry.getName().split("/");
            StringBuilder sb = new StringBuilder();
            for(int i = 0; i < strings.length - 1; i++){
                if(i > 0)sb.append("/");
                sb.append(strings[i]);
            }
            if(!strings[0].equals("java.base")) return;
            System.out.println("analyzing library: " + entry);
            try (InputStream is = zipFile.getInputStream(entry)) {
                JavaParser parser = new JavaParser();
                CompilationUnit unit = parser.parse(is).getResult().get();
                pathAbs = entry.getName();
                pathDir = sb.toString();
                pathLinkLib(pathAbs, pathDir);
                VoidVisitor<?> visitor = new FirstVisitor("", "", new ArrayDeque<>());
                unit.accept(visitor, null);

            } catch (IOException e) {
                e.printStackTrace();
            }
        });*/

        /*for(String pathDirTmp:DataStore.memoryPathAbsLibrary.keySet()) {
            AnalysisLib(pathDirTmp);
        }*/

        //OutputCheckLib();

        isLibraryCheck = false;

        String pathRoot = "/Library/Java/JavaVirtualMachines/jdk-15.jdk/Contents/Home!/java.base";
        String pathRootAndroid = "/Users/kzm0308/Library/Android/sdk/sources/android-28";
        if(args.length == 0);
        else {
            pathRoot = args[0];
            pathRootAndroid = args[1];
        }
        SourceRoot root = new SourceRoot(Paths.get(pathRoot));
        List<ParseResult<CompilationUnit>> results = root.tryToParse("");

        //source code analysis
        for(ParseResult<CompilationUnit> result:results){
            CompilationUnit cu = result.getResult().get();
            CompilationUnit.Storage cus = cu.getStorage().get();
            pathAbs = cus.getPath().toString();
            pathDir = cus.getDirectory().toString();
            DataStore.sourceRootPathName = cus.getSourceRoot().toString();
            pathLink(pathAbs, pathDir);
            DataStore.initMemoryAlreadyOutput(pathAbs);
            DataStore.memoryStatic.putIfAbsent(pathAbs, false);
            VoidVisitor<?> visitor = new FirstVisitor("", "", new ArrayDeque<>());
            cu.accept(visitor, null);
        }

        //Android Library Analysis
        if(!pathRootAndroid.equals("no")){
            for(String pathAbsTmp:DataStore.memoryImport.keySet()){
                for(ImportDeclaration ID:DataStore.memoryImport.get(pathAbsTmp)){
                    if(ID.isAsterisk()){
                        String pathImp = ID.getName().getQualifier().get().toString().replace(".", "/");
                        DataStore.memoryImportAlreadyAnalysis.add(pathImp);
                    } else {
                        String pathImp = ID.getNameAsString().replace(".", "/") + ".java";
                        DataStore.memoryImportAlreadyAnalysis.add(pathImp);
                    }
                }
            }
            for(String str:DataStore.memoryImportAlreadyAnalysis){
                System.out.println("import:" + str);
            }
            ZipFile jarFile = new ZipFile(new File(pathRootAndroid));
            isLibraryCheck = true;
            jarFile.stream().forEach(entry -> {
                String[] strings = entry.getName().split("/");
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < strings.length - 1; i++) {
                    if (i > 0) sb.append("/");
                    sb.append(strings[i]);
                }
                if(DataStore.memoryImportAlreadyAnalysis.contains(entry.getName())
                        || DataStore.memoryImportAlreadyAnalysis.contains(entry.getName().replace(".class", ".java"))) {
                    System.out.println("analyzing library: " + entry);
                    try (InputStream is = jarFile.getInputStream(entry)) {
                        JavaParser parser = new JavaParser();
                        if(parser.parse(is).getResult().isPresent()) {
                            CompilationUnit unit = parser.parse(is).getResult().get();
                            pathAbs = entry.getName();
                            pathDir = sb.toString();
                            pathLinkLib(pathAbs, pathDir);
                            VoidVisitor<?> visitor = new FirstVisitor("", "", new ArrayDeque<>());
                            unit.accept(visitor, null);
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });

            isLibraryCheck = false;
        }

        //OutputCheck();

        for(String pathDirTmp:DataStore.memoryPathAbs.keySet()) {
            Analysis(pathDirTmp);
        }

        //OutputCheck();

    }

    private static void Analysis(String pathDir){
        for(String pathAbs:DataStore.memoryPathAbs.get(pathDir)){
            //System.out.println(pathAbs);
            AnalysisInClass(pathAbs);
        }
    }

    private static void AnalysisLib(String pathDir){
        for(String pathAbs:DataStore.memoryPathAbsLibrary.get(pathDir)){
            //System.out.println(pathAbs);
            AnalysisInClass(pathAbs);
        }
    }

    private static void AnalysisInClass(String pathAbs){
        for(Triplets<String, String, ClassTemporaryStore> triplets:DataStore.memoryBeforeCheck.getDataListKey1(pathAbs)){
            ClassTemporaryStore CTS = triplets.getRightValue();
            ArrayDeque<Range> rangeStructure = CTS.rangeStructure.clone();
            rangeStructure.add(CTS.range);

            ClassInformation CI = DataStore.memoryClass.getData(pathAbs, CTS.name);
            extendCheck(pathAbs, CI);
            CI = DataStore.memoryClass.getData(pathAbs, CTS.name);

            isOrderCheck = true;

            for(FieldDeclaration fd:CTS.listFD){
                FieldVisitor fieldVisitor = new FieldVisitor(CTS.name, pathAbs, CTS.blockStructure, rangeStructure, CI);
                fd.accept(fieldVisitor, null);
                CI = (ClassInformation) fieldVisitor.getBIAfterAnalysis();
            }
            //static初期化子チェック
            for (InitializerDeclaration id : CTS.listID) {
                InitializerSecondVisitor initializerVisitor = new InitializerSecondVisitor(CTS.name, pathAbs, CTS.blockStructure, rangeStructure, CI);
                for (AnnotationExpr annotationExpr : id.getAnnotations()) {
                    if (annotationExpr.getNameAsString().equals("static")) {
                        id.accept(initializerVisitor, null);
                        CI = (ClassInformation) initializerVisitor.getBIAfterAnalysis();
                    }
                }
            }
            //通常の初期化子チェック
            NonStatic:
            for (InitializerDeclaration id : CTS.listID) {
                InitializerSecondVisitor initializerVisitor = new InitializerSecondVisitor(CTS.name, pathAbs, CTS.blockStructure, rangeStructure, CI);
                for (AnnotationExpr annotationExpr : id.getAnnotations()) {
                    if (annotationExpr.getNameAsString().equals("static"))
                        continue NonStatic;
                }
                id.accept(initializerVisitor, null);
                CI = (ClassInformation) initializerVisitor.getBIAfterAnalysis();
            }

            for (ConstructorDeclaration cd : CTS.listCD) {
                ConstructorSecondVisitor constructorVisitor = new ConstructorSecondVisitor(CTS.name, pathAbs, CTS.blockStructure, rangeStructure, CI);
                cd.accept(constructorVisitor, null);
                CI = (ClassInformation) constructorVisitor.getBIAfterAnalysis();
            }

            isOrderCheck = false;
            for (MethodDeclaration md : CTS.listMD) {
                MethodSecondVisitor methodVisitor = new MethodSecondVisitor(CTS.name, pathAbs, CTS.blockStructure, rangeStructure, CI);
                md.accept(methodVisitor, null);
                CI = (ClassInformation) methodVisitor.getBIAfterAnalysis();
            }


            DataStore.memoryClass.setData(pathAbs, CTS.name, CI);
        }
    }

    //デバッグ用の通常ファイル出力メソッド
    private static void OutputCheck(){
        for(String pathDir:DataStore.memoryPathAbs.keySet()) {
            for (String pathAbs : DataStore.memoryPathAbs.get(pathDir)) {
                for (Triplets<String, String, ClassInformation> triplets : DataStore.memoryClass.getDataListKey1(pathAbs)) {
                    String classname = triplets.getRightValue().name;
                    if(!classname.equals("TextView")) continue;//debug
                    OutputClass(triplets.getRightValue());
                }
            }
        }
    }

    //デバッグ用のライブラリ出力メソッド
    private static void OutputCheckLib(){
        for(String pathDir:DataStore.memoryPathAbsLibrary.keySet()) {
            for (String pathAbs : DataStore.memoryPathAbsLibrary.get(pathDir)) {
                for (Triplets<String, String, ClassInformation> triplets : DataStore.memoryClass.getDataListKey1(pathAbs)) {
                    String classname = triplets.getRightValue().name;
                    if(!classname.equals("PeerConnectionClient")) continue;//debug
                    OutputClass(triplets.getRightValue());
                }
            }
        }
    }

    private static void OutputClass(ClassInformation CI){
        String classname = CI.name;
        String kind = CI.kind;
        boolean isStatic = CI.isStatic;
        boolean isEnum = CI.isEnum;
        String structure = CI.blockStructure;
        ArrayDeque<Range> ranges = CI.rangeStructure.clone();
        boolean isContainStatic = CI.isContainStatic;
        boolean isOpen = CI.isOpen;
        System.out.println
                ("name:" + classname + " kind:" + kind + " isStatic:" + isStatic
                        + " isEnum:" + isEnum + " isContainStatic:" + isContainStatic);
        System.out.println("definition:" + structure + " isOpen:" + isOpen);
        System.out.println("rangeIN:" + ranges);
        System.out.println("field information:" + classname);
        for (String name : CI.getMemoryF().keySet()) {
            FieldInformation fd = CI.getMemoryF().get(name);
            OutputFieldInfo(fd);
        }

        System.out.println("all infomation:" + classname);
        for (Triplets<String, Range, BlockInformation> tripletsBI : CI.memoryB.getDataList()) {
            BlockInformation BI = tripletsBI.getRightValue();
            OutputScope(BI);
        }
        System.out.println();
    }

    private static void OutputScope(BlockInformation BI){
        System.out.println("name:" + BI.name + " kind:" + BI.kind + " isStatic:" + BI.isStatic + " range:" + BI.range);
        System.out.println("definition:" + BI.blockStructure);
        System.out.println("rangeIN:" + BI.rangeStructure.clone());
        if(BI.kind.equals("Method")){
            System.out.println("get:" + ((MethodInformation) BI).getAccess + " set:" + ((MethodInformation) BI).setAccess + " fix:" + ((MethodInformation) BI).fix + " field:" + ((MethodInformation) BI).accessField);
        }
        System.out.println("variable information:" + BI.name);
        for(String name:BI.getMemoryF().keySet()){
            FieldInformation fd = BI.getMemoryF().get(name);
            OutputFieldInfo(fd);
        }

        if(BI.getMemory() != null){
            for(Triplets<String, Range, BlockInformation> tripletsBI:BI.getMemory().getDataList()) {
                BlockInformation BILower = tripletsBI.getRightValue();
                OutputScope(BILower);
            }
        }
    }

    private static void OutputFieldInfo(FieldInformation fd){
        System.out.println(fd.name + " type:" + fd.type + " dim:" + fd.dim
                + " assign:" + fd.assign + " nullable:" + fd.nullable + " static:" + fd.isStatic
                + " initializable:" + fd.initializable + " formerRW:" + fd.formerRW);
        System.out.println("KotlinPrivate:" + fd.isKotlinPrivate);
        System.out.println("isTypeValue:" + fd.isConvertValueType);
        System.out.println("isOpen:" + fd.isOpen);
        System.out.println("isOverride:" + fd.isOverride);
        System.out.println("definition:" + fd.blockStructure);
        System.out.println("range:" + fd.range);
        System.out.println("rangeIN:" + fd.rangeStructure.clone());
    }

    private static void OutputMethodInfo(MethodInformation md){
        System.out.println(md.name + " type:" + md.type + " lines:" + md.lines
                + " fix:" + md.fix + " nullable:" + md.nullable + " static:" + md.isStatic
                + " accessfield:" + md.accessField + " access:" + (md.getAccess || md.setAccess));
        System.out.println("definition:" + md.blockStructure);
        System.out.println("range:" + md.range);
        System.out.println("rangeIN:" + md.rangeStructure.clone());
        System.out.println("local variable");
        for(String name:md.getMemoryF().keySet()){
            FieldInformation fd = md.getMemoryF().get(name);
            OutputFieldInfo(fd);
        }
    }



    private static class FirstVisitor extends VoidVisitorAdapter<Void> {
        String classname = "";
        String structure = "";
        ArrayDeque<Range> rangeStructure = new ArrayDeque<>();
        ArrayList<ImportDeclaration> Import_list = new ArrayList<>();

        public FirstVisitor(String classname, String structure, ArrayDeque<Range> rangeStructure){
            this.classname = classname;
            this.structure = structure;
            this.rangeStructure = rangeStructure;
        }


        @Override
        public void visit(ClassOrInterfaceDeclaration md, Void arg){
            String classname = md.getNameAsString();
            DataStore.memoryImport.put(pathAbs, Import_list);

            int size_extend = md.getExtendedTypes().size();
            int size_implement = md.getImplementedTypes().size();
            Type classEx = null;
            ArrayList<Type> classIm = new ArrayList<>();
            if(size_extend != 0){
                classEx = md.getExtendedTypes().get(0);
            }
            if(size_implement != 0){
                for(int i = 0; i < size_implement ;i++){
                    classIm.add(md.getImplementedTypes(i));
                }
            }

            String structureThis = this.structure + "/" + md.getNameAsString();

            Range range = md.getRange().get();
            String CorI = "class";
            if (md.isInterface()) CorI = "interface";
            ClassInformation info = new ClassInformation(classname, CorI, structureThis, pathAbs, range, this.rangeStructure, classEx, classIm, false, md.isStatic(), md.isPrivate());
            info.isInner = md.isInnerClass();
            DataStore.memoryClass.setData(pathAbs, classname, info);

            ArrayList<InitializerDeclaration> listID = new ArrayList<>();
            ArrayDeque<Range> rangeStructure = this.rangeStructure.clone();
            rangeStructure.add(range);
            for (BodyDeclaration<?> bodyDeclaration : md.getMembers()) {
                SomeVisitor visitor = new SomeVisitor(classname, structureThis, rangeStructure);
                bodyDeclaration.accept(visitor, null);
                listID.addAll(visitor.getListID());
            }
            ClassTemporaryStore CTS = new ClassTemporaryStore(classname, structureThis, pathDir, range, this.rangeStructure, md.getFields(), md.getMethods(), md.getConstructors(), listID);
            DataStore.memoryBeforeCheck.setData(pathAbs, classname, CTS);
        }

        @Override
        public void visit(EnumDeclaration md, Void arg){
            String classname = md.getNameAsString();

            String structureThis = this.structure + "/" + md.getNameAsString();
            Range range = md.getRange().get();
            ClassInformation info = new ClassInformation(classname, "Class", structureThis, pathDir, range, this.rangeStructure, true, md.isStatic(), md.isPrivate());
            DataStore.memoryClass.setData(pathAbs, classname, info);

            ArrayList<InitializerDeclaration> listID = new ArrayList<>();
            ArrayDeque<Range> rangeStructure = this.rangeStructure.clone();
            rangeStructure.add(range);
            for (BodyDeclaration<?> bodyDeclaration : md.getMembers()) {
                SomeVisitor visitor = new SomeVisitor(classname, structureThis, rangeStructure);
                bodyDeclaration.accept(visitor, null);
                listID.addAll(visitor.getListID());
            }

            ClassTemporaryStore CTS = new ClassTemporaryStore(classname, structureThis, pathDir, range, this.rangeStructure, md.getFields(), md.getMethods(), md.getConstructors(), listID);
            DataStore.memoryBeforeCheck.setData(pathAbs, classname, CTS);
        }

        @Override
        public void visit(ImportDeclaration md, Void arg){
            super.visit(md, arg);
            Import_list.add(md);
            searchCI_FromImport(md);
        }

        public static void searchCI_FromImport(ImportDeclaration ID){
            String identifier = ID.getName().getIdentifier();
            String qualifier = ID.getName().getQualifier().get().toString().replace(".", "/");
            String searchDirPath = DataStore.sourceRootPathName + "/" + qualifier;

            ArrayList<String> arrayTmp = DataStore.memoryPathAbs.get(searchDirPath);
            if(arrayTmp != null) {
                for (String path : arrayTmp) {
                    String searchFullPath = searchDirPath+ "/" + identifier + ".java";
                    if (path.equals(searchFullPath)) {
                        DataStore.memoryImportToFile.put(ID.getNameAsString(), searchFullPath);
                    }else if (path.equals(searchDirPath + ".java")) {
                        if (DataStore.memoryClass.getData(searchDirPath + ".java", identifier) != null) {
                            DataStore.memoryImportToFile.put(ID.getNameAsString(), searchDirPath + ".java");
                        }
                    }
                }
            } else {
                if (DataStore.memoryClass.getData(searchDirPath + ".java", identifier) != null) {
                    DataStore.memoryImportToFile.put(ID.getNameAsString(), searchDirPath + ".java");
                }
            }

        }

    }

    private static class SomeVisitor extends VoidVisitorAdapter<Void> {
        String classname = "";
        String structure = "";
        ArrayDeque<Range> rangeStructure = new ArrayDeque<>();
        ArrayList<InitializerDeclaration> initializer_list = new ArrayList<>();

        public SomeVisitor(String classname, String structure, ArrayDeque<Range> rangeStructure){
            this.classname = classname;
            this.structure = structure;
            this.rangeStructure = rangeStructure;
            initializer_list = new ArrayList<>();
        }

        @Override
        public void visit(ClassOrInterfaceDeclaration md, Void arg) {
            ClassInformation CI = DataStore.memoryClass.getData(pathAbs, classname);
            if(!CI.isContainStatic)CI.isContainStatic = md.isStatic();
            DataStore.memoryClass.setData(pathAbs, classname, CI);

            FirstVisitor visitor = new FirstVisitor(classname, this.structure, this.rangeStructure);
            md.accept(visitor, null);
        }

        @Override
        public void visit(ConstructorDeclaration md, Void arg){
            ConstructorFirstVisitor visitor = new ConstructorFirstVisitor(this.classname, this.structure, rangeStructure);
            md.accept(visitor, null);
        }

        @Override
        public void visit(EnumDeclaration md, Void arg){
            ClassInformation CI = DataStore.memoryClass.getData(pathAbs, classname);
            if(!CI.isContainStatic)CI.isContainStatic = md.isStatic();
            DataStore.memoryClass.setData(pathAbs, classname, CI);

            FirstVisitor visitor = new FirstVisitor(this.classname, this.structure, this.rangeStructure);
            md.accept(visitor, null);
        }

        @Override
        public void visit(FieldDeclaration md, Void arg){
            NodeList<Modifier> modifiers = new NodeList<>();
            if(md.getModifiers().size() != 0) modifiers = md.getModifiers();
            for(VariableDeclarator vd: md.getVariables()) {
                VariableFirstVisitor VarVisitor
                        = new VariableFirstVisitor(this.classname, structure, this.rangeStructure, modifiers);
                vd.accept(VarVisitor, null);
            }
        }

        @Override
        public void visit(InitializerDeclaration md, Void arg){
            if(!isLibraryCheck) {
                if (!DataStore.memoryStatic.get(pathAbs)) DataStore.memoryStatic.put(pathAbs, md.isStatic());
            }
            InitializerFirstVisitor visitor = new InitializerFirstVisitor(this.classname, this.structure, rangeStructure);
            md.accept(visitor, null);
        }

        @Override
        public void visit(MethodDeclaration md, Void arg){
            MethodFirstVisitor visitor = new MethodFirstVisitor(this.classname, this.structure, rangeStructure);
            md.accept(visitor, null);
        }

        public ArrayList<InitializerDeclaration> getListID(){
            return this.initializer_list;
        }
    }

    // 最初の解析で簡単に情報を集める
    private static class VariableFirstVisitor extends VoidVisitorAdapter<Void>{
        String classname = "";
        String structure = "";
        ArrayDeque<Range> rangeStructure = new ArrayDeque<>();
        NodeList<Modifier> modifiers = new NodeList<>();
        private ArrayList<BlockInformation> BIStore = new ArrayList<>();

        public VariableFirstVisitor(String name, String structure, ArrayDeque<Range> rangeStructure, NodeList<Modifier> modifiers){
            classname = name;
            this.structure = structure;
            this.rangeStructure = rangeStructure;
            this.modifiers = modifiers;
        }


        @Override
        public void visit(VariableDeclarator md, Void arg){
            String fieldname = md.getNameAsString();
            Type type = md.getType();
            Type valueType = null;
            Range range = md.getRange().get();
            boolean isStatic = false;
            boolean isKotlinPrivate = false;
            int dim = 0;
            boolean assign = false;
            boolean nullable = true;
            boolean initializable = true;
            String formerRW = "";
            String structureThis = structure + "/" + fieldname;

            if(md.getInitializer().isEmpty()) {
                initializable = false;
                //formerRW = "R";
            } else {
                formerRW = "W";
                ValueInitialVisitor visitor = new ValueInitialVisitor(classname, fieldname, range, rangeStructure, structureThis);
                md.getInitializer().get().accept(visitor, null);
                valueType = visitor.getTypeDef();

                for(BlockInformation BI:visitor.getBILower()) BIStore.add(BI);
            }


            if(type.isArrayType()){
                /*int dimNum = type.length();
                type = type.replace("[]", "");
                dimNum = dimNum - type.length();

                 */
                dim = type.getArrayLevel();
            }


            for (Modifier modifier : modifiers) {
                if (modifier.toString().startsWith("static")) {
                    if (!isLibraryCheck) {
                        if (!DataStore.memoryStatic.get(pathAbs)) DataStore.memoryStatic.put(pathAbs, true);
                    }
                    isStatic = true;
                } else if (modifier.toString().startsWith("private")) {
                    isKotlinPrivate = true;
                }
            }

            FieldInformation FI
                    = new FieldInformation(fieldname, structureThis,
                    pathDir, md.getType(), dim, assign, nullable, isStatic,
                    isKotlinPrivate, initializable, valueType, formerRW, range, rangeStructure);

            ClassInformation CI = DataStore.memoryClass.getData(pathAbs, classname);
            if(!CI.isContainStatic)CI.isContainStatic = isStatic;
            if (structure.equals(CI.blockStructure)){
                for(BlockInformation BILow : BIStore){
                    CI.setMemory(BILow);
                }
                CI.setMemoryF(FI);
                DataStore.memoryClass.setData(pathAbs, classname, CI);
            }

        }
    }

    private static class ConstructorFirstVisitor extends VoidVisitorAdapter<Void>{
        String classname = "";
        String structure = "";
        ArrayDeque<Range> rangeStructure = new ArrayDeque<>();
        ArrayList<String> paramList = new ArrayList<>();
        private ArrayList<BlockInformation> BIUpperStore = new ArrayList<>();

        public ConstructorFirstVisitor(String name, String structure, ArrayDeque<Range> rangeStructure){
            classname = name;
            this.structure = structure;
            this.rangeStructure = rangeStructure;
        }

        @Override
        public void visit(ConstructorDeclaration md, Void arg){
            if(!isLibraryCheck) {
                if (!DataStore.memoryStatic.get(pathAbs)) DataStore.memoryStatic.put(pathAbs, md.isStatic());
            }

            String structureThis = this.structure + "/" + md.getNameAsString();
            Range range = md.getRange().get();
            BlockInformation BI = new BlockInformation(md.getNameAsString(), "Constructor", md.isStatic(), md.isPrivate(), structureThis, pathDir, range, this.rangeStructure);

            ArrayDeque<Range> rangeStructure = this.rangeStructure.clone();
            rangeStructure.add(range);

            for(Parameter tmp:md.getParameters()){
                paramList.add(tmp.getNameAsString());
                InBlockFirstVisitorS visitor = new InBlockFirstVisitorS(classname, range, rangeStructure, structureThis, paramList);
                tmp.accept(visitor, null);
                BI.setMemoryF(visitor.getLocal());
            }

            InBlockFirstVisitorS visitor = new InBlockFirstVisitorS(classname, range, rangeStructure, structureThis, paramList);
            md.getBody().accept(visitor, null);
            BI.setMemoryF(visitor.getLocal());
            for(BlockInformation BILow : visitor.getBILower()){
                BI.setMemory(BILow);
            }
            ClassInformation CI = DataStore.memoryClass.getData(pathAbs, classname);
            if(!CI.isContainStatic)CI.isContainStatic = md.isStatic();
            if (structure.equals(CI.blockStructure)){
                CI.setMemory(BI);
                DataStore.memoryClass.setData(pathAbs, classname, CI);
            } else {
                BIUpperStore.add(BI);
            }
        }

        public ArrayList<BlockInformation> getBILower() {
            return this.BIUpperStore;
        }
    }

    private static class InitializerFirstVisitor extends VoidVisitorAdapter<Void>{
        String classname = "";
        String structure = "";
        ArrayDeque<Range> rangeStructure = new ArrayDeque<>();
        private ArrayList<BlockInformation> BIUpperStore = new ArrayList<>();

        public InitializerFirstVisitor(String name, String structure, ArrayDeque<Range> rangeStructure){
            classname = name;
            this.structure = structure;
            this.rangeStructure = rangeStructure;
        }

        @Override
        public void visit(InitializerDeclaration md, Void arg){
            if(!isLibraryCheck) {
                if (!DataStore.memoryStatic.get(pathAbs)) DataStore.memoryStatic.put(pathAbs, md.isStatic());
            }

            String structureThis = this.structure + "/Initializer";
            Range range = md.getRange().get();
            BlockInformation BI = new BlockInformation("Initializer", "Initializer", md.isStatic(), structureThis, pathDir, range, this.rangeStructure);

            ArrayDeque<Range> rangeStructure = this.rangeStructure.clone();
            rangeStructure.add(range);
            InBlockFirstVisitorS visitor = new InBlockFirstVisitorS(classname, range, rangeStructure, structureThis);
            md.getBody().accept(visitor, null);
            BI.setMemoryF(visitor.getLocal());
            for(BlockInformation BILow : visitor.getBILower()){
                BI.setMemory(BILow);
            }
            ClassInformation CI = DataStore.memoryClass.getData(pathAbs, classname);
            if(!CI.isContainStatic)CI.isContainStatic = md.isStatic();
            if (structure.equals(CI.blockStructure)){
                CI.setMemory(BI);
                DataStore.memoryClass.setData(pathAbs, classname, CI);
            } else {
                BIUpperStore.add(BI);
            }
        }

        public ArrayList<BlockInformation> getBILower() {
            return this.BIUpperStore;
        }
    }

    private static class MethodFirstVisitor extends VoidVisitorAdapter<Void>{
        String classname = "";
        String structure = "";
        ArrayDeque<Range> rangeStructure = new ArrayDeque<>();
        ArrayList<String> paramList = new ArrayList<>();
        private ArrayList<BlockInformation> BIUpperStore = new ArrayList<>();

        public MethodFirstVisitor(String name, String structure, ArrayDeque<Range> rangeStructure){
            classname = name;
            this.structure = structure;
            this.rangeStructure = rangeStructure;
        }

        @Override
        public void visit(MethodDeclaration md, Void arg){
            boolean isAccessorCheckG = search_get(md);
            boolean isAccessorCheckS = search_set(md);
            boolean isStatic = false;
            boolean isKotlinPrivate = false;
            String methodname = md.getNameAsString();
            Range range = md.getRange().get();

            for(Modifier mod:md.getModifiers()){
                if(mod.toString().startsWith("static")) {
                    if(!isLibraryCheck) {
                        if (!DataStore.memoryStatic.get(pathAbs)) DataStore.memoryStatic.put(pathAbs, true);
                    }
                    isStatic = true;
                } else if(mod.toString().startsWith("private")){
                    isKotlinPrivate = true;
                }
            }

            int numLine = 0;
            if(md.getBody().isPresent())
                numLine = md.getBody().get().getChildNodes().size();

            boolean isReturnNull = true;
            String structureThis = this.structure + "/" + md.getNameAsString();
            MethodInformation MI
                    = new MethodInformation(methodname, structureThis, pathDir, isStatic, isKotlinPrivate,
                    isAccessorCheckG, isAccessorCheckS, false, "", numLine, isReturnNull, md.getType(), range, this.rangeStructure);
            ArrayDeque<Range> rangeStructure = this.rangeStructure.clone();
            rangeStructure.add(range);
            MI.setParamTypes(md.getParameters());

            for(Parameter param:md.getParameters()){
                paramList.add(param.getNameAsString());
                InBlockFirstVisitorS visitor = new InBlockFirstVisitorS(classname, range, rangeStructure, structureThis, paramList);
                param.accept(visitor, null);
                MI.setMemoryF(visitor.getLocal());
            }

            if(md.getBody().isPresent()){
                InBlockFirstVisitorS visitor = new InBlockFirstVisitorS(classname, range, rangeStructure, structureThis, paramList);
                md.getBody().get().accept(visitor, null);
                MI.setMemoryF(visitor.getLocal());
                for(BlockInformation BILow : visitor.getBILower()){
                    MI.setMemory(BILow);
                }
            }
            ClassInformation CI = DataStore.memoryClass.getData(pathAbs, classname);
            if(!CI.isContainStatic)CI.isContainStatic = isStatic;
            if (structure.equals(CI.blockStructure)){
                CI.setMemory(MI);
                DataStore.memoryClass.setData(pathAbs, classname, CI);
            } else {
                BIUpperStore.add(MI);
            }
        }

        public ArrayList<BlockInformation> getBILower() {
            return this.BIUpperStore;
        }
    }

    public static class InBlockFirstVisitorS extends VoidVisitorAdapter<Void>{
        String classname = "";
        String methodname = "";
        String structure = "";
        ArrayDeque<Range> rangeStructure = new ArrayDeque<>();
        Range range = null;
        ArrayList<String> paramList = new ArrayList<>();
        private ArrayList<FieldInformation> localValueList = new ArrayList<>();
        private ArrayList<BlockInformation> BIUpperStore = new ArrayList<>();

        //Method用のコンストラクタ
        public InBlockFirstVisitorS
        (String classname, String methodname, Range range, ArrayDeque<Range> rangeStructure, String structure,
         ArrayList<String> paramList){
            this.classname = classname;
            this.methodname = methodname;
            this.structure = structure;
            this.rangeStructure = rangeStructure;
            this.range = range;
            this.paramList = paramList;
        }

        //Constructor用のコンストラクタ
        public InBlockFirstVisitorS(String classname, Range range, ArrayDeque<Range> rangeStructure, String structure, ArrayList<String> paramList){
            this(classname, "", range, rangeStructure, structure, paramList);
        }

        //Initializer用のコンストラクタ
        public InBlockFirstVisitorS(String classname, Range range, ArrayDeque<Range> rangeStructure, String structure){
            this(classname, "", range, rangeStructure, structure, new ArrayList<String>());
        }

        @Override
        public void visit(AssignExpr md, Void arg){
            String structure = this.structure;
            if(md.getValue().isObjectCreationExpr()) structure = this.structure + "/" + md.getTarget().toString();
            structure = DataStore.structureChangeCheck(md.getValue(), this.structure, md.getTarget().toString());
            ValueInitialVisitor visitor = new ValueInitialVisitor(classname, methodname, range, rangeStructure, structure);
            md.getValue().accept(visitor, null);
            for (BlockInformation BILow : visitor.getBILower()) {
                BIUpperStore.add(BILow);
            }

        }

        @Override
        public void visit(DoStmt md, Void arg){
            //do-whileのブロック文内部の確認
            String structure = this.structure + "/Do-while";
            Range range = md.getRange().get();
            ArrayDeque<Range> rangeStructure = this.rangeStructure.clone();
            rangeStructure.add(range);

            //内部チェック
            InBlockFirstVisitorS bodyVisitor = new InBlockFirstVisitorS(classname, methodname, range, rangeStructure, structure, paramList);
            md.getBody().accept(bodyVisitor, null);

            BlockInformation BI = new BlockInformation("Do-while", "Do-while", structure, pathDir, range, this.rangeStructure);
            BI.setMemoryF(bodyVisitor.getLocal());
            for(BlockInformation BILow : bodyVisitor.getBILower()){
                BI.setMemory(BILow);
            }
            BIUpperStore.add(BI);
        }

        @Override
        public void visit(ForStmt md, Void arg){
            //forのブロック文内部の確認
            String structure = this.structure + "/For";
            Range range = md.getRange().get();
            ArrayDeque<Range> rangeStructure = this.rangeStructure.clone();
            rangeStructure.add(range);

            BlockInformation BI = new BlockInformation("For", "For", structure, pathDir, range, this.rangeStructure);

            //initializeが変数宣言だった場合の解析
            for(Expression expression:md.getInitialization()) {
                InBlockFirstVisitorS initVisitor = new InBlockFirstVisitorS(classname, methodname, this.range, rangeStructure, structure, paramList);
                expression.accept(initVisitor, null);
                BI.setMemoryF(initVisitor.getLocal());
                for(BlockInformation BILow : initVisitor.getBILower()){
                    BI.setMemory(BILow);
                }
            }

            //内部チェック
            InBlockFirstVisitorS bodyVisitor = new InBlockFirstVisitorS(classname, methodname, range, rangeStructure, structure, paramList);
            md.getBody().accept(bodyVisitor, null);
            BI.setMemoryF(bodyVisitor.getLocal());
            for (BlockInformation BILow : bodyVisitor.getBILower()) {
                BI.setMemory(BILow);
            }
            BIUpperStore.add(BI);
        }

        @Override
        public void visit(ForEachStmt md, Void arg){
            //foreachのiterableにあたる部分の読み込み変数のチェックとブロック文内部の確認
            String structure = this.structure + "/ForEach";
            Range range = md.getRange().get();
            ArrayDeque<Range> rangeStructure = this.rangeStructure.clone();
            rangeStructure.add(range);

            BlockInformation BI = new BlockInformation("ForEach", "ForEach", structure, pathDir, range, this.rangeStructure);

            InBlockFirstVisitorS variableVisitor = new InBlockFirstVisitorS(classname, methodname, this.range, rangeStructure, structure, paramList);
            md.getVariable().accept(variableVisitor, null);
            BI.setMemoryF(variableVisitor.getLocal());
            for(BlockInformation BILow : variableVisitor.getBILower()){
                BI.setMemory(BILow);
            }

            InBlockFirstVisitorS bodyVisitor = new InBlockFirstVisitorS(classname, methodname, range, rangeStructure, structure, paramList);
            md.getBody().accept(bodyVisitor, null);
            BI.setMemoryF(bodyVisitor.getLocal());
            for (BlockInformation BILow : bodyVisitor.getBILower()) {
                BI.setMemory(BILow);
            }
            BIUpperStore.add(BI);
        }

        @Override
        public void visit(IfStmt md, Void arg){
            //ifのブロック文内部の確認
            String structure = this.structure + "/if";
            Range range = md.getThenStmt().getRange().get();
            ArrayDeque<Range> rangeStructure = this.rangeStructure.clone();
            rangeStructure.add(range);

            //内部チェック
            InBlockFirstVisitorS thenBlockVisitor = new InBlockFirstVisitorS(classname, methodname, range, rangeStructure, structure, paramList);
            md.getThenStmt().accept(thenBlockVisitor, null);

            BlockInformation BI = new BlockInformation("If", "If", structure, pathDir, range, this.rangeStructure);
            BI.setMemoryF(thenBlockVisitor.getLocal());
            for (BlockInformation BILow : thenBlockVisitor.getBILower()) {
                BI.setMemory(BILow);
            }
            BIUpperStore.add(BI);

            //elseチェック
            if(md.getElseStmt().isPresent()){
                structure = this.structure + "/Else";
                range = md.getElseStmt().get().getRange().get();
                rangeStructure = this.rangeStructure.clone();
                rangeStructure.add(range);
                InBlockFirstVisitorS elseIfVisitor = new InBlockFirstVisitorS(classname, methodname, this.range, rangeStructure, structure, paramList);
                md.getElseStmt().get().accept(elseIfVisitor, null);
                BI = new BlockInformation("Else", "Else", structure, pathDir, range, this.rangeStructure);
                BI.setMemoryF(elseIfVisitor.getLocal());
                for (BlockInformation BILow : elseIfVisitor.getBILower()) {
                    BI.setMemory(BILow);
                }
                BIUpperStore.add(BI);
            }
        }

        @Override
        public void visit(LocalClassDeclarationStmt md, Void arg){
            FirstVisitor visitor = new FirstVisitor(classname, this.structure, this.rangeStructure);
            md.accept(visitor, null);
        }

        @Override
        public void visit(MethodCallExpr md, Void arg){
            ValueInitialVisitor visitor = new ValueInitialVisitor(classname, md.getNameAsString(), range, rangeStructure, structure);
            md.accept(visitor, null);
            for (BlockInformation BILow : visitor.getBILower()) {
                BIUpperStore.add(BILow);
            }
        }

        @Override
        public void visit(ObjectCreationExpr md, Void arg){
            ValueInitialVisitor visitor = new ValueInitialVisitor(classname, md.getType().toString(), range, rangeStructure, structure);
            md.accept(visitor, null);
            for (BlockInformation BILow : visitor.getBILower()) {
                BIUpperStore.add(BILow);
            }
        }

        @Override
        public void visit(Parameter md, Void arg){
            String fieldname = md.getNameAsString();
            Type type = md.getType();
            int dim = 0;
            boolean assign = false;
            boolean nullable = true;
            boolean initializable = true;
            String formerRW = "W";


            if(type.isArrayType()){
                /*int dimNum = type.length();
                type = type.replace("[]", "");
                dimNum = dimNum - type.length();

                 */
                dim = type.asArrayType().getArrayLevel();
            }

            Range range = md.getRange().get();

            String structureThis = this.structure + "/" + fieldname;
            FieldInformation FI
                    = new FieldInformation(fieldname, structureThis, pathDir, md.getType(), dim, assign,
                    nullable, initializable, type, formerRW, range, this.rangeStructure);

            localValueList.add(FI);
        }

        @Override
        public void visit(ReturnStmt md, Void arg){
            if(md.getExpression().isPresent()) {
                ValueInitialVisitor visitor = new ValueInitialVisitor(classname, methodname, range, rangeStructure, structure);
                md.getExpression().get().accept(visitor, null);
                for (BlockInformation BILow : visitor.getBILower()) {
                    BIUpperStore.add(BILow);
                }
            }
        }

        @Override
        public void visit(SwitchExpr md, Void arg){
            ValueInitialVisitor visitor = new ValueInitialVisitor(classname, methodname, range, rangeStructure, structure);
            md.accept(visitor, null);
        }

        @Override
        public void visit(SwitchStmt md, Void arg){
            //switchのブロック文内部の確認
            String structure = this.structure + "/Switch";
            Range range = md.getRange().get();
            ArrayDeque<Range> rangeStructure = this.rangeStructure.clone();
            rangeStructure.add(range);
            BlockInformation BI = new BlockInformation("Switch", "Switch", structure, pathDir, range, this.rangeStructure);

            for(SwitchEntry entry:md.getEntries()){
                String structureLabel = structure + "/";
                StringBuilder label = new StringBuilder();
                for(Expression expression:entry.getLabels()) label.append(expression);
                if(entry.getLabels().size() == 0) label.append("default");
                structureLabel = structureLabel + label;
                Range rangeEntry = entry.getRange().get();
                ArrayDeque<Range> rangeStructureEntry = rangeStructure.clone();
                rangeStructureEntry.add(rangeEntry);
                BlockInformation BIEntry = new BlockInformation("SwitchEntry", "SwitchEntry", structureLabel, pathDir, rangeEntry, rangeStructure);
                for(Statement statement:entry.getStatements()){
                    InBlockFirstVisitorS statementVisitor = new InBlockFirstVisitorS(classname, methodname, range, rangeStructureEntry, structureLabel, paramList);
                    statement.accept(statementVisitor, null);
                    BIEntry.setMemoryF(statementVisitor.getLocal());
                    for(BlockInformation BILow : statementVisitor.getBILower()){
                        BIEntry.setMemory(BILow);
                    }
                }
                BI.setMemory(BIEntry);
            }
            BIUpperStore.add(BI);

        }

        @Override
        public void visit(SynchronizedStmt md, Void arg) {
            String structureThis = this.structure + "/Synchronized";
            ArrayDeque<Range> rangeStructure = this.rangeStructure.clone();
            Range range = md.getRange().get();
            rangeStructure.add(range);
            InBlockFirstVisitorS bodyVisitor = new InBlockFirstVisitorS(classname, methodname, range, rangeStructure, structureThis, paramList);
            md.getBody().accept(bodyVisitor, null);

            BlockInformation BI = new BlockInformation("Synchronized", "Synchronized", structureThis, pathDir, range, this.rangeStructure);
            BI.setMemoryF(bodyVisitor.getLocal());
            for(BlockInformation BILow : bodyVisitor.getBILower()){
                BI.setMemory(BILow);
            }
            BIUpperStore.add(BI);
        }

        @Override
        public void visit(TryStmt md, Void arg){
            String structure = this.structure + "/Try";
            Range range = md.getTryBlock().getRange().get();
            ArrayDeque<Range> rangeStructure = this.rangeStructure.clone();
            rangeStructure.add(range);

            InBlockFirstVisitorS tryBlockVisitor = new InBlockFirstVisitorS(classname, methodname, range, rangeStructure, structure, paramList);
            md.getTryBlock().accept(tryBlockVisitor, null);

            BlockInformation BI = new BlockInformation("Try", "Try", structure, pathDir, range, this.rangeStructure);
            BI.setMemoryF(tryBlockVisitor.getLocal());
            for (BlockInformation BILow : tryBlockVisitor.getBILower()) {
                BI.setMemory(BILow);
            }
            BIUpperStore.add(BI);

            for (CatchClause catchClause : md.getCatchClauses()) {
                structure = this.structure + "/Catch";
                range = catchClause.getRange().get();
                rangeStructure = this.rangeStructure.clone();
                rangeStructure.add(range);

                InBlockFirstVisitorS catchBlockVisitor = new InBlockFirstVisitorS(classname, methodname, range, rangeStructure, structure, paramList);
                catchClause.accept(catchBlockVisitor, null);
                BI = new BlockInformation("Catch", "Catch", structure, pathDir, range, this.rangeStructure);
                BI.setMemoryF(catchBlockVisitor.getLocal());
                for (BlockInformation BILow : catchBlockVisitor.getBILower()) {
                    BI.setMemory(BILow);
                }
                BIUpperStore.add(BI);
            }

            if (md.getFinallyBlock().isPresent()) {
                structure = this.structure + "/Finally";
                range = md.getFinallyBlock().get().getRange().get();
                rangeStructure = this.rangeStructure.clone();
                rangeStructure.add(range);
                InBlockFirstVisitorS finallyBlockVisitor = new InBlockFirstVisitorS(classname, methodname, range, rangeStructure, structure, paramList);
                md.getFinallyBlock().get().accept(finallyBlockVisitor, null);

                BI = new BlockInformation("Finally", "Finally", structure, pathDir, range, this.rangeStructure);
                BI.setMemoryF(finallyBlockVisitor.getLocal());
                for (BlockInformation BILow : finallyBlockVisitor.getBILower()) {
                    BI.setMemory(BILow);
                }
                BIUpperStore.add(BI);
            }
        }

        @Override
        public void visit(VariableDeclarator md, Void arg){
            String fieldname = md.getNameAsString();
            Type type = md.getType();
            Type valueType = null;
            Range range = md.getRange().get();
            int dim = 0;
            boolean assign = false;
            boolean nullable = true;
            boolean initializable = true;
            String formerRW = "";
            String structureThis = structure + "/" + fieldname;

            if(md.getInitializer().isEmpty()) {
                assign = true;
                initializable = false;
                formerRW = "R";
            } else {
                formerRW = "W";
                ValueInitialVisitor visitor = new ValueInitialVisitor(classname, fieldname, range, rangeStructure, structureThis);
                md.getInitializer().get().accept(visitor, null);
                valueType = visitor.getTypeDef();

                for(BlockInformation BILow : visitor.getBILower()){
                    BIUpperStore.add(BILow);
                }

            }

            if(type.isArrayType()){
                /*int dimNum = type.length();
                type = type.replace("[]", "");
                dimNum = dimNum - type.length();

                 */
                dim = type.asArrayType().getArrayLevel();
            }

            FieldInformation FI
                    = new FieldInformation(fieldname, structureThis, pathDir, md.getType(), dim, assign,
                    nullable, initializable, valueType, formerRW, range, this.rangeStructure);

            localValueList.add(FI);
        }

        @Override
        public void visit(WhileStmt md, Void arg){
            //whileのブロック文内部の確認
            String structure = this.structure + "/While";
            Range range = md.getRange().get();
            ArrayDeque<Range> rangeStructure = this.rangeStructure.clone();
            rangeStructure.add(range);

            //内部チェック
            InBlockFirstVisitorS bodyVisitor = new InBlockFirstVisitorS(classname, methodname, range, rangeStructure, structure, paramList);
            md.getBody().accept(bodyVisitor, null);

            BlockInformation BI = new BlockInformation("While", "While", structure, pathDir, range, this.rangeStructure);
            BI.setMemoryF(bodyVisitor.getLocal());
            for(BlockInformation BILow : bodyVisitor.getBILower()){
                BI.setMemory(BILow);
            }
            BIUpperStore.add(BI);

        }

        public ArrayList<FieldInformation> getLocal(){
            return this.localValueList;
        }

        public ArrayList<BlockInformation> getBILower() {
            return this.BIUpperStore;
        }
    }

    public static class ValueInitialVisitor extends VoidVisitorAdapter<Void>{
        String classname = "";
        String contentsname = "";
        Range range = null;
        String structure = "";
        ArrayDeque<Range> rangeStructure = new ArrayDeque<>();
        ArrayList<String> paramList = new ArrayList<>();

        private ArrayList<BlockInformation> BIUpperStore = new ArrayList<>();
        private Type valueType = null;


        public ValueInitialVisitor
                (String classname, String contentsname, Range range, ArrayDeque<Range> rangeStructure, String structure) {
            this.classname = classname;
            this.contentsname = contentsname;
            this.range = range;
            this.rangeStructure = rangeStructure;
            this.structure = structure;
        }

        @Override
        public void visit(EnclosedExpr md, Void arg){
            super.visit(md, arg);
        }

        @Override
        public void visit(LambdaExpr md, Void arg){
            String structureThis = this.structure + "/" + contentsname;
            ArrayDeque<Range> rangeStructure = this.rangeStructure.clone();
            Range range = md.getRange().get();
            rangeStructure.add(range);
            BlockInformation BI = new BlockInformation(contentsname, "Lambda", structureThis, pathDir, range, this.rangeStructure);

            for(Parameter param:md.getParameters()){
                paramList.add(param.getNameAsString());
                InBlockFirstVisitorS visitor = new InBlockFirstVisitorS(classname, contentsname, range, rangeStructure, structureThis, paramList);
                param.accept(visitor, null);
                BI.setMemoryF(visitor.getLocal());
            }

            InBlockFirstVisitorS visitor = new InBlockFirstVisitorS(classname, contentsname, range, rangeStructure, structureThis, paramList);
            md.getBody().accept(visitor, null);
            BI.setMemoryF(visitor.getLocal());
            for(BlockInformation BILow : visitor.getBILower()){
                BI.setMemory(BILow);
            }
            BIUpperStore.add(BI);
        }

        @Override
        public void visit(MethodCallExpr md, Void arg){
            if(md.getScope().isPresent()){
                ValueInitialVisitor visitor = new ValueInitialVisitor(classname, md.getNameAsString(), range, rangeStructure, structure);
                md.getScope().get().accept(visitor, null);
                for(BlockInformation BILow : visitor.getBILower()){
                    BIUpperStore.add(BILow);
                }
            }
            if(md.getArguments().size() != 0){
                for(Expression expression:md.getArguments()){
                    String structureThis = structure;
                    if(expression.isObjectCreationExpr()) structureThis = structure + "/" + md.getNameAsString();
                    structureThis = DataStore.structureChangeCheck(expression, this.structure, md.getNameAsString());
                    ValueInitialVisitor visitor = new ValueInitialVisitor(classname, md.getNameAsString(), expression.getRange().get(), rangeStructure, structureThis);
                    expression.accept(visitor, null);
                    for(BlockInformation BILow : visitor.getBILower()){
                        BIUpperStore.add(BILow);
                    }

                }
            }

        }

        @Override
        public void visit(ObjectCreationExpr md, Void arg){
            valueType = md.getType();
            for(Expression expression:md.getArguments()) {
                String structureThis = this.structure;
                if(expression.isObjectCreationExpr()) structureThis = structure + "/" + md.getType();
                structureThis = DataStore.structureChangeCheck(expression, this.structure, md.getType().toString());
                ValueInitialVisitor visitor = new ValueInitialVisitor(classname, md.getType().toString(), range, rangeStructure, structureThis);
                expression.accept(visitor, null);
                for(BlockInformation BILow : visitor.getBILower()){
                    BIUpperStore.add(BILow);
                }
            }

            //匿名クラスの解析
            if (md.getAnonymousClassBody().isPresent()) {
                String structureThis = this.structure + "/" + md.getType();
                ArrayDeque<Range> rangeStructure = this.rangeStructure.clone();
                Range range = md.getRange().get();
                rangeStructure.add(range);
                BlockInformation BI = new BlockInformation(md.getType().toString(), "AnonymousClass", structureThis, pathDir, range, this.rangeStructure);

                for (BodyDeclaration bd : md.getAnonymousClassBody().get()) {
                    if(bd.isFieldDeclaration()){
                        InBlockFirstVisitorS visitor = new InBlockFirstVisitorS(classname, range, rangeStructure, structureThis, paramList);
                        bd.accept(visitor, null);
                        BI.setMemoryF(visitor.getLocal());
                    } else if(bd.isMethodDeclaration()){
                        MethodFirstVisitor visitor = new MethodFirstVisitor(this.classname, structureThis, rangeStructure);
                        md.accept(visitor, null);
                        for(BlockInformation BILow:visitor.getBILower()) BI.setMemory(BILow);
                    } else if(bd.isConstructorDeclaration()){
                        ConstructorFirstVisitor visitor = new ConstructorFirstVisitor(this.classname, structureThis, rangeStructure);
                        md.accept(visitor, null);
                        for(BlockInformation BILow:visitor.getBILower()) BI.setMemory(BILow);
                    } else {//initializer
                        InitializerFirstVisitor visitor = new InitializerFirstVisitor(this.classname, structureThis, rangeStructure);
                        md.accept(visitor, null);
                        for(BlockInformation BILow:visitor.getBILower()) BI.setMemory(BILow);
                    }
                }
                BIUpperStore.add(BI);
            }
        }

        @Override
        public void visit(SwitchExpr md, Void arg){
            //switchのブロック文内部の確認
            String structure = this.structure + "/Switch";
            Range range = md.getRange().get();
            ArrayDeque<Range> rangeStructure = this.rangeStructure.clone();
            rangeStructure.add(range);
            BlockInformation BI = new BlockInformation("Switch", "Switch", structure, pathDir, range, this.rangeStructure);

            for(SwitchEntry entry:md.getEntries()){
                String structureLabel = structure + "/";
                StringBuilder label = new StringBuilder();
                for(Expression expression:entry.getLabels()) label.append(expression);
                if(entry.getLabels().size() == 0) label.append("default");
                structureLabel = structureLabel + label;
                Range rangeEntry = entry.getRange().get();
                ArrayDeque<Range> rangeStructureEntry = rangeStructure.clone();
                rangeStructureEntry.add(rangeEntry);
                BlockInformation BIEntry = new BlockInformation("SwitchEntry", "SwitchEntry", structureLabel, pathDir, rangeEntry, rangeStructure);
                for(Statement statement:entry.getStatements()){
                    InBlockFirstVisitorS statementVisitor = new InBlockFirstVisitorS(classname, contentsname, range, rangeStructureEntry, structureLabel, paramList);
                    statement.accept(statementVisitor, null);
                    BIEntry.setMemoryF(statementVisitor.getLocal());
                    for(BlockInformation BILow : statementVisitor.getBILower()){
                        BIEntry.setMemory(BILow);
                    }
                }
                BI.setMemory(BIEntry);
            }
            BIUpperStore.add(BI);
        }

        public ArrayList<BlockInformation> getBILower() {
            return this.BIUpperStore;
        }

        public Type getTypeDef(){
            return this.valueType;
        }
    }

    //２回目の解析でもう少し詳しい情報を集める
    private static class FieldVisitor extends VoidVisitorAdapter<Void>{
        String classname = "";
        String pathAbs = "";
        String structure = "";
        ArrayDeque<Range> rangeStructure = new ArrayDeque<>();
        private boolean isAnonymousAnalysis = false;
        private BlockInformation BI = null;
        private ArrayDeque<BlockInformation> BIUpper = new ArrayDeque<>();
        public FieldVisitor(String name, String pathAbs, String structure, ArrayDeque<Range> rangeStructure,
                            BlockInformation BI){
            classname = name;
            this.pathAbs = pathAbs;
            this.structure = structure;
            this.rangeStructure = rangeStructure;
            this.BI = BI;
        }

        public FieldVisitor(String name, String pathAbs, String structure, ArrayDeque<Range> rangeStructure,
                            BlockInformation BI, ArrayDeque<BlockInformation> dequeBI, boolean flag) {
            this(name, pathAbs, structure, rangeStructure, BI);
            this.BIUpper = dequeBI.clone();
            isAnonymousAnalysis = flag;
        }


        @Override
        public void visit(FieldDeclaration md, Void arg) {
            NodeList<Modifier> modifiers = new NodeList<>();
            if (md.getModifiers().size() != 0) modifiers = md.getModifiers();
            for (VariableDeclarator vd : md.getVariables()) {
                VariableSecondVisitor VarVisitor
                        = new VariableSecondVisitor(this.classname, pathAbs, structure, this.rangeStructure, BI, BIUpper, modifiers, isAnonymousAnalysis);
                vd.accept(VarVisitor, null);
                BI = VarVisitor.getBIAfterAnalysis();
            }
        }

        public BlockInformation getBIAfterAnalysis(){
            return this.BI;
        }

        public ArrayDeque<BlockInformation> getBIUpperAfterAnalysis(){
            return this.BIUpper;
        }
    }

    private static class VariableSecondVisitor extends VoidVisitorAdapter<Void>{
        String classname = "";
        String pathAbs = "";
        String structure = "";
        ArrayDeque<Range> rangeStructure = new ArrayDeque<>();
        NodeList<Modifier> modifiers = new NodeList<>();
        private boolean isAnonymousAnalysis = false;
        private BlockInformation BI = null;
        private ArrayDeque<BlockInformation> BIUpper = new ArrayDeque<>();

        public VariableSecondVisitor(String name, String pathAbs, String structure,
                                     ArrayDeque<Range> rangeStructure, BlockInformation BI,
                                     NodeList<Modifier> modifiers){
            classname = name;
            this.pathAbs = pathAbs;
            this.structure = structure;
            this.rangeStructure = rangeStructure;
            this.BI = BI;
            this.modifiers = modifiers;
        }

        public VariableSecondVisitor(String name, String pathAbs, String structure,
                                     ArrayDeque<Range> rangeStructure, BlockInformation BI,
                                     ArrayDeque<BlockInformation> dequeBI,
                                     NodeList<Modifier> modifiers, boolean flag){
            this(name, pathAbs, structure, rangeStructure, BI, modifiers);
            this.BIUpper = dequeBI.clone();
            isAnonymousAnalysis = flag;
        }
            @Override
        public void visit(VariableDeclarator md, Void arg){
            String fieldname = md.getNameAsString();
            Type type = md.getType();
            Type valueType = null;
            Range range = md.getRange().get();
            boolean isStatic = false;
            boolean isKotlinPrivate = false;
            int dim = 0;
            boolean assign = false;
            boolean nullable = true;
            boolean initializable = true;
            boolean isAnonymous = false;
            boolean isLambda = false;
            boolean isFinal = false;
            String formerRW = "";

            if(md.getInitializer().isEmpty()) {
                assign = true;
                initializable = false;
                //formerRW = "R";
            } else {
                formerRW = "W";
                AssignVisitorS targetVisitor = new AssignVisitorS(classname, pathAbs, fieldname, range, rangeStructure, structure + "/" + fieldname, BI, BIUpper, false);
                md.getInitializer().get().accept(targetVisitor, null);
                valueType = targetVisitor.getTypeDef();
                nullable = targetVisitor.getIsReturnNull();
                isAnonymous = targetVisitor.isAnonymousAnalysis();
                isLambda = targetVisitor.isLambdaAnalysis();
                BI = targetVisitor.getBIAfterAnalysis();
            }
            
            if(type.isArrayType()){
                /*int dimNum = type.length();
                type = type.replace("[]", "");
                dimNum = dimNum - type.length();

                 */
                dim = type.asArrayType().getArrayLevel();
            }

            for(Modifier modifier:modifiers){
                if(modifier.toString().startsWith("static")) {
                    if(!isLibraryCheck) {
                        if (!DataStore.memoryStatic.get(pathAbs)) DataStore.memoryStatic.put(pathAbs, true);
                    }
                    isStatic = true;
                } else if(modifier.toString().startsWith("private")){
                    isKotlinPrivate = true;
                }else if(modifier.toString().startsWith("final")){
                    isFinal = true;
                }
            }

            FieldInformation FI = BI.getMemoryF().get(fieldname);
            if(FI == null) {
                FI = new FieldInformation(fieldname, structure + "/" + fieldname, pathDir,
                        md.getType(), dim, assign, nullable, isStatic, isKotlinPrivate, initializable,
                        valueType, formerRW, range, rangeStructure);
            }
            FI.isAnonymous = isAnonymous;
            FI.isLambda = isLambda;
            FI.nullable = nullable;

            if(valueType != null){
                FI.isConvertValueType = typeChangeCheck(type, valueType);
                if(FI.isConvertValueType) {
                    if(type.isClassOrInterfaceType() && valueType.isClassOrInterfaceType()){
                        type.replace(type.asClassOrInterfaceType().getName(), valueType.asClassOrInterfaceType().getName());
                        FI.valueType = type;
                    }
                }
            }

            if(BI instanceof ClassInformation){
                ClassInformation CITmp = (ClassInformation) BI;
                FI.isOverride = extendCheckField(fieldname, pathAbs, CITmp, isFinal) && !isFinal;
            }

            BI.setMemoryF(FI);
        }

        public BlockInformation getBIAfterAnalysis(){
            return this.BI;
        }

        public ArrayDeque<BlockInformation> getBIUpperAfterAnalysis(){
            return this.BIUpper;
        }
    }

    private static class ConstructorSecondVisitor extends VoidVisitorAdapter<Void>{
        String classname = "";
        String pathAbs = "";
        String structure = "";
        ArrayDeque<Range> rangeStructure = new ArrayDeque<>();
        ArrayList<String> paramList = new ArrayList<>();
        private boolean isAnonymousAnalysis = false;
        private BlockInformation BI = null;
        private ArrayDeque<BlockInformation> BIUpper = new ArrayDeque<>();

        public ConstructorSecondVisitor(String name, String pathAbs, String structure,
                                        ArrayDeque<Range> rangeStructure, BlockInformation BI){
            classname = name;
            this.pathAbs = pathAbs;
            this.structure = structure;
            this.rangeStructure = rangeStructure;
            this.BI = BI;
        }

        public ConstructorSecondVisitor(String name, String pathAbs, String structure,
                                        ArrayDeque<Range> rangeStructure, BlockInformation BI,
                                        ArrayDeque<BlockInformation> dequeBI, boolean flag){
            this(name, pathAbs, structure, rangeStructure, BI);
            this.BIUpper = dequeBI.clone();
            isAnonymousAnalysis = flag;
        }

            @Override
        public void visit(ConstructorDeclaration md, Void arg){
            if(!isLibraryCheck) {
                if (!DataStore.memoryStatic.get(pathAbs)) DataStore.memoryStatic.put(pathAbs, md.isStatic());
            }

            String structureThis = this.structure + "/" + md.getNameAsString();
            Range range = md.getRange().get();

            BlockInformation BI = this.BI.getMemoryData(structureThis, range);
            if(BI == null)
                BI = new BlockInformation(md.getNameAsString(), "Constructor", md.isStatic(), md.isPrivate(), structureThis, pathDir, range, this.rangeStructure);

            ArrayDeque<Range> rangeStructure = this.rangeStructure.clone();
            rangeStructure.add(range);
            if(!isAnonymousAnalysis) BIUpper.add(this.BI);

            for(Parameter tmp:md.getParameters()){
                paramList.add(tmp.getNameAsString());
                InBlockSecondVisitorS visitor = new InBlockSecondVisitorS(classname, pathAbs, range, rangeStructure, structureThis, BI, BIUpper, paramList);
                tmp.accept(visitor, null);
                BI = visitor.getBIAfterAnalysis();
                BIUpper = visitor.getBIUpperAfterAnalysis();
            }

            InBlockSecondVisitorS visitor = new InBlockSecondVisitorS(classname, pathAbs, range, rangeStructure, structureThis, BI, BIUpper, paramList);
            md.getBody().accept(visitor, null);
            BI = visitor.getBIAfterAnalysis();
            BIUpper = visitor.getBIUpperAfterAnalysis();

            if(!isAnonymousAnalysis) this.BI = BIUpper.pollLast();
            if (this.BI instanceof ClassInformation) {
                ((ClassInformation) this.BI).isContainStatic = md.isStatic() || ((ClassInformation) this.BI).isContainStatic;
            }
            this.BI.setMemory(BI);

        }

        public BlockInformation getBIAfterAnalysis(){
            return this.BI;
        }

        public ArrayDeque<BlockInformation> getBIUpperAfterAnalysis(){
            return this.BIUpper;
        }
    }

    private static class InitializerSecondVisitor extends VoidVisitorAdapter<Void>{
        String classname = "";
        String pathAbs = "";
        String structure = "";
        ArrayDeque<Range> rangeStructure = new ArrayDeque<>();
        private boolean isAnonymousAnalysis = false;
        private BlockInformation BI = null;
        private ArrayDeque<BlockInformation> BIUpper = new ArrayDeque<>();

        public InitializerSecondVisitor(String name, String pathAbs, String structure,
                                        ArrayDeque<Range> rangeStructure, BlockInformation BI){
            classname = name;
            this.pathAbs = pathAbs;
            this.structure = structure;
            this.rangeStructure = rangeStructure;
            this.BI = BI;
        }

        public InitializerSecondVisitor(String name, String pathAbs, String structure,
                                        ArrayDeque<Range> rangeStructure, BlockInformation BI,
                                        ArrayDeque<BlockInformation> dequeBI, boolean flag){
            this(name, pathAbs, structure, rangeStructure, BI);
            this.BIUpper = dequeBI.clone();
            isAnonymousAnalysis = flag;
        }

            @Override
        public void visit(InitializerDeclaration md, Void arg){
            if(!isLibraryCheck) {
                if (!DataStore.memoryStatic.get(pathAbs)) DataStore.memoryStatic.put(pathAbs, md.isStatic());
            }

            String structureThis = this.structure + "/Initializer";
            Range range = md.getRange().get();

            BlockInformation BI = this.BI.getMemoryData(structureThis, range);
            if(BI == null) BI = new BlockInformation("Initializer", "Initializer", md.isStatic(), structureThis, pathDir, range, this.rangeStructure);

            ArrayDeque<Range> rangeStructure = this.rangeStructure.clone();
            rangeStructure.add(range);

            if(!isAnonymousAnalysis) BIUpper.add(this.BI);
            InBlockSecondVisitorS visitor = new InBlockSecondVisitorS(classname, pathAbs, range, rangeStructure, structureThis, BI, BIUpper);
            md.getBody().accept(visitor, null);
            BI = visitor.getBIAfterAnalysis();
            BIUpper = visitor.getBIUpperAfterAnalysis();

            if(!isAnonymousAnalysis) this.BI = BIUpper.pollLast();

            if (this.BI instanceof ClassInformation) {
                ((ClassInformation) this.BI).isContainStatic = md.isStatic() || ((ClassInformation) this.BI).isContainStatic;
            }
            this.BI.setMemory(BI);
        }

        public BlockInformation getBIAfterAnalysis(){
            return this.BI;
        }

        public ArrayDeque<BlockInformation> getBIUpperAfterAnalysis(){
            return this.BIUpper;
        }
    }

    private static class MethodSecondVisitor extends VoidVisitorAdapter<Void>{
        String classname = "";
        String pathAbs = "";
        String methodname = "";
        String fieldname = "";
        String structure = "";
        ArrayDeque<Range> rangeStructure = new ArrayDeque<>();
        boolean isStatic = false;
        boolean isAccessorCheckG = false;
        boolean isAccessorCheckS = false;
        boolean isFixableG = false;
        boolean isFixableS = false;
        int numLine = 0;
        ArrayList<String> paramList = new ArrayList<>();
        private boolean isAnonymousAnalysis = false;
        private BlockInformation BI = null;
        private ArrayDeque<BlockInformation> BIUpper = new ArrayDeque<>();


        public MethodSecondVisitor(String name, String pathAbs, String structure,
                                   ArrayDeque<Range> rangeStructure, BlockInformation BI){
            classname = name;
            this.pathAbs = pathAbs;
            this.structure = structure;
            this.rangeStructure = rangeStructure;
            this.BI = BI;
            paramList = new ArrayList<>();
        }

        public MethodSecondVisitor(String name, String pathAbs, String structure,
                                   ArrayDeque<Range> rangeStructure, BlockInformation BI,
                                   ArrayDeque<BlockInformation> dequeBI, boolean flag){
            this(name, pathAbs, structure, rangeStructure, BI);
            this.BIUpper = dequeBI.clone();
            isAnonymousAnalysis = flag;
            paramList = new ArrayList<>();
        }

        @Override
        public void visit(MethodDeclaration md, Void arg){
            isAccessorCheckG = search_get(md);
            isAccessorCheckS = search_set(md);
            methodname = md.getNameAsString();
            Range range = md.getRange().get();
            Type retType = md.getType();

            for(Modifier mod:md.getModifiers()){
                if(mod.toString().equals("static ")) {
                    if(!isLibraryCheck) {
                        if (!DataStore.memoryStatic.get(pathAbs)) DataStore.memoryStatic.put(pathAbs, true);
                    }
                    isStatic = true;
                    break;
                }
            }

            if(md.getBody().isPresent())
                numLine = md.getBody().get().getChildNodes().size();

            String structureThis = this.structure + "/" + md.getNameAsString();


            NodeList<Parameter> parameters = md.getParameters();
            Type[] types = DataStore.ParamToTypes(parameters);

            MethodInformation MI = (MethodInformation) this.BI.getMemoryData(structureThis, range);
            if(MI == null) MI = new MethodInformation(methodname, structureThis, pathDir, isStatic, md.isPrivate(), isAccessorCheckG, isAccessorCheckS, false, fieldname, numLine, false, md.getType(), range, this.rangeStructure);

            ArrayDeque<Range> rangeStructure = this.rangeStructure.clone();
            rangeStructure.add(range);
            //MI.setParamTypes(md.getParameters());

            if(!isAnonymousAnalysis)BIUpper.add(this.BI);

            for(Parameter param:parameters){
                paramList.add(param.getNameAsString());
                InBlockSecondVisitorS visitor = new InBlockSecondVisitorS(classname, pathAbs,  methodname, range, rangeStructure, structureThis, isAccessorCheckG, isAccessorCheckS, MI, BIUpper, paramList);
                param.accept(visitor, null);
                if(visitor.getBIAfterAnalysis() instanceof MethodInformation)
                    MI = (MethodInformation) visitor.getBIAfterAnalysis();
                BIUpper = visitor.getBIUpperAfterAnalysis();
            }

            if(md.getBody().isPresent()) {
                InBlockSecondVisitorS visitor = new InBlockSecondVisitorS(classname, pathAbs,  methodname, range, rangeStructure, structureThis, isAccessorCheckG, isAccessorCheckS, MI, BIUpper, paramList);
                md.getBody().get().accept(visitor, null);
                fieldname = visitor.getFieldname();
                isFixableG = visitor.getIsFixableG();
                isFixableS = visitor.getIsFixableS();
                MI.nullable = visitor.getIsReturnNull();
                retType = visitor.getRetType();
                if(visitor.getBIAfterAnalysis() instanceof MethodInformation)
                    MI = (MethodInformation) visitor.getBIAfterAnalysis();
                BIUpper = visitor.getBIUpperAfterAnalysis();
            }

            //fix check
            if(isAccessorCheckG || isAccessorCheckS) {
                String fieldname = methodname.substring(3, 4).toLowerCase() + methodname.substring(4);
                if (DataStore.memoryClass.getData(pathAbs, classname).getMemoryF().get(fieldname) != null) {
                    FieldInformation FIM = DataStore.memoryClass.getData(pathAbs, classname).getMemoryF().get(fieldname);
                    Type retTypeM = FIM.type;
                    if (isAccessorCheckG && isFixableG) {
                        if (retTypeM.toString().equals(md.getType().toString())) MI.accessField = fieldname;
                        else isFixableG = false;
                    } else if (isAccessorCheckS && isFixableS) {
                        if (retTypeM.toString().equals(types[0].toString())) MI.accessField = fieldname;
                        else isFixableS = false;
                    }
                } else {
                    isFixableG = false;
                    isFixableS = false;
                }
            }


            MI.fix = isFixableS||isFixableG;

            if(typeChangeCheck(md.getType(), retType)){
                MI.returnType = retType;
                MI.isConvertReturnType = true;
            }
            /*ClassInformation CI = DataStore.memoryClass.getData(pathAbs, classname);
            if(!CI.isContainStatic)CI.isContainStatic = isStatic;
            if (structure.equals(CI.blockStructure)){
                CI.setMemoryM(MI);
                DataStore.memoryClass.setData(pathAbs, classname, CI);
            }*/
            if(!isAnonymousAnalysis)this.BI = BIUpper.pollLast();

            if (this.BI instanceof ClassInformation) {
                ((ClassInformation) this.BI).isContainStatic = md.isStatic() || ((ClassInformation) this.BI).isContainStatic;
                if (md.getAnnotations().size() != 0) {
                    for (int i = 0; i < md.getAnnotations().size(); i++) {
                        if (md.getAnnotation(i).getNameAsString().equals("Override")) {
                            MI.isOverride = extendCheckMethod(methodname, types, pathAbs, (ClassInformation) this.BI);
                        }
                    }
                }
            }
            this.BI.setMemory(MI);
        }

        public BlockInformation getBIAfterAnalysis(){
            return this.BI;
        }

        public ArrayDeque<BlockInformation> getBIUpperAfterAnalysis(){
            return this.BIUpper;
        }
    }

    public static class InBlockSecondVisitorS extends VoidVisitorAdapter<Void>{
        String classname = "";
        String pathAbs = "";
        String methodname = "";
        String structure = "";
        Range range = null;
        ArrayDeque<Range> rangeStructure = new ArrayDeque<>();
        private String fieldname = "";
        private boolean isAccessorCheckG = false;
        private boolean isAccessorCheckS = false;
        private boolean isFixableG = false;
        private boolean isFixableS = false;
        private boolean isReturnNull = false;
        private Type retType = null;
        ArrayList<String> paramList = new ArrayList<>();
        private BlockInformation BI = null;
        private ArrayDeque<BlockInformation> BIUpper = new ArrayDeque<>();

        //Method用のコンストラクタ
        public InBlockSecondVisitorS
                (String classname, String pathAbs, String methodname, Range range,
                 ArrayDeque<Range> rangeStructure, String structure,
                 boolean isAccessorCheckG, boolean isAccessorCheckS, BlockInformation BI,
                 ArrayDeque<BlockInformation> dequeBI, ArrayList<String> paramList){
            this.classname = classname;
            this.pathAbs = pathAbs;
            this.methodname = methodname;
            this.structure = structure;
            this.rangeStructure = rangeStructure;
            this.range = range;
            this.isAccessorCheckG = isAccessorCheckG;
            this.isAccessorCheckS = isAccessorCheckS;
            this.BI = BI;
            this.BIUpper = dequeBI.clone();
            this.paramList = paramList;
        }

        //Constructor用のコンストラクタ
        public InBlockSecondVisitorS(String classname, String pathAbs, Range range,
                                     ArrayDeque<Range> rangeStructure, String structure,
                                     BlockInformation BI, ArrayDeque<BlockInformation> dequeBI, 
                                     ArrayList<String> paramList){
            this(classname, pathAbs, "", range, rangeStructure, structure, false, false, BI, dequeBI, paramList);
        }

        //Initializer用のコンストラクタ
        public InBlockSecondVisitorS(String classname, String pathAbs, Range range,
                                     ArrayDeque<Range> rangeStructure, String structure,
                                     BlockInformation BI, ArrayDeque<BlockInformation> dequeBI){
            this(classname, pathAbs, "", range, rangeStructure, structure, false, false, BI, dequeBI, new ArrayList<String>());
        }

        @Override
        public void visit(AssignExpr md, Void arg){
            String structure = this.structure;
            String value = md.getValue().toString();
            String contents = methodname;
            boolean isParentAssign = md.getParentNode().get() instanceof AssignExpr;
            boolean isValueAssign = md.getValue().isAssignExpr();
            boolean isMultiAssign = !isParentAssign && isValueAssign;

            //式の右側の解析
            if(md.getValue().isObjectCreationExpr()) {
                //contents = md.getTarget().toString();
                structure = structure + "/" + md.getTarget().toString();
            }
            structure = DataStore.structureChangeCheck(md.getValue(), this.structure, md.getTarget().toString());
            if(isMultiAssign){
                InBlockSecondVisitorS valueVisitor = new InBlockSecondVisitorS(classname, pathAbs, contents, range, rangeStructure, structure, isAccessorCheckG, isAccessorCheckS, BI, BIUpper, paramList);
                md.getValue().accept(valueVisitor, null);
                BI = valueVisitor.getBIAfterAnalysis();
                BIUpper = valueVisitor.getBIUpperAfterAnalysis();
            } else {
                //sus point
                AssignVisitorS valueVisitor = new AssignVisitorS(classname, pathAbs, contents, this.range, rangeStructure, structure, isAccessorCheckG, isAccessorCheckS, BI, BIUpper, paramList, false);
                md.getValue().accept(valueVisitor, null);
                BI = valueVisitor.getBIAfterAnalysis();
                BIUpper = valueVisitor.getBIUpperAfterAnalysis();
            }

            //多重代入の真ん中解析
            if(isMultiAssign){
                AssignVisitorS valueVisitor = new AssignVisitorS(classname, pathAbs, contents, this.range, rangeStructure, structure, isAccessorCheckG, isAccessorCheckS, BI, BIUpper, paramList, false);
                md.getValue().asAssignExpr().getTarget().accept(valueVisitor, null);
                BI = valueVisitor.getBIAfterAnalysis();
                BIUpper = valueVisitor.getBIUpperAfterAnalysis();
            }

            //式の左側の解析

            //代入以外のAssign文の場合、一度値を参照してから代入するので、先にReadのチェック
            if (!md.getOperator().toString().equals("ASSIGN")) {
                AssignVisitorS targetVisitor = new AssignVisitorS(classname, pathAbs, contents, this.range, rangeStructure, structure, isAccessorCheckG, isAccessorCheckS, BI, BIUpper, paramList, false, value);
                md.getTarget().accept(targetVisitor, null);
                BI = targetVisitor.getBIAfterAnalysis();
                BIUpper = targetVisitor.getBIUpperAfterAnalysis();
            }
            AssignVisitorS targetVisitor = new AssignVisitorS(classname, pathAbs, contents, this.range, rangeStructure, structure, isAccessorCheckG, isAccessorCheckS, BI, BIUpper, paramList, true, value);
            md.getTarget().accept(targetVisitor, null);
            BI = targetVisitor.getBIAfterAnalysis();
            BIUpper = targetVisitor.getBIUpperAfterAnalysis();
            isFixableS = targetVisitor.getIsFixableS();
            fieldname = targetVisitor.getFieldname();
        }

        @Override
        public void visit(DoStmt md, Void arg){
            //do-whileのconditionにあたる部分の読み込み変数のチェックとブロック文内部の確認
            String structure = this.structure + "/Do-while";
            Range range = md.getRange().get();
            ArrayDeque<Range> rangeStructure = this.rangeStructure.clone();
            rangeStructure.add(range);
            //conditionチェック
            AssignVisitorS conditionVisitor = new AssignVisitorS(classname, pathAbs, methodname, this.range, this.rangeStructure, structure, isAccessorCheckG, isAccessorCheckS, BI, BIUpper, paramList, false);
            md.getCondition().accept(conditionVisitor, null);
            BI = conditionVisitor.getBIAfterAnalysis();
            BIUpper = conditionVisitor.getBIUpperAfterAnalysis();

            BlockInformation BI = this.BI.getMemoryData(structure, range);//new BlockInformation("Do-while", "Do-while", false, structure, pathDir, range, this.rangeStructure);

            BIUpper.add(this.BI);

            //内部チェック
            InBlockSecondVisitorS bodyVisitor = new InBlockSecondVisitorS(classname, pathAbs, methodname, this.range, rangeStructure, structure, isAccessorCheckG, isAccessorCheckS, BI, BIUpper, paramList);
            md.getBody().accept(bodyVisitor, null);
            BI = bodyVisitor.getBIAfterAnalysis();
            BIUpper = bodyVisitor.getBIUpperAfterAnalysis();
            setIsReturnNull(bodyVisitor.getIsReturnNull());

            this.BI = BIUpper.pollLast();
            this.BI.setMemory(BI);
            /*ArrayDeque<BlockInformation> dequeBI = getBIfromClass(pathAbs, this.classname, this.rangeStructure, structure);
            BlockInformation BIUpper = dequeBI.pollLast();
            BIUpper.setMemory(BI);
            BI = storeBItoClass(dequeBI, BIUpper);
            ClassInformation CI = DataStore.memoryClass.getData(pathAbs, classname);
            CI.setMemory(BI);
            DataStore.memoryClass.setData(pathAbs, classname, CI);

             */
        }

        @Override
        public void visit(ForStmt md, Void arg){
            //forのinitial, compare, updateにあたる部分の読み込み変数のチェックとブロック文内部の確認
            String structure = this.structure + "/For";
            Range range = md.getRange().get();
            ArrayDeque<Range> rangeStructure = this.rangeStructure.clone();
            rangeStructure.add(range);
            BlockInformation BI = this.BI.getMemoryData(structure, range);//new BlockInformation("For", "For", false, structure, pathDir, range, this.rangeStructure);

            BIUpper.add(this.BI);
            //initialチェック
            for(Expression expression:md.getInitialization()) {
                InBlockSecondVisitorS initVisitor = new InBlockSecondVisitorS(classname, pathAbs, methodname, this.range, rangeStructure, structure, isAccessorCheckG, isAccessorCheckS, BI, BIUpper, paramList);
                expression.accept(initVisitor, null);
                BI = initVisitor.getBIAfterAnalysis();
                BIUpper = initVisitor.getBIUpperAfterAnalysis();
            }

            //compareチェック
            if(md.getCompare().isPresent()) {
                AssignVisitorS compareVisitor = new AssignVisitorS(classname, pathAbs, methodname, this.range, rangeStructure, structure, isAccessorCheckG, isAccessorCheckS, BI, BIUpper, paramList, false);
                md.getCompare().get().accept(compareVisitor, null);
                BI = compareVisitor.getBIAfterAnalysis();
                BIUpper = compareVisitor.getBIUpperAfterAnalysis();
            }

            //updateチェック
            for(Expression expression:md.getUpdate()){
                AssignVisitorS updateVisitor = new AssignVisitorS(classname, pathAbs, methodname, this.range, rangeStructure, structure, isAccessorCheckG, isAccessorCheckS, BI, BIUpper, paramList, false);
                expression.accept(updateVisitor, null);
                BI = updateVisitor.getBIAfterAnalysis();
                BIUpper = updateVisitor.getBIUpperAfterAnalysis();
            }

            //内部チェック
            InBlockSecondVisitorS bodyVisitor = new InBlockSecondVisitorS(classname, pathAbs, methodname, this.range, rangeStructure, structure, isAccessorCheckG, isAccessorCheckS, BI, BIUpper, paramList);
            md.getBody().accept(bodyVisitor, null);
            BI = bodyVisitor.getBIAfterAnalysis();
            BIUpper = bodyVisitor.getBIUpperAfterAnalysis();
            setIsReturnNull(bodyVisitor.getIsReturnNull());

            this.BI = BIUpper.pollLast();
            this.BI.setMemory(BI);
            /*ArrayDeque<BlockInformation> dequeBI = getBIfromClass(pathAbs, this.classname, this.rangeStructure, structure);
            BlockInformation BIUpper = dequeBI.pollLast();
            BIUpper.setMemory(BI);
            BI = storeBItoClass(dequeBI, BIUpper);
            ClassInformation CI = DataStore.memoryClass.getData(pathAbs, classname);
            CI.setMemory(BI);
            DataStore.memoryClass.setData(pathAbs, classname, CI);

             */

        }

        @Override
        public void visit(ForEachStmt md, Void arg){
            //foreachのiterableにあたる部分の読み込み変数のチェックとブロック文内部の確認
            String structure = this.structure + "/ForEach";
            Range range = md.getRange().get();
            ArrayDeque<Range> rangeStructure = this.rangeStructure.clone();
            rangeStructure.add(range);

            BlockInformation BI = this.BI.getMemoryData(structure, range);//new BlockInformation("ForEach", "ForEach", structure, pathDir, range, this.rangeStructure);

            BIUpper.add(this.BI);

            InBlockSecondVisitorS variableVisitor = new InBlockSecondVisitorS(classname, pathAbs, methodname, this.range, rangeStructure, structure, isAccessorCheckG, isAccessorCheckS, BI, BIUpper, paramList);
            md.getVariable().accept(variableVisitor, null);
            BI = variableVisitor.getBIAfterAnalysis();
            BIUpper = variableVisitor.getBIUpperAfterAnalysis();

            AssignVisitorS iterableVisitor = new AssignVisitorS(classname, pathAbs, methodname, this.range, rangeStructure, structure, isAccessorCheckG, isAccessorCheckS, BI, BIUpper, paramList, false);
            md.getIterable().accept(iterableVisitor, null);
            BI = iterableVisitor.getBIAfterAnalysis();
            BIUpper = iterableVisitor.getBIUpperAfterAnalysis();

            InBlockSecondVisitorS bodyVisitor = new InBlockSecondVisitorS(classname, pathAbs, methodname, this.range, rangeStructure, structure, isAccessorCheckG, isAccessorCheckS, BI, BIUpper, paramList);
            md.getBody().accept(bodyVisitor, null);
            BI = bodyVisitor.getBIAfterAnalysis();
            BIUpper = bodyVisitor.getBIUpperAfterAnalysis();
            setIsReturnNull(bodyVisitor.getIsReturnNull());

            this.BI = BIUpper.pollLast();
            this.BI.setMemory(BI);
        }

        @Override
        public void visit(IfStmt md, Void arg){
            //ifのconditionにあたる部分の読み込み変数のチェックとブロック文内部の確認
            String structureThis = this.structure + "/if";
            Range range = md.getThenStmt().getRange().get();
            ArrayDeque<Range> rangeStructure = this.rangeStructure.clone();
            rangeStructure.add(range);

            //conditionチェック
            AssignVisitorS conditionVisitor = new AssignVisitorS(classname, pathAbs, methodname, this.range, this.rangeStructure, this.structure, isAccessorCheckG, isAccessorCheckS, BI, BIUpper, paramList, false);
            md.getCondition().accept(conditionVisitor, null);
            BI = conditionVisitor.getBIAfterAnalysis();
            BIUpper = conditionVisitor.getBIUpperAfterAnalysis();

            BlockInformation BI = this.BI.getMemoryData(structureThis, range);//new BlockInformation("If", "If", false, structure, pathDir, range, this.rangeStructure);
            BIUpper.add(this.BI);

            InBlockSecondVisitorS thenBlockVisitor = new InBlockSecondVisitorS(classname, pathAbs, methodname, range, rangeStructure, structureThis, isAccessorCheckG, isAccessorCheckS, BI, BIUpper, paramList);
            md.getThenStmt().accept(thenBlockVisitor, null);
            BI = thenBlockVisitor.getBIAfterAnalysis();
            BIUpper = thenBlockVisitor.getBIUpperAfterAnalysis();
            setIsReturnNull(thenBlockVisitor.getIsReturnNull());

            this.BI = BIUpper.pollLast();
            this.BI.setMemory(BI);
            /*ArrayDeque<BlockInformation> dequeBI = getBIfromClass(pathAbs, this.classname, this.rangeStructure, structure);
            BlockInformation BIUpper = dequeBI.pollLast();
            BIUpper.setMemory(BI);
            BI = storeBItoClass(dequeBI, BIUpper);
            ClassInformation CI = DataStore.memoryClass.getData(pathAbs, classname);
            CI.setMemory(BI);
            DataStore.memoryClass.setData(pathAbs, classname, CI);

             */

            //elseチェック
            if(md.getElseStmt().isPresent()){
                structureThis = this.structure + "/Else";
                range = md.getElseStmt().get().getRange().get();
                rangeStructure = this.rangeStructure.clone();
                rangeStructure.add(range);
                BI = this.BI.getMemoryData(structureThis, range);//new BlockInformation("Else", "Else", false, structure, pathDir, range, this.rangeStructure);

                BIUpper.add(this.BI);
                InBlockSecondVisitorS elseIfVisitor = new InBlockSecondVisitorS(classname, pathAbs, methodname, this.range, rangeStructure, structureThis, isAccessorCheckG, isAccessorCheckS, BI, BIUpper, paramList);
                md.getElseStmt().get().accept(elseIfVisitor, null);
                BI = elseIfVisitor.getBIAfterAnalysis();
                BIUpper = elseIfVisitor.getBIUpperAfterAnalysis();
                setIsReturnNull(elseIfVisitor.getIsReturnNull());

                this.BI = BIUpper.pollLast();
                this.BI.setMemory(BI);
                /*dequeBI = getBIfromClass(pathAbs, this.classname, this.rangeStructure, structure);
                BIUpper = dequeBI.pollLast();
                BIUpper.setMemory(BI);
                BI = storeBItoClass(dequeBI, BIUpper);
                CI = DataStore.memoryClass.getData(pathAbs, classname);
                CI.setMemory(BI);
                DataStore.memoryClass.setData(pathAbs, classname, CI);

                 */
            }
        }

        @Override
        public void visit(LabeledStmt md, Void arg){
            InBlockSecondVisitorS visitor = new InBlockSecondVisitorS(classname, pathAbs,  methodname, range, rangeStructure, structure, isAccessorCheckG, isAccessorCheckS, BI, BIUpper, paramList);
            md.getStatement().accept(visitor, null);
            BI = visitor.getBIAfterAnalysis();
            BIUpper = visitor.getBIUpperAfterAnalysis();
            setIsReturnNull(visitor.getIsReturnNull());
        }

        @Override
        public void visit(MethodCallExpr md, Void arg){
            //メソッド呼び出しの引数に使われているかどうかを確認
            //ここでは何もせず、visitorに明け渡す
            AssignVisitorS visitor = new AssignVisitorS(classname, pathAbs, methodname, this.range, rangeStructure, structure, isAccessorCheckG, isAccessorCheckS, BI, BIUpper, paramList, false);
            md.accept(visitor, null);
            BI = visitor.getBIAfterAnalysis();
            BIUpper = visitor.getBIUpperAfterAnalysis();
            setIsReturnNull(visitor.getIsReturnNull());
        }

        @Override
        public void visit(ObjectCreationExpr md, Void arg){
            //ここでは何もせず、visitorに明け渡す
            AssignVisitorS visitor = new AssignVisitorS(classname, pathAbs, methodname, this.range, rangeStructure, structure, isAccessorCheckG, isAccessorCheckS, BI, BIUpper, paramList, false);
            md.accept(visitor, null);
            BI = visitor.getBIAfterAnalysis();
            BIUpper = visitor.getBIUpperAfterAnalysis();
            setIsReturnNull(visitor.getIsReturnNull());
        }

        @Override
        public void visit(Parameter md, Void arg) {
            String fieldname = md.getNameAsString();
            Type type = md.getType();
            int dim = 0;
            boolean assign = false;
            boolean nullable = true;
            boolean initializable = true;
            String formerRW = "W";


            if (type.isArrayType()) {
                /*int dimNum = type.length();
                type = type.replace("[]", "");
                dimNum = dimNum - type.length();

                 */
                dim = type.asArrayType().getArrayLevel();
            }

            Range range = md.getRange().get();

            String structureThis = this.structure + "/" + fieldname;

            FieldInformation FI = BI.getMemoryF().get(fieldname);
            if(FI == null) FI = new FieldInformation(fieldname, structureThis, pathDir, md.getType(), dim, assign,
                    nullable, initializable, type, formerRW, range, this.rangeStructure);
            BI.setMemoryF(FI);

        }

        @Override
        public void visit(ReturnStmt md, Void arg){
            if(md.getExpression().isPresent()) {
                AssignVisitorS visitor = new AssignVisitorS(classname, pathAbs, methodname, this.range, rangeStructure, structure, isAccessorCheckG, isAccessorCheckS, BI, BIUpper, paramList, false);
                md.getExpression().get().accept(visitor, null);
                isFixableG = visitor.getIsFixableG();
                fieldname = visitor.getFieldname();
                setIsReturnNull(visitor.getIsReturnNull());
                retType = visitor.getTypeDef();
                BI = visitor.getBIAfterAnalysis();
                BIUpper = visitor.getBIUpperAfterAnalysis();
            }
        }

        @Override
        public void visit(SwitchExpr md, Void arg){
            AssignVisitorS visitor = new AssignVisitorS(classname, pathAbs, methodname, this.range, rangeStructure, structure, isAccessorCheckG, isAccessorCheckS, BI, BIUpper, paramList, false);
            md.accept(visitor, null);
            BI = visitor.getBIAfterAnalysis();
            BIUpper = visitor.getBIUpperAfterAnalysis();
            isFixableG = visitor.getIsFixableG();
            fieldname = visitor.getFieldname();
            setIsReturnNull(visitor.getIsReturnNull());
        }

        @Override
        public void visit(SwitchStmt md, Void arg){
            //switchのselectorにあたる部分の読み込み変数のチェックとブロック文内部の確認
            String structure = this.structure + "/Switch";
            Range range = md.getRange().get();
            ArrayDeque<Range> rangeStructure = this.rangeStructure.clone();
            rangeStructure.add(range);

            AssignVisitorS selectorVisitor = new AssignVisitorS(classname, pathAbs, methodname, this.range, this.rangeStructure, this.structure, isAccessorCheckG, isAccessorCheckS, BI, BIUpper, paramList, false);
            md.getSelector().accept(selectorVisitor, null);
            BI = selectorVisitor.getBIAfterAnalysis();
            BIUpper = selectorVisitor.getBIUpperAfterAnalysis();

            BlockInformation BI = this.BI.getMemoryData(structure, range);//new BlockInformation("Switch", "Switch", false, structure, pathDir, range, this.rangeStructure);

            BIUpper.add(this.BI);

            for(SwitchEntry entry:md.getEntries()){
                String structureLabel = structure + "/";
                StringBuilder label = new StringBuilder();
                for(Expression expression:entry.getLabels()) label.append(expression);
                if(entry.getLabels().size() == 0) label.append("default");
                structureLabel = structureLabel + label;
                Range rangeEntry = entry.getRange().get();
                ArrayDeque<Range> rangeStructureEntry = rangeStructure.clone();
                rangeStructureEntry.add(rangeEntry);
                BlockInformation BIEntry = BI.getMemoryData(structureLabel, rangeEntry);//new BlockInformation("SwitchEntry", "SwitchEntry", structureLabel, pathDir, rangeEntry, rangeStructure);
                BIUpper.add(BI);
                for(Statement statement:entry.getStatements()){
                    InBlockSecondVisitorS statementVisitor = new InBlockSecondVisitorS(classname, pathAbs, methodname, range, rangeStructureEntry, structureLabel, isAccessorCheckG, isAccessorCheckS, BIEntry, BIUpper, paramList);
                    statement.accept(statementVisitor, null);
                    BIEntry = statementVisitor.getBIAfterAnalysis();
                    BIUpper = statementVisitor.getBIUpperAfterAnalysis();
                    setIsReturnNull(statementVisitor.getIsReturnNull());
                }
                BI = BIUpper.pollLast();
                BI.setMemory(BIEntry);
            }

            this.BI = BIUpper.pollLast();
            this.BI.setMemory(BI);
            /*ArrayDeque<BlockInformation> dequeBI = getBIfromClass(pathAbs, this.classname, this.rangeStructure, structure);
            BlockInformation BIUpper = dequeBI.pollLast();
            BIUpper.setMemory(BI);
            BI = storeBItoClass(dequeBI, BIUpper);
            ClassInformation CI = DataStore.memoryClass.getData(pathAbs, classname);
            CI.setMemory(BI);
            DataStore.memoryClass.setData(pathAbs, classname, CI);

             */

        }

        @Override
        public void visit(SynchronizedStmt md, Void arg) {
            String structureThis = this.structure + "/Synchronized";
            ArrayDeque<Range> rangeStructure = this.rangeStructure.clone();
            Range range = md.getRange().get();
            rangeStructure.add(range);

            //expressionチェック
            AssignVisitorS conditionVisitor = new AssignVisitorS(classname, pathAbs, methodname, this.range, this.rangeStructure, structureThis, isAccessorCheckG, isAccessorCheckS, BI, BIUpper, paramList, false);
            md.getExpression().accept(conditionVisitor, null);
            BI = conditionVisitor.getBIAfterAnalysis();
            BIUpper = conditionVisitor.getBIUpperAfterAnalysis();

            BlockInformation BI = this.BI.getMemoryData(structureThis, range);
            if(BI == null)BI = new BlockInformation("Synchronized", "Synchronized", false, structureThis, pathDir, range, this.rangeStructure);

            BIUpper.add(this.BI);
            //内部チェック
            InBlockSecondVisitorS bodyVisitor = new InBlockSecondVisitorS(classname, pathAbs, methodname, range, rangeStructure, structureThis, isAccessorCheckG, isAccessorCheckS, BI, BIUpper, paramList);
            md.getBody().accept(bodyVisitor, null);
            BI = bodyVisitor.getBIAfterAnalysis();
            BIUpper = bodyVisitor.getBIUpperAfterAnalysis();
            setIsReturnNull(bodyVisitor.getIsReturnNull());
            this.BI = BIUpper.pollLast();
            this.BI.setMemory(BI);
        }

        @Override
        public void visit(TryStmt md, Void arg){
            String structure = this.structure + "/Try";
            Range range = md.getTryBlock().getRange().get();
            ArrayDeque<Range> rangeStructure = this.rangeStructure.clone();
            rangeStructure.add(range);

            BlockInformation BI = this.BI.getMemoryData(structure, range);//new BlockInformation("Try", "Try", structure, pathDir, range, this.rangeStructure);
            BIUpper.add(this.BI);

            InBlockSecondVisitorS tryBlockVisitor = new InBlockSecondVisitorS(classname, pathAbs, methodname, range, rangeStructure, structure, isAccessorCheckG, isAccessorCheckS, BI, BIUpper, paramList);
            md.getTryBlock().accept(tryBlockVisitor, null);
            BI = tryBlockVisitor.getBIAfterAnalysis();
            BIUpper = tryBlockVisitor.getBIUpperAfterAnalysis();
            setIsReturnNull(tryBlockVisitor.getIsReturnNull());
            this.BI = BIUpper.pollLast();
            this.BI.setMemory(BI);

            for (CatchClause catchClause : md.getCatchClauses()) {
                structure = this.structure + "/Catch";
                range = catchClause.getRange().get();
                rangeStructure = this.rangeStructure.clone();
                rangeStructure.add(range);

                BI = this.BI.getMemoryData(structure, range);//new BlockInformation("Catch", "Catch", structure, pathDir, range, this.rangeStructure);
                BIUpper.add(this.BI);

                InBlockSecondVisitorS catchBlockVisitor = new InBlockSecondVisitorS(classname, pathAbs, methodname, range, rangeStructure, structure, isAccessorCheckG, isAccessorCheckS, BI, BIUpper, paramList);
                catchClause.accept(catchBlockVisitor, null);
                BI = catchBlockVisitor.getBIAfterAnalysis();
                BIUpper = catchBlockVisitor.getBIUpperAfterAnalysis();
                setIsReturnNull(catchBlockVisitor.getIsReturnNull());
                this.BI = BIUpper.pollLast();
                this.BI.setMemory(BI);
            }

            if (md.getFinallyBlock().isPresent()) {
                structure = this.structure + "/Finally";
                range = md.getFinallyBlock().get().getRange().get();
                rangeStructure = this.rangeStructure.clone();
                rangeStructure.add(range);

                BI = this.BI.getMemoryData(structure, range);//new BlockInformation("Finally", "Finally", structure, pathDir, range, this.rangeStructure);
                BIUpper.add(this.BI);

                InBlockSecondVisitorS finallyBlockVisitor = new InBlockSecondVisitorS(classname, pathAbs, methodname, range, rangeStructure, structure, isAccessorCheckG, isAccessorCheckS, BI, BIUpper, paramList);
                md.getFinallyBlock().get().accept(finallyBlockVisitor, null);
                BI = finallyBlockVisitor.getBIAfterAnalysis();
                BIUpper = finallyBlockVisitor.getBIUpperAfterAnalysis();
                setIsReturnNull(finallyBlockVisitor.getIsReturnNull());
                this.BI = BIUpper.pollLast();
                this.BI.setMemory(BI);
            }
        }

        @Override
        public void visit(UnaryExpr md, Void arg){
            AssignVisitorS visitor = new AssignVisitorS(classname, pathAbs, methodname, range, rangeStructure, structure, isAccessorCheckG, isAccessorCheckS, BI, BIUpper, paramList, false);
            md.accept(visitor, null);
            BI = visitor.getBIAfterAnalysis();
            BIUpper = visitor.getBIUpperAfterAnalysis();
            setIsReturnNull(visitor.getIsReturnNull());
        }

        @Override
        public void visit(VariableDeclarator md, Void arg){
            String fieldname = md.getNameAsString();
            Type type = md.getType();
            Type valueType = null;
            Range range = md.getRange().get();
            int dim = 0;
            boolean assign = false;
            boolean nullable = true;
            boolean initializable = true;
            String formerRW = "";

            if(md.getInitializer().isEmpty()) {
                assign = true;
                initializable = false;
                formerRW = "R";
            } else {
                formerRW = "W";
                AssignVisitorS targetVisitor = new AssignVisitorS(classname, pathAbs, fieldname, range, rangeStructure, structure + "/" + fieldname, BI, BIUpper, false);
                md.getInitializer().get().accept(targetVisitor, null);
                valueType = targetVisitor.getTypeDef();
                nullable = targetVisitor.getIsReturnNull();
                BI = targetVisitor.getBIAfterAnalysis();
                BIUpper = targetVisitor.getBIUpperAfterAnalysis();
            }

            if(type.isArrayType()){
                /*int dimNum = type.length();
                type = type.replace("[]", "");
                dimNum = dimNum - type.length();

                 */
                dim = type.asArrayType().getArrayLevel();
            }

            FieldInformation FI = BI.getMemoryF().get(fieldname);
            if(FI == null) {
                FI = new FieldInformation(fieldname, structure + "/" + fieldname, pathDir, md.getType(), dim, assign,
                        nullable, initializable, valueType, formerRW, range, this.rangeStructure);
            }
            if(valueType != null){
                FI.isConvertValueType = typeChangeCheck(type, valueType);
                if(FI.isConvertValueType) {
                    if(type.isClassOrInterfaceType() && valueType.isClassOrInterfaceType()){
                        type.replace(type.asClassOrInterfaceType().getName(), valueType.asClassOrInterfaceType().getName());
                        FI.valueType = type;
                    }
                }
            }

            BI.setMemoryF(FI);

        }

        @Override
        public void visit(WhileStmt md, Void arg){
            //whileのconditionにあたる部分の読み込み変数のチェックとブロック文内部の確認
            String structure = this.structure + "/While";
            Range range = md.getRange().get();
            ArrayDeque<Range> rangeStructure = this.rangeStructure.clone();
            rangeStructure.add(range);

            //conditionチェック
            AssignVisitorS conditionVisitor = new AssignVisitorS(classname, pathAbs, methodname, this.range, this.rangeStructure, structure, isAccessorCheckG, isAccessorCheckS, BI, BIUpper, paramList, false);
            md.getCondition().accept(conditionVisitor, null);
            BI = conditionVisitor.getBIAfterAnalysis();
            BIUpper = conditionVisitor.getBIUpperAfterAnalysis();

            BlockInformation BI = this.BI.getMemoryData(structure, range);//new BlockInformation("While", "While", false, structure, pathDir, range, this.rangeStructure);

            BIUpper.add(this.BI);

            //内部チェック
            InBlockSecondVisitorS bodyVisitor = new InBlockSecondVisitorS(classname, pathAbs,  methodname, range, rangeStructure, structure, isAccessorCheckG, isAccessorCheckS, BI, BIUpper, paramList);
            md.getBody().accept(bodyVisitor, null);
            BI = bodyVisitor.getBIAfterAnalysis();
            BIUpper = bodyVisitor.getBIUpperAfterAnalysis();
            setIsReturnNull(bodyVisitor.getIsReturnNull());

            this.BI = BIUpper.pollLast();
            this.BI.setMemory(BI);
            /*ArrayDeque<BlockInformation> dequeBI = getBIfromClass(pathAbs, this.classname, this.rangeStructure, structure);
            BlockInformation BIUpper = dequeBI.pollLast();
            BIUpper.setMemory(BI);
            BI = storeBItoClass(dequeBI, BIUpper);
            ClassInformation CI = DataStore.memoryClass.getData(pathAbs, classname);
            CI.setMemory(BI);
            DataStore.memoryClass.setData(pathAbs, classname, CI);

             */

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
        
        public void setIsReturnNull(boolean flag){
            this.isReturnNull = this.isReturnNull || flag;
        }

        public Type getRetType(){
            return this.retType;
        }

        public BlockInformation getBIAfterAnalysis(){
            return this.BI;
        }

        public ArrayDeque<BlockInformation> getBIUpperAfterAnalysis(){
            return this.BIUpper;
        }

    }

    public static class AssignVisitorS extends VoidVisitorAdapter<Void> {
        String classname = "";
        String pathAbs = "";
        String contentsname = "";
        Range range = null;
        String structure = "";
        ArrayDeque<Range> rangeStructure = new ArrayDeque<>();
        private String fieldname = "";
        private boolean isAccessorCheckG = false;
        private boolean isAccessorCheckS = false;
        private boolean isFixableG = false;
        private boolean isFixableS = false;
        ArrayList<String> paramList = new ArrayList<>();
        private boolean isWriteCheck = false;
        private String nullCheckValue = "";
        private boolean isReturnNull = false;
        private boolean isAnonymousAnalysis = false;
        private boolean isLambdaAnalysis = false;
        private BlockInformation BI = null;
        private ArrayDeque<BlockInformation> BIUpper = new ArrayDeque<>();
        private Type typeDef = null;

        public AssignVisitorS
                (String classname, String pathAbs, String contentsname, Range range,
                 ArrayDeque<Range> rangeStructure, String structure, boolean isAccessorCheckG,
                 boolean isAccessorCheckS, BlockInformation BI, ArrayDeque<BlockInformation> dequeBI,
                 ArrayList<String> paramList, boolean flag, String value) {
            this.classname = classname;
            this.pathAbs = pathAbs;
            this.contentsname = contentsname;
            this.range = range;
            this.rangeStructure = rangeStructure;
            this.structure = structure;
            this.isAccessorCheckG = isAccessorCheckG;
            this.isAccessorCheckS = isAccessorCheckS;
            this.BI = BI;
            this.BIUpper = dequeBI.clone(); 
            this.paramList = paramList;
            this.isWriteCheck = flag;
            this.nullCheckValue = value;
        }

        public AssignVisitorS
                (String classname, String pathAbs, String contentsname, Range range,
                 ArrayDeque<Range> rangeStructure, String structure,
                 boolean isAccessorCheckG, boolean isAccessorCheckS, BlockInformation BI,
                 ArrayDeque<BlockInformation> dequeBI, ArrayList<String> paramList, boolean flag) {
            this(classname, pathAbs, contentsname, range, rangeStructure, structure, isAccessorCheckG, isAccessorCheckS, BI, dequeBI, paramList, flag, "");
        }

        public AssignVisitorS
                (String classname, String pathAbs, String contentsname, Range range,
                 ArrayDeque<Range> rangeStructure, String structure, BlockInformation BI,
                 ArrayDeque<BlockInformation> dequeBI, boolean flag) {
            this(classname, pathAbs, contentsname, range, rangeStructure, structure, false, false, BI, dequeBI, null, flag, "");
        }

        @Override
        public void visit(ArrayCreationExpr md, Void arg){
            this.isReturnNull = true;
        }

        @Override
        public void visit(ArrayInitializerExpr md, Void arg){
            //要素ありの配列初期化の際の、要素の解析
            if(md.getValues().size() != 0){
                for(Expression expression:md.getValues()){
                    AssignVisitorS visitor = new AssignVisitorS(classname, pathAbs, contentsname, range, rangeStructure, structure, isAccessorCheckG, isAccessorCheckS, BI, BIUpper, paramList, false);
                    expression.accept(visitor, null);
                    BI = visitor.getBIAfterAnalysis();
                    BIUpper = visitor.getBIUpperAfterAnalysis();
                }
            }
        }

        @Override
        public void visit(AssignExpr md, Void arg){
            //メソッド呼び出しの引数に使われているかどうかを確認
            //ここでは何もせず、visitorに明け渡す
            InBlockSecondVisitorS visitor = new InBlockSecondVisitorS(classname, pathAbs, contentsname, range, rangeStructure, structure, isAccessorCheckG, isAccessorCheckS, BI, BIUpper, paramList);
            md.accept(visitor, null);
            BI = visitor.getBIAfterAnalysis();
            BIUpper = visitor.getBIUpperAfterAnalysis();
        }

        @Override
        public void visit(BinaryExpr md, Void arg){
            //計算式に使われている変数の確認
            //最左分岐なので左側解析
            AssignVisitorS leftVisitor = new AssignVisitorS(classname, pathAbs, contentsname, range, rangeStructure, structure, isAccessorCheckG, isAccessorCheckS, BI, BIUpper, paramList, isWriteCheck, nullCheckValue);
            md.getLeft().accept(leftVisitor, null);
            BI = leftVisitor.getBIAfterAnalysis();
            BIUpper = leftVisitor.getBIUpperAfterAnalysis();

            //右側解析
            AssignVisitorS rightVisitor = new AssignVisitorS(classname, pathAbs, contentsname, range, rangeStructure, structure, isAccessorCheckG, isAccessorCheckS, BI, BIUpper, paramList, isWriteCheck, nullCheckValue);
            md.getRight().accept(rightVisitor, null);
            BI = rightVisitor.getBIAfterAnalysis();
            BIUpper = rightVisitor.getBIUpperAfterAnalysis();
        }

        @Override
        public void visit(BlockStmt md, Void arg){
            for(Statement statement:md.getStatements()){
                AssignVisitorS visitor = new AssignVisitorS(classname, pathAbs, contentsname, range, rangeStructure, structure, isAccessorCheckG, isAccessorCheckS, BI, BIUpper, paramList, isWriteCheck, nullCheckValue);
                statement.accept(visitor, null);
            }
        }

        @Override
        public void visit(CastExpr md, Void arg){
            super.visit(md, arg);
        }

        @Override
        public void visit(EnclosedExpr md, Void arg){
            super.visit(md, arg);
        }

        @Override
        public void visit(FieldAccessExpr md, Void arg){
            //変数の直接参照の確認、計算式とかからもこっちに飛んでくる
            boolean flagThis = md.getScope().isThisExpr();
            boolean flagSuper = md.getScope().isSuperExpr();

            String classname = this.classname;
            String valName = md.getNameAsString();
            ClassInformation CI = null;

            FieldInformation FI = null;
            if(flagThis) {
                CI = (ClassInformation) BIUpper.pollFirst();
                if (CI != null) FI = CI.getMemoryF().get(valName);
            } else if(flagSuper){//スーパークラス、インターフェースなど(CIから呼び出せるやつ)
                ClassInformation CITmp = (ClassInformation)BIUpper.peekLast();
                boolean flag = false;
                if(CITmp != null){
                    //Extendの捜索 DataStore.memoryClass.getDataListKey2(CITmp.classEx);
                    if(CITmp.classEx != null)CI = DataStore.memoryClass.getData(pathAbs, CITmp.classEx.toString());
                    if(CI == null && CITmp.classEx != null){
                        if(CITmp.classEx.isClassOrInterfaceType())
                            CI = DataStore.memoryClass.getData(pathAbs, CITmp.classEx.asClassOrInterfaceType().getNameAsString());
                    }
                    if (CI != null) {
                        FI = CI.getMemoryF().get(valName);
                        flag = FI != null;
                    }
                    for(Type type:CITmp.classIm){
                        if(flag) {
                            break;
                        }
                        CI = DataStore.memoryClass.getData(pathAbs, type.toString());
                        if(CI == null){
                            if(type.isClassOrInterfaceType())
                                CI = DataStore.memoryClass.getData(pathAbs, type.asClassOrInterfaceType().getNameAsString());
                        }
                        if (CI != null) {
                            FI = CI.getMemoryF().get(valName);
                            flag = FI != null;
                        }
                    }
                }
            } else {
                //解析
                AssignVisitorS visitor = new AssignVisitorS(classname, pathAbs, contentsname, this.range, rangeStructure, structure, isAccessorCheckG, isAccessorCheckS, BI, BIUpper, paramList, false, nullCheckValue);
                md.getScope().accept(visitor, null);
                Type typeTmp = visitor.getTypeDef();
                if(typeTmp != null){
                    classname = visitor.getTypeDef().toString();
                }

                CI = searchDefinitionField(valName, classname, pathAbs);
                if(CI == null && typeTmp != null){
                    if(typeTmp.isClassOrInterfaceType()){
                        CI = searchDefinitionField(valName, typeTmp.asClassOrInterfaceType().getNameAsString(), pathAbs);
                    }
                }
                if(CI == null){
                    String importStr = DataStore.searchImport(classname, pathAbs);
                    CI = searchDefinitionField(valName, classname, DataStore.memoryImportToFile.get(importStr));
                }

                if(CI != null) {
                    FI = CI.getMemoryF().get(valName);
                    if(FI != null) typeDef = FI.isConvertValueType ? FI.valueType :FI.type;

                    if (CI.isEnum) {
                        ClassOrInterfaceType typeNodeTmp = new ClassOrInterfaceType(null, classname);
                        typeDef = typeNodeTmp;
                    }
                } else {
                    CI = DataStore.memoryClass.getData(pathAbs, classname);

                    if(CI != null){
                        if(CI.isEnum) typeDef = typeTmp;
                    }
                }
            }


            if(FI != null){
                if(FI.isKotlinPrivate){
                    if(!flagThis) {
                        FI.isKotlinPrivate = false;
                    }
                }
                if(isWriteCheck) {
                    CI.setMemoryF(writeInfoUpdate(FI, nullCheckValue));

                    if (isAccessorCheckS) {
                        String fieldname = contentsname.substring(3, 4).toLowerCase() + contentsname.substring(4);
                        if(fieldname.equals(valName)){
                            isFixableS = true;
                            this.fieldname = valName;
                        }
                        /*for (String paramName : paramList) {
                            if (valName.equals(paramName) && nullCheckValue.equals(paramName)) {
                                isFixableS = true;
                                this.fieldname = valName;
                            }
                        }*/
                    }
                } else {
                    CI.setMemoryF(readInfoUpdate(FI));

                    if (isAccessorCheckG) {
                        String fieldname = contentsname.substring(3, 4).toLowerCase() + contentsname.substring(4);
                        if(fieldname.equals(valName)){
                            isFixableG = true;
                            this.fieldname = valName;
                        }
                    }
                }

                if(flagThis){
                    BIUpper.addFirst(CI);
                    typeDef = FI.type;
                }
                else if(flagSuper) {
                    DataStore.memoryClass.setData(CI.pathDir, CI.name, CI);
                    typeDef = FI.type;
                } else {//別クラス
                    DataStore.memoryClass.setData(CI.pathDir, CI.name, CI);
                }
            } else {
                if(flagThis){
                    if(CI != null) BIUpper.addFirst(CI);
                }
            }
        }

        @Override
        public void visit(InstanceOfExpr md, Void arg){
            AssignVisitorS visitor =  new AssignVisitorS(classname, pathAbs, contentsname, this.range, rangeStructure, structure, isAccessorCheckG, isAccessorCheckS, BI, BIUpper, paramList, false);
            md.getExpression().accept(visitor, null);
        }

        @Override
        public void visit(LambdaExpr md, Void arg){
            isLambdaAnalysis = true;
            String structureThis = this.structure + "/" + contentsname;
            ArrayDeque<Range> rangeStructure = this.rangeStructure.clone();
            Range range = md.getRange().get();
            rangeStructure.add(range);
            BlockInformation BI = this.BI.getMemoryData(structureThis, range);
            if(BI == null) BI = new BlockInformation(contentsname, "Lambda", structureThis, pathDir, range, this.rangeStructure);

            BIUpper.add(this.BI);
            for(Parameter param:md.getParameters()){
                //paramList.add(param.getNameAsString());
                InBlockSecondVisitorS visitor = new InBlockSecondVisitorS(classname, pathAbs, contentsname, range, rangeStructure, structureThis, isAccessorCheckG, isAccessorCheckS, BI, BIUpper, paramList);
                param.accept(visitor, null);
                BI = visitor.getBIAfterAnalysis();
                BIUpper = visitor.getBIUpperAfterAnalysis();
            }

            ArrayList<FieldInformation> localList = new ArrayList<>();
            InBlockSecondVisitorS visitor = new InBlockSecondVisitorS(classname, pathAbs, contentsname, range, rangeStructure, structureThis, isAccessorCheckG, isAccessorCheckS, BI, BIUpper, paramList);
            md.getBody().accept(visitor, null);
            BI = visitor.getBIAfterAnalysis();
            BIUpper = visitor.getBIUpperAfterAnalysis();
            /*ArrayDeque<BlockInformation> dequeBI = getBIfromClass(pathAbs, this.classname, this.rangeStructure, structure);
            if(dequeBI.peekLast() != null) {
                BlockInformation BIUpper = dequeBI.pollLast();
                BIUpper.setMemory(BI);
                BI = storeBItoClass(dequeBI, BIUpper);
            }*/
            this.BI = BIUpper.pollLast();
            this.BI.setMemory(BI);

        }

        @Override
        public void visit(MethodCallExpr md, Void arg){
            if(md.getScope().isPresent()){
                AssignVisitorS visitor = new AssignVisitorS(classname, pathAbs, md.getNameAsString(), range, rangeStructure, structure, isAccessorCheckG, isAccessorCheckS, BI, BIUpper, paramList, false);
                md.getScope().get().accept(visitor, null);
                BI = visitor.getBIAfterAnalysis();
                BIUpper = visitor.getBIUpperAfterAnalysis();
            }
            //メソッド呼び出しの引数を確認
            if(md.getArguments().size() != 0){
                for(Expression expression:md.getArguments()){
                    String structureThis = this.structure;
                    if(expression.isObjectCreationExpr()) structureThis = structure + "/" + md.getNameAsString();
                    structureThis = DataStore.structureChangeCheck(expression, this.structure, md.getNameAsString());
                    AssignVisitorS visitor = new AssignVisitorS(classname, pathAbs, md.getNameAsString(), expression.getRange().get(), rangeStructure, structureThis, isAccessorCheckG, isAccessorCheckS, BI, BIUpper, paramList, false);
                    expression.accept(visitor, null);
                    BI = visitor.getBIAfterAnalysis();
                    BIUpper = visitor.getBIUpperAfterAnalysis();
                }
            }
        }

        @Override
        public void visit(NameExpr md, Void arg){
            //変数(スコープなし)の確認
            String valName = md.getNameAsString();
            BlockInformation BI = this.BI;
            FieldInformation FI = BI.getMemoryF().get(valName);
            if(FI != null){
                if(BI instanceof ClassInformation){

                }else {
                    if (DataStore.rangeDefinitionCheck(FI.range, md.getRange().get())) ;
                    else FI = null;
                }
            }
            boolean isUpperCheck = false;
            ArrayDeque<BlockInformation> dequeBI = new ArrayDeque<>();
            while (FI == null){
                if(BIUpper.peekLast() == null) break;
                isUpperCheck = true;
                if(BIUpper.peekLast().getMemoryF() != null){
                    BI = BIUpper.pollLast();
                    FI = BI.getMemoryF().get(valName);
                    dequeBI.add(BI);
                    if(FI != null){
                        if(BI instanceof ClassInformation){

                        }else {
                            if (DataStore.rangeDefinitionCheck(FI.range, md.getRange().get())) ;
                            else FI = null;
                        }
                    }
                }
            }
            if(FI != null){
                typeDef = FI.isConvertValueType ? FI.valueType :FI.type;
                if(isWriteCheck) {
                    BI.setMemoryF(writeInfoUpdate(FI, nullCheckValue));
                    if (isAccessorCheckS) {
                        String fieldname = contentsname.substring(3, 4).toLowerCase() + contentsname.substring(4);
                        if(fieldname.equals(valName)){
                            isFixableS = true;
                            this.fieldname = valName;
                        }
                        /*for (String paramName : paramList) {
                            if (valName.equals(paramName) && nullCheckValue.equals(paramName)) {
                                isFixableS = true;
                                this.fieldname = valName;
                            }
                        }*/
                    }
                }
                else {
                    BI.setMemoryF(readInfoUpdate(FI));
                    if (isAccessorCheckG) {
                        if(contentsname.length() > 3) {
                            String fieldname = contentsname.substring(3, 4).toLowerCase() + contentsname.substring(4);
                            if (fieldname.equals(valName)) {
                                isFixableG = true;
                                this.fieldname = valName;
                            }
                        }
                    }
                }
            } else {
                typeDef = new ClassOrInterfaceType(null, valName);
                if(DataStore.memoryClass.getDataListKey2(valName) != null){
                    for(Triplets<String, String, ClassInformation> triplets:DataStore.memoryClass.getDataListKey2(valName)){
                        ClassInformation CI = triplets.getRightValue();
                        if(CI.isKotlinPrivate) {
                            CI.isKotlinPrivate = false;
                            DataStore.memoryClass.setData(triplets.getLeftValue(), triplets.getCenterValue(), CI);
                        }
                    }
                }
            }
            if(isUpperCheck){
                if(FI != null){
                    BIUpper.addLast(BI);
                    dequeBI.pollLast();
                }
                while (dequeBI.peekLast() != null){
                    BlockInformation BItmp = dequeBI.pollLast();
                    BIUpper.addLast(BItmp);
                }
            } else {
                this.BI = BI;
            }
        }

        @Override
        public void visit(NullLiteralExpr md, Void arg){
            this.isReturnNull = true;
        }

        @Override
        public void visit(ObjectCreationExpr md, Void arg){
            typeDef = md.getType();

            //インスタンス生成に必要な引数の部分の解析
            if(md.getArguments().size() != 0){
                for(Expression expression:md.getArguments()){
                    String structureThis = structure;
                    if(expression.isObjectCreationExpr()) structureThis = structure + "/" + md.getType();
                    structureThis = DataStore.structureChangeCheck(expression, this.structure, md.getType().toString());
                    AssignVisitorS visitor = new AssignVisitorS(classname, pathAbs, md.getType().toString(), range, rangeStructure, structureThis, isAccessorCheckG, isAccessorCheckS, BI, BIUpper, paramList, false);
                    expression.accept(visitor, null);
                    BI = visitor.getBIAfterAnalysis();
                    BIUpper = visitor.getBIUpperAfterAnalysis();
                }
            }

            //匿名クラスの解析
            if (md.getAnonymousClassBody().isPresent()) {
                isAnonymousAnalysis = true;
                String structureThis = this.structure + "/" + md.getType();
                ArrayDeque<Range> rangeStructure = this.rangeStructure.clone();
                Range range = md.getRange().get();
                rangeStructure.add(range);
                BlockInformation BI = this.BI.getMemoryData(structureThis, range);//new BlockInformation(contentsname, "AnonymousClass", false, structure, pathDir, range, this.rangeStructure);

                BIUpper.add(this.BI);
                for (BodyDeclaration bd : md.getAnonymousClassBody().get()) {
                    if(bd.isFieldDeclaration()){
                        FieldVisitor visitor = new FieldVisitor(contentsname, pathAbs, structureThis, rangeStructure, BI, BIUpper, true);
                        bd.accept(visitor, null);
                        BI = visitor.getBIAfterAnalysis();
                        BIUpper = visitor.getBIUpperAfterAnalysis();
                    } else if(bd.isMethodDeclaration()){
                        MethodSecondVisitor visitor = new MethodSecondVisitor(contentsname, pathAbs, structureThis, rangeStructure, BI, BIUpper, true);
                        bd.accept(visitor, null);
                        BI = visitor.getBIAfterAnalysis();
                        BIUpper = visitor.getBIUpperAfterAnalysis();
                    } else if(bd.isConstructorDeclaration()){
                        ConstructorSecondVisitor visitor = new ConstructorSecondVisitor(contentsname, pathAbs, structureThis, rangeStructure, BI, BIUpper, true);
                        bd.accept(visitor, null);
                        BI = visitor.getBIAfterAnalysis();
                        BIUpper = visitor.getBIUpperAfterAnalysis();
                    } else {//initializer
                        InitializerSecondVisitor visitor = new InitializerSecondVisitor(contentsname, pathAbs, structureThis, rangeStructure, BI, BIUpper, true);
                        bd.accept(visitor, null);
                        BI = visitor.getBIAfterAnalysis();
                        BIUpper = visitor.getBIUpperAfterAnalysis();
                    }
                }
                /*ArrayDeque<BlockInformation> dequeBI = getBIfromClass(pathAbs, this.classname, this.rangeStructure, structure);
                if(dequeBI.peekLast() != null) {
                    BlockInformation BIUpper = dequeBI.pollLast();
                    BIUpper.setMemory(BI);
                    BI = storeBItoClass(dequeBI, BIUpper);
                }*/
                this.BI = BIUpper.pollLast();
                this.BI.setMemory(BI);
                /*ClassInformation CI = DataStore.memoryClass.getData(pathAbs, classname);
                CI.setMemory(BI);
                DataStore.memoryClass.setData(pathAbs, classname, CI);*/
            }
        }

        @Override
        public void visit(SwitchExpr md, Void arg){
            //switchのselectorにあたる部分の読み込み変数のチェックとブロック文内部の確認
            String structure = this.structure + "/Switch";
            Range range = md.getRange().get();
            ArrayDeque<Range> rangeStructure = this.rangeStructure.clone();
            rangeStructure.add(range);

            AssignVisitorS selectorVisitor = new AssignVisitorS(classname, pathAbs, contentsname, this.range, this.rangeStructure, this.structure, isAccessorCheckG, isAccessorCheckS, BI, BIUpper, paramList, false);
            md.getSelector().accept(selectorVisitor, null);
            BI = selectorVisitor.getBIAfterAnalysis();
            BIUpper = selectorVisitor.getBIUpperAfterAnalysis();

            BlockInformation BI = this.BI.getMemoryData(structure, range);
            //new BlockInformation("Switch", "Switch", structure, pathDir, range, this.rangeStructure);

            BIUpper.add(this.BI);

            for(SwitchEntry entry:md.getEntries()){
                String structureLabel = structure + "/";
                StringBuilder label = new StringBuilder();
                for(Expression expression:entry.getLabels()) label.append(expression);
                if(entry.getLabels().size() == 0) label.append("default");
                structureLabel = structureLabel + label;
                Range rangeEntry = entry.getRange().get();
                ArrayDeque<Range> rangeStructureEntry = rangeStructure.clone();
                rangeStructureEntry.add(rangeEntry);
                BlockInformation BIEntry = BI.getMemoryData(structureLabel, rangeEntry);//new BlockInformation("SwitchEntry", "SwitchEntry", structureLabel, pathDir, rangeEntry, rangeStructure);
                BIUpper.add(BI);
                for(Statement statement:entry.getStatements()){
                    InBlockSecondVisitorS statementVisitor = new InBlockSecondVisitorS(classname, pathAbs, contentsname, range, rangeStructureEntry, structureLabel, isAccessorCheckG, isAccessorCheckS, BIEntry, BIUpper, paramList);
                    statement.accept(statementVisitor, null);
                    BIEntry = statementVisitor.getBIAfterAnalysis();
                    BIUpper = statementVisitor.getBIUpperAfterAnalysis();
                }
                BI = BIUpper.pollLast();
                BI.setMemory(BIEntry);
            }
            this.BI = BIUpper.pollLast();
            this.BI.setMemory(BI);
            /*ArrayDeque<BlockInformation> dequeBI = getBIfromClass(pathAbs, this.classname, this.rangeStructure, structure);
            BlockInformation BIUpper = dequeBI.pollLast();
            BIUpper.setMemory(BI);
            BI = storeBItoClass(dequeBI, BIUpper);
            ClassInformation CI = DataStore.memoryClass.getData(pathAbs, classname);
            CI.setMemory(BI);
            DataStore.memoryClass.setData(pathAbs, classname, CI);

             */
        }


        @Override
        public void visit(UnaryExpr md, Void arg){
            //read check
            AssignVisitorS visitor = new AssignVisitorS(classname, pathAbs, contentsname, range, rangeStructure, structure, isAccessorCheckG, isAccessorCheckS, BI, BIUpper, paramList, false);
            md.getExpression().accept(visitor, null);
            BI = visitor.getBIAfterAnalysis();
            BIUpper = visitor.getBIUpperAfterAnalysis();

            //write check
            visitor = new AssignVisitorS(classname, pathAbs, contentsname, range, rangeStructure, structure, isAccessorCheckG, isAccessorCheckS, BI, BIUpper, paramList, true);
            md.getExpression().accept(visitor, null);
            BI = visitor.getBIAfterAnalysis();
            BIUpper = visitor.getBIUpperAfterAnalysis();
        }

        private FieldInformation readInfoUpdate(FieldInformation FI){
            FieldInformation FITmp = FI;
            if (FI.formerRW.equals("R") && !FI.initializable)
                FITmp.nullable = true;

            if (FI.formerRW.equals("") && !FI.initializable && !FI.assign) {
                FITmp.formerRW = "R";
                FITmp.nullable = true;
            }

            if(FI.nullable) isReturnNull = true;

            return FITmp;
        }

        private FieldInformation writeInfoUpdate(FieldInformation FI, String value){
            FieldInformation FITmp = FI;

            FITmp.assign = true;

            if (FI.formerRW.equals("") && !FI.initializable) {
                if(isOrderCheck)FITmp.formerRW = "W";
            }

            if (value.equals("null")) FITmp.nullable = true;

            return FITmp;
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

        public boolean isAnonymousAnalysis(){
            return this.isAnonymousAnalysis;
        }

        public boolean isLambdaAnalysis(){
            return this.isLambdaAnalysis;
        }


        public BlockInformation getBIAfterAnalysis(){
            return this.BI;
        }

        public ArrayDeque<BlockInformation> getBIUpperAfterAnalysis(){
            return this.BIUpper;
        }

        public Type getTypeDef(){
            return this.typeDef;
        }

    }

    public static ClassInformation searchDefinitionField(String target, String defClass, String pathAbs){

        //同一ファイル最上位、フィールド定義
        String pathDirThis = pathAbs;
        if(DataStore.memoryPathAbs.get(pathDirThis) != null) {
            for (String path : DataStore.memoryPathAbs.get(pathDirThis)) {
                for (Triplets<String, String, ClassInformation> tripletsCI : DataStore.memoryClass.getDataListKey1(path)) {
                    if (tripletsCI.getCenterValue().equals(defClass)) {
                        ClassInformation CITmp = tripletsCI.getRightValue();
                        for (String name : CITmp.getMemoryF().keySet()) {
                            if (name.equals(target)) {
                                return CITmp;
                            }
                        }
                    }
                }
            }
        }
        //全範囲から検索(同一ディレクトリを除く)
        //hashmap.keysetでできるかも
        for(String pathDir:DataStore.memoryPathAbs.keySet()){
            if(pathDir.equals(pathDirThis)) continue;
            for(String path:DataStore.memoryPathAbs.get(pathDir)){
                for(Triplets<String, String, ClassInformation> tripletsCI:DataStore.memoryClass.getDataListKey1(path)){
                    //自クラス、extends, implementは飛ばす

                    if (tripletsCI.getCenterValue().equals(defClass)) {
                        ClassInformation CITmp = tripletsCI.getRightValue();
                        for (String name : CITmp.getMemoryF().keySet()) {
                            if (name.equals(target)) {
                                return CITmp;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }


    //今探している範囲を含むスコープであるかどうかを確認する
    //range1:今見ている情報群のスコープ、range2:探している範囲
    public static boolean rangeScopeCheck(Range range1, Range range2){
        if((range1.begin.line <= range2.begin.line) && (range1.end.line >= range2.end.line))
            return true;
        else return false;
    }

    //クラスから現在の場所に至るまでのBlockInfomationのdequeを作り出すクラス
    private static ArrayDeque<BlockInformation> getBIfromClass(String pathAbs, String classname, ArrayDeque<Range> rangeStructure, String blockStructure){
        ArrayDeque<BlockInformation> dequeBI = new ArrayDeque<>();
        ArrayDeque<Range> ranges = rangeStructure.clone();
        String[] structureSplit = blockStructure.split("/");

        //位置合わせの場所
        StringBuilder sb = new StringBuilder("/" + structureSplit[1]);
        int numStructureLayer = 2;

        ClassInformation CI = DataStore.memoryClass.getData(pathAbs, classname);
        if(CI == null) return null;
        ArrayDeque<Range> rangesCI = CI.rangeStructure.clone();

        //インナークラス用のrange,structure合わせ
        //クラスのrange位置あわせ
        boolean flag = false;
        while(rangesCI.peek() != null){
            flag = true;
            ranges.poll();
            rangesCI.poll();
        }

        //クラスのstructure位置あわせ
        while(!CI.blockStructure.equals(sb.toString()) && flag) {
            sb.append("/").append(structureSplit[numStructureLayer]);
            numStructureLayer++;
        }

        //クラス直下ではない時
        if(structureSplit.length > 2) {
            ranges.poll();//クラスの分rangeデータを破棄
            sb.append("/").append(structureSplit[numStructureLayer]);//クラスの下部にアクセス
            numStructureLayer++;
            BlockInformation BI = CI.getMemoryData(sb.toString(), ranges.poll());
            dequeBI.add(BI);

            while (ranges.peek() != null) {
                sb.append("/").append(structureSplit[numStructureLayer]);
                numStructureLayer++;
                Range range = ranges.poll();
                BI = BI.getMemoryData(sb.toString(), range);
                dequeBI.add(BI);
            }
        }


        return dequeBI;
    }

    private static BlockInformation storeBItoClass(ArrayDeque<BlockInformation> deque, BlockInformation BI){
        ArrayDeque<BlockInformation> dequeBI = deque;
        BlockInformation BInow = BI;

        while(dequeBI.peek() != null){
            BlockInformation BIOuterScope = dequeBI.pollLast();
            BIOuterScope.setMemory(BInow);
            BInow = BIOuterScope;
        }

        return BInow;
    }

    public static boolean typeChangeCheck(Type type, Type valueType){
        if(type == null) return false;
        if(valueType == null) return false;
        if(type.isClassOrInterfaceType()){
            switch (type.asClassOrInterfaceType().getName().toString()){
                case "List":
                    if(valueType.isClassOrInterfaceType()){
                        if(valueType.asClassOrInterfaceType().getName().toString().endsWith("List"))
                            return true;
                    }
                    return false;
                case "Map":
                    if(valueType.isClassOrInterfaceType()){
                        if(valueType.asClassOrInterfaceType().getName().toString().endsWith("Map"))
                            return true;
                    }
                    return false;
                case "Set":
                    if(valueType.isClassOrInterfaceType()){
                        if(valueType.asClassOrInterfaceType().getName().toString().endsWith("Set"))
                            return true;
                    }
                    return false;
                default:
                    return false;
            }
        } else return false;
    }

    private static void extendCheck(String pathAbs, ClassInformation CI){
        String pathSuper = pathDir + "/" + CI.classEx + ".java";
        ClassInformation CISuper = null;
        boolean flag = CI.classEx != null ? CI.classEx.isClassOrInterfaceType() : false;
        if(flag) {
            flag = CI.classEx.asClassOrInterfaceType().getScope().isPresent();
        }
        if(flag){
            ClassOrInterfaceType type = CI.classEx.asClassOrInterfaceType();
            CISuper = DataStore.memoryClass.getData(pathAbs, type.getNameAsString());
            if (CISuper == null) {
                if (DataStore.memoryClass.getDataListKey2(type.getNameAsString()) != null) {
                    for (Triplets<String, String, ClassInformation> triplets : DataStore.memoryClass.getDataListKey2(type.getNameAsString())) {
                        CISuper = triplets.getRightValue();
                    }
                }
            }
        } else {
            if (CI.classEx != null) CISuper = DataStore.memoryClass.getData(pathSuper, CI.classEx.toString());
            if (CISuper == null && CI.classEx != null) {
                if (DataStore.memoryClass.getDataListKey2(CI.classEx.toString()) != null) {
                    for (Triplets<String, String, ClassInformation> triplets : DataStore.memoryClass.getDataListKey2(CI.classEx.toString())) {
                        CISuper = triplets.getRightValue();
                    }
                }
            }
        }

        if(CISuper != null) {
            CISuper.isOpen = true;
            DataStore.memoryClass.setData(CISuper.pathDir, CISuper.name, CISuper);
        }
    }

    private static boolean extendCheckField(String fieldName, String pathAbs, ClassInformation CI, boolean isFinal){
        if(isFinal) return false;
        String pathSuper = pathDir + "/" + CI.classEx + ".java";
        ClassInformation CISuper = null;
        boolean flag = CI.classEx != null ? CI.classEx.isClassOrInterfaceType() : false;
        if(flag) {
            flag = CI.classEx.asClassOrInterfaceType().getScope().isPresent();
        }
        if(flag){
            ClassOrInterfaceType type = CI.classEx.asClassOrInterfaceType();
            CISuper = DataStore.memoryClass.getData(pathAbs, type.getNameAsString());
            if (CISuper == null) {
                if (DataStore.memoryClass.getDataListKey2(type.getNameAsString()) != null) {
                    for (Triplets<String, String, ClassInformation> triplets : DataStore.memoryClass.getDataListKey2(type.getNameAsString())) {
                        CISuper = triplets.getRightValue();
                    }
                }
            }
        } else {
            if (CI.classEx != null) CISuper = DataStore.memoryClass.getData(pathSuper, CI.classEx.toString());
            if (CISuper == null && CI.classEx != null) {
                if (DataStore.memoryClass.getDataListKey2(CI.classEx.toString()) != null) {
                    for (Triplets<String, String, ClassInformation> triplets : DataStore.memoryClass.getDataListKey2(CI.classEx.toString())) {
                        CISuper = triplets.getRightValue();
                    }
                }
            }
        }

        if(CISuper != null) {
            FieldInformation fi = CISuper.getMemoryF().get(fieldName);
            if(fi != null) {
                fi.isOpen = true;
                CISuper.setMemoryF(fi);
                DataStore.memoryClass.setData(CISuper.pathDir, CISuper.name, CISuper);
                return fi.isOpen;
            }

        }
        return false;
    }

    private static boolean extendCheckMethod(String methodName, Type[] types, String pathAbs, ClassInformation CI){
        String pathSuper = pathDir + "/" + CI.classEx + ".java";
        ClassInformation CISuper = null;
        boolean flag = CI.classEx != null ? CI.classEx.isClassOrInterfaceType() : false;
        if(flag) {
            flag = CI.classEx.asClassOrInterfaceType().getScope().isPresent();
        }
        if(flag){
            ClassOrInterfaceType type = CI.classEx.asClassOrInterfaceType();
            CISuper = DataStore.memoryClass.getData(pathAbs, type.getNameAsString());
            if (CISuper == null) {
                if (DataStore.memoryClass.getDataListKey2(type.getNameAsString()) != null) {
                    for (Triplets<String, String, ClassInformation> triplets : DataStore.memoryClass.getDataListKey2(type.getNameAsString())) {
                        CISuper = triplets.getRightValue();
                    }
                }
            }
        } else {
            if (CI.classEx != null) CISuper = DataStore.memoryClass.getData(pathSuper, CI.classEx.toString());
            if (CISuper == null && CI.classEx != null) {
                if (DataStore.memoryClass.getDataListKey2(CI.classEx.toString()) != null) {
                    for (Triplets<String, String, ClassInformation> triplets : DataStore.memoryClass.getDataListKey2(CI.classEx.toString())) {
                        CISuper = triplets.getRightValue();
                    }
                }
            }
        }

        if(CISuper != null) {
            String structure = CISuper.blockStructure + "/" + methodName;
            if(CISuper.getMemoryKind("Method") != null){
                ArrayList<Triplets<String, Range, BlockInformation>> arrayList = CISuper.getMemoryKind("Method");
                for(Triplets<String, Range, BlockInformation> triplets:arrayList){
                    if(triplets.getLeftValue().equals(structure)){
                        MethodInformation MI = (MethodInformation) triplets.getRightValue();
                        if(DataStore.CheckMethod(types, MI)) {
                            MI.isOpen = true;
                            CISuper.setMemory(MI);
                            DataStore.memoryClass.setData(CISuper.pathDir, CISuper.name, CISuper);
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private static void pathLink(String pathAbs, String pathDir){
        if(DataStore.memoryPathAbs.get(pathDir) == null) DataStore.memoryPathAbs.put(pathDir, new ArrayList<>());
        ArrayList<String> arrayList = DataStore.memoryPathAbs.get(pathDir);
        arrayList.add(pathAbs);
        DataStore.memoryPathAbs.put(pathDir, arrayList);
    }

    private static void pathLinkLib(String pathAbs, String pathDir){
        if(DataStore.memoryPathAbsLibrary.get(pathDir) == null) DataStore.memoryPathAbsLibrary.put(pathDir, new ArrayList<>());
        ArrayList<String> arrayList = DataStore.memoryPathAbsLibrary.get(pathDir);
        arrayList.add(pathAbs);
        DataStore.memoryPathAbsLibrary.put(pathDir, arrayList);
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