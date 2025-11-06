package org.example.utils;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.resource.ResourceUtil;

import java.io.File;
import java.io.FileInputStream;

public class StrUtils {
    public static boolean isNotEmpty(CharSequence cs) {
        return !isEmpty(cs);
    }

    public static boolean isEmpty(CharSequence cs) {
        return cs == null || cs.length() == 0;
    }
    public static String readByResource(String fileName) {
        File file = new File(fileName);
        if(file.exists()) {
            try (var inputStream = new FileInputStream(file)) {
                return IoUtil.readUtf8(inputStream);
            }catch (Exception e) {
                throw new RuntimeException("读取文件错误",e);
            }
        }
        try (var inputStream = ResourceUtil.getStream(fileName)) {
            return IoUtil.readUtf8(inputStream);
        } catch (Exception e) {
            throw new RuntimeException("读取文件错误",e);
        }
    }

    public static void main(String[] args) {
    }

}
