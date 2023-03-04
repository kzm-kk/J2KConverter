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
import com.github.javaparser.utils.Pair;
import com.github.javaparser.utils.SourceRoot;
import com.github.javaparser.utils.SourceZip;
import com.google.common.base.Charsets;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

//ライブラリ解析テスト用のクラス
public class LibraryTry {

    public static void main(String[] args) throws Exception {

        String path_root = "/Library/Java/JavaVirtualMachines/jdk-15.jdk/Contents/Home/lib/src.zip";
        if(args.length == 0);
        else path_root = args[0];
        SourceZip rootzip = new SourceZip(Paths.get(path_root));
        SourceRoot root = new SourceRoot(Paths.get("/Library/Java/JavaVirtualMachines/jdk-15.jdk/Contents/Home/jmods"));
        List<ParseResult<CompilationUnit>> results = root.tryToParse("");

        ZipFile zipFile = new ZipFile(new File(path_root));
        zipFile.stream().forEach(entry -> {
            String[] spliter = entry.getName().split("/");
            int size = spliter.length;
            if(spliter[size - 1].equals("Path.java")) {
                System.out.println(entry);
                try (InputStream is = zipFile.getInputStream(entry)) {
                    //read(is);

                    CompilationUnit unit = StaticJavaParser.parse(is);
                    System.out.println(unit.getStorage().isPresent());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        //decode(new File(path_root));
        //readZipFile(path_root);
    }

    public static void decode(File file) throws Exception {
        byte[] buf = new byte[1024];
        String str = null;
        ByteArrayOutputStream streamBuilder = null;
        int bytesRead;

        ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(file.getPath())), StandardCharsets.UTF_8);
        ZipFile zf = new ZipFile(file);
        for (Enumeration<? extends ZipEntry> e = zf.entries(); e.hasMoreElements();) {
            ZipEntry entry = e.nextElement();
            //System.out.print(entry.getName());
            if (entry.isDirectory());
            else {
                Path zipPath = Paths.get(file.getPath());
                try (FileSystem fs = FileSystems.newFileSystem(zipPath, ClassLoader.getSystemClassLoader())) {
                    Path path = fs.getPath(entry.getName());
                    try (InputStream is = Files.newInputStream(path)) {
                        if(entry.getName().equals("classes/java/math/BigDecimal.class")) {
                            for (;;) {
                                int len = is.read(buf);
                                if (len < 0) break;
                                //bufを使って処理
                            }
                            JavaParser parser = new JavaParser();
                            is.close();
                            ParseResult<CompilationUnit> result = parser.parse(is);
                            System.out.println(result.getResult().isPresent());
                        }
                    }
                }
                /*System.out.println();
                InputStream is = zf.getInputStream(entry);
                str = new String(zis.readAllBytes());
                int d;
                if(entry.getName().equals("classes/java/math/BigDecimal.class")) {
                    while ((d = is.read()) != -1) {
                        System.out.printf("%c", d);
                    }
                }

                //とりあえず出力しておく
                //System.out.println(entry.toString());//new String(str.getBytes("UTF-8"), "UTF-8"));
                is.close();

                 */
            }

        }
        zf.close();
    }
    public static void readZipFile(String filePath) throws Exception {
        java.util.zip.ZipFile zf = new java.util.zip.ZipFile(filePath);
        InputStream in = new BufferedInputStream(new FileInputStream(filePath));
        ZipInputStream zin = new ZipInputStream(in);
        ZipEntry ze;
        while ((ze = zin.getNextEntry()) != null) {
            long size = ze.getSize();
            if (size > 0) {
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(zf.getInputStream(ze)));
                String line;
                while ((line = br.readLine()) != null) {
                    System.out.println(line);
                }
                br.close();
            }
            System.out.println();
        }
        zin.closeEntry();
    }

    public static void process(final File file) throws IOException {
        final ZipInputStream zipInStream = new ZipInputStream(
                new BufferedInputStream(new FileInputStream(file)));
        try {
            for (;;) {
                final ZipEntry entry = zipInStream.getNextEntry();
                if (entry == null) break;
                if (entry.isDirectory()) {
                    System.out.println(" ディレクトリ名: [" +
                            entry.getName() + "]");
                } else {
                    System.out.println(" ファイル名: [" +
                            entry.getName() + "]");
                    /* ファイルデータの読み込み */
                    final ByteArrayOutputStream outStream
                            = new ByteArrayOutputStream();
                    for (;;) {
                        int iRead = zipInStream.read();
                        if (iRead < 0) break;
                        outStream.write(iRead);
                    }
                    outStream.flush();
                    outStream.close();
                    System.out.println(" 内容: ["
                            + new String(outStream.toByteArray()) +
                            "]");
                }
                zipInStream.closeEntry();
            }
        } finally {
            zipInStream.close();
        }
    }
}
