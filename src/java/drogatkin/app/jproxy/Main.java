package drogatkin.app.jproxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Main extends HttpServlet {
	
	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String proxyTo = req.getPathInfo();
		URL toURL = null;
		try {
			toURL = new URL(proxyTo.substring(1)); // remove slash
			URL toURL2 = toURL;
			HttpURLConnection con = (HttpURLConnection) toURL.openConnection();
			String met = req.getMethod();
			con.setDoOutput("POST".equalsIgnoreCase(met));
			con.setDoInput(true);
			con.setRequestMethod(met);
			con.setInstanceFollowRedirects(false);
			Enumeration<String> hdrs =req.getHeaderNames();
			while(hdrs.hasMoreElements()) {
				String n = hdrs.nextElement();
				if ("host".equals(n)) con.setRequestProperty(n, toURL2.getHost());
				else {
					Enumeration<String> vs = req.getHeaders(n);
					while(vs.hasMoreElements()) {
						con.addRequestProperty(n, vs.nextElement());
					}
				}				
			}
			if ("POST".equalsIgnoreCase(met)) {
				String contentType = req.getContentType();
				if (contentType != null && contentType.toLowerCase().indexOf("multipart/form-data") >= 0) {
					copy(req.getInputStream(), con.getOutputStream());
				} else {
				    OutputStream os = con.getOutputStream();				    
					req.getParameterMap().forEach((String n, String[] vs) -> {
						try {
					 for(String v:vs) {os.write(URLEncoder.encode(n).getBytes("UTF-8")); os.write("=".getBytes("UTF-8")); os.write(URLEncoder.encode(v).getBytes("UTF-8")); os.write("&".getBytes("UTF-8"));}					 
						}catch(Exception e) {throw new RuntimeException(e);} });
					os.write("\r\n".getBytes("UTF-8")); os.flush();
				}
					
			}
			resp.setStatus(con.getResponseCode(), con.getResponseMessage());
			//InputStream cis = con.getInputStream();
			con.getHeaderFields().forEach((String n,List<String> vs)->{
				if (n != null)
					vs.forEach((String v)->{boolean oneHdr = false; switch(n.toLowerCase()) { case "keep-alive": v=null;break; case "location": v=adjustLocation(v, req); oneHdr = true; break; case "connection": v= "close"; oneHdr = true; break;} if (oneHdr) resp.setHeader(n, v); else resp.addHeader(n, v); /*log(String.format("Add header :%s=%s%n", n,v));*/});
			});
			copy(con.getInputStream(), resp.getOutputStream());			
		} catch(Exception e) {
			e.printStackTrace();
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Request "+toURL+" was processed with "+e);
		}
	}

	public static void main(String[] args) {
		System.out.printf("JProxy (c) 2014 D Rogatkin%n");
	}
	
	static String adjustLocation(String l, HttpServletRequest req) {
		return req.getScheme()+"://"+req.getServerName()+":"+req.getServerPort()+req.getServletPath()+"/"+l;
	}
			
	static void copy(InputStream is, OutputStream os) throws IOException {
		byte[] b = new byte[16*1024];
		do {
			int l = is.read(b);
			//if (l > 0)
				//System.err.print(new String(b, 0, l));
			if (l > 0)
				os.write(b, 0, l);
			else break;
		} while(true);
	}

}
