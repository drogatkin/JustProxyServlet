package drogatkin.app.jproxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
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
		URL toURL = null;
		try {
			toURL = getForwardURL(req); // remove slash
			if (toURL == null) {
				resp.sendError(resp.SC_USE_PROXY, "Proxy forward request wasn't specified");
				return;
			}
			URL toURL2 = toURL;
			HttpURLConnection con = (HttpURLConnection) toURL2.openConnection();
			//URL.setURLStreamHandlerFactory(arg0);
			String met = req.getMethod();
			con.setDoOutput("POSTLOCKPROPFINDPUT".indexOf(met) >= 0);
			con.setDoInput(true);
			try {
				con.setRequestMethod(met);
			} catch (IOException io) {
				if (!setProtected(con, "method", met))
					log("Can't set method to :" + met);
			}
			con.setInstanceFollowRedirects(false);
			long cl = req.getContentLength();
			boolean chunked = false;
			Enumeration<String> hdrs = req.getHeaderNames();
			while (hdrs.hasMoreElements()) {
				String n = hdrs.nextElement();
				Enumeration<String> vs = req.getHeaders(n);
				switch (n.toLowerCase()) {
				case "host":
					con.setRequestProperty(n, toURL2.getHost());
					if (vs.hasMoreElements())
						con.addRequestProperty("X-Forwarded-Host", vs.nextElement());
					break;
				case "content-length":
					if (vs.hasMoreElements()) {
						try {
							cl = Long.parseLong(vs.nextElement());
						} catch (Exception e) {

						}
					}

					break;
				case "transfer-encoding":
					String encoding = vs.hasMoreElements() ? vs.nextElement() : null;
					chunked = "chunked".equalsIgnoreCase(encoding);
					con.setRequestProperty(n, encoding);
					break;
				default:
					if (vs.hasMoreElements())
						con.setRequestProperty(n, vs.nextElement());
					while (vs.hasMoreElements()) {
						con.addRequestProperty(n, vs.nextElement());
					}
				}
			}
			con.addRequestProperty("X-Forwarded-For", req.getRemoteAddr());
			con.addRequestProperty("X-Forwarded-Server", InetAddress.getLocalHost().getHostName()); // TODO find out
			con.addRequestProperty("X-Forwarded-Request",  req.getRequestURL().toString());
			if ("POST".equalsIgnoreCase(met)) {
				String contentType = req.getContentType();
				if (contentType != null && contentType.toLowerCase().indexOf("multipart/form-data") >= 0) {
					con.setRequestProperty("content-length", Long.toString(cl));
					copy(req.getInputStream(), con.getOutputStream());
				} else {
					StringBuilder sb = new StringBuilder();
					req.getParameterMap().forEach(
							(String n, String[] vs) -> {
								try {
									for (String v : vs) {
										sb.append(URLEncoder.encode(n, "UTF-8")).append("=")
												.append(URLEncoder.encode(v, "UTF-8")).append("&");
									}
								} catch (Exception e) {
									throw new RuntimeException(e);
								}
							});
					sb.append("\r\n");
					con.setRequestProperty("content-length", Integer.toString(sb.length()));
					OutputStream os = con.getOutputStream();
					os.write(sb.toString().getBytes());
				}
			} else if (cl > 0 || chunked) {
				if (cl > 0)
					con.setRequestProperty("content-length", Long.toString(cl));
				copy(req.getInputStream(), con.getOutputStream());
			}

			resp.setStatus(con.getResponseCode(), con.getResponseMessage());
			//InputStream cis = con.getInputStream();
			con.getHeaderFields().forEach((String n, List<String> vs) -> {
				if (n != null) {
					resp.setHeader(n, null);
					vs.forEach((String v) -> {
						boolean oneHdr = false;
						switch (n.toLowerCase()) {
						case "keep-alive":
							v = null;
							break;
						case "location":
							v = adjustLocation(v, req);
							oneHdr = true;
							break;
						case "connection":
							v = "close";
							oneHdr = true;
							break;
						}
						if (oneHdr)
							resp.setHeader(n, v);
						else
							resp.addHeader(n, v); /*log(String.format("Add header :%s=%s%n", n,v));*/
					});
				}
			});
			copy(con.getInputStream(), resp.getOutputStream());
		} catch (Exception e) {
			e.printStackTrace();
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Request " + toURL + " was processed with " + e);
		}
	}

	protected URL getForwardURL(HttpServletRequest req) throws IOException {
		String pi = req.getPathInfo();
		if (pi == null || pi.isEmpty() || "/".equals(pi))
			return null;
		//log("q:"+pi);
		String q = req.getQueryString();
		return new URL(pi.replaceFirst(";", ":/").substring(1) + (q == null ? "" : "?" + q));
	}

	public static void main(String[] args) {
		System.out.printf("JProxy (c) 2014 D Rogatkin%n");
	}

	static String adjustLocation(String l, HttpServletRequest req) {
		return req.getScheme() + "://" + req.getServerName() + ":" + req.getServerPort() + req.getServletPath() + "/"
				+ l.replaceFirst(":/", "%3b");
	}

	static void copy(InputStream is, OutputStream os) throws IOException {
		byte[] b = new byte[16 * 1024];
		do {
			int l = is.read(b);
			//if (l > 0)
			//System.err.print(new String(b, 0, l));
			if (l > 0)
				os.write(b, 0, l);
			else
				break;
		} while (true);
	}

	static <O, V> boolean setProtected(O obj, String member, V val) {
		try {
			Class<?> c = obj instanceof Class ? (Class) obj : obj.getClass();
			Field f = null;
			try {
				f = c.getField(member);
			} catch (NoSuchFieldException nf) {
				while ((c = c.getSuperclass()) != null) {
					try {
						f = c.getDeclaredField(member);
						break;
					} catch (NoSuchFieldException nf2) {

					}
				}
			}
			f.setAccessible(true);
			f.set(obj instanceof Class ? null : obj, val);
		} catch (Exception e) {
			//e.printStackTrace();
			return false;
		}
		return true;
	}

}
