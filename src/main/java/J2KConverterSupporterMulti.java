import com.github.javaparser.ParseResult;
import com.github.javaparser.Range;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.utils.Pair;
import com.github.javaparser.utils.SourceRoot;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

public class J2KConverterSupporterMulti {

    public static HashMap<String, HashMap<String, String>> memory_field_Tmp = new HashMap<>();
    public static HashMap<String, HashMap<String, Object>> memory_method_Tmp = new HashMap<>();
    public static String pathAbs = "";
    public static String pathDir = "";

    public static void main(String[] args) throws IOException {
        DataStore.init();

        String path_root = "/Users/kzm0308/IdeaProjects/J2KConverter/src/main/java/pac1";
        if(args.length == 0)
            path_root = DataStore.pathName;
        else path_root = args[0];
        SourceRoot root = new SourceRoot(Paths.get(path_root));
        List<ParseResult<CompilationUnit>> results = root.tryToParse("");

        //情報収集フェーズ(構文解析して変数の型やメソッドの返り値の型などの情報をストックする)
        for(ParseResult<CompilationUnit> result:results){
            CompilationUnit cu = result.getResult().get();
            CompilationUnit.Storage cus = cu.getStorage().get();
            pathAbs = cus.getPath().toString();
            pathDir = cus.getDirectory().toString();
            pathLink(pathAbs, pathDir);
            DataStore.memoryStatic.putIfAbsent(pathAbs, false);
            VoidVisitor<?> visitor = new FirstVisitor("", "", new ArrayDeque<>());
            cu.accept(visitor, null);
            DataStore.memory_classname.add(pathAbs);
        }

        //OutputCheck(path_root);

        //判断フェーズ(変数のvar/val、get/set判断、変数名/メソッド名の被り)
        Analysis();

        OutputCheck(path_root);

        for(String name:DataStore.memory_classlibrary){
            System.out.println(name);
        }

    }

    private static void Analysis(){
        for(String pathAbs:DataStore.memoryPathAbs.get(pathDir)){
            memory_field_Tmp = new HashMap<>();
            memory_method_Tmp = new HashMap<>();
            //System.out.println(pathAbs);
            for(Triplets<String, String, ClassTemporaryStore> triplets:DataStore.memoryBeforeCheck.getDataListKey1(pathAbs)){
                ClassTemporaryStore CTS = triplets.getRightValue();
                ArrayDeque<Range> rangeStructure = CTS.rangeStructure.clone();
                rangeStructure.add(CTS.range);
                System.out.println("analysis class:" + CTS.name);
                System.out.println("structure:" + CTS.blockStructure);
                System.out.println("range:" + CTS.rangeStructure.clone());

                for(FieldDeclaration fd:CTS.listFD){
                    FieldVisitor fieldVisitor = new FieldVisitor(CTS.name, pathAbs, CTS.blockStructure, rangeStructure);
                    fd.accept(fieldVisitor, null);
                }
                //static初期化子チェック
                for (InitializerDeclaration id : CTS.listID) {
                    InitializerSecondVisitor initializerVisitor = new InitializerSecondVisitor(CTS.name, pathAbs, CTS.blockStructure, rangeStructure);
                    for (AnnotationExpr annotationExpr : id.getAnnotations()) {
                        if (annotationExpr.getNameAsString().equals("static"))
                            id.accept(initializerVisitor, null);
                    }
                }
                //通常の初期化子チェック
                NonStatic:
                for (InitializerDeclaration id : CTS.listID) {
                    InitializerSecondVisitor initializerVisitor = new InitializerSecondVisitor(CTS.name, pathAbs, CTS.blockStructure, rangeStructure);
                    for (AnnotationExpr annotationExpr : id.getAnnotations()) {
                        if (annotationExpr.getNameAsString().equals("static"))
                            continue NonStatic;
                    }
                    id.accept(initializerVisitor, null);
                }

                for (ConstructorDeclaration cd : CTS.listCD) {
                    ConstructorSecondVisitor constructorVisitor = new ConstructorSecondVisitor(CTS.name, pathAbs, CTS.blockStructure, rangeStructure);
                    cd.accept(constructorVisitor, null);
                }


                DataStore.memory_field_info.put(CTS.name, memory_field_Tmp);

                for (MethodDeclaration md : CTS.listMD) {
                    MethodSecondVisitor methodVisitor = new MethodSecondVisitor(CTS.name, pathAbs, CTS.blockStructure, rangeStructure);
                    md.accept(methodVisitor, null);
                }
                DataStore.memory_method_info.put(CTS.name, memory_method_Tmp);
            }

            DataStore.memory_field_info.put(pathAbs, memory_field_Tmp);

        }
    }

    //デバッグ用の出力メソッド
    private static void OutputCheck(String pathDir){
        for(String pathAbs:DataStore.memoryPathAbs.get(pathDir)){
            for(Triplets<String, String, ClassInfomation> triplets:DataStore.memoryClass.getDataListKey1(pathAbs)){
                String classname = triplets.getRightValue().name;
                String kind = triplets.getRightValue().kind;
                boolean isStatic = triplets.getRightValue().isStatic;
                boolean isEnum = triplets.getRightValue().isEnum;
                String structure = triplets.getRightValue().blockStructure;
                ArrayDeque<Range> ranges = triplets.getRightValue().rangeStructure.clone();
                boolean isContainStatic = triplets.getRightValue().isContainStatic;
                System.out.println
                        ("name:" + classname + " kind:" + kind + " isStatic:" + isStatic
                                + " isEnum:" + isEnum + " isContainStatic:" + isContainStatic);
                System.out.println("definition:" + structure);
                System.out.println("rangeIN:" + ranges);
                System.out.println("field information:" + classname);
                for(String name:triplets.getRightValue().getMemoryF().keySet()){
                    FieldInfomation fd = triplets.getRightValue().getMemoryF().get(name);
                    OutputFieldInfonew(fd);
                }

                System.out.println("all infomation:" + classname);
                for(Triplets<String, Range, BlockInfomation> tripletsBI:triplets.getRightValue().memoryB.getDataList()){
                    BlockInfomation BI = tripletsBI.getRightValue();
                    OutputScope(BI);
                }
                System.out.println();
            }
        }
    }

    private static void OutputScope(BlockInfomation BI){
        System.out.println("name:" + BI.name + " kind:" + BI.kind + " isStatic:" + BI.isStatic + " range:" + BI.range);
        System.out.println("definition:" + BI.blockStructure);
        System.out.println("rangeIN:" + BI.rangeStructure.clone());
        System.out.println("variable information:" + BI.name);
        for(String name:BI.getMemoryF().keySet()){
            FieldInfomation fd = BI.getMemoryF().get(name);
            OutputFieldInfonew(fd);
        }

        if(BI.getMemory() != null){
            for(Triplets<String, Range, BlockInfomation> tripletsBI:BI.getMemory().getDataList()) {
                BlockInfomation BILower = tripletsBI.getRightValue();
                OutputScope(BILower);
            }
        }
    }

    private static void OutputFieldInfonew(FieldInfomation fd){
        System.out.println(fd.name + " type:" + fd.type + " dim:" + fd.dim
                + " assign:" + fd.assign + " nullable:" + fd.nullable + " static:" + fd.isStatic
                + " initializable:" + fd.initializable + " formerRW:" + fd.formerRW);
        System.out.println("definition:" + fd.blockStructure);
        System.out.println("range:" + fd.range);
        System.out.println("rangeIN:" + fd.rangeStructure.clone());
    }

    private static void OutputMethodInfonew(MethodInfomation md){
        System.out.println(md.name + " type:" + md.type + " lines:" + md.lines
                + " fix:" + md.fix + " nullable:" + md.nullable + " static:" + md.isStatic
                + " accessfield:" + md.accessField + " access:" + md.access);
        System.out.println("definition:" + md.blockStructure);
        System.out.println("range:" + md.range);
        System.out.println("rangeIN:" + md.rangeStructure.clone());
        System.out.println("local variable");
        for(String name:md.getMemoryF().keySet()){
            FieldInfomation fd = md.getMemoryF().get(name);
            OutputFieldInfonew(fd);
        }
    }


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
        String structure = "";
        ArrayDeque<Range> rangeStructure = new ArrayDeque<>();
        ArrayList<ImportDeclaration> Import_list = new ArrayList<>();
        ArrayList<FieldDeclaration> fds = new ArrayList<>();

        public FirstVisitor(String classname, String structure, ArrayDeque<Range> rangeStructure){
            this.classname = classname;
            this.structure = structure;
            this.rangeStructure = rangeStructure;
        }

        @Override
        public void visit(ImportDeclaration md, Void arg){
            super.visit(md, arg);
            Import_list.add(md);
        }

        @Override
        public void visit(ClassOrInterfaceDeclaration md, Void arg){
            String classname = md.getNameAsString();
            DataStore.memory_import.put(classname, Import_list);//pathAbsに変える
            //DataStore.memory_import.put(pathAbs, Import_list);

            DataStore.memory_classname.add(classname);//消す
            int size_extend = md.getExtendedTypes().size();
            int size_implement = md.getImplementedTypes().size();
            String classEx = "";
            ArrayList<String> classIm = new ArrayList<>();
            if(size_extend != 0){
                DataStore.memory_extend.put(classname, md.getExtendedTypes().get(0).getNameAsString());//消す
                classEx = md.getExtendedTypes().get(0).getNameAsString();
            }
            if(size_implement != 0){
                ArrayList<String> names = new ArrayList<>();//消す
                for(int i = 0; i < size_implement ;i++){
                    names.add(md.getImplementedTypes(i).getNameAsString());//消す
                    classIm.add(md.getImplementedTypes(i).getNameAsString());
                }
                DataStore.memory_implement.put(classname, names);//消す
            }

            //以下４行消去
            fds.addAll(md.getFields());
            DataStore.memory_classfield.put(classname, fds);
            DataStore.memory_classmethod.put(classname, md.getMethods());
            DataStore.memory_constructor.put(classname, md.getConstructors());

            String structureThis = this.structure + "/" + md.getNameAsString();

            Range range = md.getRange().get();
            String CorI = "class";
            if (md.isInterface()) CorI = "interface";
            ClassInfomation info = new ClassInfomation(classname, CorI, structureThis, pathAbs, range, this.rangeStructure, classEx, classIm, false, md.isStatic());
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

            //以下７行消去
            DataStore.memory_classname.add(classname);
            DataStore.memory_enum.add(classname);
            fds = new ArrayList<>();
            fds.addAll(md.getFields());
            DataStore.memory_classfield.put(classname, fds);
            DataStore.memory_classmethod.put(classname, md.getMethods());
            DataStore.memory_constructor.put(classname, md.getConstructors());

            String structureThis = this.structure + "/" + md.getNameAsString();
            Range range = md.getRange().get();
            ClassInfomation info = new ClassInfomation(classname, "Class", structureThis, pathDir, range, this.rangeStructure, true, md.isStatic());
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

    }

    private static class SomeVisitor extends VoidVisitorAdapter<Void> {
        String classname = "";
        String structure = "";
        ArrayDeque<Range> rangeStructure = new ArrayDeque<>();
        ArrayList<String> inner_list = new ArrayList<>();
        ArrayList<InitializerDeclaration> initializer_list = new ArrayList<>();

        public SomeVisitor(String classname, String structure, ArrayDeque<Range> rangeStructure){
            this.classname = classname;
            this.structure = structure;
            this.rangeStructure = rangeStructure;
            initializer_list = new ArrayList<>();
        }

        @Override
        public void visit(ClassOrInterfaceDeclaration md, Void arg) {
            //インナークラス周りの構造全部消す
            String classname = md.getNameAsString();
            if (DataStore.memory_innerclass.get(this.classname) == null) inner_list = new ArrayList<>();
            else inner_list = DataStore.memory_innerclass.get(this.classname);
            inner_list.add(classname);
            DataStore.memory_innerclass.put(this.classname, inner_list);

            FirstVisitor visitor = new FirstVisitor(classname, this.structure, this.rangeStructure);
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
        public void visit(MethodDeclaration md, Void arg){
            MethodFirstVisitor visitor = new MethodFirstVisitor(this.classname, this.structure, rangeStructure);
            md.accept(visitor, null);
        }

        @Override
        public void visit(ConstructorDeclaration md, Void arg){
            ConstructorFirstVisitor visitor = new ConstructorFirstVisitor(this.classname, this.structure, rangeStructure);
            md.accept(visitor, null);
        }

        @Override
        public void visit(InitializerDeclaration md, Void arg){
            if(!DataStore.memoryStatic.get(pathAbs))DataStore.memoryStatic.put(pathAbs, md.isStatic());
            DataStore.isStaticI = DataStore.isStaticI || md.isStatic();
            this.initializer_list.add(md);
            InitializerFirstVisitor visitor = new InitializerFirstVisitor(this.classname, this.structure, rangeStructure);
            md.accept(visitor, null);
        }

        @Override
        public void visit(EnumDeclaration md, Void arg){

            FirstVisitor visitor = new FirstVisitor(this.classname, this.structure, this.rangeStructure);
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
        private ArrayList<BlockInfomation> BIStore = new ArrayList<>();

        public VariableFirstVisitor(String name, String structure, ArrayDeque<Range> rangeStructure, NodeList<Modifier> modifiers){
            classname = name;
            this.structure = structure;
            this.rangeStructure = rangeStructure;
            this.modifiers = modifiers;
        }


        @Override
        public void visit(VariableDeclarator md, Void arg){
            String fieldname = md.getNameAsString();
            String type = md.getTypeAsString();
            Range range = md.getRange().get();
            boolean isStatic = false;
            int dim = 0;
            boolean assign = false;
            boolean nullable = false;
            boolean initializable = true;
            String formerRW = "";
            String structureThis = structure + "/" + fieldname;

            if(md.getInitializer().isEmpty()) {
                initializable = false;
                //formerRW = "R";
            } else {
                formerRW = "W";
                ValueInitialVisitor visitor = new ValueInitialVisitor(classname, fieldname, range, rangeStructure, structure);
                md.getInitializer().get().accept(visitor, null);
                for(BlockInfomation BI:visitor.getBILower()) BIStore.add(BI);
            }


            if(md.getType().isArrayType()){
                int dimNum = type.length();
                type = type.replace("[]", "");
                dimNum = dimNum - type.length();
                dim = dimNum / 2;
                type = type.concat("-array");
            }

            for(Modifier modifier:modifiers){
                if(modifier.toString().equals("static ")) {
                    if(!DataStore.memoryStatic.get(pathAbs))DataStore.memoryStatic.put(pathAbs, true);
                    DataStore.isStaticF = true;
                    isStatic = true;
                    break;
                }
            }

            FieldInfomation FI
                    = new FieldInfomation(fieldname, structureThis,
                    pathDir, type, dim, assign, nullable, isStatic,
                    initializable, formerRW, range, rangeStructure);

            ClassInfomation CI = DataStore.memoryClass.getData(pathAbs, classname);
            if(!CI.isContainStatic)CI.isContainStatic = isStatic;
            if (structure.equals(CI.blockStructure)){
                for(BlockInfomation BILow : BIStore){
                    CI.setMemory(BILow);
                }
                CI.setMemoryF(FI);
                DataStore.memoryClass.setData(pathAbs, classname, CI);
            }

        }
    }

    private static class MethodFirstVisitor extends VoidVisitorAdapter<Void>{
        String classname = "";
        String structure = "";
        ArrayDeque<Range> rangeStructure = new ArrayDeque<>();
        ArrayList<String> paramList = new ArrayList<>();

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
            String methodname = md.getNameAsString();
            String type = md.getTypeAsString();
            Range range = md.getRange().get();

            for(Modifier mod:md.getModifiers()){
                if(mod.toString().equals("static ")) {
                    if(!DataStore.memoryStatic.get(pathAbs))DataStore.memoryStatic.put(pathAbs, true);
                    DataStore.isStaticM = true;
                    isStatic = true;
                    break;
                }
            }

            if(md.getType().isArrayType()){
                type = type.replace("[]", "").concat("-array");
            }

            int numLine = 0;
            if(md.getBody().isPresent())
                numLine = md.getBody().get().getChildNodes().size();

            initLocalValueList(classname, range);

            boolean isReturnNull = true;
            boolean access = isAccessorCheckG || isAccessorCheckS;
            String structureThis = this.structure + "/" + md.getNameAsString();
            MethodInfomation MI
                    = new MethodInfomation(methodname, structureThis, pathDir, isStatic, access, false,
                    "", numLine, isReturnNull, type, range, this.rangeStructure);
            ArrayDeque<Range> rangeStructure = this.rangeStructure.clone();
            rangeStructure.add(range);


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
                for(BlockInfomation BILow : visitor.getBILower()){
                    MI.setMemory(BILow);
                }
            }
            ClassInfomation CI = DataStore.memoryClass.getData(pathAbs, classname);
            if(!CI.isContainStatic)CI.isContainStatic = isStatic;
            if (structure.equals(CI.blockStructure)){
                CI.setMemoryM(MI);
                CI.setMemory(MI);
                DataStore.memoryClass.setData(pathAbs, classname, CI);
            }
        }

    }

    private static class ConstructorFirstVisitor extends VoidVisitorAdapter<Void>{
        String classname = "";
        String structure = "";
        ArrayDeque<Range> rangeStructure = new ArrayDeque<>();
        ArrayList<String> paramList = new ArrayList<>();

        public ConstructorFirstVisitor(String name, String structure, ArrayDeque<Range> rangeStructure){
            classname = name;
            this.structure = structure;
            this.rangeStructure = rangeStructure;
        }

        @Override
        public void visit(ConstructorDeclaration md, Void arg){
            if (!DataStore.memoryStatic.get(pathAbs)) DataStore.memoryStatic.put(pathAbs, md.isStatic());

            String structureThis = this.structure + "/" + md.getNameAsString();
            Range range = md.getRange().get();
            BlockInfomation BI = new BlockInfomation(md.getNameAsString(), "Constructor", md.isStatic(), structureThis, pathDir, range, this.rangeStructure);

            initLocalValueList(classname, range);
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
            for(BlockInfomation BILow : visitor.getBILower()){
                BI.setMemory(BILow);
            }
            ClassInfomation CI = DataStore.memoryClass.getData(pathAbs, classname);
            if(!CI.isContainStatic)CI.isContainStatic = md.isStatic();
            if (structure.equals(CI.blockStructure)){
                CI.setMemory(BI);
                DataStore.memoryClass.setData(pathAbs, classname, CI);
            }
        }

    }

    private static class InitializerFirstVisitor extends VoidVisitorAdapter<Void>{
        String classname = "";
        String structure = "";
        ArrayDeque<Range> rangeStructure = new ArrayDeque<>();

        public InitializerFirstVisitor(String name, String structure, ArrayDeque<Range> rangeStructure){
            classname = name;
            this.structure = structure;
            this.rangeStructure = rangeStructure;
        }

        @Override
        public void visit(InitializerDeclaration md, Void arg){
            if (!DataStore.memoryStatic.get(pathAbs)) DataStore.memoryStatic.put(pathAbs, md.isStatic());

            String structureThis = this.structure + "/Initializer";
            Range range = md.getRange().get();
            BlockInfomation BI = new BlockInfomation("Initializer", "Initializer", md.isStatic(), structureThis, pathDir, range, this.rangeStructure);

            initLocalValueList(classname, range);

            ArrayDeque<Range> rangeStructure = this.rangeStructure.clone();
            rangeStructure.add(range);
            InBlockFirstVisitorS visitor = new InBlockFirstVisitorS(classname, range, rangeStructure, structureThis);
            md.getBody().accept(visitor, null);
            BI.setMemoryF(visitor.getLocal());
            for(BlockInfomation BILow : visitor.getBILower()){
                BI.setMemory(BILow);
            }
            ClassInfomation CI = DataStore.memoryClass.getData(pathAbs, classname);
            if(!CI.isContainStatic)CI.isContainStatic = md.isStatic();
            if (structure.equals(CI.blockStructure)){
                CI.setMemory(BI);
                DataStore.memoryClass.setData(pathAbs, classname, CI);
            }
        }

    }

    public static class InBlockFirstVisitorS extends VoidVisitorAdapter<Void>{
        String classname = "";
        String methodname = "";
        String structure = "";
        ArrayDeque<Range> rangeStructure = new ArrayDeque<>();
        Range range = null;
        ArrayList<String> paramList = new ArrayList<>();
        private ArrayList<FieldInfomation> localValueList = new ArrayList<>();
        private ArrayList<BlockInfomation> BIUpperStore = new ArrayList<>();

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
        public void visit(VariableDeclarator md, Void arg){
            HashMap<String,String> data = new HashMap<>();
            String fieldname = md.getNameAsString();
            String type = md.getTypeAsString();
            Range range = md.getRange().get();
            int dim = 0;
            boolean assign = false;
            boolean nullable = false;
            boolean initializable = true;
            String formerRW = "";
            String structureThis = structure + "/" + fieldname;

            if(md.getInitializer().isEmpty()) {
                assign = true;
                nullable = true;
                initializable = false;
                formerRW = "R";
            } else {
                formerRW = "W";
                ValueInitialVisitor visitor = new ValueInitialVisitor(classname, fieldname, range, rangeStructure, structure);
                md.getInitializer().get().accept(visitor, null);
                for(BlockInfomation BILow : visitor.getBILower()){
                    BIUpperStore.add(BILow);
                }

            }

            if(md.getType().isArrayType()){
                int dimNum = type.length();
                type = type.replace("[]", "");
                dimNum = dimNum - type.length();
                dim = dimNum / 2;
                type = type.concat("-array");
            }

            data.put("type", type);
            data.put("dim", dim+"");
            data.put("assign", assign+"");
            data.put("nullable", nullable+"");
            data.put("initializable", initializable+"");
            data.put("formerRW", formerRW);

            FieldInfomation FI
                    = new FieldInfomation(fieldname, structureThis, pathDir, type, dim, assign,
                    nullable, false, initializable, formerRW, range, this.rangeStructure);

            localValueList.add(FI);
        }

        @Override
        public void visit(Parameter md, Void arg){
            String fieldname = md.getNameAsString();
            String type = md.getTypeAsString();
            int dim = 0;
            boolean assign = false;
            boolean nullable = false;
            boolean initializable = true;
            String formerRW = "W";


            if(md.getType().isArrayType()){
                int dimNum = type.length();
                type = type.replace("[]", "");
                dimNum = dimNum - type.length();
                dim = dimNum / 2;
                type = type.concat("-array");
            }

            Range range = md.getRange().get();

            String structureThis = this.structure + "/" + fieldname;
            FieldInfomation FI
                    = new FieldInfomation(fieldname, structureThis, pathDir, type, dim, assign,
                    nullable, false, initializable, formerRW, range, this.rangeStructure);

            localValueList.add(FI);
        }

        @Override
        public void visit(LocalClassDeclarationStmt md, Void arg){
            FirstVisitor visitor = new FirstVisitor(classname, this.structure, this.rangeStructure);
            md.accept(visitor, null);
        }

        @Override
        public void visit(DoStmt md, Void arg){
            //do-whileのブロック文内部の確認
            String structure = this.structure + "/Do-while";
            Range range = md.getRange().get();
            ArrayDeque<Range> rangeStructure = this.rangeStructure.clone();
            rangeStructure.add(range);

            //内部チェック
            InBlockFirstVisitorS bodyVisitor = new InBlockFirstVisitorS(classname, methodname, this.range, rangeStructure, structure, paramList);
            md.getBody().accept(bodyVisitor, null);

            BlockInfomation BI = new BlockInfomation("Do-while", "Do-while", false, structure, pathDir, range, this.rangeStructure);
            BI.setMemoryF(bodyVisitor.getLocal());
            for(BlockInfomation BILow : bodyVisitor.getBILower()){
                BI.setMemory(BILow);
            }
            BIUpperStore.add(BI);
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

            BlockInfomation BI = new BlockInfomation("While", "While", false, structure, pathDir, range, this.rangeStructure);
            BI.setMemoryF(bodyVisitor.getLocal());
            for(BlockInfomation BILow : bodyVisitor.getBILower()){
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

            BlockInfomation BI = new BlockInfomation("For", "For", false, structure, pathDir, range, this.rangeStructure);

            //initializeが変数宣言だった場合の解析
            for(Expression expression:md.getInitialization()) {
                InBlockFirstVisitorS initVisitor = new InBlockFirstVisitorS(classname, methodname, this.range, rangeStructure, structure, paramList);
                expression.accept(initVisitor, null);
                BI.setMemoryF(initVisitor.getLocal());
                for(BlockInfomation BILow : initVisitor.getBILower()){
                    BI.setMemory(BILow);
                }
            }

            //内部チェック
            InBlockFirstVisitorS bodyVisitor = new InBlockFirstVisitorS(classname, methodname, range, rangeStructure, structure, paramList);
            md.getBody().accept(bodyVisitor, null);
            BI.setMemoryF(bodyVisitor.getLocal());
            for (BlockInfomation BILow : bodyVisitor.getBILower()) {
                BI.setMemory(BILow);
            }
            BIUpperStore.add(BI);
        }

        @Override
        public void visit(ForEachStmt md, Void arg){
            //foreachのiterableにあたる部分の読み込み変数のチェックとブロック文内部の確認
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

            BlockInfomation BI = new BlockInfomation("If", "If", false, structure, pathDir, range, this.rangeStructure);
            BI.setMemoryF(thenBlockVisitor.getLocal());
            for (BlockInfomation BILow : thenBlockVisitor.getBILower()) {
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
                BI = new BlockInfomation("Else", "Else", false, structure, pathDir, range, this.rangeStructure);
                BI.setMemoryF(elseIfVisitor.getLocal());
                for (BlockInfomation BILow : elseIfVisitor.getBILower()) {
                    BI.setMemory(BILow);
                }
                BIUpperStore.add(BI);
            }
        }

        @Override
        public void visit(SwitchStmt md, Void arg){
            //switchのブロック文内部の確認
            String structure = this.structure + "/Switch";
            Range range = md.getRange().get();
            ArrayDeque<Range> rangeStructure = this.rangeStructure.clone();
            rangeStructure.add(range);
            BlockInfomation BI = new BlockInfomation("Switch", "Switch", false, structure, pathDir, range, this.rangeStructure);

            for(SwitchEntry entry:md.getEntries()){
                String structureLabel = structure + "/";
                StringBuilder label = new StringBuilder();
                for(Expression expression:entry.getLabels()) label.append(expression);
                if(entry.getLabels().size() == 0) label.append("default");
                structureLabel = structureLabel + label;
                Range rangeEntry = entry.getRange().get();
                ArrayDeque<Range> rangeStructureEntry = rangeStructure.clone();
                rangeStructureEntry.add(rangeEntry);
                BlockInfomation BIEntry = new BlockInfomation("SwitchEntry", "SwitchEntry", false, structureLabel, pathDir, rangeEntry, rangeStructure);
                for(Statement statement:entry.getStatements()){
                    InBlockFirstVisitorS statementVisitor = new InBlockFirstVisitorS(classname, methodname, range, rangeStructureEntry, structureLabel, paramList);
                    statement.accept(statementVisitor, null);
                    BIEntry.setMemoryF(statementVisitor.getLocal());
                    for(BlockInfomation BILow : statementVisitor.getBILower()){
                        BIEntry.setMemory(BILow);
                    }
                }
                BI.setMemory(BIEntry);
            }
            BIUpperStore.add(BI);

        }

        @Override
        public void visit(SwitchExpr md, Void arg){
            //switchのブロック文内部の確認
            String structure = this.structure + "/Switch";
            Range range = md.getRange().get();
            ArrayDeque<Range> rangeStructure = this.rangeStructure.clone();
            rangeStructure.add(range);
            BlockInfomation BI = new BlockInfomation("Switch", "Switch", false, structure, pathDir, range, this.rangeStructure);

            for(SwitchEntry entry:md.getEntries()){
                String structureLabel = structure + "/";
                StringBuilder label = new StringBuilder();
                for(Expression expression:entry.getLabels()) label.append(expression);
                if(entry.getLabels().size() == 0) label.append("default");
                structureLabel = structureLabel + label;
                Range rangeEntry = entry.getRange().get();
                ArrayDeque<Range> rangeStructureEntry = rangeStructure.clone();
                rangeStructureEntry.add(rangeEntry);
                BlockInfomation BIEntry = new BlockInfomation("SwitchEntry", "SwitchEntry", false, structureLabel, pathDir, rangeEntry, rangeStructure);
                for(Statement statement:entry.getStatements()){
                    InBlockFirstVisitorS statementVisitor = new InBlockFirstVisitorS(classname, methodname, range, rangeStructureEntry, structureLabel, paramList);
                    statement.accept(statementVisitor, null);
                    BIEntry.setMemoryF(statementVisitor.getLocal());
                    for(BlockInfomation BILow : statementVisitor.getBILower()){
                        BIEntry.setMemory(BILow);
                    }
                }
                BI.setMemory(BIEntry);
            }
            BIUpperStore.add(BI);
        }

        public ArrayList<FieldInfomation> getLocal(){
            return this.localValueList;
        }

        public ArrayList<BlockInfomation> getBILower() {
            return this.BIUpperStore;
        }
    }

    public static class ValueInitialVisitor extends VoidVisitorAdapter<Void>{
        String classname = "";
        String pathAbs = "";
        String contentsname = "";
        Range range = null;
        String structure = "";
        ArrayDeque<Range> rangeStructure = new ArrayDeque<>();
        ArrayList<String> paramList = new ArrayList<>();

        private ArrayList<BlockInfomation> BIUpperStore = new ArrayList<>();

        private boolean isAnonymousAnalysis = false;
        private boolean isLambdaAnalysis = false;

        public ValueInitialVisitor
                (String classname, String contentsname, Range range, ArrayDeque<Range> rangeStructure, String structure) {
            this.classname = classname;
            this.contentsname = contentsname;
            this.range = range;
            this.rangeStructure = rangeStructure;
            this.structure = structure;
        }

        @Override
        public void visit(LambdaExpr md, Void arg){
            BlockInfomation BI = new BlockInfomation(contentsname, "Lambda", false, structure + "/" + contentsname, pathDir, range, this.rangeStructure);

            for(Parameter param:md.getParameters()){
                paramList.add(param.getNameAsString());
                InBlockFirstVisitorS visitor = new InBlockFirstVisitorS(classname, contentsname, range, rangeStructure, structure, paramList);
                param.accept(visitor, null);
                BI.setMemoryF(visitor.getLocal());
            }

            InBlockFirstVisitorS visitor = new InBlockFirstVisitorS(classname, contentsname, range, rangeStructure, structure, paramList);
            md.getBody().accept(visitor, null);
            BI.setMemoryF(visitor.getLocal());
            BIUpperStore.add(BI);
        }

        public ArrayList<BlockInfomation> getBILower() {
            return this.BIUpperStore;
        }
    }

    //２回目の解析でもう少し詳しい情報を集める
    private static class FieldVisitor extends VoidVisitorAdapter<Void>{
        String classname = "";
        String pathAbs = "";
        String structure = "";
        ArrayDeque<Range> rangeStructure = new ArrayDeque<>();
        private boolean isAnonymousAnalysis = false;
        private ArrayList<FieldInfomation> FIStoreList = null;
        public FieldVisitor(String name, String pathAbs, String structure, ArrayDeque<Range> rangeStructure){
            classname = name;
            this.pathAbs = pathAbs;
            this.structure = structure;
            this.rangeStructure = rangeStructure;
        }

        public FieldVisitor(String name, String pathAbs, String structure, ArrayDeque<Range> rangeStructure, boolean flag) {
            this(name, pathAbs, structure, rangeStructure);
            isAnonymousAnalysis = flag;
            if(flag) FIStoreList = new ArrayList<>();
        }


        @Override
        public void visit(FieldDeclaration md, Void arg){
            NodeList<Modifier> modifiers = new NodeList<>();
            if(md.getModifiers().size() != 0) modifiers = md.getModifiers();
            for(VariableDeclarator vd: md.getVariables()) {
                VariableSecondVisitor VarVisitor
                        = new VariableSecondVisitor(this.classname, pathAbs, structure, this.rangeStructure, modifiers, isAnonymousAnalysis);
                vd.accept(VarVisitor, null);
                if(VarVisitor.getFIstore() != null) FIStoreList.add(VarVisitor.getFIstore());
            }
        }

        public ArrayList<FieldInfomation> getFIStoreList(){
            return FIStoreList;
        }
    }

    private static class VariableSecondVisitor extends VoidVisitorAdapter<Void>{
        String classname = "";
        String pathAbs = "";
        String structure = "";
        ArrayDeque<Range> rangeStructure = new ArrayDeque<>();
        NodeList<Modifier> modifiers = new NodeList<>();
        private boolean isAnonymousAnalysis = false;
        private FieldInfomation FIstore = null;
        private ArrayList<BlockInfomation> BIstore = new ArrayList<>();

        public VariableSecondVisitor(String name, String pathAbs, String structure, ArrayDeque<Range> rangeStructure, NodeList<Modifier> modifiers){
            classname = name;
            this.pathAbs = pathAbs;
            this.structure = structure;
            this.rangeStructure = rangeStructure;
            this.modifiers = modifiers;
        }

        public VariableSecondVisitor(String name, String pathAbs, String structure, ArrayDeque<Range> rangeStructure, NodeList<Modifier> modifiers, boolean flag){
            this(name, pathAbs, structure, rangeStructure, modifiers);
            isAnonymousAnalysis = flag;
        }
            @Override
        public void visit(VariableDeclarator md, Void arg){
            HashMap<String,String> data = new HashMap<>();
            String fieldname = md.getNameAsString();
            String type = md.getTypeAsString();
            Range range = md.getRange().get();
            boolean isStatic = false;
            int dim = 0;
            boolean assign = false;
            boolean nullable = false;
            boolean initializable = true;
            boolean isAnonymous = false;
            boolean isLambda = false;
            String formerRW = "";

            if(md.getInitializer().isEmpty()) {
                initializable = false;
                //formerRW = "R";
            } else {
                formerRW = "W";
                AssignVisitorS targetVisitor = new AssignVisitorS(classname, pathAbs, fieldname, range, rangeStructure, structure, false);
                md.getInitializer().get().accept(targetVisitor, null);
                isAnonymous = targetVisitor.isAnonymousAnalysis();
                isLambda = targetVisitor.isLambdaAnalysis();
                BIstore = targetVisitor.getBILower();
            }
            
            if(md.getType().isArrayType()){
                int dimNum = type.length();
                type = type.replace("[]", "");
                dimNum = dimNum - type.length();
                dim = dimNum / 2;
                type = type.concat("-array");
            }

            for(Modifier modifier:modifiers){
                if(modifier.toString().equals("static ")) {
                    if(!DataStore.memoryStatic.get(pathAbs))DataStore.memoryStatic.put(pathAbs, true);
                    DataStore.isStaticF = true;
                    isStatic = true;
                    break;
                }
            }

            data.put("type", type);
            data.put("static", isStatic + "");
            data.put("dim", dim+"");
            data.put("assign", assign+"");
            data.put("nullable", nullable+"");
            data.put("initializable", initializable+"");
            data.put("formerRW", formerRW);

            memory_field_Tmp.put(fieldname, data);

            FieldInfomation FI
                    = new FieldInfomation(fieldname, structure + "/" + fieldname, pathDir, type, dim, assign,
                    nullable, isStatic, initializable, formerRW, range, rangeStructure);
            FI.isAnonymous = isAnonymous;
            FI.isLambda = isLambda;

            if(isAnonymousAnalysis){
                FIstore = FI;
            } else {
                FIstore = null;
                ClassInfomation CI = DataStore.memoryClass.getData(pathAbs, classname);
                if (!CI.isContainStatic) CI.isContainStatic = isStatic;
                if (structure.equals(CI.blockStructure + "/" + classname)) {
                    CI.setMemoryF(FI);
                    for(BlockInfomation BI:BIstore){
                        CI.setMemory(BI);
                    }
                    DataStore.memoryClass.setData(pathAbs, classname, CI);
                }
            }
        }

        public FieldInfomation getFIstore(){
            return FIstore;
        }
    }

    private static class ConstructorSecondVisitor extends VoidVisitorAdapter<Void>{
        String classname = "";
        String pathAbs = "";
        String structure = "";
        ArrayDeque<Range> rangeStructure = new ArrayDeque<>();
        ArrayList<String> paramList = new ArrayList<>();
        private boolean isAnonymousAnalysis = false;

        public ConstructorSecondVisitor(String name, String pathAbs, String structure, ArrayDeque<Range> rangeStructure){
            classname = name;
            this.pathAbs = pathAbs;
            this.structure = structure;
            this.rangeStructure = rangeStructure;
        }

        public ConstructorSecondVisitor(String name, String pathAbs, String structure, ArrayDeque<Range> rangeStructure, boolean flag){
            this(name, pathAbs, structure, rangeStructure);
            isAnonymousAnalysis = flag;
        }

            @Override
        public void visit(ConstructorDeclaration md, Void arg){
            if (!DataStore.memoryStatic.get(pathAbs)) DataStore.memoryStatic.put(pathAbs, md.isStatic());

            String structureThis = this.structure + "/" + md.getNameAsString();
            Range range = md.getRange().get();
            BlockInfomation BI = new BlockInfomation(md.getNameAsString(), "Constructor", md.isStatic(), structureThis, pathDir, range, this.rangeStructure);

            initLocalValueList(classname, range);
            ArrayDeque<Range> rangeStructure = this.rangeStructure.clone();
            rangeStructure.add(range);

            for(Parameter tmp:md.getParameters()){
                paramList.add(tmp.getNameAsString());
                InBlockSecondVisitorS visitor = new InBlockSecondVisitorS(classname, pathAbs, range, rangeStructure, structureThis, paramList);
                tmp.accept(visitor, null);
                BI.setMemoryF(visitor.getLocal());
            }

            InBlockSecondVisitorS visitor = new InBlockSecondVisitorS(classname, pathAbs, range, rangeStructure, structureThis, paramList);
            md.getBody().accept(visitor, null);
            BI.setMemoryF(visitor.getLocal());
            for (BlockInfomation BILow : visitor.getBILower()) {
                BI.setMemory(BILow);
            }
            ClassInfomation CI = DataStore.memoryClass.getData(pathAbs, classname);
            if(!CI.isContainStatic)CI.isContainStatic = md.isStatic();
            CI.setMemory(BI);
            DataStore.memoryClass.setData(pathAbs, classname, CI);
        }
    }

    private static class InitializerSecondVisitor extends VoidVisitorAdapter<Void>{
        String classname = "";
        String pathAbs = "";
        String structure = "";
        ArrayDeque<Range> rangeStructure = new ArrayDeque<>();
        private boolean isAnonymousAnalysis = false;

        public InitializerSecondVisitor(String name, String pathAbs, String structure, ArrayDeque<Range> rangeStructure){
            classname = name;
            this.pathAbs = pathAbs;
            this.structure = structure;
            this.rangeStructure = rangeStructure;
        }

        public InitializerSecondVisitor(String name, String pathAbs, String structure, ArrayDeque<Range> rangeStructure, boolean flag){
            this(name, pathAbs, structure, rangeStructure);
            isAnonymousAnalysis = flag;
        }

            @Override
        public void visit(InitializerDeclaration md, Void arg){
            if (!DataStore.memoryStatic.get(pathAbs)) DataStore.memoryStatic.put(pathAbs, md.isStatic());

            String structureThis = this.structure + "/Initializer";
            Range range = md.getRange().get();
            BlockInfomation BI = new BlockInfomation("Initializer", "Initializer", md.isStatic(), structureThis, pathDir, range, this.rangeStructure);
            //System.out.println("pathDir:" + pathDir + "\npathAbs:" + pathAbs + "\nclassname:" + this.classname);
            initLocalValueList(classname, range);

            ArrayDeque<Range> rangeStructure = this.rangeStructure.clone();
            rangeStructure.add(range);
            InBlockSecondVisitorS visitor = new InBlockSecondVisitorS(classname, pathAbs, range, rangeStructure, structureThis);
            md.getBody().accept(visitor, null);
            BI.setMemoryF(visitor.getLocal());
            for(BlockInfomation BILow : visitor.getBILower()){
                BI.setMemory(BILow);
            }
            ClassInfomation CI = DataStore.memoryClass.getData(pathAbs, classname);
            if(!CI.isContainStatic)CI.isContainStatic = md.isStatic();
            CI.setMemory(BI);
            DataStore.memoryClass.setData(pathAbs, classname, CI);
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
        String type = "";
        ArrayList<String> paramList = new ArrayList<>();
        private boolean isAnonymousAnalysis = false;
        private MethodInfomation MIStore = null;
        HashMap<String, HashMap<String, String>> memory_local_Tmp = new HashMap<>();


        public MethodSecondVisitor(String name, String pathAbs, String structure, ArrayDeque<Range> rangeStructure){
            classname = name;
            this.pathAbs = pathAbs;
            this.structure = structure;
            this.rangeStructure = rangeStructure;
        }

        public MethodSecondVisitor(String name, String pathAbs, String structure, ArrayDeque<Range> rangeStructure, boolean flag){
            this(name, pathAbs, structure, rangeStructure);
            isAnonymousAnalysis = flag;
        }

            @Override
        public void visit(MethodDeclaration md, Void arg){
            isAccessorCheckG = search_get(md);
            isAccessorCheckS = search_set(md);
            methodname = md.getNameAsString();
            type = md.getTypeAsString();
            Range range = md.getRange().get();


            for(Modifier mod:md.getModifiers()){
                if(mod.toString().equals("static ")) {
                    if(!DataStore.memoryStatic.get(pathAbs))DataStore.memoryStatic.put(pathAbs, true);
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
            String structureThis = this.structure + "/" + md.getNameAsString();
            ArrayList<FieldInfomation> localList = new ArrayList<>();

            boolean access = isAccessorCheckG || isAccessorCheckS;
            MethodInfomation MI
                    = new MethodInfomation(methodname, structureThis, pathDir, isStatic, access, false, fieldname, numLine, isReturnNull, type, range, this.rangeStructure);

            ArrayDeque<Range> rangeStructure = this.rangeStructure.clone();
            rangeStructure.add(range);

            for(Parameter param:md.getParameters()){
                paramList.add(param.getNameAsString());
                InBlockSecondVisitorS visitor = new InBlockSecondVisitorS(classname, pathAbs,  methodname, range, rangeStructure, structureThis, isAccessorCheckG, isAccessorCheckS, paramList);
                param.accept(visitor, null);
                MI.setMemoryF(visitor.getLocal());
            }

            if(md.getBody().isPresent()) {
                InBlockSecondVisitorS visitor = new InBlockSecondVisitorS(classname, pathAbs,  methodname, range, rangeStructure, structureThis, isAccessorCheckG, isAccessorCheckS, paramList);
                md.getBody().get().accept(visitor, null);
                MI.setMemoryF(visitor.getLocal());
                for(BlockInfomation BILow : visitor.getBILower()){
                    MI.setMemory(BILow);
                }
                fieldname = visitor.getFieldname();
                isFixableG = visitor.getIsFixableG();
                isFixableS = visitor.getIsFixableS();
                isReturnNull = visitor.getIsReturnNull();
                localList.addAll(visitor.getLocal());
            }

            setData(isFixableS||isFixableG, fieldname, range, isReturnNull);
            MI.fix = isFixableS||isFixableG;


            //System.out.println("pathDir:" + pathDir + "\npathAbs:" + pathAbs + "\nclassname:" + this.classname);
            /*ClassInfomation CI = DataStore.memoryClass.getData(pathAbs, classname);
            if(!CI.isContainStatic)CI.isContainStatic = isStatic;
            if (structure.equals(CI.blockStructure)){
                CI.setMemoryM(MI);
                DataStore.memoryClass.setData(pathAbs, classname, CI);
            }*/



            if(isAnonymousAnalysis) {
                MIStore = MI;
            } else {
                MIStore = null;
                ClassInfomation CI = DataStore.memoryClass.getData(pathAbs, classname);
                if(!CI.isContainStatic)CI.isContainStatic = md.isStatic();
                CI.setMemory(MI);
                DataStore.memoryClass.setData(pathAbs, classname, CI);
            }
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

        public MethodInfomation getMIStore(){
            return MIStore;
        }
    }

    public static class InBlockSecondVisitorS extends VoidVisitorAdapter<Void>{
        String classname = "";
        String pathAbs = "";
        String methodname = "";
        String structure = "";
        ArrayDeque<Range> rangeStructure = new ArrayDeque<>();
        Range range = null;
        private String fieldname = "";
        private boolean isAccessorCheckG = false;
        private boolean isAccessorCheckS = false;
        private boolean isFixableG = false;
        private boolean isFixableS = false;
        private boolean isReturnNull = false;
        ArrayList<String> paramList = new ArrayList<>();
        private ArrayList<FieldInfomation> localValueList = new ArrayList<>();
        private ArrayList<BlockInfomation> BIUpperStore = new ArrayList<>();

        //Method用のコンストラクタ
        public InBlockSecondVisitorS
                (String classname, String pathAbs, String methodname, Range range, ArrayDeque<Range> rangeStructure, String structure,
                 boolean isAccessorCheckG, boolean isAccessorCheckS, ArrayList<String> paramList){
            this.classname = classname;
            this.pathAbs = pathAbs;
            this.methodname = methodname;
            this.structure = structure;
            this.rangeStructure = rangeStructure;
            this.range = range;
            this.isAccessorCheckG = isAccessorCheckG;
            this.isAccessorCheckS = isAccessorCheckS;
            this.paramList = paramList;
        }

        //Constructor用のコンストラクタ
        public InBlockSecondVisitorS(String classname, String pathAbs, Range range, ArrayDeque<Range> rangeStructure, String structure, ArrayList<String> paramList){
            this(classname, pathAbs, "", range, rangeStructure, structure, false, false, paramList);
        }

        //Initializer用のコンストラクタ
        public InBlockSecondVisitorS(String classname, String pathAbs, Range range, ArrayDeque<Range> rangeStructure, String structure){
            this(classname, pathAbs, "", range, rangeStructure, structure, false, false, new ArrayList<String>());
        }

        @Override
        public void visit(VariableDeclarator md, Void arg){
            HashMap<String,String> data = new HashMap<>();
            String fieldname = md.getNameAsString();
            String type = md.getTypeAsString();
            int dim = 0;
            boolean assign = false;
            boolean nullable = false;
            boolean initializable = true;
            String formerRW = "";

            if(md.getInitializer().isEmpty()) {
                assign = true;
                nullable = true;
                initializable = false;
                formerRW = "R";
            } else {
                formerRW = "W";
                AssignVisitorS targetVisitor = new AssignVisitorS(classname, pathAbs, fieldname, range, rangeStructure, structure, false);
                md.getInitializer().get().accept(targetVisitor, null);
                for(BlockInfomation BILow : targetVisitor.getBILower()){
                    BIUpperStore.add(BILow);
                }
            }

            if(md.getType().isArrayType()){
                int dimNum = type.length();
                type = type.replace("[]", "");
                dimNum = dimNum - type.length();
                dim = dimNum / 2;
                type = type.concat("-array");
            }

            data.put("type", type);
            data.put("dim", dim+"");
            data.put("assign", assign+"");
            data.put("nullable", nullable+"");
            data.put("initializable", initializable+"");
            data.put("formerRW", formerRW);

            DataStore.memory_localValue_info.get(classname).get(range).put(fieldname, data);

            Range range = md.getRange().get();

            FieldInfomation FI
                    = new FieldInfomation(fieldname, structure + "/" + fieldname, pathDir, type, dim, assign,
                    nullable, false, initializable, formerRW, range, this.rangeStructure);

            localValueList.add(FI);
        }

        @Override
        public void visit(Parameter md, Void arg) {
            String fieldname = md.getNameAsString();
            String type = md.getTypeAsString();
            int dim = 0;
            boolean assign = false;
            boolean nullable = false;
            boolean initializable = true;
            String formerRW = "W";


            if (md.getType().isArrayType()) {
                int dimNum = type.length();
                type = type.replace("[]", "");
                dimNum = dimNum - type.length();
                dim = dimNum / 2;
                type = type.concat("-array");
            }

            Range range = md.getRange().get();

            String structureThis = this.structure + "/" + fieldname;
            FieldInfomation FI
                    = new FieldInfomation(fieldname, structureThis, pathDir, type, dim, assign,
                    nullable, false, initializable, formerRW, range, this.rangeStructure);

            localValueList.add(FI);
        }

        @Override
        public void visit(AssignExpr md, Void arg){
            //式の右側の解析
            AssignVisitorS valueVisitor = new AssignVisitorS(classname, pathAbs, methodname, this.range, rangeStructure, structure, isAccessorCheckG, isAccessorCheckS, paramList, false);
            md.getValue().accept(valueVisitor, null);

            //式の左側の解析
            String value = md.getValue().toString();

            //代入以外のAssign文の場合、一度値を参照してから代入するので、先にReadのチェック
            if (!md.getOperator().toString().equals("ASSIGN")) {
                AssignVisitorS targetVisitor = new AssignVisitorS(classname, pathAbs, methodname, this.range, rangeStructure, structure, isAccessorCheckG, isAccessorCheckS, paramList, false, value);
                md.getTarget().accept(targetVisitor, null);
            }
            AssignVisitorS targetVisitor = new AssignVisitorS(classname, pathAbs, methodname, this.range, rangeStructure, structure, isAccessorCheckG, isAccessorCheckS, paramList, true, value);
            md.getTarget().accept(targetVisitor, null);
            isFixableS = targetVisitor.getIsFixableS();
            fieldname = targetVisitor.getFieldname();
        }

        @Override
        public void visit(ReturnStmt md, Void arg){
            if(md.getExpression().isPresent()) {
                AssignVisitorS visitor = new AssignVisitorS(classname, pathAbs, methodname, this.range, rangeStructure, structure, isAccessorCheckG, isAccessorCheckS, paramList, false);
                md.getExpression().get().accept(visitor, null);
                isFixableG = visitor.getIsFixableG();
                fieldname = visitor.getFieldname();
                isReturnNull = visitor.getIsReturnNull();
                for(BlockInfomation BI:visitor.getBILower()){
                    BIUpperStore.add(BI);
                }
            }
        }

        @Override
        public void visit(MethodCallExpr md, Void arg){
            //メソッド呼び出しの引数に使われているかどうかを確認
            //ここでは何もせず、visitorに明け渡す
            AssignVisitorS visitor = new AssignVisitorS(classname, pathAbs, methodname, this.range, rangeStructure, structure, isAccessorCheckG, isAccessorCheckS, paramList, false);
            md.accept(visitor, null);

        }

        @Override
        public void visit(DoStmt md, Void arg){
            //do-whileのconditionにあたる部分の読み込み変数のチェックとブロック文内部の確認
            String structure = this.structure + "/Do-while";
            Range range = md.getRange().get();
            ArrayDeque<Range> rangeStructure = this.rangeStructure.clone();
            rangeStructure.add(range);
            //conditionチェック
            AssignVisitorS conditionVisitor = new AssignVisitorS(classname, pathAbs, methodname, this.range, this.rangeStructure, structure, isAccessorCheckG, isAccessorCheckS, paramList, false);
            md.getCondition().accept(conditionVisitor, null);

            //内部チェック
            InBlockSecondVisitorS bodyVisitor = new InBlockSecondVisitorS(classname, pathAbs, methodname, this.range, rangeStructure, structure, isAccessorCheckG, isAccessorCheckS, paramList);
            md.getBody().accept(bodyVisitor, null);

            BlockInfomation BI = new BlockInfomation("Do-while", "Do-while", false, structure, pathDir, range, this.rangeStructure);
            BI.setMemoryF(bodyVisitor.getLocal());
            for(BlockInfomation BILow : bodyVisitor.getBILower()){
                BI.setMemory(BILow);
            }
            BIUpperStore.add(BI);
            /*ArrayDeque<BlockInfomation> dequeBI = getBIfromClass(pathAbs, this.classname, this.rangeStructure, structure);
            BlockInfomation BIUpper = dequeBI.pollLast();
            BIUpper.setMemory(BI);
            BI = storeBItoClass(dequeBI, BIUpper);
            ClassInfomation CI = DataStore.memoryClass.getData(pathAbs, classname);
            CI.setMemory(BI);
            DataStore.memoryClass.setData(pathAbs, classname, CI);

             */
        }

        @Override
        public void visit(WhileStmt md, Void arg){
            //whileのconditionにあたる部分の読み込み変数のチェックとブロック文内部の確認
            String structure = this.structure + "/While";
            Range range = md.getRange().get();
            ArrayDeque<Range> rangeStructure = this.rangeStructure.clone();
            rangeStructure.add(range);
            //conditionチェック
            AssignVisitorS conditionVisitor = new AssignVisitorS(classname, pathAbs, methodname, this.range, this.rangeStructure, structure, isAccessorCheckG, isAccessorCheckS, paramList, false);
            md.getCondition().accept(conditionVisitor, null);

            //内部チェック
            InBlockSecondVisitorS bodyVisitor = new InBlockSecondVisitorS(classname, pathAbs,  methodname, range, rangeStructure, structure, isAccessorCheckG, isAccessorCheckS, paramList);
            md.getBody().accept(bodyVisitor, null);

            BlockInfomation BI = new BlockInfomation("While", "While", false, structure, pathDir, range, this.rangeStructure);
            BI.setMemoryF(bodyVisitor.getLocal());
            for(BlockInfomation BILow : bodyVisitor.getBILower()){
                BI.setMemory(BILow);
            }
            BIUpperStore.add(BI);
            /*ArrayDeque<BlockInfomation> dequeBI = getBIfromClass(pathAbs, this.classname, this.rangeStructure, structure);
            BlockInfomation BIUpper = dequeBI.pollLast();
            BIUpper.setMemory(BI);
            BI = storeBItoClass(dequeBI, BIUpper);
            ClassInfomation CI = DataStore.memoryClass.getData(pathAbs, classname);
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

            BlockInfomation BI = new BlockInfomation("For", "For", false, structure, pathDir, range, this.rangeStructure);


            //initialチェック
            for(Expression expression:md.getInitialization()) {
                InBlockSecondVisitorS initVisitor = new InBlockSecondVisitorS(classname, pathAbs, methodname, this.range, rangeStructure, structure, isAccessorCheckG, isAccessorCheckS, paramList);
                expression.accept(initVisitor, null);
                BI.setMemoryF(initVisitor.getLocal());
            }

            //System.out.println("initial ok");

            //compareチェック
            if(md.getCompare().isPresent()) {
                AssignVisitorS compareVisitor = new AssignVisitorS(classname, pathAbs, methodname, this.range, rangeStructure, structure, isAccessorCheckG, isAccessorCheckS, paramList, false);
                md.getCompare().get().accept(compareVisitor, null);
            }

            //System.out.println("compare ok");

            //updateチェック
            for(Expression expression:md.getUpdate()){
                AssignVisitorS updateVisitor = new AssignVisitorS(classname, pathAbs, methodname, this.range, rangeStructure, structure, isAccessorCheckG, isAccessorCheckS, paramList, false);
                expression.accept(updateVisitor, null);
            }

            //System.out.println("update ok");

            //内部チェック
            InBlockSecondVisitorS bodyVisitor = new InBlockSecondVisitorS(classname, pathAbs, methodname, this.range, rangeStructure, structure, isAccessorCheckG, isAccessorCheckS, paramList);
            md.getBody().accept(bodyVisitor, null);

            BI.setMemoryF(bodyVisitor.getLocal());
            for(BlockInfomation BILow : bodyVisitor.getBILower()){
                BI.setMemory(BILow);
            }
            BIUpperStore.add(BI);
            /*ArrayDeque<BlockInfomation> dequeBI = getBIfromClass(pathAbs, this.classname, this.rangeStructure, structure);
            BlockInfomation BIUpper = dequeBI.pollLast();
            BIUpper.setMemory(BI);
            BI = storeBItoClass(dequeBI, BIUpper);
            ClassInfomation CI = DataStore.memoryClass.getData(pathAbs, classname);
            CI.setMemory(BI);
            DataStore.memoryClass.setData(pathAbs, classname, CI);

             */

        }

        @Override
        public void visit(ForEachStmt md, Void arg){
            //foreachのiterableにあたる部分の読み込み変数のチェックとブロック文内部の確認
        }

        @Override
        public void visit(IfStmt md, Void arg){
            //ifのconditionにあたる部分の読み込み変数のチェックとブロック文内部の確認
            String structure = this.structure + "/if";
            Range range = md.getThenStmt().getRange().get();
            ArrayDeque<Range> rangeStructure = this.rangeStructure.clone();
            rangeStructure.add(range);
            //System.out.println("if-check:" + structure);
            //conditionチェック
            AssignVisitorS conditionVisitor = new AssignVisitorS(classname, pathAbs, methodname, this.range, this.rangeStructure, this.structure, isAccessorCheckG, isAccessorCheckS, paramList, false);
            md.getCondition().accept(conditionVisitor, null);

            //System.out.println("condition ok");

            InBlockSecondVisitorS thenBlockVisitor = new InBlockSecondVisitorS(classname, pathAbs, methodname, range, rangeStructure, structure, isAccessorCheckG, isAccessorCheckS, paramList);
            md.getThenStmt().accept(thenBlockVisitor, null);

            //System.out.println("if-then ok");

            BlockInfomation BI = new BlockInfomation("If", "If", false, structure, pathDir, range, this.rangeStructure);
            BI.setMemoryF(thenBlockVisitor.getLocal());
            for(BlockInfomation BILow : thenBlockVisitor.getBILower()){
                BI.setMemory(BILow);
            }
            BIUpperStore.add(BI);
            /*ArrayDeque<BlockInfomation> dequeBI = getBIfromClass(pathAbs, this.classname, this.rangeStructure, structure);
            BlockInfomation BIUpper = dequeBI.pollLast();
            BIUpper.setMemory(BI);
            BI = storeBItoClass(dequeBI, BIUpper);
            ClassInfomation CI = DataStore.memoryClass.getData(pathAbs, classname);
            CI.setMemory(BI);
            DataStore.memoryClass.setData(pathAbs, classname, CI);

             */


            //elseチェック
            if(md.getElseStmt().isPresent()){
                structure = this.structure + "/Else";
                range = md.getElseStmt().get().getRange().get();
                rangeStructure = this.rangeStructure.clone();
                rangeStructure.add(range);
                InBlockSecondVisitorS elseIfVisitor = new InBlockSecondVisitorS(classname, pathAbs, methodname, this.range, rangeStructure, structure, isAccessorCheckG, isAccessorCheckS, paramList);
                md.getElseStmt().get().accept(elseIfVisitor, null);
                //System.out.println("if-else ok");
                BI = new BlockInfomation("Else", "Else", false, structure, pathDir, range, this.rangeStructure);
                BI.setMemoryF(elseIfVisitor.getLocal());
                for(BlockInfomation BILow : elseIfVisitor.getBILower()){
                    BI.setMemory(BILow);
                }
                BIUpperStore.add(BI);
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
        public void visit(SwitchStmt md, Void arg){
            //switchのselectorにあたる部分の読み込み変数のチェックとブロック文内部の確認
            String structure = this.structure + "/Switch";
            Range range = md.getRange().get();
            ArrayDeque<Range> rangeStructure = this.rangeStructure.clone();
            rangeStructure.add(range);
            BlockInfomation BI = new BlockInfomation("Switch", "Switch", false, structure, pathDir, range, this.rangeStructure);

            AssignVisitorS selectorVisitor = new AssignVisitorS(classname, pathAbs, methodname, this.range, this.rangeStructure, this.structure, isAccessorCheckG, isAccessorCheckS, paramList, false);
            md.getSelector().accept(selectorVisitor, null);

            for(SwitchEntry entry:md.getEntries()){
                String structureLabel = structure + "/";
                StringBuilder label = new StringBuilder();
                for(Expression expression:entry.getLabels()) label.append(expression);
                if(entry.getLabels().size() == 0) label.append("default");
                structureLabel = structureLabel + label;
                Range rangeEntry = entry.getRange().get();
                ArrayDeque<Range> rangeStructureEntry = rangeStructure.clone();
                rangeStructureEntry.add(rangeEntry);
                BlockInfomation BIEntry = new BlockInfomation("SwitchEntry", "SwitchEntry", false, structureLabel, pathDir, rangeEntry, rangeStructure);
                for(Statement statement:entry.getStatements()){
                    InBlockSecondVisitorS statementVisitor = new InBlockSecondVisitorS(classname, pathAbs, methodname, range, rangeStructureEntry, structureLabel, isAccessorCheckG, isAccessorCheckS, paramList);
                    statement.accept(statementVisitor, null);
                    BIEntry.setMemoryF(statementVisitor.getLocal());
                }
                BI.setMemory(BIEntry);
            }
            BIUpperStore.add(BI);
            /*ArrayDeque<BlockInfomation> dequeBI = getBIfromClass(pathAbs, this.classname, this.rangeStructure, structure);
            BlockInfomation BIUpper = dequeBI.pollLast();
            BIUpper.setMemory(BI);
            BI = storeBItoClass(dequeBI, BIUpper);
            ClassInfomation CI = DataStore.memoryClass.getData(pathAbs, classname);
            CI.setMemory(BI);
            DataStore.memoryClass.setData(pathAbs, classname, CI);

             */

        }

        @Override
        public void visit(SwitchExpr md, Void arg){
            AssignVisitorS visitor = new AssignVisitorS(classname, pathAbs, methodname, this.range, rangeStructure, structure, isAccessorCheckG, isAccessorCheckS, paramList, false);
            md.accept(visitor, null);
            isFixableG = visitor.getIsFixableG();
            fieldname = visitor.getFieldname();
            isReturnNull = visitor.getIsReturnNull();
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

        public ArrayList<FieldInfomation> getLocal(){
            return this.localValueList;
        }

        public ArrayList<BlockInfomation> getBILower() {
            return this.BIUpperStore;
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
        private ArrayList<BlockInfomation> BIUpperStore = new ArrayList<>();

        public AssignVisitorS
        (String classname, String pathAbs, String contentsname, Range range, ArrayDeque<Range> rangeStructure, String structure,
         boolean isAccessorCheckG, boolean isAccessorCheckS, ArrayList<String> paramList, boolean flag, String value) {
            this.classname = classname;
            this.pathAbs = pathAbs;
            this.contentsname = contentsname;
            this.range = range;
            this.rangeStructure = rangeStructure;
            this.structure = structure;
            this.isAccessorCheckG = isAccessorCheckG;
            this.isAccessorCheckS = isAccessorCheckS;
            this.paramList = paramList;
            this.isWriteCheck = flag;
            this.nullCheckValue = value;
        }

        public AssignVisitorS
                (String classname, String pathAbs, String contentsname, Range range, ArrayDeque<Range> rangeStructure, String structure,
                 boolean isAccessorCheckG, boolean isAccessorCheckS, ArrayList<String> paramList, boolean flag) {
            this(classname, pathAbs, contentsname, range, rangeStructure, structure, isAccessorCheckG, isAccessorCheckS, paramList, flag, "");
        }

        public AssignVisitorS
                (String classname, String pathAbs, String contentsname, Range range,
                 ArrayDeque<Range> rangeStructure, String structure, boolean flag) {
            this(classname, pathAbs, contentsname, range, rangeStructure, structure, false, false, null, flag, "");
        }

        @Override
        public void visit(BlockStmt md, Void arg){
            for(Statement statement:md.getStatements()){
                AssignVisitorS visitor = new AssignVisitorS(classname, pathAbs, contentsname, range, rangeStructure, structure, isAccessorCheckG, isAccessorCheckS, paramList, isWriteCheck, nullCheckValue);
                statement.accept(visitor, null);
            }
        }

        @Override
        public void visit(SwitchExpr md, Void arg){
            //switchのselectorにあたる部分の読み込み変数のチェックとブロック文内部の確認
            String structure = this.structure + "/Switch";
            Range range = md.getRange().get();
            ArrayDeque<Range> rangeStructure = this.rangeStructure.clone();
            rangeStructure.add(range);
            BlockInfomation BI = new BlockInfomation("Switch", "Switch", false, structure, pathDir, range, this.rangeStructure);

            AssignVisitorS selectorVisitor = new AssignVisitorS(classname, pathAbs, contentsname, this.range, this.rangeStructure, this.structure, isAccessorCheckG, isAccessorCheckS, paramList, false);
            md.getSelector().accept(selectorVisitor, null);

            for(SwitchEntry entry:md.getEntries()){
                String structureLabel = structure + "/";
                StringBuilder label = new StringBuilder();
                for(Expression expression:entry.getLabels()) label.append(expression);
                if(entry.getLabels().size() == 0) label.append("default");
                structureLabel = structureLabel + label;
                Range rangeEntry = entry.getRange().get();
                ArrayDeque<Range> rangeStructureEntry = rangeStructure.clone();
                rangeStructureEntry.add(rangeEntry);
                BlockInfomation BIEntry = new BlockInfomation("SwitchEntry", "SwitchEntry", false, structureLabel, pathDir, rangeEntry, rangeStructure);
                for(Statement statement:entry.getStatements()){
                    InBlockSecondVisitorS statementVisitor = new InBlockSecondVisitorS(classname, pathAbs, contentsname, range, rangeStructureEntry, structureLabel, isAccessorCheckG, isAccessorCheckS, paramList);
                    statement.accept(statementVisitor, null);
                    BIEntry.setMemoryF(statementVisitor.getLocal());
                }
                BI.setMemory(BIEntry);
            }
            BIUpperStore.add(BI);
            /*ArrayDeque<BlockInfomation> dequeBI = getBIfromClass(pathAbs, this.classname, this.rangeStructure, structure);
            BlockInfomation BIUpper = dequeBI.pollLast();
            BIUpper.setMemory(BI);
            BI = storeBItoClass(dequeBI, BIUpper);
            ClassInfomation CI = DataStore.memoryClass.getData(pathAbs, classname);
            CI.setMemory(BI);
            DataStore.memoryClass.setData(pathAbs, classname, CI);

             */
        }

        @Override
        public void visit(BinaryExpr md, Void arg){
            //計算式に使われている変数の確認
            //最左分岐なので左側解析
            AssignVisitorS leftVisitor = new AssignVisitorS(classname, pathAbs, contentsname, range, rangeStructure, structure, isAccessorCheckG, isAccessorCheckS, paramList, isWriteCheck, nullCheckValue);
            md.getLeft().accept(leftVisitor, null);

            //右側解析
            AssignVisitorS rightVisitor = new AssignVisitorS(classname, pathAbs, contentsname, range, rangeStructure, structure, isAccessorCheckG, isAccessorCheckS, paramList, isWriteCheck, nullCheckValue);
            md.getRight().accept(rightVisitor, null);
        }

        @Override
        public void visit(UnaryExpr md, Void arg){
            AssignVisitorS visitor = new AssignVisitorS(classname, pathAbs, contentsname, range, rangeStructure, structure, isAccessorCheckG, isAccessorCheckS, paramList, false);
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
                    AssignVisitorS visitor = new AssignVisitorS(classname, pathAbs, contentsname, range, rangeStructure, structure, isAccessorCheckG, isAccessorCheckS, paramList, false);
                    expression.accept(visitor, null);
                }
            }
        }


        @Override
        public void visit(ObjectCreationExpr md, Void arg){
            //インスタンス生成に必要な引数の部分の解析


            //匿名クラスの解析
            if (md.getAnonymousClassBody().isPresent()) {
                isAnonymousAnalysis = true;
                BlockInfomation BI = new BlockInfomation(contentsname, "AnonymousClass", false, structure + "/" + contentsname, pathDir, range, this.rangeStructure);

                for (BodyDeclaration bd : md.getAnonymousClassBody().get()) {
                    if(bd.isFieldDeclaration()){
                        FieldVisitor visitor = new FieldVisitor(contentsname, pathAbs, structure, rangeStructure, true);
                        bd.accept(visitor, null);
                        BI.setMemoryF(visitor.getFIStoreList());
                    } else if(bd.isMethodDeclaration()){
                        MethodSecondVisitor visitor = new MethodSecondVisitor(contentsname, pathAbs, structure, rangeStructure, true);
                        bd.accept(visitor, null);
                        BI.setMemory(visitor.getMIStore());
                    } else if(bd.isConstructorDeclaration()){
                        ConstructorSecondVisitor visitor = new ConstructorSecondVisitor(contentsname, pathAbs, structure, rangeStructure, true);
                        bd.accept(visitor, null);
                        //BI.setMemory(visitor.getMIStore());
                    } else {//initializer
                        InitializerSecondVisitor visitor = new InitializerSecondVisitor(contentsname, pathAbs, structure, rangeStructure, true);
                        bd.accept(visitor, null);
                        //BI.setMemory(visitor.getMIStore());
                    }
                }
                /*ArrayDeque<BlockInfomation> dequeBI = getBIfromClass(pathAbs, this.classname, this.rangeStructure, structure);
                if(dequeBI.peekLast() != null) {
                    BlockInfomation BIUpper = dequeBI.pollLast();
                    BIUpper.setMemory(BI);
                    BI = storeBItoClass(dequeBI, BIUpper);
                }*/
                BIUpperStore.add(BI);
                /*ClassInfomation CI = DataStore.memoryClass.getData(pathAbs, classname);
                CI.setMemory(BI);
                DataStore.memoryClass.setData(pathAbs, classname, CI);*/
            }
        }

        @Override
        public void visit(LambdaExpr md, Void arg){
            isLambdaAnalysis = true;
            BlockInfomation BI = new BlockInfomation(contentsname, "Lambda", false, structure + "/" + contentsname, pathDir, range, this.rangeStructure);

            for(Parameter param:md.getParameters()){
                //paramList.add(param.getNameAsString());
                InBlockSecondVisitorS visitor = new InBlockSecondVisitorS(classname, pathAbs, contentsname, range, rangeStructure, structure, isAccessorCheckG, isAccessorCheckS, paramList);
                param.accept(visitor, null);
                BI.setMemoryF(visitor.getLocal());
            }

            ArrayList<FieldInfomation> localList = new ArrayList<>();
            InBlockSecondVisitorS visitor = new InBlockSecondVisitorS(classname, pathAbs, contentsname, range, rangeStructure, structure, isAccessorCheckG, isAccessorCheckS, paramList);
            md.getBody().accept(visitor, null);
            BI.setMemoryF(visitor.getLocal());
            localList.addAll(visitor.getLocal());
            /*ArrayDeque<BlockInfomation> dequeBI = getBIfromClass(pathAbs, this.classname, this.rangeStructure, structure);
            if(dequeBI.peekLast() != null) {
                BlockInfomation BIUpper = dequeBI.pollLast();
                BIUpper.setMemory(BI);
                BI = storeBItoClass(dequeBI, BIUpper);
            }*/
            ClassInfomation CI = DataStore.memoryClass.getData(pathAbs, classname);
            CI.setMemory(BI);
            DataStore.memoryClass.setData(pathAbs, classname, CI);
        }

        @Override
        public void visit(ArrayInitializerExpr md, Void arg){
            //要素ありの配列初期化の際の、要素の解析
        }

        @Override
        public void visit(NameExpr md, Void arg){
            //System.out.println("variable range:" + md.getRange().get());
            //変数(スコープなし)の確認
            if(isWriteCheck) variableWriteCheck(md.toString(), nullCheckValue);
            else variableReadCheck(md.toString());

            //System.out.println("variable check R/W finish");
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
            boolean flagThis = false;
            boolean flagSuper = false;
            //自クラス以外の使用を見る
            String[] scopes = target.split("\\.");
            //local,param,fieldの重複を探る。scopeの最も左の値を探る
            if(scopes[0].equals("this"))//最初がthisから始まるアクセス記述
                flagThis = true;
            else if(scopes[0].equals("super"))
                flagSuper = true;


            int countScope = 0;
            int indexBeforeArray = 0;
            String classname = this.classname;
            String valName = "";
            do {
                indexBeforeArray = scopes[countScope].indexOf("[");
                if (indexBeforeArray < 0) indexBeforeArray = scopes[countScope].length();
                valName = scopes[countScope].substring(0, indexBeforeArray);

                FieldInfomation FI = null;
                BlockInfomation BIstore = null;
                //System.out.println("analysis:" + valName);
                ArrayDeque<BlockInfomation> dequeBI = getBIfromClass(pathAbs, this.classname, this.rangeStructure, this.structure);
                Pair<ArrayDeque<BlockInfomation>, FieldInfomation> pairBF = DataStore.searchDefinitionLocal(valName, range, dequeBI);
                if(pairBF != null && !flagSuper && !flagThis){
                    FI = pairBF.b;
                    if (FI.formerRW.equals("R") && !FI.initializable)
                        FI.nullable = true;

                    if(FI.nullable) isReturnNull = true;

                    dequeBI = pairBF.a;
                    BlockInfomation BI = dequeBI.pollLast();
                    BI.setMemoryF(FI);

                    BIstore = storeBItoClass(dequeBI, BI);

                }

                if(BIstore != null){
                    ClassInfomation CI = DataStore.memoryClass.getData(pathAbs, this.classname);
                    CI.setMemory(BIstore);
                    DataStore.memoryClass.setData(pathAbs, this.classname, CI);
                }

                Triplets<String, ClassInfomation, FieldInfomation> tripletsCF = null;
                if(FI == null) tripletsCF = DataStore.searchDefinitionField(valName, classname, pathAbs, this.classname);

                if(tripletsCF != null){
                    FI = tripletsCF.getRightValue();
                    if (FI.formerRW.equals("") && !FI.initializable && !FI.assign) {
                        FI.formerRW = "R";
                        FI.nullable = true;
                    }

                    if(FI.nullable) isReturnNull = true;

                    if (isAccessorCheckG) {
                        isFixableG = true;
                        this.fieldname = valName;
                    }

                    ClassInfomation CI = tripletsCF.getCenterValue();
                    CI.setMemoryF(FI);
                    String pathAbs = tripletsCF.getLeftValue();
                    String name = CI.name;
                    DataStore.memoryClass.setData(pathAbs, name, CI);
                }

                countScope++;
                if (FI != null) classname = FI.type;
                if (classname.contains("-array"))
                    classname = classname.replace("-array", "");
            } while (countScope < scopes.length);
        }

        private void variableWriteCheck(String target, String value){
            boolean flagThis = false;
            boolean flagSuper = false;
            //自クラス以外の使用を見る
            String[] scopes = target.split("\\.");
            //local,param,fieldの重複を探る。scopeの最も左の値を探る
            if(scopes[0].equals("this"))//最初がthisから始まるアクセス記述
                flagThis = true;
            else if(scopes[0].equals("super"))
                flagSuper = true;


            int countScope = 0;
            int indexBeforeArray = 0;
            String classname = this.classname;
            String valName = "";
            do {
                indexBeforeArray = scopes[countScope].indexOf("[");
                if (indexBeforeArray < 0) indexBeforeArray = scopes[countScope].length();
                valName = scopes[countScope].substring(0, indexBeforeArray);

                FieldInfomation FI = null;
                BlockInfomation BIstore = null;
                ArrayDeque<BlockInfomation> dequeBI = getBIfromClass(pathAbs, this.classname, this.rangeStructure, this.structure);
                Pair<ArrayDeque<BlockInfomation>, FieldInfomation> pairBF = DataStore.searchDefinitionLocal(valName, range, dequeBI);
                if(pairBF != null && !flagSuper && !flagThis){
                    FI = pairBF.b;
                    FI.assign = true;

                    if (value.equals("null")) FI.nullable = true;

                    dequeBI = pairBF.a;
                    BlockInfomation BI = dequeBI.pollLast();
                    BI.setMemoryF(FI);

                    BIstore = storeBItoClass(dequeBI, BI);

                }

                if(BIstore != null){
                    ClassInfomation CI = DataStore.memoryClass.getData(pathAbs, this.classname);
                    CI.setMemory(BIstore);
                    DataStore.memoryClass.setData(pathAbs, this.classname, CI);
                }

                Triplets<String, ClassInfomation, FieldInfomation> tripletsCF = DataStore.searchDefinitionField(valName, classname, pathAbs, this.classname);

                if(tripletsCF != null){
                    FI = tripletsCF.getRightValue();
                    FI.assign = true;
                    if (FI.formerRW.equals("") && !FI.initializable) {
                        FI.formerRW = "W";
                    }

                    if (value.equals("null")) FI.nullable = true;

                    if (isAccessorCheckS) {
                        for (String paramName : paramList) {
                            if (flagThis) {
                                if (scopes[1].equals(paramName) && value.equals(paramName)) {
                                    isFixableS = true;
                                    this.fieldname = valName;
                                }
                            } else {
                                if (scopes[0].equals(paramName) && value.equals(paramName)) {
                                    isFixableS = true;
                                    this.fieldname = valName;
                                }
                            }
                        }
                    }

                    ClassInfomation CI = tripletsCF.getCenterValue();
                    CI.setMemoryF(FI);
                    String pathAbs = tripletsCF.getLeftValue();
                    String name = CI.name;
                    DataStore.memoryClass.setData(pathAbs, name, CI);
                }

                countScope++;
                if (FI != null) classname = FI.type;
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

        public boolean isAnonymousAnalysis(){
            return this.isAnonymousAnalysis;
        }

        public boolean isLambdaAnalysis(){
            return this.isLambdaAnalysis;
        }

        public ArrayList<BlockInfomation> getBILower() {
            return this.BIUpperStore;
        }

    }

    //クラスから現在の場所に至るまでのBlockInfomationのdequeを作り出すクラス
    private static ArrayDeque<BlockInfomation> getBIfromClass(String pathAbs, String classname, ArrayDeque<Range> rangeStructure, String blockStructure){
        ArrayDeque<BlockInfomation> dequeBI = new ArrayDeque<>();
        ArrayDeque<Range> ranges = rangeStructure.clone();
        String[] structureSplit = blockStructure.split("/");
        System.out.println("classname:" + classname + " structure:" + blockStructure + " range:" + ranges);

        //位置合わせの場所
        StringBuilder sb = new StringBuilder("/" + structureSplit[1]);
        int numStructureLayer = 2;

        ClassInfomation CI = DataStore.memoryClass.getData(pathAbs, classname);
        ArrayDeque<Range> rangesCI = CI.rangeStructure.clone();

        //インナークラス用のrange,structure合わせ
        //クラスのrange位置あわせ
        boolean flag = false;
        while(rangesCI.peek() != null){
            flag = true;
            ranges.poll();
            rangesCI.poll();
            /*System.out.println("ranges:" + ranges);
            System.out.println("rangesCI:" + rangesCI);*/
        }

        //クラスのstructure位置あわせ
        while(!CI.blockStructure.equals(sb.toString()) && flag) {
            sb.append("/").append(structureSplit[numStructureLayer]);
            numStructureLayer++;
            /*System.out.println("sb:" + sb.toString());
            System.out.println("aim:" + CI.blockStructure);*/
        }

        System.out.println("sb:" + sb + " num:" + numStructureLayer + " len:" + structureSplit.length);
        //クラス直下ではない時
        if(structureSplit.length > 2) {
            ranges.poll();//クラスの分rangeデータを破棄
            sb.append("/").append(structureSplit[numStructureLayer]);//クラスの下部にアクセス
            numStructureLayer++;
            BlockInfomation BI = CI.getMemoryData(sb.toString(), ranges.poll());
            dequeBI.add(BI);

            //System.out.println("begin/structure:" + sb.toString() + " range:" + ranges);

            while (ranges.peek() != null) {
                sb.append("/").append(structureSplit[numStructureLayer]);
                numStructureLayer++;
                Range range = ranges.poll();
                //System.out.println("structure:" + sb.toString() + " range:" + range);
                BI = BI.getMemoryData(sb.toString(), range);
                //System.out.println("name:" + BI.name + " structure:" + BI.blockStructure + " ranges:" + ranges);
                dequeBI.add(BI);
            }
        }


        return dequeBI;
    }

    private static BlockInfomation storeBItoClass(ArrayDeque<BlockInfomation> deque, BlockInfomation BI){
        ArrayDeque<BlockInfomation> dequeBI = deque;
        BlockInfomation BInow = BI;

        while(dequeBI.peek() != null){
            BlockInfomation BIOuterScope = dequeBI.pollLast();
            BIOuterScope.setMemory(BInow);
            BInow = BIOuterScope;
        }

        return BInow;
    }

    private static void pathLink(String pathAbs, String pathDir){
        if(DataStore.memoryPathAbs.get(pathDir) == null) DataStore.memoryPathAbs.put(pathDir, new ArrayList<>());
        ArrayList<String> arrayList = DataStore.memoryPathAbs.get(pathDir);
        arrayList.add(pathAbs);
        DataStore.memoryPathAbs.put(pathDir, arrayList);
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