import java.io.*;
import java.util.ArrayList;

public class ConvertOutputer {

    public static String convertDirName = "/outkt";

    public static void ConvertAction(String rootPath, String path){
        try{
            CreateDir(rootPath + convertDirName);
            System.setOut(new PrintStream(ConvertOutputer.CreateConvertFile(rootPath, path)));
        }catch (IOException e){
            System.out.println("convert failed:" + e.getMessage());
        }
    }

    public static void CreateDir(String path){
        System.setOut(System.out);
        File file = new File(path);
        if(!file.exists()){//ディレクトリが存在しない時
            file.mkdir();
            System.out.println("directory created");
        } else {//ディレクトリが既にある時
            //System.out.println("directory already exist");
        }
    }

    public static FileOutputStream CreateConvertFile(String rootPath, String path) throws IOException {
        String underConvertPath = path.replace(rootPath, rootPath + convertDirName);
        String createPath = underConvertPath.replace(".java",  ".kt");
        File file = new File(createPath);
        if(!file.exists()) file.createNewFile();
        FileOutputStream fos = new FileOutputStream(file);
        return fos;
    }

    public static ArrayList<String> dumpFile(String path, String pathKt, int level){
        String convertPath = pathKt;
        if(level == 0){
            convertPath = convertPath + convertDirName;
            CreateDir(convertPath);
        }
        // ファイル一覧取得
        File[] files = new File(path).listFiles();
        ArrayList<String> filePathList = new ArrayList<>();
        if(files == null){
            return new ArrayList<>();
        }
        for (File tmpFile : files) {
            String name = tmpFile.getName();
            if(tmpFile.isDirectory()){// ディレクトリの場合
                if(!name.equals("outkt")) {
                    String newPath = path + "/" + name;
                    String newConvertPath = convertPath + "/" + name;
                    //CreateDir(newConvertPath);
                    ArrayList<String> tmpList = dumpFile(newPath, newConvertPath, level + 1);
                    filePathList.addAll(tmpList);
                }
            }else{// ファイルの場合
                //if(name.substring(tmpFile.name.lastIndexOf(".") + 1).equals("java"))
                    filePathList.add(path + "/" + name);
            }
        }
        return filePathList;
    }

}
