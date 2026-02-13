package com.luoye.context;


/**
 * 线程本地变量，用于存储当前用户信息
 */
public class BaseContext {

    // 用于存储用户ID的ThreadLocal
    private static ThreadLocal<Long> idThreadLocal = new ThreadLocal<>();
    // 用于存储身份类型的ThreadLocal
    private static ThreadLocal<String> identityThreadLocal = new ThreadLocal<>();

    /**
     * 设置当前用户ID
     * @param id 用户ID
     */
    public static void setCurrentId(Long id) {
        idThreadLocal.set(id);

    }

    /**
     * 获取当前用户ID
     * @return 当前用户ID
     */
    public static Long getCurrentId() {
        Long id = idThreadLocal.get();
        // 添加调试日志
        System.out.println("BaseContext获取ID - 线程ID: " + Thread.currentThread().getId() +
                ", ID: " + id);
        return id;
    }

    /**
     * 移除当前用户ID
     */
    public static void removeCurrentId() {
        idThreadLocal.remove();
    }

    /**
     * 设置当前用户身份
     * @param identity 身份类型
     */
    public static void setCurrentIdentity(String identity) {
        identityThreadLocal.set(identity);
    }

    /**
     * 获取当前用户身份
     * @return 当前用户身份
     */
    public static String getCurrentIdentity() {
        String identity = identityThreadLocal.get();
        // 添加调试日志
        System.out.println("BaseContext获取Identity - 线程ID: " + Thread.currentThread().getId() +
                ", Identity: " + identity);
        return identity;
    }

    /**
     * 移除当前用户身份
     */
    public static void removeCurrentIdentity() {
        identityThreadLocal.remove();
    }

    /**
     * 设置当前用户ID和身份类型
     * @param id 用户ID
     * @param identity 身份类型
     */
    public static void setCurrentId(Long id, String identity) {
        idThreadLocal.set(id);
        identityThreadLocal.set(identity);
        // 添加调试日志
        System.out.println("BaseContext设置 - 线程ID: " + Thread.currentThread().getId() +
                ", ID: " + id + ", Identity: " + identity);

    }

    /**
     * 清除所有当前用户信息
     */
    public static void clear() {
        idThreadLocal.remove();
        identityThreadLocal.remove();
    }
}
