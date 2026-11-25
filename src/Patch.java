import org.objectweb.asm.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class Patch implements Opcodes {
    static int count = 0;
    static void hit(String s) { count++; System.out.println("  patched: " + s); }

    static byte[] run(byte[] in, java.util.function.Function<ClassVisitor, ClassVisitor> f) {
        ClassReader cr = new ClassReader(in);
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cr.accept(f.apply(cw), 0);
        return cw.toByteArray();
    }

    /** canon(id) pattern: [id] GETSTATIC ly.aH ; GETFIELD ly.bc  ->  [canon(id)] 66 */
    static class RailId extends ClassVisitor {
        final String cls; boolean pend;
        RailId(ClassVisitor cv, String cls) { super(ASM9, cv); this.cls = cls; }
        public MethodVisitor visitMethod(int a, String n, String d, String s, String[] e) {
            return new MethodVisitor(ASM9, super.visitMethod(a, n, d, s, e)) {
                public void visitFieldInsn(int op, String o, String nm, String ds) {
                    if (op == GETSTATIC && o.equals("ly") && nm.equals("aH")) {
                        pend = true; super.visitMethodInsn(INVOKESTATIC, "Ext", "canon", "(I)I", false); return; }
                    if (pend && op == GETFIELD && o.equals("ly") && nm.equals("bc")) {
                        pend = false; super.visitIntInsn(BIPUSH, 66); hit(cls + "." + n + " rail id check"); return; }
                    super.visitFieldInsn(op, o, nm, ds);
                }
            };
        }
    }

    public static void main(String[] a) throws Exception {
        Path jar = Paths.get(a[0]), out = Paths.get(a[1]);
        Map<String, byte[]> files = new LinkedHashMap<>();
        // load extracted dir
        try (var st = Files.walk(jar)) { for (Path p : (Iterable<Path>) st.filter(Files::isRegularFile)::iterator) files.put(jar.relativize(p).toString().replace('\\', '/'), Files.readAllBytes(p)); }

        files.put("ly.class", run(files.get("ly.class"), cv -> new ClassVisitor(ASM9, cv) {
            public MethodVisitor visitMethod(int ac, String n, String d, String s, String[] e) {
                MethodVisitor mv = super.visitMethod(ac, n, d, s, e);
                if (!n.equals("<clinit>")) return mv;
                return new MethodVisitor(ASM9, mv) {
                    public void visitFieldInsn(int op, String o, String nm, String ds) {
                        super.visitFieldInsn(op, o, nm, ds);
                        // register right after the last vanilla block, before the item-for-block loop
                        if (op == PUTSTATIC && nm.equals("ba") && ds.equals("Lly;")) { super.visitMethodInsn(INVOKESTATIC, "Ext", "init", "()V", false); hit("ly.<clinit> register powered rails"); }
                    }
                };
            }
        }));

        files.put("dw.class", run(files.get("dw.class"), cv -> new ClassVisitor(ASM9, cv) {
            public MethodVisitor visitMethod(int ac, String n, String d, String s, String[] e) {
                MethodVisitor mv = super.visitMethod(ac, n, d, s, e);
                if (!n.equals("<init>")) return mv;
                return new MethodVisitor(ASM9, mv) {
                    public void visitInsn(int op) { if (op == RETURN) { super.visitVarInsn(ALOAD, 0); super.visitMethodInsn(INVOKESTATIC, "Ext", "recipes", "(Ldw;)V", false); hit("dw recipe"); } super.visitInsn(op); }
                    public void visitMethodInsn(int op, String o, String nm, String ds, boolean itf) {
                        if (op == INVOKESTATIC && o.equals("java/util/Collections") && nm.equals("sort") && ds.equals("(Ljava/util/List;Ljava/util/Comparator;)V")) { super.visitMethodInsn(INVOKESTATIC, "Ext", "sortList", ds, false); hit("dw safe sort"); return; }
                        super.visitMethodInsn(op, o, nm, ds, itf);
                    }
                };
            }
        }));

        files.put("jo.class", run(files.get("jo.class"), cv -> new RailId(cv, "jo")));

        // world renderer: chunk sorts can't throw any more (crash when switching Fancy/Fast graphics), dirty queue sorted less often
        files.put("e.class", run(files.get("e.class"), cv -> new ClassVisitor(ASM9, cv) {
            public MethodVisitor visitMethod(int ac, String n, String d, String s, String[] ex) {
                return new MethodVisitor(ASM9, super.visitMethod(ac, n, d, s, ex)) {
                    public void visitMethodInsn(int op, String o, String nm, String ds, boolean itf) {
                        if (op == INVOKESTATIC && o.equals("java/util/Collections") && nm.equals("sort") && ds.equals("(Ljava/util/List;Ljava/util/Comparator;)V")) { super.visitMethodInsn(INVOKESTATIC, "Ext", "sortDirty", ds, false); hit("e dirty chunk sort"); return; }
                        if (op == INVOKESTATIC && o.equals("java/util/Arrays") && nm.equals("sort") && ds.equals("([Ljava/lang/Object;Ljava/util/Comparator;)V")) { super.visitMethodInsn(INVOKESTATIC, "Ext", "sort", ds, false); hit("e." + n + " safe sort"); return; }
                        super.visitMethodInsn(op, o, nm, ds, itf);
                    }
                };
            }
        }));

        files.put("oc.class", run(files.get("oc.class"), cv -> new RailId(cv, "oc") {
            boolean prevE = false; boolean done = false;
            public MethodVisitor visitMethod(int ac, String n, String d, String s, String[] e) {
                MethodVisitor base = super.visitMethod(ac, n, d, s, e);
                if (!n.equals("e_")) return base;
                return new MethodVisitor(ASM9, base) {
                    boolean afterE = false;
                    public void visitMethodInsn(int op, String o, String nm, String ds, boolean itf) {
                        afterE = (!done && o.equals("cn") && nm.equals("e") && ds.equals("(III)I"));
                        super.visitMethodInsn(op, o, nm, ds, itf);
                    }
                    public void visitVarInsn(int op, int v) {
                        super.visitVarInsn(op, v);
                        if (afterE && op == ISTORE && v == 10) {
                            super.visitVarInsn(ALOAD, 0);
                            super.visitVarInsn(ALOAD, 0); super.visitFieldInsn(GETFIELD, "oc", "ag", "Lcn;");
                            super.visitVarInsn(ILOAD, 1); super.visitVarInsn(ILOAD, 2); super.visitVarInsn(ILOAD, 3); super.visitVarInsn(ILOAD, 10);
                            super.visitMethodInsn(INVOKESTATIC, "Ext", "cartRail", "(Loc;Lcn;IIII)V", false);
                            done = true; afterE = false; hit("oc.e_ powered rail boost");
                        } else if (op != ISTORE) afterE = afterE && false;
                    }
                };
            }
        }));

        files.put("mk.class", run(files.get("mk.class"), cv -> new ClassVisitor(ASM9, cv) {
            public MethodVisitor visitMethod(int ac, String n, String d, String s, String[] e) {
                return new MethodVisitor(ASM9, super.visitMethod(ac, n, d, s, e)) {
                    public void visitFieldInsn(int op, String o, String nm, String ds) {
                        if (op == GETFIELD && o.equals("if") && nm.equals("bc")) {
                            super.visitInsn(POP); super.visitMethodInsn(INVOKESTATIC, "Ext", "canon", "(I)I", false); super.visitIntInsn(BIPUSH, 66); hit("mk." + n + " neighbour check"); return; }
                        super.visitFieldInsn(op, o, nm, ds);
                    }
                    public void visitMethodInsn(int op, String o, String nm, String ds, boolean itf) {
                        if (op == INVOKEVIRTUAL && o.equals("cn") && nm.equals("b") && ds.equals("(IIII)V")) {
                            super.visitMethodInsn(INVOKESTATIC, "Ext", "setMeta", "(Lcn;IIII)V", false); hit("mk meta write"); return; }
                        super.visitMethodInsn(op, o, nm, ds, itf);
                    }
                };
            }
        }));

        files.put("fr.class", run(files.get("fr.class"), cv -> new ClassVisitor(ASM9, cv) {
            public MethodVisitor visitMethod(int ac, String n, String d, String s, String[] e) {
                MethodVisitor mv = super.visitMethod(ac, n, d, s, e);
                if (n.equals("<init>") && d.equals("(Lnet/minecraft/client/Minecraft;Ljava/io/File;)V")) {
                    return new MethodVisitor(ASM9, mv) {
                        public void visitInsn(int op) {
                            if (op == RETURN) {
                                super.visitVarInsn(ALOAD, 0); super.visitFieldInsn(GETFIELD, "fr", "A", "Ljava/io/File;");
                                super.visitMethodInsn(INVOKESTATIC, "Ext", "load", "(Ljava/io/File;)V", false); hit("fr load fov");
                            }
                            super.visitInsn(op);
                        }
                    };
                }
                if (n.equals("b") && d.equals("(I)I")) return new Pre(mv) { void code() { Label l = new Label(); mv.visitVarInsn(ILOAD, 1); mv.visitIntInsn(BIPUSH, 10); mv.visitJumpInsn(IF_ICMPNE, l); mv.visitInsn(ICONST_1); mv.visitInsn(IRETURN); mv.visitLabel(l); hit("fr.b(I)I"); } };
                if (n.equals("c") && d.equals("(I)F")) return new Pre(mv) { void code() { Label l = new Label(); mv.visitVarInsn(ILOAD, 1); mv.visitIntInsn(BIPUSH, 10); mv.visitJumpInsn(IF_ICMPNE, l); mv.visitMethodInsn(INVOKESTATIC, "Ext", "fovNorm", "()F", false); mv.visitInsn(FRETURN); mv.visitLabel(l); hit("fr.c(I)F"); } };
                if (n.equals("d") && d.equals("(I)Ljava/lang/String;")) return new Pre(mv) { void code() { Label l = new Label(); mv.visitVarInsn(ILOAD, 1); mv.visitIntInsn(BIPUSH, 10); mv.visitJumpInsn(IF_ICMPNE, l); mv.visitMethodInsn(INVOKESTATIC, "Ext", "fovLabel", "()Ljava/lang/String;", false); mv.visitInsn(ARETURN); mv.visitLabel(l); hit("fr.d(I)String"); } };
                if (n.equals("a") && d.equals("(IF)V")) return new Pre(mv) { void code() { Label l = new Label(); mv.visitVarInsn(ILOAD, 1); mv.visitIntInsn(BIPUSH, 10); mv.visitJumpInsn(IF_ICMPNE, l); mv.visitVarInsn(FLOAD, 2); mv.visitMethodInsn(INVOKESTATIC, "Ext", "setFovNorm", "(F)V", false); mv.visitInsn(RETURN); mv.visitLabel(l); hit("fr.a(IF)V"); } };
                return mv;
            }
        }));

        files.put("iq.class", run(files.get("iq.class"), cv -> new ClassVisitor(ASM9, cv) {
            public MethodVisitor visitMethod(int ac, String n, String d, String s, String[] e) {
                MethodVisitor mv = super.visitMethod(ac, n, d, s, e);
                if (!(n.equals("d") && d.equals("(F)F"))) return mv;
                return new MethodVisitor(ASM9, mv) {
                    boolean done = false;
                    public void visitLdcInsn(Object c) {
                        if (!done && c instanceof Float && ((Float) c) == 70.0F) { done = true; super.visitMethodInsn(INVOKESTATIC, "Ext", "fov", "()F", false); hit("iq.d FOV"); }
                        else super.visitLdcInsn(c);
                    }
                };
            }
        }));

        files.put("ay.class", run(files.get("ay.class"), cv -> new ClassVisitor(ASM9, cv) {
            public MethodVisitor visitMethod(int ac, String n, String d, String s, String[] e) {
                MethodVisitor mv = super.visitMethod(ac, n, d, s, e);
                if (!(n.equals("a") && d.equals("()V"))) return mv;
                return new MethodVisitor(ASM9, mv) {
                    int st = 0; boolean done = false;
                    public void visitFieldInsn(int op, String o, String nm, String ds) {
                        super.visitFieldInsn(op, o, nm, ds);
                        if (op == GETFIELD && o.equals("fr") && nm.equals("v")) { super.visitInsn(ICONST_1); super.visitInsn(IADD); hit("ay option count +1"); }
                    }
                    public void visitIntInsn(int op, int v) {
                        if (!done && op == BIPUSH && v == 120) st = 1;
                        else if (!done && st == 2 && op == BIPUSH && v == 12) { done = true; super.visitIntInsn(BIPUSH, 24); hit("ay Controls button moved"); return; }
                        else if (st != 1) st = 0;
                        super.visitIntInsn(op, v);
                    }
                    public void visitInsn(int op) { if (st == 1 && op == IADD) st = 2; else if (st != 0 && st != 2) st = 0; super.visitInsn(op); }
                };
            }
        }));

        files.put("lu.class", run(files.get("lu.class"), cv -> new ClassVisitor(ASM9, cv) {
            public MethodVisitor visitMethod(int ac, String n, String d, String s, String[] e) {
                MethodVisitor mv = super.visitMethod(ac, n, d, s, e);
                return new MethodVisitor(ASM9, mv) {
                    boolean done = false;
                    public void visitLdcInsn(Object c) {
                        if (c instanceof String && ((String) c).startsWith("Minecraft Alpha v1.1.2") && !((String) c).contains("(")) { super.visitLdcInsn(c); super.visitMethodInsn(INVOKESTATIC, "Ext", "title", "(Ljava/lang/String;)Ljava/lang/String;", false); hit("lu HUD title"); }
                        else super.visitLdcInsn(c);
                    }
                    public void visitMethodInsn(int op, String o, String nm, String ds, boolean itf) {
                        if (!done && op == INVOKESTATIC && o.equals("java/lang/Runtime") && nm.equals("getRuntime")) {
                            done = true;
                            super.visitVarInsn(ALOAD, 8);
                            super.visitVarInsn(ALOAD, 0); super.visitFieldInsn(GETFIELD, "lu", "g", "Lnet/minecraft/client/Minecraft;");
                            super.visitFieldInsn(GETFIELD, "net/minecraft/client/Minecraft", "g", "Lbi;");
                            super.visitFieldInsn(GETFIELD, "dm", "ak", "D");
                            super.visitVarInsn(ALOAD, 0); super.visitFieldInsn(GETFIELD, "lu", "g", "Lnet/minecraft/client/Minecraft;");
                            super.visitFieldInsn(GETFIELD, "net/minecraft/client/Minecraft", "g", "Lbi;");
                            super.visitFieldInsn(GETFIELD, "dm", "al", "D");
                            super.visitVarInsn(ALOAD, 0); super.visitFieldInsn(GETFIELD, "lu", "g", "Lnet/minecraft/client/Minecraft;");
                            super.visitFieldInsn(GETFIELD, "net/minecraft/client/Minecraft", "g", "Lbi;");
                            super.visitFieldInsn(GETFIELD, "dm", "am", "D");
                            super.visitMethodInsn(INVOKESTATIC, "Ext", "pos", "(DDD)Ljava/lang/String;", false);
                            super.visitInsn(ICONST_2); super.visitIntInsn(BIPUSH, 42); super.visitLdcInsn(16777215);
                            super.visitMethodInsn(INVOKEVIRTUAL, "kd", "a", "(Ljava/lang/String;III)V", false);
                            super.visitVarInsn(ALOAD, 8);
                            super.visitMethodInsn(INVOKESTATIC, "Ext", "fps", "()Ljava/lang/String;", false);
                            super.visitInsn(ICONST_2); super.visitIntInsn(BIPUSH, 52); super.visitLdcInsn(16777215);
                            super.visitMethodInsn(INVOKEVIRTUAL, "kd", "a", "(Ljava/lang/String;III)V", false);
                            hit("lu F3 coordinates + fps");
                        }
                        super.visitMethodInsn(op, o, nm, ds, itf);
                    }
                };
            }
        }));

        files.put("bi.class", run(files.get("bi.class"), cv -> new ClassVisitor(ASM9, cv) {
            public MethodVisitor visitMethod(int ac, String n, String d, String s, String[] e) {
                MethodVisitor mv = super.visitMethod(ac, n, d, s, e);
                if (!(n.equals("j") && d.equals("()V"))) return mv;
                return new MethodVisitor(ASM9, mv) {
                    public void visitMethodInsn(int op, String o, String nm, String ds, boolean itf) {
                        super.visitMethodInsn(op, o, nm, ds, itf);
                        if (o.equals("lv") && nm.equals("a") && ds.equals("(Ldm;)V")) {
                            super.visitVarInsn(ALOAD, 0); super.visitMethodInsn(INVOKESTATIC, "Ext", "tick", "(Lbi;)V", false); hit("bi.j creative tick"); }
                    }
                };
            }
        }));

        files.put("ge.class", run(files.get("ge.class"), cv -> new ClassVisitor(ASM9, cv) {
            public MethodVisitor visitMethod(int ac, String n, String d, String s, String[] e) {
                return new MethodVisitor(ASM9, super.visitMethod(ac, n, d, s, e)) {
                    public void visitLdcInsn(Object c) {
                        if (c instanceof Double && ((Double) c) == 0.08D) { super.visitVarInsn(ALOAD, 0); super.visitMethodInsn(INVOKESTATIC, "Ext", "grav", "(Lge;)D", false); hit("ge gravity"); }
                        else super.visitLdcInsn(c);
                    }
                };
            }
        }));

        files.put("dm.class", run(files.get("dm.class"), cv -> new ClassVisitor(ASM9, cv) {
            public MethodVisitor visitMethod(int ac, String n, String d, String s, String[] e) {
                MethodVisitor mv = super.visitMethod(ac, n, d, s, e);
                if (n.equals("a") && d.equals("(Lkh;I)Z")) return new Pre(mv) { void code() { Label l = new Label(); mv.visitVarInsn(ALOAD, 0); mv.visitMethodInsn(INVOKESTATIC, "Ext", "noHurt", "(Ldm;)Z", false); mv.visitJumpInsn(IFEQ, l); mv.visitInsn(ICONST_0); mv.visitInsn(IRETURN); mv.visitLabel(l); hit("dm no damage"); } };
                if (n.equals("a") && d.equals("(Lly;)F")) return new Pre(mv) { void code() { Label l = new Label(); mv.visitFieldInsn(GETSTATIC, "Ext", "creative", "Z"); mv.visitJumpInsn(IFEQ, l); mv.visitLdcInsn(10000.0F); mv.visitInsn(FRETURN); mv.visitLabel(l); hit("dm instant break"); } };
                return mv;
            }
        }));

        files.put("net/minecraft/client/Minecraft.class", run(files.get("net/minecraft/client/Minecraft.class"), cv -> new ClassVisitor(ASM9, cv) {
            public MethodVisitor visitMethod(int ac, String n, String d, String s, String[] e) {
                return new MethodVisitor(ASM9, super.visitMethod(ac, n, d, s, e)) {
                    boolean afterKey = false, f4 = false;
                    public void visitMethodInsn(int op, String o, String nm, String ds, boolean itf) {
                        super.visitMethodInsn(op, o, nm, ds, itf);
                        afterKey = (op == INVOKESTATIC && o.equals("org/lwjgl/input/Keyboard") && nm.equals("getEventKey"));
                        if (op == INVOKESPECIAL && o.equals("lo") && nm.equals("<init>")) {
                            super.visitMethodInsn(INVOKESTATIC, "Ext", "screen", "(Lbh;)Lbh;", false); hit("Minecraft inventory screen"); }
                    }
                    public void visitIntInsn(int op, int v) {
                        if (afterKey && !f4 && op == BIPUSH && v == 63) {
                            f4 = true; Label l = new Label();
                            super.visitInsn(DUP); super.visitIntInsn(BIPUSH, 62); super.visitJumpInsn(IF_ICMPNE, l);
                            super.visitMethodInsn(INVOKESTATIC, "Ext", "toggleCreative", "()V", false); super.visitLabel(l);
                            hit("Minecraft F4 toggle");
                        }
                        afterKey = false;
                        super.visitIntInsn(op, v);
                    }
                    public void visitInsn(int op) { afterKey = false; super.visitInsn(op); }
                    public void visitVarInsn(int op, int v) { afterKey = false; super.visitVarInsn(op, v); }
                    public void visitJumpInsn(int op, Label l) { afterKey = false; super.visitJumpInsn(op, l); }
                };
            }
        }));

        // title screen: Singleplayer opens WorldsScreen instead of jq, and entering the title screen leaves the world
        files.put("cx.class", run(files.get("cx.class"), cv -> new ClassVisitor(ASM9, cv) {
            public MethodVisitor visitMethod(int ac, String n, String d, String s, String[] e) {
                MethodVisitor base = super.visitMethod(ac, n, d, s, e);
                if (n.equals("a") && d.equals("()V")) base = new Pre(base) { void code() { mv.visitMethodInsn(INVOKESTATIC, "Ext", "leaveWorld", "()V", false); hit("cx.a leave world"); } };
                if (n.equals("a") && d.equals("(Lfk;)V")) base = new Pre(base) { void code() { Label l = new Label(); mv.visitVarInsn(ALOAD, 0); mv.visitVarInsn(ALOAD, 1); mv.visitMethodInsn(INVOKESTATIC, "SkinScreen", "titleButton", "(Lbh;Lfk;)Z", false); mv.visitJumpInsn(IFEQ, l); mv.visitInsn(RETURN); mv.visitLabel(l); hit("cx Change Skin button"); } };
                final boolean init = n.equals("a") && d.equals("()V");
                return new MethodVisitor(ASM9, base) {
                    public void visitInsn(int op) {
                        if (init && op == RETURN) { super.visitVarInsn(ALOAD, 0); super.visitMethodInsn(INVOKESTATIC, "SkinScreen", "titleInit", "(Lbh;)V", false); hit("cx title buttons"); }
                        super.visitInsn(op);
                    }
                    public void visitTypeInsn(int op, String t) {
                        if (op == NEW && t.equals("jq")) { t = "WorldsScreen"; hit("cx new WorldsScreen"); }
                        super.visitTypeInsn(op, t);
                    }
                    public void visitMethodInsn(int op, String o, String nm, String ds, boolean itf) {
                        if (op == INVOKESPECIAL && o.equals("jq") && nm.equals("<init>")) o = "WorldsScreen";
                        super.visitMethodInsn(op, o, nm, ds, itf);
                    }
                };
            }
        }));

        // container screens: shift click (craft all / move stack) and right click drag
        files.put("ee.class", run(files.get("ee.class"), cv -> new ClassVisitor(ASM9, cv) {
            public MethodVisitor visitMethod(int ac, String n, String d, String s, String[] e) {
                MethodVisitor mv = super.visitMethod(ac, n, d, s, e);
                if (n.equals("a") && d.equals("(III)V")) return new Pre(mv) { void code() { Label l = new Label(); mv.visitVarInsn(ALOAD, 0); mv.visitVarInsn(ILOAD, 1); mv.visitVarInsn(ILOAD, 2); mv.visitVarInsn(ILOAD, 3); mv.visitMethodInsn(INVOKESTATIC, "Inv", "click", "(Lee;III)Z", false); mv.visitJumpInsn(IFEQ, l); mv.visitInsn(RETURN); mv.visitLabel(l); hit("ee shift click"); } };
                if (n.equals("a") && d.equals("(IIF)V")) return new Pre(mv) { void code() { mv.visitVarInsn(ALOAD, 0); mv.visitVarInsn(ILOAD, 1); mv.visitVarInsn(ILOAD, 2); mv.visitMethodInsn(INVOKESTATIC, "Inv", "frame", "(Lee;II)V", false); hit("ee right drag"); } };
                return mv;
            }
        }));

        // skins: the minecraft.net skin server is gone; own player = .minecraft/skin.png, others = the recreatecraft server
        String[][] skinCls = { { "bi", "localSkinUrl" }, { "nt", "skinUrl" } };
        for (String[] sc : skinCls) {
            files.put(sc[0] + ".class", run(files.get(sc[0] + ".class"), cv -> new ClassVisitor(ASM9, cv) {
                public MethodVisitor visitMethod(int ac, String n, String d, String s, String[] e) {
                    MethodVisitor mv = super.visitMethod(ac, n, d, s, e);
                    if (!n.equals("<init>")) return mv;
                    return new MethodVisitor(ASM9, mv) {
                        public void visitInsn(int op) {
                            if (op == RETURN) {
                                super.visitVarInsn(ALOAD, 0); super.visitVarInsn(ALOAD, 0); super.visitFieldInsn(GETFIELD, "dm", "i", "Ljava/lang/String;");
                                super.visitMethodInsn(INVOKESTATIC, "SkinScreen", sc[1], "(Ljava/lang/String;)Ljava/lang/String;", false);
                                super.visitFieldInsn(PUTFIELD, "kh", "aY", "Ljava/lang/String;"); hit(sc[0] + " skin url");
                            }
                            super.visitInsn(op);
                        }
                    };
                }
            }));
        }
        files.put("kz.class", run(files.get("kz.class"), cv -> new ClassVisitor(ASM9, cv) {
            public MethodVisitor visitMethod(int ac, String n, String d, String s, String[] e) {
                MethodVisitor mv = super.visitMethod(ac, n, d, s, e);
                if (!(n.equals("run") && d.equals("()V"))) return mv;
                return new Pre(mv) { void code() { Label l = new Label(); mv.visitVarInsn(ALOAD, 0); mv.visitFieldInsn(GETFIELD, "kz", "a", "Ljava/lang/String;"); mv.visitVarInsn(ALOAD, 0); mv.visitFieldInsn(GETFIELD, "kz", "b", "Lhb;"); mv.visitVarInsn(ALOAD, 0); mv.visitFieldInsn(GETFIELD, "kz", "c", "Lcc;");
                    mv.visitMethodInsn(INVOKESTATIC, "SkinScreen", "loadLocal", "(Ljava/lang/String;Lhb;Lcc;)Z", false); mv.visitJumpInsn(IFEQ, l); mv.visitInsn(RETURN); mv.visitLabel(l); hit("kz local skin file"); } };
            }
        }));

        // multiplayer: new Socket(InetAddress.getByName(host), port) -> Ext.connect(host, port, mc) (HTTP tunnel for wasmer.app servers)
        files.put("gy.class", run(files.get("gy.class"), cv -> new ClassVisitor(ASM9, cv) {
            public MethodVisitor visitMethod(int ac, String n, String d, String s, String[] e) {
                MethodVisitor mv = super.visitMethod(ac, n, d, s, e);
                if (!n.equals("<init>")) return mv;
                return new MethodVisitor(ASM9, mv) {
                    boolean skipDup = false;
                    public void visitTypeInsn(int op, String t) { if (op == NEW && t.equals("java/net/Socket")) { skipDup = true; return; } super.visitTypeInsn(op, t); }
                    public void visitInsn(int op) { if (skipDup && op == DUP) { skipDup = false; return; } super.visitInsn(op); }
                    public void visitMethodInsn(int op, String o, String nm, String ds, boolean itf) {
                        if (op == INVOKESTATIC && o.equals("java/net/InetAddress") && nm.equals("getByName")) return;
                        if (op == INVOKESPECIAL && o.equals("java/net/Socket") && nm.equals("<init>")) {
                            super.visitVarInsn(ALOAD, 1);
                            super.visitMethodInsn(INVOKESTATIC, "Ext", "connect", "(Ljava/lang/String;ILnet/minecraft/client/Minecraft;)Ljava/net/Socket;", false); hit("gy connect"); return; }
                        super.visitMethodInsn(op, o, nm, ds, itf);
                    }
                };
            }
        }));
        // multiplayer screen: server address up to 64 characters (was 32), long enough for name.wasmer.app
        files.put("gc.class", run(files.get("gc.class"), cv -> new ClassVisitor(ASM9, cv) {
            public MethodVisitor visitMethod(int ac, String n, String d, String s, String[] e) {
                MethodVisitor mv = super.visitMethod(ac, n, d, s, e);
                if (n.equals("a") && d.equals("(Lfk;)V")) return new MethodVisitor(ASM9, mv) {
                    public void visitFieldInsn(int op, String o, String nm, String ds) {
                        super.visitFieldInsn(op, o, nm, ds);
                        if (op == GETFIELD && o.equals("gc") && nm.equals("i") && ds.equals("Ljava/lang/String;")) {
                            super.visitMethodInsn(INVOKESTATIC, "Ext", "cleanAddress", "(Ljava/lang/String;)Ljava/lang/String;", false); hit("gc clean address"); }
                    }
                };
                if (!(n.equals("a") && d.equals("(CI)V"))) return mv;
                return new MethodVisitor(ASM9, mv) {
                    public void visitIntInsn(int op, int v) { if (op == BIPUSH && v == 32) { v = 64; hit("gc address length"); } super.visitIntInsn(op, v); }
                };
            }
        }));

        // optional 3rd arg: textures dir -> powered rail tiles into terrain.png (tile 129 = off, 130 = on)
        if (a.length > 2) {
            var t = javax.imageio.ImageIO.read(new ByteArrayInputStream(files.get("terrain.png")));
            var img = new java.awt.image.BufferedImage(t.getWidth(), t.getHeight(), java.awt.image.BufferedImage.TYPE_INT_ARGB);
            img.getGraphics().drawImage(t, 0, 0, null);
            int[][] tiles = { { 129, 0 }, { 130, 1 } };
            String[] names = { "powered_rail_off_16x16.png", "powered_rail_on_16x16.png" };
            for (int[] tl : tiles) {
                var tex = javax.imageio.ImageIO.read(Paths.get(a[2], names[tl[1]]).toFile());
                int x0 = (tl[0] % 16) * 16, y0 = (tl[0] / 16) * 16;
                for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) img.setRGB(x0 + x, y0 + y, tex.getRGB(x, y));
            }
            var bo = new ByteArrayOutputStream(); javax.imageio.ImageIO.write(img, "png", bo);
            files.put("terrain.png", bo.toByteArray()); hit("terrain.png powered rail tiles");
        }
        files.keySet().removeIf(k -> k.startsWith("META-INF") && (k.endsWith(".SF") || k.endsWith(".DSA")));

        // write jar contents dir
        for (var en : files.entrySet()) { Path p = out.resolve(en.getKey()); Files.createDirectories(p.getParent()); Files.write(p, en.getValue()); }
        System.out.println("total patches: " + count);
    }

    static abstract class Pre extends MethodVisitor {
        final MethodVisitor mv;
        Pre(MethodVisitor mv) { super(ASM9, mv); this.mv = mv; }
        abstract void code();
        public void visitCode() { super.visitCode(); code(); }
    }
}
