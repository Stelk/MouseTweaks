package yalter.mousetweaks;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;

public class ReflectionCache {
	private HashMap<String, Class> classes = new HashMap<String, Class>();
	private HashMap<String, Method> methods = new HashMap<String, Method>();
	private HashMap<String, Field> fields = new HashMap<String, Field>();

	public boolean isInstance(Object obj, String name) {
		Class clazz = classes.get(name);
		return clazz.isInstance(obj);
	}

	public Object invokeMethod(Object obj, String name, Object... args) throws InvocationTargetException {
		Method method = methods.get(name);

		try {
			return method.invoke(obj, args);
		} catch (IllegalAccessException e) {
			CrashReport crashreport = CrashReport.create(e, "Invoking method in MouseTweaks' reflection");
			throw new CrashException(crashreport);
		}
	}

	public Object getFieldValue(Object obj, String name) {
		Field field = fields.get(name);

		try {
			return field.get(obj);
		} catch (IllegalAccessException e) {
			CrashReport crashreport = CrashReport.create(e, "Getting field value in MouseTweaks' reflection");
			throw new CrashException(crashreport);
		}
	}

	public void setFieldValue(Object obj, String name, Object value) {
		Field field = fields.get(name);

		try {
			field.set(obj, value);
		} catch (IllegalAccessException e) {
			CrashReport crashreport = CrashReport.create(e, "Setting field value in MouseTweaks' reflection");
			throw new CrashException(crashreport);
		}
	}

	void storeClass(String name, Class clazz) {
		classes.put(name, clazz);
	}

	void storeMethod(String name, Method method) {
		methods.put(name, method);
	}

	void storeField(String name, Field field) {
		fields.put(name, field);
	}
}
