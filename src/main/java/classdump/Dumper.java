package classdump;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

@SuppressWarnings({"rawtypes", "CallToPrintStackTrace"})
public final class Dumper implements ClassFileTransformer {
    private static final File DIR;

    static {
        File dir = DIR = new File("_classdump");
        if (dir.exists())
            rm(dir);
    }

    public final boolean debug;

    public Dumper(boolean debug) {
        this.debug = debug;
    }

    private static void rm(File file) {
        if (file.isDirectory())
            //noinspection DataFlowIssue
            for (File f : file.listFiles())
                rm(f);
        if (!file.delete())
            throw new RuntimeException("Failed to delete ".concat(file.getPath()));
    }

    public static void premain(String args, Instrumentation inst) {
        boolean debug = "debug".equalsIgnoreCase(args);
        inst.addTransformer(new Dumper(debug));

        if (debug)
            System.out.println("Dumping classes to ".concat(DIR.getAbsolutePath()));

        // already loaded classes
        int i = 0;
        Class[] classes = inst.getAllLoadedClasses();
        for (int length = classes.length; i < length; i++) {
            Class cls = classes[i];
            if (cls.isArray())
                continue;
            String name = cls.getName().replace('.', '/');
            try {
                String clsName = name.concat(".class");
                File file = new File(DIR, clsName);
                if (file.exists())
                    continue;

                InputStream in = cls.getResourceAsStream("/".concat(clsName));
                if (in != null) {
                    try {
                        //noinspection ResultOfMethodCallIgnored ???????? (what)
                        file.getParentFile().mkdirs();
                        FileOutputStream out = new FileOutputStream(file);
                        try {
                            byte[] buf = new byte[8 * 1024];
                            for (int j; (j = in.read(buf)) != -1; )
                                out.write(buf, 0, j);
                        } finally {
                            out.close();
                        }
                    } finally {
                        in.close();
                    }
                    if (debug)
                        System.out.println("Dumped class ".concat(name));
                } else if (debug)
                    System.out.println("Unable to get bytecode for ".concat(name));
            } catch (Exception e) {
                System.err.println("Unable to dump class ".concat(name));
                e.printStackTrace();
            }
        }

        if (debug)
            System.out.println(String.valueOf(i).concat(" classes loaded during initialization"));
    }

    public byte[] transform(ClassLoader classLoader, String name, Class clazz, ProtectionDomain protectionDomain, byte[] bytes) {
        try {
            File file = new File(DIR, name.concat(".class"));
            //noinspection ResultOfMethodCallIgnored ???????? (what)
            file.getParentFile().mkdirs();
            FileOutputStream out = new FileOutputStream(file);
            try {
                out.write(bytes);
            } finally {
                out.close();
            }
            if (debug)
                System.out.println("Dumped class ".concat(name));
        } catch (Exception e) {
            System.err.println("Unable to dump class ".concat(name));
            e.printStackTrace();
        }
        return null;
    }
}