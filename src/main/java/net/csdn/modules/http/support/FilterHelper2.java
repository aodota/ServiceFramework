package net.csdn.modules.http.support;

import net.csdn.annotation.filter.AroundFilter;
import net.csdn.annotation.filter.BeforeFilter;
import net.csdn.annotation.rest.At;
import net.csdn.common.logging.CSLogger;
import net.csdn.common.logging.Loggers;
import net.csdn.common.reflect.ReflectHelper;
import net.csdn.filter.FilterHelper;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static net.csdn.common.collections.WowCollections.list;
import static net.csdn.common.collections.WowCollections.map;
import static net.csdn.common.reflect.ReflectHelper.findFieldsByAnnotation;
import static net.csdn.common.reflect.ReflectHelper.findMethodsByAnnotation;

/**
 * 4/8/13 WilliamZhu(allwefantasy@gmail.com)
 */
public class FilterHelper2 {

    private static CSLogger logger = Loggers.getLogger(FilterHelper2.class);

    public static Map<Method, Map<Class, List<Method>>> create(Class clzz) {
        Map<Method, Map<Class, List<Method>>> result = map();
        createFromFields(result, clzz, BeforeFilter.class);
        createFromFields(result, clzz, AroundFilter.class);
        createFromStaticBlock(result, clzz);
        return result;
    }

    private static void createFromFields(Map<Method, Map<Class, List<Method>>> result, Class clzz, Class annotation) {

        List<Method> actions = findMethodsByAnnotation(clzz, At.class);

        for (Method action : actions) {
            List<Field> fields = findFieldsByAnnotation(clzz, annotation);
            if (!result.containsKey(action)) {
                result.put(action, map(annotation, list()));
            } else if (!result.get(action).containsKey(annotation)) {
                result.get(action).put(annotation, new ArrayList<Method>());
            }
            for (Field field : fields) {
                try {
                    field.setAccessible(true);
                    Map filterInfo = (Map) field.get(null);
                    String filterMethod = field.getName().substring(1, field.getName().length());
                    result.get(action).get(annotation).addAll(whoFilterThisMethod(clzz, filterMethod, filterInfo, action));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }

            }
        }

    }

    /*
      从静态代码块中构建过滤器集合
     */
    private static void createFromStaticBlock(Map<Method, Map<Class, List<Method>>> result, Class clzz) {

        //知道controller类中所有actions
        List<Method> actions = findMethodsByAnnotation(clzz, At.class);
        List<String> filterFileNames = list("parent$_before_filter_info", "parent$_around_filter_info");

        for (Method action : actions) {
            for (String name : filterFileNames) {
                Class annotation = "parent$_before_filter_info".equals(name) ? BeforeFilter.class : AroundFilter.class;
                if (!result.containsKey(action)) {
                    result.put(action, map(annotation, list()));
                } else if (!result.get(action).containsKey(annotation)) {
                    result.get(action).put(annotation, new ArrayList<Method>());
                }

                //获取过滤器配置信息
                Map<String, Map> temp = (Map) ReflectHelper.staticMethod(clzz, name);

                for (Map.Entry<String, Map> entry : temp.entrySet()) {
                    result.get(action).get(annotation).addAll((whoFilterThisMethod(clzz, entry.getKey(), entry.getValue(), action)));
                }
            }
        }

    }

    private static List<Method> whoFilterThisMethod(Class clzz, String filterMethod, Map filterInfo, Method method) {
        List<Method> result = list();

        if (filterInfo.containsKey(FilterHelper.BeforeFilter.only)) {
            List<String> actions = (List<String>) filterInfo.get(FilterHelper.BeforeFilter.only);
            if (actions.contains(method.getName())) {
                Method method1 = ReflectHelper.findMethodByName(clzz, filterMethod);
                if (method1 != null) {
                    result.add(method1);
                } else {
                    logger.error("method filter: " + filterMethod + " not exists");
                }
            }
        } else {
            if (filterInfo.containsKey(FilterHelper.BeforeFilter.except)) {
                List<String> actions = (List<String>) filterInfo.get(FilterHelper.BeforeFilter.except);
                if (!actions.contains(method.getName())) {
                    Method method1 = ReflectHelper.findMethodByName(clzz, filterMethod);
                    if (method1 != null) {
                        result.add(method1);
                    } else {
                        logger.error("method filter: " + filterMethod + " not exists");
                    }
                }
            } else {
                Method method1 = ReflectHelper.findMethodByName(clzz, filterMethod);

                if (method1 != null) {
                    result.add(method1);
                } else {
                    logger.error("method filter: " + filterMethod + " not exists");
                }
            }
        }

        return result;

    }

}
