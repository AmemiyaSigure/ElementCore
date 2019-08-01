package com.elementtimes.elementcore.annotation.processor;

import com.elementtimes.elementcore.annotation.AnnotationInitializer;
import com.elementtimes.elementcore.annotation.annotations.ModSkip;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * 加载 Class
 *
 * @author luqin2007
 */
public class ModClassLoader {

    @SafeVarargs
    public static void load(AnnotationInitializer initializer, Class<? extends Annotation>... support) {
        HashMap<Class, ArrayList<AnnotatedElement>> elements = initializer.elements;
        for (Class aClass : support) {
            if (!elements.containsKey(aClass)) {
                elements.put(aClass, new ArrayList<>());
            }
        }

        try {
            Set<Class<?>> classes = getClasses(initializer.modInfo.pkgName);
            for (Class<?> aClass : classes) {
                for (Class<? extends Annotation> annotation : support) {
                    if (aClass.getAnnotation(annotation) != null) {
                        elements.get(annotation).add(aClass);
                        break;
                    }
                }
                for (Field field : aClass.getDeclaredFields()) {
                    field.setAccessible(true);
                    for (Class<? extends Annotation> annotation : support) {
                        if (field.getAnnotation(annotation) != null) {
                            elements.get(annotation).add(field);
                            break;
                        }
                    }
                }
                for (Method method : aClass.getDeclaredMethods()) {
                    method.setAccessible(true);
                    for (Class<? extends Annotation> annotation : support) {
                        if (method.getAnnotation(annotation) != null) {
                            elements.get(annotation).add(method);
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 从包package中获取所有的Class
     * 修改：忽略 ModSkip 注解
     * 代码来自 http://guoliangqi.iteye.com/blog/644876
     */
    private static Set<Class<?>> getClasses(String pack) throws Exception {
        Set<Class<?>> classes = new LinkedHashSet<>();
        String packageName = pack;
        String packageDirName = packageName.replace('.', '/');
        Enumeration<URL> dirs;
        dirs = Thread.currentThread().getContextClassLoader().getResources(packageDirName);
        while (dirs.hasMoreElements()) {
            URL url = dirs.nextElement();
            String protocol = url.getProtocol();
            if ("file".equals(protocol)) {
                System.err.println("file类型的扫描");
                String filePath = URLDecoder.decode(url.getFile(), "UTF-8");
                findAndAddClassesInPackageByFile(packageName, filePath, classes);
            } else if ("jar".equals(protocol)) {
                System.err.println("jar类型的扫描");
                JarFile jar = ((JarURLConnection) url.openConnection()).getJarFile();
                Enumeration<JarEntry> entries = jar.entries();
                Set<String> classTmp = new LinkedHashSet<>();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (name.charAt(0) == '/') {
                        name = name.substring(1);
                    }
                    if (name.startsWith(packageDirName)) {
                        int idx = name.lastIndexOf('/');
                        if (idx != -1) {
                            packageName = name.substring(0, idx).replace('/', '.');
                        }
                        if (name.endsWith(".class") && !entry.isDirectory()) {
                            String className = name.substring(packageName.length() + 1, name.length() - 6);
                            classTmp.add(packageName + "." + className);
                        }
                    }
                }
                // 排除
                Set<String> remove = new LinkedHashSet<>();
                classTmp.stream()
                        .filter(c -> c.endsWith("package-info"))
                        .forEach(pInfos -> {
                            try {
                                Class aClass = Thread.currentThread().getContextClassLoader().loadClass(pInfos);
                                Annotation annotation = aClass.getAnnotation(ModSkip.class);
                                if (annotation != null) {
                                    String name = aClass.getPackage().getName();
                                    System.out.println("跳过：" + name);
                                    remove.add(name);
                                }
                            } catch (ClassNotFoundException ignored) {}
                        });
                classTmp.removeIf(r -> {
                    for (String s : remove) {
                        if (r.startsWith(s)) {
                            return true;
                        }
                    }
                    return false;
                });
                for (String s : classTmp) {
                    classes.add(Class.forName(s));
                }
            }
        }

        return classes;
    }

    /**
     * 修改：忽略 ModSkip 注解
     * 以文件的形式来获取包下的所有Class
     */
    private static void findAndAddClassesInPackageByFile(String packageName, String packagePath, Set<Class<?>> classes) throws Exception {
        File dir = new File(packagePath);
        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }
        // 忽略 ModSkip
        File[] pInfos = dir.listFiles(file -> "package-info.class".equals(file.getName()));
        if (pInfos != null && pInfos.length > 0) {
            Class<?> aClass = Thread.currentThread().getContextClassLoader().loadClass(packageName + ".package-info");
            Annotation annotation = aClass.getAnnotation(ModSkip.class);
            if (annotation != null) {
                System.out.println("跳过：" + aClass.getPackage().getName());
                return;
            }
        }
        File[] dirFiles = dir.listFiles(file -> file.isDirectory() || (file.getName().endsWith(".class")));
        if (dirFiles != null && dirFiles.length > 0) {
            for (File file : dirFiles) {
                if (file.isDirectory()) {
                    findAndAddClassesInPackageByFile(packageName + "." + file.getName(), file.getAbsolutePath(), classes);
                } else {
                    String className = file.getName().substring(0, file.getName().length() - 6);
                    classes.add(Thread.currentThread().getContextClassLoader().loadClass(packageName + '.' + className));
                }
            }
        }
    }
}

//    /**
//     * 获取所有命令
//     */
//    public static ArrayList<CommandBase> getCommands() {
//        if (sCommands == null) {
//            sCommands = new ArrayList<>();
//            sElementClasses.get(ModCommand.class)
//                    .forEach(aClass -> {
//                        try {
//                            sCommands.set((CommandBase) get(aClass));
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        }
//                    });
//        }
//        return sCommands;
//    }
//
//    /**
//     * 获取所有药水效果
//     */
//    public static ArrayList<Potion> getPotions() {
//        if (sPotions == null) {
//            sPotions = new ArrayList<>();
//            sElementClasses.get(ModPotion.class)
//                    .forEach(aClass -> {
//                        try {
//                            ModPotion info = (ModPotion) aClass.getAnnotation(ModPotion.class);
//                            Potion potion = (Potion) get(aClass);
//                            potion.setPotionName(info.name());
//                            potion.setRegistryName(info.registerName());
//                            sPotions.set(potion);
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        }
//                    });
//        }
//        return sPotions;
//    }
//
//    /**
//     * 获取所有桶
//     * @deprecated 有万能桶了。该注解可能会被删除，也可能会保留（自定义桶，比如某些液体一桶的容量）
//     */
//    @Deprecated
//    public static ArrayList<ItemBucket> getBuckets() {
//        if (sBuckets == null) {
//            sBuckets = new ArrayList<>();
//            sElementClasses.get(ModFluid.ModBucket.class)
//                    .forEach(aClass -> {
//                        try {
//                            ModFluid.ModBucket info = (ModFluid.ModBucket) aClass.getAnnotation(ModFluid.ModBucket.class);
//                            ItemBucket item = (ItemBucket) get(aClass);
//                            item.setRegistryName(info.registerName());
//                            item.setUnlocalizedName(info.unlocalizedName());
//                            item.setCreativeTab((CreativeTabs) get(Class.forName(info.creativeTab())));
//                            sBuckets.set(item);
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        }
//                    });
//        }
//        return sBuckets;
//    }
//
//    /**
//     * 获取所有实体
//     */
//    public static BiMap<Class, ModEntity> getEntities() {
//        if (sEntities == null) {
//            sEntities = HashBiMap.create();
//            sElementClasses.get(ModEntity.class)
//                    .forEach(aClass -> {
//                        try {
//                            ModEntity entity = (ModEntity) aClass.getAnnotation(ModEntity.class);
//                            sEntities.put(aClass, entity);
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        }
//                    });
//        }
//        return sEntities;
//    }
//
//    /**
//     * 获取所有 Capability
//     */
//    public static ArrayList<ModCapability> getCapabilities() {
//        if (sCapabilities == null) {
//            sCapabilities = new ArrayList<>();
//            sElementClasses.get(ModCapability.class)
//                    .forEach(aClass -> sCapabilities.set((ModCapability) aClass.getAnnotation(ModCapability.class)));
//        }
//        return sCapabilities;
//    }
//
//    /**
//     * 获取通信
//     * 注意: 本操作附带有注册过程 调用后无需注册
//     */
//    public static BiMap<Integer, BiMap<Side, SimpleNetworkWrapper>> getNetworks() {
//        if (sNetworks == null) {
//            sNetworks = HashBiMap.create();
//            sElementClasses.get(ModNetwork.class)
//                    .forEach(aClass -> {
//                        try {
//                            ModNetwork info = (ModNetwork) aClass.getAnnotation(ModNetwork.class);
//                            IMessageHandler handler = (IMessageHandler) get(Class.forName(info.handlerClass()));
//                            for (Side side : info.side()) {
//                                SimpleNetworkWrapper instance = NetworkRegistry.INSTANCE.newSimpleChannel(ElementTimes.MODID);
//                                instance.registerMessage(handler, aClass, info.id(), side);
//                                if (!sNetworks.containsKey(info.id()))
//                                    sNetworks.put(info.id(), HashBiMap.create());
//                                sNetworks.get(info.id()).put(side, instance);
//                            }
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        }
//                    });
//        }
//        return sNetworks;
//    }