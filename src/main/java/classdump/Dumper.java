package classdump;

import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.ProtectionDomain;
import java.util.Comparator;
import java.util.stream.Stream;

@SuppressWarnings("rawtypes")
public final class Dumper implements ClassFileTransformer {
    private static final Path DIR = Paths.get("_classdump");

    static {
        if (Files.exists(DIR))
            try {
                try (Stream<Path> walk = Files.walk(DIR)) {
                    walk.sorted(Comparator.reverseOrder()).forEach(Dumper::rm);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
    }

    private static void rm(Path path) {
        try {
            Files.delete(path);
        } catch (IOException e) {
            throw new RuntimeException(path.toString(), e);
        }
    }

    private final boolean debug;

    public Dumper(boolean debug) {
        this.debug = debug;
    }

    public static void premain(String args, Instrumentation inst) {
        boolean debug = "debug".equalsIgnoreCase(args);
        inst.addTransformer(new Dumper(debug));
        Class[] classes = inst.getAllLoadedClasses();

        if (debug)
            System.out.println("Dumping classes to " + DIR.toAbsolutePath());

        //already loaded classes
        for (Class cls : classes) {
            if (cls.isArray())
                continue;
            String name = cls.getName().replace('.', '/');
            try {
                String clsName = name + ".class";
                InputStream in = cls.getResourceAsStream("/" + clsName);
                if (in != null) {
                    try {
                        Path path = DIR.resolve(clsName);
                        Files.createDirectories(path.getParent());
                        Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
                    } finally {
                        in.close();
                    }
                    if (debug)
                        System.out.println("Dumped class " + name);
                } else if (debug)
                    System.out.println("Unable to get bytecode for " + name);
            } catch (Exception e) {
                System.err.println("Unable to dump class " + name);
                //noinspection CallToPrintStackTrace
                e.printStackTrace();
            }
        }


        if (debug)
            System.out.println(inst.getAllLoadedClasses().length + " classes loaded during initialization");
    }

    @Override
    public byte[] transform(ClassLoader classLoader, String name, Class clazz, ProtectionDomain protectionDomain, byte[] bytes) {
        try {
            Path path = DIR.resolve(name + ".class");
            Files.createDirectories(path.getParent());
            Files.write(path, bytes);
            if (debug)
                System.out.println("Dumped class " + name);
        } catch (Exception e) {
            System.err.println("Unable to dump class " + name);
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
        }
        return null;
    }
}