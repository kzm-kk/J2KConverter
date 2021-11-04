import com.github.javaparser.ParseResult;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.comments.LineComment;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.utils.SourceRoot;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException {

        File file = new File("./src/main/java/numer0n.java");
        CompilationUnit cu = StaticJavaParser.parse(file);
        SourceRoot root = new SourceRoot(Paths.get("./src/main/java"));
        List<ParseResult<CompilationUnit>> cu2 = root.tryToParse("");

        VoidVisitor<?> visitor = new ASTEditVisitor();
        cu.accept(visitor, null);
        visitor = new ClassorImportVisitor();
        cu.accept(visitor, null);
        System.out.println(cu);
        //Optional<CompilationUnit> cu3 = cu2.get(1).getResult();
        /*ProjectRoot projectRoot =
                new SymbolSolverCollectionStrategy()
                        .collect(Paths.get("./src/main/java"));*/

        //System.out.println(cu3.get());
        //System.out.println(projectRoot.toString());
    }

    private static class ASTEditVisitor extends VoidVisitorAdapter<Void>{
        HashMap<String, Boolean> var_map;//String:name, Boolean:isnull

        @Override
        public void visit(FieldDeclaration md, Void arg){

        }

        @Override
        public void visit(VariableDeclarationExpr md, Void arg){

        }

        @Override
        public void visit(VariableDeclarator md, Void arg){

        }

        @Override
        public void visit(MethodCallExpr md, Void arg){

        }

    }

    private static class ClassorImportVisitor extends VoidVisitorAdapter<Void>{

        @Override
        public void visit(ClassOrInterfaceDeclaration md, Void arg){
            super.visit(md, arg);
            String mod = md.getChildNodes().get(0).toString();
            System.out.println(mod + "class " + md.getName() + "{");
            VoidVisitor<?> visitor = new SomeVisitor();
            md.accept(visitor, null);
            System.out.println("}");
        }

        @Override
        public void visit(ImportDeclaration md, Void arg){
            super.visit(md, arg);
            System.out.println("Import: " + md.toString());
        }
    }

    private static class FieldVisitor extends VoidVisitorAdapter<Void>{

        @Override
        public void visit(Modifier md, Void arg){
            super.visit(md,arg);
            System.out.print(md.toString());
        }

        @Override
        public void visit(VariableDeclarator md, Void arg) {

            super.visit(md, arg);
            System.out.print("var " + md.getName());
            String value =  "";
            if(md.getInitializer().get() != null)
            value = md.getInitializer().get().toString();
            if(value != "Optional.empty" || value != "")System.out.println(" = " + value);
            else System.out.println(":" + md.getType() + "?");
        }
    }


    private static class SomeVisitor extends VoidVisitorAdapter<Void> {

        @Override
        public void visit(FieldDeclaration md, Void arg) {
            super.visit(md, arg);
            VoidVisitor visitor = new FieldVisitor();
            md.accept(visitor, null);
        }

        @Override
        public void visit(MethodDeclaration md, Void arg) {
            super.visit(md, arg);
            String line_param = "";
            int count = md.getParameters().size();
            if(count > 0) {
                for (int i = 0; count > i; i++) {
                    String param = md.getParameter(i).getName().toString();
                    String type_param = md.getParameter(i).getType().toString();
                    type_param = type_param.substring(0,1).toUpperCase()
                            + type_param.substring(1).toLowerCase();
                    param = param.concat(":").concat(type_param);
                    if (i != 0) line_param = line_param.concat(", ");
                    line_param = line_param.concat(param);
                }
            }
            String name = md.getName().getIdentifier();
            String type = md.getType().toString();
            String mod = md.getModifiers().toString()
                    .replace("[", "").replace(" ]", "");
            if(type.equals("void"))System.out.print(mod + " fun " + name
                    + "(" + line_param + ")");
            else System.out.print(mod + " fun " + name + "(" + line_param
                    + "):" + type + "?");
            VoidVisitor<?> visitor = new MethodVisitor();
            md.accept(visitor, null);
        }

        @Override
        public void visit(MarkerAnnotationExpr md, Void arg){
            super.visit(md, arg);
            System.out.println(md.toString());
        }

        @Override
        public void visit(LineComment md, Void arg){
            super.visit(md, arg);
            System.out.println(md.toString());
        }
    }

    private static class MethodVisitor extends VoidVisitorAdapter<Void>{

        @Override
        public void visit(BlockStmt md, Void arg){
            System.out.println("{");
            super.visit(md, arg);
            System.out.println("}");
        }

        @Override
        public void visit(ExpressionStmt md, Void arg){
            super.visit(md, arg);
            VoidVisitor<?> visitor = new ExpressionVisitor();
            md.accept(visitor, null);
        }

        @Override
        public void visit(IfStmt md, Void arg){
            //super.visit(md, arg);
            System.out.print("if(");
            VoidVisitor<?> visitor = new MethodCallVisitor();
            md.accept(visitor, null);
            //super.visit(md, arg);
            System.out.print(")");
            super.visit(md, arg);
            //visitor = new MethodVisitor();
            //md.accept(visitor,null);
        }

        @Override
        public void visit(ForStmt md, Void arg){
            //super.visit(md, arg);
            System.out.print("for(");
            VoidVisitor<?> visitor = new CompareVisitor();
            //md.accept(visitor, null)
            md.getInitialization().accept(visitor, null);
            md.getCompare().get().accept(visitor, null);
            md.getUpdate().accept(visitor,null);
            System.out.print(")");
            super.visit(md, arg);
            //visitor = new MethodVisitor();
            //md.accept(visitor,null);
        }

        /*@Override
        public void visit(UnaryExpr md, Void arg){
            super.visit(md, arg);
            if(md.getOperator() != null) System.out.print(md.getOperator().toString());
            System.out.println(md.toString());
        }*/
    }

    private static class ExpressionVisitor extends VoidVisitorAdapter<Void> {

        @Override
        public void visit(MethodCallExpr md, Void arg){
            //super.visit(md, arg);
            String scope = md.getScope().get().toString();
            String name = md.getName().getIdentifier();
            String content = "";
            if(md.getArguments().size() != 0)content = md.getArguments().get(0).toString();

            /*VoidVisitor<?> visitor = new MethodCallVisitor();
            md.accept(visitor, null);
            System.out.println(md.toString());*/
            /*System.out.print(scope + "." + name +"(");
            super.visit(md, arg);
            System.out.println(")");*/
            System.out.println(scope + "." + name + "(" + content + ")");
            //System.out.println(md.toString());
        }

        @Override
        public void visit(VariableDeclarationExpr md, Void arg){
            super.visit(md, arg);
            //VoidVisitor<?> visitor = new MethodCallVisitor();
            //md.accept(visitor, null);
        }

        @Override
        public void visit(AssignExpr md, Void arg){
            super.visit(md, arg);
            System.out.println(md.toString());
            if(md.getTarget().toString().equals(md.getValue().toString())){
                Expression tmp = StaticJavaParser.parseExpression("this."+ md.getTarget().toString());
                md.setTarget(tmp);
            }
        }

        @Override
        public void visit(UnaryExpr md, Void arg){
            super.visit(md, arg);
            System.out.println(md.toString());
        }

        /*@Override
        public void visit(BinaryExpr md, Void arg){
            super.visit(md, arg);
            System.out.print(md.toString());
        }

        @Override
        public void visit(StringLiteralExpr md, Void arg){
            super.visit(md, arg);
            System.out.print(md.toString());
        }*/

    }

    private static class MethodCallVisitor extends VoidVisitorAdapter<Void> {
        @Override
        public void visit(FieldAccessExpr md, Void arg){
            super.visit(md, arg);
            System.out.print("");
        }

        @Override
        public void visit(BinaryExpr md, Void arg){
            super.visit(md, arg);
            System.out.print(md.toString());
        }

        @Override
        public void visit(VariableDeclarationExpr md, Void arg){
            super.visit(md, arg);
            System.out.print(md.toString());
        }

        @Override
        public void visit(VariableDeclarator md, Void arg) {
            super.visit(md, arg);
            System.out.print("var " + md.getName());
            String value = md.getInitializer().get().toString();
            if(value != "Optional.empty")System.out.println(" = " + value);
            else System.out.println(":" + md.getType() + "?");
        }
    }

    private static class CompareVisitor extends VoidVisitorAdapter<Void> {

        int initial;
        String literal;
        int step;
        int last;
        public void initialize(){
            initial = 0;
            literal = "";
            step = 1;
            last = 1;
        }
        //HashMap<String, > map = new HashMap();

        @Override
        public void visit(AssignExpr md, Void arg){
            super.visit(md, arg);
            String operator = md.getOperator().toString();
            String target = md.getTarget().toString();
            int value = Integer.parseInt(md.getValue().toString());
            System.out.print(md.toString());
        }

        @Override
        public void visit(UnaryExpr md, Void arg){
            super.visit(md, arg);
            if(md.getOperator() != null) System.out.print(md.getOperator().toString());
            System.out.print(md.toString());
        }

        @Override
        public void visit(BinaryExpr md, Void arg){
            super.visit(md, arg);
            String operator = md.getOperator().toString();
            System.out.print(md.getOperator().toString());
            System.out.print(md.toString());
        }

        @Override
        public void visit(StringLiteralExpr md, Void arg){
            super.visit(md, arg);
            System.out.print(md.toString());
        }

        @Override
        public void visit(VariableDeclarator md, Void arg){

        }

    }

}

