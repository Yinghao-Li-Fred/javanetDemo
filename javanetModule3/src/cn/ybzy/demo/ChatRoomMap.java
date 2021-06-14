package cn.ybzy.demo;

import java.util.*;

/**
 * 新定义的一个数据结构，用来保存用户和Socket对应的输出流之间的映射关系
 * @param <K>:代表客户端的用户名
 * @param <V>:是一个SocketChannel
 */
public class ChatRoomMap<K, V> {
    // 本质上保存数据的是一个特殊的HashMap,我们定义过的，线程安全
    public Map<K, V> map = Collections.synchronizedMap(new HashMap<>());

    // 可以根据V值来删除指定的项目
    public synchronized void removeByValue(Object value) {
        for (Object key : map.keySet()) {
            if (map.get(key) == value) {
                map.remove(key); // 通过key把数据项删掉
                break;
            }
        }
    }

    // 获取所有的Value组合成的set集合
    public synchronized Set<V> getValueSet() {
        Set<V> res = new HashSet<>();
        // 将map中的value添加到res集合里
        for (Object key : map.keySet()) {
            res.add(map.get(key));
        }
        return res;
    }

    // 根据value值找到key值
    public synchronized K getKeyByValue(V val) {
        for (K key : map.keySet()) {
            if (map.get(key)==val || map.get(key).equals(val)) {
                return key;
            }
        }
        return null;
    }

    // 添加数据到ChatRoomMap，此数据结构规定value不能重复
    public synchronized V put(K key, V value) {
        // 遍历所有的value值，不能重复
        for (V val : getValueSet()) {
            if (val.equals(value) && val.hashCode() == value.hashCode()) {
                throw new RuntimeException("Can't have duplicate values in the map! ");
            }
        }
        return map.put(key, value);
    }
}
