package com.unicool.recording.util;

import android.os.Environment;
import android.os.StatFs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/*
 *  @项目名：  Nor-Android 
 *  @包名：    com.uniool.recording.service
 *  @文件名:   FileUtil
 *  @创建者:   cjf
 *  @创建时间:  2017/9/21 19:56
 *  @描述：    http://blog.csdn.net/zqs62761130/article/details/42464785
 */
public class FileUtil {
    public final static long K = 1024;
    public final static long M = K * 1024;
    public final static long G = M * 1024;
    // 外置存储卡默认预警临界值
    public static final long THRESHOLD_WARNING_SPACE = 200 * M;
    // 保存文件时所需的最小空间的默认值
    public static final long THRESHOLD_MIN_SPCAE = 6 * M;

    private FileUtil() {
    }

    public static boolean delFiles(String path) {
        File dir = new File(path);
        if (!isFileExists(dir)) return false;
        if (dir.isFile()) {
            return dir.delete() && !dir.exists();
        } else {
            for (File file : dir.listFiles()) {
                if (file.isFile()) {
                    if (!file.delete() || file.exists()) {
                        delFiles(file.getAbsolutePath()); //删除失败再删一次
                    }
                } else {
                    delFiles(file.getAbsolutePath());
                }
            }
        }
        return dir.delete() && !dir.exists() || delFiles(dir.getAbsolutePath()); //删除目录本身，
    }

    public static boolean isFileExists(String path) {
        try {
            File f = new File(path);
            return f.exists();
        } catch (Exception e) { //SecurityException
            e.printStackTrace();
            return false;
        }
    }

    public static boolean isFileExists(File f) {
        try {
            return f.exists();
        } catch (Exception e) { //SecurityException
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 删除最早创建的一个文件或不为空的整个二级文件夹
     *
     * @param byPrimaryDirectory true：删除整个二级文件夹，false：一次只删除一个文件
     */
    public static boolean deleteOldestFiles(File parentDir, boolean byPrimaryDirectory) {
        File oldestFile = getOldestFiles(parentDir, false, true);
        if (oldestFile == null) return false;
        if (oldestFile.isFile()) {
            return oldestFile.delete();
        } else {
            if (oldestFile.list().length == 0) {
                return oldestFile.delete() && deleteOldestFiles(parentDir, byPrimaryDirectory);
                //return oldestFile.delete();
            }
            if (byPrimaryDirectory) {
                return delFiles(oldestFile.getAbsolutePath()); //按一级目录进行删除
            } else {
                return deleteOldestFiles(oldestFile, false); //按单个文件进行删除
            }
        }
    }

    public static File getOldestFiles(File parentDir, boolean isNewest, final boolean isSortByName) {
        if (!isFileExists(parentDir)) return null;
        if (parentDir.isFile()) return parentDir;
        File[] files = parentDir.listFiles();
        if (files == null || files.length == 0) return null;
        Arrays.sort(files, new Comparator<File>() {
            @Override
            public int compare(File f1, File f2) {
                if (isSortByName) {
                    String s1 = f1.getName();
                    s1 = s1.lastIndexOf(".") > 0 ? s1.substring(0, s1.indexOf(".")) : s1;
                    String s2 = f2.getName();
                    s2 = s2.lastIndexOf(".") > 0 ? s2.substring(0, s2.indexOf(".")) : s2;
                    try {
                        return Long.valueOf(s1).compareTo(Long.valueOf(s2));
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                }
                return Long.valueOf(f1.lastModified()).compareTo(f2.lastModified());
            }
        });
        return isNewest ? files[files.length - 1] : files[0];
    }

    public static long getFilesSize(File f) {
        if (!isFileExists(f)) return -1L;
        if (f.isFile()) {
            return f.length();
        } else {
            long size = 0;
            for (File file : f.listFiles()) {
                if (file.isFile()) {
                    size += file.length();
                } else {
                    size += getFilesSize(file.getAbsolutePath());
                }
            }
            return size;
        }
    }

    public static long getFilesSize(String path) {
        return getFilesSize(new File(path));
    }

    public static File makeDirectory(String path) {
        File file = new File(path);
        boolean exist = file.exists();
        if (!exist) {
            exist = file.mkdirs();
        }
        return exist ? file : null;
    }

    public static File makeFile(String filePath) {
        File file = new File(filePath);
        boolean exist = file.exists();
        if (!exist) {
            try {
                exist = file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return exist ? file : null;
    }

    public static long getSDFreeSize(String path) {
        if (!isFileExists(path)) return -1L;
        //取得内置SD卡文件路径
        // File path = Environment.getExternalStorageDirectory();
        StatFs sf = new StatFs(path);
        //获取单个数据块的大小(Byte)
        long blockSize = sf.getBlockSizeLong();
        //空闲的数据块的数量
        long freeBlocks = sf.getAvailableBlocksLong();
        //返回SD卡空闲大小
        return freeBlocks * blockSize; //单位Byte
//        return (freeBlocks * blockSize) / 1024; //单位KB
//        return (freeBlocks * blockSize) / 1024 / 1024; //单位MB
    }

    /**
     * 移动文件，移动成功后删除原文件，
     *
     * @param srcDir
     * @param dstDir
     * @param file_P_N 保持文件的后面部分路径不变
     * @return
     */
    public static boolean moveFiles(String srcDir, String dstDir, String file_P_N) {
        File origin = new File(srcDir + "/" + file_P_N);
        if (!isFileExists(origin)) return false;
        try {
            if (origin.isFile()) {
                boolean b = copyFile(origin, new File(dstDir + file_P_N));
                if (b) origin.delete();
            } else {
                for (File of : origin.listFiles()) {
                    if (of.isFile()) {
                        boolean b = copyFile(of, new File(dstDir + "/" + origin.getName() + "/" + of.getName()));
                        if (b) of.delete();
                    } else {
                        moveFiles(of.getAbsolutePath(), dstDir + "/" + origin.getName(), "/" + of.getName());
                    }
                }
            }
            return origin.delete(); //文件夹
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean copyFile(File sourceFile, File destFile) throws IOException {
        if (!isFileExists(sourceFile)) return false;
        if (!isFileExists(destFile.getParentFile())) {
            boolean mkdirs = destFile.getParentFile().mkdirs();
            if (!mkdirs) return false;
        }
        if (!destFile.exists()) {
            boolean newFile = destFile.createNewFile();
            if (!newFile) return false;
        }
        FileChannel source = null;
        FileChannel destination = null;
        try {
            source = new FileInputStream(sourceFile).getChannel();
            destination = new FileOutputStream(destFile).getChannel();
            destination.transferFrom(source, 0, source.size());
            return true;
        } finally {
            if (source != null) {
                source.close();
            }
            if (destination != null) {
                destination.close();
            }
        }
    }

    public static File[] haveZip4jFiles(String dir) {
        File d = new File(dir);
        if (!isFileExists(d)) return null;
        if (d.isFile()) return null;
        List<File> fileList = new ArrayList<>();
        for (File f : d.listFiles()) {
            if (f.isFile()) {
                if (f.getName().endsWith(".zip")) {
                    fileList.add(f);
                }
            } else {
                File[] files = haveZip4jFiles(f.getAbsolutePath());
                if (files != null) fileList.addAll(Arrays.asList(files));
            }
        }
        return fileList.toArray(new File[fileList.size()]);
    }

    /**
     * @return List<String>
     * @throws IOException
     * @Title: getExtSDCardPaths
     * @Description: to obtain storage paths, the first path is theoretically
     * the returned value of
     * Environment.getExternalStorageDirectory(), namely the
     * primary external storage. It can be the storage of internal
     * device, or that of external sdcard. If paths.size() >1,
     * basically, the current device contains two type of storage:
     * one is the storage of the device itself, one is that of
     * external sdcard. Additionally, the paths is directory.
     */
    public static List<String> getExtSDCardPaths() {
        List<String> paths = new ArrayList<String>();
        String extFileStatus = Environment.getExternalStorageState();
        File extFile = Environment.getExternalStorageDirectory();
        if (extFileStatus.equals(Environment.MEDIA_MOUNTED)
                && extFile.exists() && extFile.isDirectory()
                && extFile.canWrite()) {
            paths.add(extFile.getAbsolutePath());
        }
        InputStream is = null;
        InputStreamReader isr = null;
        BufferedReader br = null;
        try {
            // obtain executed result of command line code of 'mount', to judge
            // whether tfCard exists by the result
            Runtime runtime = Runtime.getRuntime();
            Process process = runtime.exec("mount");
            is = process.getInputStream();
            isr = new InputStreamReader(is);
            br = new BufferedReader(isr);
            String line = null;
            int mountPathIndex = 1;
            while ((line = br.readLine()) != null) {
                // format of sdcard file system: vfat/fuse
                if ((!line.contains("fat") && !line.contains("fuse") && !line.contains("storage"))
                        || line.contains("secure")
                        || line.contains("asec")
                        || line.contains("firmware")
                        || line.contains("shell")
                        || line.contains("obb")
                        || line.contains("legacy")
                        || line.contains("data")) {
                    continue;
                }
                String[] parts = line.split(" ");
                int length = parts.length;
                if (mountPathIndex >= length) {
                    continue;
                }
                String mountPath = parts[mountPathIndex];
                if (!mountPath.contains("/") || mountPath.contains("data")
                        || mountPath.contains("Data")) {
                    continue;
                }
                File mountRoot = new File(mountPath);
                if (!mountRoot.exists() || !mountRoot.isDirectory()
                        || !mountRoot.canWrite()) {
                    continue;
                }
                boolean equalsToPrimarySD = mountPath.equals(extFile
                        .getAbsolutePath());
                if (equalsToPrimarySD) {
                    continue;
                }
                paths.add(mountPath);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (br != null) br.close();
                if (isr != null) isr.close();
                if (is != null) is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return paths;
    }
}
