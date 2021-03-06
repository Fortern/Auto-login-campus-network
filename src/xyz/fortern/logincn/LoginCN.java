package xyz.fortern.logincn;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class LoginCN {
	private static final Logger log = LogManager.getLogger(LoginCN.class);

	//获取一个请求的响应体
	private static String getResponse(String url) {
		StringBuilder result = new StringBuilder();
		try {
			URLConnection conn = new URL(url).openConnection();
			conn.setRequestProperty("accept", "*/*");
			conn.setRequestProperty("connection", "Keep-Alive");
			conn.setRequestProperty("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1");
			conn.connect();
			BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
			String line;
			while((line = in.readLine()) != null)
				result.append(line).append("\n");
			result.deleteCharAt(result.length()-1);//删除最后一个换行符
			return result.toString();
		} catch (ConnectException e ){
			log.error("连接超时，可能未连接至校园网！");
		} catch (IOException e) {
			log.error("IO错误！");
			e.printStackTrace();
		}
		return null;
	}

	//登录校园网
	public static String login(String urlWithParam) {
		/*
		  登录成功时的响应体:(以换行符为开头)
		  dr1004({"result":"1","msg":"\u8ba4\u8bc1\u6210\u529f"})
		  已经登录的情况下再次登录的响应体:
		  dr1004({"result":"0","msg":"","ret_code":2})
		  账号不存在时的响应体:
		  dr1004({"result":"0","msg":"dXNlcmlkIGVycm9yMQ==","ret_code":1})
		  密码错误:
		  dr1004({"result":"0","msg":"dXNlcmlkIGVycm9yMg==","ret_code":1})
		 */
		return getResponse(urlWithParam);
	}
	//获取局域网IP地址
	public static String getRealIP() {
		String result = getResponse("http://172.28.0.22/drcom/chkstatus?callback=dr1002");
		if(result == null){
			return null;
		}
		//,"v46ip":"10.130.76.243",
		int begin = result.indexOf("\"v46ip\"") + 8;
		begin = result.indexOf("\"",begin) + 1;
		return result.substring(begin, result.indexOf("\"",begin));
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
		String account = null;
		String password = null;
		String isp = null;
		try {
			//jar中的文件，只能以流的方式读取
			BufferedReader reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(LoginCN.class.getClassLoader().getResourceAsStream("user.txt"))));
			account = reader.readLine();
			password = reader.readLine();
			switch(reader.readLine()) {
				case "1": isp = "@telecom";break;
				case "2": isp = "@unicom";break;
			}
		} catch (Exception e) {
			log.error("无法读取用户数据,请检查后手动运行程序");
			e.printStackTrace();
			System.exit(0);
		}
		StringBuilder url = new StringBuilder("http://172.28.0.22:801/eportal/?c=Portal&a=login&callback=dr1004&login_method=1&user_account=")
				.append(account).append(isp).append("&user_password=").append(password).append("&wlan_user_ip=");
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
			log.info("IP地址为：" + ip);
			log.info("尝试发送登录请求");
			String result = login(url + ip);
			if(result == null){
				log.info("30秒后尝试重新登录");
				try {
					Thread.sleep(30000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				continue;
			}
			log.info("响应体：" + result);
			//如果返回登录成功或已经登录的消息，则检查一次网络连接
			if(result.charAt(41) == '2' || result.charAt(18) == '1') {
				while(isConnect()){
					log.info("已连接到Internet");
					try {
						Thread.sleep(180000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				log.warn("网络已断开，将重新登录");
				//continue;
			}else{
				log.error("登录失败，请检查登录信息是否正确，然后重启程序");
				System.exit(0);
			}
		}
	}
}
