package mods.tesseract.dragonflyjs;

import jdk.nashorn.api.scripting.ScriptObjectMirror;

import java.util.ArrayList;

public class JSProxy {
    public static ArrayList<ScriptObjectMirror> cachedFunctions = new ArrayList<>();

    public static Object invokeJSAll(int method, Object... a) {
        return cachedFunctions.get(method).call(null, a);
    }

    public static Object invokeJS(int method, Object a0) {
        return invokeJSAll(method, a0);
    }

    public static Object invokeJS(int method, Object a0, Object a1) {
        return invokeJSAll(method, a0, a1);
    }

    public static Object invokeJS(int method, Object a0, Object a1, Object a2) {
        return invokeJSAll(method, a0, a1, a2);
    }

    public static Object invokeJS(int method, Object a0, Object a1, Object a2, Object a3) {
        return invokeJSAll(method, a0, a1, a2, a3);
    }

    public static Object invokeJS(int method, Object a0, Object a1, Object a2, Object a3, Object a4) {
        return invokeJSAll(method, a0, a1, a2, a3, a4);
    }

    public static Object invokeJS(int method, Object a0, Object a1, Object a2, Object a3, Object a4, Object a5) {
        return invokeJSAll(method, a0, a1, a2, a3, a4, a5);
    }

    public static Object invokeJS(int method, Object a0, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6) {
        return invokeJSAll(method, a0, a1, a2, a3, a4, a5, a6);
    }

    public static Object invokeJS(int method, Object a0, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8) {
        return invokeJSAll(method, a0, a1, a2, a3, a4, a5, a6, a7, a8);
    }

    public static Object invokeJS(int method, Object a0, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9) {
        return invokeJSAll(method, a0, a1, a2, a3, a4, a5, a6, a7, a8, a9);
    }

    public static Object invokeJS(int method, Object a0, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10) {
        return invokeJSAll(method, a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10);
    }

    public static Object invokeJS(int method, Object a0, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11) {
        return invokeJSAll(method, a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11);
    }

    public static Object invokeJS(int method, Object a0, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12) {
        return invokeJSAll(method, a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12);
    }

    public static Object invokeJS(int method, Object a0, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13) {
        return invokeJSAll(method, a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13);
    }

    public static Object invokeJS(int method, Object a0, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14) {
        return invokeJSAll(method, a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14);
    }

    public static Object invokeJS(int method, Object a0, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15) {
        return invokeJSAll(method, a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15);
    }

    public static Object invokeJS(int method, Object a0, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16) {
        return invokeJSAll(method, a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16);
    }
}
