package xyz.fortern.logincn;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Scanner;

public class LoginCN {
	private static final Logger log = LogManager.getLogger(LoginCN.class);
	//登录校园网
	public static String login(String urlWithParam) {
		StringBuilder result = new StringBuilder();
		try {
			URL realUrl = new URL(urlWithParam);
			URLConnection conn = realUrl.openConnection();
			conn.setRequestProperty("accept", "*/*");
			conn.setRequestProperty("connection", "Keep-Alive");
			conn.setRequestProperty("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1");
			conn.connect();
			BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
			String line;
			while((line = in.readLine()) != null)
				result.append("\n").append(line);
			return result.toString();
		} catch (ConnectException e) {
			log.error("连接超时，请检查是否连接到校园网");
		} catch (Exception e) {
			e.printStackTrace();
		}
			/*
			//获取响应头不是必要的
			Map<String, List<String>> map = conn.getHeaderFields();
			for(String key: map.keySet()) {
				System.out.println(key + "--->" + map.get(key));
			}
			*/
			/*获取响应体
			  登录成功时的响应体
			  dr1004({"result":"1","msg":"\u8ba4\u8bc1\u6210\u529f"})
			  已经登录的情况下再次登录的响应体
			  dr1004({"result":"0","msg":"","ret_code":2})
			*/
		return null;
	}
	//获取局域网IP地址
	public static String getRealIP() {
		try {
			//获取到所有的网卡
			Enumeration<NetworkInterface> allNetInterfaces = NetworkInterface.getNetworkInterfaces();
			while (allNetInterfaces.hasMoreElements()) {
				NetworkInterface netInterface = allNetInterfaces.nextElement();
				// 去除回环接口127.0.0.1，子接口，未运行的接口
				if (netInterface.isLoopback() || netInterface.isVirtual() || !netInterface.isUp()) {
					continue;
				}
				//获取名称中是否包含 Intel Realtek 的网卡
				if (!netInterface.getDisplayName().contains("Intel")
						&& !netInterface.getDisplayName().contains("Realtek")
						&& !netInterface.getDisplayName().contains("Atheros")
						&& !netInterface.getDisplayName().contains("Broadcom")) {
					continue;
				}
				log.info("DisplayName: " + netInterface.getDisplayName());
				Enumeration<InetAddress> addresses = netInterface.getInetAddresses();
				while (addresses.hasMoreElements()) {
					InetAddress ip = addresses.nextElement();
					if (ip instanceof Inet4Address) {
						log.info("HostAddress: " + ip.getHostAddress());
						return ip.getHostAddress();
					}
				}
				break;
			}
		} catch (SocketException e) {
			e.printStackTrace();
		}
		return null;
	}
	//检测网络连接
	public static boolean isConnect(){
		Runtime runtime = Runtime.getRuntime();
		Process process;
		try {
			process = runtime.exec("ping " + "www.baidu.com");
			InputStream in = process.getInputStream();
			InputStreamReader inReader = new InputStreamReader(in,"GBK");
			BufferedReader br = new BufferedReader(inReader);
			String line;
			StringBuilder sb = new StringBuilder();
			while ((line = br.readLine()) != null) {
				sb.append(line);
			}
			log.info(sb);
			in.close();
			inReader.close();
			br.close();
			if (!sb.toString().equals("") && sb.toString().indexOf("TTL") > 0) {
				return true;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}
	public static void main(String[] args) {
		log.info("--- 程序启动 ---");
		Scanner input = null;
		String path;
		try {
			path = LoginCN.class.getClassLoader().getResource("user.txt").getPath();
			path = URLDecoder.decode(path, "utf-8");
			input = new Scanner(new File(path));
		} catch (Exception e) {
			log.error("无法读取用户数据,请检查后手动运行程序");
			e.printStackTrace();
			System.exit(0);
		}
		String account = input.nextLine();
		String password = input.nextLine();
		String isp = "";
		switch(input.nextLine()) {
			case "1": isp = "@telecom";break;
			case "2": isp = "@unicom";break;
		}
		String surl = "http://172.28.0.22:801/eportal/?c=Portal&a=login&callback=dr1004&login_method=1&user_account=" +
				account + isp + "&user_password=" + password + "&wlan_user_ip=";
		while(true){
			log.info("尝试获取ip地址");
			String ip = getRealIP();
			if(ip == null){
				log.error("获取局域网IP失败，请检查网络连接");
				log.error("30秒后重试...");
				try {
					Thread.sleep(30000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				continue;
			}
			log.info("尝试发送登录请求");
			String s = login(surl + ip);
			if(s==null){
				log.info("30秒后尝试重新登录");
				try {
					Thread.sleep(30000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				continue;
			}
			log.info("响应体：" + s);
			switch(s.charAt(18)){
				case '1':
				case '2':
					while(isConnect()){
						try {
							Thread.sleep(180000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}continue; //跳转到while的开头
				case '3':
					log.error("登录失败，请检查登录信息是否正确，然后重启程序");
					System.exit(0);
			}
		}
	}
}
