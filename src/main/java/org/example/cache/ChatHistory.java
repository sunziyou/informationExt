package org.example.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.ai.openai.api.OpenAiApi;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ChatHistory {
    private static Cache<String, List<OpenAiApi.ChatCompletionMessage>> cache = null;

    static {
        cache = Caffeine.newBuilder()
                .expireAfterWrite(24, TimeUnit.HOURS) // 写入后10分钟过期
                .maximumSize(1000) // 最大缓存条目数
                .build();
    }
    public static void put(String key, List<OpenAiApi.ChatCompletionMessage> value) {
        cache.put(key, value);
    }
    //如果历史记录太长则删除最靠前的第一次回复，第一个是系统提示不能删除
    public static List<OpenAiApi.ChatCompletionMessage> get(String key) {
        List<OpenAiApi.ChatCompletionMessage> ifPresent = cache.getIfPresent(key);
        if(ifPresent == null) {
            return new ArrayList<>();
        }
        int maxSize = 15;

        if(ifPresent.size()>15){
           int count= ifPresent.size()-maxSize;
           for(int i=0;i<count;i++){
               ifPresent.remove(1);
           }

        }
        cache.put(key, ifPresent);
        return ifPresent;
    }
    public static void invalidate(String key) {
        cache.invalidate(key);
    }

    public static void main(String[] args) {
        List<String> ss = new ArrayList<>();
        ss.add("1");
        ss.add("2");
        ss.add("3");
        ss.add("4");
        ss.add("5");
        ss.add("6");
        ss.add("7");
        for(int i=0;i<3;i++){
            ss.remove(1);
        }
       System.out.println(ss);
    }
}

