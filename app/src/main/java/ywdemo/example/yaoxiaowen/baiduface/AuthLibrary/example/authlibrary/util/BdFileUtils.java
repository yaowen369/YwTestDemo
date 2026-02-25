package ywdemo.example.yaoxiaowen.baiduface.AuthLibrary.example.authlibrary.util;

import android.content.Context;
import android.util.Log;


import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import ywdemo.example.yaoxiaowen.baiduface.AuthLibrary.example.authlibrary.CodeDetail;
import ywdemo.example.yaoxiaowen.baiduface.AuthLibrary.example.authlibrary.data.SituationData;

public class BdFileUtils {
    private static final String TAG = "BdAuthFileUtils";


    public static SituationData readAuthFile(Context context , String filePath) {
        File iniFile = new File(filePath + File.separator + "license.ini");
        File keyFile = new File(filePath + File.separator + "license.key");
        SituationData situationData = new SituationData();
        // 读取ini和key文件
        if (keyFile.exists() && iniFile.exists() && keyFile.isFile() && iniFile.isFile()) {
            return readAuthFile(context , keyFile , iniFile);
            // 读取zip文件
        } else {
            File zipFile = new File(filePath + File.separator + "License.zip");
            if (zipFile.exists() && zipFile.isFile()) {
                SituationData zipData = unZip(zipFile.getAbsolutePath(), filePath);
                if (zipData.getCode() == CodeDetail.SUCCESS) {
                    return readAuthFile(context , filePath);
                }
                situationData.setMassage(zipData.getMassage());
                situationData.setCode(zipData.getCode());
                return zipData;
            }
            situationData.setMassage("未找到授权文件,请将文件放到" + filePath + "目录");
            situationData.setCode(CodeDetail.FILE_NOT_ERROR);
            return situationData;
        }
    }
    public static SituationData readAuthFile(Context context , File keyFile , File iniFile) {
        SituationData situationData = new SituationData();
        SituationData iniData = readIniFile(iniFile);
        SituationData keyData = readkeyFile(keyFile);
        // ini文件校验
        if (iniData.getCode() != CodeDetail.SUCCESS) {
            situationData.setMassage(iniData.getMassage());
            situationData.setCode(iniData.getCode());
            return situationData;
        }
        // key文件校验
        if (keyData.getCode() != CodeDetail.SUCCESS) {
            situationData.setMassage(keyData.getMassage());
            situationData.setCode(keyData.getCode());
            return situationData;
        }
        situationData.setCode(CodeDetail.SUCCESS);
        situationData.setKey(keyData.getKey());
        situationData.setValue(iniData.getValue());
//            saveAuthFile(keyData.getKey() , iniData.getValue() , context.getCacheDir().getAbsolutePath());
        return situationData;
    }

    public static void saveAuthFile(String key, String[] value, String path) {
        File iniFile = new File(path + File.separator + "license.ini");
        File keyFile = new File(path + File.separator + "license.key");
        if (iniFile.exists()){
            iniFile.delete();
        }
        if (keyFile.exists()){
            keyFile.delete();
        }
        StringBuilder iniValue = new StringBuilder();
        if (value != null){
            for (int i = 0 , k = value.length ; i < k ; i++){
                if (i == k - 1 ){
                    iniValue.append(value[i]);
                }else {
                    iniValue.append(value[i]).append("\n");
                }
            }
        }
        saveAuthFile(key , keyFile.getAbsolutePath());
        saveAuthFile(iniValue.toString(), iniFile.getAbsolutePath());
    }

    public static void saveAuthFile(String value, String path) {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(path); // 输出流创建文件时必须保证父路径存在
            byte[] buf = value.getBytes();
            fos.write(buf, 0, buf.length);
            fos.flush();
            fos.close();
        } catch (Exception e) {
            Log.e(TAG , path + "保存失败");
        }
    }

    /**
     * 解压文件
     *
     * @throws
     */
    public static SituationData unZip(String zipFilePath, String targetDir) {
        SituationData zipSituationData = new SituationData();
        File destDir = new File(targetDir);
        if (!destDir.exists()) {
            boolean mkdirs = destDir.mkdirs();
        }
        File file = new File(zipFilePath);
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis));
            String targetBasePath = destDir.getAbsolutePath();
            zipSituationData = extractZip(zis, targetBasePath);
        } catch (FileNotFoundException e) {
            zipSituationData.setCode(CodeDetail.FILE_ZIP_CREATE_ERROR);
            zipSituationData.setMassage("FileInputStream创建失败,请确认读取权限是否开启 , " + e.getMessage());
        }
        return zipSituationData;
    }

    private static SituationData extractZip(ZipInputStream zis, String targetPath) {
        ZipEntry entry = null;
        SituationData zipSituationData = new SituationData();
        try {
            entry = zis.getNextEntry();
            zipSituationData.setCode(CodeDetail.SUCCESS);
            while (entry != null) {
                File file = new File(targetPath + File.separator + entry.getName());
                if (!entry.isDirectory()) {
                    File parentFile = file.getParentFile();
                    if (parentFile != null && !parentFile.exists()) {
                        parentFile.mkdirs();
                    }
                    FileOutputStream fos = null;
                    try {
                        fos = new FileOutputStream(file); // 输出流创建文件时必须保证父路径存在
                        int len = 0;
                        byte[] buf = new byte[1024];
                        while ((len = zis.read(buf)) != -1) {
                            fos.write(buf, 0, len);
                        }
                        fos.flush();
                    } catch (Exception e) {
                        zipSituationData.setCode(CodeDetail.FILE_ZIP_READ_ERROR);
                        zipSituationData.setMassage("zip文件读取失败,请确认权限是否开启 , " + e.getMessage());
                    } finally {
                        if (fos != null) {
                            fos.close();
                        }
                    }
                    zis.closeEntry();
                    entry = zis.getNextEntry();
                }
            }
        } catch (IOException e) {
            zipSituationData.setCode(CodeDetail.FILE_ZIP_READ_ERROR);
            zipSituationData.setMassage("zip文件读取失败,请确认权限是否开启 , " + e.getMessage());
        }
        return zipSituationData;
    }


    public static SituationData readkeyFile(File file) {
        SituationData situationData = new SituationData();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String line = "";
            StringBuilder value = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                // 处理每行数据
                value.append(line);
            }
            situationData.setKey(String.valueOf(value));
            situationData.setCode(CodeDetail.SUCCESS);
        } catch (IOException e) {
            situationData.setCode(CodeDetail.FILE_READ_ERROR);
            situationData.setMassage("文件读取失败,徐确认读取权限是否开启");
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    situationData.setCode(CodeDetail.FILE_CLEAN_ERROR);
                    situationData.setMassage("文件读取失败，缓存清理错误");
                }
            }
        }
        return situationData;
    }
    public static SituationData readIniFile(File file) {
        SituationData situationData = new SituationData();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String line = "";
            int i = 0;
            String[] value = new String[2];
            while ((line = reader.readLine()) != null) {
                // 处理每行数据
                if (i < 2){
                    value[i] = line;
                    i += 1;
                }
            }
            situationData.setValue(value);
            situationData.setCode(CodeDetail.SUCCESS);
        } catch (IOException e) {
            situationData.setCode(CodeDetail.FILE_READ_ERROR);
            situationData.setMassage("文件读取失败,徐确认读取权限是否开启");
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    situationData.setCode(CodeDetail.FILE_CLEAN_ERROR);
                    situationData.setMassage("文件读取失败，缓存清理错误");
                }
            }
        }
        return situationData;
    }
}
