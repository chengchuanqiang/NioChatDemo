package com.mychat;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Properties;

/**
 * 文件读取
 * @author ccq
 *
 */
public class PropertyFactory {
	static Properties pops = new Properties();
	static Properties vpops = new Properties();
	static Properties errorPops = new Properties();
	static {
		try {
			pops.load(PropertyFactory.class.getClassLoader().getResourceAsStream("system.properties"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private PropertyFactory() {
	}; 

	public static String getProperty(String key) {
		return pops.getProperty(key);
	}
	
	public static void writeVaule(String key,String value){
		Properties prop = new Properties();     
	     try{
	         //读取属性文件a.properties
	         InputStream in = PropertyFactory.class.getClassLoader().getResourceAsStream("system.properties");
	         prop.load(in);     ///加载属性列表
	         Iterator<String> it=prop.stringPropertyNames().iterator();
	         while(it.hasNext()){
	             String k=it.next();
	             System.out.println(k+":"+prop.getProperty(k));
	         }
	         in.close();
	         ///保存属性到b.properties文件
	         FileOutputStream oFile = new FileOutputStream(PropertyFactory.class.getClassLoader().getResource("/system.properties").getFile());//true表示追加打开
	         prop.setProperty(key, value);
	         prop.store(oFile, null);
	         oFile.close();
	         pops.load(PropertyFactory.class.getClassLoader().getResourceAsStream("system.properties"));
	     }
	     catch(Exception e){
	         e.printStackTrace();
	     }
    }  
	
}
