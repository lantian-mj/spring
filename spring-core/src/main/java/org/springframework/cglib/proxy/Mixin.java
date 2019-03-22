//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.springframework.cglib.proxy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.springframework.asm.ClassVisitor;
import org.springframework.cglib.core.AbstractClassGenerator;
import org.springframework.cglib.core.ClassesKey;
import org.springframework.cglib.core.KeyFactory;
import org.springframework.cglib.core.ReflectUtils;

public abstract class Mixin {
    private static final Mixin.MixinKey KEY_FACTORY;
    private static final Map ROUTE_CACHE;
    public static final int STYLE_INTERFACES = 0;
    public static final int STYLE_BEANS = 1;
    public static final int STYLE_EVERYTHING = 2;

    public Mixin() {
    }

    public abstract Mixin newInstance(Object[] var1);

    public static Mixin create(Object[] delegates) {
        Mixin.Generator gen = new Mixin.Generator();
        gen.setDelegates(delegates);
        return gen.create();
    }

    public static Mixin create(Class[] interfaces, Object[] delegates) {
        Mixin.Generator gen = new Mixin.Generator();
        gen.setClasses(interfaces);
        gen.setDelegates(delegates);
        return gen.create();
    }

    public static Mixin createBean(Object[] beans) {
        return createBean((ClassLoader)null, beans);
    }

    public static Mixin createBean(ClassLoader loader, Object[] beans) {
        Mixin.Generator gen = new Mixin.Generator();
        gen.setStyle(1);
        gen.setDelegates(beans);
        gen.setClassLoader(loader);
        return gen.create();
    }

    public static Class[] getClasses(Object[] delegates) {
        return (Class[])route(delegates).classes.clone();
    }

    private static Mixin.Route route(Object[] delegates) {
        Object key = ClassesKey.create(delegates);
        Mixin.Route route = (Mixin.Route)ROUTE_CACHE.get(key);
        if(route == null) {
            ROUTE_CACHE.put(key, route = new Mixin.Route(delegates));
        }

        return route;
    }

    static {
        KEY_FACTORY = (Mixin.MixinKey)KeyFactory.create(Mixin.MixinKey.class, KeyFactory.CLASS_BY_NAME);
        ROUTE_CACHE = Collections.synchronizedMap(new HashMap());
    }

    private static class Route {
        private Class[] classes;
        private int[] route;

        Route(Object[] delegates) {
            Map map = new HashMap();
            ArrayList collect = new ArrayList();

            int i;
            for(i = 0; i < delegates.length; ++i) {
                Class delegate = delegates[i].getClass();
                collect.clear();
                ReflectUtils.addAllInterfaces(delegate, collect);
                Iterator it = collect.iterator();

                while(it.hasNext()) {
                    Class iface = (Class)it.next();
                    if(!map.containsKey(iface)) {
                        map.put(iface, new Integer(i));
                    }
                }
            }

            this.classes = new Class[map.size()];
            this.route = new int[map.size()];
            i = 0;

            for(Iterator it = map.keySet().iterator(); it.hasNext(); ++i) {
                Class key = (Class)it.next();
                this.classes[i] = key;
                this.route[i] = ((Integer)map.get(key)).intValue();
            }

        }
    }

    public static class Generator extends AbstractClassGenerator {
        private static final Source SOURCE;
        private Class[] classes;
        private Object[] delegates;
        private int style = 0;
        private int[] route;

        public Generator() {
            super(SOURCE);
        }

        protected ClassLoader getDefaultClassLoader() {
            return this.classes[0].getClassLoader();
        }

        public void setStyle(int style) {
            switch(style) {
                case 0:
                case 1:
                case 2:
                    this.style = style;
                    return;
                default:
                    throw new IllegalArgumentException("Unknown mixin style: " + style);
            }
        }

        public void setClasses(Class[] classes) {
            this.classes = classes;
        }

        public void setDelegates(Object[] delegates) {
            this.delegates = delegates;
        }

        public Mixin create() {
            if(this.classes == null && this.delegates == null) {
                throw new IllegalStateException("Either classes or delegates must be set");
            } else {
                switch(this.style) {
                    case 0:
                        if(this.classes == null) {
                            Mixin.Route r = Mixin.route(this.delegates);
                            this.classes = r.classes;
                            this.route = r.route;
                        }
                        break;
                    case 1:
                    case 2:
                        if(this.classes == null) {
                            this.classes = ReflectUtils.getClasses(this.delegates);
                        } else if(this.delegates != null) {
                            Class[] temp = ReflectUtils.getClasses(this.delegates);
                            if(this.classes.length != temp.length) {
                                throw new IllegalStateException("Specified classes are incompatible with delegates");
                            }

                            for(int i = 0; i < this.classes.length; ++i) {
                                if(!this.classes[i].isAssignableFrom(temp[i])) {
                                    throw new IllegalStateException("Specified class " + this.classes[i] + " is incompatible with delegate class " + temp[i] + " (index " + i + ")");
                                }
                            }
                        }
                }

                this.setNamePrefix(this.classes[ReflectUtils.findPackageProtected(this.classes)].getName());
                return (Mixin)super.create(Mixin.KEY_FACTORY.newInstance(this.style, ReflectUtils.getNames(this.classes), this.route));
            }
        }

        public void generateClass(ClassVisitor v) {
            switch(this.style) {
                case 0:
                    new MixinEmitter(v, this.getClassName(), this.classes, this.route);
                    break;
                case 1:
                    new MixinBeanEmitter(v, this.getClassName(), this.classes);
                    break;
                case 2:
                    new MixinEverythingEmitter(v, this.getClassName(), this.classes);
            }

        }

        protected Object firstInstance(Class type) {
            return ((Mixin)ReflectUtils.newInstance(type)).newInstance(this.delegates);
        }

        protected Object nextInstance(Object instance) {
            return ((Mixin)instance).newInstance(this.delegates);
        }

        static {
            SOURCE = new Source(Mixin.class.getName());
        }
    }

    interface MixinKey {
        Object newInstance(int var1, String[] var2, int[] var3);
    }
}
