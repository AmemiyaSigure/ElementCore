package com.elementtimes.elementcore.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Capability 能力系统
 * @author luqin2007
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ModCapability {

    /**
     * Capability 对应数据接口类
     */
    String typeInterfaceClass();

    /**
     * Capability 对应数据接口类的实现类
     */
    String typeImplementationClass();

    /**
     * Capability.IStorage 实现类
     */
    String storageClass();
}
