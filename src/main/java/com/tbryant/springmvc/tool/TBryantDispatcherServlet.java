package com.tbryant.springmvc.tool;

import com.tbryant.springmvc.annotation.TBryantAutowired;
import com.tbryant.springmvc.annotation.TBryantController;
import com.tbryant.springmvc.annotation.TBryantRequestMapping;
import com.tbryant.springmvc.annotation.TBryantService;
import com.tbryant.springmvc.domain.TBryantHandler;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TBryantDispatcherServlet extends HttpServlet {

	@Override
	public void init(ServletConfig config) throws ServletException {
		//1.加载配置文件
		doLoadConfig(config.getInitParameter("contextConfigLocation"));
		//2.扫描package中所有文件，把className存入classNames
		doScanner("com.tbryant.springmvc");
		//3.初始化classNames中所有类，存入IOC容器（beanName对应bean）
		doInstance();
		//4.依赖注入
		doAutowired();
		//5.初始化HandlerMapping（url对应method）
		initHandlerMapping();

		System.out.println("TBryantSpringMVC init over");
	}

	//1.加载配置文件
	private void doLoadConfig(String contextConfigLocation) {
		//1.
		InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation);

		//2.加载配置文件

	}

	private List<String> classNames = new ArrayList<>();

	//2.扫描package中所有文件，把className存入classNames
	private void doScanner(String packageName) {
		URL url = this.getClass().getClassLoader().getResource("/" + packageName.replaceAll("\\.", "/"));
		//获取所有字节码文件
		File classDir = new File(url.getFile());
		for (File file : classDir.listFiles()) {
			if (file.isDirectory()) {
				doScanner(packageName + "." + file.getName());
			} else {
				classNames.add(packageName + "." + file.getName().replace(".class", ""));
			}
		}
	}

	private Map<String, Object> ioc = new HashMap<>();

	//3.初始化classNames中所有类，存入IOC容器（beanName对应bean）
	private void doInstance() {
		try {
			if (classNames.isEmpty()) {
				return;
			}
			for (String className : classNames) {
				Class<?> type = Class.forName(className);
				if (type.isAnnotationPresent(TBryantController.class)) {
					//@Controller注解的类
					ioc.put(StringUtils.uncapitalize(type.getSimpleName()), type.newInstance());
				} else if (type.isAnnotationPresent(TBryantService.class)) {
					//@Service注解的类
					Object instance = type.newInstance();
					ioc.put(StringUtils.uncapitalize(type.getSimpleName()), instance);
				} else {
					continue;
				}
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	//4.依赖注入
	private void doAutowired() {
		if (ioc.isEmpty()) {
			return;
		}
		for (Map.Entry<String, Object> entry : ioc.entrySet()) {
			//获取类种所有属性
			Field[] fields = entry.getValue().getClass().getDeclaredFields();
			for (Field field : fields) {
				//找@Autowired注解的属性
				if (!field.isAnnotationPresent(TBryantAutowired.class)) {
					continue;
				}
				TBryantAutowired tbryantAutowired = field.getAnnotation(TBryantAutowired.class);
				String beanName = tbryantAutowired.value().trim();
				if ("".equals(beanName)) {
					beanName = StringUtils.uncapitalize(field.getType().getSimpleName());
				}
				//暴力反射
				field.setAccessible(true);
				try {
					field.set(entry.getValue(), ioc.get(beanName));
					System.out.println(entry.getValue() + " is autowired ,object is " + ioc.get(beanName));
				} catch (Exception e) {
					e.printStackTrace();
					continue;
				}
			}
		}
	}

	private Map<String, Method> handlerMapping = new HashMap<>();

	//5.初始化HandlerMapping（url对应method）
	private void initHandlerMapping() {
		if (ioc.isEmpty()) {
			return;
		}
		for (Map.Entry<String, Object> entry : ioc.entrySet()) {
			Class<?> clazz = entry.getValue().getClass();
			//HandlerMapping只认TBryantController
			if (!clazz.isAnnotationPresent(TBryantController.class)) {
				continue;
			}
			String url = "";
			if (clazz.isAnnotationPresent(TBryantRequestMapping.class)) {
				TBryantRequestMapping jRequestMapping = clazz.getAnnotation(TBryantRequestMapping.class);
				url = jRequestMapping.value().trim();
			}
			Method[] methods = clazz.getMethods();
			for (Method method : methods) {
				//如果没有JRequestMapping ,直接跳过
				if (!method.isAnnotationPresent(TBryantRequestMapping.class)) {
					continue;
				}
				TBryantRequestMapping tbryantRequestMapping = method.getAnnotation(TBryantRequestMapping.class);
				String murl = url + tbryantRequestMapping.value().trim();
				TBryantHandler tbryantHandler = new TBryantHandler();
				tbryantHandler.setController(entry.getValue());
				tbryantHandler.setMethod(method);
				handlerMapping.put(murl, tbryantHandler);
				System.out.println("Mapping : " + murl + "  " + tbryantHandler);
			}
		}
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		this.doPost(req, resp);
		System.out.println("TBryantSpringMVC doGet over");
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try {
			doDispatch(req, resp);
		} catch () {
			resp.getWriter().write("500");
		}
		System.out.println("TBryantSpringMVC doPost over");
	}

	private void doDispatch(HttpServletRequest req, HttpServletResponse resp) {
		try {
			if (handlerMapping.isEmpty()) {
				return;
			}
			String url = req.getRequestURI();
			String contextPath = req.getContextPath();
			url = url.replace(contextPath, "").replaceAll("/+", "/");
			if (!handlerMapping.containsKey(url)) {
				resp.getWriter().write("404");
			}
			Method method = handlerMapping.get(url);
			//获取方法参数列表
			Class<?>[] parameterTypes = method.getParameterTypes();
			//获取请求参数
			Map<String, String[]> parameterMap = req.getParameterMap();


		} catch (Exception e) {
		}
	}
}
