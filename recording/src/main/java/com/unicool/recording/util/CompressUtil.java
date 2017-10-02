package com.unicool.recording.util;

import android.text.TextUtils;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.util.Zip4jConstants;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/*
 *  @项目名：  NationalCar-Android 
 *  @包名：    com.uniool.recording.service
 *  @文件名:   CompressUtil
 *  @创建者:   cjf
 *  @创建时间:  2017/9/27 1:53
 *  @描述：    http://blog.csdn.net/xxdddail/article/details/24001439
 *            依赖zip4j开源项目(http://www.lingala.net/zip4j/)
 */
public class CompressUtil {
    /**
     * 使用给定密码解压指定的ZIP压缩文件到指定目录
     * <p>
     * 如果指定目录不存在,可以自动创建,不合法的路径将导致异常被抛出
     *
     * @param zip    指定的ZIP压缩文件
     * @param dest   解压目录
     * @param passwd ZIP文件的密码
     * @return 解压后文件数组
     * @throws ZipException 压缩文件有损坏或者解压缩失败抛出
     */
    public static File[] unzip(String zip, String dest, String passwd) throws ZipException {
        File zipFile = new File(zip);
        return unzip(zipFile, dest, passwd);
    }

    /**
     * 使用给定密码解压指定的ZIP压缩文件到当前目录
     *
     * @param zip    指定的ZIP压缩文件
     * @param passwd ZIP文件的密码
     * @return 解压后文件数组
     * @throws ZipException 压缩文件有损坏或者解压缩失败抛出
     */
    public static File[] unzip(String zip, String passwd) throws ZipException {
        File zipFile = new File(zip);
        File parentDir = zipFile.getParentFile();
        return unzip(zipFile, parentDir.getAbsolutePath(), passwd);
    }

    /**
     * 使用给定密码解压指定的ZIP压缩文件到指定目录
     * <p>
     * 如果指定目录不存在,可以自动创建,不合法的路径将导致异常被抛出
     *
     * @param zipFile 指定的ZIP压缩文件
     * @param dest    解压目录
     * @param passwd  ZIP文件的密码
     * @return 解压后文件数组
     * @throws ZipException 压缩文件有损坏或者解压缩失败抛出
     */
    public static File[] unzip(File zipFile, String dest, String passwd) throws ZipException {
        ZipFile zFile = new ZipFile(zipFile);
        zFile.setFileNameCharset("GBK");
        if (!zFile.isValidZipFile()) {
            throw new ZipException("压缩文件不合法,可能被损坏.");
        }
        File destDir = new File(dest);
        if (destDir.isDirectory() && !destDir.exists()) {
            destDir.mkdir();
        }
        if (zFile.isEncrypted()) {
            zFile.setPassword(passwd.toCharArray());
        }
        zFile.extractAll(dest);

        List<FileHeader> headerList = zFile.getFileHeaders();
        List<File> extractedFileList = new ArrayList<File>();
        for (FileHeader fileHeader : headerList) {
            if (!fileHeader.isDirectory()) {
                extractedFileList.add(new File(destDir, fileHeader.getFileName()));
            }
        }
        File[] extractedFiles = new File[extractedFileList.size()];
        extractedFileList.toArray(extractedFiles);
        return extractedFiles;
    }

    /**
     * 压缩指定文件到当前文件夹
     *
     * @param src 要压缩的指定文件
     * @return 最终的压缩文件存放的绝对路径, 如果为null则说明压缩失败.
     */
    public static String zip(String src) {
        return zip(src, null);
    }

    /**
     * 使用给定密码压缩指定文件或文件夹到当前目录
     *
     * @param src    要压缩的文件
     * @param passwd 压缩使用的密码
     * @return 最终的压缩文件存放的绝对路径, 如果为null则说明压缩失败.
     */
    public static String zip(String src, String passwd) {
        return zip(src, null, passwd);
    }

    /**
     * 使用给定密码压缩指定文件或文件夹到当前目录
     *
     * @param src    要压缩的文件
     * @param dest   压缩文件存放路径
     * @param passwd 压缩使用的密码
     * @return 最终的压缩文件存放的绝对路径, 如果为null则说明压缩失败.
     */
    public static String zip(String src, String dest, String passwd) {
        return zip(src, dest, true, passwd);
    }

    /**
     * 使用给定密码压缩指定文件或文件夹到指定位置.
     * <p>
     * dest可传最终压缩文件存放的绝对路径,也可以传存放目录,也可以传null或者"".<br />
     * 如果传null或者""则将压缩文件存放在当前目录,即跟源文件同目录,压缩文件名取源文件名,以.zip为后缀;<br />
     * 如果以路径分隔符(File.separator)结尾,则视为目录,压缩文件名取源文件名,以.zip为后缀,否则视为文件名.
     *
     * @param src         要压缩的文件或文件夹路径
     * @param dest        压缩文件存放路径
     * @param isCreateDir 是否在压缩文件里创建目录,仅在压缩文件为目录时有效.<br />
     *                    如果为false,将直接压缩目录下文件到压缩文件.
     * @param passwd      压缩使用的密码，使用AES加密
     * @return 最终的压缩文件存放的绝对路径, 如果为null则说明压缩失败.
     */
    public static String zip(String src, String dest, boolean isCreateDir, String passwd) {
        File srcFile = new File(src);
        dest = buildDestinationZipFilePath(srcFile, dest);
        ZipParameters parameters = new ZipParameters();
        parameters.setCompressionMethod(Zip4jConstants.COMP_DEFLATE);           // 压缩方式  
        parameters.setCompressionLevel(Zip4jConstants.DEFLATE_LEVEL_NORMAL);    // 压缩级别  
        if (!TextUtils.isEmpty(passwd)) {
            parameters.setEncryptFiles(true);
            parameters.setEncryptionMethod(Zip4jConstants.ENC_METHOD_AES); // 加密方式  
            parameters.setAesKeyStrength(Zip4jConstants.AES_STRENGTH_256); // Set AES Key strength. 128/256
            parameters.setPassword(passwd.toCharArray());
        }
        try {
            ZipFile zipFile = new ZipFile(dest);
            if (srcFile.isDirectory()) {
                // 如果不创建目录的话,将直接把给定目录下的文件压缩到压缩文件,即没有目录结构  
                if (!isCreateDir) {
                    File[] subFiles = srcFile.listFiles();
                    ArrayList<File> temp = new ArrayList<File>();
                    Collections.addAll(temp, subFiles);
                    zipFile.addFiles(temp, parameters);
                    return dest;
                }
                zipFile.addFolder(srcFile, parameters);
            } else {
                zipFile.addFile(srcFile, parameters);
            }
            return dest;
        } catch (ZipException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 构建压缩文件存放路径,如果不存在将会创建
     * 传入的可能是文件名或者目录,也可能不传,此方法用以转换最终压缩文件的存放路径
     *
     * @param srcFile   源文件
     * @param destParam 压缩目标路径
     * @return 正确的压缩文件存放路径
     */
    private static String buildDestinationZipFilePath(File srcFile, String destParam) {
        if (TextUtils.isEmpty(destParam)) {
            if (srcFile.isDirectory()) {
                destParam = srcFile.getParent() + File.separator + srcFile.getName() + ".zip";
            } else {
                String fileName = srcFile.getName().substring(0, srcFile.getName().lastIndexOf("."));
                destParam = srcFile.getParent() + File.separator + fileName + ".zip";
            }
        } else {
            createDestDirectoryIfNecessary(destParam);  // 在指定路径不存在的情况下将其创建出来  
            if (destParam.endsWith(File.separator)) {
                String fileName = "";
                if (srcFile.isDirectory()) {
                    fileName = srcFile.getName();
                } else {
                    fileName = srcFile.getName().substring(0, srcFile.getName().lastIndexOf("."));
                }
                destParam += fileName + ".zip";
            }
        }
        return destParam;
    }

    /**
     * 在必要的情况下创建压缩文件存放目录,比如指定的存放路径并没有被创建
     *
     * @param destParam 指定的存放路径,有可能该路径并没有被创建
     */
    private static void createDestDirectoryIfNecessary(String destParam) {
        File destDir = null;
        if (destParam.endsWith(File.separator)) {
            destDir = new File(destParam);
        } else {
            destDir = new File(destParam.substring(0, destParam.lastIndexOf(File.separator)));
        }
        if (!destDir.exists()) {
            destDir.mkdirs();
        }
    }

    /**
     * 删除压缩文件中的目录
     *
     * @param zFile       指定的ZIP压缩文件
     * @param removeFiles 需删除的目标文件或文件夹（建议带文件分隔符），相对zip文件的路径
     * @param passwd      加密压缩需要添加密码
     * @throws ZipException 压缩文件有损坏或者解压缩失败抛出
     */
    public static void removeFilesFromZipArchive(String zFile, String removeFiles, String passwd) throws ZipException {
        // 创建ZipFile并设置编码
        ZipFile zipFile = new ZipFile(zFile);
        zipFile.setFileNameCharset("GBK");
        if (!zipFile.isValidZipFile()) {
            throw new ZipException("压缩文件不合法,可能被损坏.");
        }
        if (zipFile.isEncrypted()) {
            zipFile.setPassword(passwd.toCharArray());
        }

        // 如果文件（夹）不存在, 直接返回
        FileHeader dirHeader = zipFile.getFileHeader(removeFiles);
        if (null == dirHeader) {
            if (removeFiles.endsWith(File.separator)) return;
            removeFiles += File.separator; //缺少文件分隔符造成null
            dirHeader = zipFile.getFileHeader(removeFiles);
            if (null == dirHeader) return;
        }

        // 判断要删除的是文件还是目录
        if (!dirHeader.isDirectory()) {
            zipFile.removeFile(dirHeader);
            return;
        }

        // 遍历压缩文件中所有的FileHeader, 将指定删除目录下的子文件名保存起来
        List<FileHeader> headerList = zipFile.getFileHeaders();
        List<FileHeader> rmFiles = new ArrayList<FileHeader>();
        for (FileHeader subHeader : headerList) {
            String fileName = subHeader.getFileName();
            if (fileName.startsWith(dirHeader.getFileName()) && !fileName.equals(dirHeader.getFileName())) {
                rmFiles.add(subHeader);
            }
        }

        // 遍历删除指定目录下的所有子文件, 最后删除指定目录(此时已为空目录)
        for (FileHeader rmFile : rmFiles) {
            if (rmFile.isDirectory()) {
                removeFilesFromZipArchive(zFile, rmFile.getFileName(), passwd);
            } else {
                zipFile.removeFile(rmFile);
            }
        }
        zipFile.removeFile(dirHeader);
    }

    /**
     * 删除压缩包中最早创建的文件
     *
     * @param zFile  指定的ZIP压缩文件
     * @param passwd 加密压缩需要添加密码
     */
    public static void removeOldestFileFromZipArchive(String zFile, String passwd) throws ZipException {
        // 创建ZipFile并设置编码
        ZipFile zipFile = new ZipFile(zFile);
        zipFile.setFileNameCharset("GBK");
        if (!zipFile.isValidZipFile()) {
            throw new ZipException("压缩文件不合法,可能被损坏.");
        }
        if (zipFile.isEncrypted()) {
            zipFile.setPassword(passwd.toCharArray());
        }

        List<FileHeader> fileHeaders = zipFile.getFileHeaders();
        Collections.sort(fileHeaders, new Comparator<FileHeader>() {
            @Override
            public int compare(FileHeader f1, FileHeader f2) {
                return Integer.valueOf(f1.getLastModFileTime()).compareTo(f2.getLastModFileTime());
            }
        });
        FileHeader rmFile = fileHeaders.get(0);
        if (rmFile.isDirectory()) {
            removeOldestFileFromZipArchive(zFile, passwd);
        } else {
            zipFile.removeFile(rmFile);
        }
    }

    /**
     * Add folder to the zip file
     *
     * @param zipFile     完整路径的zipFile文件
     * @param folderInZip 文件夹相对zip的路径
     * @param passwd      AES-256 方式加密密码
     * @throws ZipException
     */
    public static String addFolder2Zip(String zipFile, String folderInZip, String passwd) {
        ZipParameters parameters = new ZipParameters();
        parameters.setCompressionMethod(Zip4jConstants.COMP_DEFLATE);           // 压缩方式
        parameters.setCompressionLevel(Zip4jConstants.DEFLATE_LEVEL_NORMAL);    // 压缩级别
        if (!TextUtils.isEmpty(passwd)) {
            parameters.setEncryptFiles(true);
            parameters.setEncryptionMethod(Zip4jConstants.ENC_METHOD_AES); // 加密方式
            parameters.setAesKeyStrength(Zip4jConstants.AES_STRENGTH_256); // Set AES Key strength. 128/256
            parameters.setPassword(passwd.toCharArray());
        }
        createDestDirectoryIfNecessary(new File(zipFile).getParent());
        try {
            ZipFile zf = new ZipFile(zipFile);
            if (!folderInZip.endsWith(File.separator)) folderInZip += File.separator; //x
            if (zf.getFileHeader(folderInZip) != null) return folderInZip;
            // Add folder to the zip file
            zf.addFolder(folderInZip, parameters);
            return folderInZip;
        } catch (ZipException e) {
            e.printStackTrace();
        }
        return null;
    }

}
