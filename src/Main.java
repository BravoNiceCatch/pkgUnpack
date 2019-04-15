import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class Main {
    static String filePath = "C:/Users/13574/Desktop/scene.pkg";
    static FileInputStream fis;
    static String version = "";
    //文件总数
    static int fileCount;
    static long ds_ptr = 0;
    static String extDtr;
    static PKGFile[] pkgFiles;

    public static void main(String[] args) throws IOException {
        if(args.length!=1){
            System.out.println("语法不正确");
            return;
        }else {
            filePath=args[0];
        }
        prepareFile(filePath);
        filePath = filePath.replace("\\", "/");
        extDtr = filePath.substring(0, filePath.lastIndexOf("/") + 1) + "ext_" + filePath.substring(filePath.lastIndexOf("/") + 1, filePath.lastIndexOf("."));
        File tempDir = new File(extDtr);
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        } else {
            delFolder(tempDir.getAbsolutePath());
            tempDir.mkdirs();
        }
        readFiles();
        System.out.printf("文件总数：%d\n", fileCount);
        System.out.println("文件名\t文件偏移量\t文件大小\n--------------------------------------------------");
        for (PKGFile pkgFile : pkgFiles) {
            System.out.printf("%s\t%d\t%d\n", pkgFile.name, pkgFile.offset, pkgFile.size);
        }
        fis.close();
        System.out.println("\n开始提取");
        for (PKGFile pkgFile : pkgFiles) {
            getFile(pkgFile);
        }
    }

    /**
     * 初始化文件
     *
     * @param filePath 文件路径
     * @throws IOException
     */
    static void prepareFile(String filePath) throws IOException {
        fis = new FileInputStream(filePath);
        readHeader();
    }

    /**
     * 读取文件头
     *
     * @throws IOException
     */
    static void readHeader() throws IOException {
        version = readStr();
        if (!(version.equals("PKGV0001")||version.equals("PKGV0002"))) {
            System.out.println("文件不支持");
            System.exit(0);
        } else {
            System.out.println("文件版本：PKGV0001");
            byte[] bytes = new byte[4];
            fis.read(bytes, 0, 4);
            fileCount = (int) getSize(bytes);
            ds_ptr += 4;
        }
    }

    /**
     * 读取文件名
     *
     * @return 文件名
     * @throws IOException
     */
    static String readStr() throws IOException {
        int size = 0;
        byte[] bytes = new byte[4];
        fis.read(bytes, 0, 4);
        ds_ptr += 4;
        size = (int) getSize(bytes);
        byte[] bytes1 = new byte[size];
        fis.read(bytes1, 0, size);
        ds_ptr += size;
        return new String(bytes1);
    }

    /**
     * 删除文件夹下所有文件
     *
     * @param path 文件夹路径
     * @return
     */
    public static boolean delAllFile(String path) {
        boolean flag = false;
        File file = new File(path);
        if (!file.exists()) {
            return flag;
        }
        if (!file.isDirectory()) {
            return flag;
        }
        String[] tempList = file.list();
        File temp = null;
        for (int i = 0; i < tempList.length; i++) {
            if (path.endsWith(File.separator)) {
                temp = new File(path + tempList[i]);
            } else {
                temp = new File(path + File.separator + tempList[i]);
            }
            if (temp.isFile()) {
                temp.delete();
            }
            if (temp.isDirectory()) {
                delAllFile(path + "/" + tempList[i]);//先删除文件夹里面的文件
                delFolder(path + "/" + tempList[i]);//再删除空文件夹
                flag = true;
            }
        }
        return flag;
    }

    /**
     * 删除文件夹
     *
     * @param folderPath 文件夹路径
     */
    public static void delFolder(String folderPath) {
        try {
            delAllFile(folderPath); //删除完里面所有内容
            String filePath = folderPath;
            filePath = filePath.toString();
            java.io.File myFilePath = new java.io.File(filePath);
            myFilePath.delete(); //删除空文件夹
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取每一个文件的信息
     *
     * @throws IOException
     */
    static void readFiles() throws IOException {
        pkgFiles = new PKGFile[fileCount];
        for (int i = 0; i < fileCount; i++) {
            String name = readStr();
            byte[] bytes = new byte[4];
            fis.read(bytes, 0, 4);
            ds_ptr += 4;
            long offset = getSize(bytes);
            fis.read(bytes, 0, 4);
            ds_ptr += 4;
            long size = getSize(bytes);
            pkgFiles[i] = new PKGFile(name, offset, size);
        }
        System.out.println(String.format("总偏移量：%d\n", ds_ptr));
    }

    /**
     * 获取文件大小和偏移量
     *
     * @param bytes 读取到的数组
     * @return 大小或偏移量
     */
    static long getSize(byte[] bytes) {
        String str = "";
        for (int i = bytes.length - 1; i >= 0; i--) {
            byte b = bytes[i];
            int i1 = b < 0 ? b + 256 : b;
            String temp = Integer.toHexString(i1);
            str += temp.length() < 2 ? "0" + temp : temp;
        }
        return Long.valueOf(str, 16);
    }

    static void getFile(PKGFile pkgFile) throws IOException {
        String name = pkgFile.name;
        long offset = pkgFile.offset;
        long size = pkgFile.size;
        int position = name.lastIndexOf("/");
        String folder = position == -1 ? "" : name.substring(0, position);
        File folder1 = new File(extDtr + "/" + folder);
        if (!folder1.exists()) {
            folder1.mkdirs();
        }
        FileOutputStream fos = new FileOutputStream(extDtr + "/" + name);
        FileInputStream fis = new FileInputStream(filePath);
        byte[] buffer = new byte[4096];
        fis.skip(offset + ds_ptr);
        long times = size / 4096;
        for (int i = 0; i < times; i++) {
            fis.read(buffer, 0, 4096);
            fos.write(buffer, 0, 4096);
        }
        int remain = (int) size % 4096;
        fis.read(buffer, 0, remain);
        fos.write(buffer, 0, remain);
        System.out.printf("已提取\t%s\n", name);
        fis.close();
        fos.close();
    }
}

class PKGFile {
    String name;
    long offset;
    long size;

    public PKGFile(String name, long offset, long size) {
        this.name = name;
        this.offset = offset;
        this.size = size;
    }
}